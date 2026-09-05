package com.guild.commands.handler.admin;

import com.guild.commands.handler.SubCommandErrors;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.RelationManagementGUI;
import com.guild.models.Guild;
import com.guild.models.GuildRelation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuildAdminRelationHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = ctx.languageManager().getCoreMessage("admin.relation.usage", "&cUsage: /guildadmin relation <list|create|delete|gui>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "gui" -> {
                if (sender instanceof Player player) {
                    RelationManagementGUI relationGUI = new RelationManagementGUI(ctx.plugin(), player);
                    ctx.plugin().getGuiManager().openGUI(player, relationGUI);
                } else {
                    String playerOnly = ctx.languageManager().getCoreMessage("admin.relation.gui-player-only", "&cThis command can only be executed by a player!");
                    sender.sendMessage(ColorUtils.colorize(playerOnly));
                }
            }
            case "list" -> handleList(ctx, sender);
            case "create" -> {
                if (args.length < 5) {
                    String usage = ctx.languageManager().getCoreMessage("admin.relation.create-usage", "&cUsage: /guildadmin relation create <guild1> <guild2> <relation type>");
                    String types = ctx.languageManager().getCoreMessage("admin.relation.create-types", "&7Relation types: ally|enemy|war|truce|neutral");
                    sender.sendMessage(ColorUtils.colorize(usage));
                    sender.sendMessage(ColorUtils.colorize(types));
                    return;
                }
                handleCreate(ctx, sender, args);
            }
            case "delete" -> {
                if (args.length < 4) {
                    String usage = ctx.languageManager().getCoreMessage("admin.relation.delete-usage", "&cUsage: /guildadmin relation delete <guild1> <guild2>");
                    sender.sendMessage(ColorUtils.colorize(usage));
                    return;
                }
                handleDelete(ctx, sender, args);
            }
            default -> {
                String invalid = ctx.languageManager().getCoreMessage("admin.relation.invalid-operation", "&cInvalid relation operation! Use list|create|delete|gui");
                sender.sendMessage(ColorUtils.colorize(invalid));
            }
        }
    }

    private void handleList(GuildAdminCommandContext ctx, CommandSender sender) {
        String title = ctx.languageManager().getCoreMessage("admin.relation.title", "&6=== Guild Relation List ===");
        sender.sendMessage(ColorUtils.colorize(title));
        ctx.guildService().getAllGuildsAsync().thenCompose(guilds -> {
            List<CompletableFuture<List<GuildRelation>>> relationFutures = new ArrayList<>();

            for (Guild guild : guilds) {
                relationFutures.add(ctx.guildService().getGuildRelationsAsync(guild.getId()));
            }

            return CompletableFuture.allOf(relationFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<GuildRelation> allRelations = new ArrayList<>();
                        for (CompletableFuture<List<GuildRelation>> future : relationFutures) {
                            try {
                                List<GuildRelation> part = future.join();
                                if (part != null) {
                                    allRelations.addAll(part);
                                }
                            } catch (Exception e) {
                                SubCommandErrors.logFailure(ctx.plugin(), "admin-relation-list-fetch", e);
                            }
                        }
                        return allRelations;
                    });
        }).thenAccept(relations -> SubCommandErrors.guardAsync(
                ctx.plugin(), ctx.languageManager(), sender,
                "admin-relation-list", "admin.relation.error", "&cFailed to list guild relations.", () -> {
            if (relations.isEmpty()) {
                String empty = ctx.languageManager().getCoreMessage("admin.relation.empty", "&cNo guild relations");
                ctx.sendMessage(sender, ColorUtils.colorize(empty));
                return;
            }

            for (GuildRelation relation : relations) {
                String status = AdminRelationFormat.getRelationStatusText(ctx.languageManager(), relation.getStatus());
                String type = AdminRelationFormat.getRelationTypeText(ctx.languageManager(), relation.getType());
                String format = ctx.languageManager().getCoreMessage("admin.relation.format", "&e{guild1} ↔ {guild2} &7- {type} &7- {status}")
                        .replace("{guild1}", relation.getGuild1Name())
                        .replace("{guild2}", relation.getGuild2Name())
                        .replace("{type}", type)
                        .replace("{status}", status);
                ctx.sendMessage(sender, ColorUtils.colorize(format));
            }
        })).exceptionally(SubCommandErrors.handleAsyncFailure(
                ctx.plugin(), ctx.languageManager(), sender,
                "admin-relation-list", "admin.relation.error", "&cFailed to list guild relations."));
    }

    private void handleCreate(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        String guild1Name = args[2];
        String guild2Name = args[3];
        String relationTypeStr = args[4];

        GuildRelation.RelationType relationType;
        try {
            relationType = GuildRelation.RelationType.valueOf(relationTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            String invalidType = ctx.languageManager().getCoreMessage("admin.relation.invalid-type", "&cInvalid relation type! Use: ally, enemy, war, truce, neutral");
            sender.sendMessage(ColorUtils.colorize(invalidType));
            return;
        }

        CompletableFuture<Guild> guild1Future = ctx.guildService().getGuildByNameAsync(guild1Name);
        CompletableFuture<Guild> guild2Future = ctx.guildService().getGuildByNameAsync(guild2Name);

        CompletableFuture.allOf(guild1Future, guild2Future).thenAccept(v -> SubCommandErrors.guardAsync(
                ctx.plugin(), ctx.languageManager(), sender,
                "admin-relation-create", "admin.relation.create-error", "&cError creating relation!", () -> {
            Guild guild1 = guild1Future.join();
            Guild guild2 = guild2Future.join();

            if (guild1 == null) {
                String notFound = ctx.languageManager().getCoreMessage("admin.relation.not-found-guild", "&cGuild {guild} not found!")
                        .replace("{guild}", guild1Name);
                ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                return;
            }
            if (guild2 == null) {
                String notFound = ctx.languageManager().getCoreMessage("admin.relation.not-found-guild", "&cGuild {guild} not found!")
                        .replace("{guild}", guild2Name);
                ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                return;
            }
            if (guild1.getId() == guild2.getId()) {
                String cantSelf = ctx.languageManager().getCoreMessage("admin.relation.cannot-relation-self", "&cCannot create a relation with the same guild!");
                ctx.sendMessage(sender, ColorUtils.colorize(cantSelf));
                return;
            }

            ctx.guildService().createGuildRelationAsync(
                    guild1.getId(), guild2.getId(),
                    guild1.getName(), guild2.getName(),
                    relationType, UUID.randomUUID(), "Admin"
            ).thenAccept(success -> SubCommandErrors.guardAsync(
                    ctx.plugin(), ctx.languageManager(), sender,
                    "admin-relation-create-save", "admin.relation.create-error", "&cError creating relation!", () -> {
                        if (success) {
                            String typeText = AdminRelationFormat.getRelationTypeText(ctx.languageManager(), relationType);
                            String successMsg = ctx.languageManager().getCoreMessage("admin.relation.create-success", "&aCreated relation: {guild1} ↔ {guild2} ({type})")
                                    .replace("{guild1}", guild1Name)
                                    .replace("{guild2}", guild2Name)
                                    .replace("{type}", typeText);
                            ctx.sendMessage(sender, ColorUtils.colorize(successMsg));
                        } else {
                            String failed = ctx.languageManager().getCoreMessage("admin.relation.create-failed", "&cFailed to create relation!");
                            ctx.sendMessage(sender, ColorUtils.colorize(failed));
                        }
                    }))
            .exceptionally(SubCommandErrors.handleAsyncFailure(
                    ctx.plugin(), ctx.languageManager(), sender,
                    "admin-relation-create-save", "admin.relation.create-error", "&cError creating relation!"));
        })).exceptionally(SubCommandErrors.handleAsyncFailure(
                ctx.plugin(), ctx.languageManager(), sender,
                "admin-relation-create", "admin.relation.create-error", "&cError creating relation!"));
    }

    private void handleDelete(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        String guild1Name = args[2];
        String guild2Name = args[3];

        CompletableFuture<Guild> guild1Future = ctx.guildService().getGuildByNameAsync(guild1Name);
        CompletableFuture<Guild> guild2Future = ctx.guildService().getGuildByNameAsync(guild2Name);

        CompletableFuture.allOf(guild1Future, guild2Future).thenAccept(v -> SubCommandErrors.guardAsync(
                ctx.plugin(), ctx.languageManager(), sender,
                "admin-relation-delete", "admin.relation.delete-error", "&cError deleting relation!", () -> {
            Guild guild1 = guild1Future.join();
            Guild guild2 = guild2Future.join();

            if (guild1 == null) {
                String notFound = ctx.languageManager().getCoreMessage("admin.relation.not-found-guild", "&cGuild {guild} not found!")
                        .replace("{guild}", guild1Name);
                ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                return;
            }
            if (guild2 == null) {
                String notFound = ctx.languageManager().getCoreMessage("admin.relation.not-found-guild", "&cGuild {guild} not found!")
                        .replace("{guild}", guild2Name);
                ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                return;
            }

            ctx.guildService().getGuildRelationsAsync(guild1.getId()).thenAccept(relations -> SubCommandErrors.guardAsync(
                    ctx.plugin(), ctx.languageManager(), sender,
                    "admin-relation-delete-find", "admin.relation.delete-error", "&cError deleting relation!", () -> {
                        for (GuildRelation relation : relations) {
                            if ((relation.getGuild1Id() == guild1.getId() && relation.getGuild2Id() == guild2.getId())
                                    || (relation.getGuild1Id() == guild2.getId() && relation.getGuild2Id() == guild1.getId())) {

                                ctx.guildService().deleteGuildRelationAsync(relation.getId())
                                        .thenAccept(success -> SubCommandErrors.guardAsync(
                                                ctx.plugin(), ctx.languageManager(), sender,
                                                "admin-relation-delete-save", "admin.relation.delete-error", "&cError deleting relation!", () -> {
                                                    if (success) {
                                                        String successMsg = ctx.languageManager().getCoreMessage("admin.relation.delete-success", "&aDeleted relation: {guild1} ↔ {guild2}")
                                                                .replace("{guild1}", guild1Name)
                                                                .replace("{guild2}", guild2Name);
                                                        ctx.sendMessage(sender, ColorUtils.colorize(successMsg));
                                                    } else {
                                                        String failed = ctx.languageManager().getCoreMessage("admin.relation.delete-failed", "&cFailed to delete relation!");
                                                        ctx.sendMessage(sender, ColorUtils.colorize(failed));
                                                    }
                                                }))
                                        .exceptionally(SubCommandErrors.handleAsyncFailure(
                                                ctx.plugin(), ctx.languageManager(), sender,
                                                "admin-relation-delete-save", "admin.relation.delete-error", "&cError deleting relation!"));
                                return;
                            }
                        }
                        String notFound = ctx.languageManager().getCoreMessage("admin.relation.not-found", "&cNo relation found between {guild1} and {guild2}!")
                                .replace("{guild1}", guild1Name)
                                .replace("{guild2}", guild2Name);
                        ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                    }))
            .exceptionally(SubCommandErrors.handleAsyncFailure(
                    ctx.plugin(), ctx.languageManager(), sender,
                    "admin-relation-delete-find", "admin.relation.delete-error", "&cError deleting relation!"));
        })).exceptionally(SubCommandErrors.handleAsyncFailure(
                ctx.plugin(), ctx.languageManager(), sender,
                "admin-relation-delete", "admin.relation.delete-error", "&cError deleting relation!"));
    }
}
