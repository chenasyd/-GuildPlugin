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

## GuildWarAPI

| Method | Description |
|--------|-------------|
| `isAvailable()` / `unavailableReason()` | False when disabled or worlds unavailable |
| `getActiveMatches()` / `getMatch(id)` / `getMatchByPlayer(uuid)` / `status(player)` | Lookup |
| `challenge(...)` | Start a challenge (`preset`/`mode`/`max`/`score`/`duration` nullable → config) |
| `accept` / `deny` / `join` / `leave` / `ready` / `cancel` | Match flow |
| `forceEnd(matchId, reason)` | Force end |
| `getRecentMatches(limit)` / `getMatchHistory(reportId)` / `getLatestMatchForPlayer(uuid)` | Historical reports (`WarReportSnapshot`) |

Implementation: `com.guild.war.api.GuildWarAPIImpl`.

## Events

Package: `com.guild.war.event`

| Event | When | Cancelable |
|-------|------|------------|
| `WarMatchStartEvent` | Phase → `ACTIVE` | No |
| `WarMatchEndEvent` | After winner/score written, **before** arena destroy | No |

`WarMatchEndEvent#getSnapshot()` → `WarReportSnapshot` (immutable: guilds, scores, mode, reason, participants, seasonId, duration).

### Example: listen and reward

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

Builtin config rewards (`guild-war.rewards.*`) also listen to this event; set `enabled: false` to keep events only.

## Notes

1. Async methods return `CompletableFuture`. On Folia, mutate player state from callbacks via `CompatibleScheduler` / the entity thread.
2. Teleport via the API / `FoliaTeleportUtils.safeTeleport`; do not call `player.teleport` across regions.
3. Player-facing text goes through `LanguageManager` (`world.*` / `war.*`). API failures are often `LocalizedException` (lang key).

## Related docs

- [GuildWorld_EN.md](./GuildWorld_EN.md) / [GuildWorld.md](./GuildWorld.md)
- [GuildWar_EN.md](./GuildWar_EN.md) / [GuildWar.md](./GuildWar.md)
- [CrossServer-War.md](./CrossServer-War.md) 
- [SDK Developer Guide](./SDK%20Developer-Guide.md)
