package com.guild.core.backup;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * MySQL backup: JDBC logical dump to {@code .sql.gz}, or optional {@code mysqldump}.
 */
public final class MysqlBackupWriter {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public MysqlBackupWriter(GuildPlugin plugin, DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.logger = plugin.getLogger();
    }

    public File write(File backupDir, String archiveBaseName, String method, String mysqldumpPath)
            throws Exception {
        if ("mysqldump".equalsIgnoreCase(method)) {
            return writeViaMysqldump(backupDir, archiveBaseName, mysqldumpPath);
        }
        return writeViaJdbc(backupDir, archiveBaseName);
    }

    private File writeViaMysqldump(File backupDir, String archiveBaseName, String mysqldumpPath)
            throws Exception {
        String bin = (mysqldumpPath == null || mysqldumpPath.isBlank()) ? "mysqldump" : mysqldumpPath;
        File sqlFile = new File(backupDir, archiveBaseName + "_mysql.sql");
        File gzFile = new File(backupDir, archiveBaseName + "_mysql.sql.gz");

        List<String> cmd = new ArrayList<>();
        cmd.add(bin);
        cmd.add("--single-transaction");
        cmd.add("--routines");
        cmd.add("--triggers");
        cmd.add("-h");
        cmd.add(databaseManager.getMysqlHost());
        cmd.add("-P");
        cmd.add(String.valueOf(databaseManager.getMysqlPort()));
        cmd.add("-u");
        cmd.add(databaseManager.getMysqlUsername());
        if (databaseManager.getMysqlPassword() != null && !databaseManager.getMysqlPassword().isEmpty()) {
            cmd.add("-p" + databaseManager.getMysqlPassword());
        }
        cmd.add(databaseManager.getMysqlDatabase());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (var in = process.getInputStream();
             var fos = new FileOutputStream(sqlFile)) {
            in.transferTo(fos);
        }
        boolean finished = process.waitFor(30, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            //noinspection ResultOfMethodCallIgnored
            sqlFile.delete();
            throw new IOException("mysqldump timed out");
        }
        if (process.exitValue() != 0) {
            //noinspection ResultOfMethodCallIgnored
            sqlFile.delete();
            throw new IOException("mysqldump exited with code " + process.exitValue());
        }

        gzipFile(sqlFile, gzFile);
        //noinspection ResultOfMethodCallIgnored
        sqlFile.delete();
        return gzFile;
    }

    private File writeViaJdbc(File backupDir, String archiveBaseName) throws Exception {
        File gzFile = new File(backupDir, archiveBaseName + "_mysql.sql.gz");
        try (Connection conn = databaseManager.getConnection();
             GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(gzFile));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzip, StandardCharsets.UTF_8))) {

            writer.write("-- GuildPlugin MySQL backup (JDBC)\n");
            writer.write("-- Database: " + databaseManager.getMysqlDatabase() + "\n");
            writer.write("SET NAMES utf8mb4;\n");
            writer.write("SET FOREIGN_KEY_CHECKS=0;\n\n");

            List<String> tables = listTables(conn);
            for (String table : tables) {
                dumpTable(conn, writer, table);
            }

            writer.write("SET FOREIGN_KEY_CHECKS=1;\n");
            writer.flush();
        }
        return gzFile;
    }

    private List<String> listTables(Connection conn) throws Exception {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                if (name != null && !name.isBlank()) {
                    tables.add(name);
                }
            }
        }
        return tables;
    }

    private void dumpTable(Connection conn, BufferedWriter writer, String table) throws Exception {
        String safe = backtick(table);
        writer.write("-- Table `" + table + "`\n");
        writer.write("DROP TABLE IF EXISTS " + safe + ";\n");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE " + safe)) {
            if (rs.next()) {
                String create = rs.getString(2);
                writer.write(create);
                writer.write(";\n\n");
            }
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + safe)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            StringBuilder colList = new StringBuilder();
            for (int i = 1; i <= cols; i++) {
                if (i > 1) colList.append(", ");
                colList.append(backtick(md.getColumnName(i)));
            }

            int batch = 0;
            StringBuilder values = new StringBuilder();
            while (rs.next()) {
                if (batch > 0) {
                    values.append(",\n");
                }
                values.append("(");
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) values.append(", ");
                    values.append(sqlLiteral(rs, i, md.getColumnType(i)));
                }
                values.append(")");
                batch++;
                if (batch >= 100) {
                    writer.write("INSERT INTO " + safe + " (" + colList + ") VALUES\n");
                    writer.write(values.toString());
                    writer.write(";\n");
                    values.setLength(0);
                    batch = 0;
                }
            }
            if (batch > 0) {
                writer.write("INSERT INTO " + safe + " (" + colList + ") VALUES\n");
                writer.write(values.toString());
                writer.write(";\n");
            }
            writer.write("\n");
        }
    }

    private static String backtick(String ident) {
        return "`" + ident.replace("`", "``") + "`";
    }

    private static String sqlLiteral(ResultSet rs, int index, int type) throws Exception {
        Object obj = rs.getObject(index);
        if (obj == null || rs.wasNull()) {
            return "NULL";
        }
        switch (type) {
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.BOOLEAN:
            case Types.BIT:
                return obj.toString();
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB: {
                byte[] bytes = rs.getBytes(index);
                if (bytes == null) return "NULL";
                StringBuilder hex = new StringBuilder("0x");
                for (byte b : bytes) {
                    hex.append(String.format(Locale.ROOT, "%02X", b));
                }
                return hex.toString();
            }
            default: {
                String s = rs.getString(index);
                if (s == null) return "NULL";
                return "'" + s.replace("\\", "\\\\").replace("'", "''").replace("\n", "\\n").replace("\r", "\\r") + "'";
            }
        }
    }

    private static void gzipFile(File src, File dest) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
             GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(dest))) {
            in.transferTo(out);
        }
    }
}
