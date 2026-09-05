package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.base.AbstractPagedMemberGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/** 提升成员 GUI */
public class PromoteMemberGUI extends AbstractPagedMemberGUI {

    public PromoteMemberGUI(GuildPlugin plugin, Guild guild, Player player) {
        super(plugin, guild, player);
    }

    @Override
    protected List<GuildMember> filterMembers(List<GuildMember> allMembers) {
        return allMembers.stream()
                .filter(m -> !m.getPlayerUuid().equals(guild.getLeaderUuid()))
                .filter(m -> m.getRole() != GuildMember.Role.OFFICER)
                .collect(Collectors.toList());
    }

    @Override
    protected String titleKey() {
        return "gui.promote-member.title";
    }

    @Override
    protected String titleDefault() {
        return "&6Promote Member - Page {page}";
    }

    @Override
    protected String bedrockTitleKey() {
        return "gui.promote-member.bedrock-title";
    }

    @Override
    protected String bedrockTitleDefault() {
        return "&6Promote Member";
    }

    @Override
    protected String bedrockTitlePageKey() {
        return "gui.promote-member.bedrock-title-page";
    }

    @Override
    protected String bedrockTitlePageDefault() {
        return "&6Promote Member - Page {page}";
    }

    @Override
    protected String bedrockContentKey() {
        return "gui.promote-member.bedrock-content";
    }

    @Override
    protected String bedrockContentDefault() {
        return "&fSelect a member to promote to officer";
    }

    @Override
    protected String bedrockNoMembersKey() {
        return "gui.promote-member.bedrock-no-members";
    }

    @Override
    protected String bedrockNoMembersDefault() {
        return "&fNo members available to promote";
    }

    @Override
    protected String memberDisplayNamePrefix() {
        return "&6";
    }

    @Override
    protected String bedrockMemberButtonPrefix() {
        return "§6";
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
        return "&6";
    }

    @Override
    protected String memberClickLoreKey() {
        return "gui.promote-member.click-promote";
    }

    @Override
    protected String memberClickLoreDefault() {
        return "Click to promote to officer";
    }

    @Override
    protected void onMemberSelected(Player player, GuildMember member) {
        GuildMember executor = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (executor == null || executor.getGuildId() != guild.getId() || !plugin.getMembershipRules().canPromote(executor)) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
            return;
        }
        plugin.getGuiManager().openGUI(player,
                new ConfirmPromoteMemberGUI(plugin, guild, member, player, getGuiType()));
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
    }
}
