package com.guild.war.model;

/** 公会战对局阶段。 */
public enum WarPhase {
    /** 已发起，等待对方接受 */
    PENDING,
    /** 已接受，双方报名 */
    SIGNUP,
    /** 创建地图并传送 */
    PREPARING,
    /** 场内倒计时，禁止伤害 */
    COUNTDOWN,
    /** 激战中 */
    ACTIVE,
    /** 已结束 */
    ENDED
}
