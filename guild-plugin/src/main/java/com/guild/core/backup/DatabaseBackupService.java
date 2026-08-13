package com.guild.core.backup;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Database backup (SQLite zip / MySQL sql.gz) and light maintenance.
 */
public class DatabaseBackupService {

    public enum BackupReason {
        MANUAL,
        STARTUP_DAILY,
        VERSION_CHANGE
    }

    public record BackupResult(boolean success, String message, File file) {}

    public record MaintenanceResult(boolean success, String summary) {}

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String META_NAME = ".last-backup-meta.yml";

    private final GuildPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final SqliteBackupWriter sqliteWriter;
    private final MysqlBackupWriter mysqlWriter;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DatabaseBackupService(GuildPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.logger = plugin.getLogger();
        this.sqliteWriter = new SqliteBackupWriter(plugin, databaseManager);
        this.mysqlWriter = new MysqlBackupWriter(plugin, databaseManager);
    }

    public File getBackupDirectory() {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        String dir = cfg.getString("backup.directory", "backup");
        File folder = new File(dir);
        if (!folder.isAbsolute()) {
            folder = new File(plugin.getDataFolder(), dir);
        }
        if (!folder.exists() && !folder.mkdirs()) {
            logger.warning("[Backup] Failed to create backup directory: " + folder.getAbsolutePath());
        }
        return folder;
    }

    public boolean isEnabled() {
        return plugin.getConfigManager().getMainConfig().getBoolean("backup.enabled", true);
    }

    public int getMaxBackups() {
        return Math.max(1, plugin.getConfigManager().getMainConfig().getInt("backup.max-backups", 50));
    }

    /**
     * Startup policy: one backup if no success today OR plugin version changed.
     */
    public void maybeAutoBackupOnStartup() {
        if (!isEnabled()) {
            logger.info("[Backup] Auto backup disabled (backup.enabled=false)");
            return;
        }
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        boolean daily = cfg.getBoolean("backup.on-startup-daily", true);
        boolean onVersion = cfg.getBoolean("backup.on-version-change", true);

        BackupMeta meta = BackupMeta.load(new File(getBackupDirectory(), META_NAME));
        String version = sanitizeVersion(plugin.getDescription().getVersion());

        boolean needDaily = daily && !meta.hasBackupToday();
        boolean needVersion = onVersion && !meta.isSameVersion(version);
        if (!needDaily && !needVersion) {
            logger.info("[Backup] Skip startup backup (already today, version unchanged)");
            return;
        }
        BackupReason reason = needVersion ? BackupReason.VERSION_CHANGE : BackupReason.STARTUP_DAILY;
        backupAsync(reason).thenAccept(result -> {
            if (result.success()) {
                logger.info("[Backup] Startup backup OK (" + reason + "): " +
                        (result.file() != null ? result.file().getName() : ""));
            } else {
                logger.warning("[Backup] Startup backup failed: " + result.message());
            }
        });
    }

    public CompletableFuture<BackupResult> backupAsync(BackupReason reason) {
        return CompletableFuture.supplyAsync(() -> {
            if (!running.compareAndSet(false, true)) {
                return new BackupResult(false, "Backup already in progress", null);
            }
            try {
                return doBackup(reason);
            } catch (Exception e) {
                logger.severe("[Backup] Failed: " + e.getMessage());
                return new BackupResult(false, e.getMessage() != null ? e.getMessage() : e.toString(), null);
            } finally {
                running.set(false);
            }
        });
    }

    private BackupResult doBackup(BackupReason reason) throws Exception {
        File backupDir = getBackupDirectory();
        String version = sanitizeVersion(plugin.getDescription().getVersion());
        String ts = LocalDateTime.now().format(FILE_TS);
        String base = "guild-backup_" + version + "_" + ts;

        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        File out;
        String dbType;
        if (databaseManager.getDatabaseType() == DatabaseManager.DatabaseType.SQLITE) {
            dbType = "sqlite";
            out = sqliteWriter.write(backupDir, base);
        } else {
            dbType = "mysql";
            String method = cfg.getString("backup.mysql.method", "jdbc");
            String dumpPath = cfg.getString("backup.mysql.mysqldump-path", "mysqldump");
            out = mysqlWriter.write(backupDir, base, method, dumpPath);
        }

        BackupMeta meta = BackupMeta.load(new File(backupDir, META_NAME));
        meta.markSuccess(version, dbType, out.getName());
        meta.save(new File(backupDir, META_NAME));
        pruneOldBackups(backupDir);

        return new BackupResult(true,
                "Backup created (" + reason.name().toLowerCase(Locale.ROOT) + "): " + out.getName(),
                out);
    }

