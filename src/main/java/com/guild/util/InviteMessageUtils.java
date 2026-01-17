package com.guild.util;

import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;

public final class InviteMessageUtils {

    private InviteMessageUtils() {}

    public static String formatInviteSent(GuildPlugin plugin, Player inviter, Player target) {
        String template = plugin.getConfigManager().getMessagesConfig().getString("invite.sent", "&a已向 &e{player} &a发送邀请！");
        return ColorUtils.colorize(template.replace("{player}", target.getName()));
    }

    public static String formatInviteReceived(GuildPlugin plugin, Player inviter, Guild guild) {
        String template = plugin.getConfigManager().getMessagesConfig().getString("invite.received", "&e{inviter} 邀请您加入工会: {guild}");
        return ColorUtils.colorize(template.replace("{inviter}", inviter.getName()).replace("{guild}", guild.getName()));
    }

    public static String formatInviteFailed(GuildPlugin plugin) {
        String template = plugin.getConfigManager().getMessagesConfig().getString("invite.failed", "&c邀请发送失败！");
        return ColorUtils.colorize(template);
    }

    public static String formatAlreadyInGuild(GuildPlugin plugin, String playerName) {
        String template = plugin.getConfigManager().getMessagesConfig().getString("invite.already-in-guild", "&c玩家 {player} 已经加入了其他工会！");
        return ColorUtils.colorize(template.replace("{player}", playerName));
    }

    public static String formatInviteTitle(GuildPlugin plugin) {
        return ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("invite.title", "&6=== 工会邀请 ==="));
    }
}
