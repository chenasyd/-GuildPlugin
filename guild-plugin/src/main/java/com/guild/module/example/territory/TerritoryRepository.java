package com.guild.module.example.territory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 公会领地映射本地存储（{@code modules/guild-territory/data/territories.json}）。
 * <p>
 * P7-b 之前仅负责目录初始化与空表加载；WG 区域真源仍在 WorldGuard 区域文件。
 */
public final class TerritoryRepository {

    private static final String STORE_FILE = "territories.json";
    private static final Type STORE_TYPE = new TypeToken<Map<String, TerritoryRecord>>() {}.getType();

    private final File storeFile;
    private final Gson gson;
    private final Logger logger;
    /** key: guildId + "@" + worldName */
    private final Map<String, TerritoryRecord> index = new ConcurrentHashMap<>();

    public TerritoryRepository(File dataDir, Logger logger) {
        this.storeFile = new File(dataDir, STORE_FILE);
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void load() {
        if (!storeFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(storeFile)) {
            Map<String, TerritoryRecord> loaded = gson.fromJson(reader, STORE_TYPE);
            if (loaded != null) {
                index.clear();
                index.putAll(loaded);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load territory store: " + storeFile, e);
        }
    }

    public void save() {
        File parent = storeFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            logger.warning("Could not create territory data directory: " + parent);
            return;
        }
        try (FileWriter writer = new FileWriter(storeFile)) {
            gson.toJson(index, writer);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save territory store: " + storeFile, e);
        }
    }

    public Map<String, TerritoryRecord> viewAll() {
        return Collections.unmodifiableMap(index);
    }

    public Optional<TerritoryRecord> get(int guildId, String worldName) {
        return Optional.ofNullable(index.get(key(guildId, worldName)));
    }

    public void put(TerritoryRecord record) {
        index.put(key(record.getGuildId(), record.getWorldName()), record);
    }

    public void remove(int guildId, String worldName) {
        index.remove(key(guildId, worldName));
    }

    public List<TerritoryRecord> findByGuildId(int guildId) {
        return index.values().stream()
                .filter(record -> record.getGuildId() == guildId)
                .collect(Collectors.toList());
    }

    public void removeAllForGuild(int guildId) {
        index.entrySet().removeIf(entry -> entry.getValue().getGuildId() == guildId);
    }

    static String key(int guildId, String worldName) {
        return guildId + "@" + worldName;
    }
}
