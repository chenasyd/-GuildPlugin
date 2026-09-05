package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.base.AbstractConfirmGUI;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** 确认离开公会 GUI */
public class ConfirmLeaveGuildGUI extends AbstractConfirmGUI {

    public ConfirmLeaveGuildGUI(GuildPlugin plugin, Guild guild, Player player, String sourceGuiType) {
        super(plugin, guild, player, sourceGuiType != null ? sourceGuiType : "GuildSettingsGUI");
    }

    /**
     * @deprecated 使用 {@link #ConfirmLeaveGuildGUI(GuildPlugin, Guild, Player, String)} 传递来源标识
     */
    @Deprecated
    public ConfirmLeaveGuildGUI(GuildPlugin plugin, Guild guild, Player player) {
        this(plugin, guild, player, "GuildSettingsGUI");
    }

    @Override
    protected String titleKey() {
        return "gui.confirm-leave-guild.title";
    }

    @Override
    protected String titleDefault() {
        return "&cConfirm Leave Guild";
    }

    @Override
    protected String bedrockTitleKey() {
        return "gui.confirm-leave-guild.bedrock-title";
    }

    @Override
    protected String bedrockTitleDefault() {
        return "&cConfirm Leave Guild";
    }

    @Override
    protected String bedrockContentKey() {
        return "gui.confirm-leave-guild.bedrock-content";
    }

    @Override
    protected String bedrockContentDefault() {
        return "&fGuild: &e{guild}\n&fAre you sure you want to leave this guild?\n&cThis action cannot be undone!";
    }

    @Override
    protected String bedrockConfirmKey() {
        return "gui.confirm-leave-guild.bedrock-confirm";
    }

    @Override
    protected String bedrockConfirmDefault() {
        return "&cConfirm Leave";
    }

    @Override
    protected String bedrockCancelKey() {
        return "gui.confirm-leave-guild.bedrock-cancel";
    }

    @Override
    protected String bedrockCancelDefault() {
        return "&aCancel";
    }

    @Override
    protected Material confirmMaterial() {
        return Material.REDSTONE_BLOCK;
    }

    @Override
    protected String confirmButtonKey() {
        return "gui.confirm-leave-guild.confirm-button";
    }

    @Override
    protected String confirmButtonDefault() {
        return "&cConfirm Leave";
    }

    @Override
    protected String confirmLoreKey() {
        return "gui.confirm-leave-guild.confirm-lore";
    }

    @Override
    protected String confirmLoreDefault() {
        return "&7Click to confirm leaving guild";
    }

    @Override
    protected String cancelButtonKey() {
        return "gui.confirm-leave-guild.cancel-button";
    }

    @Override
    protected String cancelButtonDefault() {
        return "&aCancel";
    }

    @Override
    protected String cancelLoreKey() {
        return "gui.confirm-leave-guild.cancel-lore";
    }

    @Override
    protected String cancelLoreDefault() {
        return "&7Cancel leaving guild";
    }

    @Override
    protected ItemStack createInfoItem() {
        String guildName = ColorUtils.stripColor(guild.getName());
        return createItem(
                Material.BOOK,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.confirm-leave-guild.info-title", "&cConfirm Leave Guild")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.confirm-leave-guild.guild", "&7Guild: &e{guild}", "{guild}", guildName)),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.confirm-leave-guild.confirm-question",
                        "&7Are you sure you want to leave this guild?")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.confirm-leave-guild.warning", "&cThis action cannot be undone!"))
        );
    }

    @Override
    protected String[] bedrockContentPlaceholders() {
        return new String[]{"{guild}", ColorUtils.stripColor(guild.getName())};
    }

    @Override
    protected void onConfirm(Player player) {
        if (player.getUniqueId().equals(guild.getLeaderUuid())) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.confirm-leave-guild.leave.leader-cannot-leave",
                    "&cGuild leader cannot leave the guild!")));
            return;
        }

        plugin.getGuildService().removeGuildMemberAsync(player.getUniqueId(), player.getUniqueId())
                .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.confirm-leave-guild.leave.success",
                                "&aYou have successfully left the guild: {guild}",
                                "{guild}", guild.getName())));
                        player.closeInventory();
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.confirm-leave-guild.leave.failed", "&cFailed to leave the guild!")));
                    }
                }));
    }

    @Override
    protected void onCancel(Player player) {
        if ("MemberGuildGUI".equals(sourceGuiType)) {
            plugin.getGuiManager().openGUI(player, new MemberGuildGUI(plugin, guild, player));
        } else {
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
        }
    }
}
