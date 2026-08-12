package com.guild.world.registry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 意图日志（journal.log）— 意外恢复机制的核心。
 *
 * <p>原则：<b>先写日志、后执行操作</b>。每个世界操作在开始前写入
 * {@code *_START}，正常完成后追加 {@code *_DONE}。
 *
 * <ul>
 *   <li>正常关服：所有操作都有 {@code *_DONE}，journal 为空；</li>
 *   <li>异常中断（崩服/强杀/断电）：存在有 {@code *_START} 而无 {@code *_DONE}
 *       的操作，启动自检时据此判断"上次操作进行到哪一步"并执行补偿。</li>
 * </ul>
 *
 * <p>文件按行追加写入（append），单行已写入的内容不会因崩溃丢失。
 */
public class WorldJournal {

    /** 操作类型 */
    public enum Op {
        /** 创建世界 */
        CREATE,
        /** 加载世界 */
        LOAD,
        /** 卸载世界 */
        UNLOAD,
        /** 删除世界 */
        DELETE,
        /** 粘贴预设 */
        PASTE
    }

    /** 一条未完成（pending）操作记录 */
    public record PendingOp(String world, Op op, int count) {
    }

    private final File file;
    private final Logger logger;
    private final Object lock = new Object();

    public WorldJournal(File worldsDir, Logger logger) {
        this.file = new File(worldsDir, "journal.log");
        this.logger = logger;
    }

    public File getFile() {
        return file;
    }

    /** 记录操作开始 */
    public void begin(Op op, String world) {
        append(op.name() + "_START", world);
    }

    /** 记录操作完成 */
    public void done(Op op, String world) {
        append(op.name() + "_DONE", world);
    }

    private void append(String tag, String world) {
        synchronized (lock) {
            try (FileWriter fw = new FileWriter(file, true)) {
                fw.write(System.currentTimeMillis() + "|" + world + "|" + tag + System.lineSeparator());
            } catch (IOException e) {
                logger.severe("[WorldJournal] Failed to append '" + tag + "' for " + world + ": " + e.getMessage());
            }
        }
    }

    /**
     * 返回所有未完成操作（START 数 - DONE 数 &gt; 0）。
     */
    public List<PendingOp> pending() {
        synchronized (lock) {
            if (!file.exists()) {
                return List.of();
            }
            Map<String, Integer> counts = new HashMap<>();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length < 3) {
                        continue;
                    }
                    String world = parts[1].trim();
                    String tag = parts[2].trim();
                    String opName = null;
                    boolean start = false;
                    for (Op op : Op.values()) {
                        if (tag.startsWith(op.name())) {
                            opName = op.name();
                            start = tag.endsWith("_START");
                            break;
                        }
                    }
                    if (opName == null) {
                        continue;
                    }
                    String key = world + "|" + opName;
                    counts.merge(key, start ? 1 : -1, Integer::sum);
                }
            } catch (IOException e) {
                logger.severe("[WorldJournal] Failed to read journal: " + e.getMessage());
                return List.of();
            }

            List<PendingOp> result = new ArrayList<>();
            counts.forEach((key, value) -> {
                if (value > 0) {
                    String[] parts = key.split("\\|");
                    result.add(new PendingOp(parts[0], Op.valueOf(parts[1]), value));
                }
            });
            return result;
        }
    }

    /** 是否存在未完成操作（用于崩溃检测） */
    public boolean hasPending() {
        return !pending().isEmpty();
    }

    /** 清空日志（正常关服/恢复流程结束后调用） */
    public void clear() {
        synchronized (lock) {
            try (FileWriter fw = new FileWriter(file, false)) {
                // 截断清空
            } catch (IOException e) {
                logger.severe("[WorldJournal] Failed to clear journal: " + e.getMessage());
            }
        }
    }
}
