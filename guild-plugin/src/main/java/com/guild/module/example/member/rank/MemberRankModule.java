package com.guild.module.example.member.rank;

import com.guild.GuildPlugin;
import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.event.MemberEventData;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.economy.CurrencyManager;
import com.guild.module.example.member.rank.gui.MemberRankGUI;
import com.guild.module.example.member.rank.gui.MemberRankSettingsGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 成员贡献排名模块
 * <p>
 * 功能：
 * <ul>
 *   <li>在 GuildSettingsGUI 注入"贡献排名"管理按钮</li>
 *   <li>在 GuildInfoGUI 注入"排行榜"查看按钮（所有成员可见）</li>
 *   <li>监听成员加入/离开事件自动维护排名数据</li>
 *   <li>提供贡献值手动调整功能（管理员）</li>
 * </ul>
 */
public class MemberRankModule implements GuildModule {

    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;
    private MemberRankManager rankManager;
    private OnlineActivityTracker onlineActivityTracker;

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;

        // 初始化排名管理器，使用数据库存储
        this.rankManager = new MemberRankManager(context.getPlugin());
        rankManager.loadAll();
        this.onlineActivityTracker = new OnlineActivityTracker(this);
        onlineActivityTracker.start();

        int defaultContribution = context.getConfig().getInt("default-contribution-on-join", 0);
        boolean autoDepositEnabled = context.getConfig().getBoolean("auto-deposit.enabled", true);
        double autoDepositAmount = context.getConfig().getDouble("auto-deposit.amount", 10);

        GuildPluginAPI api = context.getApi();

        // 监听成员加入事件 —— 自动在排名中创建记录
        api.onMemberJoin(new MemberEventHandler() {
            @Override
            public void onEvent(MemberEventData data) {
                var record = rankManager.getOrCreate(data.getGuildId(), data.getPlayerUuid(), data.getPlayerName());
                if (defaultContribution > 0 && record != null) {
                    record.addACoin(defaultContribution);
                }
            }

            @Override
            public Object getModuleInstance() {
                return MemberRankModule.this;
            }
        });

        // 监听成员离开事件 —— 从排名中移除记录并重置 A 币
        api.onMemberLeave(new MemberEventHandler() {
            @Override
            public void onEvent(MemberEventData data) {
                // 重置该玩家的 A 币
                var currencyManager = context.getApi().getCurrencyManager();
                currencyManager.withdraw(data.getGuildId(), data.getPlayerUuid(), CurrencyManager.CurrencyType.A_COIN, 
                    currencyManager.getBalance(data.getGuildId(), data.getPlayerUuid(), CurrencyManager.CurrencyType.A_COIN));
                
                // 从排名中移除记录
                rankManager.removeMember(data.getGuildId(), data.getPlayerUuid());
            }

            @Override
            public Object getModuleInstance() {
                return MemberRankModule.this;
            }
        });

        // 监听公会删除事件 —— 重置所有成员的 A 币
        api.onGuildDelete(new com.guild.sdk.event.GuildEventHandler() {
            @Override
            public void onEvent(com.guild.sdk.event.GuildEventData data) {
                int guildId = data.getGuildId();
                // 重置该公会所有成员的 A 币
                var currencyManager = context.getApi().getCurrencyManager();
                var guildService = context.getPlugin().getGuildService();
                
                try {
                    // 获取公会所有成员
                    var members = guildService.getGuildMembers(guildId);
                    for (var member : members) {
                        // 重置每个成员的 A 币
                        currencyManager.withdraw(guildId, member.getPlayerUuid(), CurrencyManager.CurrencyType.A_COIN, 
                            currencyManager.getBalance(guildId, member.getPlayerUuid(), CurrencyManager.CurrencyType.A_COIN));
                    }
                } catch (Exception e) {
                    context.getLogger().severe("[MemberRank] 重置公会成员 A 币失败: " + e.getMessage());
                }
                
                // 清除该公会的排名数据
                rankManager.clearByGuild(guildId);
            }

            @Override
            public Object getModuleInstance() {
                return MemberRankModule.this;
            }
        });

        // 在 GuildSettingsGUI 中注册"贡献排名"按钮（管理入口，自动分配槽位）
        ItemStack settingsButton = createSettingsButton();
        api.registerGUIButton(
                "GuildSettingsGUI",
                GUIExtensionHook.AUTO_SLOT,
                settingsButton,
                "member-rank",
                (player, ctx) -> handleOpenRankGUIFromSettings(player, ctx)
        );

        // 在 GuildSettingsGUI 中注册"A币设置"按钮（配置入口，自动分配槽位）
        ItemStack rankSettingsButton = createRankSettingsButton();
        api.registerGUIButton(
                "GuildSettingsGUI",
                GUIExtensionHook.AUTO_SLOT,
                rankSettingsButton,
                "member-rank",
                (player, ctx) -> handleOpenRankSettingsGUI(player, ctx)
        );

