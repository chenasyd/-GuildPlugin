package com.guild.util;

import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;

public final class InviteMessageUtils {

    private InviteMessageUtils() {}

    public static String formatInviteSent(GuildPlugin plugin, Player inviter, Player target) {
        String template = plugin.getLanguageManager().getMessage(inviter, "invite.sent",
            "&a已向 &e{player} &a发送邀请！",
            "{player}", target.getName());
        return ColorUtils.colorize(template);
    }

    public static String formatInviteReceived(GuildPlugin plugin, Player receiver, Player inviter, Guild guild) {
        String template = plugin.getLanguageManager().getMessage(receiver, "invite.received",
            "&e{inviter} 邀请您加入工会: {guild}",
            "{inviter}", inviter.getName(), "{guild}", guild.getName());
        return ColorUtils.colorize(template);
    }

    public static String formatInviteFailed(GuildPlugin plugin, Player player) {
        String template = plugin.getLanguageManager().getMessage(player, "invite.failed",
            "&c邀请发送失败！");
        return ColorUtils.colorize(template);
    }

    public static String formatAlreadyInGuild(GuildPlugin plugin, Player player, String playerName) {
        String template = plugin.getLanguageManager().getMessage(player, "invite.already-in-guild",
            "&c玩家 {player} 已经加入了其他工会！",
            "{player}", playerName);
        return ColorUtils.colorize(template);
    }

    public static String formatInviteTitle(GuildPlugin plugin, Player player) {
        return ColorUtils.colorize(plugin.getLanguageManager().getMessage(player, "invite.title",
            "&6=== 工会邀请 ==="));
    }
}
