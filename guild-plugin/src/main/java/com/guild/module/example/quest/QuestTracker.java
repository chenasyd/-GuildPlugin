package com.guild.module.example.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ScheduledTaskHandle;

import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestObjective;
import com.guild.module.example.quest.model.QuestProgress;

public class QuestTracker implements Listener {
    private final GuildQuestModule module;
    private final Logger logger;
    private boolean running = false;

    /**
     * Timer management map - key: "playerUuid_questId", value: ScheduledTaskHandle
     * Tracks all active online-hour tracking tasks to prevent duplicates and memory leaks
     */
    private final Map<String, ScheduledTaskHandle> activeTasks = new ConcurrentHashMap<>();

    public QuestTracker(GuildQuestModule module) {
        this.module = module;
        this.logger = module.getContext().getLogger();
    }

    public void start() {
        if (running) return;
        try {
            module.getContext().getPlugin().getServer().getPluginManager()
                .registerEvents(this, module.getContext().getPlugin());
            running = true;
            logger.info("[Quest-Tracker] Event listeners started");
        } catch (Exception e) {
            logger.warning("[Quest-Tracker] Failed to register event listeners: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;

        // Clean up all active timers to prevent memory leaks
        int taskCount = activeTasks.size();
        for (ScheduledTaskHandle task : activeTasks.values()) {
            if (task != null && !task.isCancelled()) {
                try {
                    task.cancel();
                } catch (Exception e) {
                    logger.warning("[Quest-Tracker] Error cancelling timer: " + e.getMessage());
                }
            }
        }
        activeTasks.clear();

        // Unregister event listeners
        EntityDeathEvent.getHandlerList().unregister(this);
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);

        if (taskCount > 0) {
            logger.info("[Quest-Tracker] Cleaned up " + taskCount + " active timer(s)");
        }
        logger.info("[Quest-Tracker] Event listeners stopped");
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) return;
        Player player = event.getEntity().getKiller();
        UUID uuid = player.getUniqueId();
        int guildId = getGuildId(uuid);
        if (guildId <= 0) return;
        EntityType entityType = event.getEntityType();
        boolean isHostile = isHostileMob(entityType);

        // One-pass get all quest types to reduce duplicate iteration
        List<QuestDefinition> allQuests = new ArrayList<>();
        allQuests.addAll(module.getQuestManager().getDefinitionsByType(QuestDefinition.QuestType.DAILY));
        allQuests.addAll(module.getQuestManager().getDefinitionsByType(QuestDefinition.QuestType.WEEKLY));
        allQuests.addAll(module.getQuestManager().getDefinitionsByType(QuestDefinition.QuestType.ONE_TIME));

        for (QuestDefinition def : allQuests) {
            for (int i = 0; i < def.getObjectives().size(); i++) {
                QuestObjective obj = def.getObjectives().get(i);
                if (obj.getType() == QuestObjective.ObjectiveType.KILL_MOBS) {
                    // Determine whether to update based on quest type and mob type
                    boolean shouldUpdate = false;
                    if (def.getType() == QuestDefinition.QuestType.DAILY) {
                        shouldUpdate = isHostile || entityType == EntityType.PLAYER;
                    } else {
                        shouldUpdate = isHostile;
                    }
                    
                    if (shouldUpdate) {
                        module.getQuestManager().updateAndSave(guildId, uuid, def.getId(), i, 1);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        int guildId = getGuildId(uuid);
        if (guildId <= 0) return;

        // Get player's accepted online-hour quests
        var questManager = module.getQuestManager();
        java.util.List<QuestProgress> activeQuests = questManager.getPlayerActiveQuests(guildId, uuid);

        int trackingCount = 0;
        for (QuestProgress progress : activeQuests) {
            QuestDefinition def = questManager.getDefinition(progress.getQuestId());
            if (def == null) {
                logger.warning("[Quest-Tracker] Quest definition not found: " + progress.getQuestId());
                continue;
            }

            // Only process quests with online-hour objectives
            if (!hasOnlineHourObjective(def)) continue;

            // Start online tracking (avoids duplicates internally)
            startOnlineTracking(player, guildId, def, progress);
            trackingCount++;
        }

        if (trackingCount > 0) {
    }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        int guildId = getGuildId(uuid);

        // Clean up all active timers for this player to prevent memory leaks
        cleanupPlayerTasks(uuid);

        if (guildId > 0) {
            module.getQuestManager().saveGuildProgress(guildId);
        }
    }

    /**
     * Start tracking timer for a player's online-hour quest
     */
    private void startOnlineTracking(Player player, int guildId, QuestDefinition def, QuestProgress progress) {
        String taskId = buildTaskKey(player.getUniqueId(), def.getId());

        // Avoid creating duplicate timers
        if (activeTasks.containsKey(taskId)) {
            return;
        }

        // Skip if quest is already completed
        if (def.isCompleted(progress.getObjectiveProgress())) {
            return;
        }

        UUID playerUuid = player.getUniqueId();
        String questId = def.getId();

        // Create and register timer - every 15 minutes
        ScheduledTaskHandle task = CompatibleScheduler.runTaskTimer(
            module.getContext().getPlugin(),
            () -> updateOnlineProgress(playerUuid, guildId, questId),
            1200L,   // 1-minute delay (60s / 20 ticks)
            18000L   // Every 15 minutes (900s / 20 ticks)
        );

        // Store in management map
        activeTasks.put(taskId, task);
    }

    /**
     * Update online hour progress
     */
    private void updateOnlineProgress(UUID playerUuid, int guildId, String questId) {
        // Check if player is still online
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            // Player offline, cancel tracking
            cancelTask(buildTaskKey(playerUuid, questId));
            return;
        }

        // Get latest quest progress
        QuestProgress progress = module.getQuestManager()
            .getPlayerQuest(guildId, playerUuid, questId);

        if (progress == null || progress.isClaimed()) {
            // Quest not found or already claimed, cancel tracking
            cancelTask(buildTaskKey(playerUuid, questId));
            return;
        }

        QuestDefinition def = module.getQuestManager().getDefinition(questId);
        if (def == null) {
            cancelTask(buildTaskKey(playerUuid, questId));
            return;
        }

        // Check if quest is completed
        if (def.isCompleted(progress.getObjectiveProgress())) {
            // Mark as completed and cancel tracking
            progress.markAsCompleted();
            module.getQuestManager().saveGuildProgress(guildId);
            cancelTask(buildTaskKey(playerUuid, questId));
            return;
        }

        // Update all online-hour objective progress
        for (int i = 0; i < def.getObjectives().size(); i++) {
            QuestObjective obj = def.getObjectives().get(i);
            if (obj.getType() == QuestObjective.ObjectiveType.ONLINE_HOURS) {
                // Update every 15 minutes, +15 each tick
                module.getQuestManager().updateAndSave(guildId, playerUuid, questId, i, 15);
            }
        }
    }

    /**
     * Cancel a specific scheduled task
     */
    private void cancelTask(String taskId) {
        ScheduledTaskHandle task = activeTasks.remove(taskId);
        if (task != null && !task.isCancelled()) {
            try {
                task.cancel();
            } catch (Exception e) {
                logger.warning("[Quest-Tracker] Error cancelling task: " + taskId + " - " + e.getMessage());
            }
        }
    }

    /**
     * Clean up all active timers for a player
     */
    private void cleanupPlayerTasks(UUID playerUuid) {
        String prefix = playerUuid.toString() + "_";
        final int[] cleanedCount = {0};

        // Use iterator to safely remove
        activeTasks.keySet().removeIf(key -> {
            if (key.startsWith(prefix)) {
                ScheduledTaskHandle task = activeTasks.get(key);
                if (task != null && !task.isCancelled()) {
                    try {
                        task.cancel();
                        cleanedCount[0]++;
                    } catch (Exception e) {
                        logger.warning("[Quest-Tracker] Error cleaning up task: " + key + " - " + e.getMessage());
                    }
                }
                return true;
            }
            return false;
        });

    }

    /**
     * Build unique task identifier key
     */
    private String buildTaskKey(UUID playerUuid, String questId) {
        return playerUuid.toString() + "_" + questId;
    }

    /**
     * Count active tracking tasks for a player
     */
    private int countPlayerActiveTasks(UUID playerUuid) {
        String prefix = playerUuid.toString() + "_";
        int count = 0;
        for (String key : activeTasks.keySet()) {
            if (key.startsWith(prefix)) count++;
        }
        return count;
    }

    /**
     * Check if a quest definition contains an online-hour objective
     */
    private boolean hasOnlineHourObjective(QuestDefinition def) {
        for (QuestObjective obj : def.getObjectives()) {
            if (obj.getType() == QuestObjective.ObjectiveType.ONLINE_HOURS) {
                return true;
            }
        }
        return false;
    }

    private int getGuildId(UUID uuid) {
        var guild = module.getContext().getPlugin().getGuildService().getPlayerGuild(uuid);
        return guild != null ? guild.getId() : 0;
    }

    private static boolean isHostileMob(EntityType type) {
        return switch (type) {
            case ZOMBIE, SKELETON, SPIDER, CREEPER, ENDERMAN, WITCH,
                 BLAZE, GHAST, MAGMA_CUBE, WITHER_SKELETON, PILLAGER,
                 RAVAGER, HOGLIN, ZOGLIN, GUARDIAN, ELDER_GUARDIAN -> true;
            default -> false;
        };
    }

    /**
     * Get the current number of active timers (for monitoring and debugging)
     */
    public int getActiveTaskCount() {
        return activeTasks.size();
    }

    /**
     * Handle player deposit to guild event.
     * Used to update DEPOSIT_MONEY type objective progress.
     */
    public void onPlayerDepositMoney(UUID playerUuid, double amount) {
        if (amount <= 0) return;
        
        int guildId = getGuildId(playerUuid);
        if (guildId <= 0) return;
        
        var questManager = module.getQuestManager();
        
        // One-pass get all quest types to reduce duplicate iteration
        List<QuestDefinition> allQuests = new ArrayList<>();
        for (QuestDefinition.QuestType questType : QuestDefinition.QuestType.values()) {
            allQuests.addAll(questManager.getDefinitionsByType(questType));
        }
        
        int depositAmount = (int) amount;
        
        for (QuestDefinition def : allQuests) {
            for (int i = 0; i < def.getObjectives().size(); i++) {
                QuestObjective obj = def.getObjectives().get(i);
                if (obj.getType() == QuestObjective.ObjectiveType.DEPOSIT_MONEY) {
                    questManager.updateAndSave(guildId, playerUuid, def.getId(), i, depositAmount);
                }
            }
        }
    }
}