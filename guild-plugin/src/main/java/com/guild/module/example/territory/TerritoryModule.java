package com.guild.module.example.territory;

import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDataDirectory;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;

/**
 * 公会领地模块（WorldGuard 软依赖）。
 * <p>
 * WorldGuard 可用时使用 {@link WorldGuardTerritoryBridge}；否则降级为 No-Op。
 */
public final class TerritoryModule implements GuildModule {

    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;

    private TerritorySettings settings;
    private TerritoryRepository repository;
    private TerritoryBridge bridge;
    private WorldGuardProbe.Availability availability;
    private TerritoryMemberSync memberSync;
    private TerritorySelectionManager selectionManager;
    private TerritoryCommandHandler commandHandler;
    private TerritoryTexts texts;
    private TerritoryHomeProtectIntegration homeProtectIntegration;

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;
        this.texts = new TerritoryTexts(context);
        this.settings = new TerritorySettings(context);

        File dataDir = ModuleDataDirectory.getModuleDataRoot(context);
        this.repository = new TerritoryRepository(dataDir, context.getLogger());
        repository.load();

        refreshBridgeAndAvailability();
        logLoadStatus();

        this.memberSync = new TerritoryMemberSync(context, bridge, repository, this);
        memberSync.register(context.getApi());
        memberSync.repairAllOnLoad();

        this.selectionManager = new TerritorySelectionManager();
        context.registerEvents(new TerritorySelectionListener(context, selectionManager, this, texts));

        this.commandHandler = new TerritoryCommandHandler(this, context, selectionManager, texts);
        context.getApi().registerSubCommand(
                "guild-territory",
                "guild",
                "territory",
                (sender, args) -> commandHandler.handle(sender, args),
                "guild.territory.info"
        );

        registerHomeProtectIntegration();
        broadcastStatusToAdmins();
    }

    @Override
    public void onDisable() {
        if (repository != null) {
            repository.save();
        }
        if (homeProtectIntegration != null && context != null) {
            context.getApi().unregisterHomeProtectIntegration(this);
        }
        state = ModuleState.UNLOADED;
    }

    @Override
    public void onConfigReload(ModuleContext context) {
        this.context = context;
        if (settings != null) {
            settings.reload();
        }
        refreshBridgeAndAvailability();
        logLoadStatus();
        registerHomeProtectIntegration();
    }

    private void refreshBridgeAndAvailability() {
        this.availability = WorldGuardProbe.probe();
        this.bridge = TerritoryBridgeFactory.create(availability, repository, context.getLogger(), settings);
    }

    private void registerHomeProtectIntegration() {
        if (context == null || settings == null) {
            return;
        }
        String mode = settings.getHomeProtectMode();
        if (TerritoryHomeProtectIntegration.MODE_OFF.equals(mode)) {
            if (homeProtectIntegration != null) {
                context.getApi().unregisterHomeProtectIntegration(this);
                homeProtectIntegration = null;
            }
            return;
        }

        if (homeProtectIntegration == null) {
            homeProtectIntegration = new TerritoryHomeProtectIntegration(this, context);
            context.getApi().registerHomeProtectIntegration(this, homeProtectIntegration);
        }

        if (bridge.isOperational() && TerritoryHomeProtectIntegration.MODE_DEFER.equals(mode)) {
            context.getLogger().info("Deferring guild.home-protect to WorldGuard (home-protect.mode=defer).");
        } else if (bridge.isOperational()) {
            context.getLogger().info("Home-protect merge mode active (home-protect.mode=merge).");
        }
    }

    private void logLoadStatus() {
        if (bridge.isOperational()) {
            context.getLogger().info("WorldGuard territory bridge active.");
        } else {
            context.getLogger().warning("Guild territory module loaded in degraded mode; missing: "
                    + availability.describeMissing()
                    + ". Install WorldGuard + WorldEdit to enable territory features.");
        }
    }

    private void broadcastStatusToAdmins() {
        if (texts == null || context == null) {
            return;
        }
        String message;
        if (bridge.isOperational()) {
            message = texts.format("module.territory.status-ready",
                    "&a领地模块已加载（WorldGuard 可用）");
        } else {
            message = texts.format("module.territory.status-degraded",
                    "&e领地模块已降级加载：缺少 {0}", availability.describeMissing());
        }
        context.getLogger().info(ColorUtils.stripColor(message));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (context.getPlugin().getPermissionManager().hasPermission(player, "guild.territory.admin")) {
                player.sendMessage(message);
            }
        }
    }

    @Override
    public ModuleDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void setDescriptor(ModuleDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public ModuleState getState() {
        return state;
    }

    public TerritoryBridge getBridge() {
        return bridge;
    }

    public TerritoryRepository getRepository() {
        return repository;
    }

    public TerritorySettings getSettings() {
        return settings;
    }

    public WorldGuardProbe.Availability getAvailability() {
        return availability;
    }

    public TerritoryMemberSync getMemberSync() {
        return memberSync;
    }

    public TerritoryCommandHandler getCommandHandler() {
        return commandHandler;
    }

    public boolean isWorldGuardReady() {
        return bridge != null && bridge.isOperational();
    }
}
