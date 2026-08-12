package com.guild.world.model;

/**
 * 受管世界的生命周期状态。
 *
 * <p>状态机：
 * <pre>
 * REGISTERED ──加载──▶ LOADING ──成功──▶ READY ──开战──▶ BUSY
 *    ▲                                      │              │
 *    │                                      │              │ 结束
 *    └──────── UNLOADED ◀──保存── UNLOADING ◀──────────────┘
 * </pre>
 *
 * <p>异常状态：
 * <ul>
 *   <li>{@link #ERROR} — 操作失败（创建/加载/粘贴异常）</li>
 *   <li>{@link #STALE} — 启动自检发现的残留世界（上次运行异常结束遗留）</li>
 * </ul>
 */
public enum WorldStatus {

    /** 已注册但未加载（可被 load 恢复） */
    REGISTERED,

    /** 正在加载/创建中 */
    LOADING,

    /** 已就绪，可进入 */
    READY,

    /** 使用中（如战斗进行中） */
    BUSY,

    /** 正在卸载 */
    UNLOADING,

    /** 已正常卸载（等价于 REGISTERED 的显式标记） */
    UNLOADED,

    /** 操作失败 */
    ERROR,

    /** 启动自检发现的上次异常遗留 */
    STALE
}
