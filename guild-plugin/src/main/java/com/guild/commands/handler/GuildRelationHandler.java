package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildRelation;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public class GuildRelationHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                if (args.length < 2) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.relation.usage", "&cUsage: /guild relation <list|create|delete|accept|reject>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                String subCommand = args[1].toLowerCase();
        
                switch (subCommand) {
                    case "list":
                        handleRelationList(ctx, player);
                        break;
                    case "create":
                        if (args.length < 3) {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.create.usage", "&cUsage: /guild relation create <guild name> <relation type>");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        String targetGuildName = args[2];
                        String relationType = args.length >= 4 ? args[3] : "alliance";
                        handleRelationCreate(ctx, player, targetGuildName, relationType);
                        break;
                    case "delete":
                        if (args.length < 3) {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.delete.usage", "&cUsage: /guild relation delete <guild name>");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        targetGuildName = args[2];
                        handleRelationDelete(ctx, player, targetGuildName);
                        break;
                    case "accept":
                        if (args.length < 3) {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.accept.usage", "&cUsage: /guild relation accept <guild name>");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        targetGuildName = args[2];
                        handleRelationAccept(ctx, player, targetGuildName);
                        break;
                    case "reject":
                        if (args.length < 3) {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.reject.usage", "&cUsage: /guild relation reject <guild name>");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        targetGuildName = args[2];
                        handleRelationReject(ctx, player, targetGuildName);
                        break;
                    default:
                        String message = ctx.languageManager().getCoreMessage(player, "guild.relation.invalid-subcommand", "&cInvalid subcommand!");
                        player.sendMessage(ColorUtils.colorize(message));
                        break;
                }

    }

    private void handleRelationList(GuildCommandContext ctx, Player player) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        List<GuildRelation> relations = ctx.plugin().getGuildService().getGuildRelationsAsync(guild.getId()).join();
                        if (relations.isEmpty()) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.no-relations", "&cThis guild has no relations!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.list.title", "&aGuild Relations List:");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                
                        for (GuildRelation relation : relations) {
                            Guild targetGuild = ctx.guildService().getGuildById(relation.getOtherGuildId(guild.getId()));
                            if (targetGuild != null) {
                                String relationMessage = ctx.languageManager().getCoreMessage(player, "guild.relation.list.item", "&b{0} - &f{1}");
                                relationMessage = relationMessage.replace("{0}", targetGuild.getName());
                                relationMessage = relationMessage.replace("{1}", relation.getType().name());
                                String finalRelationMessage = relationMessage;
                                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                    player.sendMessage(ColorUtils.colorize(finalRelationMessage));
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.error", "&cAn error occurred while managing relations!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

    private void handleRelationCreate(GuildCommandContext ctx, Player player, String targetGuildName, String relationType) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.plugin().getMembershipRules().canManageGuild(player)) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.no-permission", "&cYou do not have permission to manage guild relations!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        Guild targetGuild = ctx.guildService().getGuildByName(targetGuildName);
                        if (targetGuild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.guild-not-found", "&cTarget guild does not exist!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (targetGuild.getId() == guild.getId()) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.cannot-relate-self", "&cYou cannot establish a relation with your own guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 检查是否已存在关系
                        GuildRelation existingRelation = ctx.plugin().getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId()).join();
                        if (existingRelation != null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.already-exists", "&cA relation with this guild already exists!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 创建关系
                        boolean success = ctx.plugin().getGuildService().createGuildRelationAsync(guild.getId(), targetGuild.getId(), guild.getName(), targetGuild.getName(), GuildRelation.RelationType.valueOf(relationType.toUpperCase()), player.getUniqueId(), player.getName()).join();
                
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.create.success", "&aRelation request sent!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.error", "&cAn error occurred while managing relations!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

    private void handleRelationDelete(GuildCommandContext ctx, Player player, String targetGuildName) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.plugin().getMembershipRules().canManageGuild(player)) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.no-permission", "&cYou do not have permission to manage guild relations!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        Guild targetGuild = ctx.guildService().getGuildByName(targetGuildName);
                        if (targetGuild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.guild-not-found", "&cTarget guild does not exist!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 检查关系是否存在
                        GuildRelation relation = ctx.plugin().getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId()).join();
                        if (relation == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.not-found", "&cNo relation found with this guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 删除关系
                        boolean success = ctx.plugin().getGuildService().deleteGuildRelationAsync(relation.getId()).join();
                
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.delete.success", "&aRelation deleted!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.error", "&cAn error occurred while managing relations!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

    private void handleRelationAccept(GuildCommandContext ctx, Player player, String targetGuildName) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.plugin().getMembershipRules().canManageGuild(player)) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.no-permission", "&cYou do not have permission to manage guild relations!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        Guild targetGuild = ctx.guildService().getGuildByName(targetGuildName);
                        if (targetGuild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.guild-not-found", "&cTarget guild does not exist!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 检查是否有待处理的关系请求
                        GuildRelation relation = ctx.plugin().getGuildService().getGuildRelationAsync(targetGuild.getId(), guild.getId()).join();
                        if (relation == null || relation.getStatus() != GuildRelation.RelationStatus.PENDING) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.no-pending-request", "&cNo pending relation request from this guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 接受关系请求
                        relation.setStatus(GuildRelation.RelationStatus.ACTIVE);
                        boolean success = ctx.plugin().getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.ACTIVE).join();
                
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.accept.success", "&aRelation request accepted!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.error", "&cAn error occurred while managing relations!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

    private void handleRelationReject(GuildCommandContext ctx, Player player, String targetGuildName) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.plugin().getMembershipRules().canManageGuild(player)) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.no-permission", "&cYou do not have permission to manage guild relations!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        Guild targetGuild = ctx.guildService().getGuildByName(targetGuildName);
                        if (targetGuild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.guild-not-found", "&cTarget guild does not exist!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 检查是否有待处理的关系请求
                        GuildRelation relation = ctx.plugin().getGuildService().getGuildRelationAsync(targetGuild.getId(), guild.getId()).join();
                        if (relation == null || relation.getStatus() != GuildRelation.RelationStatus.PENDING) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.relation.no-pending-request", "&cNo pending relation request from this guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 拒绝关系请求
                        boolean success = ctx.plugin().getGuildService().deleteGuildRelationAsync(relation.getId()).join();
                
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.reject.success", "&aRelation request rejected!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.relation.error", "&cAn error occurred while managing relations!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }


}
