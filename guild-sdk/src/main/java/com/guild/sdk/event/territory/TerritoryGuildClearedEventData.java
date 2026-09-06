package com.guild.sdk.event.territory;

import com.guild.sdk.territory.TerritoryEventSource;

/** 公会解散或批量清空领地元数据。 */
public final class TerritoryGuildClearedEventData {

    private final int guildId;
    private final String guildName;
    private final int removedCount;
    private final TerritoryEventSource source;

    public TerritoryGuildClearedEventData(int guildId, String guildName, int removedCount,
                                          TerritoryEventSource source) {
        this.guildId = guildId;
        this.guildName = guildName != null ? guildName : "";
        this.removedCount = removedCount;
        this.source = source != null ? source : TerritoryEventSource.GUILD_DELETE;
    }

    public int getGuildId() {
        return guildId;
    }

    public String getGuildName() {
        return guildName;
    }

    public int getRemovedCount() {
        return removedCount;
    }

    public TerritoryEventSource getSource() {
        return source;
    }
}
