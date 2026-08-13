package com.guild.activity;

import com.guild.GuildPlugin;
import com.guild.core.module.ModuleManager;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.gui.GuildInfoGUI;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Starts builtin activity tracking and registers the GuildInfoGUI button.
 */
public final class ActivityBootstrap {

    public static final String MODULE_ID = "builtin-activity";

    private final GuildPlugin plugin;
    private final ActivitySettings settings;
    private final ActivityRepository repository;
    private final ActivityScoreService scoreService;
    private ActivityTracker tracker;
    private boolean buttonRegistered;

    public ActivityBootstrap(GuildPlugin plugin) {
        this.plugin = plugin;
        this.settings = new ActivitySettings(plugin);
        this.repository = new ActivityRepository(plugin);
        this.scoreService = new ActivityScoreService(plugin, repository, settings);
    }

    public ActivityScoreService getScoreService() {
        return scoreService;
    }

    public ActivitySettings getSettings() {
        return settings;
    }

    public void start() {
        settings.reload(plugin);
        if (!settings.isEnabled()) {
            plugin.getLogger().info("[Activity] Disabled (guild-activity.enabled=false)");
            return;
        }
        tracker = new ActivityTracker(plugin, repository, settings);
        tracker.start();
    }

    /**
     * Hot-reload activity settings. Restarts tracker when enabled/interval changes;
     * (un)registers GuildInfo button when toggle changes.
     */
    public void reload() {
        int previousInterval = settings.getTickIntervalSeconds();
        boolean wasEnabled = settings.isEnabled();
        boolean wasButton = settings.isRegisterInfoButton();
        settings.reload(plugin);

        if (!settings.isEnabled()) {
            if (tracker != null) {
                tracker.stop();
                tracker = null;
            }
            unregisterInfoButton();
            if (wasEnabled) {
                plugin.getLogger().info("[Activity] Disabled after reload (guild-activity.enabled=false)");
            }
            return;
        }

        boolean needRestart = tracker == null
                || !wasEnabled
                || previousInterval != settings.getTickIntervalSeconds();
        if (needRestart) {
            if (tracker != null) {
                tracker.stop();
                tracker = null;
            }
            tracker = new ActivityTracker(plugin, repository, settings);
            tracker.start();
        }

        if (!settings.isRegisterInfoButton()) {
            unregisterInfoButton();
        } else if (!wasButton || !buttonRegistered) {
            registerInfoButton();
        }
    }

    private void unregisterInfoButton() {
        if (!buttonRegistered) {
            return;
        }
        ModuleManager mm = plugin.getModuleManager();
        if (mm != null) {
            mm.getRegistry().getGuiExtensionHook().unregisterByModule(MODULE_ID);
        }
        buttonRegistered = false;
    }

    /**
     * Call after {@link ModuleManager} is ready so GUI extension hook exists.
     */
    public void registerInfoButton() {
        if (!settings.isEnabled() || !settings.isRegisterInfoButton() || buttonRegistered) {
            return;
        }
        ModuleManager mm = plugin.getModuleManager();
        if (mm == null) {
            return;
        }
        GUIExtensionHook hook = mm.getRegistry().getGuiExtensionHook();
        ItemStack button = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Member Contribution"); // fallback; resolved via lang keys
            meta.setLore(Arrays.asList(
                    "View member contribution and activity",
                    "Hybrid score leaderboard"));
            button.setItemMeta(meta);
        }
        hook.registerButton(GuildInfoGUI.GUI_TYPE, 12, button, MODULE_ID,
                (player, ctx) -> {
                    Guild g = null;
                    if (ctx != null && ctx.length > 0 && ctx[0] instanceof Guild) {
                        g = (Guild) ctx[0];
                    }
                    openFromInfo(player, g);
                },
                "module.activity.info-button",
                "module.activity.info-button-lore1",
                "module.activity.info-button-lore2");
        buttonRegistered = true;
        plugin.getLogger().info("[Activity] Registered GuildInfoGUI button (" + MODULE_ID + ")");
    }

    private void openFromInfo(Player player, Guild guild) {
        if (guild == null) {
            plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(g -> {
                if (g == null) {
                    return;
                }
                CompatibleOpen.open(plugin, player, g, GuildActivityGUI.ReturnTo.GUILD_INFO, null);
            });
            return;
        }
        CompatibleOpen.open(plugin, player, guild, GuildActivityGUI.ReturnTo.GUILD_INFO, null);
    }

    public void shutdown() {
        if (tracker != null) {
            tracker.stop();
            tracker = null;
        }
        unregisterInfoButton();
    }

    /** Tiny helper to avoid importing CompatibleScheduler in multiple call sites. */
    static final class CompatibleOpen {
        static void open(GuildPlugin plugin, Player player, Guild guild,
                         GuildActivityGUI.ReturnTo returnTo,
                         com.guild.models.GuildMember member) {
            com.guild.core.utils.CompatibleScheduler.runTask(plugin, player, () ->
                    plugin.getGuiManager().openGUI(player,
                            new GuildActivityGUI(plugin, guild, player, returnTo, member)));
        }
    }
}
