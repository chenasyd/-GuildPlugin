package com.guild.bungee.data;

/**
 * Represents how a player connected to the proxy.
 *
 * <ul>
 *   <li>{@link #JAVA} — Standard Java Edition client</li>
 *   <li>{@link #BEDROCK} — Bedrock Edition client via Geyser translation layer</li>
 * </ul>
 */
public enum PlayerConnectionType {
    JAVA,
    BEDROCK;

    /**
     * Serialize to wire format string.
     */
    public String toWire() {
        return name();
    }

    /**
     * Parse from wire format string.
     */
    public static PlayerConnectionType fromWire(String s) {
        if (s == null) return JAVA;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return JAVA;
        }
    }
}
