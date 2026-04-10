package com.guild.core.module;

/**
 * 模块状态枚举
 */
public enum ModuleState {
    /** 未加载（jar存在但未初始化） */
    UNLOADED,
    /** 正在加载中 */
    LOADING,
    /** 已加载且运行中 */
    ACTIVE,
    /** 正在禁用中 */
    DISABLING,
    /** 加载/运行时出错 */
    ERROR
}
