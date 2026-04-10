package com.guild.module.example.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ActivityDataPersistence {

    private final File dataFile;
    private final Logger logger;

    private final Gson gson;
    private LocalDate currentDate = LocalDate.now();
    private final Map<UUID, PlayerDailyData> playerDataMap = new ConcurrentHashMap<>();

    private static final int RETENTION_DAYS = 30;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ActivityDataPersistence(File dataDir, Logger logger) {
        this.logger = logger;
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.dataFile = new File(dataDir, "activity-data.json");

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

    public void load() {
        if (!dataFile.exists()) {
            logger.info("[Stats-持久化] 数据文件不存在，将创建新文件");
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, PlayerDailyData>>(){}.getType();
            Map<String, PlayerDailyData> loadedData = gson.fromJson(reader, type);

            if (loadedData != null) {
                for (Map.Entry<String, PlayerDailyData> entry : loadedData.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        playerDataMap.put(uuid, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        logger.warning("[Stats-持久化] 无效的UUID: " + entry.getKey());
                    }
                }

                // 清理过期数据
                cleanupOldData();

                // 设置当前日期为最新记录的日期（或今天）
                determineCurrentDate(loadedData);

                logger.info(String.format(
                    "[Stats-持久化] 已加载 %d 个玩家的活跃度数据 (当前日期: %s)",
                    playerDataMap.size(), currentDate));
            }
        } catch (IOException e) {
            logger.severe("[Stats-持久化] 读取数据失败: " + e.getMessage());
        }
    }

    public void save() {
        // 先刷新所有当前在线玩家的数据（由调用者负责）
        
        Map<String, PlayerDailyData> saveData = new HashMap<>();
        for (Map.Entry<UUID, PlayerDailyData> entry : playerDataMap.entrySet()) {
            saveData.put(entry.getKey().toString(), entry.getValue());
        }

        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(saveData, writer);
        } catch (IOException e) {
            logger.severe("[Stats-持久化] 保存数据失败: " + e.getMessage());
        }
    }

    public void recordOnlineMinutes(UUID uuid, long minutes) {
        String dateKey = currentDate.format(DATE_FORMATTER);
        PlayerDailyData data = playerDataMap.computeIfAbsent(uuid,
            k -> new PlayerDailyData());

        long currentMinutes = data.getDailyMinutes(dateKey);
        data.setDailyMinutes(dateKey, currentMinutes + minutes);

        // 更新本周活跃天数
        updateActiveDaysThisWeek(data);
    }

    public long getOnlineMinutesToday(UUID uuid) {
        PlayerDailyData data = playerDataMap.get(uuid);
        if (data == null) return 0L;

        String dateKey = currentDate.format(DATE_FORMATTER);
        return data.getDailyMinutes(dateKey);
    }

    public int getActiveDaysThisWeek(UUID uuid) {
        PlayerDailyData data = playerDataMap.get(uuid);
        if (data == null) return 0;
        return data.getActiveDaysThisWeek();
    }

    public boolean isNewDay() {
        return !LocalDate.now().equals(currentDate);
    }

    public void switchToNewDay() {
        LocalDate today = LocalDate.now();
        if (today.equals(currentDate)) return;

        LocalDate oldDate = currentDate;
        currentDate = today;

        logger.info(String.format(
            "[Stats-持久化] 日期切换: %s -> %s",
            oldDate, currentDate));

        // 重置所有玩家的本周活跃计数（新的一周开始时）
        if (isNewWeek(oldDate, today)) {
            resetWeeklyCounts();
        } else {
            // 同一周内跨天：更新昨天活跃的玩家的本周活跃天数
            String yesterdayKey = oldDate.format(DATE_FORMATTER);
            for (PlayerDailyData data : playerDataMap.values()) {
                long yesterdayMinutes = data.getDailyMinutes(yesterdayKey);
                if (yesterdayMinutes >= 5) {
                    int currentDays = data.getActiveDaysThisWeek();
                    data.setActiveDaysThisWeek(Math.min(currentDays + 1, 7));
                }
                data.setActiveToday(false); // 重置"今日已活跃"，让新的一天重新检测
            }
            logger.info("[Stats-持久化] 已更新同周内跨天活跃天数");
        }

        // 保存数据（包含旧日期的最后统计）
        save();

        // 清理过期数据
        cleanupOldData();
    }

    public void removePlayer(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public void clearAll() {
        playerDataMap.clear();
        save(); // 保存空数据
    }

    private void updateActiveDaysThisWeek(PlayerDailyData data) {
        String todayKey = currentDate.format(DATE_FORMATTER);
        long todayMinutes = data.getDailyMinutes(todayKey);

        if (todayMinutes >= 5 && !data.hasActiveToday()) {
            data.setActiveToday(true);
            int currentDays = data.getActiveDaysThisWeek();
            data.setActiveDaysThisWeek(Math.min(currentDays + 1, 7));
        }
    }

    private boolean isNewWeek(LocalDate oldDate, LocalDate newDate) {
        // 简单判断：如果新旧日期不在同一周内
        int oldWeekOfYear = oldDate.getDayOfWeek().getValue();
        int newWeekOfYear = newDate.getDayOfWeek().getValue();

        // 如果跨越了周日（第7天）到周一（第1天），或者日期差超过7天
        if (newWeekOfYear < oldWeekOfYear) return true; // 从周日到周一
        if (newDate.toEpochDay() - oldDate.toEpochDay() >= 7) return true;
        return false;
    }

    private void resetWeeklyCounts() {
        for (PlayerDailyData data : playerDataMap.values()) {
            data.setActiveDaysThisWeek(0);
            data.setActiveToday(false);
        }
        logger.info("[Stats-持久化] 已重置所有玩家的周活跃计数");
    }

    private void cleanupOldData() {
        LocalDate cutoffDate = LocalDate.now().minusDays(RETENTION_DAYS);
        String cutoffKey = cutoffDate.format(DATE_FORMATTER);

        int removedCount = 0;
        var iterator = playerDataMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            PlayerDailyData data = entry.getValue();

            // 移除早于截止日期的所有每日记录
            data.cleanupOlderThan(cutoffKey);

            // 如果该玩家没有任何近期数据，移除整个记录
            if (data.getDailyMinutes().isEmpty()) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            logger.info(String.format(
                "[Stats-持久化] 清理了 %d 个无近期数据的玩家记录", removedCount));
        }
    }

    private void determineCurrentDate(Map<String, PlayerDailyData> loadedData) {
        // 找出最新的有数据的日期
        LocalDate latestDate = LocalDate.MIN;

        for (PlayerDailyData data : loadedData.values()) {
            for (String dateKey : data.getDailyMinutes().keySet()) {
                try {
                    LocalDate date = LocalDate.parse(dateKey, DATE_FORMATTER);
                    if (date.isAfter(latestDate)) {
                        latestDate = date;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (!latestDate.equals(LocalDate.MIN) && !latestDate.isAfter(LocalDate.now())) {
            currentDate = latestDate;
        } else {
            currentDate = LocalDate.now();
        }
    }

    /**
     * 玩家每日数据模型
     */
    public static class PlayerDailyData {
        private Map<String, Long> dailyMinutes = new HashMap<>();
        private int activeDaysThisWeek = 0;
        private boolean activeToday = false;

        public Map<String, Long> getDailyMinutes() { return dailyMinutes; }
        public void setDailyMinutes(Map<String, Long> dailyMinutes) { this.dailyMinutes = dailyMinutes; }

        public long getDailyMinutes(String dateKey) {
            return dailyMinutes.getOrDefault(dateKey, 0L);
        }

        public void setDailyMinutes(String dateKey, long minutes) {
            dailyMinutes.put(dateKey, minutes);
        }

        public int getActiveDaysThisWeek() { return activeDaysThisWeek; }
        public void setActiveDaysThisWeek(int days) { this.activeDaysThisWeek = days; }

        public boolean hasActiveToday() { return activeToday; }
        public void setActiveToday(boolean active) { this.activeToday = active; }

        public void cleanupOlderThan(String cutoffKey) {
            dailyMinutes.entrySet().removeIf(entry ->
                entry.getKey().compareTo(cutoffKey) < 0
            );
        }
    }
}
