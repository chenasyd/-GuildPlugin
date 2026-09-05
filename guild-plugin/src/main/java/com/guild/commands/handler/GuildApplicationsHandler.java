package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.entity.Player;

public class GuildApplicationsHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                SubCommandErrors.guardPlayerFuture(ctx.plugin(), ctx.languageManager(), player,
                        "applications", "general.error", "&cAn error occurred while opening applications!",
                        ctx.plugin().getGuildService().getPlayerGuildAsync(player.getUniqueId()))
                        .thenAccept(guild -> {
                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        if (guild == null) {
                            String msg = ctx.languageManager().getCoreMessage(player, "general.no-guild", "&cYou are not in any guild!");
                            player.sendMessage(ColorUtils.colorize(msg));
                            return;
                        }
                        // 异步检查角色权限（与 MainGuildGUI.openApplicationManagementGUI 一致）
                        SubCommandErrors.guardPlayerFuture(ctx.plugin(), ctx.languageManager(), player,
                                "applications-member", "general.error", "&cAn error occurred while opening applications!",
                                ctx.plugin().getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()))
                                .thenAccept(member -> {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                if (member == null || !ctx.plugin().getMembershipRules().canInvite(member)) {
                                    String msg = ctx.languageManager().getCoreMessage(player, "general.no-permission", "&cInsufficient role permission!");
                                    player.sendMessage(ColorUtils.colorize(msg));
                                    return;
                                }
                                ctx.plugin().getGuiManager().openGUI(player,
                                    new com.guild.gui.ApplicationManagementGUI(ctx.plugin(), guild, player));
                            });
                        });
                    });
                });

    }

}
