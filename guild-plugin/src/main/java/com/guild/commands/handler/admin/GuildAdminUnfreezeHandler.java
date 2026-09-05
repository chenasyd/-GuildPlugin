package com.guild.commands.handler.admin;

import com.guild.commands.handler.SubCommandErrors;
import com.guild.core.utils.ColorUtils;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class GuildAdminUnfreezeHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = ctx.languageManager().getCoreMessage("admin.unfreeze.usage", "&cUsage: /guildadmin unfreeze <guild name>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        UUID operatorUuid = sender instanceof org.bukkit.entity.Player p ? p.getUniqueId() : null;
        ctx.guildService().getGuildByNameAsync(guildName)
                .thenAccept(guild -> SubCommandErrors.guardAsync(
                        ctx.plugin(), ctx.languageManager(), sender,
                        "admin-unfreeze", "admin.unfreeze.error", "&cFailed to unfreeze guild.", () -> {
                            if (guild == null) {
                                String notFound = ctx.languageManager().getCoreMessage("admin.unfreeze.not-found", "&cGuild {guild} does not exist!")
                                        .replace("{guild}", guildName);
                                ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                                return;
                            }

                            ctx.guildService().updateGuildFrozenStatusAsync(guild.getId(), false, operatorUuid)
                                    .thenAccept(success -> SubCommandErrors.guardAsync(
                                            ctx.plugin(), ctx.languageManager(), sender,
                                            "admin-unfreeze-update", "admin.unfreeze.error", "&cFailed to unfreeze guild.", () -> {
                                                if (success) {
                                                    String successMsg = ctx.languageManager().getCoreMessage("admin.unfreeze.success", "&aGuild {guild} has been unfrozen!")
                                                            .replace("{guild}", guildName);
                                                    ctx.sendMessage(sender, ColorUtils.colorize(successMsg));
                                                } else {
                                                    String failed = ctx.languageManager().getCoreMessage("admin.unfreeze.failed", "&cFailed to unfreeze guild!");
                                                    ctx.sendMessage(sender, ColorUtils.colorize(failed));
                                                }
                                            }))
                                    .exceptionally(SubCommandErrors.handleAsyncFailure(
                                            ctx.plugin(), ctx.languageManager(), sender,
                                            "admin-unfreeze-update", "admin.unfreeze.error", "&cFailed to unfreeze guild."));
                        }))
                .exceptionally(SubCommandErrors.handleAsyncFailure(
                        ctx.plugin(), ctx.languageManager(), sender,
                        "admin-unfreeze", "admin.unfreeze.error", "&cFailed to unfreeze guild."));
    }
}
