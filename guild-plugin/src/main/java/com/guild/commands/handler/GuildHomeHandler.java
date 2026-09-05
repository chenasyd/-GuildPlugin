package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.entity.Player;

public class GuildHomeHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("sethome")) {
            handleSetHome(ctx, player);
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("home")) {
            handleHome(ctx, player);
        }
    }

    private void handleSetHome(GuildCommandContext ctx, Player player) {
                if (!ctx.plugin().getPermissionManager().hasPermission(player, "guild.sethome")) {
                    String message = ctx.languageManager().getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
        SubCommandErrors.runPlayerAsync(ctx.plugin(), ctx.languageManager(), player,
                "sethome", "guild.sethome.error", "&cAn error occurred while setting the guild home!", () -> {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.sethome.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.plugin().getMembershipRules().canManageGuild(player)) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.sethome.no-permission", "&cYou do not have permission to set the guild home!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 设置公会 home 位置
                        ctx.plugin().getGuildService().setGuildHome(guild.getId(), player.getLocation(), player.getUniqueId());
                        if (ctx.plugin().getGuildHomeProtectListener() != null) {
                            ctx.plugin().getGuildHomeProtectListener().refreshHomesAsync();
                        }
                
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.sethome.success", "&aGuild home has been set!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                });

    }

    private void handleHome(GuildCommandContext ctx, Player player) {
                if (!ctx.plugin().getPermissionManager().hasPermission(player, "guild.home")) {
                    String message = ctx.languageManager().getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 校验玩家是否为公会成员
                com.guild.models.GuildMember member = ctx.guildService().getGuildMember(player.getUniqueId());
                if (member == null) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.home.not-in-guild", "&cYou are not in any guild!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.home.not-in-guild", "&cYou are not in any guild!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                SubCommandErrors.guardPlayerFuture(ctx.plugin(), ctx.languageManager(), player,
                        "home", "guild.home.error", "&cAn error occurred while teleporting to guild home!",
                        ctx.plugin().getGuildService().getGuildHomeAsync(guild.getId()))
                        .thenAccept(location -> {
                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        if (location != null) {
                            startHomeTeleportDelay(ctx, player, location);
                        } else {
                            String message = ctx.languageManager().getCoreMessage(player, "home.not-set", "&cGuild home has not been set yet!");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                    });
                });

    }

    private void startHomeTeleportDelay(GuildCommandContext ctx, Player player, org.bukkit.Location targetLocation) {
                com.guild.util.GuildHomeTeleport.start(ctx.plugin(), player, targetLocation, false,
                        () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "home.success", "&aTeleported to guild home!");
                            player.sendMessage(ColorUtils.colorize(message));
                        },
                        reason -> {
                            String message = ctx.languageManager().getCoreMessage(player, "home.teleport-failed",
                                    "&cTeleport failed, please try again!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });

    }

}
