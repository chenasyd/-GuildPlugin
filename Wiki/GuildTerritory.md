# 公会领地（Guild Territory）

> 语言: **中文** | [English](./GuildTerritory_EN.md)

基于 **WorldGuard + WorldEdit** 的公会领地方案：官员/会长选区声明 WG 区域，成员自动写入区域 members，与内置 `guild.home-protect` 可协调。

## 前置

| 项 | 说明 |
|----|------|
| 插件 | GuildPlugin 主插件 + `plugins/GuildPlugin/modules/guild-territory.jar` |
| 软依赖 | 服务端安装 **WorldGuard**、**WorldEdit**（运行时探测，非 Maven 硬依赖） |
| 权限 | 见下表 |
| 角色 | `claim` / `unclaim` / `wand` 需官员或会长 |

## 权限

| 节点 | 默认 | 说明 |
|------|------|------|
| `guild.territory.info` | true | 查看当前世界领地（需在公会中） |
| `guild.territory.claim` | true | 声明领地（仍需官员/会长） |
| `guild.territory.unclaim` | true | 放弃领地 |
| `guild.territory.admin` | op | `admin list/force-unclaim/repair-sync` |

内置权限矩阵：在公会内的玩家可使用 `info`；`claim/unclaim` 额外要求 `canManageGuild`（官员/会长）。

## 命令

| 命令 | 说明 |
|------|------|
| `/guild territory wand` | 获取选区斧，左键 Pos1 / 右键 Pos2 |
| `/guild territory pos1` / `pos2` | 以当前位置设角点 |
| `/guild territory claim` | 将选区声明为本公会领地（当前世界） |
| `/guild territory unclaim` | 放弃当前世界领地 |
| `/guild territory info` | 查看当前世界领地信息 |
| `/guild territory gui` | 打开领地管理 GUI |
| `/guild territory admin list` | 列出全部领地（管理员） |
| `/guild territory admin force-unclaim <公会\|ID> [世界]` | 强制放弃 |
| `/guild territory admin repair-sync [公会\|ID]` | 修复 WG 成员同步 |

每公会每世界 **一块** 领地；区域 ID 固定为 `guild_{guildId}`。

## 配置（`config.yml` → `modules.guild-territory`）

| 键 | 默认 | 说明 |
|----|------|------|
| `claim.wand-material` | `WOODEN_AXE` | 选区工具材质 |
| `claim.min-volume` | `1` | 最小选区体积（方块） |
| `claim.max-volume` | `50000` | 最大选区体积 |
| `claim.cost` | `0` | 声明时从**公会金库**扣除；0 为免费 |
| `claim.allowed-worlds` | `[]` | 非空时为白名单 |
| `claim.denied-worlds` | `[]` | 黑名单 |
| `region.priority` | `10` | WG 区域优先级 |
| `flags.*` | 见默认 config | `allow` / `deny` / `none`（不设置） |
| `home-protect.mode` | `defer` | `defer` / `merge` / `off` |
| `member-sync.enabled` | `true` | 成员变更同步 WG |
| `member-sync.repair-on-load` | `false` | 启动时全量修复 |
| `gui.enabled` | `true` | 启用领地 GUI |
| `gui.register-settings-button` | `true` | 在公会设置 GUI 注入管理按钮（官员/会长） |
| `gui.register-info-button` | `true` | 在公会信息 GUI 注入查看按钮（全体成员） |

| `cross-server.enabled` | `true` | 使用共享 DB 表 `guild_territories`（关闭则回退 `territories.json`） |
| `cross-server.server-id` | `""` | 留空则**首次启动随机生成**并写入 `modules/guild-territory/data/server-id.txt`，重启复用 |
| `cross-server.broadcast-events` | `true` | claim/unclaim 后通过 Bungee 广播，加速其它子服内存缓存更新 |
| `cross-server.materialize-on-load` | `true` | 启动时为本机 DB 记录补建 WG 区域 |
| `cross-server.materialize-on-world-load` | `true` | 世界延迟加载时再次尝试 materialize |
| `cross-server.materialize-retry-failed` | `true` | 是否重试上次 `sync_state=FAILED` 的记录 |

### 跨服元数据（C-CS-A）

