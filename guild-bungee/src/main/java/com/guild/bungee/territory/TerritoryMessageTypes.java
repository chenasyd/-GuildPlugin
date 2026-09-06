package com.guild.bungee.territory;

/**
 * Cross-server guild territory message type constants.
 *
 * <p>All types use the {@code territory.} prefix and travel on channel {@code guild:main}.
 */
public final class TerritoryMessageTypes {

    public static final String PUSH = "territory.push";
    public static final String BROADCAST = "territory.broadcast";

    private TerritoryMessageTypes() {
    }

    public static boolean isTerritoryType(String type) {
        return type != null && type.startsWith("territory.");
    }
}
