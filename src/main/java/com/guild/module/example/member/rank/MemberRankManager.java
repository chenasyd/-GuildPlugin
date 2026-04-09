package com.guild.module.example.member.rank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * 成员贡献排名管理器
 * <p>
 * 存储结构：guildId -> List&lt;MemberRank&gt;
 * 使用 ConcurrentHashMap 保证线程安全，支持 JSON 文件持久化。
 */
public class MemberRankManager {

    /** 工会ID -> 该工会的成员排名列表 */
    private final Map<Integer, List<MemberRank>> ranks = new ConcurrentHashMap<>();

    private final Gson gson;
    private final File dataDir;
    private final Logger logger;

    public MemberRankManager(File dataDir, Logger logger) {
        this.dataDir = dataDir;
        this.logger = logger;

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                                context.serialize(src.format(formatter)))
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                                LocalDateTime.parse(json.getAsString(), formatter))
                .create();

        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    // ==================== CRUD ====================

    /**
     * 获取工会的成员排名（按贡献值降序排列）
     */
    public List<MemberRank> getRanks(int guildId) {
        List<MemberRank> list = ranks.get(guildId);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<MemberRank> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> Long.compare(b.getContribution(), a.getContribution()));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * 获取指定成员的排名数据
     */
    public MemberRank getMemberRank(int guildId, UUID playerUuid) {
        List<MemberRank> list = ranks.get(guildId);
        if (list == null) return null;
        for (MemberRank r : list) {
            if (r.getPlayerUuid().equals(playerUuid)) {
                return r;
            }
        }
        return null;
    }

    /**
     * 确保成员在排名列表中存在（若不存在则创建）
     */
    public MemberRank getOrCreate(int guildId, UUID playerUuid, String playerName) {
        List<MemberRank> list = ranks.computeIfAbsent(guildId, k -> new CopyOnWriteArrayList<>());
        for (MemberRank r : list) {
            if (r.getPlayerUuid().equals(playerUuid)) {
                // 更新名称（玩家可能改名）
                r.setPlayerName(playerName);
                return r;
            }
        }
        MemberRank rank = new MemberRank(playerUuid, playerName, guildId);
        list.add(rank);
        return rank;
    }

    /**
     * 增加成员贡献值
     *
     * @return 更新后的 MemberRank，若工会不存在返回 null
     */
    public MemberRank addContribution(int guildId, UUID playerUuid, String playerName, long amount) {
        MemberRank rank = getOrCreate(guildId, playerUuid, playerName);
        if (rank == null) return null;
        rank.addContribution(amount);
        saveGuildRanks(guildId, ranks.get(guildId));
        return rank;
    }

    /**
     * 减少成员贡献值（不低于0）
     *
     * @return 更新后的 MemberRank，若工会不存在返回 null
     */
    public MemberRank reduceContribution(int guildId, UUID playerUuid, String playerName, long amount) {
        MemberRank rank = getOrCreate(guildId, playerUuid, playerName);
        if (rank == null) return null;
        long newContribution = Math.max(0, rank.getContribution() - amount);
        rank.setContribution(newContribution);
        rank.touchActive();
        saveGuildRanks(guildId, ranks.get(guildId));
        return rank;
    }

    /**
     * 记录成员活跃
     */
    public void touchActive(int guildId, UUID playerUuid, String playerName) {
        MemberRank rank = getOrCreate(guildId, playerUuid, playerName);
        if (rank != null) {
            rank.touchActive();
        }
    }

    /**
     * 移除某工会中某成员的排名数据（成员离开时调用）
     */
    public void removeMember(int guildId, UUID playerUuid) {
        List<MemberRank> list = ranks.get(guildId);
        if (list == null) return;
        list.removeIf(r -> r.getPlayerUuid().equals(playerUuid));
        if (list.isEmpty()) {
            ranks.remove(guildId);
            File file = getGuildFile(guildId);
            if (file.exists()) file.delete();
        } else {
            saveGuildRanks(guildId, list);
        }
    }

    /**
     * 清除某工会的所有排名数据（工会删除时调用）
     */
    public void clearByGuild(int guildId) {
        List<MemberRank> removed = ranks.remove(guildId);
        if (removed != null) {
            File file = getGuildFile(guildId);
            if (file.exists()) file.delete();
        }
    }

    /** 清除所有数据 */
    public void clearAll() {
        ranks.clear();
    }

    // ==================== 持久化 ====================

    /**
     * 从磁盘加载所有排名数据
     */
    public void loadAll() {
        if (!dataDir.exists()) return;

        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return;

        int loadedCount = 0;
        for (File file : files) {
            try {
                String fileName = file.getName();
                String guildIdStr = fileName.substring(0, fileName.length() - 5);
                int guildId;
                try {
                    guildId = Integer.parseInt(guildIdStr);
                } catch (NumberFormatException e) {
                    logger.warning("Skipping invalid rank file: " + fileName);
                    continue;
                }

                List<MemberRank> list = loadGuildRanks(file);
                if (list != null && !list.isEmpty()) {
                    ranks.put(guildId, list);
                    loadedCount += list.size();
                }
            } catch (Exception e) {
                logger.warning("Failed to load rank file: " + file.getName()
                        + " - " + e.getMessage());
            }
        }
        logger.info("Loaded " + loadedCount + " member rank(s) from disk");
    }

    /**
     * 保存所有排名数据到磁盘
     */
    public void saveAll() {
        int guildCount = 0;
        for (Map.Entry<Integer, List<MemberRank>> entry : ranks.entrySet()) {
            try {
                saveGuildRanks(entry.getKey(), entry.getValue());
                guildCount++;
            } catch (Exception e) {
                logger.warning("Failed to save ranks for guild " + entry.getKey()
                        + " - " + e.getMessage());
            }
        }
        if (guildCount > 0) {
            logger.info("Saved ranks for " + guildCount + " guild(s) to disk");
        }
    }

    private File getGuildFile(int guildId) {
        return new File(dataDir, guildId + ".json");
    }

    private List<MemberRank> loadGuildRanks(File file) {
        try (FileReader reader = new FileReader(file)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            List<MemberRank> list = new ArrayList<>();
            for (JsonElement element : array) {
                MemberRank rank = gson.fromJson(element, MemberRank.class);
                list.add(rank);
            }
            return list;
        } catch (IOException e) {
            logger.warning("Error reading " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    public void saveGuildRanks(int guildId, List<MemberRank> list) {
        File file = getGuildFile(guildId);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(list, writer);
        } catch (IOException e) {
            logger.warning("Error writing " + file.getName() + ": " + e.getMessage());
        }
    }
}
