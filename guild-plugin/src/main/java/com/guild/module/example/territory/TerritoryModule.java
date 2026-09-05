package com.guild.module.example.territory;

import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDataDirectory;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;

import java.io.File;

/**
 * 公会领地模块（WorldGuard 软依赖骨架）。
 * <p>
 * 当前阶段：探测 WG/WE、初始化本地映射存储、注册 No-Op 桥接器。
 * 具体 claim/sync/命令 见 {@code package-info.java} 设计说明。
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
        if (availability.fullyReady()) {
            context.getLogger().info("WorldGuard + WorldEdit detected; territory bridge pending implementation (P7-b).");
            this.bridge = new NoOpTerritoryBridge(context.getLogger(), "P7-b not implemented");
        } else {
            context.getLogger().warning("Guild territory module loaded in degraded mode; missing: "
                    + availability.describeMissing()
                    + ". Install WorldGuard + WorldEdit to enable territory features.");
            this.bridge = new NoOpTerritoryBridge(context.getLogger(), "missing " + availability.describeMissing());
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
        return availability != null && availability.fullyReady();
    }
}
