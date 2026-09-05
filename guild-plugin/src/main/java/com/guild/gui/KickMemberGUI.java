package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.base.AbstractPagedMemberGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.entity.Player;

/** 踢出成员 GUI */
public class KickMemberGUI extends AbstractPagedMemberGUI {

    public KickMemberGUI(GuildPlugin plugin, Guild guild, Player player) {
        super(plugin, guild, player);
    }

    @Override
    protected boolean refreshGuiAfterLoad() {
        return false;
    }

    @Override
    protected String titleKey() {
        return "gui.kick-member.title";
    }

    @Override
    protected String titleDefault() {
        return "&6Kick Member - Page {page}";
    }

    @Override
    protected String bedrockTitleKey() {
        return "gui.kick-member.bedrock-title";
    }

    @Override
    protected String bedrockTitleDefault() {
        return "&6Kick Member";
    }

    @Override
    protected String bedrockTitlePageKey() {
        return "gui.kick-member.bedrock-title-page";
    }

    @Override
    protected String bedrockTitlePageDefault() {
        return "&6Kick Member - Page {page}";
    }

    @Override
    protected String bedrockContentKey() {
        return "gui.kick-member.bedrock-content";
    }

    @Override
    protected String bedrockContentDefault() {
        return "&fSelect a member to kick";
    }

    @Override
    protected String bedrockNoMembersKey() {
        return "gui.kick-member.bedrock-no-members";
    }

    @Override
    protected String bedrockNoMembersDefault() {
        return "&fNo members available to kick";
    }

    @Override
    protected String memberDisplayNamePrefix() {
        return "&c";
    }

    @Override
    protected String bedrockMemberButtonPrefix() {
        return "§c";
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
        return "&c";
    }

    @Override
    protected String memberClickLoreKey() {
        return "gui.kick-member.click-kick";
    }

    @Override
    protected String memberClickLoreDefault() {
        return "Click to kick this member";
    }

    @Override
    protected boolean validateAccess(Player player) {
        return plugin.getMembershipRules().canKick(player);
    }

    @Override
    protected void onMemberSelected(Player player, GuildMember member) {
        if (!plugin.getMembershipRules().canKick(player)) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.common.no-permission", "&cInsufficient permission")));
            return;
        }
        plugin.getGuiManager().openGUI(player,
                new ConfirmKickMemberGUI(plugin, guild, member, player, getGuiType()));
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
    }
}
