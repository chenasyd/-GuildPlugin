package com.guild.module.example.stats;

import com.guild.GuildPlugin;
import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.GuildEventData;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.event.MemberEventData;
import com.guild.module.example.stats.gui.*;
import com.guild.module.example.stats.model.ActivityReport;
import com.guild.module.example.stats.model.GuildStatistics;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;

        File dataDir = new File(context.getPlugin().getDataFolder(),
            "data" + File.separator + "guild-stats");
        File statsDir = new File(dataDir, "statistics");

        this.statsManager = new GuildStatsManager(statsDir, context.getLogger());
        this.dataCache = new StatsDataCache();
        this.webReporter = new WebReporter(context.getApi(), context.getLogger(), context.getConfig());
        this.activityPersistence = new ActivityDataPersistence(dataDir, context.getLogger());
        this.activityTracker = new ActivityTracker(this, activityPersistence);
        this.economyFetcher = new EconomyContributionFetcher(this, context.getLogger());
        this.activityCalculator = new ActivityCalculator(activityTracker);

        statsManager.loadAll();
        activityTracker.start();

        GuildPluginAPI api = context.getApi();

        registerGUIButtons(api);
        registerCommands(api);
        registerEventHandlers(api);
        startScheduledTasks();

        context.getLogger().info(ColorUtils.colorize(
            context.getMessage("module.stats.loaded", "&a[统计模块] 公会数据统计系统已启用")));
    }

    @Override
    public void onDisable() {
        this.state = ModuleState.UNLOADED;
        if (activityTracker != null) {
            activityTracker.stop();
        }
        if (statsManager != null) {
            statsManager.saveAll();
            statsManager.clearAll();
        }
        dataCache.clearAll();
        context.getLogger().info(ColorUtils.colorize(
            context.getMessage("module.stats.unloaded", "&e[统计模块] 统计系统已关闭")));
    }

    @Override
    public ModuleDescriptor getDescriptor() { return descriptor; }

    @Override
    public void setDescriptor(ModuleDescriptor descriptor) { this.descriptor = descriptor; }

    @Override
    public ModuleState getState() { return state; }

    private void registerGUIButtons(GuildPluginAPI api) {
        ItemStack statsButton = createItem(Material.BOOK,
            "&6&l" + context.getMessage("module.stats.button-name", "数据统计"),
            "&7查看公会详细运营数据",
            "&7成员活跃度 &8| &7经济报表 &8| &7排行榜");
        api.registerGUIButton("GuildInfoGUI", 16, statsButton, "guild-stats",
            (player, ctx) -> openStatsOverview(player, ctx));

        ItemStack rankingButton = createItem(Material.GOLD_BLOCK,
            "&e&l" + context.getMessage("module.stats.ranking-button", "公会排行"),
            "&7查看全服公会综合实力排行");
        api.registerGUIButton("MainGuildGUI", GUIExtensionHook.AUTO_SLOT,
            rankingButton, "guild-stats",
            (player, ctx) -> openGuildRanking(player));
    }

    private void registerCommands(GuildPluginAPI api) {
        api.registerSubCommand("guild", "stats",
            (sender, args) -> handleStatsCommand(sender, args),
            "guild.stats.view");
    }

    private void handleStatsCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "top":
                    openGuildRanking(player);
                    break;
                case "refresh":
                    if (player.hasPermission("guild.stats.admin.refresh")) {
                        player.sendMessage(ColorUtils.colorize("&a[Stats] 正在刷新..."));
                        forceRefresh(player);
                    } else {
                        player.sendMessage(ColorUtils.colorize("&c权限不足"));
                    }
                    break;
                default:
                    showHelp(player);
            }
        } else {
            openMyGuildStats(player);
        }
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
        context.runTimer(100L, 6000L, () -> updateAllGuildsStatistics());

        boolean webEnabled = context.getConfig().getBoolean("web-report.enabled", false);
        if (webEnabled) {
            context.runTimer(200L, 72000L, () ->
                webReporter.reportAllGuilds(dataCache.getAllCachedStats()));
        }

        int retentionDays = context.getConfig().getInt("retention-days", 30);
        context.runTimer(1000L, 1728000L, () ->
            statsManager.cleanupOlderThanDays(retentionDays));
    }

    private void updateAllGuildsStatistics() {
        GuildPluginAPI api = context.getApi();
        api.getAllGuilds()
            .thenAccept(guilds -> {
                List<CompletableFuture<Void>> tasks = new ArrayList<>();
                for (GuildData guild : guilds) {
                    tasks.add(updateSingleGuild(guild.getId())
                        .thenAccept(stats -> {
                            dataCache.updateCache(guild.getId(), stats);
                            statsManager.put(stats);
                        }));
                }
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                    .thenRun(() -> context.getLogger().info(
                        "[Stats] 已更新 " + guilds.size() + " 个公会的统计数据"))
                    .exceptionally(ex -> {
                        context.getLogger().warning("[Stats] 批量更新失败: " + ex.getMessage());
                        return null;
                    });
            })
            .exceptionally(ex -> {
                context.getLogger().severe("[Stats] 获取公会列表失败: " + ex.getMessage());
                return null;
            });
    }

    private CompletableFuture<GuildStatistics> updateSingleGuild(int guildId) {
        GuildPluginAPI api = context.getApi();
        var guildService = context.getServiceContainer().get(com.guild.services.GuildService.class);
        CompletableFuture<Long> expFuture = guildService != null
            ? guildService.getGuildEconomyAsync(guildId)
                .thenApply(econ -> econ != null ? (long) econ.getExperience() : 0L)
            : CompletableFuture.completedFuture(0L);
        return api.getGuildById(guildId)
            .thenCompose(guild -> {
                if (guild == null) return CompletableFuture.completedFuture(null);
                return api.getGuildMembers(guildId)
                    .thenCombine(expFuture, (members, experience) ->
                        calculateStats(guild, members, experience));
            });
    }

    private GuildStatistics calculateStats(GuildData guild, List<MemberData> members, long experience) {
        GuildStatistics stats = new GuildStatistics(guild.getId());
        stats.setGuildName(guild.getName());
        stats.setLevel(guild.getLevel());
        stats.setMemberCount(guild.getMemberCount());
        stats.setMaxMembers(guild.getMaxMembers());
        stats.setBalance(guild.getBalance());
        stats.setExperience(experience);

        double totalContrib = 0;
        int onlineCount = 0;
        for (MemberData m : members) {
            totalContrib += m.getContribution();
            if (m.isOnline()) onlineCount++;
        }

        stats.setTotalContribution(totalContrib);
        stats.setAvgContribution(members.isEmpty() ? 0 : totalContrib / members.size());
        stats.setActiveMemberCount(onlineCount);

        // 使用 ActivityCalculator 计算真实的活跃度评分（而非简单公式）
        double activityScore;
        if (!members.isEmpty()) {
            ActivityReport report = activityCalculator.calculate(guild, members);
            if (report != null && !report.getMembers().isEmpty()) {
                double totalScore = 0;
                for (var m : report.getMembers()) {
                    totalScore += m.getActivityScore();
                }
                activityScore = members.isEmpty() ? 0 : totalScore / members.size();
            } else {
                activityScore = calculateFallbackActivityScore(onlineCount, totalContrib, members.size());
            }
        } else {
            activityScore = 0;
        }
        stats.setActivityScore(activityScore);

        double overallScore = guild.getLevel() * 50 + activityScore * 3 +
            Math.min(totalContrib, 10000) * 0.05;
        stats.setOverallScore(overallScore);

        return stats;
    }

    private double calculateFallbackActivityScore(int onlineCount, double totalContrib, int memberCount) {
        double onlineScore = Math.min(30.0, onlineCount * 30.0 / Math.max(memberCount, 1));
        double contribScore = Math.min(40.0, (memberCount > 0 ? (totalContrib / memberCount) * 0.004 : 0));
        double baseScore = Math.min(20.0, memberCount * 2.0);
        return Math.min(100.0, contribScore + onlineScore + baseScore + 10.0);
    }

    private void openStatsOverview(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            player.sendMessage(ColorUtils.colorize("&c无法获取工会信息"));
            return;
        }

        player.sendMessage(ColorUtils.colorize("&e[Stats] 正在加载最新统计数据..."));

        java.util.concurrent.CompletableFuture<GuildStatistics> statsFuture = updateSingleGuild(guild.getId());
        java.util.concurrent.CompletableFuture<EconomyContributionFetcher.EconomySummary> economyFuture =
            economyFetcher.fetchEconomySummary(guild.getId())
                .exceptionally(ex -> {
                    context.getLogger().warning("[Stats] 经济数据加载失败(非致命): " + ex.getMessage());
                    return null;
                });

        statsFuture.thenAcceptBoth(economyFuture, (stats, econSummary) -> {
            if (stats != null) {
                dataCache.updateCache(guild.getId(), stats);
                context.getLogger().info(String.format(
                    "[Stats] 刷新公会 %s 统计: 活跃度=%.1f 贡献=%.0f 经济=%s",
                    guild.getName(),
                    stats.getActivityScore(),
                    stats.getTotalContribution(),
                    econSummary != null ? String.format("净$%,.0f", econSummary.netTotal) : "无"));
                try {
                    context.openGUI(player, new StatsOverviewGUI(this, guild, stats, econSummary));
                } catch (Exception e) {
                    player.sendMessage(ColorUtils.colorize(
                        "&c[Stats] 打开界面失败: " + e.getMessage()));
                    e.printStackTrace();
                }
            } else {
                GuildStatistics fallback = dataCache.getCachedStats(guild.getId());
                if (fallback != null) {
                    context.openGUI(player, new StatsOverviewGUI(this, guild, fallback, econSummary));
                } else {
                    player.sendMessage(ColorUtils.colorize(
                        "&c[Stats] 无法加载统计数据"));
                }
            }
        })
        .exceptionally(ex -> {
            GuildStatistics fallback = dataCache.getCachedStats(guild.getId());
            if (fallback != null) {
                context.openGUI(player, new StatsOverviewGUI(this, guild, fallback, null));
            } else {
                player.sendMessage(ColorUtils.colorize(
                    "&c[Stats] 加载失败: " + ex.getMessage()));
            }
            return null;
        });
    }

    private void openGuildRanking(Player player) {
        List<GuildStatistics> allStats = dataCache.getAllCachedStats();
        allStats.sort((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()));
        context.openGUI(player, new GuildRankingGUI(this, allStats));
    }

    private void openMyGuildStats(Player player) {
        UUID uuid = player.getUniqueId();
        GuildPlugin plugin = context.getPlugin();
        Guild guild = plugin.getGuildService().getPlayerGuild(uuid);
        if (guild != null) {
            openStatsOverview(player, guild);
        } else {
            player.sendMessage(ColorUtils.colorize("&c你不在任何公会中"));
        }
    }

    private void forceRefresh(Player player) {
        updateAllGuildsStatistics();
        player.sendMessage(ColorUtils.colorize("&a[Stats] 已触发全量刷新，请稍后查看"));
    }

    private void showHelp(Player player) {
        player.sendMessage(ColorUtils.colorize("&6&l=== 公会统计命令 ==="));
        player.sendMessage(ColorUtils.colorize("&e/guild stats &7- 查看自己公会"));
        player.sendMessage(ColorUtils.colorize("&e/guild stats top &7- 全服排行"));
        player.sendMessage(ColorUtils.colorize("&e/guild stats refresh &7- 刷新数据(OP)"));
    }

    private Guild extractGuild(Object... ctx) {
        if (ctx != null && ctx.length > 0 && ctx[0] instanceof Guild) {
            return (Guild) ctx[0];
        }
        return null;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            if (lore != null && lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(ColorUtils.colorize(line));
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public ModuleContext getContext() { return context; }
    public StatsDataCache getDataCache() { return dataCache; }
    public ActivityCalculator getActivityCalculator() { return activityCalculator; }
    public EconomyContributionFetcher getEconomyFetcher() { return economyFetcher; }
}
