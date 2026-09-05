package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.ConfirmDeleteGuildGUI;
import com.guild.models.Guild;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public class GuildDeleteHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("confirm")) {
                handleDeleteConfirm(ctx, player);
            } else if (args[1].equalsIgnoreCase("cancel")) {
                handleDeleteCancel(ctx, player);
            } else {
                handleDelete(ctx, player);
            }
        } else {
            handleDelete(ctx, player);
        }
    }

    private void handleDelete(GuildCommandContext ctx, Player player) {
                if (!ctx.plugin().getMembershipRules().canDeleteGuild(player)) {
                    String message = ctx.languageManager().getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.delete.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.plugin().getMembershipRules().canDeleteGuild(player)) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.delete.only-master", "&cOnly the leader can delete the guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 打开确认删除GUI
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            ConfirmDeleteGuildGUI confirmGUI = new ConfirmDeleteGuildGUI(ctx.plugin(), guild, player);
                            ctx.plugin().getGuiManager().openGUI(player, confirmGUI);
                        });
                    } catch (Exception e) {
                        SubCommandErrors.logAndNotifyPlayer(ctx.plugin(), ctx.languageManager(), player,
                                "delete", e, "guild.delete.error",
                                "&cAn error occurred while deleting the guild!");
                    }
                });

    }

    private void handleDeleteConfirm(GuildCommandContext ctx, Player player) {
                if (!ctx.plugin().getMembershipRules().canDeleteGuild(player)) {
                    String message = ctx.languageManager().getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.delete.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.plugin().getMembershipRules().canDeleteGuild(player)) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.delete.only-master", "&cOnly the leader can delete the guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        boolean success = ctx.guildService().deleteGuild(guild.getId(), player.getUniqueId());
                        if (success) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.delete.success", "&aGuild has been deleted successfully!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                        } else {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.delete.error", "&cAn error occurred while deleting the guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                        }
                    } catch (Exception e) {
                        SubCommandErrors.logAndNotifyPlayer(ctx.plugin(), ctx.languageManager(), player,
                                "delete", e, "guild.delete.error",
                                "&cAn error occurred while deleting the guild!");
                    }
                });

    }

    private void handleDeleteCancel(GuildCommandContext ctx, Player player) {
                String message = ctx.languageManager().getCoreMessage(player, "guild.delete.cancel", "&aGuild deletion cancelled!");
                player.sendMessage(ColorUtils.colorize(message));

    }

}
