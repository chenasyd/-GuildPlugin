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
import org.bukkit.scheduler.BukkitTask;

import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestObjective;
import com.guild.module.example.quest.model.QuestProgress;

public class QuestTracker implements Listener {
    private final GuildQuestModule module;
    private final Logger logger;
    private boolean running = false;

    /**
     * 定时器管理Map - key: "playerUuid_questId", value: BukkitTask
     * 用于跟踪所有活跃的在线时长追踪任务，防止重复创建和内存泄漏
     */
    private final Map<String, BukkitTask> activeTasks = new ConcurrentHashMap<>();

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
            logger.info("[Quest-Tracker] 事件监听已启动");
            logger.info("[Quest-Tracker] 定时器管理已初始化 (当前活跃任务: " + activeTasks.size() + ")");
        } catch (Exception e) {
            logger.warning("[Quest-Tracker] 注册事件监听失败: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;

        // 清理所有活跃的定时器 - 防止内存泄漏
        int taskCount = activeTasks.size();
        for (BukkitTask task : activeTasks.values()) {
            if (task != null && !task.isCancelled()) {
                try {
                    task.cancel();
                } catch (Exception e) {
                    logger.warning("[Quest-Tracker] 取消定时器异常: " + e.getMessage());
                }
            }
        }
        activeTasks.clear();

        // 取消事件监听
        EntityDeathEvent.getHandlerList().unregister(this);
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);

        if (taskCount > 0) {
            logger.info("[Quest-Tracker] 已清理 " + taskCount + " 个活跃定时器");
        }
        logger.info("[Quest-Tracker] 事件监听已停止");
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

        // 优化：一次性获取所有任务类型，减少重复遍历
        List<QuestDefinition> allQuests = new ArrayList<>();
        allQuests.addAll(module.getQuestManager().getDefinitionsByType(QuestDefinition.QuestType.DAILY));
        allQuests.addAll(module.getQuestManager().getDefinitionsByType(QuestDefinition.QuestType.WEEKLY));
        allQuests.addAll(module.getQuestManager().getDefinitionsByType(QuestDefinition.QuestType.ONE_TIME));

        for (QuestDefinition def : allQuests) {
            for (int i = 0; i < def.getObjectives().size(); i++) {
                QuestObjective obj = def.getObjectives().get(i);
                if (obj.getType() == QuestObjective.ObjectiveType.KILL_MOBS) {
                    // 根据任务类型和怪物类型判断是否更新
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

        // 获取玩家已接取的在线时长任务
        var questManager = module.getQuestManager();
        java.util.List<QuestProgress> activeQuests = questManager.getPlayerActiveQuests(guildId, uuid);

        logger.info("[Quest-Tracker] 玩家 " + player.getName() + 
            " 登录，检查 " + activeQuests.size() + " 个活跃任务");

        int trackingCount = 0;
        for (QuestProgress progress : activeQuests) {
            QuestDefinition def = questManager.getDefinition(progress.getQuestId());
            if (def == null) {
                logger.warning("[Quest-Tracker] 未找到任务定义: " + progress.getQuestId());
                continue;
            }

            // 只处理包含在线时长目标的任务
            if (!hasOnlineHourObjective(def)) continue;

            // 启动在线时长追踪（内部会避免重复创建）
            startOnlineTracking(player, guildId, def, progress);
            trackingCount++;
        }

        if (trackingCount > 0) {
            logger.info("[Quest-Tracker] 玩家 " + player.getName() + 
                " 已启动 " + trackingCount + " 个在线时长追踪任务");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        int guildId = getGuildId(uuid);

        // 清理该玩家的所有活跃定时器 - 防止内存泄漏
        cleanupPlayerTasks(uuid);

        if (guildId > 0) {
            module.getQuestManager().saveGuildProgress(guildId);
        }

        logger.fine("[Quest-Tracker] 玩家 " + player.getName() + " 已退出，已清理相关定时器");
    }

    /**
     * 为指定玩家的在线时长任务启动追踪定时器
     *
     * @param player   目标玩家
     * @param guildId  公会ID
     * @param def      任务定义
     * @param progress 任务进度
     */
    private void startOnlineTracking(Player player, int guildId, QuestDefinition def, QuestProgress progress) {
        String taskId = buildTaskKey(player.getUniqueId(), def.getId());

        logger.info("[Quest-Tracker] 准备启动在线追踪: " + taskId + 
            " (任务: " + def.getName() + ")");

        // 避免重复创建定时器 - 如果已存在则跳过
        if (activeTasks.containsKey(taskId)) {
            logger.info("[Quest-Tracker] 任务追踪已存在: " + taskId + "，跳过重复创建");
            return;
        }

        // 检查任务是否已完成
        if (def.isCompleted(progress.getObjectiveProgress())) {
            logger.info("[Quest-Tracker] 任务已完成: " + def.getId() + "，无需追踪");
            return;
        }

        UUID playerUuid = player.getUniqueId();
        String questId = def.getId();

        logger.info("[Quest-Tracker] 创建定时器: " + taskId + 
            " (延迟: 1分钟, 间隔: 15分钟)");

        // 创建并注册定时器 - 改为每15分钟执行一次，提高精度
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
            module.getContext().getPlugin(),
            () -> updateOnlineProgress(playerUuid, guildId, questId),
            1200L,  // 延迟1分钟（60秒 / 20 ticks）
            18000L  // 每15分钟执行一次（900秒 / 20 ticks）
        );

        // 存储到管理Map中
        activeTasks.put(taskId, task);

        logger.info("[Quest-Tracker] ✅ 已成功启动在线追踪: " + taskId +
            " (总活跃任务: " + activeTasks.size() + ")");
    }

    /**
     * 更新在线时长进度
     */
    private void updateOnlineProgress(UUID playerUuid, int guildId, String questId) {
        // 检查玩家是否仍在线
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            // 玩家离线，取消该任务追踪
            cancelTask(buildTaskKey(playerUuid, questId));
            return;
        }

        // 获取最新的任务进度
        QuestProgress progress = module.getQuestManager()
            .getPlayerQuest(guildId, playerUuid, questId);

        if (progress == null || progress.isClaimed()) {
            // 任务不存在或已领取，取消追踪
            cancelTask(buildTaskKey(playerUuid, questId));
            return;
        }

        QuestDefinition def = module.getQuestManager().getDefinition(questId);
        if (def == null) {
            cancelTask(buildTaskKey(playerUuid, questId));
            return;
        }

        // 检查任务是否已完成
        if (def.isCompleted(progress.getObjectiveProgress())) {
            // 标记为已完成并取消追踪
            progress.markAsCompleted();
            module.getQuestManager().saveGuildProgress(guildId);
            cancelTask(buildTaskKey(playerUuid, questId));
            
            logger.info("[Quest-Tracker] 玩家 " + player.getName() + 
                " 完成在线时长任务: " + def.getName());
            return;
        }

        // 更新所有在线时长目标的进度
        for (int i = 0; i < def.getObjectives().size(); i++) {
            QuestObjective obj = def.getObjectives().get(i);
            if (obj.getType() == QuestObjective.ObjectiveType.ONLINE_HOURS) {
                // 每15分钟更新一次，每次加15分钟
                module.getQuestManager().updateAndSave(guildId, playerUuid, questId, i, 15);
                
                logger.fine("[Quest-Tracker] 更新进度: " + player.getName() + 
                    " -> " + def.getName() + " [" + i + "] +15分钟");
            }
        }
    }

    /**
     * 取消指定的定时器任务
     *
     * @param taskId 任务ID ("uuid_questId")
     */
    private void cancelTask(String taskId) {
        BukkitTask task = activeTasks.remove(taskId);
        if (task != null && !task.isCancelled()) {
            try {
                task.cancel();
                logger.fine("[Quest-Tracker] 已取消任务追踪: " + taskId);
            } catch (Exception e) {
                logger.warning("[Quest-Tracker] 取消任务异常: " + taskId + " - " + e.getMessage());
            }
        }
    }

    /**
     * 清理指定玩家的所有活跃定时器
     *
     * @param playerUuid 玩家UUID
     */
    private void cleanupPlayerTasks(UUID playerUuid) {
        String prefix = playerUuid.toString() + "_";
        final int[] cleanedCount = {0};

        // 使用迭代器安全删除
        activeTasks.keySet().removeIf(key -> {
            if (key.startsWith(prefix)) {
                BukkitTask task = activeTasks.get(key);
                if (task != null && !task.isCancelled()) {
                    try {
                        task.cancel();
                        cleanedCount[0]++;
                    } catch (Exception e) {
                        logger.warning("[Quest-Tracker] 清理任务异常: " + key + " - " + e.getMessage());
                    }
                }
                return true;
            }
            return false;
        });

        if (cleanedCount[0] > 0) {
            logger.info("[Quest-Tracker] 已清理玩家 " + cleanedCount[0] + " 个活跃定时器");
        }
    }

    /**
     * 构建任务唯一标识键
     */
    private String buildTaskKey(UUID playerUuid, String questId) {
        return playerUuid.toString() + "_" + questId;
    }

    /**
     * 统计指定玩家的活跃追踪任务数量
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
     * 检查任务定义是否包含在线时长目标
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
     * 获取当前活跃定时器数量（用于监控和调试）
     */
    public int getActiveTaskCount() {
        return activeTasks.size();
    }

    /**
     * 处理玩家存钱到公会的事件
     * 用于更新DEPOSIT_MONEY类型的目标进度
     *
     * @param playerUuid 玩家UUID
     * @param amount      存入金额
     */
    public void onPlayerDepositMoney(UUID playerUuid, double amount) {
        if (amount <= 0) return;
        
        int guildId = getGuildId(playerUuid);
        if (guildId <= 0) return;
        
        var questManager = module.getQuestManager();
        
        // 优化：一次性获取所有任务，减少重复遍历
        List<QuestDefinition> allQuests = new ArrayList<>();
        for (QuestDefinition.QuestType questType : QuestDefinition.QuestType.values()) {
            allQuests.addAll(questManager.getDefinitionsByType(questType));
        }
        
        int depositAmount = (int) amount;
        Player player = Bukkit.getPlayer(playerUuid);
        String playerName = player != null ? player.getName() : playerUuid.toString();
        
        for (QuestDefinition def : allQuests) {
            for (int i = 0; i < def.getObjectives().size(); i++) {
                QuestObjective obj = def.getObjectives().get(i);
                if (obj.getType() == QuestObjective.ObjectiveType.DEPOSIT_MONEY) {
                    // 更新进度
                    questManager.updateAndSave(guildId, playerUuid, def.getId(), i, depositAmount);
                    
                    logger.info("[Quest-Tracker] " + playerName + 
                        " 存入 $" + String.format("%.0f", amount) + 
                        " -> 任务: " + def.getName());
                }
            }
        }
    }
}