package com.guild.commands.handler.admin;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.services.GuildService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 管理员子命令 Handler 共享依赖。
 */
public final class GuildAdminCommandContext {

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final GuildService guildService;

    public GuildAdminCommandContext(GuildPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guildService = plugin.getGuildService();
    }

    public GuildPlugin plugin() {
        return plugin;
    }

    public LanguageManager languageManager() {
        return languageManager;
    }

    public GuildService guildService() {
        return guildService;
    }

    /** Folia 兼容：Player 在实体线程发消息，控制台直接发送。 */
    public void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player player && Bukkit.getServer() != null) {
            CompatibleScheduler.runTask(plugin, player, () -> sender.sendMessage(message));
        } else {
            sender.sendMessage(message);
        }
    }
}
