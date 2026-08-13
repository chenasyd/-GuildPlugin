package com.guild.core.backup;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Persists last successful backup metadata under the backup directory.
 */
public final class BackupMeta {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String lastSuccessTime;
    private String lastSuccessDay;
    private String lastPluginVersion;
    private String lastDbType;
    private String lastFile;

    public static BackupMeta load(File metaFile) {
        BackupMeta meta = new BackupMeta();
        if (metaFile == null || !metaFile.exists()) {
            return meta;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(metaFile);
        meta.lastSuccessTime = yaml.getString("last-success-time");
        meta.lastSuccessDay = yaml.getString("last-success-day");
        meta.lastPluginVersion = yaml.getString("last-plugin-version");
        meta.lastDbType = yaml.getString("last-db-type");
        meta.lastFile = yaml.getString("last-file");
        return meta;
    }

    public void save(File metaFile) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("last-success-time", lastSuccessTime);
        yaml.set("last-success-day", lastSuccessDay);
        yaml.set("last-plugin-version", lastPluginVersion);
        yaml.set("last-db-type", lastDbType);
        yaml.set("last-file", lastFile);
        yaml.save(metaFile);
    }

    public void markSuccess(String pluginVersion, String dbType, String fileName) {
        LocalDateTime now = LocalDateTime.now();
        this.lastSuccessTime = now.format(ISO);
        this.lastSuccessDay = LocalDate.now().toString();
        this.lastPluginVersion = pluginVersion;
        this.lastDbType = dbType;
        this.lastFile = fileName;
    }

    public boolean hasBackupToday() {
        return lastSuccessDay != null && lastSuccessDay.equals(LocalDate.now().toString());
    }

    public boolean isSameVersion(String currentVersion) {
        if (currentVersion == null) {
            return lastPluginVersion == null;
        }
        return currentVersion.equals(lastPluginVersion);
    }

    public String getLastSuccessTime() { return lastSuccessTime; }
    public String getLastPluginVersion() { return lastPluginVersion; }
    public String getLastFile() { return lastFile; }
    public String getLastDbType() { return lastDbType; }
}
