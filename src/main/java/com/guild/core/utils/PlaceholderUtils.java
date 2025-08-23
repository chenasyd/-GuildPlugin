package com.guild.core.utils;

import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import com.guild.core.time.TimeProvider;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 占位符处理工具类
 */
public class PlaceholderUtils {
    
    private static final DateTimeFormatter DATE_FORMATTER = TimeProvider.FULL_FORMATTER;
    
    /**
     * 替换工会相关占位符
     * @param text 原始文本
     * @param guild 工会对象
     * @param player 玩家对象
     * @return 替换后的文本
     */
    public static String replaceGuildPlaceholders(String text, Guild guild, Player player) {
        if (text == null || guild == null) {
            return text;
        }
        
        String result = text
            // 工会基本信息
            .replace("{guild_name}", guild.getName())
            .replace("{guild_tag}", guild.getTag() != null ? guild.getTag() : "")
            .replace("{guild_description}", guild.getDescription() != null ? guild.getDescription() : "")
            .replace("{guild_id}", String.valueOf(guild.getId()))
            .replace("{guild_created_time}", guild.getCreatedAt().format(DATE_FORMATTER))
            .replace("{guild_created_date}", guild.getCreatedAt().toLocalDate().toString())
            
            // 工会领导信息
            .replace("{leader_name}", guild.getLeaderName())
            .replace("{leader_uuid}", guild.getLeaderUuid().toString())
            
            // 工会位置信息
            .replace("{guild_home_world}", guild.getHomeWorld() != null ? guild.getHomeWorld() : "")
            .replace("{guild_home_x}", String.valueOf(guild.getHomeX()))
            .replace("{guild_home_y}", String.valueOf(guild.getHomeY()))
            .replace("{guild_home_z}", String.valueOf(guild.getHomeZ()))
            .replace("{guild_home_location}", formatHomeLocation(guild))
            
            // 玩家信息
            .replace("{player_name}", player != null ? player.getName() : "")
            .replace("{player_uuid}", player != null ? player.getUniqueId().toString() : "")
            .replace("{player_display_name}", player != null ? player.getDisplayName() : "")
            
            // 静态信息
            .replace("{guild_level}", String.valueOf(guild.getLevel()))
            .replace("{guild_balance}", String.valueOf(guild.getBalance()))
            .replace("{guild_max_members}", String.valueOf(guild.getMaxMembers()))
            .replace("{guild_frozen}", guild.isFrozen() ? "已冻结" : "正常")
            
            // 经济相关变量 - 支持GUI配置中的变量名
            .replace("{guild_balance_formatted}", formatBalance(guild.getBalance()))
            .replace("{guild_next_level_requirement}", getNextLevelRequirement(guild.getLevel()))
            .replace("{guild_level_progress}", getLevelProgress(guild.getLevel(), guild.getBalance()))
            .replace("{guild_upgrade_cost}", getUpgradeCost(guild.getLevel()))
            .replace("{guild_currency_name}", "金币")
            .replace("{guild_currency_name_singular}", "金币")
            
            // 兼容性变量 - 支持旧格式
            .replace("{guild_max_exp}", getNextLevelRequirement(guild.getLevel()))
            .replace("{guild_exp_percentage}", getLevelProgress(guild.getLevel(), guild.getBalance()));
        
        // 处理颜色代码
        return ColorUtils.colorize(result);
    }
    
    /**
     * 异步替换工会相关占位符（包含动态数据）
     * @param text 原始文本
     * @param guild 工会对象
     * @param player 玩家对象
     * @param guildService 工会服务
     * @return 替换后的文本的CompletableFuture
     */
    public static CompletableFuture<String> replaceGuildPlaceholdersAsync(String text, Guild guild, Player player, com.guild.services.GuildService guildService) {
        if (text == null || guild == null) {
            return CompletableFuture.completedFuture(text);
        }
        
        // 先替换静态占位符
        String result = replaceGuildPlaceholders(text, guild, player);
        
        // 异步获取动态数据
        return guildService.getGuildMemberCountAsync(guild.getId()).thenApply(memberCount -> {
            try {
                return result
                    .replace("{member_count}", String.valueOf(memberCount))
                    .replace("{online_member_count}", String.valueOf(memberCount)) // 暂时使用总成员数，后续可以添加在线统计
                    .replace("{guild_max_exp}", getNextLevelRequirement(guild.getLevel()))
                    .replace("{guild_exp_percentage}", getLevelProgress(guild.getLevel(), guild.getBalance()));
            } catch (Exception e) {
                // 如果获取失败，使用默认值
                return result
                    .replace("{member_count}", "0")
                    .replace("{online_member_count}", "0")
                    .replace("{guild_max_exp}", getNextLevelRequirement(guild.getLevel()))
                    .replace("{guild_exp_percentage}", getLevelProgress(guild.getLevel(), guild.getBalance()));
            }
        });
    }
    