    private void pruneOldBackups(File backupDir) {
        File[] files = backupDir.listFiles((dir, name) ->
                name.startsWith("guild-backup_")
                        && (name.endsWith(".zip") || name.endsWith(".sql.gz") || name.endsWith(".sql")));
        if (files == null || files.length <= getMaxBackups()) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int remove = files.length - getMaxBackups();
        for (int i = 0; i < remove; i++) {
            if (!files[i].delete()) {
                logger.warning("[Backup] Failed to delete old backup: " + files[i].getName());
            } else {
                logger.info("[Backup] Pruned old backup: " + files[i].getName());
            }
        }
    }

    public CompletableFuture<MaintenanceResult> runMaintenanceAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doMaintenance();
            } catch (Exception e) {
                logger.severe("[Backup] Maintenance failed: " + e.getMessage());
                return new MaintenanceResult(false, e.getMessage() != null ? e.getMessage() : e.toString());
            }
        });
    }

    private MaintenanceResult doMaintenance() throws Exception {
        StringBuilder sb = new StringBuilder();
        DatabaseManager.DatabaseType type = databaseManager.getDatabaseType();
        sb.append("Type: ").append(type).append('\n');

        BackupMeta meta = BackupMeta.load(new File(getBackupDirectory(), META_NAME));
        sb.append("Last backup: ")
                .append(meta.getLastSuccessTime() != null ? meta.getLastSuccessTime() : "n/a")
                .append('\n');
        if (meta.getLastFile() != null) {
            sb.append("Last file: ").append(meta.getLastFile()).append('\n');
        }

        if (type == DatabaseManager.DatabaseType.SQLITE) {
            File db = databaseManager.getSqliteDatabaseFile();
            sb.append("File: ").append(db.getAbsolutePath()).append('\n');
            sb.append("Size: ").append(db.exists() ? formatSize(db.length()) : "missing").append('\n');
            try (Connection conn = databaseManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA optimize");
                try {
                    stmt.execute("VACUUM");
                    sb.append("Optimize: PRAGMA optimize + VACUUM OK\n");
                } catch (Exception ve) {
                    sb.append("Optimize: PRAGMA optimize OK; VACUUM skipped (").append(ve.getMessage()).append(")\n");
                }
            }
        } else {
            sb.append("Host: ").append(databaseManager.getMysqlHost())
                    .append(':').append(databaseManager.getMysqlPort())
                    .append('/').append(databaseManager.getMysqlDatabase()).append('\n');
            int tables = 0;
            try (Connection conn = databaseManager.getConnection()) {
                DatabaseMetaData md = conn.getMetaData();
                try (ResultSet rs = md.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        tables++;
                        String table = rs.getString("TABLE_NAME");
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute("OPTIMIZE TABLE `" + table.replace("`", "``") + "`");
                        } catch (Exception ignored) {
                            // some engines / privileges may reject OPTIMIZE; continue
                        }
                    }
                }
            }
            sb.append("Tables: ").append(tables).append('\n');
            sb.append("Optimize: OPTIMIZE TABLE attempted\n");
        }

        sb.append("Backup dir: ").append(getBackupDirectory().getAbsolutePath());
        return new MaintenanceResult(true, sb.toString());
    }

    public BackupMeta getMeta() {
        return BackupMeta.load(new File(getBackupDirectory(), META_NAME));
    }

    private static String sanitizeVersion(String version) {
        if (version == null || version.isBlank()) {
            return "unknown";
        }
        return version.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
