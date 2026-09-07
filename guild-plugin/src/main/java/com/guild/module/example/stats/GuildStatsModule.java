package com.guild.module.example.stats;

import com.guild.GuildPlugin;
import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDataDirectory;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.GuildEventData;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.event.MemberEventData;
import com.guild.sdk.gui.GUILayoutDefinition;
import com.guild.sdk.gui.ModuleGUIRegistration;
import com.guild.module.example.stats.gui.GuildRankingGUI;
import com.guild.module.example.stats.gui.StatsOverviewGUI;
import com.guild.module.example.stats.model.GuildStatistics;
import com.guild.core.events.EventBus;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.List;

public class GuildStatsModule implements GuildModule {

    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;

    private GuildStatsManager statsManager;
    private StatsDataCache dataCache;
    private WebReporter webReporter;
    private ActivityDataPersistence activityPersistence;
    private ActivityTracker activityTracker;
    private ActivityCalculator activityCalculator;
    private EconomyContributionFetcher economyFetcher;
    private StatsAggregator statsAggregator;
    private StatsCommandHandler statsCommandHandler;

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;

        File dataDir = ModuleDataDirectory.getModuleDataRoot(context);
        File statsDir = new File(dataDir, "statistics");

        this.statsManager = new GuildStatsManager(statsDir, context.getLogger());
        this.dataCache = new StatsDataCache();
        // 延迟创建 WebReporter，仅在配置启用时实例化，避免无谓的资源消耗
        boolean webEnabled = context.getConfig().getBoolean("web-report.enabled", false);
        this.webReporter = webEnabled ? new WebReporter(context.getApi(), context.getLogger(), context.getConfig()) : null;
        this.activityPersistence = new ActivityDataPersistence(dataDir, context.getLogger());
        this.activityTracker = new ActivityTracker(this, activityPersistence);
        this.economyFetcher = new EconomyContributionFetcher(this, context.getLogger());
        this.activityCalculator = new ActivityCalculator(activityTracker);

        statsManager.loadAll();
        StatsActivityBridge.configure(context, activityTracker);

        this.statsAggregator = new StatsAggregator(context, statsManager, dataCache, activityCalculator);
        this.statsCommandHandler = new StatsCommandHandler(this, statsAggregator);

        GuildPluginAPI api = context.getApi();

        registerGUIButtons(api);
        registerCommands(api);
        registerEventHandlers(api);
        startScheduledTasks();

        context.runLater(100L, () -> {
            if (webReporter != null) webReporter.healthCheck();
            context.getLogger().info(
                context.getMessage("module.stats.init-done", "[Stats] Initialization complete"));
        });

        context.getEventBus().subscribe("guild-stats", StatsRefreshedEvent.class, event ->
            context.getLogger().info(String.format("[Stats-Event] Guild #%d %s stats refreshed (activity=%.1f)",
                event.guildId, event.guildName, event.activityScore)));

