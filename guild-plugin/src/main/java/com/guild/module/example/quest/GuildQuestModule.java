package com.guild.module.example.quest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.utils.ColorUtils;
import com.guild.module.example.quest.gui.ActiveQuestsGUI;
import com.guild.module.example.quest.gui.QuestDetailGUI;
import com.guild.module.example.quest.gui.QuestListGUI;
import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestObjective;
import com.guild.module.example.quest.model.QuestProgress;
import com.guild.module.example.quest.model.QuestReward;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.economy.CurrencyManager;
import com.guild.sdk.event.EconomyEventData;
import com.guild.sdk.event.EconomyEventHandler;
import com.guild.sdk.event.GuildEventData;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventData;
import com.guild.sdk.event.MemberEventHandler;

public class GuildQuestModule implements GuildModule {
    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;

    private QuestManager questManager;
    private QuestTracker questTracker;
    private QuestRewardHandler rewardHandler;

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;
        File dataDir = new File(context.getPlugin().getDataFolder(), "modules/quest");

        this.questManager = new QuestManager(dataDir, context.getLogger());
        questManager.setContext(context);
        registerDefaultQuests();
        questManager.loadAll();

        this.rewardHandler = new QuestRewardHandler(context);
        this.questTracker = new QuestTracker(this);

        GuildPluginAPI api = context.getApi();
        registerGUIButtons(api);
        registerCommands(api);
        registerEventHandlers(api);
        startScheduledTasks();

        api.registerCustomGUI("quest-detail", (player, data) -> {
            QuestDefinition def = (QuestDefinition) data.get("definition");
            int guildId = toInt(data.get("guildId"), 0);
            UUID playerUuid = (UUID) data.get("playerUuid");
            
            if (guildId <= 0) {
                context.getLogger().warning("[Quest-Factory] ⚠️ guildId无效: " +
                    data.get("guildId") + " (type=" + (data.get("guildId") != null ?
                    data.get("guildId").getClass().getSimpleName() : "null") +
                    "), player=" + player.getName());
            }
            
            try {
                QuestProgress progress = null;
                if (def != null && guildId > 0 && playerUuid != null) {
                    progress = questManager.getPlayerQuest(guildId, playerUuid, def.getId());
                }
                
                return new QuestDetailGUI.Builder(this)
                    .fromDefinition(def)
                    .fromProgress(progress)
                    .withGuildInfo(guildId, playerUuid)
                    .build();
                
            } catch (Exception e) {
                context.getLogger().severe("[Quest-Factory] 创建QuestDetailGUI失败: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    context.getLogger().severe("    at " + element.toString());
                }
                try {
                    return new QuestDetailGUI.Builder(this)
                        .withGuildInfo(guildId != 0 ? guildId : -1, 
                            playerUuid != null ? playerUuid : new UUID(0, 0))
                        .build();
                } catch (Exception fallbackEx) {
                    throw new RuntimeException("无法创建任何GUI", fallbackEx);
                }
            }
        });

        api.registerCustomGUI("quest-active-list", (player, data) -> {
            int guildId = toInt(data.get("guildId"), 0);
            UUID playerUuid = player.getUniqueId();
            List<QuestProgress> active = questManager.getPlayerActiveQuests(guildId, playerUuid);
            return new ActiveQuestsGUI(this, active, guildId, playerUuid);
        });

        context.runLater(100L, () -> {
            questTracker.start();
            context.getLogger().info(ColorUtils.colorize(
                context.getMessage("module.quest.loaded",
                    "&a[任务模块] 公会任务系统已启用 (" + questManager.getDefinitions().size() + " 个任务)")));
        });

