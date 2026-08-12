package com.guild.bungee.war;

/**
 * Cross-server guild-war message type constants (protocol skeleton for P3).
 *
 * <p>All types use the {@code war.} prefix and travel on channel {@code guild:main}.
 */
public final class WarMessageTypes {

    public static final String CHALLENGE = "war.challenge";
    public static final String CHALLENGE_NOTIFY = "war.challenge.notify";
    public static final String ACCEPT = "war.accept";
    public static final String DENY = "war.deny";
    public static final String ARENA_CREATE = "war.arena.create";
    public static final String ARENA_READY = "war.arena.ready";
    public static final String TRANSFER = "war.transfer";
    public static final String END_SNAPSHOT = "war.end.snapshot";
    public static final String REPORT_FANOUT = "war.report.fanout";

    private WarMessageTypes() {
    }

    public static boolean isWarType(String type) {
        return type != null && type.startsWith("war.");
    }
}
