package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import org.bukkit.entity.Player;

public class GuildChatHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                com.guild.chat.GuildChatManager chatManager = ctx.plugin().getGuildChatManager();
                if (chatManager == null) {
                    player.sendMessage(ColorUtils.colorize("&cGuild chat is not available."));
                    return;
                }

                // /guild chat <消息> — 直接发送一条公会消息（不切换模式）
                if (args.length > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        if (i > 1) sb.append(" ");
                        sb.append(args[i]);
                    }
                    String msg = sb.toString();
                    com.guild.models.GuildMember member = ctx.guildService().getGuildMember(player.getUniqueId());
                    if (member == null) {
                        String err = ctx.languageManager().getCoreMessage(player, "guild.chat.not-in-guild",
                            "&cYou are not in a guild!");
                        player.sendMessage(ColorUtils.colorize(err));
                        return;
                    }
                    com.guild.models.Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                    if (guild == null) {
                        player.sendMessage(ColorUtils.colorize("&cGuild not found!"));
                        return;
                    }
                    String formatted = chatManager.formatMessage(player, member.getRole(), msg);
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        com.guild.models.GuildMember pm = ctx.guildService().getGuildMember(p.getUniqueId());
                        if (pm != null && pm.getGuildId() == guild.getId()) {
                            p.sendMessage(ColorUtils.colorize(formatted));
                        }
                    }
                    return;
                }

                // /guild chat — 切换聊天模式
                boolean enabled = chatManager.toggleChatMode(player);
                if (enabled) {
                    String msg = ctx.languageManager().getCoreMessage(player, "guild.chat.enabled",
                        "&aGuild chat &aenabled&a. Your messages will be sent to guild members.");
                    player.sendMessage(ColorUtils.colorize(msg));
                } else {
                    String msg = ctx.languageManager().getCoreMessage(player, "guild.chat.disabled",
                        "&eGuild chat &cdisabled&e. Your messages will be sent to global chat.");
                    player.sendMessage(ColorUtils.colorize(msg));
                }

    }

}
