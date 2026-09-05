package com.guild.commands.handler.admin;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.ConfirmDeleteGuildGUI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GuildAdminDeleteHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = ctx.languageManager().getCoreMessage("admin.delete.usage", "&cUsage: /guildadmin delete <guild name>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        ctx.guildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                String notFound = ctx.languageManager().getCoreMessage("admin.delete.not-found", "&cGuild {guild} does not exist!")
                        .replace("{guild}", guildName);
                ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                return;
            }

            if (sender instanceof Player player) {
                CompatibleScheduler.runTask(ctx.plugin(), player, () -> ctx.plugin().getGuiManager().openGUI(player,
                        new ConfirmDeleteGuildGUI(ctx.plugin(), guild, player, "GuildListManagementGUI", true)));
            } else {
                ctx.guildService().forceDeleteGuildAsync(guild.getId(), null).thenAccept(success -> {
                    if (success) {
                        String successMsg = ctx.languageManager().getCoreMessage("admin.delete.success", "&aGuild {guild} has been forcibly deleted!")
                                .replace("{guild}", guildName);
                        sender.sendMessage(ColorUtils.colorize(successMsg));
                    } else {
                        String failed = ctx.languageManager().getCoreMessage("admin.delete.failed", "&cFailed to delete guild!");
                        sender.sendMessage(ColorUtils.colorize(failed));
                    }
                });
            }
        });
    }
}
