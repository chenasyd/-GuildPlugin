package com.guild.module.example.quest;

import com.guild.core.module.ModuleContext;
import com.guild.models.GuildContribution;
import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestProgress;
import com.guild.module.example.quest.model.QuestReward;
import com.guild.sdk.economy.CurrencyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class QuestRewardHandler {
    private final ModuleContext context;
    private final Logger logger;

    public QuestRewardHandler(ModuleContext context) {
        this.context = context;
        this.logger = context.getLogger();
    }

    /**
     * 发放任务奖励（主入口）
     * 
     * @param player 目标玩家
     * @param definition 任务定义
     * @param progress 任务进度
     */
    public void grantRewards(Player player, QuestDefinition definition, QuestProgress progress) {
        if (player == null || !player.isOnline()) {
            logger.warning("[Quest-Reward] 玩家不在线，无法发放奖励");
            return;
        }

        List<String> successRewards = new ArrayList<>();
        List<String> failedRewards = new ArrayList<>();

        for (QuestReward reward : definition.getRewards()) {
            boolean success = false;
            
            switch (reward.getType()) {
                case CONTRIBUTION:
                    success = grantContributionReward(player, reward.getAmount(), 
                        definition.getName(), successRewards, failedRewards);
                    break;
                    
                case MONEY:
                    success = grantMoneyReward(player, reward.getAmount(), 
                        definition.getName(), successRewards, failedRewards);
                    break;
                    
                case EXP:
                    success = grantExpReward(player, reward.getAmount(), 
                        definition.getName(), successRewards, failedRewards);
                    break;
            }
        }

        // 标记为已领取（同步操作）
        synchronized (progress) {
            progress.setClaimed();
        }
        
        // 记录详细日志
        logRewardResult(player.getName(), definition.getName(), successRewards, failedRewards);
        
        // 通知玩家
        notifyPlayer(player, definition.getName(), successRewards, failedRewards);
    }

    /**
     * 发放C币奖励
     * 
     * @return 是否成功
     */
    private boolean grantContributionReward(Player player, double amount, 
                                           String questName,
                                           List<String> successList, 
                                           List<String> failedList) {
        try {
            var guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
            if (guild == null) {
                String msg = "C币+" + (int)amount + " (失败: 不在公会中)";
                failedList.add(msg);
                logger.warning("[Quest-Reward] " + player.getName() + 
                    " 无法领取C币: 不在公会中 (任务: " + questName + ")");
                return false;
            }

            // 使用货币API发放C币
            boolean success = context.getApi().depositCurrency(
                guild.getId(),
                player.getUniqueId(),
                player.getName(),
                CurrencyManager.CurrencyType.C_COIN,
                amount
            );

            if (success) {
                logger.info("[Quest-Reward] " + player.getName() + 
                    " C币+" + (int)amount + " 已发放 (任务: " + questName + ")");
                String msg = "C币+" + (int)amount;
                successList.add(msg);
                
                // 发送货币变动消息
                context.getApi().getCurrencyManager().sendCurrencyMessage(
                    player, 
                    CurrencyManager.CurrencyType.C_COIN, 
                    amount, 
                    true
                );
                
                return true;
            } else {
                String msg = "C币+" + (int)amount + " (发放失败)";
                failedList.add(msg);
                logger.warning("[Quest-Reward] " + player.getName() + 
                    " C币发放失败 (任务: " + questName + ")");
                return false;
            }

        } catch (Exception e) {
            String msg = "C币+" + (int)amount + " (异常: " + e.getMessage() + ")";
            failedList.add(msg);
            logger.severe("[Quest-Reward] 发放C币异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 发放金币奖励
     * 
     * @return 是否成功
     */
    private boolean grantMoneyReward(Player player, double amount, 
                                    String questName,
                                    List<String> successList, 
                                    List<String> failedList) {
        try {
            // 检查Vault经济系统是否可用
            var econReg = Bukkit.getServer().getServicesManager()
                .getRegistration(net.milkbowl.vault.economy.Economy.class);
            
            if (econReg == null) {
                String msg = "$" + (int)amount + " (失败: 经济系统未安装)";
                failedList.add(msg);
                logger.warning("[Quest-Reward] 经济系统不可用，无法发放金币 (任务: " + questName + ")");
                return false;
            }

            net.milkbowl.vault.economy.Economy econ = econReg.getProvider();
            
            // 检查玩家是否有接收金币的权限（可选）
            if (!player.hasPermission("guild.economy.deposit")) {
                String msg = "$" + (int)amount + " (失败: 权限不足)";
                failedList.add(msg);
                logger.fine("[Quest-Reward] " + player.getName() + 
                    " 无权限接收金币 (任务: " + questName + ")");
                return false;
            }

            // 发放金币
            boolean success = econ.depositPlayer(player, amount).transactionSuccess();
            
            if (success) {
                String msg = "$" + String.format("%.0f", amount);
                successList.add(msg);
                logger.info("[Quest-Reward] " + player.getName() + 
                    " 金币+$" + String.format("%.0f", amount) + " (任务: " + questName + ")");
                return true;
            } else {
                String msg = "$" + String.format("%.0f", amount) + " (交易失败)";
                failedList.add(msg);
                logger.warning("[Quest-Reward] " + player.getName() + 
                    " 金币发放交易失败 (任务: " + questName + ")");
                return false;
            }

        } catch (Exception e) {
            String msg = "$" + String.format("%.0f", amount) + " (异常: " + e.getMessage() + ")";
            failedList.add(msg);
            logger.severe("[Quest-Reward] 发放金币异常: " + e.getMessage() + 
                " (任务: " + questName + ", 玩家: " + player.getName() + ")");
            return false;
        }
    }

    /**
     * 发放经验值奖励
     * 
     * @return 是否成功
     */
    private boolean grantExpReward(Player player, double amount, 
                                  String questName,
                                  List<String> successList, 
                                  List<String> failedList) {
        try {
            int expAmount = (int) amount;
            
            // 使用Bukkit原生API给予经验
            player.giveExp(expAmount);
            
            String msg = "经验+" + expAmount;
            successList.add(msg);
            
            logger.fine("[Quest-Reward] " + player.getName() + 
                " 经验+" + expAmount + " (任务: " + questName + ")");
            return true;

        } catch (Exception e) {
            String msg = "经验+" + (int)amount + " (异常: " + e.getMessage() + ")";
            failedList.add(msg);
            logger.severe("[Quest-Reward] 发放经验异常: " + e.getMessage() + 
                " (任务: " + questName + ", 玩家: " + player.getName() + ")");
            return false;
        }
    }

    /**
     * 记录奖励发放结果的详细日志
     */
    private void logRewardResult(String playerName, String questName, 
                                List<String> successList, List<String> failedList) {
        StringBuilder logMsg = new StringBuilder();
        logMsg.append(String.format("[Quest-Reward] %s 领取任务 '%s' 奖励: ", 
            playerName, questName));
        
        if (!successList.isEmpty()) {
            logMsg.append("✓ ").append(String.join(", ", successList));
        }
        
        if (!failedList.isEmpty()) {
            if (!successList.isEmpty()) logMsg.append(" | ");
            logMsg.append("✗ ").append(String.join(", ", failedList));
        }
        
        if (failedList.isEmpty()) {
            logger.info(logMsg.toString());
        } else {
            logger.warning(logMsg.toString());
        }
    }

    /**
     * 通知玩家奖励领取结果
     */
    private void notifyPlayer(Player player, String questName, 
                            List<String> successList, List<String> failedList) {
        if (!player.isOnline()) return;

        // 构建消息
        StringBuilder message = new StringBuilder();
        message.append("&6&l[任务奖励]&r &a你已领取任务 '&e").append(questName)
              .append("&e' 的奖励！\n");

        if (!successList.isEmpty()) {
            message.append("&a获得: &f").append(String.join("&7, &f", successList)).append("\n");
        }

        if (!failedList.isEmpty()) {
            message.append("&c部分奖励发放失败: &7")
                  .append(String.join("&7, &c", failedList))
                  .append("\n&7请联系管理员处理");
        }

        // 发送消息（使用颜色代码）
        context.sendMessage(player, "quest.reward-result", message.toString());

        // 如果有失败的奖励，额外提示
        if (!failedList.isEmpty()) {
            context.sendMessage(player, "quest.reward-partial", 
                "&e[Quest] 部分奖励未能正常发放，请截图并联系管理员");
        }
    }

    // ==================== 工厂方法 ====================

    public static QuestReward createDefaultContribution(double amount) {
        return new QuestReward(QuestReward.RewardType.CONTRIBUTION, amount);
    }

    public static QuestReward createDefaultMoney(double amount) {
        return new QuestReward(QuestReward.RewardType.MONEY, amount);
    }

    public static QuestReward createDefaultExp(double amount) {
        return new QuestReward(QuestReward.RewardType.EXP, amount);
    }

    // ==================== 验证和诊断方法 ====================

    /**
     * 检查Vault经济系统是否可用
     */
    public boolean isEconomyAvailable() {
        return Bukkit.getServer().getServicesManager()
            .getRegistration(net.milkbowl.vault.economy.Economy.class) != null;
    }

    /**
     * 检查玩家是否可以接收指定类型的奖励
     */
    public boolean canReceiveReward(Player player, QuestReward.RewardType type) {
        switch (type) {
            case CONTRIBUTION:
                return context.getPlugin().getGuildService()
                    .getPlayerGuild(player.getUniqueId()) != null;
                    
            case MONEY:
                return isEconomyAvailable() && 
                    player.hasPermission("guild.economy.deposit");
                    
            case EXP:
                return true; // 经验值总是可以接收
                
            default:
                return false;
        }
    }

    /**
     * 验证所有奖励是否可发放（用于预检查）
     */
    public List<String> validateRewards(Player player, QuestDefinition definition) {
        List<String> issues = new ArrayList<>();
        
        for (QuestReward reward : definition.getRewards()) {
            switch (reward.getType()) {
                case CONTRIBUTION:
                    if (context.getPlugin().getGuildService()
                        .getPlayerGuild(player.getUniqueId()) == null) {
                        issues.add("C币奖励: 你不在公会中");
                    }
                    break;
                    
                case MONEY:
                    if (!isEconomyAvailable()) {
                        issues.add("金币奖励: 服务器未安装经济系统");
                    } else if (!player.hasPermission("guild.economy.deposit")) {
                        issues.add("金币奖励: 权限不足 (需要 guild.economy.deposit)");
                    }
                    break;
                    
                case EXP:
                    // 经验值无需特殊条件
                    break;
            }
        }
        
        return issues;
    }
}
