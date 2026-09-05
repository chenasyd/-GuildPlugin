package com.guild.module.example.territory;

import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDataDirectory;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;

import java.io.File;

/**
 * 公会领地模块（WorldGuard 软依赖）。
 * <p>
 * P7-b：WG 可用时使用 {@link WorldGuardTerritoryBridge}；否则降级为 No-Op。
 */
public final class TerritoryModule implements GuildModule {

    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;

    private TerritoryRepository repository;
    private TerritoryBridge bridge;
    private WorldGuardProbe.Availability availability;

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;

        File dataDir = ModuleDataDirectory.getModuleDataRoot(context);
        this.repository = new TerritoryRepository(dataDir, context.getLogger());
        repository.load();

        this.availability = WorldGuardProbe.probe();
        this.bridge = TerritoryBridgeFactory.create(availability, repository, context.getLogger());
        if (bridge.isOperational()) {
            context.getLogger().info("WorldGuard territory bridge active (P7-b).");
        } else {
            context.getLogger().warning("Guild territory module loaded in degraded mode; missing: "
                    + availability.describeMissing()
                    + ". Install WorldGuard + WorldEdit to enable territory features.");
        }

        // P7-c: register MemberEventHandler for syncMembers
        // P7-d: registerSubCommand / GUI for claim & info
        // P7-e: coordinate with GuildHomeProtectListener when operational
    }

    @Override
    public void onDisable() {
        if (repository != null) {
            repository.save();
        }
        state = ModuleState.UNLOADED;
    }

    @Override
    public void onConfigReload(ModuleContext context) {
        // P7-b: reload flag defaults from modules.guild-territory.*
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

    /** 供后续命令/GUI 与单测使用。 */
    public TerritoryBridge getBridge() {
        return bridge;
    }

    public TerritoryRepository getRepository() {
        return repository;
    }

    public WorldGuardProbe.Availability getAvailability() {
        return availability;
    }

    public boolean isWorldGuardReady() {
        return bridge != null && bridge.isOperational();
    }
}
