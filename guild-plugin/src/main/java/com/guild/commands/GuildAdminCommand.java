package com.guild.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.AdminGuildGUI;
import com.guild.gui.ConfirmDeleteGuildGUI;
import com.guild.gui.RelationManagementGUI;
import com.guild.models.Guild;
import com.guild.models.GuildRelation;
import com.guild.core.language.LanguageManager; // 新增
import com.guild.core.module.ModuleManager;
import com.guild.update.UpdateManager;
import com.guild.update.UpdateManager.VersionInfo;

/**
 * 工会管理员命令
 */
public class GuildAdminCommand implements CommandExecutor, TabCompleter {
    
    private final GuildPlugin plugin;
    private final LanguageManager languageManager; // 新增字段
    
    public GuildAdminCommand(GuildPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager(); // 初始化
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guild.admin")) {
            String msg = languageManager.getCoreMessage("general.no-permission", "&c您没有权限执行此操作！");
            sender.sendMessage(ColorUtils.colorize(msg));
            return true;
        }
        
        if (args.length == 0) {
            if (sender instanceof Player player) {
                // 打开管理员GUI
                AdminGuildGUI adminGUI = new AdminGuildGUI(plugin, player);
                plugin.getGuiManager().openGUI(player, adminGUI);
            } else {
                handleHelp(sender);
            }
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "list":
                handleList(sender, args);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "delete":
                handleDelete(sender, args);
                break;
            case "freeze":
                handleFreeze(sender, args);
                break;
            case "unfreeze":
                handleUnfreeze(sender, args);
                break;
            case "transfer":
                handleTransfer(sender, args);
                break;
            case "economy":
                handleEconomy(sender, args);
                break;
            case "relation":
                handleRelation(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "test":
                handleTest(sender, args);
                break;
            case "update":
                handleUpdate(sender, args);
                break;
            case "help":
                handleHelp(sender);
                break;
            default:
                sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("general.unknown-command", "&c未知命令！使用 /guildadmin help 查看帮助。")));
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("guild.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "info", "delete", "freeze", "unfreeze", "transfer", "economy", "relation", "reload", "update", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "info":
                case "delete":
                case "freeze":
                case "unfreeze":
                case "transfer":
                case "economy":
                    // 获取所有工会名称
                    plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                        for (Guild guild : guilds) {
                            completions.add(guild.getName());
                        }
                    });
                    break;
                case "update":
                    completions.addAll(Arrays.asList("check", "download"));
                    break;
                case "relation":
                    completions.addAll(Arrays.asList("list", "create", "delete", "gui"));
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "transfer":
                    // 获取在线玩家
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
                case "economy":
                    completions.addAll(Arrays.asList("set", "add", "remove", "info"));
                    break;
                case "relation":
                    if ("create".equals(args[1])) {
                        // 第3个参数是第一个工会名称，获取所有工会名称
                        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                            for (Guild guild : guilds) {
                                completions.add(guild.getName());
                            }
                        });
                    }
                    break;
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "relation":
                    if ("create".equals(args[1])) {
                        // 第4个参数是第二个工会名称，获取所有工会名称
                        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                            for (Guild guild : guilds) {
                                completions.add(guild.getName());
                            }
                        });
                    }
                    break;
            }
        } else if (args.length == 5) {
            switch (args[0].toLowerCase()) {
                case "relation":
                    if ("create".equals(args[1])) {
                        // 第5个参数是关系类型
                        completions.addAll(Arrays.asList("ally", "enemy", "war", "truce", "neutral"));
                    }
                    break;
            }
        }
        
        return completions;
    }
    
    private void handleList(CommandSender sender, String[] args) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            String title = languageManager.getCoreMessage("admin.list.title", "&6=== 工会列表 ===");
            sender.sendMessage(ColorUtils.colorize(title));
            if (guilds.isEmpty()) {
                String empty = languageManager.getCoreMessage("admin.list.empty", "&c暂无工会");
                sender.sendMessage(ColorUtils.colorize(empty));
                return;
            }

            for (Guild guild : guilds) {
                String statusKey = guild.isFrozen() ? "admin.list.status-frozen" : "admin.list.status-normal";
                String status = languageManager.getCoreMessage(statusKey, guild.isFrozen() ? "&c[冻结]" : "&a[正常]");
                String format = languageManager.getCoreMessage("admin.list.format",
                    "&e{name} &7- 会长: &f{leader} &7- 等级: &f{level} &7{status}");
                String message = format
                    .replace("{name}", guild.getName())
                    .replace("{leader}", guild.getLeaderName())
                    .replace("{level}", String.valueOf(guild.getLevel()))
                    .replace("{status}", status);
                sender.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = languageManager.getCoreMessage("admin.info.usage", "&c用法: /guildadmin info <工会名称>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                String notFound = languageManager.getCoreMessage("admin.info.not-found", "&c工会 {guild} 不存在！")
                    .replace("{guild}", guildName);
                sender.sendMessage(ColorUtils.colorize(notFound));
                return;
            }

            String title = languageManager.getCoreMessage("admin.info.title", "&6=== 工会信息 ===");
            sender.sendMessage(ColorUtils.colorize(title));

            String nameMsg = languageManager.getCoreMessage("admin.info.name", "&e名称: &f{name}")
                .replace("{name}", guild.getName());
            sender.sendMessage(ColorUtils.colorize(nameMsg));

            String tagDisplay = guild.getTag() != null ? guild.getTag() :
                languageManager.getCoreMessage("admin.info.no-tag", "无");
            String tagMsg = languageManager.getCoreMessage("admin.info.tag", "&e标签: &f{tag}")
                .replace("{tag}", tagDisplay);
            sender.sendMessage(ColorUtils.colorize(tagMsg));

            String leaderMsg = languageManager.getCoreMessage("admin.info.leader", "&e会长: &f{leader}")
                .replace("{leader}", guild.getLeaderName());
            sender.sendMessage(ColorUtils.colorize(leaderMsg));

            String levelMsg = languageManager.getCoreMessage("admin.info.level", "&e等级: &f{level}")
                .replace("{level}", String.valueOf(guild.getLevel()));
            sender.sendMessage(ColorUtils.colorize(levelMsg));

            String balanceMsg = languageManager.getCoreMessage("admin.info.balance", "&e资金: &f{balance}")
                .replace("{balance}", String.valueOf(guild.getBalance()));
            sender.sendMessage(ColorUtils.colorize(balanceMsg));

            String statusKey = guild.isFrozen() ? "admin.info.status-frozen" : "admin.info.status-normal";
            String statusText = languageManager.getCoreMessage(statusKey, guild.isFrozen() ? "冻结" : "正常");
            sender.sendMessage(ColorUtils.colorize("&e状态: &f" + statusText));

            plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(count -> {
                String membersMsg = languageManager.getCoreMessage("admin.info.members", "&e成员数量: &f{count}/{max}")
                    .replace("{count}", String.valueOf(count))
                    .replace("{max}", String.valueOf(guild.getMaxMembers()));
                sender.sendMessage(ColorUtils.colorize(membersMsg));
            });
        });
    }
    
    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = languageManager.getCoreMessage("admin.delete.usage", "&c用法: /guildadmin delete <工会名称>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                String notFound = languageManager.getCoreMessage("admin.delete.not-found", "&c工会 {guild} 不存在！")
                    .replace("{guild}", guildName);
                sender.sendMessage(ColorUtils.colorize(notFound));
                return;
            }

            if (sender instanceof Player) {
                Player player = (Player) sender;
                CompatibleScheduler.runTask(plugin, () -> plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild, player)));
            } else {
                plugin.getGuildService().deleteGuildAsync(guild.getId(), UUID.randomUUID()).thenAccept(success -> {
                    if (success) {
                        String successMsg = languageManager.getCoreMessage("admin.delete.success", "&a工会 {guild} 已被强制删除！")
                            .replace("{guild}", guildName);
                        sender.sendMessage(ColorUtils.colorize(successMsg));
                    } else {
                        String failed = languageManager.getCoreMessage("admin.delete.failed", "&c删除工会失败！");
                        sender.sendMessage(ColorUtils.colorize(failed));
                    }
                });
            }
        });
    }
    
    private void handleFreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = languageManager.getCoreMessage("admin.freeze.usage", "&c用法: /guildadmin freeze <工会名称>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                String notFound = languageManager.getCoreMessage("admin.freeze.not-found", "&c工会 {guild} 不存在！")
                    .replace("{guild}", guildName);
                sender.sendMessage(ColorUtils.colorize(notFound));
                return;
            }

            String success = languageManager.getCoreMessage("admin.freeze.success", "&a工会 {guild} 已被冻结！")
                .replace("{guild}", guildName);
            sender.sendMessage(ColorUtils.colorize(success));
        });
    }
    
    private void handleUnfreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = languageManager.getCoreMessage("admin.unfreeze.usage", "&c用法: /guildadmin unfreeze <工会名称>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                String notFound = languageManager.getCoreMessage("admin.unfreeze.not-found", "&c工会 {guild} 不存在！")
                    .replace("{guild}", guildName);
                sender.sendMessage(ColorUtils.colorize(notFound));
                return;
            }

            String success = languageManager.getCoreMessage("admin.unfreeze.success", "&a工会 {guild} 已被解冻！")
                .replace("{guild}", guildName);
            sender.sendMessage(ColorUtils.colorize(success));
        });
    }
    
    private void handleTransfer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            String usage = languageManager.getCoreMessage("admin.transfer.usage", "&c用法: /guildadmin transfer <工会名称> <新会长>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        String newLeaderName = args[2];

        Player newLeader = Bukkit.getPlayer(newLeaderName);
        if (newLeader == null) {
            String notOnline = languageManager.getCoreMessage("admin.transfer.player-not-online", "&c玩家 {player} 不在线！")
                .replace("{player}", newLeaderName);
            sender.sendMessage(ColorUtils.colorize(notOnline));
            return;
        }

        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                String notFound = languageManager.getCoreMessage("admin.transfer.not-found", "&c工会 {guild} 不存在！")
                    .replace("{guild}", guildName);
                sender.sendMessage(ColorUtils.colorize(notFound));
                return;
            }

            plugin.getGuildService().getGuildMemberAsync(guild.getId(), newLeader.getUniqueId()).thenAccept(member -> {
                if (member == null) {
                    String notMember = languageManager.getCoreMessage("admin.transfer.not-member", "&c玩家 {player} 不是该工会成员！")
                        .replace("{player}", newLeaderName);
                    sender.sendMessage(ColorUtils.colorize(notMember));
                    return;
                }

                String success = languageManager.getCoreMessage("admin.transfer.success", "&a工会 {guild} 的会长已转让给 {player}！")
                    .replace("{guild}", guildName)
                    .replace("{player}", newLeaderName);
                sender.sendMessage(ColorUtils.colorize(success));
            });
        });
    }
    
    private void handleEconomy(CommandSender sender, String[] args) {
        if (args.length < 4) {
            String usage = languageManager.getCoreMessage("admin.economy.usage", "&c用法: /guildadmin economy <工会名称> <set|add|remove> <金额>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        String operation = args[2];
        double amount;

        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            String invalidAmount = languageManager.getCoreMessage("admin.economy.invalid-amount", "&c金额格式错误！");
            sender.sendMessage(ColorUtils.colorize(invalidAmount));
            return;
        }

        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                String notFound = languageManager.getCoreMessage("admin.economy.not-found", "&c工会 {guild} 不存在！")
                    .replace("{guild}", guildName);
                sender.sendMessage(ColorUtils.colorize(notFound));
                return;
            }

            final double[] newBalance = {guild.getBalance()};
            switch (operation.toLowerCase()) {
                case "set":
                    newBalance[0] = amount;
                    break;
                case "add":
                    newBalance[0] += amount;
                    break;
                case "remove":
                    newBalance[0] -= amount;
                    if (newBalance[0] < 0) newBalance[0] = 0;
                    break;
                default:
                    String invalidOp = languageManager.getCoreMessage("admin.economy.invalid-operation", "&c无效的操作！使用 set|add|remove");
                    sender.sendMessage(ColorUtils.colorize(invalidOp));
                    return;
            }

            plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), newBalance[0]).thenAccept(success -> {
                if (success) {
                    String formattedAmount = plugin.getEconomyManager().format(newBalance[0]);
                    String successMsg = languageManager.getCoreMessage("admin.economy.success", "&a工会 {guild} 的资金已更新为: {balance}")
                        .replace("{guild}", guildName)
                        .replace("{balance}", formattedAmount);
                    sender.sendMessage(ColorUtils.colorize(successMsg));
                } else {
                    String failed = languageManager.getCoreMessage("admin.economy.failed", "&c更新工会资金失败！");
                    sender.sendMessage(ColorUtils.colorize(failed));
                }
            });
        });
    }
    
    private void handleRelation(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = languageManager.getCoreMessage("admin.relation.usage", "&c用法: /guildadmin relation <list|create|delete|gui>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "gui":
                if (sender instanceof Player player) {
                    RelationManagementGUI relationGUI = new RelationManagementGUI(plugin, player);
                    plugin.getGuiManager().openGUI(player, relationGUI);
                } else {
                    String playerOnly = languageManager.getCoreMessage("admin.relation.gui-player-only", "&c此命令只能由玩家执行！");
                    sender.sendMessage(ColorUtils.colorize(playerOnly));
                }
                break;
            case "list":
                String title = languageManager.getCoreMessage("admin.relation.title", "&6=== 工会关系列表 ===");
                sender.sendMessage(ColorUtils.colorize(title));
                plugin.getGuildService().getAllGuildsAsync().thenCompose(guilds -> {
                    List<CompletableFuture<List<GuildRelation>>> relationFutures = new ArrayList<>();

                    for (Guild guild : guilds) {
                        relationFutures.add(plugin.getGuildService().getGuildRelationsAsync(guild.getId()));
                    }

                    return CompletableFuture.allOf(relationFutures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> {
                            List<GuildRelation> allRelations = new ArrayList<>();
                            for (CompletableFuture<List<GuildRelation>> future : relationFutures) {
                                try {
                                    allRelations.addAll(future.get());
                                } catch (Exception e) {
                                    String errorMsg = languageManager.getCoreMessage("admin.relation.fetch-error", "获取工会关系时发生错误: {error}")
                                        .replace("{error}", e.getMessage());
                                    plugin.getLogger().warning(errorMsg);
                                }
                            }
                            return allRelations;
                        });
                }).thenAccept(relations -> {
                    if (relations.isEmpty()) {
                        String empty = languageManager.getCoreMessage("admin.relation.empty", "&c暂无工会关系");
                        sender.sendMessage(ColorUtils.colorize(empty));
                        return;
                    }

                    for (GuildRelation relation : relations) {
                        String status = getRelationStatusText(relation.getStatus());
                        String type = getRelationTypeText(relation.getType());
                        String format = languageManager.getCoreMessage("admin.relation.format", "&e{guild1} ↔ {guild2} &7- {type} &7- {status}")
                            .replace("{guild1}", relation.getGuild1Name())
                            .replace("{guild2}", relation.getGuild2Name())
                            .replace("{type}", type)
                            .replace("{status}", status);
                        sender.sendMessage(ColorUtils.colorize(format));
                    }
                });
                break;
            case "create":
                if (args.length < 5) {
                    String usage = languageManager.getCoreMessage("admin.relation.create-usage", "&c用法: /guildadmin relation create <工会1> <工会2> <关系类型>");
                    String types = languageManager.getCoreMessage("admin.relation.create-types", "&7关系类型: ally|enemy|war|truce|neutral");
                    sender.sendMessage(ColorUtils.colorize(usage));
                    sender.sendMessage(ColorUtils.colorize(types));
                    return;
                }
                handleCreateRelation(sender, args);
                break;
            case "delete":
                if (args.length < 4) {
                    String usage = languageManager.getCoreMessage("admin.relation.delete-usage", "&c用法: /guildadmin relation delete <工会1> <工会2>");
                    sender.sendMessage(ColorUtils.colorize(usage));
                    return;
                }
                handleDeleteRelation(sender, args);
                break;
            default:
                String invalid = languageManager.getCoreMessage("admin.relation.invalid-operation", "&c无效的关系操作！使用 list|create|delete|gui");
                sender.sendMessage(ColorUtils.colorize(invalid));
                break;
        }
    }
    
    private void handleCreateRelation(CommandSender sender, String[] args) {
        String guild1Name = args[2];
        String guild2Name = args[3];
        String relationTypeStr = args[4];

        GuildRelation.RelationType relationType;
        try {
            relationType = GuildRelation.RelationType.valueOf(relationTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            String invalidType = languageManager.getCoreMessage("admin.relation.invalid-type", "&c无效的关系类型！使用: ally, enemy, war, truce, neutral");
            sender.sendMessage(ColorUtils.colorize(invalidType));
            return;
        }

        CompletableFuture<Guild> guild1Future = plugin.getGuildService().getGuildByNameAsync(guild1Name);
        CompletableFuture<Guild> guild2Future = plugin.getGuildService().getGuildByNameAsync(guild2Name);

        CompletableFuture.allOf(guild1Future, guild2Future).thenAccept(v -> {
            try {
                Guild guild1 = guild1Future.get();
                Guild guild2 = guild2Future.get();

                if (guild1 == null) {
                    String notFound = languageManager.getCoreMessage("admin.relation.not-found-guild", "&c工会 {guild} 不存在！")
                        .replace("{guild}", guild1Name);
                    sender.sendMessage(ColorUtils.colorize(notFound));
                    return;
                }
                if (guild2 == null) {
                    String notFound = languageManager.getCoreMessage("admin.relation.not-found-guild", "&c工会 {guild} 不存在！")
                        .replace("{guild}", guild2Name);
                    sender.sendMessage(ColorUtils.colorize(notFound));
                    return;
                }
                if (guild1.getId() == guild2.getId()) {
                    String cantSelf = languageManager.getCoreMessage("admin.relation.cannot-relation-self", "&c不能与自己建立关系！");
                    sender.sendMessage(ColorUtils.colorize(cantSelf));
                    return;
                }

                plugin.getGuildService().createGuildRelationAsync(
                    guild1.getId(), guild2.getId(),
                    guild1.getName(), guild2.getName(),
                    relationType, UUID.randomUUID(), "管理员"
                ).thenAccept(success -> {
                    if (success) {
                        String typeText = getRelationTypeText(relationType);
                        String successMsg = languageManager.getCoreMessage("admin.relation.create-success", "&a已创建关系: {guild1} ↔ {guild2} ({type})")
                            .replace("{guild1}", guild1Name)
                            .replace("{guild2}", guild2Name)
                            .replace("{type}", typeText);
                        sender.sendMessage(ColorUtils.colorize(successMsg));
                    } else {
                        String failed = languageManager.getCoreMessage("admin.relation.create-failed", "&c创建关系失败！");
                        sender.sendMessage(ColorUtils.colorize(failed));
                    }
                });

            } catch (Exception e) {
                String error = languageManager.getCoreMessage("admin.relation.create-error", "&c创建关系时发生错误: {error}")
                    .replace("{error}", e.getMessage());
                sender.sendMessage(ColorUtils.colorize(error));
            }
        });
    }
    
    private void handleDeleteRelation(CommandSender sender, String[] args) {
        String guild1Name = args[2];
        String guild2Name = args[3];

        CompletableFuture<Guild> guild1Future = plugin.getGuildService().getGuildByNameAsync(guild1Name);
        CompletableFuture<Guild> guild2Future = plugin.getGuildService().getGuildByNameAsync(guild2Name);

        CompletableFuture.allOf(guild1Future, guild2Future).thenAccept(v -> {
            try {
                Guild guild1 = guild1Future.get();
                Guild guild2 = guild2Future.get();

                if (guild1 == null) {
                    String notFound = languageManager.getCoreMessage("admin.relation.not-found-guild", "&c工会 {guild} 不存在！")
                        .replace("{guild}", guild1Name);
                    sender.sendMessage(ColorUtils.colorize(notFound));
                    return;
                }
                if (guild2 == null) {
                    String notFound = languageManager.getCoreMessage("admin.relation.not-found-guild", "&c工会 {guild} 不存在！")
                        .replace("{guild}", guild2Name);
                    sender.sendMessage(ColorUtils.colorize(notFound));
                    return;
                }

                plugin.getGuildService().getGuildRelationsAsync(guild1.getId()).thenAccept(relations -> {
                    for (GuildRelation relation : relations) {
                        if ((relation.getGuild1Id() == guild1.getId() && relation.getGuild2Id() == guild2.getId()) ||
                            (relation.getGuild1Id() == guild2.getId() && relation.getGuild2Id() == guild1.getId())) {

                            plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).thenAccept(success -> {
                                if (success) {
                                    String successMsg = languageManager.getCoreMessage("admin.relation.delete-success", "&a已删除关系: {guild1} ↔ {guild2}")
                                        .replace("{guild1}", guild1Name)
                                        .replace("{guild2}", guild2Name);
                                    sender.sendMessage(ColorUtils.colorize(successMsg));
                                } else {
                                    String failed = languageManager.getCoreMessage("admin.relation.delete-failed", "&c删除关系失败！");
                                    sender.sendMessage(ColorUtils.colorize(failed));
                                }
                            });
                            return;
                        }
                    }
                    String notFound = languageManager.getCoreMessage("admin.relation.not-found", "&c未找到工会 {guild1} 和 {guild2} 之间的关系！")
                        .replace("{guild1}", guild1Name)
                        .replace("{guild2}", guild2Name);
                    sender.sendMessage(ColorUtils.colorize(notFound));
                });

            } catch (Exception e) {
                String error = languageManager.getCoreMessage("admin.relation.delete-error", "&c删除关系时发生错误: {error}")
                    .replace("{error}", e.getMessage());
                sender.sendMessage(ColorUtils.colorize(error));
            }
        });
    }
    
    private String getRelationStatusText(GuildRelation.RelationStatus status) {
        String key = "admin.relation.status.unknown";
        switch (status) {
            case PENDING: key = "admin.relation.status.pending"; break;
            case ACTIVE: key = "admin.relation.status.active"; break;
            case EXPIRED: key = "admin.relation.status.expired"; break;
            case CANCELLED: key = "admin.relation.status.cancelled"; break;
        }
        return languageManager.getCoreMessage(key, "未知");
    }
    
    private String getRelationTypeText(GuildRelation.RelationType type) {
        String key = "admin.relation.type.unknown";
        switch (type) {
            case ALLY: key = "admin.relation.type.ally"; break;
            case ENEMY: key = "admin.relation.type.enemy"; break;
            case WAR: key = "admin.relation.type.war"; break;
            case TRUCE: key = "admin.relation.type.truce"; break;
            case NEUTRAL: key = "admin.relation.type.neutral"; break;
        }
        return languageManager.getCoreMessage(key, "未知");
    }
    
    private void handleReload(CommandSender sender) {
        try {
            // 配置和权限轻量，同步执行
            plugin.getConfigManager().reloadAllConfigs();
            plugin.getPermissionManager().reloadFromConfig();

            // 插件本体语言（core/gui）异步重载 — 与模块语言完全独立
            plugin.getLanguageManager().reloadLanguagesAsync(() -> {
                // 刷新所有打开的 GUI 界面
                try {
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (plugin.getGuiManager().hasOpenGUI(p)) {
                            plugin.getGuiManager().refreshGUI(p);
                        }
                    }
                } catch (Exception ignored) {}

                String success = languageManager.getCoreMessage(
                        "admin.reload.success", "&a配置已重新加载！");
                sender.sendMessage(ColorUtils.colorize(success));
            });

            // 模块语言异步重载 — 与插件本体并行执行
            plugin.getLanguageManager().reloadModuleLanguagesAsync(() -> {
                // 让 SDK / 模块层加载模块语言资源
                try {
                    ModuleManager mm = plugin.getModuleManager();
                    var api = mm.getSharedApi();
                    for (String moduleId : mm.getRegistry().getModuleIds()) {
                        try {
                            api.loadModuleLanguageResource(moduleId, null);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            String failed = languageManager.getCoreMessage("admin.reload.failed",
                    "&c重新加载配置失败: {error}")
                .replace("{error}", e.getMessage());
            sender.sendMessage(ColorUtils.colorize(failed));
        }
    }

    private void handleUpdate(CommandSender sender, String[] args) {
        UpdateManager updateManager = plugin.getUpdateManager();

        if (args.length >= 2 && "download".equalsIgnoreCase(args[1])) {
            // Download the update
            if (!sender.hasPermission("guild.admin.update")) {
                sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                    "general.no-permission", "&cYou do not have permission!")));
                return;
            }

            // Pre-download warning about old plugin JARs
            sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                "admin.update.cleanup-notice", "&6[GuildPlugin] &eImportant: After downloading, please ensure ALL old "
                    + "GuildPlugin JARs are deleted from the plugins folder before restarting!")));
            sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                "admin.update.checking", "&6[GuildPlugin] &eChecking for latest version...")));
            CompatibleScheduler.runTaskAsync(plugin, () -> {
                VersionInfo info = updateManager.checkLatestVersion();
                if (info == null) {
                    sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                        "admin.update.fetch-failed", "&c[GuildPlugin] Failed to fetch version info.")));
                    return;
                }

                String localVersion = plugin.getDescription().getVersion();
                int cmp = UpdateManager.compareVersions(localVersion, info.version);
                if (cmp >= 0) {
                    sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                        "admin.update.already-latest", "&a[GuildPlugin] You are already running the latest version (v{version}).")
                        .replace("{version}", localVersion)));
                    return;
                }

                updateManager.downloadUpdate(info, sender);

                // Post-download reminder
                sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                    "admin.update.manual-cleanup-reminder",
                    "&6[GuildPlugin] &eREMINDER: Check the plugins folder and delete ALL old "
                        + "GuildPlugin JARs (including renamed ones) before restarting the server!")));

                // Notify all online admins
                String broadcastMsg = languageManager.getCoreMessage(
                    "admin.update.download-broadcast", "&6[GuildPlugin] &e{player} downloaded v{version}. "
                        + "Remove all old GuildPlugin JARs and restart to apply.")
                    .replace("{player}", sender.getName())
                    .replace("{version}", info.version);
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("guild.admin") && !p.equals(sender)) {
                        p.sendMessage(ColorUtils.colorize(broadcastMsg));
                    }
                }
            });
            return;
        }

        // Check for updates (no download)
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
            "admin.update.checking-dual", "&6[GuildPlugin] &eChecking for updates from GitHub & Modrinth...")));
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            VersionInfo info = updateManager.checkLatestVersion();
            if (info == null) {
                sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                    "admin.update.unreachable", "&c[GuildPlugin] Unable to check for updates. Both GitHub and Modrinth are unreachable.")));
                return;
            }

            String localVersion = plugin.getDescription().getVersion();
            int cmp = UpdateManager.compareVersions(localVersion, info.version);

            sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                "admin.update.header", "&6======== GuildPlugin Update ========")));
            sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                "admin.update.source", "&eSource: &f{source}").replace("{source}", info.source)));
            sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                "admin.update.current", "&eCurrent: &fv{version}").replace("{version}", localVersion)));
            sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                cmp < 0 ? "admin.update.latest" : "admin.update.latest-up-to-date",
                "&eLatest: &fv{version}").replace("{version}", info.version)));

            if (cmp < 0) {
                if (!info.changelog.isEmpty()) {
                    sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                        "admin.update.changelog-title", "&eChangelog:")));
                    for (String line : info.changelog.split("\n")) {
                        sender.sendMessage(ColorUtils.colorize("&7  " + line));
                    }
                }
                sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                    "admin.update.usage-download", "&eUsage: &f/guildadmin update download &7to download the update")));
            } else {
                sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                    "admin.update.up-to-date", "&aYou are running the latest version.")));
            }
            sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                "admin.update.footer", "&6====================================")));
        });
    }

    private void handleTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String usage = languageManager.getCoreMessage("admin.test.usage", "&c用法: /guildadmin test <test-type>");
            String types = languageManager.getCoreMessage("admin.test.types", "&7test-type: gui, economy, relation");
            sender.sendMessage(ColorUtils.colorize(usage));
            sender.sendMessage(ColorUtils.colorize(types));
            return;
        }

        String testType = args[1];
        switch (testType.toLowerCase()) {
            case "gui":
                if (sender instanceof Player player) {
                    AdminGuildGUI adminGUI = new AdminGuildGUI(plugin, player);
                    plugin.getGuiManager().openGUI(player, adminGUI);
                    String success = languageManager.getCoreMessage("admin.test.gui-success", "&a已打开管理员GUI进行测试。");
                    sender.sendMessage(ColorUtils.colorize(success));
                } else {
                    String playerOnly = languageManager.getCoreMessage("admin.test.gui-player-only", "&c此命令只能由玩家执行！");
                    sender.sendMessage(ColorUtils.colorize(playerOnly));
                }
                break;
            case "economy":
                if (args.length < 4) {
                    String usage = languageManager.getCoreMessage("admin.test.economy-usage", "&c用法: /guildadmin test economy <工会名称> <操作> <金额>");
                    sender.sendMessage(ColorUtils.colorize(usage));
                    return;
                }
                String guildName = args[2];
                String operation = args[3];
                double amount;
                try {
                    amount = Double.parseDouble(args[4]);
                } catch (NumberFormatException e) {
                    String invalid = languageManager.getCoreMessage("admin.economy.invalid-amount", "&c金额格式错误！");
                    sender.sendMessage(ColorUtils.colorize(invalid));
                    return;
                }
                plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
                    if (guild == null) {
                        String notFound = languageManager.getCoreMessage("admin.economy.not-found", "&c工会 {guild} 不存在！")
                            .replace("{guild}", guildName);
                        sender.sendMessage(ColorUtils.colorize(notFound));
                        return;
                    }
                    final double[] newBalance = {guild.getBalance()};
                    switch (operation.toLowerCase()) {
                        case "set":
                            newBalance[0] = amount;
                            break;
                        case "add":
                            newBalance[0] += amount;
                            break;
                        case "remove":
                            newBalance[0] -= amount;
                            if (newBalance[0] < 0) newBalance[0] = 0;
                            break;
                        default:
                            String invalidOp = languageManager.getCoreMessage("admin.economy.invalid-operation", "&c无效的操作！使用 set|add|remove");
                            sender.sendMessage(ColorUtils.colorize(invalidOp));
                            return;
                    }
                    plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), newBalance[0]).thenAccept(success -> {
                        if (success) {
                            String formattedAmount = plugin.getEconomyManager().format(newBalance[0]);
                            String successMsg = languageManager.getCoreMessage("admin.economy.success", "&a工会 {guild} 的资金已更新为: {balance}")
                                .replace("{guild}", guildName)
                                .replace("{balance}", formattedAmount);
                            sender.sendMessage(ColorUtils.colorize(successMsg));
                        } else {
                            String failed = languageManager.getCoreMessage("admin.economy.failed", "&c更新工会资金失败！");
                            sender.sendMessage(ColorUtils.colorize(failed));
                        }
                    });
                });
                break;
            case "relation":
                if (args.length < 5) {
                    String usage = languageManager.getCoreMessage("admin.test.relation-usage", "&c用法: /guildadmin test relation create <工会1> <工会2> <关系类型>");
                    String types = languageManager.getCoreMessage("admin.relation.create-types", "&7关系类型: ally|enemy|war|truce|neutral");
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
                    String invalid = languageManager.getCoreMessage("admin.relation.invalid-type", "&c无效的关系类型！使用: ally, enemy, war, truce, neutral");
                    sender.sendMessage(ColorUtils.colorize(invalid));
                    return;
                }
                plugin.getGuildService().getGuildByNameAsync(guild1NameTest).thenAccept(guild1 -> {
                    if (guild1 == null) {
                        String notFound = languageManager.getCoreMessage("admin.relation.not-found-guild", "&c工会 {guild} 不存在！")
                            .replace("{guild}", guild1NameTest);
                        sender.sendMessage(ColorUtils.colorize(notFound));
                        return;
                    }
                    plugin.getGuildService().getGuildByNameAsync(guild2NameTest).thenAccept(guild2 -> {
                        if (guild2 == null) {
                            String notFound = languageManager.getCoreMessage("admin.relation.not-found-guild", "&c工会 {guild} 不存在！")
                                .replace("{guild}", guild2NameTest);
                            sender.sendMessage(ColorUtils.colorize(notFound));
                            return;
                        }
                        if (guild1.getId() == guild2.getId()) {
                            String cantSelf = languageManager.getCoreMessage("admin.relation.cannot-relation-self", "&c不能与自己建立关系！");
                            sender.sendMessage(ColorUtils.colorize(cantSelf));
                            return;
                        }
                        plugin.getGuildService().createGuildRelationAsync(
                            guild1.getId(), guild2.getId(),
                            guild1.getName(), guild2.getName(),
                            relationTypeTest, UUID.randomUUID(), "管理员"
                        ).thenAccept(success -> {
                            if (success) {
                                String typeText = getRelationTypeText(relationTypeTest);
                                String successMsg = languageManager.getCoreMessage("admin.relation.create-success", "&a已创建关系: {guild1} ↔ {guild2} ({type})")
                                    .replace("{guild1}", guild1NameTest)
                                    .replace("{guild2}", guild2NameTest)
                                    .replace("{type}", typeText);
                                sender.sendMessage(ColorUtils.colorize(successMsg));
                            } else {
                                String failed = languageManager.getCoreMessage("admin.relation.create-failed", "&c创建关系失败！");
                                sender.sendMessage(ColorUtils.colorize(failed));
                            }
                        });
                    });
                });
                break;
            default:
                String invalid = languageManager.getCoreMessage("admin.test.invalid-type", "&c无效的测试类型！使用 gui, economy, relation");
                sender.sendMessage(ColorUtils.colorize(invalid));
                break;
        }
    }
    
    private void handleHelp(CommandSender sender) {
        String title = languageManager.getCoreMessage("admin.help.title", "&6=== 工会管理员命令 ===");
        sender.sendMessage(ColorUtils.colorize(title));

        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.main", "&e/guildadmin &7- 打开管理员GUI")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.list", "&e/guildadmin list &7- 列出所有工会")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.info", "&e/guildadmin info <工会> &7- 查看工会信息")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.delete", "&e/guildadmin delete <工会> &7- 强制删除工会")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.freeze", "&e/guildadmin freeze <工会> &7- 冻结工会")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.unfreeze", "&e/guildadmin unfreeze <工会> &7- 解冻工会")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.transfer", "&e/guildadmin transfer <工会> <玩家> &7- 转让会长")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.economy", "&e/guildadmin economy <工会> <操作> <金额> &7- 管理工会经济")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.relation", "&e/guildadmin relation <操作> &7- 管理工会关系")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.reload", "&e/guildadmin reload &7- 重新加载配置")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.update", "&e/guildadmin update &7- Check for plugin updates")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.update-download", "&e/guildadmin update download &7- Download and install update")));
        sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("admin.help.help", "&e/guildadmin help &7- 显示帮助信息")));
    }
}
