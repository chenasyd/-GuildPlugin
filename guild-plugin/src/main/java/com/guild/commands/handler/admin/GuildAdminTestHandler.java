package com.guild.commands.handler.admin;

import com.guild.core.utils.ColorUtils;
import com.guild.gui.AdminGuildGUI;
import com.guild.models.GuildRelation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GuildAdminTestHandler implements GuildAdminSubCommandHandler {

    private final GuildAdminTestLangHandler langHandler = new GuildAdminTestLangHandler();

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = ctx.languageManager().getCoreMessage("admin.test.usage", "&cUsage: /guildadmin test <test-type>");
            String types = ctx.languageManager().getCoreMessage("admin.test.types", "&7test-type: gui, economy, relation");
            sender.sendMessage(ColorUtils.colorize(usage));
            sender.sendMessage(ColorUtils.colorize(types));
            return;
        }

        String testType = args[1];
        switch (testType.toLowerCase()) {
            case "gui" -> {
                if (sender instanceof Player player) {
                    AdminGuildGUI adminGUI = new AdminGuildGUI(ctx.plugin(), player);
                    ctx.plugin().getGuiManager().openGUI(player, adminGUI);
                    String success = ctx.languageManager().getCoreMessage("admin.test.gui-success", "&aAdmin GUI opened for testing.");
                    sender.sendMessage(ColorUtils.colorize(success));
                } else {
                    String playerOnly = ctx.languageManager().getCoreMessage("admin.test.gui-player-only", "&cThis command can only be executed by a player!");
                    sender.sendMessage(ColorUtils.colorize(playerOnly));
                }
            }
            case "economy" -> {
                if (args.length < 4) {
                    String usage = ctx.languageManager().getCoreMessage("admin.test.economy-usage", "&cUsage: /guildadmin test economy <guild name> <operation> <amount>");
                    sender.sendMessage(ColorUtils.colorize(usage));
                    return;
                }
                String guildName = args[2];
                String operation = args[3];
                double amount;
                try {
                    amount = Double.parseDouble(args[4]);
                } catch (NumberFormatException e) {
                    String invalid = ctx.languageManager().getCoreMessage("admin.economy.invalid-amount", "&cInvalid amount format!");
                    sender.sendMessage(ColorUtils.colorize(invalid));
                    return;
                }
                ctx.guildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
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
                    ctx.guildService().updateGuildBalanceAsync(guild.getId(), newBalance[0]).thenAccept(success -> {
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
                    });
                });
            }
            case "relation" -> {
                if (args.length < 5) {
                    String usage = ctx.languageManager().getCoreMessage("admin.test.relation-usage", "&cUsage: /guildadmin test relation create <guild1> <guild2> <relation type>");
                    String types = ctx.languageManager().getCoreMessage("admin.relation.create-types", "&7Relation types: ally|enemy|war|truce|neutral");
                    sender.sendMessage(ColorUtils.colorize(usage));
                    sender.sendMessage(ColorUtils.colorize(types));
                    return;
                }
                String guild1NameTest = args[2];
                String guild2NameTest = args[3];
                String relationTypeStrTest = args[4];
                GuildRelation.RelationType relationTypeTest;
                try {
                    relationTypeTest = GuildRelation.RelationType.valueOf(relationTypeStrTest.toUpperCase());
                } catch (IllegalArgumentException e) {
                    String invalid = ctx.languageManager().getCoreMessage("admin.relation.invalid-type", "&cInvalid relation type! Use: ally, enemy, war, truce, neutral");
                    sender.sendMessage(ColorUtils.colorize(invalid));
                    return;
                }
                ctx.guildService().getGuildByNameAsync(guild1NameTest).thenAccept(guild1 -> {
                    if (guild1 == null) {
                        String notFound = ctx.languageManager().getCoreMessage("admin.relation.not-found-guild", "&cGuild {guild} does not exist!")
                                .replace("{guild}", guild1NameTest);
                        ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                        return;
                    }
                    ctx.guildService().getGuildByNameAsync(guild2NameTest).thenAccept(guild2 -> {
                        if (guild2 == null) {
                            String notFound = ctx.languageManager().getCoreMessage("admin.relation.not-found-guild", "&cGuild {guild} does not exist!")
                                    .replace("{guild}", guild2NameTest);
                            ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                            return;
                        }
                        if (guild1.getId() == guild2.getId()) {
                            String cantSelf = ctx.languageManager().getCoreMessage("admin.relation.cannot-relation-self", "&cCannot establish a relation with yourself!");
                            ctx.sendMessage(sender, ColorUtils.colorize(cantSelf));
                            return;
                        }
                        ctx.guildService().createGuildRelationAsync(
                                guild1.getId(), guild2.getId(),
                                guild1.getName(), guild2.getName(),
                                relationTypeTest, UUID.randomUUID(), "管理员"
                        ).thenAccept(success -> {
                            if (success) {
                                String typeText = AdminRelationFormat.getRelationTypeText(ctx.languageManager(), relationTypeTest);
                                String successMsg = ctx.languageManager().getCoreMessage("admin.relation.create-success", "&aRelation created: {guild1} ↔ {guild2} ({type})")
                                        .replace("{guild1}", guild1NameTest)
                                        .replace("{guild2}", guild2NameTest)
                                        .replace("{type}", typeText);
                                ctx.sendMessage(sender, ColorUtils.colorize(successMsg));
                            } else {
                                String failed = ctx.languageManager().getCoreMessage("admin.relation.create-failed", "&cFailed to create relation!");
                                ctx.sendMessage(sender, ColorUtils.colorize(failed));
                            }
                        });
                    });
                });
            }
            case "lang" -> langHandler.handle(ctx, sender, args);
            default -> {
                String invalid = ctx.languageManager().getCoreMessage("admin.test.invalid-type", "&cInvalid test type! Use gui, economy, relation");
                sender.sendMessage(ColorUtils.colorize(invalid));
            }
        }
    }
}
