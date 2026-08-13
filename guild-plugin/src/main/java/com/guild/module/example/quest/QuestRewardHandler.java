package com.guild.module.example.quest;

import com.guild.core.module.ModuleContext;
import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestProgress;
import com.guild.module.example.quest.model.QuestReward;
import com.guild.module.example.quest.tree.GuildTreeService;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class QuestRewardHandler {
    private final ModuleContext context;
    private final Logger logger;
    private GuildTreeService treeService;

    public QuestRewardHandler(ModuleContext context) {
        this.context = context;
        this.logger = context.getLogger();
    }

    public void setTreeService(GuildTreeService treeService) {
        this.treeService = treeService;
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
            switch (reward.getType()) {
                case CONTRIBUTION:
                case EXP:
                    grantTreeExpReward(player, progress.getGuildId(), reward.getAmount(),
                        definition.getId(), successRewards, failedRewards);
                    break;

                case MONEY:
                    grantMoneyReward(player, reward.getAmount(),
                        definition.getId(), successRewards, failedRewards);
                    break;
            }
        }

        // Mark as claimed (synchronous operation)
        synchronized (progress) {
            progress.setClaimed();
        }

        QuestTexts tx = new QuestTexts(context);
        String localizedName = tx.questName(definition);

        logRewardResult(player.getName(), definition.getId(), successRewards, failedRewards);
        notifyPlayer(player, localizedName, successRewards, failedRewards, tx);
    }

    private boolean grantTreeExpReward(Player player, int guildId, double amount,
                                       String questName,
                                       List<String> successList,
                                       List<String> failedList) {
        try {
            int resolvedGuildId = guildId;
            if (resolvedGuildId <= 0) {
                var guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
                if (guild != null) {
                    resolvedGuildId = guild.getId();
                }
            }
            if (resolvedGuildId <= 0) {
                String msg = "TreeEXP+" + (int) amount + " (failed: not in guild)";
                failedList.add(msg);
                return false;
            }
            if (treeService == null) {
                failedList.add("TreeEXP+" + (int) amount + " (tree service unavailable)");
                return false;
            }

            long expAmount = Math.max(0L, Math.round(amount));
            boolean success = treeService.deposit(resolvedGuildId, player, expAmount, "quest:" + questName);
            if (success) {
                successList.add("TreeEXP+" + expAmount);
                logger.info("[Quest-Reward] " + player.getName()
                    + " TreeEXP+" + expAmount + " (quest: " + questName + ", guild=" + resolvedGuildId + ")");
                return true;
            }
            failedList.add("TreeEXP+" + expAmount + " (grant failed)");
            return false;
        } catch (Exception e) {
            failedList.add("TreeEXP+" + (int) amount + " (error: " + e.getMessage() + ")");
            logger.severe("[Quest-Reward] Tree EXP grant exception: " + e.getMessage());
            return false;
        }
    }

    private boolean grantMoneyReward(Player player, double amount,
                                    String questName,
                                    List<String> successList,
                                    List<String> failedList) {
        try {
            var economyManager = context.getPlugin().getEconomyManager();
            if (economyManager == null || !economyManager.isVaultAvailable()) {
                String msg = "$" + (int) amount + " (failed: economy not installed)";
                failedList.add(msg);
                logger.warning("[Quest-Reward] Economy unavailable, cannot grant money (quest: " + questName + ")");
                return false;
            }

            if (!player.hasPermission("guild.economy.deposit")) {
                String msg = "$" + (int) amount + " (failed: insufficient permission)";
                failedList.add(msg);
                return false;
            }

            boolean success = economyManager.deposit(player, amount);

            if (success) {
                String msg = "$" + String.format("%.0f", amount);
                successList.add(msg);
                logger.info("[Quest-Reward] " + player.getName()
                    + " received $" + String.format("%.0f", amount) + " (quest: " + questName + ")");
                return true;
            }
            failedList.add("$" + String.format("%.0f", amount) + " (transaction failed)");
            return false;

        } catch (NoClassDefFoundError | Exception e) {
            failedList.add("$" + String.format("%.0f", amount) + " (error: economy unavailable)");
            logger.warning("[Quest-Reward] Money grant skipped (economy unavailable): " + e.getMessage());
            return false;
        }
    }

    private void logRewardResult(String playerName, String questName,
                                List<String> successList, List<String> failedList) {
        StringBuilder logMsg = new StringBuilder();
        logMsg.append(String.format("[Quest-Reward] %s claimed '%s' rewards: ",
            playerName, questName));

        if (!successList.isEmpty()) {
            logMsg.append("OK ").append(String.join(", ", successList));
        }

        if (!failedList.isEmpty()) {
            if (!successList.isEmpty()) logMsg.append(" | ");
            logMsg.append("FAIL ").append(String.join(", ", failedList));
        }

        if (failedList.isEmpty()) {
            logger.info(logMsg.toString());
        } else {
            logger.warning(logMsg.toString());
        }
    }

    private void notifyPlayer(Player player, String questName,
                            List<String> successList, List<String> failedList,
                            QuestTexts tx) {
        if (!player.isOnline()) return;

        StringBuilder message = new StringBuilder();
        message.append(tx.tf("module.quest.reward-notify.header",
            "&6&l[Quest Rewards]&r &aYou have claimed rewards for '&e{0}&e'!\n",
            questName));

        if (!successList.isEmpty()) {
            message.append(tx.tf("module.quest.reward-notify.received",
                "&aReceived: &f{0}\n",
                String.join("&7, &f", successList)));
        }

        if (!failedList.isEmpty()) {
            message.append(tx.tf("module.quest.reward-notify.failed",
                "&cSome rewards failed: &7{0}\n&7Please contact an administrator",
                String.join("&7, &c", failedList)));
        }

        context.sendMessage(player, "quest.reward-result", message.toString());

        if (!failedList.isEmpty()) {
            context.sendMessage(player, "module.quest.reward-partial",
                "&e[Quest] Some rewards could not be delivered. Please screenshot and contact an administrator");
        }
    }

    public static QuestReward createDefaultMoney(double amount) {
        return new QuestReward(QuestReward.RewardType.MONEY, amount);
    }

    public static QuestReward createDefaultExp(double amount) {
        return new QuestReward(QuestReward.RewardType.EXP, amount);
    }

    public boolean isEconomyAvailable() {
        try {
            var economyManager = context.getPlugin().getEconomyManager();
            return economyManager != null && economyManager.isVaultAvailable();
        } catch (NoClassDefFoundError | Exception e) {
            return false;
        }
    }

    public boolean canReceiveReward(Player player, QuestReward.RewardType type) {
        switch (type) {
            case CONTRIBUTION:
            case EXP:
                return true; // deposited using quest progress guildId
            case MONEY:
                return isEconomyAvailable()
                    && player.hasPermission("guild.economy.deposit");
            default:
                return false;
        }
    }

    public List<String> validateRewards(Player player, QuestDefinition definition) {
        List<String> issues = new ArrayList<>();

        for (QuestReward reward : definition.getRewards()) {
            switch (reward.getType()) {
                case CONTRIBUTION:
                case EXP:
                    // Granted via quest progress guildId; no pre-check needed
                    break;
                case MONEY:
                    if (!isEconomyAvailable()) {
                        issues.add("Money reward: economy plugin not installed on this server");
                    } else if (!player.hasPermission("guild.economy.deposit")) {
                        issues.add("Money reward: insufficient permission (need guild.economy.deposit)");
                    }
                    break;
            }
        }

        return issues;
    }
}
