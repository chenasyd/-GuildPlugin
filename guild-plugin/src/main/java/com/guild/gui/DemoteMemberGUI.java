package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.base.AbstractPagedMemberGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/** 降级成员 GUI */
public class DemoteMemberGUI extends AbstractPagedMemberGUI {

    public DemoteMemberGUI(GuildPlugin plugin, Guild guild, Player player) {
        super(plugin, guild, player);
    }

    @Override
    protected List<GuildMember> filterMembers(List<GuildMember> allMembers) {
        return allMembers.stream()
                .filter(m -> !m.getPlayerUuid().equals(guild.getLeaderUuid()))
                .filter(m -> m.getRole() == GuildMember.Role.OFFICER)
                .collect(Collectors.toList());
    }

    @Override
    protected String titleKey() {
        return "gui.demote-member.title";
    }

    @Override
    protected String titleDefault() {
        return "&6Demote Member - Page {page}";
    }

    @Override
    protected String bedrockTitleKey() {
        return "gui.demote-member.bedrock-title";
    }

    @Override
    protected String bedrockTitleDefault() {
        return "&6Demote Member";
    }

    @Override
    protected String bedrockTitlePageKey() {
        return "gui.demote-member.bedrock-title-page";
    }

    @Override
    protected String bedrockTitlePageDefault() {
        return "&6Demote Member - Page {page}";
    }

    @Override
    protected String bedrockContentKey() {
        return "gui.demote-member.bedrock-content";
    }

    @Override
    protected String bedrockContentDefault() {
        return "&fSelect an officer to demote to member";
    }

    @Override
    protected String bedrockNoMembersKey() {
        return "gui.demote-member.bedrock-no-members";
    }

    @Override
    protected String bedrockNoMembersDefault() {
        return "&fNo officers available to demote";
    }

    @Override
    protected String memberDisplayNamePrefix() {
        return "&7";
    }

    @Override
    protected String bedrockMemberButtonPrefix() {
        return "§f";
    }

    @Override
    protected String memberPositionLoreKey() {
        return "gui.common.member-operation.current-position";
    }

    @Override
    protected String memberPositionLoreDefault() {
        return "Current position";
    }

    @Override
    protected String memberClickLoreColorPrefix() {
        return "&7";
    }

    @Override
    protected String memberClickLoreKey() {
        return "gui.demote-member.click-demote";
    }

    @Override
    protected String memberClickLoreDefault() {
        return "Click to demote to member";
    }

    @Override
    protected void onMemberSelected(Player player, GuildMember member) {
        GuildMember executor = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (executor == null || executor.getGuildId() != guild.getId() || !plugin.getMembershipRules().canDemote(executor)) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
            return;
        }
        plugin.getGuiManager().openGUI(player,
                new ConfirmDemoteMemberGUI(plugin, guild, member, player, getGuiType()));
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
    }
}
