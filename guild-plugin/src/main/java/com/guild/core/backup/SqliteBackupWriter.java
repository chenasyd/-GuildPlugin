package com.guild.core.backup;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Packs SQLite main DB + WAL/SHM sidecars into a zip archive.
 * <p>
 * Important: do <b>not</b> {@code wal_checkpoint(TRUNCATE)} before copying — that clears
 * {@code *.db-wal} and makes the archive look incomplete. Snapshot-copy the live files first
 * under a short write lock, then zip the copies.
 */
public final class SqliteBackupWriter {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public SqliteBackupWriter(GuildPlugin plugin, DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.logger = plugin.getLogger();
    }

    public File write(File backupDir, String archiveBaseName) throws Exception {
        File dbFile = databaseManager.getSqliteDatabaseFile();
        if (!dbFile.exists()) {
            throw new IOException("SQLite database file not found: " + dbFile.getAbsolutePath());
        }

        File parent = dbFile.getParentFile();
        File wal = new File(parent, dbFile.getName() + "-wal");
        File shm = new File(parent, dbFile.getName() + "-shm");

        File tempDir = Files.createTempDirectory(backupDir.toPath(), "sqlite-snap-").toFile();
        List<File> staged = new ArrayList<>();
        try {
            // Brief exclusive lock so WAL set is as consistent as possible while we copy
            try (Connection conn = databaseManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("BEGIN IMMEDIATE");
                try {
                    staged.add(copySibling(dbFile, tempDir));
                    // Always include sidecars when present (even if empty — restore expects the set)
                    if (wal.exists()) {
                        staged.add(copySibling(wal, tempDir));
                    } else {
                        logger.info("[Backup] SQLite WAL file not present at copy time: " + wal.getName());
                    }
                    if (shm.exists()) {
                        staged.add(copySibling(shm, tempDir));
                    }
                } finally {
                    try {
                        stmt.execute("ROLLBACK");
                    } catch (Exception ignored) {
                        // connection may already be closed by pool quirks
                    }
                }
            }

            File out = new File(backupDir, archiveBaseName + "_sqlite.zip");
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))) {
                byte[] buf = new byte[8192];
                for (File src : staged) {
                    zos.putNextEntry(new ZipEntry(src.getName()));
                    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(src))) {
                        int n;
                        while ((n = in.read(buf)) >= 0) {
                            zos.write(buf, 0, n);
                        }
                    }
                    zos.closeEntry();
                }
            }

            boolean hasWal = staged.stream().anyMatch(f -> f.getName().endsWith("-wal"));
            if (!hasWal) {
                logger.warning("[Backup] Archive has no -wal sidecar (file absent during snapshot). "
                        + "Main DB alone is still usable if WAL was empty/checkpointed.");
            } else {
                logger.info("[Backup] SQLite snapshot includes " + staged.size() + " file(s): "
                        + staged.stream().map(File::getName).reduce((a, b) -> a + ", " + b).orElse(""));
            }
            return out;
        } finally {
            deleteRecursive(tempDir);
        }
    }

    private static File copySibling(File source, File tempDir) throws IOException {
        File dest = new File(tempDir, source.getName());
        // Prefer NIO copy; fall back to stream for odd FS
        try {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(dest)) {
                in.transferTo(out);
            }
        }
        return dest;
    }

    private static void deleteRecursive(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children != null) {
            for (File c : children) {
                if (c.isDirectory()) {
                    deleteRecursive(c);
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    c.delete();
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        dir.delete();
    }
}
