package com.guild.core.utils;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 变量测试工具类 - 用于验证GUI变量替换
 */
public class VariableTestUtils {
    
    /**
     * 测试GUI变量替换
     * @param plugin 插件实例
     * @param guild 工会对象
     * @param player 玩家对象
     */
    public static void testGUIVariables(GuildPlugin plugin, Guild guild, Player player) {
        player.sendMessage("§6=== GUI变量测试 ===");
        
        // 测试基础变量
        String[] testTexts = {
            "工会名称: {guild_name}",
            "工会标签: {guild_tag}",
            "工会描述: {guild_description}",
            "工会ID: {guild_id}",
            "会长: {leader_name}",
            "工会等级: {guild_level}",
            "工会资金: {guild_balance_formatted}",
            "最大成员: {guild_max_members}",
            "工会状态: {guild_frozen}",
            "创建时间: {guild_created_date}",
            "成员数量: {member_count}/{guild_max_members}",
            "升级需求: {guild_next_level_requirement}",
            "升级进度: {guild_level_progress}"
        };
        
        for (String testText : testTexts) {
            String processed = GUIUtils.processGUIVariables(testText, guild, player);
            player.sendMessage("§e原始: §f" + testText);
            player.sendMessage("§a处理后: §f" + processed);
            
            // 检查是否有未解析的变量
            if (GUIUtils.hasUnresolvedVariables(processed)) {
                List<String> unresolved = GUIUtils.getUnresolvedVariables(processed);
                player.sendMessage("§c未解析变量: §f" + unresolved);
            }
            player.sendMessage("");
        }
        
        // 测试异步变量
        plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(memberCount -> {
            String asyncTest = "成员数量: {member_count}/{guild_max_members}";
            GUIUtils.processGUIVariablesAsync(asyncTest, guild, player, plugin).thenAccept(processed -> {
                player.sendMessage("§6异步测试: §f" + asyncTest);
                player.sendMessage("§a异步结果: §f" + processed);
            });
        });
    }
    
    /**
     * 测试颜色代码
     * @param player 玩家对象
     */
    public static void testColorCodes(Player player) {
        player.sendMessage("§6=== 颜色代码测试 ===");
        
        String[] colorTests = {
            "&a绿色文本",
            "&c红色文本",
            "&e黄色文本",
            "&b青色文本",
            "&d粉色文本",
            "&f白色文本",
            "&7灰色文本",
            "&8深灰色文本",
            "&9蓝色文本",
            "&0黑色文本",
            "&l粗体文本",
            "&n下划线文本",
            "&o斜体文本",
            "&k随机字符",
            "&r重置格式"
        };
        
        for (String test : colorTests) {
            String processed = ColorUtils.colorize(test);
            player.sendMessage("§e原始: §f" + test);
            player.sendMessage("§a处理后: §f" + processed);
            player.sendMessage("");
        }
    }
    
    /**
     * 测试PlaceholderUtils
     * @param guild 工会对象
     * @param player 玩家对象
     */
    public static void testPlaceholderUtils(Guild guild, Player player) {
        player.sendMessage("§6=== PlaceholderUtils测试 ===");
        
        String testText = "工会: {guild_name}, 会长: {leader_name}, 等级: {guild_level}, 资金: {guild_balance_formatted}";
        String processed = PlaceholderUtils.replaceGuildPlaceholders(testText, guild, player);
        
        player.sendMessage("§e原始: §f" + testText);
        player.sendMessage("§a处理后: §f" + processed);
        
        // 检查是否有未解析的变量
        if (GUIUtils.hasUnresolvedVariables(processed)) {
            List<String> unresolved = GUIUtils.getUnresolvedVariables(processed);
            player.sendMessage("§c未解析变量: §f" + unresolved);
        }
    }
}
