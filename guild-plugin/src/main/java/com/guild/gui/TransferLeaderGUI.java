package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.base.AbstractPagedMemberGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.entity.Player;

/** 转移会长 - 选择目标成员 GUI */
public class TransferLeaderGUI extends AbstractPagedMemberGUI {

    public TransferLeaderGUI(GuildPlugin plugin, Guild guild, Player player) {
        super(plugin, guild, player);
    }

    @Override
    protected String titleKey() {
        return "gui.transfer-leader.title";
    }

    @Override
    protected String titleDefault() {
        return "&6Transfer Leadership - Page {page}";
    }

    @Override
    protected String bedrockTitleKey() {
        return "gui.transfer-leader.bedrock-title";
    }

    @Override
    protected String bedrockTitleDefault() {
        return "&6Transfer Leadership";
    }

    @Override
    protected String bedrockTitlePageKey() {
        return "gui.transfer-leader.bedrock-title-page";
    }

    @Override
    protected String bedrockTitlePageDefault() {
        return "&6Transfer Leadership - Page {page}";
    }

    @Override
    protected String bedrockContentKey() {
        return "gui.transfer-leader.bedrock-content";
    }

    @Override
    protected String bedrockContentDefault() {
        return "&fSelect a member to transfer leadership to";
    }

    @Override
    protected String bedrockNoMembersKey() {
        return "gui.transfer-leader.bedrock-no-members";
    }

    @Override
    protected String bedrockNoMembersDefault() {
        return "&fNo members available to transfer leadership to";
    }

    @Override
    protected String memberDisplayNamePrefix() {
        return "&e";
    }

    @Override
    protected String bedrockMemberButtonPrefix() {
        return "§e";
    }

    @Override
    protected String memberPositionLoreKey() {
        return "gui.common.member-operation.position";
    }

    @Override
    protected String memberPositionLoreDefault() {
        return "Position";
    }

    @Override
    protected String memberClickLoreColorPrefix() {
        return "&e";
    }

    @Override
    protected String memberClickLoreKey() {
        return "gui.transfer-leader.click-transfer";
    }

    @Override
    protected String memberClickLoreDefault() {
        return "Click to transfer leadership";
    }

    @Override
    protected boolean validateAccess(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        return member != null
                && member.getGuildId() == guild.getId()
                && member.getRole() == GuildMember.Role.LEADER
                && player.getUniqueId().equals(guild.getLeaderUuid());
    }

    @Override
    protected void onUnauthorizedAccess(Player player) {
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
    }

    @Override
    protected void onMemberSelected(Player player, GuildMember member) {
        plugin.getGuiManager().openGUI(player,
                new ConfirmTransferLeaderGUI(plugin, guild, member, player, getGuiType(), false));
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
    }
}
