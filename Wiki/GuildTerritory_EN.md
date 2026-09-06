# Guild Territory

> Language: [中文](./GuildTerritory.md) | **English**

WorldGuard-based guild land claims: officers/leaders select a cuboid, claim it as `guild_{guildId}`, and members are synced into the WG region. Coordinates with builtin `guild.home-protect`.

## Requirements

| Item | Notes |
|------|--------|
| Plugins | GuildPlugin + `plugins/GuildPlugin/modules/guild-territory.jar` |
| Soft deps | **WorldGuard** + **WorldEdit** on the server (runtime probe, not a Maven hard dependency) |
| Permissions | See table below |
| Roles | `claim` / `unclaim` / `wand` require officer or leader |

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `guild.territory.info` | true | View territory in current world (must be in guild) |
| `guild.territory.claim` | true | Claim (still requires manager role) |
| `guild.territory.unclaim` | true | Unclaim |
| `guild.territory.admin` | op | `admin list/force-unclaim/repair-sync/materialize` |

Built-in permission matrix: guild members may use `info`; `claim/unclaim` additionally require `canManageGuild` (officer/leader).

## Commands

| Command | Description |
|---------|-------------|
| `/guild territory wand` | Get selection axe; left-click Pos1 / right-click Pos2 |
| `/guild territory pos1` / `pos2` | Set corner at current position |
| `/guild territory claim` | Claim selection for your guild in the current world |
| `/guild territory unclaim` | Unclaim territory in the current world |
| `/guild territory info` | View territory info in the current world |
| `/guild territory gui` | Open territory management GUI |
| `/guild territory admin list` | List all territories (admin) |
| `/guild territory admin force-unclaim <guild\|ID> [world]` | Force unclaim |
| `/guild territory admin repair-sync [guild\|ID]` | Repair WG member sync |
| `/guild territory admin materialize [guild\|ID] [world] [--force]` | Manually materialize local DB records into WG regions (no broadcast) |

One territory per guild per world; region ID is always `guild_{guildId}`.

## Configuration (`config.yml` → `modules.guild-territory`)

| Key | Default | Description |
|-----|---------|-------------|
| `claim.wand-material` | `WOODEN_AXE` | Selection tool material |
| `claim.min-volume` | `1` | Minimum selection volume (blocks) |
| `claim.max-volume` | `50000` | Maximum selection volume |
| `claim.cost` | `0` | Deducted from **guild treasury** on claim; `0` = free |
| `claim.allowed-worlds` | `[]` | Whitelist when non-empty |
| `claim.denied-worlds` | `[]` | Blacklist |
| `region.priority` | `10` | WG region priority |
| `flags.*` | see default config | `allow` / `deny` / `none` (leave unset) |
| `home-protect.mode` | `defer` | `defer` / `merge` / `off` |
| `member-sync.enabled` | `true` | Sync WG members on membership changes |
| `member-sync.repair-on-load` | `false` | Full repair on startup |
| `gui.enabled` | `true` | Enable territory GUIs |
| `gui.register-settings-button` | `true` | Inject manage button in guild settings GUI (officers/leaders) |
| `gui.register-info-button` | `true` | Inject view button in guild info GUI (all members) |
| `cross-server.enabled` | `true` | Use shared DB table `guild_territories` (off → fallback `territories.json`) |
| `cross-server.server-id` | `""` | Empty = **auto-generate on first start** and persist to `modules/guild-territory/data/server-id.txt` |
| `cross-server.broadcast-events` | `true` | After claim/unclaim, broadcast via Bungee to refresh other backends' in-memory cache |
| `cross-server.materialize-on-load` | `true` | Materialize local DB records into WG on startup |
| `cross-server.materialize-on-world-load` | `true` | Retry materialize when a world loads lazily |
| `cross-server.materialize-retry-failed` | `true` | Retry records with `sync_state=FAILED` |
| `events.enabled` | `true` | Publish SDK territory events via EventBus |

### Cross-server metadata

- **Authoritative store**: `guild_territories` (primary key `guild_id + server_id + world_name`)
- **Local server-id**: default `terr` + 12 random hex chars, persisted under module data dir (avoids collision with Bungee display names)
- WG regions are created **only on the owning backend**; other backends hold read-only metadata (GUI / `/guild territory info` can show network-wide claims)
- Existing `territories.json` is migrated to the local `server-id` on first DB enable

### Cross-server cache broadcast

- **DB first, Bungee second**: local claim/unclaim writes to shared DB, then `territory.push` → Bungee → `territory.broadcast` notifies other backends
- **Revision**: uses `updatedAtEpochMs` (or event timestamp on unclaim/clear); receiver drops inbound if local revision ≥ inbound (out-of-order protection)
- **Receiver**: validates against DB before updating in-memory index; does not re-write DB or trigger remote WG materialize
- **Limitation**: Bungee Plugin Messaging needs an online player as carrier — broadcast may fail with no players → **do not rely on Bungee delivery**; full `load()` from DB on restart self-heals
- Guild disband sends a single `guild-clear` broadcast (avoids N unclaim storms)

