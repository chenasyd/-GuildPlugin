package com.guild.sdk.territory;

/** 领地元数据与本地 WorldGuard 区域的同步状态。 */
public enum TerritorySyncState {
    PENDING,
    MATERIALIZED,
    FAILED
}
