package com.guild.module.example.quest.model;

public class QuestReward {
    public enum RewardType {
        CONTRIBUTION("贡献值"),
        MONEY("金币"),
        EXP("经验值");

        private final String displayName;
        RewardType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
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
