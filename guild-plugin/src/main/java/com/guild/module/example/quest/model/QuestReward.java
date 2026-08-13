package com.guild.module.example.quest.model;

public class QuestReward {
    public enum RewardType {
        CONTRIBUTION,
        MONEY,
        EXP;

        public String langKey() {
            return "module.quest.reward." + name().toLowerCase();
        }
    }

    private final RewardType type;
    private final double amount;

    public QuestReward(RewardType type, double amount) {
        this.type = type;
        this.amount = amount;
    }

    public RewardType getType() { return type; }
    public double getAmount() { return amount; }
}
