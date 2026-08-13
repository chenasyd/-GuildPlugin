package com.guild.module.example.quest;

import com.guild.core.module.ModuleContext;
import com.guild.core.utils.ColorUtils;
import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestObjective;
import com.guild.module.example.quest.model.QuestReward;

/**
 * Resolves quest module strings via module default language ({@code modules.yml}).
 */
public final class QuestTexts {

    private final ModuleContext context;

    public QuestTexts(ModuleContext context) {
        this.context = context;
    }

    public String t(String key, String fallback) {
        return context.getMessage(key, fallback);
    }

    /** Indexed placeholders {@code {0}}, {@code {1}}, … with an explicit fallback. */
    public String tf(String key, String fallback, Object... formatArgs) {
        String[] strArgs = null;
        if (formatArgs != null && formatArgs.length > 0) {
            strArgs = new String[formatArgs.length];
            for (int i = 0; i < formatArgs.length; i++) {
                strArgs[i] = formatArgs[i] != null ? formatArgs[i].toString() : "";
            }
        }
        return ColorUtils.colorize(
                context.getLanguageManager().getModuleIndexedMessage(key, fallback, strArgs));
    }

    public String questName(QuestDefinition def) {
        if (def == null) return t("module.quest.unknown", "Unknown Quest");
        return t(def.getNameKey(), def.getId());
    }

    public String questName(String questId) {
        if (questId == null || questId.isEmpty()) {
            return t("module.quest.unknown", "Unknown Quest");
        }
        return t("module.quest." + questId + ".name", questId);
    }

    public String questDescription(QuestDefinition def) {
        if (def == null) return "";
        return t(def.getDescriptionKey(), "");
    }

    public String objectiveDescription(QuestObjective obj) {
        if (obj == null) return t("module.quest.no-description", "No description");
        if (obj.getDescriptionKey() != null && !obj.getDescriptionKey().isEmpty()) {
            return t(obj.getDescriptionKey(), obj.getDescriptionKey());
        }
        return objectiveType(obj.getType());
    }

    public String objectiveType(QuestObjective.ObjectiveType type) {
        if (type == null) return t("module.quest.unknown", "Unknown");
        return t(type.langKey(), type.name());
    }

    public String rewardType(QuestReward.RewardType type) {
        if (type == null) return t("module.quest.unknown", "Unknown");
        return t(type.langKey(), type.name());
    }

    public String questTypeShort(QuestDefinition.QuestType type) {
        if (type == null) return t("module.quest.unknown", "Unknown");
        return switch (type) {
            case DAILY -> t("module.quest.type.daily", "Daily");
            case WEEKLY -> t("module.quest.type.weekly", "Weekly");
            case ONE_TIME -> t("module.quest.type.onetime", "One-time");
        };
    }

    public String questTypeFull(QuestDefinition.QuestType type) {
        if (type == null) return t("module.quest.unknown", "Unknown");
        return switch (type) {
            case DAILY -> t("module.quest.type.daily-full", "Daily Quest");
            case WEEKLY -> t("module.quest.type.weekly-full", "Weekly Quest");
            case ONE_TIME -> t("module.quest.type.onetime-full", "One-time Quest");
        };
    }

    public String questTypeColor(QuestDefinition.QuestType type) {
        if (type == null) return "&7";
        return switch (type) {
            case DAILY -> "&e";
            case WEEKLY -> "&6";
            case ONE_TIME -> "&c";
        };
    }
}
