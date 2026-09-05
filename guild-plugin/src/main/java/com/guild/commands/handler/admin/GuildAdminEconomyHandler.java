package com.guild.commands.handler.admin;

import com.guild.commands.handler.SubCommandErrors;
import com.guild.core.utils.ColorUtils;
import org.bukkit.command.CommandSender;

public class GuildAdminEconomyHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 4) {
            String usage = ctx.languageManager().getCoreMessage("admin.economy.usage", "&cUsage: /guildadmin economy <guild name> <set|add|remove> <amount>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        String operation = args[2];
        double amount;

        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            String invalidAmount = ctx.languageManager().getCoreMessage("admin.economy.invalid-amount", "&cInvalid amount format!");
            sender.sendMessage(ColorUtils.colorize(invalidAmount));
            return;
        }

        ctx.guildService().getGuildByNameAsync(guildName)
                .thenAccept(guild -> SubCommandErrors.guardAsync(
                        ctx.plugin(), ctx.languageManager(), sender,
                        "admin-economy", "admin.economy.error", "&cFailed to update guild balance.", () -> {
                            if (guild == null) {
                                String notFound = ctx.languageManager().getCoreMessage("admin.economy.not-found", "&cGuild {guild} does not exist!")
                                        .replace("{guild}", guildName);
                                ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                                return;
                            }

                            final double[] newBalance = {guild.getBalance()};
                            switch (operation.toLowerCase()) {
                                case "set" -> newBalance[0] = amount;
                                case "add" -> newBalance[0] += amount;
                                case "remove" -> {
                                    newBalance[0] -= amount;
                                    if (newBalance[0] < 0) {
                                        newBalance[0] = 0;
                                    }
                                }
                                default -> {
                                    String invalidOp = ctx.languageManager().getCoreMessage("admin.economy.invalid-operation", "&cInvalid operation! Use set|add|remove");
                                    ctx.sendMessage(sender, ColorUtils.colorize(invalidOp));
                                    return;
                                }
                            }

                            ctx.guildService().updateGuildBalanceAsync(guild.getId(), newBalance[0])
                                    .thenAccept(success -> SubCommandErrors.guardAsync(
                                            ctx.plugin(), ctx.languageManager(), sender,
                                            "admin-economy-update", "admin.economy.error", "&cFailed to update guild balance.", () -> {
                                                if (success) {
                                                    String formattedAmount = ctx.plugin().getEconomyManager().format(newBalance[0]);
                                                    String successMsg = ctx.languageManager().getCoreMessage("admin.economy.success", "&aGuild {guild} balance has been updated to: {balance}")
                                                            .replace("{guild}", guildName)
                                                            .replace("{balance}", formattedAmount);
                                                    ctx.sendMessage(sender, ColorUtils.colorize(successMsg));
                                                } else {
                                                    String failed = ctx.languageManager().getCoreMessage("admin.economy.failed", "&cFailed to update guild balance!");
                                                    ctx.sendMessage(sender, ColorUtils.colorize(failed));
                                                }
                                            }))
                                    .exceptionally(SubCommandErrors.handleAsyncFailure(
                                            ctx.plugin(), ctx.languageManager(), sender,
                                            "admin-economy-update", "admin.economy.error", "&cFailed to update guild balance."));
                        }))
                .exceptionally(SubCommandErrors.handleAsyncFailure(
                        ctx.plugin(), ctx.languageManager(), sender,
                        "admin-economy", "admin.economy.error", "&cFailed to update guild balance."));
    }
}