### Cross-server WG materialize-on-load

- **Local records only**: metadata from other backends is read-only; no WG regions are created locally for them
- **Triggers**: after module enable, and on `WorldLoadEvent` (lazy world load)
- **Conditions**: DB record exists but WG region missing, or `sync_state` is `PENDING` / `FAILED` (FAILED retry configurable)
- **Flow**: read DB bounds → create `ProtectedCuboidRegion` + default flags → async fetch guild members into owners/members → mark `MATERIALIZED`
- **No broadcast**: materialize is local repair only; does not send `territory.push`
- Config: `cross-server.materialize-on-load` / `materialize-on-world-load` / `materialize-retry-failed` (all default `true`)
- **Admin command**: `/guild territory admin materialize [guild] [world] [--force]` — manual trigger, ignores `materialize-on-load` toggle; `--force` retries `sync_state=FAILED` records

### SDK events & read-only API

Event data classes live in `com.guild.sdk.event.territory.*` (compile against `guild-sdk` only). The module publishes via **EventBus**; set `events.enabled: false` to disable.

| Event class | When fired |
|-------------|------------|
| `TerritoryClaimedEventData` | Local claim succeeds |
| `TerritoryUnclaimedEventData` | Local unclaim / admin force-unclaim |
| `TerritoryMaterializedEventData` | Materialize completes (startup / admin) |
| `TerritorySyncStateChangedEventData` | `sync_state` changes |
| `TerritoryGuildClearedEventData` | Guild disband clears all territories |

**Subscribe example** (other Guild modules; `module.yml` should `soft-depends: [guild-territory]`):

```java
context.getEventBus().subscribe("my-module", TerritoryClaimedEventData.class, event -> {
    // achievements, quests, etc.
});
```

**Read-only API**: `context.getApi().getTerritoryAPI()` → `TerritoryAPI` (`listByGuild`, `findAt`, etc.); returns `null` when the module is not loaded.

> Cross-server Bungee cache sync **does not** publish SDK events; events fire only on local operations (claim/unclaim/materialize/disband, etc.).

### PlaceholderAPI

Requires PlaceholderAPI; the module registers identifier `territory` on load.

| Placeholder | Description |
|-------------|-------------|
| `%guild_module_territory_has%` | Player's guild has a **local** territory in the current world |
| `%guild_module_territory_has_any%` | Player's guild has any territory (including cross-server metadata) |
| `%guild_module_territory_count%` / `count_local` | Network-wide / local territory count |
| `%guild_module_territory_region%` / `world%` / `volume%` | Current-world territory info |
| `%guild_module_territory_inside%` / `inside_own%` | Whether the player stands inside a WG territory |

Full list: [PlaceholderAPI reference](./PLACEHOLDER_API.md#guild-territory-module-guild-territory-loaded).

### GUI entry points

- **Guild Settings** → “Guild Territory” (officers/leaders + `guild.territory.claim`)
- **Guild Info** → “Territory” (`guild.territory.info`; manage actions still require role)
- **Command** → `/guild territory gui`

The management panel shows current/other-world territories, selection state, and WorldGuard readiness; managers can get the wand, set Pos1/Pos2, claim/unclaim (with confirmation dialogs).

**Bedrock native forms**: Bedrock players receive Cumulus `SimpleForm` menus instead of translated chest GUIs. Requires local Geyser or Bungee form relay; wand/Pos1/Pos2 actions reopen the form with updated state.

Set `gui.enabled: false` to disable all GUI entry points.

### Relationship with `guild.home-protect`

When the territory module is loaded and WG is ready, main config `guild.home-protect` (radius protection) behaves as:

- **`defer`** (default): fully delegated to WorldGuard; builtin home radius protection is disabled
- **`merge`**: skip builtin protection only inside WG territories or at home coords that already have a claim
- **`off`**: territory module does not affect home protection

## Data & lifecycle

1. Claim → create WG `ProtectedCuboidRegion` + write to shared DB (or JSON in single-server mode)
2. Member join/leave/promote/demote → async sync WG `owners` / `members`
3. Guild disband → remove all world territories
4. Unclaim → delete WG region and local record

## Build module JAR

```bash
mvn -B package -DskipTests -Pbuild-territory-module -pl guild-plugin -am
```

Output: `guild-plugin/target/modules/guild-territory.jar`

## Related docs

- [GuildWar](./GuildWar_EN.md) — fixed-map guild wars (**does not** involve territory claims)
- [SDK module development](./SDK%20Developer-Guide.md)
