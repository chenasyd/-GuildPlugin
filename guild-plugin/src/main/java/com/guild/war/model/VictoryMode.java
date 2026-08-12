package com.guild.war.model;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import org.bukkit.command.CommandSender;

/** 胜负模式（开战时选定）。 */
public enum VictoryMode {
    /** 击杀先到 N 分 */
    FIRST_TO_SCORE,
    /** 限时积分，结束时分高者胜 */
    TIMED_SCORE,
    /** 死亡淘汰，最后存活队伍胜（有时限兜底） */
    LAST_STANDING;

    public static VictoryMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toLowerCase();
        return switch (s) {
            case "first", "first_to_score", "score", "积分", "先到" -> FIRST_TO_SCORE;
            case "timed", "timed_score", "time", "限时", "限时积分" -> TIMED_SCORE;
            case "survive", "last", "last_standing", "survival", "存活", "淘汰" -> LAST_STANDING;
            default -> {
                try {
                    yield VictoryMode.valueOf(raw.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }

    public String displayName(GuildPlugin plugin, CommandSender sender) {
        return switch (this) {
            case FIRST_TO_SCORE -> CoreMsg.raw(plugin, sender, "war.mode.first", "积分先到");
            case TIMED_SCORE -> CoreMsg.raw(plugin, sender, "war.mode.timed", "限时积分");
            case LAST_STANDING -> CoreMsg.raw(plugin, sender, "war.mode.survive", "最终存活");
        };
    }

    /** @deprecated use displayName(plugin, sender) */
    public String displayName() {
        return displayName(null, null); // CoreMsg falls back to def
    }

    /** Lang key for per-recipient broadcasts. */
    public String langKey() {
        return switch (this) {
            case FIRST_TO_SCORE -> "war.mode.first";
            case TIMED_SCORE -> "war.mode.timed";
            case LAST_STANDING -> "war.mode.survive";
        };
    }
}
