package com.guild.module.example.territory;

/**
 * 跨服领地同步消息类型（通道 {@code guild:main}）。
 * <p>
 * 子服上报 {@link #PUSH}，Bungee 转发为 {@link #BROADCAST} 至其它子服。
 */
public final class TerritoryMessageTypes {

    /** 子服 → Proxy：claim / unclaim / guild-clear 上报。 */
    public static final String PUSH = "territory.push";

    /** Proxy → 子服：fan-out（排除来源服）。 */
    public static final String BROADCAST = "territory.broadcast";

    private TerritoryMessageTypes() {
    }

    public static boolean isTerritoryType(String type) {
        return type != null && type.startsWith("territory.");
    }
}
