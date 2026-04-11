package com.guild.module.example.quest;

import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestProgress;
import com.guild.core.module.ModuleContext;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class QuestManager {
    private final File dataDir;
    private final Logger logger;
    private final Map<String, QuestDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, List<QuestProgress>> guildProgressMap = new ConcurrentHashMap<>();
    private static final java.time.ZoneId ZONE = java.time.ZoneId.systemDefault();
    private final Object saveLock = new Object();
    private ModuleContext context;

    public QuestManager(File dataDir, Logger logger) {
        this.dataDir = new File(dataDir, "quests");
        this.logger = logger;
        this.dataDir.mkdirs();
    }

    public void setContext(ModuleContext ctx) { this.context = ctx; }

    public void registerDefinition(QuestDefinition def) {
        definitions.put(def.getId(), def);
        logger.info("[Quest] 注册任务: " + def.getName() + " (" + def.getType() + ")");
    }

    public void unregisterDefinition(String questId) {
        definitions.remove(questId);
    }

    public Collection<QuestDefinition> getDefinitions() {
        return definitions.values();
    }

    public QuestDefinition getDefinition(String questId) {
        return definitions.get(questId);
    }

    public List<QuestDefinition> getDefinitionsByType(QuestDefinition.QuestType type) {
        List<QuestDefinition> result = new ArrayList<>();
        for (QuestDefinition def : definitions.values()) {
            if (def.getType() == type) result.add(def);
        }
        return result;
    }

    public boolean acceptQuest(QuestProgress progress) {
        if (progress.getGuildId() <= 0) {
            logger.warning("[Quest] ⚠️ 拒绝接取: guildId=" + progress.getGuildId() +
                ", questId=" + progress.getQuestId() + ", player=" + progress.getPlayerName());
            return false;
        }
        String key = progressKey(progress.getGuildId(), progress.getPlayerUuid());
        List<QuestProgress> list = guildProgressMap.computeIfAbsent(key,
            k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            for (QuestProgress p : list) {
                if (p.getQuestId().equals(progress.getQuestId())) return false;
            }
            list.add(progress);
        }
        saveGuildProgress(progress.getGuildId());
        logger.info("[Quest] ✅ " + progress.getPlayerName() + " 接取: " + progress.getQuestId() +
            " (guildId=" + progress.getGuildId() + ", key=" + key + ")");
        return true;
    }

    public List<QuestProgress> getPlayerActiveQuests(int guildId, UUID playerUuid) {
        String key = progressKey(guildId, playerUuid);
        List<QuestProgress> all = guildProgressMap.get(key);
        if (all == null) return Collections.emptyList();
        List<QuestProgress> active = new ArrayList<>();
        synchronized (all) {
            for (QuestProgress p : all) {
                if (!p.isClaimed()) active.add(p);
            }
        }
        return active;
    }

    public QuestProgress getPlayerQuest(int guildId, UUID playerUuid, String questId) {
        String key = progressKey(guildId, playerUuid);
        List<QuestProgress> all = guildProgressMap.get(key);
        if (all == null) return null;
        synchronized (all) {
            for (QuestProgress p : all) {
                if (p.getQuestId().equals(questId) && !p.isClaimed()) return p;
            }
        }
        return null;
    }

    public QuestProgress getPlayerQuestAny(int guildId, UUID playerUuid, String questId) {
        String key = progressKey(guildId, playerUuid);
        List<QuestProgress> all = guildProgressMap.get(key);
        if (all == null) return null;
        synchronized (all) {
            for (QuestProgress p : all) {
                if (p.getQuestId().equals(questId)) return p;
            }
        }
        return null;
    }

    public void updateAndSave(int guildId, UUID playerUuid, String questId,
                               int objectiveIndex, int delta) {
        String key = progressKey(guildId, playerUuid);
        List<QuestProgress> all = guildProgressMap.get(key);
        if (all == null) return;
        
        synchronized (all) {
            QuestProgress progress = null;
            for (QuestProgress p : all) {
                if (p.getQuestId().equals(questId) && !p.isClaimed()) {
                    progress = p;
                    break;
                }
            }
            
            if (progress != null) {
                boolean updated = progress.updateProgress(objectiveIndex, delta);
                if (updated) {
                    checkAndMarkCompletion(progress);
                    saveGuildProgress(guildId);
                }
            }
        }
    }

    private void checkAndMarkCompletion(QuestProgress progress) {
        synchronized (progress) {
            if (progress.isCompletedMarked() || progress.isClaimed()) return;
            QuestDefinition definition = definitions.get(progress.getQuestId());
            if (definition == null) return;
            if (progress.isObjectivesCompleted(definition)) {
                progress.markAsCompleted();
                logger.info("[Quest] 任务完成: " + definition.getName() +
                    " (" + progress.getPlayerName() + ")");
            }
        }
    }

    public void claimReward(QuestProgress progress) {
        synchronized (progress) {
            progress.setClaimed();
            saveGuildProgress(progress.getGuildId());
        }
    }

    public int getAcceptedCount(int guildId, UUID playerUuid, QuestDefinition.QuestType type) {
        String key = progressKey(guildId, playerUuid);
        List<QuestProgress> all = guildProgressMap.get(key);
        if (all == null) return 0;
        int count = 0;
        synchronized (all) {
            for (QuestProgress p : all) {
                if (p.isClaimed()) continue;
                QuestDefinition def = definitions.get(p.getQuestId());
                if (def != null && def.getType() == type) count++;
            }
        }
        return count;
    }

    public int getCompletedCount(int guildId, UUID playerUuid, QuestDefinition.QuestType type) {
        String key = progressKey(guildId, playerUuid);
        List<QuestProgress> all = guildProgressMap.get(key);
        if (all == null) return 0;
        int count = 0;
        Set<String> counted = new HashSet<>();
        synchronized (all) {
            for (QuestProgress p : all) {
                if (!p.isClaimed()) continue;
                if (counted.contains(p.getQuestId())) continue;
                QuestDefinition def = definitions.get(p.getQuestId());
                if (def != null && def.getType() == type) {
                    count++;
                    counted.add(p.getQuestId());
                }
            }
        }
        return count;
    }

    public boolean canAccept(int guildId, UUID playerUuid, QuestDefinition def) {
        if (def == null) return false;
        switch (def.getType()) {
            case DAILY:
                return getAcceptedCount(guildId, playerUuid, QuestDefinition.QuestType.DAILY)
                    < context.getConfig().getInt("settings.max-daily-quests", 3);
            case WEEKLY:
                // 每周任务每周可接一次（每个任务独立）
                QuestProgress existingWeekly = getPlayerQuestAny(guildId, playerUuid, def.getId());
                if (existingWeekly != null) {
                    // 检查是否是本周的记录
                    if (isSameWeek(existingWeekly.getAcceptedTime())) {
                        return false; // 本周已接取过
                    }
                }
                return true;
            case ONE_TIME:
                QuestProgress existing = getPlayerQuestAny(guildId, playerUuid, def.getId());
                return existing == null;
            default:
                return true;
        }
    }

    public void resetDailyQuests(int guildId) {
        synchronized (saveLock) {
            resetQuestsByType(guildId, QuestDefinition.QuestType.DAILY);
            doSaveGuildProgress(guildId);
            logger.info("[Quest] 公会 #" + guildId + " 每日任务已重置");
        }
    }

    public void resetWeeklyQuests(int guildId) {
        synchronized (saveLock) {
            resetQuestsByType(guildId, QuestDefinition.QuestType.WEEKLY);
            doSaveGuildProgress(guildId);
            logger.info("[Quest] 公会 #" + guildId + " 每周任务已重置");
        }
    }

    private void resetQuestsByType(int guildId, QuestDefinition.QuestType type) {
        String prefix = guildId + "_";
        for (Map.Entry<String, List<QuestProgress>> entry : guildProgressMap.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) continue;
            List<QuestProgress> list = entry.getValue();
            synchronized (list) {
                list.removeIf(p -> {
                    QuestDefinition def = definitions.get(p.getQuestId());
                    return def != null && def.getType() == type;
                });
            }
        }
    }

    private String progressKey(int guildId, UUID playerUuid) {
        return guildId + "_" + playerUuid.toString();
    }

    public void loadAll() {
        File[] files = dataDir.listFiles((dir, name) ->
            name.endsWith("_progress.yml") && name.startsWith("guild_"));
        if (files == null) return;

        int totalLoaded = 0;
        int totalExpired = 0;
        synchronized (saveLock) {
            for (File file : files) {
                try {
                    int[] result = loadGuildFile(file);
                    totalExpired += result[0];
                    totalLoaded += result[1];
                } catch (Exception e) {
                    logger.warning("[Quest] 加载失败: " + file.getName() + " - " + e.getMessage());
                }
            }
        }
        if (totalExpired > 0) {
            logger.info("[Quest] 已清理 " + totalExpired + " 条过期任务进度");
        }
        logger.info("[Quest] 已加载 " + totalLoaded + " 条任务进度，来自 " + files.length + " 个公会文件");
    }

    private boolean isSameDay(long timestamp) {
        LocalDate tsDate = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp), ZONE).toLocalDate();
        return tsDate.equals(LocalDate.now());
    }

    private boolean isSameWeek(long timestamp) {
        LocalDate tsDate = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp), ZONE).toLocalDate();
        LocalDate monNow = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monTs = tsDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return monNow.equals(monTs);
    }

    private int[] loadGuildFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int guildId = yaml.getInt("guild-id", 0);

        if (guildId <= 0) {
            String name = file.getName();
            try {
                String idStr = name.replace("guild_", "").replace("_progress.yml", "");
                int parsedId = Integer.parseInt(idStr);
                if (parsedId > 0) {
                    logger.warning("[Quest] ⚠️ 文件 " + name + " 的 guild-id=" + guildId +
                        " 无效，从文件名提取 guildId=" + parsedId);
                    guildId = parsedId;
                }
            } catch (NumberFormatException ignored) {}
        }

        if (guildId <= 0) {
            logger.warning("[Quest] ⚠️ 跳过无效文件: " + file.getName() + " (guild-id=" + guildId + ")");
            return new int[]{0, 0};
        }

        List<?> list = yaml.getList("progress");
        if (list == null || list.isEmpty()) return new int[]{0, 0};

        int expiredCount = 0;
        int validCount = 0;

        for (Object obj : list) {
            if (!(obj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;

            String questId = (String) map.get("questId");
            String playerUuidStr = (String) map.get("playerUuid");
            String playerName = (String) map.get("playerName");
            if (questId == null || playerUuidStr == null) continue;

            UUID uuid;
            try { uuid = UUID.fromString(playerUuidStr); }
            catch (IllegalArgumentException e) { continue; }

            QuestDefinition def = definitions.get(questId);
            if (def == null) continue;

            int objCount = def.getObjectives().size();
            long acceptedTime = map.get("acceptedTime") instanceof Number ?
                ((Number) map.get("acceptedTime")).longValue() : System.currentTimeMillis();
            long completedTime = map.get("completedTime") instanceof Number ?
                ((Number) map.get("completedTime")).longValue() : 0;
            boolean claimed = Boolean.TRUE.equals(map.get("claimed"));
            long claimedTime = map.get("claimedTime") instanceof Number ?
                ((Number) map.get("claimedTime")).longValue() : 0;

            if (!claimed) {
                switch (def.getType()) {
                    case DAILY:
                        if (!isSameDay(acceptedTime)) { expiredCount++; continue; }
                        break;
                    case WEEKLY:
                        if (!isSameWeek(acceptedTime)) { expiredCount++; continue; }
                        break;
                    case ONE_TIME:
                        break;
                }
            } else {
                switch (def.getType()) {
                    case DAILY:
                        if (!isSameDay(claimedTime)) { expiredCount++; continue; }
                        break;
                    case WEEKLY:
                        if (!isSameWeek(claimedTime)) { expiredCount++; continue; }
                        break;
                    case ONE_TIME:
                        break;
                }
            }

            QuestProgress progress = new QuestProgress(questId, uuid, playerName, guildId, objCount);

            List<?> progList = (List<?>) map.get("objectiveProgress");
            if (progList != null) {
                int[] restored = new int[objCount];
                for (int i = 0; i < Math.min(progList.size(), objCount); i++) {
                    Object val = progList.get(i);
                    if (val instanceof Number) restored[i] = ((Number) val).intValue();
                }
                progress.setObjectiveProgress(restored);
            }

            progress.setAcceptedTime(acceptedTime);
            progress.setCompletedTime(completedTime);
            progress.setClaimed(claimed);
            progress.setClaimedTime(claimedTime);

            String key = progressKey(guildId, uuid);
            guildProgressMap.computeIfAbsent(key,
                k -> Collections.synchronizedList(new ArrayList<>())).add(progress);
            validCount++;
        }

        if (expiredCount > 0) {
            logger.info("[Quest] " + file.getName() + ": 过期 " + expiredCount + " 条，保留 " + validCount + " 条");
        }

        return new int[]{expiredCount, validCount};
    }

    public void saveAll() {
        Set<Integer> savedGuilds = new HashSet<>();
        for (String key : guildProgressMap.keySet()) {
            String[] parts = key.split("_", 2);
            try {
                int gid = Integer.parseInt(parts[0]);
                if (gid > 0) savedGuilds.add(gid);
                else logger.warning("[Quest] ⚠️ saveAll发现无效key: " + key);
            } catch (Exception ignored) {}
        }
        for (int guildId : savedGuilds) saveGuildProgress(guildId);
    }

    public void saveGuildProgress(int guildId) {
        synchronized (saveLock) {
            doSaveGuildProgress(guildId);
        }
    }

    private void doSaveGuildProgress(int guildId) {
        if (guildId <= 0) {
            logger.warning("[Quest] ⚠️ 拒绝保存: guildId=" + guildId + " 无效!");
            return;
        }

        File file = new File(dataDir, "guild_" + guildId + "_progress.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("guild-id", guildId);
        yaml.set("last-save", System.currentTimeMillis());

        List<Map<String, Object>> progressList = new ArrayList<>();
        String prefix = guildId + "_";
        int entryCount = 0;

        for (Map.Entry<String, List<QuestProgress>> entry : guildProgressMap.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) continue;
            List<QuestProgress> list = entry.getValue();
            synchronized (list) {
                for (QuestProgress p : list) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("questId", p.getQuestId());
                    map.put("playerUuid", p.getPlayerUuid().toString());
                    map.put("playerName", p.getPlayerName());

                    List<Integer> objProgress = new ArrayList<>();
                    for (int val : p.getObjectiveProgress()) objProgress.add(val);
                    map.put("objectiveProgress", objProgress);
                    map.put("acceptedTime", p.getAcceptedTime());
                    map.put("completedTime", p.getCompletedTime());
                    map.put("claimed", p.isClaimed());
                    map.put("claimedTime", p.getClaimedTime());
                    progressList.add(map);
                    entryCount++;
                }
            }
        }

        if (progressList.isEmpty()) {
            YamlConfiguration existing = YamlConfiguration.loadConfiguration(file);
            List<?> existingProgress = existing.getList("progress");
            if (existingProgress != null && !existingProgress.isEmpty()) {
                logger.warning("[Quest] ⚠️ 尝试保存空数据到 " + file.getName() +
                    " 但文件中有 " + existingProgress.size() + " 条记录，跳过覆盖！");
                logger.warning("[Quest]   map总条目数: " + guildProgressMap.size() +
                    ", 查找前缀: " + prefix);
                for (String key : guildProgressMap.keySet()) {
                    logger.warning("[Quest]   现有key: " + key);
                }
                return;
            }
        }

        yaml.set("progress", progressList);
        try {
            yaml.save(file);
            logger.fine("[Quest] 💾 保存 " + file.getName() + ": " + entryCount + " 条记录");
        } catch (IOException e) {
            logger.warning("[Quest] 保存失败: " + file.getName() + " - " + e.getMessage());
        }
    }

    public void clearAll() {
        guildProgressMap.clear();
    }
}
