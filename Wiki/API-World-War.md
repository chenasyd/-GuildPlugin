# GuildWorldAPI / GuildWarAPI

> Language: **中文** | [English](./API-World-War_EN.md)

供外部插件或本仓库其他模块调用。通过 `GuildPlugin` getter 或 `ServiceContainer` 获取。

```java
GuildWorldAPI worldApi = GuildPlugin.getInstance().getGuildWorldAPI();
GuildWarAPI warApi = GuildPlugin.getInstance().getGuildWarAPI();
```

## GuildWorldAPI

| 方法 | 说明 |
|------|------|
| `isAvailable()` / `unavailableReason()` | Folia 版本门禁等 |
| `getManagedWorlds()` / `getManagedWorld(name)` | 受管世界 |
| `listPresets()` / `getPreset(name)` | 预设元数据 |
| `savePresetFromSelection(player, name)` | 选区导出为 `.gws` + 元数据 |
| `pastePreset(world, pasteAt, name)` | 粘贴（`pasteAt` 对齐 schematic.origin） |
| `createWorldFromPreset(worldName, preset)` | 创建 BATTLE 虚空世界并粘贴 |
| `teleportToWorld(player, worldName)` | 传送到受管出生点 |
| `teleportToFallback(player)` | 安全回退世界 |
| `deleteWorld(name, force)` / `unloadWorld(name)` | 卸载 / 删除 |

实现：`com.guild.world.api.GuildWorldAPIImpl`（委托 `GuildWorldService`）。

## GuildWarAPI

| 方法 | 说明 |
|------|------|
| `isAvailable()` / `unavailableReason()` | 关闭或世界系统不可用时为 false |
| `getActiveMatches()` / `getMatch(id)` / `getMatchByPlayer(uuid)` / `status(player)` | 查询 |
| `challenge(...)` | 发起挑战（`preset`/`mode`/`max`/`score`/`duration` 可空 → 读配置） |
| `accept` / `deny` / `join` / `leave` / `ready` / `cancel` | 对局流程 |
| `forceEnd(matchId, reason)` | 强制结束 |
| `getRecentMatches(limit)` / `getMatchHistory(reportId)` / `getLatestMatchForPlayer(uuid)` | 历史战报（`WarReportSnapshot`） |

实现：`com.guild.war.api.GuildWarAPIImpl`。

## 事件

包：`com.guild.war.event`

| 事件 | 时机 | 可取消 |
|------|------|--------|
| `WarMatchStartEvent` | 进入 `ACTIVE` | 否 |
| `WarMatchEndEvent` | 写入胜负/比分后、**删图之前** | 否 |

`WarMatchEndEvent#getSnapshot()` → `WarReportSnapshot`（不可变：双方公会、比分、模式、原因、参与者、赛季、耗时）。

### 示例：监听结算发奖

```java
public final class MyWarHook implements Listener {
    private final Economy economy; // Vault

    @EventHandler
    public void onWarEnd(WarMatchEndEvent event) {
        WarReportSnapshot snap = event.getSnapshot();
        Integer winner = snap.winnerGuildId();
        for (WarParticipantSnapshot p : snap.participants()) {
            boolean win = winner != null && winner == p.guildId();
            Player player = Bukkit.getPlayer(p.uuid());
            if (player != null && win) {
                economy.depositPlayer(player, 100.0);
            }
        }
    }
}
```

内置配置奖励（`guild-war.rewards.*`）同样监听该事件；将 `enabled` 设为 `false` 可仅保留事件钩子。

## 注意

1. 异步方法返回 `CompletableFuture`。Folia 上请在回调里用 `CompatibleScheduler` / 实体线程改玩家状态。
2. 传送请走 API / `FoliaTeleportUtils.safeTeleport`，勿跨区域直接 `player.teleport`。
3. 面向玩家文案走 `LanguageManager`（`world.*` / `war.*`）。API 失败多为 `LocalizedException`（语言键）。

## 相关文档

- [GuildWorld.md](./GuildWorld.md) / [GuildWorld_EN.md](./GuildWorld_EN.md)
- [GuildWar.md](./GuildWar.md) / [GuildWar_EN.md](./GuildWar_EN.md)
- [CrossServer-War.md](./CrossServer-War.md)（P3 协议骨架，尚未接线生产开战）
- [SDK Developer Guide](./SDK%20Developer-Guide.md)
