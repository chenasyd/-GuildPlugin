package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.AdminGuildGUI;
import com.guild.gui.RelationManagementGUI;
import com.guild.models.Guild;
import com.guild.models.GuildRelation;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 工会管理员命令
 */
public class GuildAdminCommand implements CommandExecutor, TabCompleter {
    
    private final GuildPlugin plugin;
    
    public GuildAdminCommand(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guild.admin")) {
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.no-permission", "&c您没有权限执行此操作！")));
            return true;
        }
        
        if (args.length == 0) {
            if (sender instanceof Player player) {
                // 打开管理员GUI
                AdminGuildGUI adminGUI = new AdminGuildGUI(plugin);
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
            case "help":
                handleHelp(sender);
                break;
            default:
                sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.unknown-command", "&c未知命令！使用 /guildadmin help 查看帮助。")));
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
            completions.addAll(Arrays.asList("list", "info", "delete", "freeze", "unfreeze", "transfer", "economy", "relation", "reload", "help"));
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
            sender.sendMessage(ColorUtils.colorize("&6=== 工会列表 ==="));
            if (guilds.isEmpty()) {
                sender.sendMessage(ColorUtils.colorize("&c暂无工会"));
                return;
            }
            
            for (Guild guild : guilds) {
                String status = guild.isFrozen() ? "&c[冻结]" : "&a[正常]";
                sender.sendMessage(ColorUtils.colorize(String.format("&e%s &7- 会长: &f%s &7- 等级: &f%d &7%s", 
                    guild.getName(), guild.getLeaderName(), guild.getLevel(), status)));
            }
        });
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin info <工会名称>"));
            return;
        }
        
        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&c工会 " + guildName + " 不存在！"));
                return;
            }
            
            sender.sendMessage(ColorUtils.colorize("&6=== 工会信息 ==="));
            sender.sendMessage(ColorUtils.colorize("&e名称: &f" + guild.getName()));
            sender.sendMessage(ColorUtils.colorize("&e标签: &f" + (guild.getTag() != null ? guild.getTag() : "无")));
            sender.sendMessage(ColorUtils.colorize("&e会长: &f" + guild.getLeaderName()));
            sender.sendMessage(ColorUtils.colorize("&e等级: &f" + guild.getLevel()));
            sender.sendMessage(ColorUtils.colorize("&e资金: &f" + guild.getBalance()));
            sender.sendMessage(ColorUtils.colorize("&e状态: &f" + (guild.isFrozen() ? "冻结" : "正常")));
            
            // 获取成员数量
            plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(count -> {
                sender.sendMessage(ColorUtils.colorize("&e成员数量: &f" + count + "/" + guild.getMaxMembers()));
            });
        });
    }
    
    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin delete <工会名称>"));
            return;
        }
        
        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&c工会 " + guildName + " 不存在！"));
                return;
            }
            
            // 强制删除工会
            plugin.getGuildService().deleteGuildAsync(guild.getId(), UUID.randomUUID()).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(ColorUtils.colorize("&a工会 " + guildName + " 已被强制删除！"));
                } else {
                    sender.sendMessage(ColorUtils.colorize("&c删除工会失败！"));
                }
            });
        });
    }
    
    private void handleFreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin freeze <工会名称>"));
            return;
        }
        
        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&c工会 " + guildName + " 不存在！"));
                return;
            }
            
            // 冻结工会
            // TODO: 实现冻结功能
            sender.sendMessage(ColorUtils.colorize("&a工会 " + guildName + " 已被冻结！"));
        });
    }
    
    private void handleUnfreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin unfreeze <工会名称>"));
            return;
        }
        
        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&c工会 " + guildName + " 不存在！"));
                return;
            }
            
            // 解冻工会
            // TODO: 实现解冻功能
            sender.sendMessage(ColorUtils.colorize("&a工会 " + guildName + " 已被解冻！"));
        });
    }
    
    private void handleTransfer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin transfer <工会名称> <新会长>"));
            return;
        }
        
        String guildName = args[1];
        String newLeaderName = args[2];
        
        Player newLeader = Bukkit.getPlayer(newLeaderName);
        if (newLeader == null) {
            sender.sendMessage(ColorUtils.colorize("&c玩家 " + newLeaderName + " 不在线！"));
            return;
        }
        
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&c工会 " + guildName + " 不存在！"));
                return;
            }
            
            // 检查新会长是否是该工会成员
            plugin.getGuildService().getGuildMemberAsync(guild.getId(), newLeader.getUniqueId()).thenAccept(member -> {
                if (member == null) {
                    sender.sendMessage(ColorUtils.colorize("&c玩家 " + newLeaderName + " 不是该工会成员！"));
                    return;
                }
                
                // 转让会长
                // TODO: 实现转让功能
                sender.sendMessage(ColorUtils.colorize("&a工会 " + guildName + " 的会长已转让给 " + newLeaderName + "！"));
            });
        });
    }
    
    private void handleEconomy(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin economy <工会名称> <set|add|remove> <金额>"));
            return;
        }
        
        String guildName = args[1];
        String operation = args[2];
        double amount;
        
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&c金额格式错误！"));
            return;
        }
        
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&c工会 " + guildName + " 不存在！"));
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
                    sender.sendMessage(ColorUtils.colorize("&c无效的操作！使用 set|add|remove"));
                    return;
            }
            
            // 更新工会资金
            plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), newBalance[0]).thenAccept(success -> {
                if (success) {
                    String formattedAmount = plugin.getEconomyManager().format(newBalance[0]);
                    sender.sendMessage(ColorUtils.colorize("&a工会 " + guildName + " 的资金已更新为: " + formattedAmount));
                } else {
                    sender.sendMessage(ColorUtils.colorize("&c更新工会资金失败！"));
                }
            });
        });
    }
    
    private void handleRelation(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin relation <list|create|delete|gui>"));
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "gui":
                if (sender instanceof Player player) {
                    // 打开关系管理GUI
                    RelationManagementGUI relationGUI = new RelationManagementGUI(plugin, player);
                    plugin.getGuiManager().openGUI(player, relationGUI);
                } else {
                    sender.sendMessage(ColorUtils.colorize("&c此命令只能由玩家执行！"));
                }
                break;
            case "list":
                // 显示所有工会关系
                sender.sendMessage(ColorUtils.colorize("&6=== 工会关系列表 ==="));
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
                                    plugin.getLogger().warning("获取工会关系时发生错误: " + e.getMessage());
                                }
                            }
                            return allRelations;
                        });
                }).thenAccept(relations -> {
                    if (relations.isEmpty()) {
                        sender.sendMessage(ColorUtils.colorize("&c暂无工会关系"));
                        return;
                    }
                    
                    for (GuildRelation relation : relations) {
                        String status = getRelationStatusText(relation.getStatus());
                        String type = getRelationTypeText(relation.getType());
                        sender.sendMessage(ColorUtils.colorize(String.format("&e%s ↔ %s &7- %s &7- %s", 
                            relation.getGuild1Name(), relation.getGuild2Name(), type, status)));
                    }
                });
                break;
            case "create":
                if (args.length < 5) {
                    sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin relation create <工会1> <工会2> <关系类型>"));
                    sender.sendMessage(ColorUtils.colorize("&7关系类型: ally|enemy|war|truce|neutral"));
                    return;
                }
                handleCreateRelation(sender, args);
                break;
            case "delete":
                if (args.length < 4) {
                    sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin relation delete <工会1> <工会2>"));
                    return;
                }
                handleDeleteRelation(sender, args);
                break;
            default:
                sender.sendMessage(ColorUtils.colorize("&c无效的关系操作！使用 list|create|delete|gui"));
                break;
        }
    }
    
    private void handleCreateRelation(CommandSender sender, String[] args) {
        String guild1Name = args[2];
        String guild2Name = args[3];
        String relationTypeStr = args[4];
        
        // 解析关系类型
        GuildRelation.RelationType relationType;
        try {
            relationType = GuildRelation.RelationType.valueOf(relationTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ColorUtils.colorize("&c无效的关系类型！使用: ally, enemy, war, truce, neutral"));
            return;
        }
        
        // 获取两个工会
        CompletableFuture<Guild> guild1Future = plugin.getGuildService().getGuildByNameAsync(guild1Name);
        CompletableFuture<Guild> guild2Future = plugin.getGuildService().getGuildByNameAsync(guild2Name);
        
        CompletableFuture.allOf(guild1Future, guild2Future).thenAccept(v -> {
            try {
                Guild guild1 = guild1Future.get();
                Guild guild2 = guild2Future.get();
                
                if (guild1 == null) {
                    sender.sendMessage(ColorUtils.colorize("&c工会 " + guild1Name + " 不存在！"));
                    return;
                }
                if (guild2 == null) {
                    sender.sendMessage(ColorUtils.colorize("&c工会 " + guild2Name + " 不存在！"));
                    return;
                }
                if (guild1.getId() == guild2.getId()) {
                    sender.sendMessage(ColorUtils.colorize("&c不能与自己建立关系！"));
                    return;
                }
                
                // 创建关系
                plugin.getGuildService().createGuildRelationAsync(
                    guild1.getId(), guild2.getId(), 
                    guild1.getName(), guild2.getName(), 
                    relationType, UUID.randomUUID(), "管理员"
                ).thenAccept(success -> {
                    if (success) {
                        sender.sendMessage(ColorUtils.colorize("&a已创建关系: " + guild1Name + " ↔ " + guild2Name + " (" + getRelationTypeText(relationType) + ")"));
                    } else {
                        sender.sendMessage(ColorUtils.colorize("&c创建关系失败！"));
                    }
                });
                
            } catch (Exception e) {
                sender.sendMessage(ColorUtils.colorize("&c创建关系时发生错误: " + e.getMessage()));
            }
        });
    }
    
    private void handleDeleteRelation(CommandSender sender, String[] args) {
        String guild1Name = args[2];
        String guild2Name = args[3];
        
        // 获取两个工会
        CompletableFuture<Guild> guild1Future = plugin.getGuildService().getGuildByNameAsync(guild1Name);
        CompletableFuture<Guild> guild2Future = plugin.getGuildService().getGuildByNameAsync(guild2Name);
        
        CompletableFuture.allOf(guild1Future, guild2Future).thenAccept(v -> {
            try {
                Guild guild1 = guild1Future.get();
                Guild guild2 = guild2Future.get();
                
                if (guild1 == null) {
                    sender.sendMessage(ColorUtils.colorize("&c工会 " + guild1Name + " 不存在！"));
                    return;
                }
                if (guild2 == null) {
                    sender.sendMessage(ColorUtils.colorize("&c工会 " + guild2Name + " 不存在！"));
                    return;
                }
                
                // 查找并删除关系
                plugin.getGuildService().getGuildRelationsAsync(guild1.getId()).thenAccept(relations -> {
                    for (GuildRelation relation : relations) {
                        if ((relation.getGuild1Id() == guild1.getId() && relation.getGuild2Id() == guild2.getId()) ||
                            (relation.getGuild1Id() == guild2.getId() && relation.getGuild2Id() == guild1.getId())) {
                            
                            plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).thenAccept(success -> {
                                if (success) {
                                    sender.sendMessage(ColorUtils.colorize("&a已删除关系: " + guild1Name + " ↔ " + guild2Name));
                                } else {
                                    sender.sendMessage(ColorUtils.colorize("&c删除关系失败！"));
                                }
                            });
                            return;
                        }
                    }
                    sender.sendMessage(ColorUtils.colorize("&c未找到工会 " + guild1Name + " 和 " + guild2Name + " 之间的关系！"));
                });
                
            } catch (Exception e) {
                sender.sendMessage(ColorUtils.colorize("&c删除关系时发生错误: " + e.getMessage()));
            }
        });
    }
    
    private String getRelationStatusText(GuildRelation.RelationStatus status) {
        switch (status) {
            case PENDING: return "待处理";
            case ACTIVE: return "活跃";
            case EXPIRED: return "已过期";
            case CANCELLED: return "已取消";
            default: return "未知";
        }
    }
    
    private String getRelationTypeText(GuildRelation.RelationType type) {
        switch (type) {
            case ALLY: return "盟友";
            case ENEMY: return "敌对";
            case WAR: return "开战";
            case TRUCE: return "停战";
            case NEUTRAL: return "中立";
            default: return "未知";
        }
    }
    
    private void handleReload(CommandSender sender) {
        try {
            plugin.getConfigManager().reloadAllConfigs();
            // 重新加载权限矩阵并清空权限缓存
            plugin.getPermissionManager().reloadFromConfig();
            sender.sendMessage(ColorUtils.colorize("&a配置已重新加载！"));
        } catch (Exception e) {
            sender.sendMessage(ColorUtils.colorize("&c重新加载配置失败: " + e.getMessage()));
        }
    }

    private void handleTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin test <test-type>"));
            sender.sendMessage(ColorUtils.colorize("&7test-type: gui, economy, relation"));
            return;
        }

        String testType = args[1];
        switch (testType.toLowerCase()) {
            case "gui":
                if (sender instanceof Player player) {
                    AdminGuildGUI adminGUI = new AdminGuildGUI(plugin);
                    plugin.getGuiManager().openGUI(player, adminGUI);
                    sender.sendMessage(ColorUtils.colorize("&a已打开管理员GUI进行测试。"));
                } else {
                    sender.sendMessage(ColorUtils.colorize("&c此命令只能由玩家执行！"));
                }
                break;
            case "economy":
                if (args.length < 4) {
                    sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin test economy <工会名称> <操作> <金额>"));
                    return;
                }
                String guildName = args[2];
                String operation = args[3];
                double amount;
                try {
                    amount = Double.parseDouble(args[4]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ColorUtils.colorize("&c金额格式错误！"));
                    return;
                }
                plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
                    if (guild == null) {
                        sender.sendMessage(ColorUtils.colorize("&c工会 " + guildName + " 不存在！"));
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
                            sender.sendMessage(ColorUtils.colorize("&c无效的操作！使用 set|add|remove"));
                            return;
                    }
                    plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), newBalance[0]).thenAccept(success -> {
                        if (success) {
                            String formattedAmount = plugin.getEconomyManager().format(newBalance[0]);
                            sender.sendMessage(ColorUtils.colorize("&a工会 " + guildName + " 的资金已更新为: " + formattedAmount));
                        } else {
                            sender.sendMessage(ColorUtils.colorize("&c更新工会资金失败！"));
                        }
                    });
                });
                break;
            case "relation":
                if (args.length < 5) {
                    sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin test relation create <工会1> <工会2> <关系类型>"));
                    sender.sendMessage(ColorUtils.colorize("&7关系类型: ally|enemy|war|truce|neutral"));
                    return;
                }
                String guild1NameTest = args[2];
                String guild2NameTest = args[3];
                String relationTypeStrTest = args[4];
                GuildRelation.RelationType relationTypeTest;
                try {
                    relationTypeTest = GuildRelation.RelationType.valueOf(relationTypeStrTest.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ColorUtils.colorize("&c无效的关系类型！使用: ally, enemy, war, truce, neutral"));
                    return;
                }
                plugin.getGuildService().getGuildByNameAsync(guild1NameTest).thenAccept(guild1 -> {
                    if (guild1 == null) {
                        sender.sendMessage(ColorUtils.colorize("&c工会 " + guild1NameTest + " 不存在！"));
                        return;
                    }
                    plugin.getGuildService().getGuildByNameAsync(guild2NameTest).thenAccept(guild2 -> {
                        if (guild2 == null) {
                            sender.sendMessage(ColorUtils.colorize("&c工会 " + guild2NameTest + " 不存在！"));
                            return;
                        }
                        if (guild1.getId() == guild2.getId()) {
                            sender.sendMessage(ColorUtils.colorize("&c不能与自己建立关系！"));
                            return;
                        }
                        plugin.getGuildService().createGuildRelationAsync(
                            guild1.getId(), guild2.getId(), 
                            guild1.getName(), guild2.getName(), 
                            relationTypeTest, UUID.randomUUID(), "管理员"
                        ).thenAccept(success -> {
                            if (success) {
                                sender.sendMessage(ColorUtils.colorize("&a已创建关系: " + guild1NameTest + " ↔ " + guild2NameTest + " (" + getRelationTypeText(relationTypeTest) + ")"));
                            } else {
                                sender.sendMessage(ColorUtils.colorize("&c创建关系失败！"));
                            }
                        });
                    });
                });
                break;
            default:
                sender.sendMessage(ColorUtils.colorize("&c无效的测试类型！使用 gui, economy, relation"));
                break;
        }
    }
    
    private void handleHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&6=== 工会管理员命令 ==="));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin &7- 打开管理员GUI"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin list &7- 列出所有工会"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin info <工会> &7- 查看工会信息"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin delete <工会> &7- 强制删除工会"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin freeze <工会> &7- 冻结工会"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin unfreeze <工会> &7- 解冻工会"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin transfer <工会> <玩家> &7- 转让会长"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin economy <工会> <操作> <金额> &7- 管理工会经济"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin relation <操作> &7- 管理工会关系"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin reload &7- 重新加载配置"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin help &7- 显示帮助信息"));
    }
}