        context.getEventBus().subscribe(QuestCompletedEvent.class, event ->
            context.getLogger().info(String.format("[Quest-Event] %s 完成任务: %s (公会#%d)",
                event.playerName, event.questName, event.guildId)));
    }

    private void registerDefaultQuests() {
        // 每日任务 1: 每日狩猎
        QuestDefinition daily1 = new QuestDefinition(
            "daily_hunter", 
            context.getMessage("module.quest.daily_hunter.name", "每日狩猎"), 
            context.getMessage("module.quest.daily_hunter.description", "击杀指定数量的怪物"),
            QuestDefinition.QuestType.DAILY, 1, 1, true);
        daily1.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
            15, context.getMessage("module.quest.daily_hunter.objective", "击杀 15 只敌对生物")));
        daily1.addReward(new QuestReward(QuestReward.RewardType.CONTRIBUTION, 30));
        daily1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 50));
        questManager.registerDefinition(daily1);

        // 每日任务 2: 每日坚守
        QuestDefinition daily2 = new QuestDefinition(
            "daily_online", 
            context.getMessage("module.quest.daily_online.name", "每日坚守"), 
            context.getMessage("module.quest.daily_online.description", "保持在线一定时间"),
            QuestDefinition.QuestType.DAILY, 2, 1, true);
        daily2.addObjective(new QuestObjective(QuestObjective.ObjectiveType.ONLINE_HOURS,
            60, context.getMessage("module.quest.daily_online.objective", "在线 60 分钟")));
        daily2.addReward(new QuestReward(QuestReward.RewardType.CONTRIBUTION, 20));
        daily2.addReward(new QuestReward(QuestReward.RewardType.EXP, 500));
        questManager.registerDefinition(daily2);

        // 每周任务 1: 每周贡献者
        QuestDefinition weekly1 = new QuestDefinition(
            "weekly_contributor", 
            context.getMessage("module.quest.weekly_contributor.name", "每周贡献者"), 
            context.getMessage("module.quest.weekly_contributor.description", "为公会做出大量贡献"),
            QuestDefinition.QuestType.WEEKLY, 1, 2, true);
        weekly1.addObjective(new QuestObjective(
            QuestObjective.ObjectiveType.DEPOSIT_MONEY, 2000, 
            context.getMessage("module.quest.weekly_contributor.objective", "存入 $2000 到公会资金")));
        weekly1.addReward(new QuestReward(QuestReward.RewardType.CONTRIBUTION, 100));
        weekly1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 300));
        questManager.registerDefinition(weekly1);

        // 每周任务 2: 每周猎手
        QuestDefinition weekly2 = new QuestDefinition(
            "weekly_slayer", 
            context.getMessage("module.quest.weekly_slayer.name", "每周猎手"), 
            context.getMessage("module.quest.weekly_slayer.description", "大量击杀怪物证明实力"),
            QuestDefinition.QuestType.WEEKLY, 2, 3, true);
        weekly2.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
            100, context.getMessage("module.quest.weekly_slayer.objective", "击杀 100 只敌对生物")));
        weekly2.addReward(new QuestReward(QuestReward.RewardType.CONTRIBUTION, 80));
        weekly2.addReward(new QuestReward(QuestReward.RewardType.EXP, 2000));
        questManager.registerDefinition(weekly2);

        // 一次性任务: 初试锋芒
        QuestDefinition oneTime1 = new QuestDefinition(
            "onetime_first_blood", 
            context.getMessage("module.quest.onetime_first_blood.name", "初试锋芒"), 
            context.getMessage("module.quest.onetime_first_blood.description", "完成你的第一个任务"),
            QuestDefinition.QuestType.ONE_TIME, 1, 1, false);
        oneTime1.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
            5, context.getMessage("module.quest.onetime_first_blood.objective", "击杀 5 只怪物")));
        oneTime1.addReward(new QuestReward(QuestReward.RewardType.CONTRIBUTION, 25));
        oneTime1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 100));
        questManager.registerDefinition(oneTime1);
    }

    private void registerGUIButtons(GuildPluginAPI api) {
        ItemStack questButton = createItem(Material.BOOK,
            "&6&l" + context.getMessage("module.quest.button-name", "公会任务"),
            "&7" + context.getMessage("module.quest.quest-list-lore", "接取和追踪公会任务"),
            "&7" + context.getMessage("module.quest.quest-types", "每日 &8| &7每周 &8| &7一次性"),
            "",
            "&e" + context.getMessage("module.quest.click-open", "点击打开任务面板"));
        api.registerGUIButton("GuildInfoGUI", 14, questButton, "guild-quest",
            (player, ctx) -> openQuestList(player, ctx));

        ItemStack activeButton = createItem(Material.COMPASS,
            "&a&l" + context.getMessage("module.quest.active-quests", "进行中任务"),
            "&7" + context.getMessage("module.quest.active-quests-lore", "查看你当前正在进行的任务进度"));
        api.registerGUIButton("GuildInfoGUI", GUIExtensionHook.AUTO_SLOT,
            activeButton, "guild-quest",
            (player, ctx) -> openActiveQuests(player));
    }

    private void registerCommands(GuildPluginAPI api) {
        api.registerSubCommand("guild", "quest", (sender, args) -> handleQuestCommand(sender, args), "guild.quest");
        api.registerSubCommand("guild", "tasks", (sender, args) -> handleQuestCommand(sender, args), "guild.quest");
        api.registerSubCommand("guild", "currencies", (sender, args) -> handleCurrenciesCommand(sender, args), "guild.quest");
        api.registerSubCommand("guild", "currency", (sender, args) -> handleCurrenciesCommand(sender, args), "guild.quest");
    }

    private void handleQuestCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            if (player.hasPermission("guild.quest.admin.reset")) {
                var guild = context.getPlugin().getGuildService()
                    .getPlayerGuild(player.getUniqueId());
                if (guild != null) {
                    questManager.resetDailyQuests(guild.getId());
                    context.sendMessage(player, "module.quest.reset-done", context.getMessage("module.quest.reset-done", "&a[Quest] 每日任务已重置"));
                }
            } else {
                context.sendMessage(player, "module.quest.no-permission", context.getMessage("module.quest.no-permission", "&c权限不足"));
            }
        } else {
            openQuestList(player);
        }
    }

    private void handleCurrenciesCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        
        var guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            context.sendMessage(player, "module.quest.error.no-guild", context.getMessage("module.quest.error.no-guild", "&c你不在任何公会中"));
            return;
        }
        
        int guildId = guild.getId();
        UUID playerUuid = player.getUniqueId();
        
        // 构建货币信息消息
        StringBuilder message = new StringBuilder();
        message.append(ColorUtils.colorize(context.getMessage("module.quest.currency.title", "&6=== 货币查询 ===\n")));
        
        // 查询金币（Vault）
        var economyManager = context.getPlugin().getServiceContainer().get(com.guild.core.economy.EconomyManager.class);
        double goldBalance = economyManager.getBalance(player);
        String goldName = economyManager.getCurrencyName();
        String goldMessage = context.getMessage("module.quest.currency.gold", "&e{currency}: &f{balance}");
        goldMessage = goldMessage.replace("{currency}", goldName).replace("{balance}", economyManager.format(goldBalance));
        message.append(ColorUtils.colorize(goldMessage)).append("\n");
        
        // 查询A币、B币、C币
        var currencyManager = context.getApi().getCurrencyManager();
        for (CurrencyManager.CurrencyType type : CurrencyManager.CurrencyType.values()) {
            double balance = currencyManager.getBalance(guildId, playerUuid, type);
            String coinMessage = context.getMessage("module.quest.currency.coin", "&e{currency}: &f{balance}");
            coinMessage = coinMessage.replace("{currency}", type.getDisplayName()).replace("{balance}", String.format("%.0f", balance));
            message.append(ColorUtils.colorize(coinMessage)).append("\n");
        }
        
        message.append(ColorUtils.colorize(context.getMessage("module.quest.currency.footer", "&6================")));
        
        player.sendMessage(message.toString());
    }

    private void registerEventHandlers(GuildPluginAPI api) {
        api.onGuildDelete(new GuildEventHandler() {
            @Override
            public void onEvent(GuildEventData data) { questManager.saveAll(); }
            @Override
            public Object getModuleInstance() { return GuildQuestModule.this; }
        });

        api.onMemberLeave(new MemberEventHandler() {
            @Override
            public void onEvent(MemberEventData data) {
                int guildId = data.getGuildId();
                UUID playerUuid = data.getPlayerUuid();
                
                // 清理该玩家在该工会的所有任务进度
                questManager.clearPlayerProgress(guildId, playerUuid);
                
                context.getLogger().info("[Quest] 清理玩家 " + data.getPlayerName() + " 在工会 " + guildId + " 的任务进度");
            }

            @Override
            public Object getModuleInstance() { return GuildQuestModule.this; }
        });

        api.onEconomyDeposit(new EconomyEventHandler() {
            @Override
            public void onEvent(EconomyEventData data) {
                questTracker.onPlayerDepositMoney(data.getPlayerUuid(), data.getAmount());
            }
            @Override
            public Object getModuleInstance() { return GuildQuestModule.this; }
        });
    }

    private void startScheduledTasks() {
        context.runTimer(1200L, 600L, () ->
            context.getApi().getAllGuilds().thenAcceptAsync(guilds ->
                org.bukkit.Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                    for (var guild : guilds) questManager.saveGuildProgress(guild.getId());
                })
            ));
        int resetHour = context.getConfig().getInt("settings.quest-reset-hour", 4);
        long resetDelayTicks = calculateSecondsUntil(resetHour) * 20L;
        context.runTimer(Math.max(1200L, resetDelayTicks), 172800000L, () ->
            context.getApi().getAllGuilds().thenAcceptAsync(guilds ->
                org.bukkit.Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                    for (var guild : guilds) {
                        questManager.resetDailyQuests(guild.getId());
                        notifyQuestReset(guild.getId(), "daily");
                    }
                })
            ));
        long weeklyDelayTicks = calculateSecondsUntilWeekly(resetHour) * 20L;
        context.runTimer(Math.max(2400L, weeklyDelayTicks), 604800000L, () ->
            context.getApi().getAllGuilds().thenAcceptAsync(guilds ->
                org.bukkit.Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                    for (var guild : guilds) {
                        questManager.resetWeeklyQuests(guild.getId());
                        notifyQuestReset(guild.getId(), "weekly");
                    }
                })
            ));
    }

    private static long calculateSecondsUntil(int targetHour) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int currentMinute = cal.get(java.util.Calendar.MINUTE);
        int currentSecond = cal.get(java.util.Calendar.SECOND);
        int diffSeconds = (targetHour - currentHour) * 3600 - currentMinute * 60 - currentSecond;
        if (diffSeconds <= 0) diffSeconds += 86400;
        return diffSeconds;
    }

    private static long calculateSecondsUntilWeekly(int targetHour) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
        int daysUntilMonday = (java.util.Calendar.MONDAY - dayOfWeek + 7) % 7;
        if (daysUntilMonday == 0) {
            int currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            if (currentHour >= targetHour) daysUntilMonday = 7;
        }
        long secondsUntilTarget = calculateSecondsUntil(targetHour);
        return daysUntilMonday * 86400L + secondsUntilTarget;
    }

    @Override
    public void onDisable() {
        this.state = ModuleState.UNLOADED;
        GuildPluginAPI api = context.getApi();
        api.unregisterCustomGUI("quest-detail");
        api.unregisterCustomGUI("quest-active-list");
        if (questTracker != null) questTracker.stop();
        if (questManager != null) {
            questManager.saveAll();
        }
        context.getLogger().info(ColorUtils.colorize(
            context.getMessage("module.quest.unloaded", "&e[任务模块] 任务系统已关闭")));
    }

    @Override
    public ModuleDescriptor getDescriptor() { return descriptor; }

    @Override
    public void setDescriptor(ModuleDescriptor descriptor) { this.descriptor = descriptor; }

    @Override
    public ModuleState getState() { return state; }

    public ModuleContext getContext() { return context; }

    public QuestManager getQuestManager() { return questManager; }

    public QuestTracker getQuestTracker() { return questTracker; }

    public QuestRewardHandler getRewardHandler() { return rewardHandler; }

    public static int toInt(Object value, int defaultValue) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt((String) value); }
            catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            List<String> loreList = new ArrayList<>();
            for (String line : lore) loreList.add(ColorUtils.colorize(line));
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openQuestList(Player player, Object... ctx) {
        int guildId = extractGuildId(ctx);
        if (guildId <= 0) {
            // 如果从 ctx 中没有获取到公会 ID，尝试直接从玩家获取
            var guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
            if (guild == null) {
                context.sendMessage(player, "module.quest.error.no-guild", context.getMessage("module.quest.error.no-guild", "&c你不在任何公会中"));
                return;
            }
            guildId = guild.getId();
        }
        context.openGUI(player, new QuestListGUI(this, guildId, player.getUniqueId()));
    }

    private void openActiveQuests(Player player) {
        var guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            context.sendMessage(player, "module.quest.error.no-guild", context.getMessage("module.quest.error.no-guild", "&c你不在任何公会中"));
            return;
        }
        List<QuestProgress> active = questManager.getPlayerActiveQuests(
            guild.getId(), player.getUniqueId());
        context.openGUI(player, new ActiveQuestsGUI(this, active, guild.getId(), player.getUniqueId()));
    }

    private void notifyQuestReset(int guildId, String resetType) {
        // 通知所有相关GUI刷新
        java.util.Map<String, Object> refreshData = new java.util.HashMap<>();
        refreshData.put("guildId", guildId);
        refreshData.put("resetType", resetType);
        
        // 通知任务列表刷新
        context.notifyGUIRefresh("quest-list", refreshData);
        // 通知活跃任务列表刷新
        context.notifyGUIRefresh("quest-active-list", refreshData);
        
        context.getLogger().info("[Quest] 通知GUI刷新: 公会 #" + guildId + " " + resetType + " 任务已重置");
    }

    private int extractGuildId(Object... ctx) {
        if (ctx != null && ctx.length > 0) {
            if (ctx[0] instanceof Integer) return (Integer) ctx[0];
            if (ctx[0] instanceof com.guild.models.Guild) return ((com.guild.models.Guild) ctx[0]).getId();
            if (ctx[0] instanceof Player) {
                var guild = context.getPlugin().getGuildService().getPlayerGuild(
                    ((Player) ctx[0]).getUniqueId());
                return guild != null ? guild.getId() : 0;
            }
        }
        return 0;
    }

    public static class QuestCompletedEvent {
        public final String playerName;
        public final String questName;
        public final int guildId;
        public QuestCompletedEvent(String playerName, String questName, int guildId) {
            this.playerName = playerName;
            this.questName = questName;
            this.guildId = guildId;
        }
    }
}