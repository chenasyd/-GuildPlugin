# Guild Territory

> Language: [中文](./GuildTerritory.md) | **English**

WorldGuard-based guild land claims: officers/leaders select a cuboid, claim it as `guild_{guildId}`, and members are synced into the WG region. Coordinates with builtin `guild.home-protect`.

## Requirements

| Item | Notes |
|------|--------|
| Plugins | GuildPlugin + `plugins/GuildPlugin/modules/guild-territory.jar` |
| Soft deps | **WorldGuard** + **WorldEdit** on the server (runtime probe) |
| Roles | `claim` / `unclaim` / `wand` require officer or leader |

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `guild.territory.info` | true | View territory in current world (must be in guild) |
| `guild.territory.claim` | true | Claim (still requires manager role) |
| `guild.territory.unclaim` | true | Unclaim |
| `guild.territory.admin` | op | `admin list/force-unclaim/repair-sync/materialize` |

## Commands

| Command | Description |
|---------|-------------|
| `/guild territory gui` | Open territory management GUI |
| `/guild territory claim\|unclaim\|info\|wand\|pos1\|pos2` | Same as command table in [Chinese doc](./GuildTerritory.md#命令) |
| `/guild territory admin ...` | Admin tools |

## Configuration

See `modules.guild-territory` in `config.yml` — `claim.*`, `flags.*`, `home-protect.mode`, `member-sync.*`, `gui.*`, `cross-server.*`.

### Cross-server (C-CS-A / C-CS-B)

- **Authoritative store**: shared `guild_territories` table (`guild_id + server_id + world_name`)
- **Server ID**: auto-generated `terr` + 12 hex chars, persisted under module data dir
- **Broadcast** (`cross-server.broadcast-events`, default `true`): after claim/unclaim, pushes `territory.push` → Bungee → `territory.broadcast` so other backends refresh in-memory cache (DB remains source of truth; Bungee is best-effort)
- **Revision**: `updatedAtEpochMs`; stale messages are dropped
- WG regions are still created only on the owning backend server
- **Materialize-on-load** (`cross-server.materialize-on-load`): on startup/world load, local DB records without a matching WG region are materialized into WorldGuard (members fetched async); does not broadcast
- **Admin materialize** (v0.9+): `/guild territory admin materialize [guild] [world] [--force]` — manual trigger, ignores `materialize-on-load`; `--force` retries `FAILED` records
- **PlaceholderAPI** (v0.7+): `%guild_module_territory_*` — see [PLACEHOLDER_API.md](./PLACEHOLDER_API.md#guild-territory-module-guild-territory-loaded)

### GUI

- **Guild Settings** → “Guild Territory” (managers + `guild.territory.claim`)
- **Guild Info** → “Territory” (`guild.territory.info`)
- **Command** → `/guild territory gui`

Set `gui.enabled: false` to disable all GUI entry points.

**Bedrock native forms (v0.8+)**: Bedrock players get Cumulus `SimpleForm` menus instead of translated chest GUIs. Requires local Geyser or Bungee form relay; wand/Pos1/Pos2 actions reopen the form with updated state.

## Build

```bash
mvn -B package -DskipTests -Pbuild-territory-module -pl guild-plugin -am
```

Output: `guild-plugin/target/modules/guild-territory.jar`
