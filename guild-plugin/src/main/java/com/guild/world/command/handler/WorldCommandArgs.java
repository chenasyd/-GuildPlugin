package com.guild.world.command.handler;

import com.guild.world.model.WorldStatus;
import com.guild.world.model.WorldType;

/** /guildworld 参数解析与展示工具。 */
public final class WorldCommandArgs {

    private WorldCommandArgs() {
    }

    public static String flagValue(String[] args, int start, String flag) {
        for (int i = start; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase(flag)) {
                return args[i + 1];
            }
        }
        return null;
    }

    public static boolean containsFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(flag)) {
                return true;
            }
        }
        return false;
    }

    public static WorldType parseType(String value) {
        if (value == null) {
            return WorldType.BATTLE;
        }
        try {
            return WorldType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WorldType.BATTLE;
        }
    }

    public static String statusText(WorldStatus status) {
        return switch (status) {
            case REGISTERED -> "&7REGISTERED";
            case LOADING -> "&eLOADING";
            case READY -> "&aREADY";
            case BUSY -> "&cBUSY";
            case UNLOADING -> "&eUNLOADING";
            case UNLOADED -> "&7UNLOADED";
            case ERROR -> "&4ERROR";
            case STALE -> "&4STALE";
        };
    }
}
