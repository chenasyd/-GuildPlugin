package com.guild.module.example.quest;

import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestObjective;
import com.guild.module.example.quest.model.QuestReward;

/** 内置 Demo 任务定义注册。 */
final class QuestDefaultDefinitions {

    private QuestDefaultDefinitions() {
    }

    static void register(QuestManager questManager) {
        QuestDefinition daily1 = new QuestDefinition(
                "daily_hunter", QuestDefinition.QuestType.DAILY, 1, 1, true);
        daily1.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
                15, "module.quest.daily_hunter.objective"));
        daily1.addReward(new QuestReward(QuestReward.RewardType.EXP, 30));
        daily1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 50));
        questManager.registerDefinition(daily1);

        QuestDefinition daily2 = new QuestDefinition(
                "daily_online", QuestDefinition.QuestType.DAILY, 2, 1, true);
        daily2.addObjective(new QuestObjective(QuestObjective.ObjectiveType.ONLINE_HOURS,
                60, "module.quest.daily_online.objective"));
        daily2.addReward(new QuestReward(QuestReward.RewardType.EXP, 520));
        questManager.registerDefinition(daily2);

        QuestDefinition weekly1 = new QuestDefinition(
                "weekly_contributor", QuestDefinition.QuestType.WEEKLY, 1, 2, true);
        weekly1.addObjective(new QuestObjective(
                QuestObjective.ObjectiveType.DEPOSIT_MONEY, 2000,
                "module.quest.weekly_contributor.objective"));
        weekly1.addReward(new QuestReward(QuestReward.RewardType.EXP, 100));
        weekly1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 300));
        questManager.registerDefinition(weekly1);

        QuestDefinition weekly2 = new QuestDefinition(
                "weekly_slayer", QuestDefinition.QuestType.WEEKLY, 2, 3, true);
        weekly2.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
                100, "module.quest.weekly_slayer.objective"));
        weekly2.addReward(new QuestReward(QuestReward.RewardType.EXP, 2080));
        questManager.registerDefinition(weekly2);

        QuestDefinition oneTime1 = new QuestDefinition(
                "onetime_first_blood", QuestDefinition.QuestType.ONE_TIME, 1, 1, false);
        oneTime1.addObjective(new QuestObjective(QuestObjective.ObjectiveType.KILL_MOBS,
                5, "module.quest.onetime_first_blood.objective"));
        oneTime1.addReward(new QuestReward(QuestReward.RewardType.EXP, 25));
        oneTime1.addReward(new QuestReward(QuestReward.RewardType.MONEY, 100));
        questManager.registerDefinition(oneTime1);
    }
}
