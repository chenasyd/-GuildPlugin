package com.guild.services;

import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.QuietLog;
import com.guild.models.Guild;
import com.guild.models.GuildContribution;
import com.guild.models.GuildEconomy;
import com.guild.models.GuildLog;
import com.guild.models.GuildMember;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class GuildEconomyService extends GuildServiceSupport {

    GuildEconomyService(GuildServiceContext ctx) {
        super(ctx);
    }

     // ==================== 公会经济系统 ====================
     
     /**
      * 初始化公会经济 (异步)
      */
     public CompletableFuture<Boolean> initializeGuildEconomyAsync(int guildId) {
         return guardServiceFutureBoolean("initializeGuildEconomy", ctx.repos.economy().insertAsync(guildId));
     }
     
     /**
      * 获取公会经济信息 (异步)
      */
     public CompletableFuture<GuildEconomy> getGuildEconomyAsync(int guildId) {
         return guardServiceFutureNullable("getGuildEconomy", ctx.repos.economy().findByGuildIdAsync(guildId));
     }
     
     /**
      * 更新公会经济 (异步)
      */
     public CompletableFuture<Boolean> updateGuildEconomyAsync(int guildId, double balance, int level, double experience, double maxExperience, int maxMembers) {
         return guardServiceFutureBoolean("updateGuildEconomy", ctx.repos.economy().updateAsync(guildId, balance, level, experience, maxExperience, maxMembers, nowString()));
     }
     
     /**
      * 添加公会贡献记录 (异步)
      */
     public CompletableFuture<Boolean> addGuildContributionAsync(int guildId, UUID playerUuid, String playerName,
                                                               double amount, GuildContribution.ContributionType type, String description) {
         return guardServiceFutureBoolean("addGuildContribution", ctx.repos.contributions().insertAsync(guildId, playerUuid, playerName, amount, type, description));
     }
     
     /**
      * 获取公会贡献记录 (异步)
      */
     public CompletableFuture<List<GuildContribution>> getGuildContributionsAsync(int guildId) {
         return guardServiceFutureList("getGuildContributions", ctx.repos.contributions().findAllByGuildIdAsync(guildId));
     }
     
     /**
      * 获取玩家贡献记录 (异步)
      */
     public CompletableFuture<List<GuildContribution>> getPlayerContributionsAsync(UUID playerUuid) {
         return guardServiceFutureList("getPlayerContributions", ctx.repos.contributions().findAllByPlayerUuidAsync(playerUuid));
     }

    /**
     * 按玩家聚合公会净贡献（WITHDRAW 为负，其余为正）。
     * 返回 Map&lt;playerUuid, netAmount&gt;。
     */
    public CompletableFuture<Map<UUID, Double>> getGuildContributionNetByPlayerAsync(int guildId) {
        return guardServiceFuture("getGuildContributionNetByPlayer", ctx.repos.contributions().computeNetByPlayerAsync(guildId), Collections.emptyMap());
    }

    /**
     * 获取公会中各成员的存款总额（聚合查询，仅 DEPOSIT 类型）
     * 返回 List<GuildContribution>，每个玩家一条，amount 为累计存款总额。
     */
    public CompletableFuture<List<GuildContribution>> getGuildContributionTotalsAsync(int guildId) {
        return guardServiceFutureList("getGuildContributionTotals", ctx.repos.contributions().computeDepositTotalsAsync(guildId));
    }
    
     // ==================== 公会经济管理方法 ====================
     
     /**
      * 更新公会余额 (异步)
      */
     public CompletableFuture<Boolean> updateGuildBalanceAsync(int guildId, double balance) {
        return guardServiceFutureBoolean("updateGuildBalance", updateGuildBalanceAsync(guildId, balance, null, null));
    }

    /**
     * 管理员调整公会余额（须 guild.admin；GUI Confirm 与纵深防御入口）
     */
    public CompletableFuture<Boolean> updateGuildBalanceByAdminAsync(int guildId, double balance,
                                                                    UUID adminUuid, String adminName) {
        if (!isGuildAdmin(adminUuid)) {
            return CompletableFuture.completedFuture(false);
        }
        return guardServiceFutureBoolean("updateGuildBalanceByAdmin", updateGuildBalanceAsync(guildId, balance,
                adminUuid != null ? adminUuid.toString() : null, adminName));
    }

    public CompletableFuture<Boolean> updateGuildBalanceAsync(int guildId, double balance,
                                                               String operatorUuid, String operatorName) {
         return guardServiceFutureBoolean("updateGuildBalance", ctx.serviceRef.getGuildByIdAsync(guildId).thenCompose(guild -> {
             if (guild == null) {
                 return CompletableFuture.completedFuture(false);
             }
             
             return CompletableFuture.supplyAsync(() -> {
                 if (ctx.repos.guilds().updateBalance(guildId, balance, nowString())) {
                     QuietLog.system("Guild balance updated: " + guild.getName() + " (ID: " + guildId + ") new balance: " + balance);

                     // 异步检查是否需要自动升级，不阻塞当前操作
                     CompletableFuture.runAsync(() -> {
                         checkAndUpgradeGuildLevel(guildId, balance);
                     });

                     // 记录资金变更日志
                     double oldBalance = guild.getBalance();
                    double change = balance - oldBalance;
                    if (change != 0) {
                        GuildLog.LogType logType = change > 0 ? GuildLog.LogType.FUND_DEPOSITED : GuildLog.LogType.FUND_WITHDRAWN;
                        String description = change > 0 ? "Fund deposited" : "Fund withdrawn";
                        String details = "Change: " + (change > 0 ? "+" : "") + change + " coins, New balance: " + balance + " coins";

                        String logUuid = (operatorUuid != null && !operatorUuid.isEmpty()) ? operatorUuid : "SYSTEM";
                        String logName = (operatorName != null && !operatorName.isEmpty()) ? operatorName : "System";
                        ctx.serviceRef.logGuildActionAsync(guildId, guild.getName(), logUuid, logName,
                            logType, description, details);
                    }

                     return true;
                 }
                 return false;
             });
         }));
     }
    
    /**
     * 更新公会等级 (异步)
     */
    public CompletableFuture<Boolean> updateGuildLevelAsync(int guildId, int level) {
        return guardServiceFutureBoolean("updateGuildLevel", CompletableFuture.supplyAsync(() -> ctx.repos.guilds().updateLevel(guildId, level)));
    }
    
    /**
     * 更新公会最大成员数 (异步)
     */
    public CompletableFuture<Boolean> updateGuildMaxMembersAsync(int guildId, int maxMembers) {
        return guardServiceFutureBoolean("updateGuildMaxMembers", CompletableFuture.supplyAsync(() -> ctx.repos.guilds().updateMaxMembers(guildId, maxMembers)));
    }
    
    /**
     * 更新公会冻结状态 (异步)
     */
    public CompletableFuture<Boolean> updateGuildFrozenStatusAsync(int guildId, boolean frozen) {
        return guardServiceFutureBoolean("updateGuildFrozenStatus", updateGuildFrozenStatusAsync(guildId, frozen, null));
    }

    public CompletableFuture<Boolean> updateGuildFrozenStatusAsync(int guildId, boolean frozen, UUID operatorUuid) {
        if (operatorUuid != null && !isGuildAdmin(operatorUuid)) {
            return CompletableFuture.completedFuture(false);
        }
        return guardServiceFutureBoolean("updateGuildFrozenStatus", ctx.serviceRef.getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return CompletableFuture.supplyAsync(() -> {
                if (ctx.repos.guilds().updateFrozen(guildId, frozen)) {
                    GuildLog.LogType logType = frozen ? GuildLog.LogType.GUILD_FROZEN : GuildLog.LogType.GUILD_UNFROZEN;
                    String description = frozen ? "公会冻结" : "公会解冻";
                    String operatorId = operatorUuid != null ? operatorUuid.toString() : "SYSTEM";
                    String operatorName = "System";
                    if (operatorUuid != null) {
                        Player op = ctx.plugin.getServer().getPlayer(operatorUuid);
                        if (op != null && op.getName() != null) {
                            operatorName = op.getName();
                        }
                    }

                    ctx.serviceRef.logGuildActionAsync(guildId, guild.getName(), operatorId, operatorName,
                        logType, description, "操作: " + (frozen ? "冻结" : "解冻"));

                    return true;
                }
                return false;
            });
        }));
    }

    /**
     * 直接插入公会成员（不做已有公会检查）。
     * 仅用于建会后插入会长，以避免额外读库造成的连接争用。
     */
    private CompletableFuture<Boolean> addGuildMemberDirectAsync(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        return ctx.repos.members().insertAsync(guildId, playerUuid, playerName, role, nowString())
                .thenApply(success -> {
                    if (success) {
                        refreshPlayerPermissions(playerUuid);
                    }
                    return success;
                });
    }

    /**
     * 检查并自动升级公会等级
     */
    private void checkAndUpgradeGuildLevel(int guildId, double currentBalance) {
        guardServiceFutureNullable("checkAndUpgradeGuildLevel", ctx.serviceRef.getGuildByIdAsync(guildId).thenAccept(guild -> {
            if (guild == null) return;
            
            int currentLevel = guild.getLevel();
            if (currentLevel >= 10) return; // 已达到最高等级
            
            // 检查是否满足升级条件
            double requiredBalance = getRequiredBalanceForLevel(currentLevel);
            if (currentBalance >= requiredBalance) {
                // 自动升级
                int newLevel = currentLevel + 1;
                int newMaxMembers = getMaxMembersForLevel(newLevel);
                
                CompletableFuture.supplyAsync(() -> {
                    if (ctx.repos.guilds().updateLevelMaxMembersAndPeak(guildId, newLevel, newMaxMembers, nowString())) {
                        QuietLog.system("Guild auto-upgraded successfully: " + guild.getName() + " (ID: " + guildId + ") level: " + currentLevel + " -> " + newLevel);

                        // 记录升级日志
                        ctx.serviceRef.logGuildActionAsync(guildId, guild.getName(), "SYSTEM", "系统",
                            GuildLog.LogType.GUILD_LEVEL_UP, "公会升级", "新等级: " + newLevel + ", 新最大成员数: " + newMaxMembers);

                        return true;
                    }
                    return false;
                });
            }
        }));
    }
    
    /**
     * 获取指定等级所需的资金
     */
    private double getRequiredBalanceForLevel(int level) {
        switch (level) {
            case 1: return 5000;
            case 2: return 10000;
            case 3: return 20000;
            case 4: return 35000;
            case 5: return 50000;
            case 6: return 75000;
            case 7: return 100000;
            case 8: return 150000;
            case 9: return 200000;
            default: return Double.MAX_VALUE;
        }
    }
    
    /**
     * 获取指定等级的最大成员数
     */
    private int getMaxMembersForLevel(int level) {
        switch (level) {
            case 1: return 6;
            case 2: return 12;
            case 3: return 18;
            case 4: return 25;
            case 5: return 35;
            case 6: return 45;
            case 7: return 60;
            case 8: return 75;
            case 9: return 90;
            case 10: return 100;
            default: return 100;
        }
    }
    
    /**
     * 获取全局最大成员数上限（从 config.yml 的 guild.max-members 读取）。
     * 作为所有公会的绝对上限，即使等级系统允许更多成员也不能超过此值。
     */
    private int getGlobalMaxMembers() {
        try {
            return ctx.plugin.getConfigManager().getMainConfig().getInt("guild.max-members", 100);
        } catch (Exception e) {
            return 100;
        }
    }
    
    /**
     * 获取公会的有效最大成员数。
     * 取公会自身存储的 max_members（由等级决定）与全局配置上限的较小值。
     * 对于已超限的存量公会，此方法仅影响新成员加入，不会踢出已有成员。
     */
    public int getEffectiveMaxMembers(Guild guild) {
        int guildMax = guild.getMaxMembers();
        int globalMax = getGlobalMaxMembers();
        return Math.min(guildMax, globalMax);
    }
    
    /**
     * 检查公会是否已满员 (异步)
     * 用于在审批申请/接受邀请前预检查，避免状态已更新但成员无法加入的情况。
     */
    public CompletableFuture<Boolean> isGuildFullAsync(int guildId) {
        return guardServiceFuture("isGuildFull", ctx.serviceRef.getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(true);
            }
            return ctx.serviceRef.getGuildMemberCountAsync(guildId).thenApply(count -> count >= getEffectiveMaxMembers(guild));
        }), true);
    }
    
    /**
     * 通知公会成员升级成功
     */
    private void notifyGuildMembersOfUpgrade(int guildId, int newLevel, int newMaxMembers) {
        guardServiceFutureNullable("notifyGuildMembersOfUpgrade", ctx.serviceRef.getGuildMembersAsync(guildId).thenAccept(members -> {
            // 在每位成员所在区域线程中发送消息（Folia 实体调度）
            for (GuildMember member : members) {
                Player player = Bukkit.getPlayer(member.getPlayerUuid());
                if (player != null && player.isOnline()) {
                    CompatibleScheduler.runTask(ctx.plugin, player, () -> {
                        String message = ctx.plugin.getLanguageManager().getCoreMessage(player, "economy.level-up", "&a公会升级成功！当前等级：{level}", "{level}", String.valueOf(newLevel), "{max_members}", String.valueOf(newMaxMembers));
                        player.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                    });
                }
            }
        }));
    }

}