        context.getLogger().info(
            context.getMessage("module.stats.loaded", "[Stats] Guild stats system enabled"));

    }

    @Override
    public void onDisable() {
        this.state = ModuleState.UNLOADED;
        if (activityTracker != null) {
            activityTracker.stop();
        }
        if (statsManager != null) {
            context.runAsync(() -> {
                statsManager.saveAll();
                statsManager.clearAll();
            });
        }
        dataCache.clearAll();
        context.getLogger().info(
            context.getMessage("module.stats.unloaded", "[Stats] Stats system disabled"));
    }

    @Override
    public ModuleDescriptor getDescriptor() { return descriptor; }

    @Override
    public void setDescriptor(ModuleDescriptor descriptor) { this.descriptor = descriptor; }

    @Override
    public ModuleState getState() { return state; }

    private void registerGUIButtons(GuildPluginAPI api) {
        ItemStack statsButton = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsButton.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName("Statistics");
            statsMeta.setLore(List.of("View guild overview stats"));
            statsButton.setItemMeta(statsMeta);
        }
        api.registerGUIButton("GuildInfoGUI", 16, statsButton, "guild-stats",
            (player, ctx) -> openStatsOverview(player, ctx),
            "module.stats.button-name",
            "module.stats.button-desc",
            "module.stats.button-lore");

        ItemStack rankingButton = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta rankMeta = rankingButton.getItemMeta();
        if (rankMeta != null) {
            rankMeta.setDisplayName("Guild Ranking");
            rankMeta.setLore(List.of("View server-wide guild ranking"));
            rankingButton.setItemMeta(rankMeta);
        }
        api.registerGUIButton("MainGuildGUI", GUIExtensionHook.AUTO_SLOT,
            rankingButton, "guild-stats",
            (player, ctx) -> openGuildRanking(player),
            "module.stats.ranking-button",
            "module.stats.ranking-button-desc");

        // One custom GUI registration — demonstrates ModuleGUIRegistration (layout + moduleId)
        api.registerCustomGUI(ModuleGUIRegistration.builder("stats-overview", (player, data) -> {
            Guild guildObj = (Guild) data.get("guild");
            GuildStatistics statsData = (GuildStatistics) data.get("stats");
            EconomyContributionFetcher.EconomySummary econ =
                (EconomyContributionFetcher.EconomySummary) data.get("economySummary");
            return new StatsOverviewGUI(this, guildObj, statsData, econ);
        })
            .moduleId("guild-stats")
            .imageBinding("stats-overview")
            .layout(GUILayoutDefinition.builder()
                .function("HEADER", 0, 1, 2, 3, 4, 5, 6, 7, 8)
                .function("CONTENT", 20, 22, 24, 31)
                .function("BACK", 49)
                .build())
            .build());
    }

    private void registerCommands(GuildPluginAPI api) {
        api.registerSubCommand("guild-stats", "guild", "stats",
                (sender, args) -> statsCommandHandler.handleStatsCommand(sender, args),
                "guild.stats.view");
    }

    private void registerEventHandlers(GuildPluginAPI api) {
        api.onGuildCreate(new GuildEventHandler() {
            @Override
            public void onEvent(GuildEventData data) {
                context.getLogger().info("[Stats] 新公会: " + data.getGuildName()
                    + " (ID:" + data.getGuildId() + ")");
                GuildStatistics initialStats = new GuildStatistics(data.getGuildId());
                initialStats.setGuildName(data.getGuildName());
                statsManager.put(initialStats);
            }
            @Override
            public Object getModuleInstance() { return GuildStatsModule.this; }
        });

        api.onGuildDelete(new GuildEventHandler() {
            @Override
            public void onEvent(GuildEventData data) {
                statsManager.remove(data.getGuildId());
                dataCache.invalidate(data.getGuildId());
                context.getLogger().info("[Stats] 清理已删除公会: " + data.getGuildName());
            }
            @Override
            public Object getModuleInstance() { return GuildStatsModule.this; }
        });
    }

    private void startScheduledTasks() {
        context.runTimer(100L, 6000L, () -> statsAggregator.updateAllGuildsStatistics());

        if (webReporter != null) {
            context.runTimer(200L, 72000L, () -> webReporter.reportAllGuilds(dataCache.getAllCachedStats()));
        }

        int retentionDays = context.getConfig().getInt("retention-days", 30);
        context.runTimer(1000L, 1728000L, () ->
            context.runAsync(() -> statsManager.cleanupOlderThanDays(retentionDays)));
    }

    void openStatsOverview(Player player, Object... ctx) {
        statsCommandHandler.openStatsOverview(player, ctx);
    }

    void openGuildRanking(Player player) {
        statsCommandHandler.openGuildRanking(player);
    }

    public ModuleContext getContext() { return context; }
    public StatsDataCache getDataCache() { return dataCache; }
    public EconomyContributionFetcher getEconomyFetcher() { return economyFetcher; }

    public static class StatsRefreshedEvent {
        public final int guildId;
        public final String guildName;
        public final int totalGuilds;
        public final double activityScore;

        public StatsRefreshedEvent(int guildId, String guildName, int totalGuilds, double activityScore) {
            this.guildId = guildId;
            this.guildName = guildName;
            this.totalGuilds = totalGuilds;
            this.activityScore = activityScore;
        }
    }
}
