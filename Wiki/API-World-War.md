# GuildWorldAPI / GuildWarAPI

供外部插件或本仓库其它模块调用。通过 `GuildPlugin` getter 或 `ServiceContainer` 获取。

```java
GuildWorldAPI worldApi = GuildPlugin.getInstance().getGuildWorldAPI();
GuildWarAPI warApi = GuildPlugin.getInstance().getGuildWarAPI();
```

## GuildWorldAPI

| 方法 | 说明 |
|------|------|
| `isAvailable()` / `unavailableReason()` | Folia 版本门控等 |
| `getManagedWorlds()` / `getManagedWorld(name)` | 受管世界 |
| `listPresets()` / `getPreset(name)` | 预设元数据 |
| `savePresetFromSelection(player, name)` | 选区导出 `.gws`+meta |
| `pastePreset(world, pasteAt, name)` | 粘贴（`pasteAt` 对齐 schematic.origin） |
| `createWorldFromPreset(worldName, preset)` | 建 BATTLE 虚空并粘贴 |
| `teleportToWorld(player, worldName)` | 传到受管出生点 |
| `teleportToFallback(player)` | 安全回退世界 |
| `deleteWorld(name, force)` / `unloadWorld(name)` | 卸载/删除 |

实现类：`com.guild.world.api.GuildWorldAPIImpl`（委托 `GuildWorldService`）。

内部扩展（非 API 接口，工会战使用）：

- `GuildWorldService.createArenaFromPreset` → 返回世界 + A/B/观众出生点
- `resolvePresetSpawns`

## GuildWarAPI

| 方法 | 说明 |
|------|------|
| `isAvailable()` / `unavailableReason()` | 配置关闭或世界不可用时 false |
| `getActiveMatches()` / `getMatch(id)` / `getMatchByPlayer(uuid)` | 查询 |
| `challenge(...)` | 发起（preset/mode/max/score/duration 可空=用配置） |
| `accept` / `deny` / `join` / `leave` | 流程 |
| `forceEnd(matchId, reason)` | 强制结束 |

实现类：`com.guild.war.api.GuildWarAPIImpl`。

## 注意事项

1. 异步方法返回 `CompletableFuture`；在 Folia 上回调里改玩家状态请用 `CompatibleScheduler` / 实体线程。
2. 传送请用 API / `FoliaTeleportUtils.safeTeleport`，勿在跨区域直接 `player.teleport`。
3. 玩家可见文案走 `LanguageManager`（`world.*` / `war.*`）；API 异常多为 `LocalizedException`（含 lang key）。

## 相关文档

- [GuildWorld.md](./GuildWorld.md)
- [GuildWar.md](./GuildWar.md)
- [SDK Developer Guide](./SDK%20Developer-Guide.md)
