package com.guild.util;

import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;

public final class InviteMessageUtils {

    private InviteMessageUtils() {}

    public static String formatInviteSent(GuildPlugin plugin, Player inviter, Player target) {
        String template = plugin.getLanguageManager().getMessage("invite.sent", "&a已向 &e{player} &a发送邀请！", "{player}", target.getName());
        return ColorUtils.colorize(template);
    }

    public static String formatInviteReceived(GuildPlugin plugin, Player inviter, Guild guild) {
        String template = plugin.getLanguageManager().getMessage("invite.received", "&e{inviter} 邀请您加入工会: {guild}", "{inviter}", inviter.getName(), "{guild}", guild.getName());
        return ColorUtils.colorize(template);
    }

    public static String formatInviteFailed(GuildPlugin plugin) {
        String template = plugin.getLanguageManager().getMessage("invite.failed", "&c邀请发送失败！");
        return ColorUtils.colorize(template);
    }

    public static String formatAlreadyInGuild(GuildPlugin plugin, String playerName) {
        String template = plugin.getLanguageManager().getMessage("invite.already-in-guild", "&c玩家 {player} 已经加入了其他工会！", "{player}", playerName);
        return ColorUtils.colorize(template);
    }

    public static String formatInviteTitle(GuildPlugin plugin) {
        return ColorUtils.colorize(plugin.getLanguageManager().getMessage("invite.title", "&6=== 工会邀请 ==="));
    }
}
