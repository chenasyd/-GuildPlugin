package com.guild.world.model;

/**
 * 受管世界的用途类型。
 */
public enum WorldType {

    /** 工会战/活动战斗世界（独立世界，战后可回收） */
    BATTLE,

    /** 编辑世界（用于建造预设地图；崩溃后可直接丢弃重建） */
    EDIT,

    /** 模板世界（长期保留的公共模板） */
    TEMPLATE
}
