package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.MainGuildGUI;
import com.guild.models.Guild;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public class GuildCreateHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                if (args.length < 2) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.create.usage", "&cUsage: /guild create <guild name> [tag] [description]");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                if (!ctx.plugin().getPermissionManager().hasPermission(player, "guild.create")) {
                    String message = ctx.languageManager().getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                // 解析参数：名称（必填）、标签（可选）、描述（可选）
                // 支持引号包裹包含空格的内容，Bukkit 自动处理引号分割
                String guildName = args[1].replaceAll("[\"']", "").trim();
                String guildTag = args.length >= 3 ? args[2].replaceAll("[\"']", "").trim() : null;
                String guildDescription = args.length >= 4
                    ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)).replaceAll("[\"']", "").trim()
                    : null;
        
                // 从配置文件读取长度限制
                int minNameLength = ctx.plugin().getConfigManager().getMainConfig().getInt("guild.min-name-length", 3);
                int maxNameLength = ctx.plugin().getConfigManager().getMainConfig().getInt("guild.max-name-length", 20);
                int maxTagLength = ctx.plugin().getConfigManager().getMainConfig().getInt("guild.max-tag-length", 6);
                int maxDescriptionLength = ctx.plugin().getConfigManager().getMainConfig().getInt("guild.max-description-length", 100);
        
                // 名称验证（去掉正则限制，与GUI一致，支持颜色字符等特殊字符）
                if (guildName.isEmpty()) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.create.name-required", "&cPlease enter a guild name first!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                if (guildName.length() < minNameLength || guildName.length() > maxNameLength) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.create.name-length", "&cGuild name must be {min}-{max} characters long!" + minNameLength + "-" + maxNameLength + " characters!");
                    message = message.replace("{min}", String.valueOf(minNameLength)).replace("{max}", String.valueOf(maxNameLength));
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                // 标签验证（空字符串视为未设置，传递 null）
                if (guildTag != null && !guildTag.isEmpty()) {
                    if (guildTag.length() > maxTagLength) {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.create.tag-too-long", "&cGuild tag is too long! Maximum {max} characters allowed.");
                        message = message.replace("{max}", String.valueOf(maxTagLength));
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }
                } else {
                    guildTag = null;
                }
        
                // 描述验证（空字符串视为未设置，传递 null）
                if (guildDescription != null && !guildDescription.isEmpty()) {
                    if (guildDescription.length() > maxDescriptionLength) {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.create.description-too-long", "&cGuild description cannot exceed {max} characters!");
                        message = message.replace("{max}", String.valueOf(maxDescriptionLength));
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }
                } else {
                    guildDescription = null;
                }
        
                final String finalTag = guildTag;
                final String finalDescription = guildDescription;
        
                CompletableFuture.runAsync(() -> {
                    try {
                        // 检查玩家是否已在公会中
                        Guild existingGuild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (existingGuild != null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "create.already-in-guild", "&cYou are already in a guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 经济系统检查
                        boolean vaultAvailable = ctx.plugin().getEconomyManager().isVaultAvailable();
                        boolean noEconomyMode = ctx.plugin().getEconomyManager().isNoEconomyMode();
                
                        if (!vaultAvailable && !noEconomyMode) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.create.economy-not-available", "&cEconomy system is not available, cannot create guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 获取创建费用（无经济模式下费用为0）
                        double creationCost = vaultAvailable
                            ? ctx.plugin().getConfigManager().getMainConfig().getDouble("guild.creation-cost", 1000.0)
                            : 0.0;
                
                        // 仅在有经济系统时检查余额并扣费
                        if (vaultAvailable && !noEconomyMode) {
                            if (!ctx.plugin().getEconomyManager().hasBalance(player, creationCost)) {
                                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                    String message = ctx.languageManager().getCoreMessage(player, "guild.create.insufficient-funds", "&cInsufficient balance! Creating a guild requires {amount}!");
                                    String msg = message.replace("{amount}", ctx.plugin().getEconomyManager().format(creationCost));
                                    player.sendMessage(ColorUtils.colorize(msg));
                                });
                                return;
                            }
                    
                            if (!ctx.plugin().getEconomyManager().withdraw(player, creationCost)) {
                                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                    String message = ctx.languageManager().getCoreMessage(player, "guild.create.payment-failed", "&cFailed to deduct creation fee!");
                                    player.sendMessage(ColorUtils.colorize(message));
                                });
                                return;
                            }
                        }
                
                        final double finalCost = creationCost;
                        boolean success = ctx.guildService().createGuild(guildName, finalTag, finalDescription, player.getUniqueId(), player.getName());
                        if (success) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.create.success", "&aGuild created successfully!");
                                player.sendMessage(ColorUtils.colorize(message));
                        
                                // 打开公会信息GUI
                                MainGuildGUI mainGuildGUI = new MainGuildGUI(ctx.plugin(), player);
                                ctx.plugin().getGuiManager().openGUI(player, mainGuildGUI);
                            });
                        } else {
                            // 如果创建失败且有扣费，退还费用
                            if (vaultAvailable && !noEconomyMode && finalCost > 0) {
                                ctx.plugin().getEconomyManager().deposit(player, finalCost);
                            }
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                if (vaultAvailable && !noEconomyMode && finalCost > 0) {
                                    String refundMessage = ctx.languageManager().getCoreMessage(player, "guild.create.payment-refunded", "&eCreation fee {amount} has been refunded.");
                                    refundMessage = refundMessage.replace("{amount}", ctx.plugin().getEconomyManager().format(finalCost));
                                    player.sendMessage(ColorUtils.colorize(refundMessage));
                                }
                        
                                String message = ctx.languageManager().getCoreMessage(player, "guild.create.exists", "&cThat guild name already exists!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                        }
                    } catch (Exception e) {
                        SubCommandErrors.logAndNotifyPlayer(ctx.plugin(), ctx.languageManager(), player,
                                "create", e, "guild.create.error",
                                "&cAn error occurred while creating the guild!");
                    }
                });

    }

}
