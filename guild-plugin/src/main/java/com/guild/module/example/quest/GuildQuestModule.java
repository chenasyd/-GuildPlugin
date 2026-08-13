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
import com.guild.core.module.ModuleDataDirectory;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.utils.ColorUtils;
import com.guild.module.example.quest.gui.ActiveQuestsGUI;
import com.guild.module.example.quest.gui.GuildTreeGUI;
import com.guild.module.example.quest.gui.QuestDetailGUI;
import com.guild.module.example.quest.gui.QuestListGUI;
import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestObjective;
import com.guild.module.example.quest.model.QuestProgress;
import com.guild.module.example.quest.model.QuestReward;
import com.guild.module.example.quest.tree.GuildTreeService;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.event.EconomyEventData;
import com.guild.sdk.event.EconomyEventHandler;
import com.guild.sdk.event.GuildEventData;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventData;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.gui.GUILayoutDefinition;
import com.guild.sdk.gui.ModuleGUIRegistration;

public class GuildQuestModule implements GuildModule {
    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;

    private QuestManager questManager;
    private QuestTracker questTracker;
    private QuestRewardHandler rewardHandler;
    private GuildTreeService treeService;
    private QuestTexts texts;

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;
        this.texts = new QuestTexts(context);
        File dataDir = ModuleDataDirectory.getModuleDataRoot(context);

        this.questManager = new QuestManager(dataDir, context.getLogger());
        questManager.setContext(context);
        registerDefaultQuests();
        questManager.loadAll();

        this.rewardHandler = new QuestRewardHandler(context);
        this.treeService = new GuildTreeService(context);
        this.rewardHandler.setTreeService(treeService);
        this.questTracker = new QuestTracker(this);

        GuildPluginAPI api = context.getApi();
        registerGUIButtons(api);
        registerCommands(api);
        registerEventHandlers(api);
        startScheduledTasks();

