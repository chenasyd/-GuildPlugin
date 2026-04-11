package com.guild.module.example.quest.model;

public class QuestObjective {
    public enum ObjectiveType {
        KILL_MOBS("击杀怪物"),
        COLLECT_RESOURCES("收集资源"),
        DEPOSIT_MONEY("存入公会资金"),
        ONLINE_HOURS("在线时长(分钟)"),
        GUILD_CONTRIBUTE("贡献值增长");

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
