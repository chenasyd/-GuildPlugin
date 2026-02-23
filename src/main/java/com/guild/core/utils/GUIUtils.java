package com.guild.core.utils;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI工具类 - 统一处理GUI中的变量替换和颜色代码
 */
public class GUIUtils {
    
    /**
     * 处理GUI配置中的变量替换
     * @param text 原始文本
     * @param guild 工会对象
     * @param player 玩家对象
     * @return 替换后的文本
     */
    public static String processGUIVariables(String text, Guild guild, Player player) {
        if (text == null) {
            return "";
        }
        
        // 使用PlaceholderUtils处理基础变量
        String result = PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);
        
        // 确保颜色代码正确应用
        return ColorUtils.colorize(result);
    }
    
    /**
     * 异步处理GUI配置中的变量替换（包含动态数据）
     * @param text 原始文本
     * @param guild 工会对象
     * @param player 玩家对象
     * @param plugin 插件实例
     * @return 替换后的文本的CompletableFuture
     */
    public static CompletableFuture<String> processGUIVariablesAsync(String text, Guild guild, Player player, GuildPlugin plugin) {
        if (text == null) {
            return CompletableFuture.completedFuture("");
        }
        
        // 先处理静态变量
        String result = processGUIVariables(text, guild, player);
        
        // 异步获取动态数据
        return plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenApply(memberCount -> {
            return result
                .replace("{member_count}", String.valueOf(memberCount))
                .replace("{online_member_count}", String.valueOf(memberCount)); // 暂时使用总成员数
        });
    }
    
    /**
     * 处理GUI配置中的物品描述列表
     * @param loreList 原始描述列表
     * @param guild 工会对象
     * @param player 玩家对象
     * @return 处理后的描述列表
     */
    public static List<String> processGUILore(List<String> loreList, Guild guild, Player player) {
        List<String> processedLore = new ArrayList<>();
        
        if (loreList != null) {
            for (String line : loreList) {
                processedLore.add(processGUIVariables(line, guild, player));
            }
        }
        
        return processedLore;
    }
    
    /**
     * 异步处理GUI配置中的物品描述列表（包含动态数据）
     * @param loreList 原始描述列表
     * @param guild 工会对象
     * @param player 玩家对象
     * @param plugin 插件实例
     * @return 处理后的描述列表的CompletableFuture
     */
    public static CompletableFuture<List<String>> processGUILoreAsync(List<String> loreList, Guild guild, Player player, GuildPlugin plugin) {
        if (loreList == null || loreList.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        for (String line : loreList) {
            futures.add(processGUIVariablesAsync(line, guild, player, plugin));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<String> processedLore = new ArrayList<>();
                for (CompletableFuture<String> future : futures) {
                    try {
                        processedLore.add(future.get());
                    } catch (Exception e) {
                        plugin.getLogger().warning("处理GUI描述时发生错误: " + e.getMessage());
                        processedLore.add("&c错误");
                    }
                }
                return processedLore;
            });
    }
    
    /**
     * 处理成员相关的GUI变量
     * @param text 原始文本
     * @param member 成员对象
     * @param guild 工会对象
     * @param player 玩家对象（可为null）
     * @return 替换后的文本
     */
    public static String processMemberGUIVariables(String text, GuildMember member, Guild guild, Player player) {
        if (text == null) {
            return "";
        }

        return PlaceholderUtils.replaceMemberPlaceholders(text, member, guild, player);
    }
    
    /**
     * 验证变量是否被正确替换
     * @param text 要检查的文本
     * @return 是否包含未替换的变量
     */
    public static boolean hasUnresolvedVariables(String text) {
        if (text == null) {
            return false;
        }
        
        // 检查是否包含未替换的变量占位符
        return text.contains("{") && text.contains("}");
    }
    
    /**
     * 获取未替换的变量列表
     * @param text 要检查的文本
     * @return 未替换的变量列表
     */
    public static List<String> getUnresolvedVariables(String text) {
        List<String> unresolved = new ArrayList<>();
        
        if (text == null) {
            return unresolved;
        }
        
        // 简单的变量检测（可以扩展为更复杂的正则表达式）
        String[] parts = text.split("\\{");
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int endIndex = part.indexOf("}");
            if (endIndex > 0) {
                String variable = part.substring(0, endIndex);
                unresolved.add("{" + variable + "}");
            }
        }
        
        return unresolved;
    }
}
