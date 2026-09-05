package com.guild.commands.handler.admin;

import com.guild.core.utils.ColorUtils;
import org.bukkit.command.CommandSender;

public class GuildAdminHelpHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        String title = ctx.languageManager().getCoreMessage("admin.help.title", "&6=== Admin Commands ===");
        sender.sendMessage(ColorUtils.colorize(title));

        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.main", "&e/guildadmin &7- Open admin GUI")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.list", "&e/guildadmin list &7- List all guilds")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.info", "&e/guildadmin info <guild> &7- View guild info")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.delete", "&e/guildadmin delete <guild> &7- Force delete guild")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.freeze", "&e/guildadmin freeze <guild> &7- Freeze guild")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.unfreeze", "&e/guildadmin unfreeze <guild> &7- Unfreeze guild")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.transfer", "&e/guildadmin transfer <guild> <player> &7- Transfer leadership")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.economy", "&e/guildadmin economy <guild> <operation> <amount> &7- Manage guild economy")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.relation", "&e/guildadmin relation <operation> &7- Manage guild relations")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.reload", "&e/guildadmin reload &7- Reload configuration")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.update", "&e/guildadmin update &7- Check for plugin updates")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.update-download", "&e/guildadmin update download &7- Download and install update")));
        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage("admin.help.help", "&e/guildadmin help &7- Show help info")));
    }
}
