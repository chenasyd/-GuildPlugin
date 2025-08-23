package com.guild.core.placeholder;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import com.guild.core.time.TimeProvider;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Guild插件 PlaceholderAPI 扩展
 * 提供完整的工会数据变量支持
 */
public class GuildPlaceholderExpansion extends PlaceholderExpansion {
    
    private final GuildPlugin plugin;
    private final GuildService guildService;
    
    public GuildPlaceholderExpansion(GuildPlugin plugin, GuildService guildService) {
        this.plugin = plugin;
        this.guildService = guildService;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "guild";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "GuildTeam";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        String[] args = params.split("_");
        if (args.length == 0) {
            return "";
        }
        
        try {
            switch (args[0].toLowerCase()) {
                // 基础工会信息
                case "name":
                    return getGuildName(player);
                case "tag":
                    return getGuildTag(player);
                case "description":
                    return getGuildDescription(player);
                case "leader":
                    return getGuildLeader(player);
                case "membercount":
                    return getGuildMemberCount(player);
                case "maxmembers":
                    return getGuildMaxMembers(player);
                case "level":
                    return getGuildLevel(player);
                case "balance":
                    return getGuildBalance(player);
                case "frozen":
                    return getGuildFrozenStatus(player);
                
                // 玩家在工会中的信息
                case "role":
                    return getPlayerRole(player);
                case "joined":
                    return getPlayerJoinedTime(player);
                case "contribution":
                    return getPlayerContribution(player);
                
                // 工会状态检查
                case "hasguild":
                    return hasGuild(player);
                case "isleader":
                    return isLeader(player);
                case "isofficer":
                    return isOfficer(player);
                case "ismember":
                    return isMember(player);
                
                // 工会权限
                case "caninvite":
                    return canInvite(player);
                case "cankick":
                    return canKick(player);
                case "canpromote":
                    return canPromote(player);
                case "candemote":
                    return canDemote(player);
                case "cansethome":
                    return canSetHome(player);
                case "canmanageeconomy":
                    return canManageEconomy(player);
                
                default:
                    return "";
            }
        } catch (Exception e) {
            plugin.getLogger().warning("处理占位符时发生错误: " + e.getMessage());
            return "";
        }
    }
    
    // ==================== 基础工会信息 ====================
    
    private String getGuildName(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getName() : "无工会";
        } catch (Exception e) {
            return "无工会";
        }
    }
    
    private String getGuildTag(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getTag() : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getGuildDescription(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getDescription() : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getGuildLeader(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getLeaderName() : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getGuildMemberCount(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            if (guild == null) return "0";
            
            CompletableFuture<Integer> future = guildService.getGuildMemberCountAsync(guild.getId());
            return String.valueOf(future.get());
        } catch (Exception e) {
            return "0";
        }
    }
    
    private String getGuildMaxMembers(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? String.valueOf(guild.getMaxMembers()) : "0";
        } catch (Exception e) {
            return "0";
        }
    }
    
    private String getGuildLevel(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? String.valueOf(guild.getLevel()) : "0";
        } catch (Exception e) {
            return "0";
        }
    }
    
    private String getGuildBalance(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? String.format("%.2f", guild.getBalance()) : "0.00";
        } catch (Exception e) {
            return "0.00";
        }
    }
    
    private String getGuildFrozenStatus(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? (guild.isFrozen() ? "已冻结" : "正常") : "无工会";
        } catch (Exception e) {
            return "无工会";
        }
    }
    
    // ==================== 玩家在工会中的信息 ====================
    
    private String getPlayerRole(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null ? member.getRole().getDisplayName() : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getPlayerJoinedTime(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null || member.getJoinedAt() == null) return "";
            return member.getJoinedAt().format(TimeProvider.FULL_FORMATTER);
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getPlayerContribution(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            // 暂时返回0，因为GuildMember类还没有contribution字段
            return member != null ? "0" : "0";
        } catch (Exception e) {
            return "0";
        }
    }
    
    // ==================== 工会状态检查 ====================
    
    private String hasGuild(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? "是" : "否";
        } catch (Exception e) {
            return "否";
        }
    }
    
    private String isLeader(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null && member.getRole() == GuildMember.Role.LEADER ? "是" : "否";
        } catch (Exception e) {
            return "否";
        }
    }
    
    private String isOfficer(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null && member.getRole() == GuildMember.Role.OFFICER ? "是" : "否";
        } catch (Exception e) {
            return "否";
        }
    }
    
    private String isMember(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null ? "是" : "否";
        } catch (Exception e) {
            return "否";
        }
    }
    
    // ==================== 工会权限 ====================
    
    private String canInvite(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "否";
            
            return (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER) ? "是" : "否";
        } catch (Exception e) {
            return "否";
        }
    }
    
    private String canKick(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "否";
            
            return (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER) ? "是" : "否";
        } catch (Exception e) {
            return "否";
        }
    }
    
    private String canPromote(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "否";
            
            return member.getRole() == GuildMember.Role.LEADER ? "是" : "否";
        } catch (Exception e) {
            return "否";
        }
    }
    
    private String canDemote(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "否";
            
            return member.getRole() == GuildMember.Role.LEADER ? "是" : "否";
        } catch (Exception e) {
            return "否";
        }
    }
    
    private String canSetHome(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "否";
            
            return (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER) ? "是" : "否";
        } catch (Exception e) {
            return "否";
        }
    }
    
    private String canManageEconomy(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "否";
            
            return (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER) ? "是" : "否";
        } catch (Exception e) {
            return "否";
        }
    }
}
