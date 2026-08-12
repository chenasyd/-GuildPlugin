# GuildWorldAPI / GuildWarAPI

> Language: [中文](./API-World-War.md) | **English**

For external plugins or other modules in this repo. Obtain via `GuildPlugin` getters or `ServiceContainer`.

```java
GuildWorldAPI worldApi = GuildPlugin.getInstance().getGuildWorldAPI();
GuildWarAPI warApi = GuildPlugin.getInstance().getGuildWarAPI();
```

## GuildWorldAPI

| Method | Description |
|--------|-------------|
| `isAvailable()` / `unavailableReason()` | Folia version gate, etc. |
| `getManagedWorlds()` / `getManagedWorld(name)` | Managed worlds |
| `listPresets()` / `getPreset(name)` | Preset metadata |
| `savePresetFromSelection(player, name)` | Export selection to `.gws` + meta |
| `pastePreset(world, pasteAt, name)` | Paste (`pasteAt` aligns schematic.origin) |
| `createWorldFromPreset(worldName, preset)` | Create BATTLE void world and paste |
| `teleportToWorld(player, worldName)` | Teleport to managed spawn |
| `teleportToFallback(player)` | Safety fallback world |
| `deleteWorld(name, force)` / `unloadWorld(name)` | Unload / delete |

Implementation: `com.guild.world.api.GuildWorldAPIImpl` (delegates to `GuildWorldService`).

Internal helpers (not on the API interface; used by guild war):

- `GuildWorldService.createArenaFromPreset` → world + A/B/spectator spawns
- `resolvePresetSpawns`

## GuildWarAPI

| Method | Description |
|--------|-------------|
| `isAvailable()` / `unavailableReason()` | False when disabled or worlds unavailable |
| `getActiveMatches()` / `getMatch(id)` / `getMatchByPlayer(uuid)` | Lookup |
| `challenge(...)` | Start a challenge (`preset`/`mode`/`max`/`score`/`duration` nullable → config) |
| `accept` / `deny` / `join` / `leave` | Match flow |
| `forceEnd(matchId, reason)` | Force end |

Implementation: `com.guild.war.api.GuildWarAPIImpl`.

## Notes

1. Async methods return `CompletableFuture`. On Folia, mutate player state from callbacks via `CompatibleScheduler` / the entity thread.
2. Teleport via the API / `FoliaTeleportUtils.safeTeleport`; do not call `player.teleport` across regions.
3. Player-facing text goes through `LanguageManager` (`world.*` / `war.*`). API failures are often `LocalizedException` (lang key).

## Related docs

- [GuildWorld_EN.md](./GuildWorld_EN.md) / [GuildWorld.md](./GuildWorld.md)
- [GuildWar_EN.md](./GuildWar_EN.md) / [GuildWar.md](./GuildWar.md)
- [SDK Developer Guide](./SDK%20Developer-Guide.md)
