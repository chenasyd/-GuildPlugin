package com.guild.module.example.member.rank;

import com.guild.GuildPlugin;
import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.ConsoleLogger;
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
 * Member contribution ranking module.
 * <p>
 * Features:
 * <ul>
 *   <li>Injects "Contribution Ranking" manage button into GuildSettingsGUI</li>
 *   <li>Injects "Leaderboard" view button into GuildInfoGUI (visible to all members)</li>
 *   <li>Listens to member join/leave events to auto-maintain ranking data</li>
 *   <li>Provides admin manual contribution adjustment</li>
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

        // Initialize rank manager (uses database storage)
        this.rankManager = new MemberRankManager(context.getPlugin());
        rankManager.loadAll();
        this.onlineActivityTracker = new OnlineActivityTracker(this);
        onlineActivityTracker.start();

        int defaultContribution = context.getConfig().getInt("default-contribution-on-join", 0);
        boolean autoDepositEnabled = context.getConfig().getBoolean("auto-deposit.enabled", true);
        double autoDepositAmount = context.getConfig().getDouble("auto-deposit.amount", 10);

        GuildPluginAPI api = context.getApi();

        // Listen for member join – auto-create ranking record
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

        // Listen for member leave – remove ranking record and reset A-Coins
        api.onMemberLeave(new MemberEventHandler() {
            @Override
            public void onEvent(MemberEventData data) {
                // Reset this player's A-Coins
                var currencyManager = context.getApi().getCurrencyManager();
                currencyManager.withdraw(data.getGuildId(), data.getPlayerUuid(), CurrencyManager.CurrencyType.A_COIN, 
                    currencyManager.getBalance(data.getGuildId(), data.getPlayerUuid(), CurrencyManager.CurrencyType.A_COIN));
                
                // Remove from ranking
                rankManager.removeMember(data.getGuildId(), data.getPlayerUuid());
            }

            @Override
            public Object getModuleInstance() {
                return MemberRankModule.this;
            }
        });

        // Listen for guild delete – reset all members' A-Coins
        api.onGuildDelete(new com.guild.sdk.event.GuildEventHandler() {
            @Override
            public void onEvent(com.guild.sdk.event.GuildEventData data) {
                int guildId = data.getGuildId();
                // Reset A-Coins for all guild members
                var currencyManager = context.getApi().getCurrencyManager();
                var guildService = context.getPlugin().getGuildService();
                
                try {
                    // Get all guild members
                    var members = guildService.getGuildMembers(guildId);
                    for (var member : members) {
                        // Reset each member's A-Coins
                        currencyManager.withdraw(guildId, member.getPlayerUuid(), CurrencyManager.CurrencyType.A_COIN, 
                            currencyManager.getBalance(guildId, member.getPlayerUuid(), CurrencyManager.CurrencyType.A_COIN));
                    }
                } catch (Exception e) {
                    context.getLogger().severe("[MemberRank] Failed to reset guild members' A-Coins: " + e.getMessage());
                }
                
                // Clear ranking data for this guild
                rankManager.clearByGuild(guildId);
            }

            @Override
            public Object getModuleInstance() {
                return MemberRankModule.this;
            }
        });

        // Register "Contribution Ranking" button in GuildSettingsGUI (admin entry, auto-slot)
        ItemStack settingsButton = createSettingsButton();
        api.registerGUIButton(
                "GuildSettingsGUI",
                GUIExtensionHook.AUTO_SLOT,
                settingsButton,
                "member-rank",
                (player, ctx) -> handleOpenRankGUIFromSettings(player, ctx)
        );

        // Register "A-Coin Settings" button in GuildSettingsGUI (config entry, auto-slot)
        ItemStack rankSettingsButton = createRankSettingsButton();
        api.registerGUIButton(
                "GuildSettingsGUI",
                GUIExtensionHook.AUTO_SLOT,
                rankSettingsButton,
                "member-rank",
                (player, ctx) -> handleOpenRankSettingsGUI(player, ctx)
        );

        // Register "Leaderboard" button in GuildInfoGUI (slot 14, visible to all members)
        ItemStack infoButton = createInfoButton();
        api.registerGUIButton(
                "GuildInfoGUI",
                14,
                infoButton,
                "member-rank",
                (player, ctx) -> handleOpenRankGUIFromInfo(player, ctx)
        );

        ConsoleLogger.info(context.getMessage("module.member-rank.loaded",
                "[MemberRank] Member contribution ranking system enabled"));

        ModuleDescriptor desc = context.getDescriptor();
        context.getLogger().info(String.format("[Rank-Meta] Module metadata: id=%s name=%s version=%s author=%s",
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
        ConsoleLogger.info(context.getMessage("module.member-rank.unloaded",
                "[MemberRank] Member contribution ranking system disabled"));
    }

    @Override
    public ModuleDescriptor getDescriptor() { return descriptor; }

    @Override
    public void setDescriptor(ModuleDescriptor descriptor) { this.descriptor = descriptor; }

    @Override
    public ModuleState getState() { return state; }

    // ==================== Button Handlers ====================

    private void handleOpenRankGUIFromSettings(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            context.getApi().getPlayerGuild(player.getUniqueId()).thenAccept(gd -> {
                if (gd == null) {
                    context.runSync(() -> player.sendMessage(ColorUtils.colorize(context.getMessage("module.member-rank.error.no-guild", "&cCannot retrieve guild info"))));
                    return;
                }
                Guild g = new Guild();
                g.setId(gd.getId());
                g.setName(gd.getName());
                context.runSync(() -> context.openGUI(player, new MemberRankGUI(this, g, player, true)));
            }).exceptionally(ex -> {
                context.runSync(() -> player.sendMessage(ColorUtils.colorize(context.getMessage("module.member-rank.error.no-guild", "&cCannot retrieve guild info"))));
                return null;
            });
            return;
        }
        context.openGUI(player, new MemberRankGUI(this, guild, player, true));
    }

    private void handleOpenRankGUIFromInfo(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            context.getApi().getPlayerGuild(player.getUniqueId()).thenAccept(gd -> {
                if (gd == null) {
                    context.runSync(() -> player.sendMessage(ColorUtils.colorize(context.getMessage("module.member-rank.error.no-guild", "&cCannot retrieve guild info"))));
                    return;
                }
                Guild g = new Guild();
                g.setId(gd.getId());
                g.setName(gd.getName());
                context.runSync(() -> context.openGUI(player, new MemberRankGUI(this, g, player, false)));
            }).exceptionally(ex -> {
                context.runSync(() -> player.sendMessage(ColorUtils.colorize(context.getMessage("module.member-rank.error.no-guild", "&cCannot retrieve guild info"))));
                return null;
            });
            return;
        }
        // Info entry = browse mode: hide admin hints and disable +/- actions
        context.openGUI(player, new MemberRankGUI(this, guild, player, false));
    }

    /** Open ranking GUI (for external callers) */
    public void openRankGUI(Player player, Guild guild) {
        context.openGUI(player, new MemberRankGUI(this, guild, player, true));
    }

    // ==================== Permission Check ====================

    public boolean hasManagePermission(Player player) {
        UUID uuid = player.getUniqueId();
        GuildPlugin plugin = context.getPlugin();
        GuildMember member = plugin.getGuildService().getGuildMember(uuid);
        if (member == null) return false;
        return member.getRole() == GuildMember.Role.LEADER ||
               member.getRole() == GuildMember.Role.OFFICER;
    }

    // ==================== Utility Methods ====================

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
                    context.getMessage("module.member-rank.button-name", "A-Coin Ranking")));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.member-rank.button-desc",
                            "Manage member A-Coins and rankings")));
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
                    context.getMessage("module.member-rank.info-button-name", "A-Coin Leaderboard")));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.member-rank.info-button-desc",
                            "View guild member A-Coin rankings")));
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.member-rank.info-button-hint",
                            "&7Click to view leaderboard")));
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
                    context.getMessage("module.member-rank.settings.button-name", "A-Coin Settings")));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.member-rank.settings.button-desc",
                            "Configure A-Coin growth rules")));
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.member-rank.settings.button-hint",
                            "&7Click to open configuration")));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void handleOpenRankSettingsGUI(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            context.getApi().getPlayerGuild(player.getUniqueId()).thenAccept(gd -> {
                if (gd == null) {
                    context.runSync(() -> player.sendMessage(ColorUtils.colorize(context.getMessage("module.member-rank.error.no-guild", "&cCannot retrieve guild info"))));
                    return;
                }
                Guild g = new Guild();
                g.setId(gd.getId());
                g.setName(gd.getName());
                context.runSync(() -> {
                    if (!hasManagePermission(player)) {
                        player.sendMessage(ColorUtils.colorize(context.getMessage("module.member-rank.error.no-permission", "&cYou don't have permission to manage A-Coin settings")));
                        return;
                    }
                    context.openGUI(player, new MemberRankSettingsGUI(this, g, player));
                });
            }).exceptionally(ex -> {
                context.runSync(() -> player.sendMessage(ColorUtils.colorize(context.getMessage("module.member-rank.error.no-guild", "&cCannot retrieve guild info"))));
                return null;
            });
            return;
        }
        if (!hasManagePermission(player)) {
            player.sendMessage(ColorUtils.colorize(
                    context.getMessage("module.member-rank.error.no-permission",
                            "&cYou don't have permission to manage A-Coin settings")));
            return;
        }
        context.openGUI(player, new MemberRankSettingsGUI(this, guild, player));
    }

    // ==================== Getter ====================

    public ModuleContext getContext() { return context; }
    public MemberRankManager getRankManager() { return rankManager; }
}
