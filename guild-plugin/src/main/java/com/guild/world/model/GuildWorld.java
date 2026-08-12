package com.guild.world.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 受管世界实体（worlds.yml 注册表中的一条记录）。
 *
 * <p>该实体只承载元数据，不持有 {@link org.bukkit.World} 引用，
 * 保证注册表可在世界未加载时独立持久化与恢复。
 */
public class GuildWorld {

    private String worldName;
    private WorldType type = WorldType.BATTLE;
    private WorldStatus status = WorldStatus.REGISTERED;
    /** 生成/加载时应用的预设名（预设系统下期实现，本期仅记录） */
    private String presetName = "";
    /** 出生点，格式 "x,y,z,yaw,pitch" */
    private String spawn = "";
    /** 关联工会 ID（战斗世界） */
    private String ownerGuildId = "";
    private long createdAt;
    private long lastActiveAt;

    public GuildWorld() {
    }

    public GuildWorld(String worldName) {
        this.worldName = worldName;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.lastActiveAt = now;
    }

    /* ── 序列化 ─────────────────────────────────────────── */

    public void saveTo(ConfigurationSection section) {
        section.set("type", type.name());
        section.set("status", status.name());
        section.set("preset", presetName);
        section.set("spawn", spawn);
        section.set("owner-guild-id", ownerGuildId);
        section.set("created-at", createdAt);
        section.set("last-active-at", lastActiveAt);
    }

    public static GuildWorld loadFrom(ConfigurationSection section, String worldName) {
        if (section == null) {
            return null;
        }
        GuildWorld gw = new GuildWorld(worldName);
        gw.type = parseEnum(WorldType.class, section.getString("type", "BATTLE"), WorldType.BATTLE);
        gw.status = parseEnum(WorldStatus.class, section.getString("status", "REGISTERED"), WorldStatus.REGISTERED);
        gw.presetName = section.getString("preset", "");
        gw.spawn = section.getString("spawn", "");
        gw.ownerGuildId = section.getString("owner-guild-id", "");
        gw.createdAt = section.getLong("created-at", System.currentTimeMillis());
        gw.lastActiveAt = section.getLong("last-active-at", gw.createdAt);
        return gw;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value == null ? "" : value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /* ── 便捷方法 ───────────────────────────────────────── */

    /**
     * 解析出生点字符串为 {@link Location}（世界未加载时返回 null）。
     */
    public Location parseSpawnLocation() {
        if (spawn == null || spawn.isEmpty()) {
            return null;
        }
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        String[] parts = spawn.split(",");
        try {
            if (parts.length >= 3) {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                float yaw = parts.length >= 5 ? Float.parseFloat(parts[3].trim()) : 0f;
                float pitch = parts.length >= 5 ? Float.parseFloat(parts[4].trim()) : 0f;
                return new Location(world, x, y, z, yaw, pitch);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    public void setSpawnLocation(Location loc) {
        if (loc == null) {
            this.spawn = "";
            return;
        }
        this.spawn = loc.getX() + "," + loc.getY() + "," + loc.getZ() + ","
                + loc.getYaw() + "," + loc.getPitch();
    }

    public void touch() {
        this.lastActiveAt = System.currentTimeMillis();
    }

    /* ── getter / setter ────────────────────────────────── */

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public WorldType getType() {
        return type;
    }

    public void setType(WorldType type) {
        this.type = type;
    }

    public WorldStatus getStatus() {
        return status;
    }

    public void setStatus(WorldStatus status) {
        this.status = status;
    }

    public String getPresetName() {
        return presetName;
    }

    public void setPresetName(String presetName) {
        this.presetName = presetName == null ? "" : presetName;
    }

    public String getSpawn() {
        return spawn;
    }

    public void setSpawn(String spawn) {
        this.spawn = spawn == null ? "" : spawn;
    }

    public String getOwnerGuildId() {
        return ownerGuildId;
    }

    public void setOwnerGuildId(String ownerGuildId) {
        this.ownerGuildId = ownerGuildId == null ? "" : ownerGuildId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(long lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    @Override
    public String toString() {
        return "GuildWorld{name='" + worldName + "', type=" + type + ", status=" + status
                + ", preset='" + presetName + "', guild='" + ownerGuildId + "'}";
    }
}
