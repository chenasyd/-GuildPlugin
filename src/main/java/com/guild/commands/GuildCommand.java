package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.MainGuildGUI;
import com.guild.gui.GuildSettingsGUI;
import com.guild.gui.ConfirmDeleteGUI;
import com.guild.models.Guild;
import com.guild.services.GuildService;
import com.guild.utils.GuildDeletionManager;
import com.guild.core.utils.CompatibleScheduler;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * 工会主命令
 */
public class GuildCommand implements CommandExecutor, TabCompleter {

    private final GuildPlugin plugin;

    public GuildCommand(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;

        // 无参数：仅玩家打开主 GUI
        if (args.length == 0) {
            if (player == null) {
                sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.player-only", "&c此命令只能由玩家执行！")));
                return true;
            }
            MainGuildGUI mainGuildGUI = new MainGuildGUI(plugin);
            plugin.getGuiManager().openGUI(player, mainGuildGUI);
            return true;
        }

        // 统一处理 delete：玩家通过 GUI（ConfirmDeleteGUI）确认，控制台必须带 confirm 并直接删除
        if (args[0].equalsIgnoreCase("delete")) {
            // 玩家分支：打开确认 GUI（支持 /guild delete <name> 或 /guild delete）
            if (player != null) {
                if (args.length >= 2 && !args[1].equalsIgnoreCase("confirm") && !args[1].equalsIgnoreCase("cancel")) {
                    // 指定了工会名
                    String guildName = args[1];
                    Guild guild = plugin.getGuildManager().getGuild(guildName);
                    if (guild == null) {
                        player.sendMessage(ColorUtils.colorize("&c未找到工会: &e" + guildName));
                        return true;
                    }
                    boolean isLeader = guild.getLeaderName() != null && guild.getLeaderName().equalsIgnoreCase(player.getName());
                    boolean hasAdmin = plugin.getPermissionManager().hasPermission(player, "guild.admin");
                    if (!isLeader && !hasAdmin) {
                        player.sendMessage(ColorUtils.colorize("&c只有会长或管理员可以删除工会！"));
                        return true;
                    }
                    plugin.getGuiManager().openGUI(player, new ConfirmDeleteGUI(plugin, player, guild, hasAdmin));
                    player.sendMessage(ColorUtils.colorize("&e已打开删除确认 GUI，请在 GUI 中确认删除。"));
                    return true;
                }

                // 未指定名称或使用 confirm：查找玩家所属工会并打开确认 GUI
                findGuildForPlayer(player).thenAccept(found -> {
                    if (found == null) {
                        player.sendMessage(ColorUtils.colorize("&c未找到您可删除的工会（您不是任何工会的会长）。"));
                        player.sendMessage(ColorUtils.colorize("&e如需删除其它工会，请使用 /guildadmin delete <工会名称>"));
                        return;
                    }
                    boolean isLeader = (found.getLeaderName() != null && found.getLeaderName().equalsIgnoreCase(player.getName()));
                    boolean hasAdmin = plugin.getPermissionManager().hasPermission(player, "guild.admin");
                    if (!isLeader && !hasAdmin) {
                        player.sendMessage(ColorUtils.colorize("&c只有会长或管理员可以删除工会！"));
                        return;
                    }
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGUI(plugin, player, found, hasAdmin));
                        player.sendMessage(ColorUtils.colorize("&e已打开删除确认 GUI，请在 GUI 中确认删除。"));
                    });
                });
                return true;
            }

            // 控制台：必须带 confirm
            if (args.length < 2) {
                sender.sendMessage(ColorUtils.colorize("&c控制台删除必须指定工会名并带 confirm 参数: /guild delete <工会名> confirm"));
                return true;
            }
            String targetName = args[1];
            Guild guild = plugin.getGuildManager().getGuild(targetName);
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&c未找到工会: &e" + targetName));
                return true;
            }
            boolean confirm = args.length >= 3 && args[2].equalsIgnoreCase("confirm");
            if (!confirm) {
                sender.sendMessage(ColorUtils.colorize("&c控制台无法打开GUI，请使用: /guild delete " + targetName + " confirm"));
                return true;
            }
            ConfirmDeleteGUI.confirmDelete(plugin, sender, guild, true);
            return true;
        }

        player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.unknown-command", "&c未知命令！使用 /guild help 查看帮助。")));
        return true;
    }

    private void handleDelete(Player player, String[] args) {
        // 简化：直接打开确认删除 GUI（兼容 /guild delete、/guild delete confirm）
        findGuildForPlayer(player).thenAccept(found -> {
            if (found == null) {
                player.sendMessage(ColorUtils.colorize("&c未找到您可删除的工会（您不是任何工会的会长）。"));
                player.sendMessage(ColorUtils.colorize("&e如需删除其它工会，请使用 /guildadmin delete <工会名称>"));
                return;
            }
            boolean isLeader = (found.getLeaderName() != null && found.getLeaderName().equalsIgnoreCase(player.getName()));
            boolean hasAdmin = plugin.getPermissionManager().hasPermission(player, "guild.admin");
            if (!isLeader && !hasAdmin) {
                player.sendMessage(ColorUtils.colorize("&c只有会长或管理员可以删除工会！"));
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getGuiManager().openGUI(player, new ConfirmDeleteGUI(plugin, player, found, hasAdmin));
                player.sendMessage(ColorUtils.colorize("&e已打开删除确认 GUI，请在 GUI 中确认删除。"));
            });
        });
    }

    private CompletableFuture<Guild> findGuildForPlayer(Player player) {
        Object service = plugin.getGuildService();
        if (service == null) {
            return CompletableFuture.completedFuture(null);
        }

        String[] tryNames = {
            "getGuildByLeaderIdAsync","getGuildByLeaderAsync","getGuildByLeader",
            "getGuildByPlayerAsync","getGuildByPlayer","getGuildForPlayerAsync","getGuildByMemberAsync"
        };

        for (String name : tryNames) {
            try {
                Method m = service.getClass().getMethod(name, UUID.class);
                Object res = m.invoke(service, player.getUniqueId());
                if (res == null) continue;
                if (res instanceof CompletableFuture) {
                    @SuppressWarnings("unchecked")
                    CompletableFuture<Guild> fut = (CompletableFuture<Guild>) res;
                    return fut;
                }
                if (res instanceof java.util.concurrent.Future) {
                    return CompletableFuture.supplyAsync(() -> {
                        try { return (Guild) ((java.util.concurrent.Future<?>) res).get(); }
                        catch (Exception e) { plugin.getLogger().log(Level.WARNING, "调用 " + name + " 时发生错误", e); return null; }
                    });
                }
                if (res instanceof Guild) {
                    return CompletableFuture.completedFuture((Guild) res);
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "尝试方法 " + name + " 失败", e);
            }
        }

        // 回退：遍历所有工会查找 leaderId/leaderName
        return plugin.getGuildService().getAllGuildsAsync().thenApply(guilds -> {
            for (Guild g : guilds) {
                try {
                    Method gm = g.getClass().getMethod("getLeaderId");
                    Object leaderId = gm.invoke(g);
                    if (leaderId instanceof UUID && ((UUID) leaderId).equals(player.getUniqueId())) return g;
                } catch (Exception ignored) {}
                if (g.getLeaderName() != null && g.getLeaderName().equalsIgnoreCase(player.getName())) return g;
            }
            return null;
        });
    }
}
