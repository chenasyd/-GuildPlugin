package com.guild.module.example.quest.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Quest template. Display text is resolved from language keys at render time
 * ({@code module.quest.<id>.name} / {@code .description} / objective keys),
 * not stored as baked-in strings.
 */
public class QuestDefinition {
    private final String id;
    private final QuestType type;
    private final int sortOrder;
    private final List<QuestObjective> objectives;
    private final List<QuestReward> rewards;
    private final int minGuildLevel;
    private final boolean repeatable;

    public enum QuestType {
        DAILY,
        WEEKLY,
        ONE_TIME
    }

    public QuestDefinition(String id, QuestType type, int sortOrder,
                           int minGuildLevel, boolean repeatable) {
        this.id = id;
        this.type = type;
        this.sortOrder = sortOrder;
        this.minGuildLevel = minGuildLevel;
        this.repeatable = repeatable;
        this.objectives = new ArrayList<>();
        this.rewards = new ArrayList<>();
    }

    public void addObjective(QuestObjective objective) {
        this.objectives.add(objective);
    }

    public void addReward(QuestReward reward) {
        this.rewards.add(reward);
    }

    public String getId() { return id; }

    public String getNameKey() {
        return "module.quest." + id + ".name";
    }

    public String getDescriptionKey() {
        return "module.quest." + id + ".description";
    }

    public QuestType getType() { return type; }
    public int getSortOrder() { return sortOrder; }
    public List<QuestObjective> getObjectives() { return objectives; }
    public List<QuestReward> getRewards() { return rewards; }
    public int getMinGuildLevel() { return minGuildLevel; }
    public boolean isRepeatable() { return repeatable; }

    public boolean isCompleted(int[] progress) {
        if (progress == null || progress.length != objectives.size()) return false;
        for (int i = 0; i < objectives.size(); i++) {
            if (progress[i] < objectives.get(i).getTarget()) return false;
        }
        return true;
    }

    public double getProgressPercent(int[] progress) {
        if (objectives.isEmpty()) return 100.0;
        double total = 0;
        for (int i = 0; i < objectives.size(); i++) {
            QuestObjective obj = objectives.get(i);
            total += Math.min(1.0, (double) (progress != null && i < progress.length ? progress[i] : 0) / obj.getTarget());
        }
        return (total / objectives.size()) * 100.0;
    }
}
