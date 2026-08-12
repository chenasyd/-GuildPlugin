# GuildWar — Fixed-Map Guild PVP

> Language: [中文](./GuildWar.md) | **English**

Small team PVP: challenge → accept → signup → arena countdown → fight → settle → tear down.  
**No claims/territory.** Not linked to diplomatic `RelationType.WAR`.

## Prerequisites

- Permissions: `guild.war` (players), `guild.war.admin` (force end)
- Command: `/guildwar` (aliases: `gwar` / `工会战`)
- Requires: GuildWorld available, and a preset that has a schematic
- Challenge / accept / cancel / ready: leader or officer

## Config (`config.yml` → `guild-war`)

| Key | Default | Description |
|-----|---------|-------------|
| `enabled` | true | Master switch |
| `default-preset` | `""` | Default preset; if empty, challenge must pass `--preset` |
| `max-per-team` | 5 | Signup cap per team |
| `signup-seconds` | 60 | Signup window |
| `countdown-seconds` | 10 | In-arena countdown before fight |
| `challenge-timeout-seconds` | 120 | Challenge timeout |
| `default-mode` | first | `first` / `timed` / `survive` |
| `score-to-win` | 20 | First-to-N score |
| `timed-duration-seconds` | 600 | Timed-score duration |
| `survive-duration-seconds` | 600 | Survive mode time limit (fallback) |
| `friendly-fire` | false | Friendly fire |
| `keep-inventory` | true | Keep inventory on death for this match |
| `max-concurrent` | 3 | Max concurrent matches |
| `eliminate-to-spectator` | true | Eliminated → spectator; `false` → fallback world |

After a match: teleport players to `world.safety.fallback-world`, then delete the arena per `world.arena.post-match`.

## Flow

```text
Officer A: /guildwar challenge <guild name|tag> [--preset] [--mode first|timed|survive] [--max] [--score] [--time]
Officer B: /guildwar accept
Members:   /guildwar join
(timeout or both officers /guildwar ready) → create BATTLE world + paste → TP to A/B spawns
In-arena countdown → ACTIVE
End → return → delete world
```

## Victory modes

| Mode | Description |
|------|-------------|
| `first` | Kill scoring, first to N; **no time limit** |
| `timed` | Kill scoring; higher score when time is up |
| `survive` | Death = elimination (spectator / kick); last team standing; timed fallback |

Score modes respawn at the team spawn; survive mode eliminates permanently for that match.

## Subcommands

| Command | Description |
|---------|-------------|
| `challenge / accept / deny` | Challenge and response |
| `join / leave` | Signup (PENDING/SIGNUP only) |
| `ready` | Both officers ready → start early |
| `cancel` | Cancel before fight |
| `status` | Match status |
| `admin end <id>` | Force end |

## i18n

Message keys: `war.*` in `lang/core/{zh,en}.yml`.  
Inventory: [World-War-I18N.md](./World-War-I18N.md).

## API

See [API-World-War_EN.md](./API-World-War_EN.md) (`GuildWarAPI`).
