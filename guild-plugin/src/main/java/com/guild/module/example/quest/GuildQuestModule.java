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
                context.getLogger().warning("[Quest-Factory] Invalid guildId: " +
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
                context.getLogger().severe("[Quest-Factory] Failed to create QuestDetailGUI: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    context.getLogger().severe("    at " + element.toString());
                }
                try {
                    return new QuestDetailGUI.Builder(this)
                        .withGuildInfo(guildId != 0 ? guildId : -1, 
                            playerUuid != null ? playerUuid : new UUID(0, 0))
                        .build();
                } catch (Exception fallbackEx) {
                    throw new RuntimeException("Unable to create any GUI", fallbackEx);
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
            context.getLogger().info("[Quest] Quest system enabled");
        });

        // Load module language resources for currently loaded languages
        try {
            var lm = context.getLanguageManager();
            for (String lang : lm.getLoadedLanguages()) {
                context.getApi().loadModuleLanguageResource(context.getDescriptor().getId(), lang);
            }
        } catch (Exception ignored) {}

        context.getEventBus().subscribe(QuestCompletedEvent.class, event -> {});
    }

    private void registerDefaultQuests() {
        // Daily quest 1: Daily Hunter
        QuestDefinition daily1 = new QuestDefinition(
            "daily_hunter", 
            context.getMessage("module.quest.daily_hunter.name", "Daily Hunter"), 
            context.getMessage("module.quest.daily_hunter.description", "Kill a specified number of monsters"),
            QuestDefinition.QuestType.DAILY, 1, 1, true);
        daily1.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
            15, context.getMessage("module.quest.daily_hunter.objective", "Kill 15 hostile mobs")));
        daily1.addReward(new QuestReward(QuestReward.RewardType.CONTRIBUTION, 30));
        daily1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 50));
        questManager.registerDefinition(daily1);

        // Daily quest 2: Daily Standby
        QuestDefinition daily2 = new QuestDefinition(
            "daily_online", 
            context.getMessage("module.quest.daily_online.name", "Daily Standby"), 
            context.getMessage("module.quest.daily_online.description", "Stay online for a certain amount of time"),
            QuestDefinition.QuestType.DAILY, 2, 1, true);
        daily2.addObjective(new QuestObjective(QuestObjective.ObjectiveType.ONLINE_HOURS,
            60, context.getMessage("module.quest.daily_online.objective", "Stay online for 60 minutes")));
        daily2.addReward(new QuestReward(QuestReward.RewardType.CONTRIBUTION, 20));
        daily2.addReward(new QuestReward(QuestReward.RewardType.EXP, 500));
        questManager.registerDefinition(daily2);

        // Weekly quest 1: Weekly Contributor
        QuestDefinition weekly1 = new QuestDefinition(
            "weekly_contributor", 
            context.getMessage("module.quest.weekly_contributor.name", "Weekly Contributor"), 
            context.getMessage("module.quest.weekly_contributor.description", "Make significant contributions to the guild"),
            QuestDefinition.QuestType.WEEKLY, 1, 2, true);
        weekly1.addObjective(new QuestObjective(
            QuestObjective.ObjectiveType.DEPOSIT_MONEY, 2000, 
            context.getMessage("module.quest.weekly_contributor.objective", "Deposit $2000 to guild funds")));
        weekly1.addReward(new QuestReward(QuestReward.RewardType.CONTRIBUTION, 100));
        weekly1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 300));
        questManager.registerDefinition(weekly1);

        // Weekly quest 2: Weekly Slayer
        QuestDefinition weekly2 = new QuestDefinition(
            "weekly_slayer", 
            context.getMessage("module.quest.weekly_slayer.name", "Weekly Slayer"), 
            context.getMessage("module.quest.weekly_slayer.description", "Prove your strength by slaying many monsters"),
            QuestDefinition.QuestType.WEEKLY, 2, 3, true);
        weekly2.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
            100, context.getMessage("module.quest.weekly_slayer.objective", "Kill 100 hostile mobs")));
        weekly2.addReward(new QuestReward(QuestReward.RewardType.CONTRIBUTION, 80));
        weekly2.addReward(new QuestReward(QuestReward.RewardType.EXP, 2000));
        questManager.registerDefinition(weekly2);

        // One-time quest: First Blood
        QuestDefinition oneTime1 = new QuestDefinition(
            "onetime_first_blood", 
            context.getMessage("module.quest.onetime_first_blood.name", "First Blood"), 
            context.getMessage("module.quest.onetime_first_blood.description", "Complete your first quest"),
            QuestDefinition.QuestType.ONE_TIME, 1, 1, false);
        oneTime1.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
            5, context.getMessage("module.quest.onetime_first_blood.objective", "Kill 5 mobs")));
        oneTime1.addReward(new QuestReward(QuestReward.RewardType.CONTRIBUTION, 25));
        oneTime1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 100));
        questManager.registerDefinition(oneTime1);
    }

    private void registerGUIButtons(GuildPluginAPI api) {
        ItemStack questButton = createItem(Material.BOOK,
            "&6&l" + context.getMessage("module.quest.button-name", "Guild Quests"),
            "&7" + context.getMessage("module.quest.quest-list-lore", "Accept and track guild quests"),
            "&7" + context.getMessage("module.quest.quest-types", "Daily &8| &7Weekly &8| &7One-time"),
            "",
            "&e" + context.getMessage("module.quest.click-open", "Click to open quest panel"));
        api.registerGUIButton("GuildInfoGUI", 14, questButton, "guild-quest",
            (player, ctx) -> openQuestList(player, ctx));

        ItemStack activeButton = createItem(Material.COMPASS,
            "&a&l" + context.getMessage("module.quest.active-quests", "Active Quests"),
            "&7" + context.getMessage("module.quest.active-quests-lore", "View progress of your ongoing quests"));
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
                context.getApi().getPlayerGuild(player.getUniqueId()).thenAccept(guildData -> {
                    if (guildData != null) {
                        questManager.resetDailyQuests(guildData.getId());
                        context.runSync(() -> context.sendMessage(player, "module.quest.reset-done", "&a[Quest] Daily quests have been reset"));
                    } else {
                        context.runSync(() -> context.sendMessage(player, "module.quest.error.no-guild", context.getMessage("module.quest.error.no-guild", "&cYou are not in any guild")));
                    }
                }).exceptionally(ex -> {
                    context.runSync(() -> context.sendMessage(player, "module.quest.error.load-fail", "&cFailed to query guild: " + ex.getMessage()));
                    return null;
                });
            } else {
                context.sendMessage(player, "module.quest.no-permission", "&cInsufficient permission");
            }
        } else {
            openQuestList(player);
        }
    }

    private void handleCurrenciesCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        context.getApi().getPlayerGuild(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                context.runSync(() -> context.sendMessage(player, "module.quest.error.no-guild", "&cYou are not in any guild"));
                return;
            }

            int guildId = guild.getId();
            UUID playerUuid = player.getUniqueId();

            StringBuilder message = new StringBuilder();
            message.append(ColorUtils.colorize("&6=== Currency Info ===\n"));

            // Query Vault economy
            var economyManager = context.getPlugin().getServiceContainer().get(com.guild.core.economy.EconomyManager.class);
            double goldBalance = economyManager.getBalance(player);
            String goldName = economyManager.getCurrencyName();
            message.append(ColorUtils.colorize("&e{currency}: &f{balance}"
                .replace("{currency}", goldName).replace("{balance}", economyManager.format(goldBalance))))
                .append("\n");

            // Query guild currencies (A/B/C coins)
            var currencyManager = context.getApi().getCurrencyManager();
            for (CurrencyManager.CurrencyType type : CurrencyManager.CurrencyType.values()) {
                double balance = currencyManager.getBalance(guildId, playerUuid, type);
                message.append(ColorUtils.colorize("&e{currency}: &f{balance}"
                    .replace("{currency}", type.getDisplayName()).replace("{balance}", String.format("%.0f", balance))))
                    .append("\n");
            }

            message.append(ColorUtils.colorize("&6================"));

            context.runSync(() -> player.sendMessage(message.toString()));
        }).exceptionally(ex -> {
            context.runSync(() -> context.sendMessage(player, "module.quest.error.load-fail", "&cFailed to query guild: " + ex.getMessage()));
            return null;
        });
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
                
                questManager.clearPlayerProgress(guildId, playerUuid);
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
                com.guild.core.utils.CompatibleScheduler.runTask(context.getPlugin(), () -> {
                    for (var guild : guilds) questManager.saveGuildProgress(guild.getId());
                })
            ));
        int resetHour = context.getConfig().getInt("settings.quest-reset-hour", 4);
        long resetDelayTicks = calculateSecondsUntil(resetHour) * 20L;
        context.runTimer(Math.max(1200L, resetDelayTicks), 172800000L, () ->
            context.getApi().getAllGuilds().thenAcceptAsync(guilds ->
                com.guild.core.utils.CompatibleScheduler.runTask(context.getPlugin(), () -> {
                    for (var guild : guilds) {
                        questManager.resetDailyQuests(guild.getId());
                        notifyQuestReset(guild.getId(), "daily");
                    }
                })
            ));
        long weeklyDelayTicks = calculateSecondsUntilWeekly(resetHour) * 20L;
        context.runTimer(Math.max(2400L, weeklyDelayTicks), 604800000L, () ->
            context.getApi().getAllGuilds().thenAcceptAsync(guilds ->
                com.guild.core.utils.CompatibleScheduler.runTask(context.getPlugin(), () -> {
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
        context.getLogger().info(
            context.getMessage("module.quest.unloaded", "[Quest] Quest system disabled"));
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
            // If guild ID not provided, fetch asynchronously
            context.getApi().getPlayerGuild(player.getUniqueId()).thenAccept(guildData -> {
                if (guildData == null) {
                    context.runSync(() -> context.sendMessage(player, "module.quest.error.no-guild", context.getMessage("module.quest.error.no-guild", "&cYou are not in any guild")));
                    return;
                }
                int resolvedGuildId = guildData.getId();
                context.runSync(() -> context.openGUI(player, new QuestListGUI(this, resolvedGuildId, player.getUniqueId())));
            }).exceptionally(ex -> {
                context.runSync(() -> context.sendMessage(player, "module.quest.error.load-fail", "&cFailed to query guild: " + ex.getMessage()));
                return null;
            });
            return;
        }
        context.openGUI(player, new QuestListGUI(this, guildId, player.getUniqueId()));
    }

    private void openActiveQuests(Player player) {
        context.getApi().getPlayerGuild(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                context.runSync(() -> context.sendMessage(player, "module.quest.error.no-guild", context.getMessage("module.quest.error.no-guild", "&cYou are not in any guild")));
                return;
            }
            List<QuestProgress> active = questManager.getPlayerActiveQuests(guild.getId(), player.getUniqueId());
            context.runSync(() -> context.openGUI(player, new ActiveQuestsGUI(this, active, guild.getId(), player.getUniqueId())));
        }).exceptionally(ex -> {
            context.runSync(() -> context.sendMessage(player, "module.quest.error.load-fail", "&cFailed to query guild: " + ex.getMessage()));
            return null;
        });
    }

    private void notifyQuestReset(int guildId, String resetType) {
        // Notify related GUIs to refresh
        java.util.Map<String, Object> refreshData = new java.util.HashMap<>();
        refreshData.put("guildId", guildId);
        refreshData.put("resetType", resetType);
        
        // Notify quest list refresh
        context.notifyGUIRefresh("quest-list", refreshData);
        // Notify active quest list refresh
        context.notifyGUIRefresh("quest-active-list", refreshData);
    }

    private int extractGuildId(Object... ctx) {
        if (ctx != null && ctx.length > 0) {
            if (ctx[0] instanceof Integer) return (Integer) ctx[0];
            if (ctx[0] instanceof com.guild.models.Guild) return ((com.guild.models.Guild) ctx[0]).getId();
            if (ctx[0] instanceof Player) {
                // Do not perform blocking lookup here; caller should resolve player guild asynchronously
                return 0;
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