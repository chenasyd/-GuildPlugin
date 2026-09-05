/**
 * 公会领地模块（WorldGuard 集成）— 设计占位包。
 *
 * <h2>目标</h2>
 * 以<strong>公会名义</strong>在 WorldGuard 中创建/管理保护区域；同公会会员作为区域成员，
 * 可在领地内自由建造与交互，非会员受 WG Flag 限制。
 *
 * <h2>区域命名</h2>
 * <ul>
 *   <li>WG 区域 ID：{@code guild_{guildId}}（稳定、可预测，避免重名）</li>
 *   <li>可选显示名/metadata：公会名称（存模块 {@code territories.json}，不写进 WG ID）</li>
 * </ul>
 *
 * <h2>WG 权限模型（会员自由操作）</h2>
 * <pre>
 * owners  = 会长 UUID
 * members = 全体在籍会员 UUID（入会增、退会删）
 *
 * Flags（子区域覆盖 __global__）：
 *   build / block-break / block-place / use / interact / chest-access → 成员可用
 *   pvp、mob-damage、tnt 等 → 可配置，默认领地内关闭 PVP
 *   entry / exit → 默认允许进入（或非会员 deny，视服主策略）
 * </pre>
 * WG 的 {@code BUILD} 与成员域联动：玩家在 {@code members} 列表中即视为 MEMBER，
 * 无需为每位会员单独设 Flag。
 *
 * <h2>生命周期</h2>
 * <ol>
 *   <li>会长选区（pos1/pos2 或 WE 魔杖）→ {@code /guild territory claim}</li>
 *   <li>模块创建 {@link com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion} 并 {@code saveChanges()}</li>
 *   <li>写入 {@link TerritoryRecord} 映射 guildId ↔ regionId/world/bounds</li>
 *   <li>成员变更 SDK 事件 → 同步 {@code region.getMembers()}</li>
 *   <li>解散公会 → 删除 WG 区域 + 本地记录</li>
 * </ol>
 *
 * <h2>与现有系统关系</h2>
 * <ul>
 *   <li>{@code GuildHomeProtectListener}：领地启用且 WG 可用时，建议关闭 home-protect 或合并逻辑</li>
 *   <li>{@code SelectionManager}：可复用 pos1/pos2 选区（需开放给会长而不仅是管理员）</li>
 *   <li>跨服：WG 区域为<strong>单服单世界</strong>数据；公会 DB 存「哪服哪世界有领地」，各子服按需创建</li>
 * </ul>
 *
 * <h2>实现阶段（当前为 P7-a 骨架）</h2>
 * P7-b {@link TerritoryBridge} WG 适配 → P7-c 成员同步 → P7-d 命令/GUI → P7-e 与 home-protect 互斥
 *
 * @see TerritoryModule
 * @see WorldGuardProbe
 */
package com.guild.module.example.territory;
