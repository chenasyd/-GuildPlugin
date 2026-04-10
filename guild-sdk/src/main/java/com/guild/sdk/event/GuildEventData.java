package com.guild.sdk.event;

public class GuildEventData {
    private final int guildId;
    private final String guildName;
    private final String guildLeaderName;

    public GuildEventData(int guildId, String guildName, String guildLeaderName) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.guildLeaderName = guildLeaderName;
    }

    public int getGuildId() { return guildId; }
    public String getGuildName() { return guildName; }
    public String getGuildLeaderName() { return guildLeaderName; }
}
