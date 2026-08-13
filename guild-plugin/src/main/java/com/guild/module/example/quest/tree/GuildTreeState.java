package com.guild.module.example.quest.tree;

/**
 * In-memory snapshot of a guild quest tree.
 */
public final class GuildTreeState {
    private final int guildId;
    private int treeLevel;
    private long virtualExp;

    public GuildTreeState(int guildId, int treeLevel, long virtualExp) {
        this.guildId = guildId;
        this.treeLevel = Math.max(1, treeLevel);
        this.virtualExp = Math.max(0L, virtualExp);
    }

    public int getGuildId() {
        return guildId;
    }

    public int getTreeLevel() {
        return treeLevel;
    }

    public void setTreeLevel(int treeLevel) {
        this.treeLevel = Math.max(1, treeLevel);
    }

    public long getVirtualExp() {
        return virtualExp;
    }

    public void setVirtualExp(long virtualExp) {
        this.virtualExp = Math.max(0L, virtualExp);
    }

    public void addVirtualExp(long amount) {
        if (amount > 0) {
            this.virtualExp += amount;
        }
    }

    public boolean consumeVirtualExp(long amount) {
        if (amount <= 0) return true;
        if (virtualExp < amount) return false;
        virtualExp -= amount;
        return true;
    }
}
