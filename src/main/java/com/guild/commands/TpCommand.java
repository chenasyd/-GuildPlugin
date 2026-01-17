package com.guild.commands;

import com.guild.core.utils.ColorUtils;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

public class TpCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Location target = /* ...existing code to resolve target... */ null;

            if (plugin.safeTeleport(player, target)) {
                player.sendMessage(ColorUtils.colorize("&a已传送到目标。"));
            } else {
                player.sendMessage(ColorUtils.colorize("&c当前服务器为 Folia，传送已被禁用以避免触发看门狗。"));
            }
        }
        return false;
    }
}
