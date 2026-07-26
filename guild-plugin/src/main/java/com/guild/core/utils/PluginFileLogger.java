package com.guild.core.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 插件文件日志管理器。
 *
 * <p>将插件操作日志写入 {@code plugins/GuildPlugin/logs/} 目录下的按日分割文件。
 * 文件名格式：{@code guild-yyyy-MM-dd.log}
 *
 * <h3>日志格式：</h3>
 * <pre>
 * [2026-07-26 19:30:15] [COMMAND] [Player:Steve] /guild create MyGuild
 * [2026-07-26 19:30:16] [GUI] [Player:Steve] 打开 MainGuildGUI
 * [2026-07-26 19:30:17] [QUERY] [System] 查询工会列表 page=1
 * [2026-07-26 19:30:18] [ADMIN] [Console] /guildadmin delete TestGuild
 * </pre>
 *
 * <h3>日志类别：</h3>
 * <ul>
 *   <li>COMMAND — 玩家执行工会指令</li>
 *   <li>ADMIN — 管理员执行管理指令</li>
 *   <li>GUI — GUI 打开/关闭/点击操作</li>
 *   <li>QUERY — 数据查询操作</li>
 *   <li>SYSTEM — 插件系统事件（启动/关闭/错误）</li>
 * </ul>
 */
public class PluginFileLogger {

    /** 日志类别枚举 */
    public enum Category {
        COMMAND, ADMIN, GUI, QUERY, SYSTEM
    }

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final File logsDir;
    private final Logger julLogger;
    private final ExecutorService writer;

    private volatile LocalDate currentDate;
    private volatile PrintWriter currentWriter;
    private volatile boolean closed = false;

    /**
     * 创建文件日志管理器。
     *
     * @param dataFolder 插件数据目录（plugins/GuildPlugin）
     * @param julLogger  插件 JUL 日志器（用于控制台镜像输出）
     */
    public PluginFileLogger(File dataFolder, Logger julLogger) {
        this.logsDir = new File(dataFolder, "logs");
        this.julLogger = julLogger;
        this.writer = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "GuildPlugin-FileLogger");
            t.setDaemon(true);
            return t;
        });

        // 创建 logs 目录
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            julLogger.warning("[FileLogger] 无法创建日志目录: " + logsDir.getAbsolutePath());
        }

        this.currentDate = LocalDate.now();
        openWriter(currentDate);
    }

    // ── 公共日志方法 ─────────────────────────────────────────────

    /**
     * 记录玩家指令操作。
     *
     * @param playerName 玩家名
     * @param command    完整指令文本（如 "/guild create MyGuild"）
     */
    public void logCommand(String playerName, String command) {
        log(Category.COMMAND, "Player:" + playerName, command);
    }

    /**
     * 记录管理员指令操作。
     *
     * @param sourceName 来源名（玩家名或 "Console"）
     * @param command    完整指令文本
     */
    public void logAdmin(String sourceName, String command) {
        log(Category.ADMIN, sourceName, command);
    }

    /**
     * 记录 GUI 操作。
     *
     * @param playerName 玩家名
     * @param action     操作描述（如 "打开 MainGuildGUI"、"点击 slot=4"）
     */
    public void logGui(String playerName, String action) {
        log(Category.GUI, "Player:" + playerName, action);
    }

    /**
     * 记录数据查询操作。
     *
     * @param source  来源（玩家名或 "System"）
     * @param query   查询描述
     */
    public void logQuery(String source, String query) {
        log(Category.QUERY, source, query);
    }

    /**
     * 记录系统事件。
     *
     * @param message 事件描述
     */
    public void logSystem(String message) {
        log(Category.SYSTEM, "System", message);
    }

    /**
     * 通用日志方法。
     *
     * @param category 日志类别
     * @param source   来源标识（如 "Player:Steve"、"Console"、"System"）
     * @param message  日志内容
     */
    public void log(Category category, String source, String message) {
        if (closed) return;

        LocalDateTime now = LocalDateTime.now();
        String line = String.format("[%s] [%s] [%s] %s",
                now.format(TIME_FMT), category.name(), source, message);

        // 异步写入文件
        writer.submit(() -> writeLine(now.toLocalDate(), line));

        // 镜像到 JUL（控制台可见，使用 FINE 级别避免刷屏）
        julLogger.log(Level.FINE, "[{0}] {1}", new Object[]{category.name(), message});
    }

    // ── 生命周期 ─────────────────────────────────────────────────

    /**
     * 关闭日志管理器，刷新缓冲区并释放资源。
     * 在插件 onDisable 中调用。
     */
    public void shutdown() {
        closed = true;
        writer.submit(() -> {
            closeWriter();
        });
        writer.shutdown();
        try {
            if (!writer.awaitTermination(5, TimeUnit.SECONDS)) {
                writer.shutdownNow();
            }
        } catch (InterruptedException e) {
            writer.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ── 内部方法 ─────────────────────────────────────────────────

    private void writeLine(LocalDate date, String line) {
        try {
            // 日期切换时滚动到新文件
            if (!date.equals(currentDate)) {
                closeWriter();
                currentDate = date;
                openWriter(date);
            }

            if (currentWriter != null) {
                currentWriter.println(line);
                currentWriter.flush();
            }
        } catch (Exception e) {
            julLogger.warning("[FileLogger] 写入日志失败: " + e.getMessage());
        }
    }

    private void openWriter(LocalDate date) {
        try {
            File logFile = new File(logsDir, "guild-" + date.format(DATE_FMT) + ".log");
            currentWriter = new PrintWriter(
                    new BufferedWriter(new FileWriter(logFile, true)), false);
        } catch (IOException e) {
            julLogger.warning("[FileLogger] 无法打开日志文件: " + e.getMessage());
            currentWriter = null;
        }
    }

    private void closeWriter() {
        if (currentWriter != null) {
            currentWriter.flush();
            currentWriter.close();
            currentWriter = null;
        }
    }
}
