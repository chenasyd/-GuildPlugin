package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.base.AbstractConfirmGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** 确认删除公会 GUI */
public class ConfirmDeleteGuildGUI extends AbstractConfirmGUI {

    private final boolean adminForce;

    public ConfirmDeleteGuildGUI(GuildPlugin plugin, Guild guild, Player player) {
        this(plugin, guild, player, "GuildSettingsGUI", false);
    }

    public ConfirmDeleteGuildGUI(GuildPlugin plugin, Guild guild, Player player, String sourceGuiType) {
        this(plugin, guild, player, sourceGuiType, "GuildListManagementGUI".equals(sourceGuiType));
    }

    public ConfirmDeleteGuildGUI(GuildPlugin plugin, Guild guild, Player player, String sourceGuiType,
                                 boolean adminForce) {
        super(plugin, guild, player, sourceGuiType != null ? sourceGuiType : "GuildSettingsGUI");
        this.adminForce = adminForce;
    }

    @Override
    protected String titleKey() {
        return "gui.confirm-delete-guild.confirm-delete-guild-title";
    }

    @Override
    protected String titleDefault() {
        return "&4Confirm Delete Guild";
    }

    @Override
    protected String bedrockTitleKey() {
        return "gui.confirm-delete-guild.bedrock-title";
    }

    @Override
    protected String bedrockTitleDefault() {
        return "&4Confirm Delete Guild";
    }

    @Override
    protected String bedrockContentKey() {
        return "gui.confirm-delete-guild.bedrock-content";
    }

    @Override
    protected String bedrockContentDefault() {
        return "&fGuild: &e{guild}\n&fAre you sure you want to delete this guild?\n&cThis action will permanently delete the guild!\n&cAll members will be removed!\n&cThis action cannot be undone!";
    }

    @Override
    protected String bedrockConfirmKey() {
        return "gui.confirm-delete-guild.bedrock-confirm";
    }

    @Override
    protected String bedrockConfirmDefault() {
        return "&4Confirm Delete";
    }

    @Override
    protected String bedrockCancelKey() {
        return "gui.confirm-delete-guild.bedrock-cancel";
    }

    @Override
    protected String bedrockCancelDefault() {
        return "&aCancel";
    }

    @Override
    protected Material confirmMaterial() {
        return Material.TNT;
    }

    @Override
    protected String confirmButtonKey() {
        return "gui.confirm-delete-guild.confirm-button";
    }

    @Override
    protected String confirmButtonDefault() {
        return "&4Confirm Delete";
    }

    @Override
    protected String confirmLoreKey() {
        return "gui.confirm-delete-guild.confirm-lore-1";
    }

    @Override
    protected String confirmLoreDefault() {
        return "&7Click to confirm guild deletion";
    }

    @Override
    protected String[] confirmLoreMessages() {
        return new String[]{
                languageManager.getGuiMessage(viewer, confirmLoreKey(), confirmLoreDefault()),
                languageManager.getGuiMessage(viewer,
                        "gui.confirm-delete-guild.confirm-lore-2", "&cThis action cannot be undone!")
        };
    }

    @Override
    protected String cancelButtonKey() {
        return "gui.confirm-delete-guild.cancel-button";
    }

    @Override
    protected String cancelButtonDefault() {
        return "&aCancel";
    }

    @Override
    protected String cancelLoreKey() {
        return "gui.confirm-delete-guild.cancel-lore";
    }

    @Override
    protected String cancelLoreDefault() {
        return "&7Cancel guild deletion";
    }

    @Override
    protected ItemStack createInfoItem() {
        String guildName = ColorUtils.stripColor(guild.getName());
        return createItem(
                Material.BOOK,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.confirm-delete-guild.info-title", "&4Confirm Delete Guild")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.confirm-delete-guild.guild", "&7Guild: &e{guild}", "{guild}", guildName)),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.confirm-delete-guild.confirm-question",
                        "&7Are you sure you want to delete this guild?")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.confirm-delete-guild.warning-1",
                        "&cThis action will permanently delete the guild!")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.confirm-delete-guild.warning-2", "&cAll members will be removed!")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.confirm-delete-guild.warning-3", "&cThis action cannot be undone!"))
        );
    }

    @Override
    protected String[] bedrockContentPlaceholders() {
        return new String[]{"{guild}", ColorUtils.stripColor(guild.getName())};
    }

    @Override
    protected void onConfirm(Player player) {
        if (adminForce) {
            if (!player.hasPermission("guild.admin")) {
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.common.no-permission", "&cInsufficient permission")));
                return;
            }
        } else {
            GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
            if (member == null || member.getGuildId() != guild.getId() || member.getRole() != GuildMember.Role.LEADER) {
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
                return;
            }
        }

        var deleteFuture = adminForce
                ? plugin.getGuildService().forceDeleteGuildAsync(guild.getId(), player.getUniqueId())
                : plugin.getGuildService().deleteGuildAsync(guild.getId(), player.getUniqueId());

        deleteFuture.thenAccept(success -> {
            if (success) {
                String cleanGuildName = ColorUtils.stripColor(guild.getName());
                String message = languageManager.getGuiMessage(player,
                        "gui.confirm-delete-guild.delete.success",
                        "&aGuild {guild} has been deleted!", "{guild}", cleanGuildName);
                CompatibleScheduler.runTask(plugin, player, () -> {
                    player.sendMessage(ColorUtils.colorize(message));
                    plugin.getGuiManager().closeGUI(player);
                    if (adminForce) {
                        plugin.getGuiManager().openGUI(player, new GuildListManagementGUI(plugin, player));
                    } else {
                        plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                    }
                });
            } else {
                String message = languageManager.getGuiMessage(player,
                        "gui.confirm-delete-guild.delete.failed", "&cFailed to delete the guild!");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }

    @Override
    protected void onCancel(Player player) {
        if (adminForce || "GuildListManagementGUI".equals(sourceGuiType)) {
            plugin.getGuiManager().openGUI(player, new GuildListManagementGUI(plugin, player));
        } else if ("GuildDetailGUI".equals(sourceGuiType)) {
            plugin.getGuiManager().openGUI(player, new GuildDetailGUI(plugin, guild, player));
        } else {
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
        }
    }
}
