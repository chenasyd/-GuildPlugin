package com.guild.module.example.territory;

import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDataDirectory;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.module.example.territory.gui.ConfirmTerritoryClaimGUI;
import com.guild.module.example.territory.gui.ConfirmTerritoryUnclaimGUI;
import com.guild.module.example.territory.gui.TerritoryManagementGUI;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.gui.GUILayoutDefinition;
import com.guild.sdk.gui.ModuleGUIRegistration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.List;

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
        logLoadStatus();

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

        registerTerritoryGui(context.getApi());
        registerHomeProtectIntegration();
        context.getApi().registerPlaceholderProvider("guild-territory",
                new TerritoryPlaceholderProvider(this));
        broadcastStatusToAdmins();
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
        logLoadStatus();
        if (materializer != null) {
            this.materializer = new TerritoryMaterializer(context, repository, bridge, settings,
                    eventEmitter);
            materializer.scheduleMaterializeOnLoad();
        }
        registerHomeProtectIntegration();
    }

    private void refreshBridgeAndAvailability() {
        this.availability = WorldGuardProbe.probe();
        this.bridge = TerritoryBridgeFactory.create(
                availability, repository, context.getLogger(), settings, crossServerSync,
                eventEmitter);
    }

    private void registerTerritoryGui(GuildPluginAPI api) {
        if (settings == null || !settings.isGuiEnabled()) {
            return;
        }

        api.registerCustomGUI(ModuleGUIRegistration.builder(TerritoryManagementGUI.GUI_ID, (player, data) -> {
            Guild guild = (Guild) data.get("guild");
            boolean manage = Boolean.TRUE.equals(data.get("manage"));
            if (guild == null) {
                guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
            }
            if (guild == null) {
                throw new IllegalStateException("No guild for territory GUI");
            }
            return new TerritoryManagementGUI(this, guild, player, manage);
        })
                .moduleId("guild-territory")
                .imageBinding("territory-manage")
                .layout(GUILayoutDefinition.builder()
                        .function("HEADER", 4)
                        .function("LIST", 10, 11, 12, 13, 14, 15, 16)
                        .function("STATUS", 20, 22, 24)
                        .function("ACTIONS", 28, 29, 30, 31, 32)
                        .function("BACK", 49)
                        .build())
                .build());

        api.registerCustomGUI(ModuleGUIRegistration.builder(ConfirmTerritoryClaimGUI.GUI_ID, (player, data) -> {
            Guild guild = (Guild) data.get("guild");
            if (guild == null) {
                guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
            }
            return new ConfirmTerritoryClaimGUI(this, guild, player);
        })
                .moduleId("guild-territory")
                .imageBinding("territory-confirm-claim")
                .layout(GUILayoutDefinition.builder()
                        .function("CONFIRM", 11)
                        .function("INFO", 13)
                        .function("CANCEL", 15)
                        .build())
                .build());

        api.registerCustomGUI(ModuleGUIRegistration.builder(ConfirmTerritoryUnclaimGUI.GUI_ID, (player, data) -> {
            Guild guild = (Guild) data.get("guild");
            if (guild == null) {
                guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
            }
            return new ConfirmTerritoryUnclaimGUI(this, guild, player);
        })
                .moduleId("guild-territory")
                .imageBinding("territory-confirm-unclaim")
                .layout(GUILayoutDefinition.builder()
                        .function("CONFIRM", 11)
                        .function("INFO", 13)
                        .function("CANCEL", 15)
                        .build())
                .build());

        if (settings.isRegisterSettingsButton()) {
            ItemStack settingsButton = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = settingsButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Guild Territory");
                meta.setLore(List.of("Manage guild land claims"));
                settingsButton.setItemMeta(meta);
            }
            api.registerGUIButton("GuildSettingsGUI", GUIExtensionHook.AUTO_SLOT,
                    settingsButton, "guild-territory",
                    (player, ctx) -> handleSettingsButton(player, ctx),
                    "module.territory.gui.settings-button",
                    "module.territory.gui.settings-button-desc");
        }

        if (settings.isRegisterInfoButton()) {
            ItemStack infoButton = new ItemStack(Material.MAP);
            ItemMeta meta = infoButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Territory");
                meta.setLore(List.of("View guild territory"));
                infoButton.setItemMeta(meta);
            }
            api.registerGUIButton("GuildInfoGUI", GUIExtensionHook.AUTO_SLOT,
                    infoButton, "guild-territory",
                    (player, ctx) -> handleInfoButton(player, ctx),
                    "module.territory.gui.info-button",
                    "module.territory.gui.info-button-desc");
        }
    }

    private void handleSettingsButton(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
        }
        if (guild == null) {
            texts.send(player, "module.territory.not-in-guild", "&c你不在任何公会中。");
            return;
        }
        if (!canManageTerritory(player)) {
            texts.send(player, "module.territory.not-manager", "&c仅公会管理可操作领地。");
            return;
        }
        openTerritoryGui(player, guild, true);
    }

    private void handleInfoButton(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
        }
        if (guild == null) {
            texts.send(player, "module.territory.not-in-guild", "&c你不在任何公会中。");
            return;
        }
        if (!context.getPlugin().getPermissionManager().hasPermission(player, "guild.territory.info")) {
            texts.send(player, "module.territory.no-permission", "&c你没有权限执行此操作。");
            return;
        }
        boolean manage = canManageTerritory(player);
        openTerritoryGui(player, guild, manage);
    }

    public void openTerritoryGui(Player player, Guild guild, boolean manageMode) {
        if (settings != null && settings.isGuiEnabled()) {
            context.openGUI(player, new TerritoryManagementGUI(this, guild, player, manageMode));
        } else {
            texts.send(player, "module.territory.gui.disabled", "&c领地 GUI 已在配置中关闭。");
        }
    }

    private boolean canManageTerritory(Player player) {
        return context.getPlugin().getMembershipRules().canManageGuild(player)
                && context.getPlugin().getPermissionManager().hasPermission(player, "guild.territory.claim");
    }

    private Guild extractGuild(Object... ctx) {
        if (ctx != null && ctx.length > 0 && ctx[0] instanceof Guild guild) {
            return guild;
        }
        return null;
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