- 权威存储：`guild_territories`（主键 `guild_id + server_id + world_name`）
- 本机 `server-id`：默认 `terr` + 12 位随机 hex，持久化在模块 data 目录，避免与子服显示名冲突
- WG 区域仍只在本机创建；其它子服仅只读元数据（GUI / `/guild territory info` 可查看全网领地）
- 已有 `territories.json` 会在首次启用 DB 时自动迁移到本机 `server-id`

### 跨服缓存广播（C-CS-B）

- **DB 先写、Bungee 后通知**：本机 claim/unclaim 仍先写入共享 DB，再经 `territory.push` → Bungee → `territory.broadcast` 通知其它子服
- **revision**：使用 `updatedAtEpochMs`（或 unclaim/clear 时的事件时间戳）；接收方若本地 revision ≥ 入站则丢弃（防乱序）
- **接收方**：优先从 DB 单条校验后再更新内存索引；不重复写 DB / 不触发远端 WG materialize
- **限制**：Bungee Plugin Messaging 需在线玩家作载波，无玩家时广播可能发不出 → **不能依赖 Bungee 必达**，重启全量 `load()` DB 可自愈
- 公会解散时发送 `guild-clear` 单条广播（避免 N 次 unclaim 风暴）

### 跨服 WG materialize-on-load（C-CS-C）

- **仅本机** `server-id` 记录：其它子服的元数据只读，不会在本地创建 WG
- **触发时机**：模块启用后、以及 `WorldLoadEvent`（世界延迟加载）
- **条件**：DB 有记录但 WG 区域缺失，或 `sync_state` 为 `PENDING` / `FAILED`（可配置是否重试 FAILED）
- **流程**：读取 DB 边界 → 创建 `ProtectedCuboidRegion` + 默认 flags → 异步拉取公会成员写入 owners/members → 标记 `MATERIALIZED`
- **不广播**：materialize 为本地修复，不发送 `territory.push`
- 配置：`cross-server.materialize-on-load` / `materialize-on-world-load` / `materialize-retry-failed`（默认均 `true`）

### PlaceholderAPI（v0.7+）

需安装 PlaceholderAPI；模块加载后自动注册 identifier `territory`。

| 占位符 | 说明 |
|--------|------|
| `%guild_module_territory_has%` | 本公会当前世界是否有**本机**领地 |
| `%guild_module_territory_has_any%` | 本公会是否有任意领地（含跨服元数据） |
| `%guild_module_territory_count%` / `count_local` | 全网 / 本机领地数量 |
| `%guild_module_territory_region%` / `world%` / `volume%` | 当前世界领地信息 |
| `%guild_module_territory_inside%` / `inside_own%` | 玩家是否站在 WG 领地内 |

完整列表见 [PlaceholderAPI 参考](./PLACEHOLDER_API.md#guild-territory-module-guild-territory-loaded)。

### GUI 入口

- **公会设置** → 「公会领地」（需官员/会长 + `guild.territory.claim`）
- **公会信息** → 「领地信息」（需 `guild.territory.info`，管理操作仍受角色限制）
- **命令** → `/guild territory gui`

管理面板支持：查看当前世界与其它世界领地、选区状态、WorldGuard 就绪状态；管理模式下可获取选区斧、设 Pos1/Pos2、声明/放弃（含确认对话框）。

### 与 `guild.home-protect` 的关系

主配置 `guild.home-protect`（半径保护）在领地模块启用且 WG 就绪时：

- **`defer`**（默认）：完全交由 WorldGuard，内置 home 半径保护不生效
- **`merge`**：仅在 WG 领地内或已有领地的 home 坐标跳过内置保护
- **`off`**：领地模块不干预 home 保护

## 数据与生命周期

1. 声明 → 创建 WG `ProtectedCuboidRegion` + 写入共享 DB（或 JSON 单服模式）
2. 成员入会/退会/升降职 → 异步同步 WG `owners` / `members`
3. 公会解散 → 删除所有世界领地
4. 放弃 → 删除 WG 区域与本地记录

## 构建模块 JAR

```bash
mvn -B package -DskipTests -Pbuild-territory-module -pl guild-plugin -am
```

输出：`guild-plugin/target/modules/guild-territory.jar`

## 相关文档

- [GuildWar](./GuildWar.md) — 固定地图公会战（**不涉及**领地 claim）
- [SDK 模块开发](./SDK%20Developer-Guide.md)
