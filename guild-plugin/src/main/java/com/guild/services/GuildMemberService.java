package com.guild.services;

import com.guild.core.utils.DebugLog;
import com.guild.core.utils.QuietLog;
import com.guild.models.Guild;
import com.guild.models.GuildApplication;
import com.guild.models.GuildInvitation;
import com.guild.models.GuildLog;
import com.guild.models.GuildMember;
import com.guild.util.NotifyUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

class GuildMemberService extends GuildServiceSupport {

    GuildMemberService(GuildServiceContext ctx) {
        super(ctx);
    }

    /**
     * 添加公会成员 (异步)
     * 包含人数上限检查：查询目标公会当前成员数，与有效上限比较。
     */
    public CompletableFuture<Boolean> addGuildMemberAsync(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        DebugLog.info(ctx.logger, "[AddMember-Debug] Starting member add: guildId=" + guildId + ", player=" + playerName + ", uuid=" + playerUuid);
        
        return guardServiceFutureBoolean("addGuildMember", ctx.serviceRef.getPlayerGuildAsync(playerUuid).thenCompose(existingGuild -> {
            if (existingGuild != null) {
                ctx.logger.warning("[AddMember-Debug] Player " + playerName + " is already in guild " + existingGuild.getName());
                return CompletableFuture.completedFuture(false);
            }
            
            DebugLog.info(ctx.logger, "[AddMember-Debug] Player not in any guild, checking capacity");
            
            // 人数上限检查：获取目标公会并比较当前成员数与有效上限
            return ctx.serviceRef.getGuildByIdAsync(guildId).thenCompose(targetGuild -> {
                if (targetGuild == null) {
                    ctx.logger.warning("[AddMember-Debug] Target guild not found: guildId=" + guildId);
                    return CompletableFuture.completedFuture(false);
                }
                
                return ctx.serviceRef.getGuildMemberCountAsync(guildId).thenCompose(memberCount -> {
                    int effectiveMax = ctx.serviceRef.getEffectiveMaxMembers(targetGuild);
                    if (memberCount >= effectiveMax) {
                        DebugLog.info(ctx.logger, "[AddMember-Debug] Guild " + targetGuild.getName() + " is full (" 
                            + memberCount + "/" + effectiveMax + "), rejecting " + playerName);
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    DebugLog.info(ctx.logger, "[AddMember-Debug] Capacity OK (" + memberCount + "/" + effectiveMax + "), preparing database insert");
                    
                    return CompletableFuture.supplyAsync(() -> {
                        if (ctx.repos.members().insert(guildId, playerUuid, playerName, role, nowString())) {
                                DebugLog.info(ctx.logger, "[AddMember-Debug] Player " + playerName + " successfully joined guild (ID: " + guildId + ")");
                                refreshPlayerPermissions(playerUuid);
                                ctx.serviceRef.logGuildActionAsync(guildId, targetGuild.getName(), playerUuid.toString(), playerName,
                                    GuildLog.LogType.MEMBER_JOINED, "成员加入", "玩家: " + playerName + ", 职位: " + role.getDisplayName());
                                fireMemberJoin(targetGuild.getId(), targetGuild.getName(), playerUuid, playerName);
                                return true;
                        }
                        ctx.logger.warning("[AddMember-Debug] INSERT did not affect any rows");
                        return false;
                    });
                });
            });
        }));
    }
    
    /**
     * 添加公会成员 (同步包装器)
     */
    public boolean addGuildMember(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        try {
            return addGuildMemberAsync(guildId, playerUuid, playerName, role).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception adding guild member: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 移除公会成员 (异步)
     */
    public CompletableFuture<Boolean> removeGuildMemberAsync(UUID playerUuid, UUID requesterUuid) {
        return guardServiceFutureBoolean("removeGuildMember", ctx.serviceRef.getGuildMemberAsync(playerUuid).thenCompose(member -> {
            if (member == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return ctx.serviceRef.getGuildMemberAsync(requesterUuid).thenCompose(requester -> {
                // 检查权限
                if (requester == null || requester.getGuildId() != member.getGuildId()) {
                    return CompletableFuture.completedFuture(false);
                }
                
                // 会长不能被踢出，除非是自我离开
                if (member.getRole() == GuildMember.Role.LEADER && !playerUuid.equals(requesterUuid)) {
                    return CompletableFuture.completedFuture(false);
                }
                
                // 只有会长和官员可以踢出成员
                if (!playerUuid.equals(requesterUuid) && 
                    requester.getRole() != GuildMember.Role.LEADER && 
                    requester.getRole() != GuildMember.Role.OFFICER) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    if (ctx.repos.members().deleteByPlayerUuid(playerUuid)) {
                                QuietLog.system("Player " + member.getPlayerName() + " left guild (ID: " + member.getGuildId() + ")");
                                refreshPlayerPermissions(playerUuid);
                                ctx.serviceRef.getGuildByIdAsync(member.getGuildId()).thenAccept(guild -> {
                                    if (guild != null) {
                                        GuildLog.LogType logType = playerUuid.equals(requesterUuid) ? 
                                            GuildLog.LogType.MEMBER_LEFT : GuildLog.LogType.MEMBER_KICKED;
                                        String description = playerUuid.equals(requesterUuid) ? "成员主动离开" : "成员被踢出";
                                        String details = "玩家: " + member.getPlayerName() + 
                                            (playerUuid.equals(requesterUuid) ? "" : ", 操作者: " + requester.getPlayerName());
                                        
                                        ctx.serviceRef.logGuildActionAsync(member.getGuildId(), guild.getName(), 
                                            requesterUuid.toString(), requester.getPlayerName(),
                                            logType, description, details);
                                        
                                        // 分发成员离开事件给模块
                                        String eventType = playerUuid.equals(requesterUuid) ? "leave" : "kicked";
                                        fireMemberLeave(guild.getId(), guild.getName(), playerUuid, member.getPlayerName(), eventType);
                                    }
                                });
                                return true;
                    }
                    return false;
                });
            });
        }));
    }
    
    /**
     * 移除公会成员 (同步包装器)
     */
    public boolean removeGuildMember(UUID playerUuid, UUID requesterUuid) {
        try {
            return removeGuildMemberAsync(playerUuid, requesterUuid).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception removing guild member: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新成员角色 (异步)
     */
    public CompletableFuture<Boolean> updateMemberRoleAsync(UUID playerUuid, GuildMember.Role newRole, UUID requesterUuid) {
        return guardServiceFutureBoolean("updateMemberRole", ctx.serviceRef.getGuildMemberAsync(playerUuid).thenCompose(member -> {
            if (member == null) {
                return CompletableFuture.completedFuture(false);
            }

            if (newRole == GuildMember.Role.LEADER) {
                return CompletableFuture.completedFuture(false);
            }
            if (member.getRole() == newRole) {
                return CompletableFuture.completedFuture(false);
            }
            if (member.getRole() == GuildMember.Role.LEADER) {
                return CompletableFuture.completedFuture(false);
            }
            if (newRole == GuildMember.Role.OFFICER && member.getRole() != GuildMember.Role.MEMBER) {
                return CompletableFuture.completedFuture(false);
            }
            if (newRole == GuildMember.Role.MEMBER && member.getRole() != GuildMember.Role.OFFICER) {
                return CompletableFuture.completedFuture(false);
            }

            return ctx.serviceRef.getGuildMemberAsync(requesterUuid).thenCompose(requester -> {
                if (requester == null || requester.getGuildId() != member.getGuildId()
                        || requester.getRole() != GuildMember.Role.LEADER) {
                    return CompletableFuture.completedFuture(false);
                }

                String oldRole = member.getRole().name();
                int guildId = member.getGuildId();

                return CompletableFuture.supplyAsync(() -> {
                    if (ctx.repos.members().updateRole(playerUuid, guildId, newRole)) {
                                QuietLog.system("Player " + member.getPlayerName() + " role updated to: " + newRole.name());
                                refreshPlayerPermissions(playerUuid);
                                ctx.serviceRef.getGuildByIdAsync(guildId).thenAccept(guild -> {
                                    if (guild != null) {
                                        GuildLog.LogType logType = newRole == GuildMember.Role.OFFICER
                                                ? GuildLog.LogType.MEMBER_PROMOTED
                                                : GuildLog.LogType.MEMBER_DEMOTED;
                                        String description = newRole == GuildMember.Role.OFFICER ? "成员升职" : "成员降职";
                                        String details = "玩家: " + member.getPlayerName() + ", 新职位: " + newRole.getDisplayName()
                                                + ", 操作者: " + requester.getPlayerName();

                                        ctx.serviceRef.logGuildActionAsync(guildId, guild.getName(),
                                                requesterUuid.toString(), requester.getPlayerName(),
                                                logType, description, details);
                                        fireMemberRoleChange(guildId, guild.getName(), playerUuid, member.getPlayerName(),
                                                oldRole, newRole.name());
                                    }
                                });
                                return true;
                    }
                    return false;
                });
            });
        }));
    }
    
    /**
     * 更新成员角色 (同步包装器)
     */
    public boolean updateMemberRole(UUID playerUuid, GuildMember.Role newRole, UUID requesterUuid) {
        try {
            return updateMemberRoleAsync(playerUuid, newRole, requesterUuid).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception updating member role: " + e.getMessage());
            return false;
        }
    }

    // ==================== SDK 直接管理方法（v1.5 新增，跳过权限检查） ====================

    /**
     * 直接移除成员（供 SDK 模块调用，跳过操作者权限检查）。
     * 不影响现有的 removeGuildMemberAsync(playerUuid, requesterUuid) 方法。
     */
    public CompletableFuture<Boolean> removeGuildMemberDirectAsync(int guildId, UUID playerUuid) {
        return guardServiceFutureBoolean("removeGuildMemberDirect", ctx.serviceRef.getGuildMemberAsync(playerUuid).thenCompose(member -> {
            if (member == null || member.getGuildId() != guildId) {
                return CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.supplyAsync(() -> {
                if (ctx.repos.members().deleteByPlayerUuid(playerUuid)) {
                            refreshPlayerPermissions(playerUuid);
                            ctx.serviceRef.getGuildByIdAsync(guildId).thenAccept(guild -> {
                                if (guild != null) {
                                    ctx.serviceRef.logGuildActionAsync(guildId, guild.getName(), playerUuid.toString(), member.getPlayerName(),
                                            GuildLog.LogType.MEMBER_KICKED, "成员被API移除", "玩家: " + member.getPlayerName());
                                    fireMemberLeave(guildId, guild.getName(), playerUuid, member.getPlayerName(), "kicked");
                                }
                            });
                            return true;
                }
                return false;
            });
        }));
    }

    /**
     * 直接修改成员角色（供 SDK 模块调用，跳过操作者权限检查）。
     * 不影响现有的 updateMemberRoleAsync(playerUuid, newRole, requesterUuid) 方法。
     */
    public CompletableFuture<Boolean> updateMemberRoleDirectAsync(int guildId, UUID playerUuid, String roleName) {
        return guardServiceFutureBoolean("updateMemberRoleDirect", ctx.serviceRef.getGuildMemberAsync(playerUuid).thenCompose(member -> {
            if (member == null || member.getGuildId() != guildId) {
                return CompletableFuture.completedFuture(false);
            }
            GuildMember.Role newRole;
            try {
                newRole = GuildMember.Role.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return CompletableFuture.completedFuture(false);
            }
            String oldRole = member.getRole().name();
            return CompletableFuture.supplyAsync(() -> {
                if (ctx.repos.members().updateRoleByPlayerUuid(playerUuid, newRole)) {
                            refreshPlayerPermissions(playerUuid);
                            ctx.serviceRef.getGuildByIdAsync(guildId).thenAccept(guild -> {
                                if (guild != null) {
                                    fireMemberRoleChange(guildId, guild.getName(), playerUuid, member.getPlayerName(), oldRole, newRole.name());
                                }
                            });
                            return true;
                }
                return false;
            });
        }));
    }
    
    /**
     * 转移会长职位 (异步)
     * 将 guild_id 的会长改为 newLeaderUuid，同时将原会长降为成员。
     */
    public CompletableFuture<Boolean> transferGuildLeadershipAsync(int guildId, UUID newLeaderUuid, String newLeaderName) {
        return guardServiceFutureBoolean("transferGuildLeadership", transferGuildLeadershipAsync(guildId, newLeaderUuid, newLeaderName, null));
    }

    /**
     * 转移会长职位 (异步，带操作者记录)
     * <p>服务层校验：目标必须为本公会成员且非当前会长；写库检查影响行数。
     */
    public CompletableFuture<Boolean> transferGuildLeadershipAsync(int guildId, UUID newLeaderUuid, String newLeaderName,
                                                                   UUID requesterUuid) {
        if (newLeaderUuid == null) {
            return CompletableFuture.completedFuture(false);
        }
        return guardServiceFutureBoolean("transferGuildLeadership", ctx.serviceRef.getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            if (requesterUuid != null && !canTransferLeadership(guild, requesterUuid)) {
                return CompletableFuture.completedFuture(false);
            }
            UUID oldLeaderUuid = guild.getLeaderUuid();
            if (newLeaderUuid.equals(oldLeaderUuid)) {
                return CompletableFuture.completedFuture(false);
            }
            return ctx.serviceRef.getGuildMemberAsync(guildId, newLeaderUuid).thenCompose(newMember -> {
                if (newMember == null) {
                    return CompletableFuture.completedFuture(false);
                }
                String resolvedName = (newLeaderName != null && !newLeaderName.isEmpty())
                        ? newLeaderName : newMember.getPlayerName();
                return CompletableFuture.supplyAsync(() -> {
                    try (Connection conn = ctx.databaseManager.getConnection()) {
                        conn.setAutoCommit(false);
                        try {
                            // 1. 更新 guilds 表新会长
                            int guildUpdated = ctx.repos.guilds().updateLeader(conn, guildId, newLeaderUuid, resolvedName);
                            if (guildUpdated <= 0) {
                                conn.rollback();
                                return false;
                            }
                            // 2. 原会长降级为成员
                            ctx.repos.members().demoteLeaderToMember(conn, guildId, oldLeaderUuid);
                            // 3. 新会长升级
                            int newLeaderUpdated = ctx.repos.members().promoteMemberToLeader(conn, guildId, newLeaderUuid);
                            if (newLeaderUpdated <= 0) {
                                conn.rollback();
                                return false;
                            }
                            conn.commit();

                            // 刷新权限缓存
                            refreshPlayerPermissions(oldLeaderUuid);
                            refreshPlayerPermissions(newLeaderUuid);

                            // 触发角色变更事件
                            String oldLeaderName = guild.getLeaderName();
                            fireMemberRoleChange(guildId, guild.getName(), oldLeaderUuid, oldLeaderName, "LEADER", "MEMBER");
                            fireMemberRoleChange(guildId, guild.getName(), newLeaderUuid, resolvedName,
                                    newMember.getRole().name(), "LEADER");

                            // 记录日志
                            String actor = newLeaderUuid.toString();
                            String actorName = resolvedName;
                            if (requesterUuid != null) {
                                actor = requesterUuid.toString();
                                try {
                                    org.bukkit.OfflinePlayer requester = ctx.plugin.getServer().getOfflinePlayer(requesterUuid);
                                    if (requester.getName() != null) {
                                        actorName = requester.getName();
                                    }
                                } catch (Exception e) {
                                    ctx.logger.log(Level.FINE,
                                            "Could not resolve requester name for " + requesterUuid + ": " + e.getMessage());
                                }
                            }
                            String desc = "会长由 " + oldLeaderName + " 转移给 " + resolvedName;
                            ctx.serviceRef.logGuildActionAsync(guildId, guild.getName(), actor, actorName,
                                    GuildLog.LogType.LEADER_TRANSFERRED, desc, desc);

                            return true;
                        } catch (SQLException e) {
                            conn.rollback();
                            ctx.logger.severe("Error transferring leadership: " + e.getMessage());
                            return false;
                        }
                    } catch (SQLException e) {
                        ctx.logger.severe("Database error transferring leadership: " + e.getMessage());
                        return false;
                    }
                });
            });
        }));
    }
    /**
     * 提交申请 (异步)
     */
    public CompletableFuture<Boolean> submitApplicationAsync(int guildId, UUID playerUuid, String playerName, String message) {
        return guardServiceFutureBoolean("submitApplication", ctx.repos.applications().hasPendingAsync(playerUuid, guildId).thenCompose(hasPending -> {
            if (hasPending) {
                return CompletableFuture.completedFuture(false);
            }
            return ctx.repos.applications().insertAsync(guildId, playerUuid, playerName, message,
                    GuildApplication.ApplicationStatus.PENDING, nowString()).thenApply(inserted -> {
                if (inserted) {
                    QuietLog.system("Player " + playerName + " submitted a join application (guild ID: " + guildId + ")");
                    ctx.serviceRef.getGuildByIdAsync(guildId).thenAccept(guild -> {
                        if (guild != null) {
                            ctx.serviceRef.logGuildActionAsync(guildId, guild.getName(), playerUuid.toString(), playerName,
                                    GuildLog.LogType.APPLICATION_SUBMITTED, "申请提交", "申请消息: " + message);
                            GuildApplication application = new GuildApplication(guildId, playerUuid, playerName, message);
                            NotifyUtils.notifyLeaderNewApplication(ctx.plugin, guild, application);
                        }
                    });
                }
                return inserted;
            });
        }));
    }
    
    /**
     * 提交申请 (同步包装器)
     */
    public boolean submitApplication(int guildId, UUID playerUuid, String playerName, String message) {
        try {
            return submitApplicationAsync(guildId, playerUuid, playerName, message).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception submitting application: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 处理申请 (异步)
     * 审批通过前先检查公会是否满员，避免申请状态已更新但成员无法加入的情况。
     */
    public CompletableFuture<Boolean> processApplicationAsync(int applicationId, GuildApplication.ApplicationStatus status, UUID processorUuid) {
        return guardServiceFutureBoolean("processApplication", ctx.serviceRef.getApplicationByIdAsync(applicationId).thenCompose(application -> {
            if (application == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return ctx.serviceRef.getGuildMemberAsync(processorUuid).thenCompose(processor -> {
                // 检查处理者权限
                if (processor == null || processor.getGuildId() != application.getGuildId() || 
                    (processor.getRole() != GuildMember.Role.LEADER && processor.getRole() != GuildMember.Role.OFFICER)) {
                    return CompletableFuture.completedFuture(false);
                }
                
                // 审批通过前预检查公会人数上限
                if (status == GuildApplication.ApplicationStatus.APPROVED) {
                    return ctx.serviceRef.isGuildFullAsync(application.getGuildId()).thenCompose(isFull -> {
                        if (isFull) {
                            QuietLog.system("Application approval rejected: guild " + application.getGuildId() 
                                + " is at member capacity, applicant=" + application.getPlayerName());
                            return CompletableFuture.completedFuture(false);
                        }
                        return doProcessApplication(applicationId, application, status, processor, processorUuid);
                    });
                }
                
                return doProcessApplication(applicationId, application, status, processor, processorUuid);
            });
        }));
    }
    
    /**
     * 执行申请处理的数据库操作（内部方法）
     */
    private CompletableFuture<Boolean> doProcessApplication(int applicationId, GuildApplication application,
            GuildApplication.ApplicationStatus status, GuildMember processor, UUID processorUuid) {
        return ctx.repos.applications().updateStatusAsync(applicationId, status).thenApply(updated -> {
            if (updated) {
                QuietLog.system("Application processed: " + application.getPlayerName() + " -> " + status.name());
                ctx.serviceRef.getGuildByIdAsync(application.getGuildId()).thenAccept(guild -> {
                    if (guild != null) {
                        GuildLog.LogType logType = status == GuildApplication.ApplicationStatus.APPROVED ?
                                GuildLog.LogType.APPLICATION_ACCEPTED : GuildLog.LogType.APPLICATION_REJECTED;
                        String description = status == GuildApplication.ApplicationStatus.APPROVED ? "申请接受" : "申请拒绝";
                        String details = "申请人: " + application.getPlayerName() + ", 处理者: " + processor.getPlayerName();
                        ctx.serviceRef.logGuildActionAsync(application.getGuildId(), guild.getName(),
                                processorUuid.toString(), processor.getPlayerName(),
                                logType, description, details);
                    }
                });
            }
            return updated;
        }).thenCompose(success -> {
            if (success && status == GuildApplication.ApplicationStatus.APPROVED) {
                // 如果申请被通过，自动添加成员（ctx.members.addGuildMemberAsync 内部有二次容量校验）
                return addGuildMemberAsync(application.getGuildId(), application.getPlayerUuid(), 
                                          application.getPlayerName(), GuildMember.Role.MEMBER);
            }
            return CompletableFuture.completedFuture(success);
        });
    }
    
    /**
     * 处理申请 (同步包装器)
     */
    public boolean processApplication(int applicationId, GuildApplication.ApplicationStatus status, UUID processorUuid) {
        try {
            return processApplicationAsync(applicationId, status, processorUuid).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception processing application: " + e.getMessage());
            return false;
        }
    }
     // ==================== 邀请系统 ====================
     
     /**
      * 发送邀请 (异步)
      */
     public CompletableFuture<Boolean> sendInvitationAsync(int guildId, UUID inviterUuid, String inviterName, UUID targetUuid, String targetName) {
         return guardServiceFutureBoolean("sendInvitation", ctx.serviceRef.getGuildMemberAsync(guildId, inviterUuid).thenCompose(inviterMember -> {
             if (inviterMember == null || !ctx.plugin.getMembershipRules().canInvite(inviterMember)) {
                 return CompletableFuture.completedFuture(false);
             }
             return ctx.serviceRef.getPlayerGuildAsync(targetUuid).thenCompose(existingGuild -> {
             if (existingGuild != null) {
                 return CompletableFuture.completedFuture(false);
             }
             
             return ctx.serviceRef.getPendingInvitationAsync(targetUuid, guildId).thenCompose(existingInvitation -> {
                 if (existingInvitation != null) {
                     return CompletableFuture.completedFuture(false);
                 }
                 
                 return ctx.repos.invitations().insertAsync(guildId, targetUuid, targetName, inviterUuid, inviterName,
                         "PENDING", plusMinutesString(30), nowString()).thenCompose(ok -> {
                     if (!Boolean.TRUE.equals(ok)) {
                         return CompletableFuture.completedFuture(false);
                     }
                     QuietLog.system("Invitation sent successfully: " + inviterName + " -> " + targetName + " (guild ID: " + guildId + ")");
                     return ctx.serviceRef.getGuildByIdAsync(guildId).thenCompose(g -> {
                         String guildName = g != null ? g.getName() : ("#" + guildId);
                         return ctx.serviceRef.logGuildActionAsync(guildId, guildName,
                                 inviterUuid.toString(), inviterName,
                                 GuildLog.LogType.INVITATION_SENT,
                                 "Invitation sent",
                                 "target: " + targetName + " (" + targetUuid + ")")
                                 .thenApply(v -> true);
                     });
                 });
             });
         });
         }));
     }
     
     /**
      * 发送邀请 (同步包装器)
      */
     public boolean sendInvitation(int guildId, UUID inviterUuid, String inviterName, UUID targetUuid, String targetName) {
         try {
             return sendInvitationAsync(guildId, inviterUuid, inviterName, targetUuid, targetName).get();
         } catch (Exception e) {
             ctx.logger.severe("Exception sending invitation: " + e.getMessage());
             return false;
         }
     }
     
    /**
     * 处理邀请 (异步) - 通过inviterUuid查找
     */
    public CompletableFuture<Boolean> processInvitationAsync(UUID targetUuid, UUID inviterUuid, boolean accept) {
        return guardServiceFutureBoolean("processInvitation", ctx.serviceRef.getPendingInvitationAsync(targetUuid, inviterUuid).thenCompose(invitation -> {
            if (invitation == null) {
                return CompletableFuture.completedFuture(false);
            }
            return processInvitationDirectAsync(invitation, accept);
        }));
    }
    
    /**
     * 处理邀请 (异步) - 直接处理邀请对象
     * 接受邀请前先检查公会是否满员，避免邀请状态已更新但成员无法加入的情况。
     */
    public CompletableFuture<Boolean> processInvitationDirectAsync(GuildInvitation invitation, boolean accept) {
        DebugLog.info(ctx.logger, "[Process-Debug] Starting invitation processing: id=" + invitation.getId() + ", guildId=" + invitation.getGuildId() + ", target=" + invitation.getTargetUuid() + ", accept=" + accept);
        
        // 接受邀请前预检查公会人数上限
        if (accept) {
            return guardServiceFutureBoolean("processInvitationDirect", ctx.serviceRef.isGuildFullAsync(invitation.getGuildId()).thenCompose(isFull -> {
                if (isFull) {
                    DebugLog.info(ctx.logger, "[Process-Debug] Invitation accept rejected: guild " + invitation.getGuildId() 
                        + " is at member capacity, target=" + invitation.getTargetUuid());
                    return CompletableFuture.completedFuture(false);
                }
                return doProcessInvitation(invitation, accept);
            }));
        }
        
        return guardServiceFutureBoolean("processInvitationDirect", doProcessInvitation(invitation, accept));
    }
    
    /**
     * 执行邀请处理的数据库操作（内部方法）
     */
    private CompletableFuture<Boolean> doProcessInvitation(GuildInvitation invitation, boolean accept) {
        String status = accept ? "ACCEPTED" : "DECLINED";
        return ctx.repos.invitations().updateStatusAsync(invitation.getId(), status).thenApply(updated -> {
            if (updated) {
                DebugLog.info(ctx.logger, "[Process-Debug] Invitation status updated: " + invitation.getTargetUuid() + " -> " + status);
            } else {
                ctx.logger.warning("[Process-Debug] Invitation status update failed, no rows affected: id=" + invitation.getId());
            }
            return updated;
        }).thenCompose(success -> {
            if (!Boolean.TRUE.equals(success)) {
                return CompletableFuture.completedFuture(false);
            }
            GuildLog.LogType inviteLog = accept
                    ? GuildLog.LogType.INVITATION_ACCEPTED
                    : GuildLog.LogType.INVITATION_REJECTED;
            return ctx.serviceRef.getGuildByIdAsync(invitation.getGuildId()).thenCompose(g -> {
                String guildName = g != null ? g.getName() : ("#" + invitation.getGuildId());
                return ctx.serviceRef.logGuildActionAsync(invitation.getGuildId(), guildName,
                        invitation.getTargetUuid().toString(), invitation.getTargetName(),
                        inviteLog,
                        accept ? "Invitation accepted" : "Invitation declined",
                        "inviter: " + invitation.getInviterName()
                                + " (" + invitation.getInviterUuid() + ")")
                        .thenApply(v -> g);
            }).thenCompose(g -> {
                if (!accept) {
                    return CompletableFuture.completedFuture(true);
                }
                DebugLog.info(ctx.logger, "[Process-Debug] Preparing to add player to guild: guildId="
                        + invitation.getGuildId() + ", player=" + invitation.getTargetUuid());
                return addGuildMemberAsync(invitation.getGuildId(), invitation.getTargetUuid(),
                        invitation.getTargetName(), GuildMember.Role.MEMBER)
                        .thenCompose(addSuccess -> {
                            if (addSuccess) {
                                DebugLog.info(ctx.logger, "[Process-Debug] Player added, dispatching event");
                                if (g != null) {
                                    fireMemberJoin(g.getId(), g.getName(),
                                            invitation.getTargetUuid(), invitation.getTargetName());
                                }
                                return CompletableFuture.completedFuture(true);
                            }
                            ctx.logger.warning("[Process-Debug] Failed to add player to guild");
                            return CompletableFuture.completedFuture(false);
                        });
            });
        });
    }
     
     /**
      * 处理邀请 (同步包装器)
      */
     public boolean processInvitation(UUID targetUuid, UUID inviterUuid, boolean accept) {
         try {
             return processInvitationAsync(targetUuid, inviterUuid, accept).get();
         } catch (Exception e) {
             ctx.logger.severe("Exception processing invitation: " + e.getMessage());
             return false;
         }
     }
    
    /**
     * 清理过期的公会邀请 (异步) - 将过期邀请状态更新为EXPIRED
     * 建议定时调用，避免数据库中积累过多过期邀请
     */
    public CompletableFuture<Integer> cleanupExpiredInvitationsAsync() {
        return guardServiceFutureInt("cleanupExpiredInvitations", ctx.repos.invitations().markExpiredBeforeAsync(nowString()).thenApply(affectedRows -> {
            if (affectedRows > 0) {
                QuietLog.system("Cleaned up " + affectedRows + " expired guild invitations");
            }
            return affectedRows;
        }));
    }
    
    /**
     * 清理旧的已处理邀请 (异步) - 删除已过期超过指定天数的邀请记录
     * @param days 保留天数，超过此天数的已处理邀请（ACCEPTED/DECLINED/EXPIRED）将被删除
     */
    public CompletableFuture<Integer> cleanupOldProcessedInvitationsAsync(int days) {
        return guardServiceFutureInt("cleanupOldProcessedInvitations", ctx.repos.invitations().deleteOldProcessedAsync(days).thenApply(affectedRows -> {
            if (affectedRows > 0) {
                QuietLog.system("Cleaned up " + affectedRows + " old processed invitation records");
            }
            return affectedRows;
        }));
    }
    CompletableFuture<Boolean> addGuildMemberDirectAsync(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        return ctx.repos.members().insertAsync(guildId, playerUuid, playerName, role, nowString())
                .thenApply(success -> {
                    if (success) {
                        refreshPlayerPermissions(playerUuid);
                    }
                    return success;
                });
    }

}
