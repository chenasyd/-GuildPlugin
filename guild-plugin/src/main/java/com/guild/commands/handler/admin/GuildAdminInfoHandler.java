package com.guild.commands.handler.admin;

import com.guild.commands.handler.SubCommandErrors;
import com.guild.core.utils.ColorUtils;
import org.bukkit.command.CommandSender;

public class GuildAdminInfoHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = ctx.languageManager().getCoreMessage("admin.info.usage", "&cUsage: /guildadmin info <guild name>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        ctx.guildService().getGuildByNameAsync(guildName)
                .thenAccept(guild -> SubCommandErrors.guardAsync(
                        ctx.plugin(), ctx.languageManager(), sender,
                        "admin-info", "admin.info.error", "&cFailed to load guild info.", () -> {
                            if (guild == null) {
                                String notFound = ctx.languageManager().getCoreMessage("admin.info.not-found", "&cGuild {guild} does not exist!")
                                        .replace("{guild}", guildName);
                                ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                                return;
                            }

                            String title = ctx.languageManager().getCoreMessage("admin.info.title", "&6=== Guild Info ===");
                            ctx.sendMessage(sender, ColorUtils.colorize(title));

                            String nameMsg = ctx.languageManager().getCoreMessage("admin.info.name", "&eName: &f{name}")
                                    .replace("{name}", guild.getName());
                            ctx.sendMessage(sender, ColorUtils.colorize(nameMsg));

                            String tagDisplay = guild.getTag() != null ? guild.getTag()
                                    : ctx.languageManager().getCoreMessage("admin.info.no-tag", "None");
                            String tagMsg = ctx.languageManager().getCoreMessage("admin.info.tag", "&eTag: &f{tag}")
                                    .replace("{tag}", tagDisplay);
                            ctx.sendMessage(sender, ColorUtils.colorize(tagMsg));

                            String leaderMsg = ctx.languageManager().getCoreMessage("admin.info.leader", "&eLeader: &f{leader}")
                                    .replace("{leader}", guild.getLeaderName());
                            ctx.sendMessage(sender, ColorUtils.colorize(leaderMsg));

                            String levelMsg = ctx.languageManager().getCoreMessage("admin.info.level", "&eLevel: &f{level}")
                                    .replace("{level}", String.valueOf(guild.getLevel()));
                            ctx.sendMessage(sender, ColorUtils.colorize(levelMsg));

                            String balanceMsg = ctx.languageManager().getCoreMessage("admin.info.balance", "&eBalance: &f{balance}")
                                    .replace("{balance}", String.valueOf(guild.getBalance()));
                            ctx.sendMessage(sender, ColorUtils.colorize(balanceMsg));

                            String statusKey = guild.isFrozen() ? "admin.info.status-frozen" : "admin.info.status-normal";
                            String statusText = ctx.languageManager().getCoreMessage(statusKey, guild.isFrozen() ? "冻结" : "正常");
                            ctx.sendMessage(sender, ColorUtils.colorize("&e状态: &f" + statusText));

                            ctx.guildService().getGuildMemberCountAsync(guild.getId())
                                    .thenAccept(count -> SubCommandErrors.guardAsync(
                                            ctx.plugin(), ctx.languageManager(), sender,
                                            "admin-info-members", "admin.info.error", "&cFailed to load guild info.", () -> {
                                                String membersMsg = ctx.languageManager().getCoreMessage("admin.info.members", "&eMembers: &f{count}/{max}")
                                                        .replace("{count}", String.valueOf(count))
                                                        .replace("{max}", String.valueOf(guild.getMaxMembers()));
                                                ctx.sendMessage(sender, ColorUtils.colorize(membersMsg));
                                            }))
                                    .exceptionally(SubCommandErrors.handleAsyncFailure(
                                            ctx.plugin(), ctx.languageManager(), sender,
                                            "admin-info-members", "admin.info.error", "&cFailed to load guild info."));
                        }))
                .exceptionally(SubCommandErrors.handleAsyncFailure(
                        ctx.plugin(), ctx.languageManager(), sender,
                        "admin-info", "admin.info.error", "&cFailed to load guild info."));
    }
}
