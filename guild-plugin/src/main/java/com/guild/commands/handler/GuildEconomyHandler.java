package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class GuildEconomyHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
        if (args.length >= 1) {
            String cmd = args[0].toLowerCase();
            if (cmd.equals("economy")) {
                handleEconomy(ctx, player, args);
            } else if (cmd.equals("deposit")) {
                handleDeposit(ctx, player, args);
            } else if (cmd.equals("withdraw")) {
                handleWithdraw(ctx, player, args);
            } else if (cmd.equals("transfer")) {
                handleTransfer(ctx, player, args);
            }
        }
    }

    private void handleEconomy(GuildCommandContext ctx, Player player, String[] args) {
                if (args.length < 2) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.economy.usage", "&cUsage: /guild economy <info|deposit|withdraw|transfer>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                String subCommand = args[1].toLowerCase();
        
                switch (subCommand) {
                    case "info":
                        handleEconomyInfo(ctx, player);
                        break;
                    case "deposit":
                        if (args.length < 3) {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.economy.deposit.usage", "&cUsage: /guild economy deposit <amount>");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        try {
                            double amount = Double.parseDouble(args[2]);
                            handleDeposit(ctx, player, amount);
                        } catch (NumberFormatException e) {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                        break;
                    case "withdraw":
                        if (args.length < 3) {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.economy.withdraw.usage", "&cUsage: /guild economy withdraw <amount>");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        try {
                            double amount = Double.parseDouble(args[2]);
                            handleWithdraw(ctx, player, amount);
                        } catch (NumberFormatException e) {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                        break;
                    case "transfer":
                        if (args.length < 4) {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.economy.transfer.usage", "&cUsage: /guild economy transfer <guild name> <amount>");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        String targetGuildName = args[2];
                        try {
                            double amount = Double.parseDouble(args[3]);
                            handleTransfer(ctx, player, targetGuildName, amount);
                        } catch (NumberFormatException e) {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                        break;
                    default:
                        String message = ctx.languageManager().getCoreMessage(player, "guild.economy.invalid-subcommand", "&cInvalid subcommand!");
                        player.sendMessage(ColorUtils.colorize(message));
                        break;
                }

    }

    private void handleEconomyInfo(GuildCommandContext ctx, Player player) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.economy.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        double balance = guild.getBalance();
                
                        String message = ctx.languageManager().getCoreMessage(player, "guild.economy.info", "&aGuild Economy Info:\n&bBalance: &f{0} coins");
                        message = message.replace("{0}", String.format("%.2f", balance));
                        String finalMessage = message;
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            player.sendMessage(ColorUtils.colorize(finalMessage));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.economy.error", "&cAn error occurred while fetching economy info!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

    private void handleDeposit(GuildCommandContext ctx, Player player, String[] args) {
                if (args.length < 2) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.deposit.usage", "&cUsage: /guild deposit <amount>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                try {
                    double amount = Double.parseDouble(args[1]);
                    handleDeposit(ctx, player, amount);
                } catch (NumberFormatException e) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                    player.sendMessage(ColorUtils.colorize(message));
                }

    }

    private void handleDeposit(GuildCommandContext ctx, Player player, double amount) {
                if (amount <= 0) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.deposit.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.plugin().getEconomyManager().hasBalance(player, amount)) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.deposit.insufficient-funds", "&cYou don't have enough money!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 扣除玩家余额
                        if (!ctx.plugin().getEconomyManager().withdraw(player, amount)) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.deposit.error", "&cAn error occurred while depositing!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 增加公会余额（传入操作者信息，避免 updateGuildBalanceAsync 内部产生 SYSTEM 匿名日志）
                        boolean success = ctx.plugin().getGuildService().updateGuildBalanceAsync(
                                guild.getId(), guild.getBalance() + amount,
                                player.getUniqueId().toString(), player.getName()).join();
                        if (success) {
                            // 记录投资
                            ctx.plugin().getGuildInvestmentService().recordDeposit(guild.getId(), player.getUniqueId(), player.getName(), amount);
                            // 写入 guild_contributions 表（供 GuildFundsGUI 展示）
                            ctx.plugin().getGuildService().addGuildContributionAsync(guild.getId(), player.getUniqueId(),
                                    player.getName(), amount,
                                    com.guild.models.GuildContribution.ContributionType.DEPOSIT,
                                    ctx.languageManager().getCoreMessage(player, "deposit.contribution-desc",
                                            "{player} deposited {amount}")
                                            .replace("{player}", player.getName())
                                            .replace("{amount}", String.format("%.2f", amount)));
                            // 分发存款事件给模块
                            ctx.plugin().getGuildService().notifyEconomyDeposit(guild.getId(), guild.getName(), player.getUniqueId(), player.getName(), amount);
                        }
                
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.deposit.success", "&aSuccessfully deposited {0} coins into the guild account!");
                            String msg = message.replace("{0}", String.format("%.2f", amount));
                            player.sendMessage(ColorUtils.colorize(msg));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.deposit.error", "&cAn error occurred while depositing!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

    private void handleWithdraw(GuildCommandContext ctx, Player player, String[] args) {
                if (args.length < 2) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.withdraw.usage", "&cUsage: /guild withdraw <amount>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                try {
                    double amount = Double.parseDouble(args[1]);
                    handleWithdraw(ctx, player, amount);
                } catch (NumberFormatException e) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                    player.sendMessage(ColorUtils.colorize(message));
                }

    }

    private void handleWithdraw(GuildCommandContext ctx, Player player, double amount) {
                if (amount <= 0) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.withdraw.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.guildService().isGuildLeader(player.getUniqueId())) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.withdraw.only-master", "&cOnly the leader can withdraw from the guild account!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (guild.getBalance() < amount) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.withdraw.insufficient-funds", "&cInsufficient guild account balance!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 增加玩家余额
                        if (!ctx.plugin().getEconomyManager().deposit(player, amount)) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.withdraw.error", "&cAn error occurred while withdrawing!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 减少公会余额（传入操作者信息，避免产生 SYSTEM 匿名日志）
                        ctx.plugin().getGuildService().updateGuildBalanceAsync(
                                guild.getId(), guild.getBalance() - amount,
                                player.getUniqueId().toString(), player.getName()).join();
                        // 记录取款
                        ctx.plugin().getGuildInvestmentService().recordWithdraw(guild.getId(), player.getUniqueId(), amount);
                        // 分发取款事件给模块
                        ctx.plugin().getGuildService().notifyEconomyWithdraw(guild.getId(), guild.getName(), player.getUniqueId(), player.getName(), amount);
                
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.withdraw.success", "&aSuccessfully withdrew {0} coins from the guild account!");
                            String msg = message.replace("{0}", String.format("%.2f", amount));
                            player.sendMessage(ColorUtils.colorize(msg));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.withdraw.error", "&cAn error occurred while withdrawing!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

    private void handleTransfer(GuildCommandContext ctx, Player player, String[] args) {
                if (args.length < 3) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.usage", "&cUsage: /guild transfer <player name> <amount>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                String targetName = args[1];
                try {
                    double amount = Double.parseDouble(args[2]);
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    if (targetPlayer == null) {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.player-not-found", "&cTarget player is not online!");
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }
            
                    handleTransfer(ctx, player, targetPlayer, amount);
                } catch (NumberFormatException e) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                    player.sendMessage(ColorUtils.colorize(message));
                }

    }

    private void handleTransfer(GuildCommandContext ctx, Player player, String targetGuildName, double amount) {
                if (amount <= 0) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild sourceGuild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (sourceGuild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.guildService().isGuildLeader(player.getUniqueId())) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.only-master", "&cOnly the leader can transfer between guilds!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        Guild targetGuild = ctx.guildService().getGuildByName(targetGuildName);
                        if (targetGuild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.target-not-found", "&cTarget guild does not exist!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (sourceGuild.getId() == targetGuild.getId()) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.same-guild", "&cYou cannot transfer to your own guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (sourceGuild.getBalance() < amount) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.insufficient-funds", "&cInsufficient guild account balance!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 减少源公会余额
                        boolean sourceSuccess = ctx.plugin().getGuildService().updateGuildBalanceAsync(sourceGuild.getId(), sourceGuild.getBalance() - amount).join();
                
                        // 增加目标公会余额
                        boolean targetSuccess = ctx.plugin().getGuildService().updateGuildBalanceAsync(targetGuild.getId(), targetGuild.getBalance() + amount).join();
                
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.success", "&aSuccessfully transferred {0} coins to {1}!");
                            String msg = message.replace("{0}", String.format("%.2f", amount));
                            msg = msg.replace("{1}", targetGuild.getName());
                            player.sendMessage(ColorUtils.colorize(msg));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.error", "&cAn error occurred while transferring!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

    private void handleTransfer(GuildCommandContext ctx, Player player, Player targetPlayer, double amount) {
                if (amount <= 0) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.guildService().hasGuildPermission(player.getUniqueId())) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.no-permission", "&cYou do not have permission to transfer!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (guild.getBalance() < amount) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.insufficient-funds", "&cInsufficient guild account balance!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 减少公会余额
                        ctx.plugin().getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() - amount).join();
                
                        // 增加目标玩家余额
                        if (!ctx.plugin().getEconomyManager().deposit(targetPlayer, amount)) {
                            // 如果转账失败，恢复公会余额
                            ctx.plugin().getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() + amount).join();
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.error", "&cAn error occurred while transferring!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.success", "&aSuccessfully transferred {0} coins to {1}!");
                            String msg = message.replace("{0}", String.format("%.2f", amount));
                            msg = msg.replace("{1}", targetPlayer.getName());
                            player.sendMessage(ColorUtils.colorize(msg));
                        });
                
                        // 通知目标玩家
                        CompatibleScheduler.runTask(ctx.plugin(), targetPlayer, () -> {
                            String targetMessage = ctx.languageManager().getCoreMessage(targetPlayer, "guild.transfer.received", "&aYou received {0} coins!");
                            String msg = targetMessage.replace("{0}", String.format("%.2f", amount));
                            targetPlayer.sendMessage(ColorUtils.colorize(msg));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.transfer.error", "&cAn error occurred while transferring!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

}