        // 在 GuildInfoGUI 中注册"排行榜"按钮（固定槽位14，所有成员可见）
        ItemStack infoButton = createInfoButton();
        api.registerGUIButton(
                "GuildInfoGUI",
                14,
                infoButton,
                "member-rank",
                (player, ctx) -> handleOpenRankGUIFromInfo(player, ctx)
        );

        context.getLogger().info(ColorUtils.colorize(context.getMessage("module.member-rank.loaded",
                "&a[排名模块] 成员贡献排名系统已启用")));

        ModuleDescriptor desc = context.getDescriptor();
        context.getLogger().info(String.format("[Rank-Meta] 模块元信息: id=%s name=%s version=%s author=%s",
            desc.getId(), desc.getName(), desc.getVersion(), desc.getAuthor()));
    }

    @Override
    public void onDisable() {
        this.state = ModuleState.UNLOADED;
        if (onlineActivityTracker != null) {
            onlineActivityTracker.stop();
            onlineActivityTracker = null;
        }
        if (rankManager != null) {
            rankManager.saveAll();
            rankManager.clearAll();
        }
        context.getLogger().info(ColorUtils.colorize(context.getMessage("module.member-rank.unloaded",
                "&e[排名模块] 成员贡献排名系统已关闭")));
    }

    @Override
    public ModuleDescriptor getDescriptor() { return descriptor; }

    @Override
    public void setDescriptor(ModuleDescriptor descriptor) { this.descriptor = descriptor; }

    @Override
    public ModuleState getState() { return state; }

    // ==================== 按钮处理 ====================

    private void handleOpenRankGUIFromSettings(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            player.sendMessage(ColorUtils.colorize(
                    context.getMessage("module.member-rank.error.no-guild",
                            "&c无法获取工会信息")));
            return;
        }
        context.openGUI(player, new MemberRankGUI(this, guild, player, true));
    }

    private void handleOpenRankGUIFromInfo(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            player.sendMessage(ColorUtils.colorize(
                    context.getMessage("module.member-rank.error.no-guild",
                            "&c无法获取工会信息")));
            return;
        }
        // 信息页入口为浏览模式：隐藏管理提示并禁用加减操作
        context.openGUI(player, new MemberRankGUI(this, guild, player, false));
    }

    /** 打开排名 GUI（供外部调用） */
    public void openRankGUI(Player player, Guild guild) {
        context.openGUI(player, new MemberRankGUI(this, guild, player, true));
    }

    // ==================== 权限检查 ====================

    public boolean hasManagePermission(Player player) {
        UUID uuid = player.getUniqueId();
        GuildPlugin plugin = context.getPlugin();
        GuildMember member = plugin.getGuildService().getGuildMember(uuid);
        if (member == null) return false;
        return member.getRole() == GuildMember.Role.LEADER ||
               member.getRole() == GuildMember.Role.OFFICER;
    }

    // ==================== 工具方法 ====================

    private Guild extractGuild(Object... ctx) {
        if (ctx != null && ctx.length > 0 && ctx[0] instanceof Guild) {
            return (Guild) ctx[0];
        }
        return null;
    }

    private ItemStack createSettingsButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&6&l" +
                    context.getMessage("module.member-rank.button-name", "A币排名")));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.member-rank.button-desc",
                            "管理成员A币和排名")));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoButton() {
        Material material;
        try {
            material = Material.valueOf("GOLD_NUGGET");
        } catch (IllegalArgumentException e) {
            material = Material.GOLD_INGOT;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&e&l" +
                    context.getMessage("module.member-rank.info-button-name", "A币排行榜")));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.member-rank.info-button-desc",
                            "查看工会成员A币排名")));
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.member-rank.info-button-hint",
                            "&7点击查看排行榜")));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRankSettingsButton() {
        Material material;
        try {
            material = Material.valueOf("REDSTONE_COMPARATOR");
        } catch (IllegalArgumentException e) {
            material = Material.REDSTONE;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&6&l" +
                    context.getMessage("module.member-rank.settings.button-name", "A币设置")));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.member-rank.settings.button-desc",
                            "配置 A 币增长规则")));
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.member-rank.settings.button-hint",
                            "&7点击打开配置界面")));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void handleOpenRankSettingsGUI(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            player.sendMessage(ColorUtils.colorize(
                    context.getMessage("module.member-rank.error.no-guild",
                            "&c无法获取工会信息")));
            return;
        }
        if (!hasManagePermission(player)) {
            player.sendMessage(ColorUtils.colorize(
                    context.getMessage("module.member-rank.error.no-permission",
                            "&c您没有权限管理 A 币设置")));
            return;
        }
        context.openGUI(player, new MemberRankSettingsGUI(this, guild, player));
    }

    // ==================== Getter ====================

    public ModuleContext getContext() { return context; }
    public MemberRankManager getRankManager() { return rankManager; }
}
