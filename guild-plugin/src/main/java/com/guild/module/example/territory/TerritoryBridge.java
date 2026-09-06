package com.guild.module.example.territory;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * WorldGuard 操作抽象层。
 * <p>
 * 本接口隔离 WG API，便于单测 mock 与无 WG 环境下的 No-Op 实现。
 */
public interface TerritoryBridge {

    /** 是否已绑定可用的 WG 运行时。 */
    boolean isOperational();

    /**
     * 以公会名义创建立方体区域并写入默认 Flag + 成员。
     *
     * @return 创建后的映射记录；未实现或失败时 empty
     */
    Optional<TerritoryRecord> claimTerritory(TerritoryClaimRequest request);

    /** 删除公会对应 WG 区域。 */
    boolean unclaimTerritory(int guildId, String worldName);

    /** 全量替换区域成员列表（入会/退会/批量同步）。 */
    void syncMembers(int guildId, String worldName, Collection<UUID> memberUuids, UUID leaderUuid);

    /** 查询某公会是否在本世界拥有领地。 */
    Optional<TerritoryRecord> findTerritory(int guildId, String worldName);

    /** 查询某点落入的公会领地（若有）。 */
    Optional<TerritoryRecord> findTerritoryAt(String worldName, int x, int y, int z);

    /**
     * 本机是否已有与记录对应的 WG 区域（世界未加载时返回 {@code false}）。
     */
    boolean hasRegionForRecord(TerritoryRecord record);

    /**
     * 从 DB 元数据在本机创建/修复 WG 区域（主线程调用）。
     */
    TerritoryMaterializeOutcome materializeFromRecord(TerritoryRecord record,
                                                      UUID leaderUuid,
                                                      Collection<UUID> memberUuids);
}
