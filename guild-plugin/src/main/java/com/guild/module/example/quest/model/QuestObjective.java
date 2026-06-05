package com.guild.module.example.quest.model;

public class QuestObjective {
    public enum ObjectiveType {
        KILL_MOBS("Kill Mobs"),
        COLLECT_RESOURCES("Collect Resources"),
        DEPOSIT_MONEY("Deposit to Guild"),
        ONLINE_HOURS("Online Time (min)"),
        GUILD_CONTRIBUTE("C-Coins Growth");

        private final String displayName;
        ObjectiveType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        public static ObjectiveType fromString(String s) {
            for (ObjectiveType t : values()) if (t.name().equalsIgnoreCase(s)) return t;
            return KILL_MOBS;
        }
    }

    private final ObjectiveType type;
    private final int target;
    private final String description;

    public QuestObjective(ObjectiveType type, int target, String description) {
        this.type = type;
        this.target = target;
        this.description = description;
    }

    public ObjectiveType getType() { return type; }
    public int getTarget() { return target; }
    public String getDescription() { return description; }
}
