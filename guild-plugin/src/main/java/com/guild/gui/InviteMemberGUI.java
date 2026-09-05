package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.base.AbstractPagedPlayerGUI;
import com.guild.models.Guild;
import com.guild.models.GuildInvitation;
import com.guild.util.InviteMessageUtils;
import com.guild.util.NotifyUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** 邀请成员 GUI */
public class InviteMemberGUI extends AbstractPagedPlayerGUI {

    public InviteMemberGUI(GuildPlugin plugin, Guild guild, Player player) {
        super(plugin, guild, player);
    }

    @Override
    protected List<Player> loadEntries() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(guild.getLeaderUuid()))
                .collect(Collectors.toList());
    }

    @Override
    protected String titleKey() {
        return "gui.invite-member.title";
    }

    @Override
    protected String titleDefault() {
        return "&6邀请成员 - 第{page}页";
    }

    @Override
    protected String bedrockTitleKey() {
        return "gui.invite-member.bedrock-title";
    }

    @Override
    protected String bedrockTitleDefault() {
        return "&6Invite Members";
    }

    @Override
    protected String bedrockTitlePageKey() {
        return "gui.invite-member.bedrock-title-page";
    }

    @Override
    protected String bedrockTitlePageDefault() {
        return "&6Invite Members - Page {page}";
    }

    @Override
    protected String bedrockContentKey() {
        return "gui.invite-member.bedrock-player-list";
    }

    @Override
    protected String bedrockContentDefault() {
        return "&fOnline Players (Total {count})";
    }

    @Override
    protected String bedrockEmptyKey() {
        return "gui.invite-member.bedrock-no-players";
    }

    @Override
    protected String bedrockEmptyDefault() {
        return "&fNo online players available to invite";
    }

    @Override
    protected String bedrockMemberButtonPrefix() {
        return "§a";
    }

    @Override
    protected String backLoreKey() {
        return "gui.invite-member.back-to-settings";
    }

    @Override
    protected String backLoreDefault() {
        return "Return to guild settings";
    }

    @Override
    protected String[] bedrockContentPlaceholders() {
        return new String[]{"{count}", String.valueOf(entries.size())};
    }

    @Override
    protected ItemStack createEntryItem(Player target) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.setDisplayName(ColorUtils.colorize("&a" + target.getName()));
            meta.setLore(Arrays.asList(
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                            "gui.invite-member.click-invite", "Click to invite this player")),
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                            "gui.invite-member.join-guild", "Join guild"))
            ));
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    protected void onEntrySelected(Player inviter, Player target) {
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), inviter.getUniqueId()).thenAccept(inviterMember -> {
            if (inviterMember == null || !plugin.getMembershipRules().canInvite(inviterMember)) {
                CompatibleScheduler.runTask(plugin, inviter, () -> inviter.sendMessage(ColorUtils.colorize(
                        plugin.getLanguageManager().getGuiMessage(inviter, "gui.common.no-permission",
                                "&cInsufficient permission"))));
                return;
            }

        plugin.getGuildService().getGuildMemberAsync(target.getUniqueId()).thenAccept(member -> {
            if (member != null) {
                CompatibleScheduler.runTask(plugin, inviter, () -> inviter.sendMessage(
                        InviteMessageUtils.formatAlreadyInGuild(plugin, inviter, target.getName())));
                return;
            }

            plugin.getGuildService().sendInvitationAsync(guild.getId(), inviter.getUniqueId(), inviter.getName(),
                            target.getUniqueId(), target.getName())
                    .thenAccept(success -> CompatibleScheduler.runTask(plugin, inviter, () -> {
                        if (success) {
                            inviter.sendMessage(InviteMessageUtils.formatInviteSent(plugin, inviter, target));

                            GuildInvitation invitation = new GuildInvitation(guild.getId(), inviter.getUniqueId(),
                                    inviter.getName(), target.getUniqueId(), target.getName());
                            NotifyUtils.sendInviteWithClickableAction(plugin, target, inviter, guild, invitation);
                        } else {
                            inviter.sendMessage(InviteMessageUtils.formatInviteFailed(plugin, inviter));
                        }
                    }));
        });
        });
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
    }
}
