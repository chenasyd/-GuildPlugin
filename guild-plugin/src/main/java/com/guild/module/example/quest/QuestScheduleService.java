package com.guild.module.example.quest;

import com.guild.core.module.ModuleContext;

/** 日/周重置与进度持久化定时任务。 */
final class QuestScheduleService {

    private final GuildQuestModule module;
    private final QuestManager questManager;

    QuestScheduleService(GuildQuestModule module, QuestManager questManager) {
        this.module = module;
        this.questManager = questManager;
    }

    void start(ModuleContext context) {
        context.runTimer(1200L, 600L, () ->
                context.getApi().getAllGuilds().thenAcceptAsync(guilds -> {
                    for (var guild : guilds) {
                        questManager.saveGuildProgress(guild.getId());
                    }
                }));
        int resetHour = context.getConfig().getInt("settings.quest-reset-hour", 4);
        long resetDelayTicks = calculateSecondsUntil(resetHour) * 20L;
        context.runTimer(Math.max(1200L, resetDelayTicks), 172800000L, () ->
                context.getApi().getAllGuilds().thenAcceptAsync(guilds ->
                        com.guild.core.utils.CompatibleScheduler.runTask(context.getPlugin(), () -> {
                            for (var guild : guilds) {
                                questManager.resetDailyQuests(guild.getId());
                                notifyQuestReset(context, guild.getId(), "daily");
                            }
                        })));
        long weeklyDelayTicks = calculateSecondsUntilWeekly(resetHour) * 20L;
        context.runTimer(Math.max(2400L, weeklyDelayTicks), 604800000L, () ->
                context.getApi().getAllGuilds().thenAcceptAsync(guilds ->
                        com.guild.core.utils.CompatibleScheduler.runTask(context.getPlugin(), () -> {
                            for (var guild : guilds) {
                                questManager.resetWeeklyQuests(guild.getId());
                                notifyQuestReset(context, guild.getId(), "weekly");
                            }
                        })));
    }

    private static void notifyQuestReset(ModuleContext context, int guildId, String resetType) {
        java.util.Map<String, Object> refreshData = new java.util.HashMap<>();
        refreshData.put("guildId", guildId);
        refreshData.put("resetType", resetType);
        context.notifyGUIRefresh("quest-list", refreshData);
        context.notifyGUIRefresh("quest-active-list", refreshData);
    }

    private static long calculateSecondsUntil(int targetHour) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int currentMinute = cal.get(java.util.Calendar.MINUTE);
        int currentSecond = cal.get(java.util.Calendar.SECOND);
        int diffSeconds = (targetHour - currentHour) * 3600 - currentMinute * 60 - currentSecond;
        if (diffSeconds <= 0) {
            diffSeconds += 86400;
        }
        return diffSeconds;
    }

    private static long calculateSecondsUntilWeekly(int targetHour) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
        int daysUntilMonday = (java.util.Calendar.MONDAY - dayOfWeek + 7) % 7;
        if (daysUntilMonday == 0) {
            int currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            if (currentHour >= targetHour) {
                daysUntilMonday = 7;
            }
        }
        long secondsUntilTarget = calculateSecondsUntil(targetHour);
        return daysUntilMonday * 86400L + secondsUntilTarget;
    }
}
