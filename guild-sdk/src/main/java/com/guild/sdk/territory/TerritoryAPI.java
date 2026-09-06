package com.guild.sdk.territory;

import java.util.List;
import java.util.Optional;

/**
 * 公会领地模块只读 API。
 * <p>
 * 通过 {@link com.guild.sdk.GuildPluginAPI#getTerritoryAPI()} 获取；
 * 模块未加载时返回 {@code null}。
 */
public interface TerritoryAPI {

    /** 本机 persistent server-id。 */
    String getLocalServerId();

    /** WorldGuard 桥是否可用。 */
    boolean isWorldGuardReady();

    /** 本机 server-id + 世界下的领地。 */
    Optional<TerritorySnapshot> getLocalTerritory(int guildId, String worldName);

    /** 公会全部领地（含跨服元数据）。 */
    List<TerritorySnapshot> listByGuild(int guildId);

    /** 公会在本机的领地。 */
    List<TerritorySnapshot> listLocalByGuild(int guildId);

    /** 坐标所在 WG 领地（需 WorldGuard 就绪）。 */
    Optional<TerritorySnapshot> findAt(String worldName, int x, int y, int z);
}
