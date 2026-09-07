package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.models.Guild;
import com.guild.module.example.territory.gui.ConfirmTerritoryClaimGUI;
import com.guild.module.example.territory.gui.ConfirmTerritoryUnclaimGUI;
import com.guild.module.example.territory.gui.TerritoryManagementGUI;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.gui.GUILayoutDefinition;
import com.guild.sdk.gui.ModuleGUIRegistration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** 领地 GUI 注册与 GuildInfo/Settings 按钮入口。 */
final class TerritoryGuiRegistrar {

    private final TerritoryModule module;
    private final ModuleContext context;
    private final TerritorySettings settings;
    private final TerritoryTexts texts;

    TerritoryGuiRegistrar(TerritoryModule module, ModuleContext context,
                          TerritorySettings settings, TerritoryTexts texts) {
        this.module = module;
        this.context = context;
        this.settings = settings;
        this.texts = texts;
    }

    void register(GuildPluginAPI api) {
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
            return new TerritoryManagementGUI(module, guild, player, manage);
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
            return new ConfirmTerritoryClaimGUI(module, guild, player);
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
            return new ConfirmTerritoryUnclaimGUI(module, guild, player);
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

    void openTerritoryGui(Player player, Guild guild, boolean manageMode) {
        if (settings != null && settings.isGuiEnabled()) {
            context.openGUI(player, new TerritoryManagementGUI(module, guild, player, manageMode));
        } else {
            texts.send(player, "module.territory.gui.disabled", "&c领地 GUI 已在配置中关闭。");
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
        openTerritoryGui(player, guild, canManageTerritory(player));
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
}
