package com.guild.module.example.quest.model;

public class QuestReward {
    public enum RewardType {
        /** @deprecated Use EXP (guild tree virtual experience). Kept for legacy definitions. */
        @Deprecated
        CONTRIBUTION,
        MONEY,
        /** Guild tree virtual experience (shared pool). */
        EXP;

        public String langKey() {
            if (this == CONTRIBUTION) {
                return "module.quest.reward.exp";
            }
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
