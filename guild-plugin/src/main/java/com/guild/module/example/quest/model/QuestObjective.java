package com.guild.module.example.quest.model;

public class QuestObjective {
    public enum ObjectiveType {
        KILL_MOBS,
        COLLECT_RESOURCES,
        DEPOSIT_MONEY,
        ONLINE_HOURS,
        GUILD_CONTRIBUTE;

        public String langKey() {
            return "module.quest.objective-type." + name().toLowerCase();
        }

        public static ObjectiveType fromString(String s) {
            for (ObjectiveType t : values()) {
                if (t.name().equalsIgnoreCase(s)) return t;
            }
            return KILL_MOBS;
        }
    }

    private final ObjectiveType type;
    private final int target;
    /** Language key for the objective line; resolved at display time. */
    private final String descriptionKey;

    public QuestObjective(ObjectiveType type, int target, String descriptionKey) {
        this.type = type;
        this.target = target;
        this.descriptionKey = descriptionKey;
    }

    public ObjectiveType getType() { return type; }
    public int getTarget() { return target; }
    public String getDescriptionKey() { return descriptionKey; }
}
