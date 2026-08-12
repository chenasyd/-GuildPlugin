package com.guild.world.registry;

import com.guild.world.model.GuildWorld;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 世界注册表（worlds.yml）— 所有受管世界的元数据持久化。
 *
 * <p>文件结构：
 * <pre>
 * clean-shutdown: false
 * worlds:
 *   gw_test:
 *     type: BATTLE
 *     status: READY
 *     ...
 * </pre>
 *
 * <p>{@code clean-shutdown} 标志：正常关服置 {@code true}，插件启动时立即置
 * {@code false}。若启动时发现为 {@code false} 且 journal 非空，判定上次运行
 * 非正常结束，进入恢复模式。
 *
 * <p>写入采用<b>原子替换</b>（临时文件 + {@code ATOMIC_MOVE}），避免写一半
 * 崩溃导致注册表损坏。
 */
public class WorldRegistry {

    private final File file;
    private final Logger logger;
    private final Map<String, GuildWorld> worlds = new LinkedHashMap<>();
    private volatile boolean cleanShutdown = false;

    public WorldRegistry(File worldsDir, Logger logger) {
        this.file = new File(worldsDir, "worlds.yml");
        this.logger = logger;
    }

    public File getFile() {
        return file;
    }

    /** 从磁盘加载注册表 */
    public synchronized void load() {
        worlds.clear();
        if (!file.exists()) {
            cleanShutdown = false;
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        cleanShutdown = config.getBoolean("clean-shutdown", false);
        ConfigurationSection ws = config.getConfigurationSection("worlds");
        if (ws == null) {
            return;
        }
        for (String key : ws.getKeys(false)) {
            GuildWorld gw = GuildWorld.loadFrom(ws.getConfigurationSection(key), key);
            if (gw != null) {
                worlds.put(key, gw);
            }
        }
    }

    /** 原子写入注册表 */
    public synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("clean-shutdown", cleanShutdown);
        ConfigurationSection ws = config.createSection("worlds");
        for (GuildWorld gw : worlds.values()) {
            gw.saveTo(ws.createSection(gw.getWorldName()));
        }
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.save(tmp);
            try {
                Files.move(tmp.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            logger.severe("[WorldRegistry] Failed to save registry: " + e.getMessage());
        }
    }

    public synchronized void put(GuildWorld world) {
        worlds.put(world.getWorldName(), world);
    }

    public synchronized void remove(String worldName) {
        worlds.remove(worldName);
    }

    public synchronized GuildWorld get(String worldName) {
        return worlds.get(worldName);
    }

    public synchronized boolean contains(String worldName) {
        return worlds.containsKey(worldName);
    }

    public synchronized Collection<GuildWorld> all() {
        return new LinkedHashMap<>(worlds).values();
    }

    public synchronized int size() {
        return worlds.size();
    }

    public boolean isCleanShutdown() {
        return cleanShutdown;
    }

    public void setCleanShutdown(boolean cleanShutdown) {
        this.cleanShutdown = cleanShutdown;
    }
}