    /**
     * 替换成员相关占位符
     * @param text 原始文本
     * @param member 成员对象
     * @param guild 工会对象
     * @return 替换后的文本
     */
    public static String replaceMemberPlaceholders(String text, GuildMember member, Guild guild) {
        if (text == null || member == null) {
            return text;
        }
        
        String result = text
            // 成员基本信息
            .replace("{member_name}", member.getPlayerName())
            .replace("{member_uuid}", member.getPlayerUuid().toString())
            .replace("{member_role}", getRoleDisplayName(member.getRole()))
            .replace("{member_role_color}", getRoleColor(member.getRole()))
            .replace("{member_join_time}", member.getJoinedAt().format(DATE_FORMATTER))
            .replace("{member_join_date}", member.getJoinedAt().toLocalDate().toString())
            
            // 工会信息
            .replace("{guild_name}", guild != null ? guild.getName() : "")
            .replace("{guild_tag}", guild != null && guild.getTag() != null ? guild.getTag() : "");
        
        // 处理颜色代码
        return ColorUtils.colorize(result);
    }
    
    /**
     * 替换申请相关占位符
     * @param text 原始文本
     * @param applicantName 申请人名称
     * @param guildName 工会名称
     * @param applyTime 申请时间
     * @return 替换后的文本
     */
    public static String replaceApplicationPlaceholders(String text, String applicantName, String guildName, java.time.LocalDateTime applyTime) {
        if (text == null) {
            return text;
        }
        
        String result = text
            .replace("{applicant_name}", applicantName != null ? applicantName : "")
            .replace("{guild_name}", guildName != null ? guildName : "")
            .replace("{apply_time}", applyTime != null ? applyTime.format(DATE_FORMATTER) : "")
            .replace("{apply_date}", applyTime != null ? applyTime.toLocalDate().toString() : "");
        
        // 处理颜色代码
        return ColorUtils.colorize(result);
    }
    
    /**
     * 替换通用占位符
     * @param text 原始文本
     * @param placeholders 占位符映射
     * @return 替换后的文本
     */
    public static String replacePlaceholders(String text, String... placeholders) {
        if (text == null) {
            return text;
        }
        
        String result = text;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                result = result.replace(placeholder, value != null ? value : "");
            }
        }
        
        // 处理颜色代码
        return ColorUtils.colorize(result);
    }
    
    /**
     * 格式化工会家位置
     */
    private static String formatHomeLocation(Guild guild) {
        if (guild.getHomeWorld() == null) {
            return "未设置";
        }
        return String.format("%s %.1f, %.1f, %.1f", 
            guild.getHomeWorld(), guild.getHomeX(), guild.getHomeY(), guild.getHomeZ());
    }
    
    /**
     * 获取角色显示名称
     */
    private static String getRoleDisplayName(GuildMember.Role role) {
        switch (role) {
            case LEADER: return "会长";
            case OFFICER: return "官员";
            case MEMBER: return "成员";
            default: return "未知";
        }
    }
    
    /**
     * 获取角色颜色
     */
    private static String getRoleColor(GuildMember.Role role) {
        switch (role) {
            case LEADER: return "&6";
            case OFFICER: return "&e";
            case MEMBER: return "&7";
            default: return "&f";
        }
    }
    
    /**
     * 格式化余额
     */
    private static String formatBalance(double balance) {
        // 尝试使用经济管理器格式化，如果不可用则使用默认格式
        try {
            com.guild.GuildPlugin plugin = com.guild.GuildPlugin.getInstance();
            if (plugin != null && plugin.getEconomyManager() != null && plugin.getEconomyManager().isVaultAvailable()) {
                return plugin.getEconomyManager().format(balance);
            }
        } catch (Exception e) {
            // 忽略错误，使用默认格式
        }
        return String.format("%.2f", balance);
    }
    
    /**
     * 获取下一级升级需求
     */
    private static String getNextLevelRequirement(int currentLevel) {
        if (currentLevel >= 10) {
            return "已达到最高等级";
        }
        
        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
        }
        
        return String.format("%.2f", required);
    }
    
    /**
     * 获取等级进度
     */
    private static String getLevelProgress(int currentLevel, double currentBalance) {
        if (currentLevel >= 10) {
            return "100%";
        }
        
        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
        }
        
        double progress = (currentBalance / required) * 100;
        return String.format("%.1f%%", Math.min(progress, 100));
    }
    
    /**
     * 获取升级费用
     */
    private static String getUpgradeCost(int currentLevel) {
        if (currentLevel >= 10) {
            return "0";
        }
        
        double cost = 0;
        switch (currentLevel) {
            case 1: cost = 5000; break;
            case 2: cost = 10000; break;
            case 3: cost = 20000; break;
            case 4: cost = 35000; break;
            case 5: cost = 50000; break;
            case 6: cost = 75000; break;
            case 7: cost = 100000; break;
            case 8: cost = 150000; break;
            case 9: cost = 200000; break;
        }
        
        return String.format("%.2f", cost);
    }
}
