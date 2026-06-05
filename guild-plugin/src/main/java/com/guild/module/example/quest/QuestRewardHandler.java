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
     * Grant quest rewards (main entry point)
     */
    public void grantRewards(Player player, QuestDefinition definition, QuestProgress progress) {
        if (player == null || !player.isOnline()) {
            logger.warning("[Quest-Reward] Player offline, cannot grant rewards");
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

        // Mark as claimed (synchronous operation)
        synchronized (progress) {
            progress.setClaimed();
        }
        
        // Log detailed result
        logRewardResult(player.getName(), definition.getName(), successRewards, failedRewards);
        
        // Notify player
        notifyPlayer(player, definition.getName(), successRewards, failedRewards);
    }

    /**
     * Grant C-Coins reward
     */
    private boolean grantContributionReward(Player player, double amount, 
                                           String questName,
                                           List<String> successList, 
                                           List<String> failedList) {
        try {
            var guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
            if (guild == null) {
                String msg = "C-Coins+" + (int)amount + " (failed: not in guild)";
                failedList.add(msg);
                logger.warning("[Quest-Reward] " + player.getName() + 
                    " cannot receive C-Coins: not in guild (quest: " + questName + ")");
                return false;
            }

            // Use currency API to grant C-Coins
            boolean success = context.getApi().depositCurrency(
                guild.getId(),
                player.getUniqueId(),
                player.getName(),
                CurrencyManager.CurrencyType.C_COIN,
                amount
            );

            if (success) {
                logger.info("[Quest-Reward] " + player.getName() + 
                    " C-Coins+" + (int)amount + " granted (quest: " + questName + ")");
                String msg = "C-Coins+" + (int)amount;
                successList.add(msg);
                
                // Send currency change message
                context.getApi().getCurrencyManager().sendCurrencyMessage(
                    player, 
                    CurrencyManager.CurrencyType.C_COIN, 
                    amount, 
                    true
                );
                
                return true;
            } else {
                String msg = "C-Coins+" + (int)amount + " (grant failed)";
                failedList.add(msg);
                logger.warning("[Quest-Reward] " + player.getName() + 
                    " C-Coins grant failed (quest: " + questName + ")");
                return false;
            }

        } catch (Exception e) {
            String msg = "C-Coins+" + (int)amount + " (error: " + e.getMessage() + ")";
            failedList.add(msg);
            logger.severe("[Quest-Reward] C-Coins grant exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Grant money reward
     */
    private boolean grantMoneyReward(Player player, double amount, 
                                    String questName,
                                    List<String> successList, 
                                    List<String> failedList) {
        try {
            // Check if Vault economy is available
            var econReg = Bukkit.getServer().getServicesManager()
                .getRegistration(net.milkbowl.vault.economy.Economy.class);
            
            if (econReg == null) {
                String msg = "$" + (int)amount + " (failed: economy not installed)";
                failedList.add(msg);
                logger.warning("[Quest-Reward] Economy unavailable, cannot grant money (quest: " + questName + ")");
                return false;
            }

            net.milkbowl.vault.economy.Economy econ = econReg.getProvider();
            
            // Check if player has permission to receive money
            if (!player.hasPermission("guild.economy.deposit")) {
                String msg = "$" + (int)amount + " (failed: insufficient permission)";
                failedList.add(msg);
                logger.fine("[Quest-Reward] " + player.getName() + 
                    " lacks permission to receive money (quest: " + questName + ")");
                return false;
            }

            // Grant money
            boolean success = econ.depositPlayer(player, amount).transactionSuccess();
            
            if (success) {
                String msg = "$" + String.format("%.0f", amount);
                successList.add(msg);
                logger.info("[Quest-Reward] " + player.getName() + 
                    " received $" + String.format("%.0f", amount) + " (quest: " + questName + ")");
                return true;
            } else {
                String msg = "$" + String.format("%.0f", amount) + " (transaction failed)";
                failedList.add(msg);
                logger.warning("[Quest-Reward] " + player.getName() + 
                    " money transaction failed (quest: " + questName + ")");
                return false;
            }

        } catch (Exception e) {
            String msg = "$" + String.format("%.0f", amount) + " (error: " + e.getMessage() + ")";
            failedList.add(msg);
            logger.severe("[Quest-Reward] Money grant exception: " + e.getMessage() + 
                " (quest: " + questName + ", player: " + player.getName() + ")");
            return false;
        }
    }

    /**
     * Grant experience reward
     */
    private boolean grantExpReward(Player player, double amount, 
                                  String questName,
                                  List<String> successList, 
                                  List<String> failedList) {
        try {
            int expAmount = (int) amount;
            
            // Use Bukkit native API to grant experience
            player.giveExp(expAmount);
            
            String msg = "EXP+" + expAmount;
            successList.add(msg);
            
            logger.fine("[Quest-Reward] " + player.getName() + 
                " EXP+" + expAmount + " (quest: " + questName + ")");
            return true;

        } catch (Exception e) {
            String msg = "EXP+" + (int)amount + " (error: " + e.getMessage() + ")";
            failedList.add(msg);
            logger.severe("[Quest-Reward] EXP grant exception: " + e.getMessage() + 
                " (quest: " + questName + ", player: " + player.getName() + ")");
            return false;
        }
    }

    /**
     * Log reward result details
     */
    private void logRewardResult(String playerName, String questName, 
                                List<String> successList, List<String> failedList) {
        StringBuilder logMsg = new StringBuilder();
        logMsg.append(String.format("[Quest-Reward] %s claimed '%s' rewards: ", 
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
     * Notify player of reward result
     */
    private void notifyPlayer(Player player, String questName, 
                            List<String> successList, List<String> failedList) {
        if (!player.isOnline()) return;

        // Build message
        StringBuilder message = new StringBuilder();
        message.append("&6&l[Quest Rewards]&r &aYou have claimed rewards for '&e").append(questName)
              .append("&e'!\n");

        if (!successList.isEmpty()) {
            message.append("&aReceived: &f").append(String.join("&7, &f", successList)).append("\n");
        }

        if (!failedList.isEmpty()) {
            message.append("&cSome rewards failed: &7")
                  .append(String.join("&7, &c", failedList))
                  .append("\n&7Please contact an administrator");
        }

        // Send message (with color codes)
        context.sendMessage(player, "quest.reward-result", message.toString());

        // Extra notice if some rewards failed
        if (!failedList.isEmpty()) {
            context.sendMessage(player, "quest.reward-partial", 
                "&e[Quest] Some rewards could not be delivered. Please screenshot and contact an administrator");
        }
    }

    // ==================== Factory Methods ====================

    public static QuestReward createDefaultContribution(double amount) {
        return new QuestReward(QuestReward.RewardType.CONTRIBUTION, amount);
    }

    public static QuestReward createDefaultMoney(double amount) {
        return new QuestReward(QuestReward.RewardType.MONEY, amount);
    }

    public static QuestReward createDefaultExp(double amount) {
        return new QuestReward(QuestReward.RewardType.EXP, amount);
    }

    // ==================== Validation & Diagnostics ====================

    /**
     * Check if Vault economy is available
     */
    public boolean isEconomyAvailable() {
        return Bukkit.getServer().getServicesManager()
            .getRegistration(net.milkbowl.vault.economy.Economy.class) != null;
    }

    /**
     * Check if player can receive a specific reward type
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
                return true; // EXP can always be received
                
            default:
                return false;
        }
    }

    /**
     * Validate all rewards for a player (pre-check)
     */
    public List<String> validateRewards(Player player, QuestDefinition definition) {
        List<String> issues = new ArrayList<>();
        
        for (QuestReward reward : definition.getRewards()) {
            switch (reward.getType()) {
                case CONTRIBUTION:
                    if (context.getPlugin().getGuildService()
                        .getPlayerGuild(player.getUniqueId()) == null) {
                        issues.add("C-Coins reward: you are not in a guild");
                    }
                    break;
                    
                case MONEY:
                    if (!isEconomyAvailable()) {
                        issues.add("Money reward: economy plugin not installed on this server");
                    } else if (!player.hasPermission("guild.economy.deposit")) {
                        issues.add("Money reward: insufficient permission (need guild.economy.deposit)");
                    }
                    break;
                    
                case EXP:
                    // EXP has no special requirements
                    break;
            }
        }
        
        return issues;
    }
}
