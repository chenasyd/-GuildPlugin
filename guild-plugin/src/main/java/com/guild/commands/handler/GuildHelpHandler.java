package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import org.bukkit.entity.Player;

public class GuildHelpHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                String message = ctx.languageManager().getCoreMessage(player, "help.title", "&a=== Guild System Help ===");
                player.sendMessage(ColorUtils.colorize(message));
        
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.main-menu", "&e/guild &7- Open guild main menu")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.create", "&e/guild create <name> [tag] [description] &7- Create guild")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.info", "&e/guild info &7- View guild information")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.members", "&e/guild members &7- View guild members")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.invite", "&e/guild invite <player> &7- Invite player to join guild")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.kick", "&e/guild kick <player> &7- Kick guild member")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.promote", "&e/guild promote <player> &7- Promote guild member")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.demote", "&e/guild demote <player> &7- Demote guild member")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.accept", "&e/guild accept <inviter> &7- Accept guild invitation")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.decline", "&e/guild decline <inviter> &7- Decline guild invitation")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.leave", "&e/guild leave &7- Leave guild")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.delete", "&e/guild delete &7- Delete guild")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.sethome", "&e/guild sethome &7- Set guild home")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.home", "&e/guild home &7- Teleport to guild home")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.relation", "&e/guild relation &7- Manage guild relations")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.economy", "&e/guild economy &7- Manage guild economy")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.deposit", "&e/guild deposit <amount> &7- Deposit funds to guild")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.withdraw", "&e/guild withdraw <amount> &7- Withdraw funds from guild")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.transfer", "&e/guild transfer <guild> <amount> &7- Transfer funds to another guild")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.logs", "&e/guild logs &7- View guild operation logs")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.placeholder", "&e/guild placeholder <player|guild|rank> &7- Get placeholders")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.time", "&e/guild time &7- View guild time info")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.applications", "&e/guild applications &7- Manage guild applications")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.chat", "&e/guild chat &7- Toggle guild chat mode &7| &e/guild chat <msg> &7- Send guild message")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.warehouse", "&e/guild warehouse [page|perm|info] &7- Open guild warehouse / permissions / info")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "help.help", "&e/guild help &7- Show this help")));

    }

}
