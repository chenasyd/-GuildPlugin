package com.guild.module.example.announcement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 工会公告管理器 - 管理所有工会的公告数据
 * <p>
 * 存储结构：guildId -> List&lt;Announcement&gt;
 * 使用 ConcurrentHashMap 保证线程安全
 * 支持JSON文件持久化
 */
public class AnnouncementManager {

    /** 工会ID -> 该工会的公告列表（按创建时间倒序，最新的在前） */
    private final Map<Integer, List<Announcement>> announcements = new ConcurrentHashMap<>();

    /** 每个工会最大公告数量限制 */
    private static final int MAX_ANNOUNCEMENTS_PER_GUILD = 10;

    private final Gson gson;
    private final File dataDir;
    private final Logger logger;

    public AnnouncementManager(File dataDir, Logger logger) {
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

    /**
     * 从磁盘加载所有公告数据
     */
    public void loadAll() {
        if (!dataDir.exists()) return;

        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return;

        int loadedCount = 0;
        for (File file : files) {
            try {
                String fileName = file.getName();
                // 文件名格式: {guildId}.json
                String guildIdStr = fileName.substring(0, fileName.length() - 5);
                int guildId;
                try {
                    guildId = Integer.parseInt(guildIdStr);
                } catch (NumberFormatException e) {
                    logger.warning("Skipping invalid announcement file: " + fileName);
                    continue;
                }

                List<Announcement> list = loadGuildAnnouncements(file);
                if (list != null && !list.isEmpty()) {
                    announcements.put(guildId, list);
                    loadedCount += list.size();
                }
            } catch (Exception e) {
                logger.warning("Failed to load announcement file: " + file.getName()
                        + " - " + e.getMessage());
            }
        }
        logger.info("Loaded " + loadedCount + " announcement(s) from disk");
    }

    /**
     * 保存所有公告数据到磁盘
     */
    public void saveAll() {
        int guildCount = 0;
        for (Map.Entry<Integer, List<Announcement>> entry : announcements.entrySet()) {
            try {
                saveGuildAnnouncements(entry.getKey(), entry.getValue());
                guildCount++;
            } catch (Exception e) {
                logger.warning("Failed to save announcements for guild " + entry.getKey()
                        + " - " + e.getMessage());
            }
        }
        if (guildCount > 0) {
            logger.info("Saved announcements for " + guildCount + " guild(s) to disk");
        }
    }

    /**
     * 创建新公告
     */
    public Announcement create(int guildId, UUID authorUuid, String authorName,
                               String title, String content) {
        List<Announcement> list = announcements.computeIfAbsent(guildId, k -> new ArrayList<>());

        if (list.size() >= MAX_ANNOUNCEMENTS_PER_GUILD) {
            return null;
        }

        String id = generateId(guildId);
        Announcement announcement = new Announcement(id, guildId, authorUuid, authorName, title, content);

        list.add(0, announcement);

        saveGuildAnnouncements(guildId, list);
        return announcement;
    }

    /**
     * 获取工会的所有公告（按时间倒序）
     */
    public List<Announcement> getAnnouncements(int guildId) {
        List<Announcement> list = announcements.get(guildId);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    /**
     * 根据ID获取公告
     */
    public Announcement getById(String announcementId) {
        for (List<Announcement> list : announcements.values()) {
            if (list == null) continue;
            for (Announcement a : list) {
                if (a.getId().equals(announcementId)) {
                    return a;
                }
            }
        }
        return null;
    }

    /**
     * 更新公告标题和内容
     */
    public boolean update(String announcementId, String title, String content) {
        Announcement a = getById(announcementId);
        if (a == null) return false;
        a.setTitle(title);
        a.setContent(content);
        saveGuildAnnouncements(a.getGuildId(), announcements.get(a.getGuildId()));
        return true;
    }

    /**
     * 删除公告
     */
    public boolean delete(String announcementId) {
        for (Map.Entry<Integer, List<Announcement>> entry : announcements.entrySet()) {
            Iterator<Announcement> it = entry.getValue().iterator();
            while (it.hasNext()) {
                if (it.next().getId().equals(announcementId)) {
                    it.remove();
                    if (entry.getValue().isEmpty()) {
                        announcements.remove(entry.getKey());
                    }
                    saveGuildAnnouncements(entry.getKey(), entry.getValue());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取指定工会的公告数量
     */
    public int getCount(int guildId) {
        List<Announcement> list = announcements.get(guildId);
        return list != null ? list.size() : 0;
    }

    /** 清除某个工会的所有公告（工会删除时调用） */
    public void clearByGuild(int guildId) {
        List<Announcement> removed = announcements.remove(guildId);
        if (removed != null) {
            File file = getGuildFile(guildId);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /** 清除所有数据 */
    public void clearAll() {
        announcements.clear();
    }

    /**
     * 生成唯一公告ID
     */
    private String generateId(int guildId) {
        return "ann_" + guildId + "_" + System.currentTimeMillis() + "_" +
               UUID.randomUUID().toString().substring(0, 8);
    }

    // ==================== 持久化方法 ====================

    private File getGuildFile(int guildId) {
        return new File(dataDir, guildId + ".json");
    }

    private List<Announcement> loadGuildAnnouncements(File file) {
        try (FileReader reader = new FileReader(file)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            List<Announcement> list = new ArrayList<>();
            for (JsonElement element : array) {
                Announcement ann = gson.fromJson(element, Announcement.class);
                list.add(ann);
            }
            return list;
        } catch (IOException e) {
            logger.warning("Error reading " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private void saveGuildAnnouncements(int guildId, List<Announcement> list) {
        File file = getGuildFile(guildId);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(list, writer);
        } catch (IOException e) {
            logger.warning("Error writing " + file.getName() + ": " + e.getMessage());
        }
    }
}
