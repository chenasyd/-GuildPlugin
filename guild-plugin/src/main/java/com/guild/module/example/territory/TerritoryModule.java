package com.guild.module.example.territory;

import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDataDirectory;

import java.io.File;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.models.Guild;
import org.bukkit.entity.Player;

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
    private TerritoryServerIdentity serverIdentity;
    private TerritoryRepository repository;
    private TerritoryCrossServerSync crossServerSync;
    private TerritoryMaterializer materializer;
    private TerritoryBridge bridge;
    private WorldGuardProbe.Availability availability;
    private TerritoryMemberSync memberSync;
    private TerritorySelectionManager selectionManager;
    private TerritoryCommandHandler commandHandler;
    private TerritoryTexts texts;
    private TerritoryHomeProtectIntegration homeProtectIntegration;
    private TerritoryEventEmitter eventEmitter;
    private TerritoryAPIImpl territoryApi;
    private TerritoryGuiRegistrar guiRegistrar;

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;
        this.texts = new TerritoryTexts(context);
        this.settings = new TerritorySettings(context);

        File dataDir = ModuleDataDirectory.getModuleDataRoot(context);
        this.serverIdentity = TerritoryServerIdentity.resolve(dataDir, settings, context.getLogger());
        this.repository = new TerritoryRepository(
                dataDir,
                context.getLogger(),
                context.getPlugin().getDatabaseManager(),
                settings,
                serverIdentity);
        repository.load();

        this.eventEmitter = new TerritoryEventEmitter(context, repository, settings);
        this.territoryApi = new TerritoryAPIImpl(this);
        context.getPlugin().getServiceContainer().register(
                com.guild.sdk.territory.TerritoryAPI.class, territoryApi);

        this.crossServerSync = new TerritoryCrossServerSync(repository, settings, context.getLogger());
        crossServerSync.register();

        refreshBridgeAndAvailability();

        this.materializer = new TerritoryMaterializer(context, repository, bridge, settings,
                eventEmitter);
        materializer.scheduleMaterializeOnLoad();
        context.registerEvents(new TerritoryWorldLoadListener(materializer));

        this.memberSync = new TerritoryMemberSync(context, bridge, repository, crossServerSync,
                this, eventEmitter);
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

        this.guiRegistrar = new TerritoryGuiRegistrar(this, context, settings, texts);
        guiRegistrar.register(context.getApi());
        context.getApi().registerPlaceholderProvider("guild-territory",
                new TerritoryPlaceholderProvider(this));
        homeProtectIntegration = TerritoryModuleBootstrap.registerHomeProtectIntegration(
                this, context, settings, homeProtectIntegration);
        TerritoryModuleBootstrap.logLoadStatus(context, bridge, availability);
        TerritoryModuleBootstrap.broadcastStatusToAdmins(context, bridge, availability, texts);
    }

    @Override
    public void onDisable() {
        if (crossServerSync != null) {
            crossServerSync.unregister();
        }
        if (repository != null) {
            repository.save();
        }
        if (homeProtectIntegration != null && context != null) {
            context.getApi().unregisterHomeProtectIntegration(this);
        }
        if (context != null && context.getPlugin().getServiceContainer()
                .has(com.guild.sdk.territory.TerritoryAPI.class)) {
            context.getPlugin().getServiceContainer()
                    .unregister(com.guild.sdk.territory.TerritoryAPI.class);
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
        TerritoryModuleBootstrap.logLoadStatus(context, bridge, availability);
        if (materializer != null) {
            this.materializer = new TerritoryMaterializer(context, repository, bridge, settings,
                    eventEmitter);
            materializer.scheduleMaterializeOnLoad();
        }
        homeProtectIntegration = TerritoryModuleBootstrap.registerHomeProtectIntegration(
                this, context, settings, homeProtectIntegration);
    }

    private void refreshBridgeAndAvailability() {
        this.availability = WorldGuardProbe.probe();
        this.bridge = TerritoryBridgeFactory.create(
                availability, repository, context.getLogger(), settings, crossServerSync,
                eventEmitter);
    }

    public void openTerritoryGui(Player player, Guild guild, boolean manageMode) {
        if (guiRegistrar != null) {
            guiRegistrar.openTerritoryGui(player, guild, manageMode);
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

    public TerritorySelectionManager getSelectionManager() {
        return selectionManager;
    }

    public TerritoryTexts getTexts() {
        return texts;
    }

    public ModuleContext getContext() {
        return context;
    }

    public TerritoryServerIdentity getServerIdentity() {
        return serverIdentity;
    }

    public TerritoryCrossServerSync getCrossServerSync() {
        return crossServerSync;
    }

    public TerritoryMaterializer getMaterializer() {
        return materializer;
    }

    public TerritoryEventEmitter getEventEmitter() {
        return eventEmitter;
    }

    public boolean isWorldGuardReady() {
        return bridge != null && bridge.isOperational();
    }
}