        api.registerCustomGUI(ModuleGUIRegistration.builder("quest-detail", (player, data) -> {
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
                    // Fall back to any progress (including claimed) so detail shows completed state
                    if (progress == null) {
                        QuestProgress any = questManager.getPlayerQuestAny(guildId, playerUuid, def.getId());
                        if (any != null && any.isClaimed()
                            && questManager.isActiveInCurrentPeriod(any, def)) {
                            progress = any;
                        } else if (any != null && def.getType() == QuestDefinition.QuestType.ONE_TIME) {
                            progress = any;
                        }
                    }
                }
                
                QuestDetailGUI gui = new QuestDetailGUI.Builder(this)
                    .fromDefinition(def)
                    .fromProgress(progress)
                    .withGuildInfo(guildId, playerUuid)
                    .build();
                gui.setViewer(player);
                return gui;
                
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
        })
            .moduleId("guild-quest")
            .imageBinding("quest-detail")
            .layout(GUILayoutDefinition.builder()
                .function("HEADER", 0, 1, 2, 3, 4, 5, 6, 7, 8)
                .function("CONTENT", 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25)
                .function("ACTIONS", 28, 29, 30, 31, 32, 33, 34)
                .function("BACK", 49)
                .build())
            .build());

        api.registerCustomGUI(ModuleGUIRegistration.builder("quest-active-list", (player, data) -> {
            int guildId = toInt(data.get("guildId"), 0);
            UUID playerUuid = player.getUniqueId();
            List<QuestProgress> active = questManager.getPlayerActiveQuests(guildId, playerUuid);
            return new ActiveQuestsGUI(this, active, guildId, playerUuid);
        })
            .moduleId("guild-quest")
            .imageBinding("quest-active-list")
            .layout(GUILayoutDefinition.builder()
                .function("HEADER", 0, 1, 2, 3, 4, 5, 6, 7, 8)
                .function("CONTENT", 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43)
                .function("BACK", 49)
                .build())
            .build());

            context.runLater(100L, () -> {
            questTracker.start();
            context.getLogger().info(context.getMessage("module.quest.loaded", questManager.getDefinitions().size()));
        });

        context.getEventBus().subscribe("guild-quest", QuestCompletedEvent.class, event -> {});
    }

    private void registerDefaultQuests() {
        // Display text comes from lang keys at render time (module.quest.<id>.*)
        QuestDefinition daily1 = new QuestDefinition(
            "daily_hunter", QuestDefinition.QuestType.DAILY, 1, 1, true);
        daily1.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
            15, "module.quest.daily_hunter.objective"));
        daily1.addReward(new QuestReward(QuestReward.RewardType.EXP, 30));
        daily1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 50));
        questManager.registerDefinition(daily1);

        QuestDefinition daily2 = new QuestDefinition(
            "daily_online", QuestDefinition.QuestType.DAILY, 2, 1, true);
        daily2.addObjective(new QuestObjective(QuestObjective.ObjectiveType.ONLINE_HOURS,
            60, "module.quest.daily_online.objective"));
        daily2.addReward(new QuestReward(QuestReward.RewardType.EXP, 520));
        questManager.registerDefinition(daily2);

        QuestDefinition weekly1 = new QuestDefinition(
            "weekly_contributor", QuestDefinition.QuestType.WEEKLY, 1, 2, true);
        weekly1.addObjective(new QuestObjective(
            QuestObjective.ObjectiveType.DEPOSIT_MONEY, 2000,
            "module.quest.weekly_contributor.objective"));
        weekly1.addReward(new QuestReward(QuestReward.RewardType.EXP, 100));
        weekly1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 300));
        questManager.registerDefinition(weekly1);

        QuestDefinition weekly2 = new QuestDefinition(
            "weekly_slayer", QuestDefinition.QuestType.WEEKLY, 2, 3, true);
        weekly2.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
            100, "module.quest.weekly_slayer.objective"));
        weekly2.addReward(new QuestReward(QuestReward.RewardType.EXP, 2080));
        questManager.registerDefinition(weekly2);

        QuestDefinition oneTime1 = new QuestDefinition(
            "onetime_first_blood", QuestDefinition.QuestType.ONE_TIME, 1, 1, false);
        oneTime1.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
            5, "module.quest.onetime_first_blood.objective"));
        oneTime1.addReward(new QuestReward(QuestReward.RewardType.EXP, 25));
        oneTime1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 100));
        questManager.registerDefinition(oneTime1);
    }

    private void registerGUIButtons(GuildPluginAPI api) {
        ItemStack questButton = new ItemStack(Material.BOOK);
        ItemMeta questMeta = questButton.getItemMeta();
        if (questMeta != null) {
            questMeta.setDisplayName("Guild Quests"); // 回退文本，实际由 getDisplayItem 按模块语言解析
            questMeta.setLore(List.of("Accept and track guild quests",
                    "Daily | Weekly | One-time",
                    "Click to open quest panel",
                    ""));
            questButton.setItemMeta(questMeta);
        }
        api.registerGUIButton("GuildInfoGUI", 14, questButton, "guild-quest",
            (player, ctx) -> openQuestList(player, ctx),
            "module.quest.button-name",
            "module.quest.quest-list-lore",
            "module.quest.quest-types",
            "module.quest.click-open");

        ItemStack activeButton = new ItemStack(Material.COMPASS);
        ItemMeta activeMeta = activeButton.getItemMeta();
        if (activeMeta != null) {
            activeMeta.setDisplayName("Active Quests"); // 回退文本
            activeMeta.setLore(List.of("View your current quest progress"));
            activeButton.setItemMeta(activeMeta);
        }
        api.registerGUIButton("GuildInfoGUI", GUIExtensionHook.AUTO_SLOT,
            activeButton, "guild-quest",
            (player, ctx) -> openActiveQuests(player),
            "module.quest.active-quests",
            "module.quest.active-quests-lore");
    }

    private void registerCommands(GuildPluginAPI api) {
        api.registerSubCommand("guild-quest", "guild", "quest", (sender, args) -> handleQuestCommand(sender, args), "guild.quest");
        api.registerSubCommand("guild-quest", "guild", "tasks", (sender, args) -> handleQuestCommand(sender, args), "guild.quest");
        api.registerSubCommand("guild-quest", "guild", "currencies", (sender, args) -> handleCurrenciesCommand(sender, args), "guild.quest");
        api.registerSubCommand("guild-quest", "guild", "currency", (sender, args) -> handleCurrenciesCommand(sender, args), "guild.quest");
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
        } else if (args.length > 0 && args[0].equalsIgnoreCase("tree")) {
            openGuildTree(player);
        } else {
            openQuestList(player);
        }
    }

    public void openGuildTree(Player player) {
        context.getApi().getPlayerGuild(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                context.runSync(() -> context.sendMessage(player, "module.quest.error.no-guild",
                    context.getMessage("module.quest.error.no-guild", "&cYou are not in any guild")));
                return;
            }
            context.runSync(() -> context.openGUI(player,
                new GuildTreeGUI(this, guild.getId(), guild.getLevel(), player.getUniqueId())));
        }).exceptionally(ex -> {
            context.runSync(() -> context.sendMessage(player, "module.quest.error.load-fail",
                "&cFailed to query guild: " + ex.getMessage()));
            return null;
        });
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
            message.append(ColorUtils.colorize(context.getLanguageManager().getModuleMessage(
                "module.quest.currency.title", "=== Currency Info ==="))).append("\n");

            // Query Vault economy
            var economyManager = context.getPlugin().getServiceContainer().get(com.guild.core.economy.EconomyManager.class);
            double goldBalance = economyManager.getBalance(player);
            String goldName = economyManager.getCurrencyName();
            message.append(ColorUtils.colorize(context.getLanguageManager().getModuleMessage(
                "module.quest.currency.gold", "{currency}: {balance}")
                .replace("{currency}", goldName).replace("{balance}", economyManager.format(goldBalance))))
                .append("\n");

            // Query guild tree virtual EXP
            if (treeService != null) {
                var treeState = treeService.getOrCreate(guildId);
                message.append(ColorUtils.colorize(context.getLanguageManager().getModuleMessage(
                    "module.quest.currency.tree", "Guild Tree EXP: {balance}")
                    .replace("{balance}", String.valueOf(treeState.getVirtualExp()))))
                    .append("\n");
                message.append(ColorUtils.colorize(context.getLanguageManager().getModuleMessage(
                    "module.quest.currency.tree-level", "Guild Tree Level: {level}")
                    .replace("{level}", String.valueOf(treeState.getTreeLevel()))))
                    .append("\n");
            }

            // Query guild currencies via string API (SDK v1.5+)
            String[] types = {"A_COIN", "B_COIN", "C_COIN"};
            String[] names = {"ACoin", "BCoin", "CCoin"};
            for (int i = 0; i < types.length; i++) {
                double balance = context.getApi().getCurrencyBalance(guildId, playerUuid, types[i]);
                message.append(ColorUtils.colorize(context.getLanguageManager().getModuleMessage(
                    "module.quest.currency.coin", "{currency}: {balance}")
                    .replace("{currency}", names[i]).replace("{balance}", String.format("%.0f", balance))))
                    .append("\n");
            }

            message.append(ColorUtils.colorize(context.getLanguageManager().getModuleMessage(
                "module.quest.currency.footer", "================")));

            context.runSync(() -> player.sendMessage(message.toString()));
        }).exceptionally(ex -> {
            context.runSync(() -> context.sendMessage(player, "module.quest.error.load-fail", "&cFailed to query guild: " + ex.getMessage()));
            return null;
        });
    }

    private void registerEventHandlers(GuildPluginAPI api) {
        api.onGuildDelete(new GuildEventHandler() {
            @Override
            public void onEvent(GuildEventData data) {
                questManager.saveAll();
                if (treeService != null) {
                    treeService.invalidate(data.getGuildId());
                }
            }
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

    public GuildTreeService getTreeService() { return treeService; }

    public QuestTexts texts() {
        if (texts == null) {
            texts = new QuestTexts(context);
        }
        return texts;
    }

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