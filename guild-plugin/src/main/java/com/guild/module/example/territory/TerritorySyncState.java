package com.guild.module.example.territory;

/** 领地元数据与本地 WG 区域的同步状态（跨服元数据层）。 */
public enum TerritorySyncState {

    PENDING,
    MATERIALIZED,
    FAILED;

    public static TerritorySyncState fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return MATERIALIZED;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MATERIALIZED;
        }
    }
}
