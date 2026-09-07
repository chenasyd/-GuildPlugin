package com.guild.module.example.quest;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestProgress;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 任务领奖与完成通知。 */
final class QuestClaimService {

    private final GuildQuestModule module;

    QuestClaimService(GuildQuestModule module) {
        this.module = module;
    }

    void claimQuestReward(Player player, String questId) {
        if (player == null || questId == null || questId.isEmpty()) {
            return;
        }
        module.getContext().getApi().getPlayerGuild(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                module.getContext().runSync(() -> module.getContext().sendMessage(player, "module.quest.error.no-guild",
                        module.getContext().getMessage("module.quest.error.no-guild", "&cYou are not in any guild")));
                return;
            }
            int guildId = guild.getId();
            module.getContext().runSync(() -> doClaimQuestReward(player, guildId, questId));
        }).exceptionally(ex -> {
            module.getContext().runSync(() -> module.getContext().sendMessage(player, "module.quest.error.load-fail",
                    "&cFailed to query guild: " + ex.getMessage()));
            return null;
        });
    }

    private void doClaimQuestReward(Player player, int guildId, String questId) {
        QuestManager questManager = module.getQuestManager();
        QuestDefinition definition = questManager.getDefinition(questId);
        QuestProgress progress = questManager.getPlayerQuest(guildId, player.getUniqueId(), questId);
        if (definition == null || progress == null) {
            progress = questManager.getPlayerQuestAny(guildId, player.getUniqueId(), questId);
        }
        if (definition == null || progress == null) {
            module.getContext().sendMessage(player, "module.quest.claim.not-found",
                    "&c[Quest] No claimable progress for this quest.");
            return;
        }
        if (progress.isClaimed()) {
            module.getContext().sendMessage(player, "module.quest.claim.already",
                    "&e[Quest] Reward already claimed.");
            return;
        }
        if (!progress.isCompletedMarked() && !progress.isObjectivesCompleted(definition)) {
            module.getContext().sendMessage(player, "module.quest.claim.not-ready",
                    "&c[Quest] Quest is not completed yet.");
            return;
        }
        if (!progress.isCompletedMarked()) {
            questManager.tryMarkCompleted(progress);
        }
        module.getRewardHandler().grantRewards(player, definition, progress);
        questManager.saveGuildProgress(guildId);
        module.getContext().getEventBus().publish(
                new GuildQuestModule.QuestCompletedEvent(player.getName(),
                        module.texts().questName(definition), guildId));
        module.getContext().sendMessage(player, "module.quest.reward-claimed", "&a[Quest] Rewards granted!");

        Map<String, Object> refreshData = new HashMap<>();
        refreshData.put("guildId", guildId);
        refreshData.put("playerUuid", player.getUniqueId());
        refreshData.put("questId", questId);
        module.getContext().notifyGUIRefresh("quest-active-list", refreshData);
        module.getContext().notifyGUIRefresh("quest-detail", refreshData);
        module.getContext().notifyGUIRefresh("quest-list", refreshData);
    }

    void notifyQuestCompleted(QuestProgress progress, QuestDefinition definition) {
        if (progress == null || definition == null) {
            return;
        }
        Player player = Bukkit.getPlayer(progress.getPlayerUuid());
        if (player == null || !player.isOnline()) {
            return;
        }

        String questName = module.texts().questName(definition);
        CompatibleScheduler.runTask(module.getContext().getPlugin(), player, () -> {
            String line = module.texts().tf("module.quest.complete.notify",
                    "&a[Quest] Quest &e{0} &ahas been completed!", questName);
            player.sendMessage(line);

            String claimBtn = module.texts().t("module.quest.complete.claim-button",
                    "&e&l[Click to claim reward]");
            String claimHover = module.texts().tf("module.quest.complete.claim-hover",
                    "&7Click to claim rewards for &e{0}", questName);
            String openBtn = module.texts().t("module.quest.complete.open-button",
                    "&7[Open quest panel]");
            String openHover = module.texts().t("module.quest.complete.open-hover",
                    "&7Run /g quest to open the quest GUI");

            TextComponent claim = new TextComponent(ColorUtils.colorize(claimBtn));
            claim.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/guild quest claim " + definition.getId()));
            claim.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(ColorUtils.colorize(claimHover))));

            TextComponent sep = new TextComponent(ColorUtils.colorize(" &8| "));

            TextComponent open = new TextComponent(ColorUtils.colorize(openBtn));
            open.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild quest"));
            open.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(ColorUtils.colorize(openHover))));

            TextComponent row = new TextComponent("");
            row.addExtra(claim);
            row.addExtra(sep);
            row.addExtra(open);
            player.spigot().sendMessage(row);
        });
    }
}
