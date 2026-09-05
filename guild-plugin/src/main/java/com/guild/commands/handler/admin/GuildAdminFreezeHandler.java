package com.guild.commands.handler.admin;

import com.guild.commands.handler.SubCommandErrors;
import com.guild.core.utils.ColorUtils;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class GuildAdminFreezeHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = ctx.languageManager().getCoreMessage("admin.freeze.usage", "&cUsage: /guildadmin freeze <guild name>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        UUID operatorUuid = sender instanceof org.bukkit.entity.Player p ? p.getUniqueId() : null;
        ctx.guildService().getGuildByNameAsync(guildName)
                .thenAccept(guild -> SubCommandErrors.guardAsync(
                        ctx.plugin(), ctx.languageManager(), sender,
                        "admin-freeze", "admin.freeze.error", "&cFailed to freeze guild.", () -> {
                            if (guild == null) {
                                String notFound = ctx.languageManager().getCoreMessage("admin.freeze.not-found", "&cGuild {guild} does not exist!")
                                        .replace("{guild}", guildName);
                                ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                                return;
                            }

                            ctx.guildService().updateGuildFrozenStatusAsync(guild.getId(), true, operatorUuid)
                                    .thenAccept(success -> SubCommandErrors.guardAsync(
                                            ctx.plugin(), ctx.languageManager(), sender,
                                            "admin-freeze-update", "admin.freeze.error", "&cFailed to freeze guild.", () -> {
                                                if (success) {
                                                    String successMsg = ctx.languageManager().getCoreMessage("admin.freeze.success", "&aGuild {guild} has been frozen!")
                                                            .replace("{guild}", guildName);
                                                    ctx.sendMessage(sender, ColorUtils.colorize(successMsg));
                                                } else {
                                                    String failed = ctx.languageManager().getCoreMessage("admin.freeze.failed", "&cFailed to freeze guild!");
                                                    ctx.sendMessage(sender, ColorUtils.colorize(failed));
                                                }
                                            }))
                                    .exceptionally(SubCommandErrors.handleAsyncFailure(
                                            ctx.plugin(), ctx.languageManager(), sender,
                                            "admin-freeze-update", "admin.freeze.error", "&cFailed to freeze guild."));
                        }))
                .exceptionally(SubCommandErrors.handleAsyncFailure(
                        ctx.plugin(), ctx.languageManager(), sender,
                        "admin-freeze", "admin.freeze.error", "&cFailed to freeze guild."));
    }
}
