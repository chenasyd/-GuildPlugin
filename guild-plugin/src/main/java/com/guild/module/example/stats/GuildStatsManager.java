package com.guild.module.example.stats;

import com.guild.module.example.stats.model.GuildStatistics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class GuildStatsManager {
    private final File dataDir;
    private final Logger logger;
    private final Gson gson;
    private final ConcurrentHashMap<Integer, GuildStatistics> statsCache = new ConcurrentHashMap<>();

    public GuildStatsManager(File dataDir, Logger logger) {
        this.dataDir = dataDir;
        this.logger = logger;
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                @Override
                public void write(JsonWriter out, LocalDate value) throws IOException {
                    out.value(value.format(dateFormatter));
                }
                @Override
                public LocalDate read(JsonReader in) throws IOException {
                    return LocalDate.parse(in.nextString(), dateFormatter);
                }
            })
            .create();
    }

    public void loadAll() {
        if (!dataDir.exists()) return;

        File[] files = dataDir.listFiles((dir, name) ->
            name.endsWith("_stats.json"));
        if (files == null) return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                GuildStatistics stats = gson.fromJson(reader, GuildStatistics.class);
                if (stats == null) {
                    logger.warning("[Stats] 跳过空文件: " + file.getName());
                    continue;
                }
                if (stats.getGuildId() <= 0) {
                    logger.warning("[Stats] 跳过无效数据( guildId<=0 ): " + file.getName());
                    continue;
                }
                statsCache.put(stats.getGuildId(), stats);
                logger.info("[Stats] 加载统计: " + file.getName());
            } catch (Exception e) {
                logger.warning("[Stats] 无法加载 " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public void saveAll() {
        for (GuildStatistics stats : statsCache.values()) {
            save(stats);
        }
    }

    public void save(GuildStatistics stats) {
        File file = new File(dataDir, stats.getGuildId() + "_stats.json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(stats, writer);
        } catch (IOException e) {
            logger.severe("[Stats] 保存失败: " + e.getMessage());
        }
    }

    public GuildStatistics get(int guildId) {
        return statsCache.get(guildId);
    }

    public void put(GuildStatistics stats) {
        statsCache.put(stats.getGuildId(), stats);
    }

    public void remove(int guildId) {
        statsCache.remove(guildId);
        File file = new File(dataDir, guildId + "_stats.json");
        if (file.exists()) file.delete();
    }

    public void clearAll() {
        statsCache.clear();
    }

    public void cleanupOlderThanDays(int days) {
        long threshold = System.currentTimeMillis() - (days * 86400000L);
        statsCache.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastUpdated() < threshold) {
                remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
