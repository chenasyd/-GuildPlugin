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
| `arena-protect` | true | Cancel break/place/bucket in PREPARING/COUNTDOWN/ACTIVE |
| `broadcast-report` | true | Broadcast a short report line when the match ends |
| `season.id` | `"default"` | Current season id (stored in `war_season_stats`) |
| `rewards.enabled` | false | Builtin rewards master switch (events always fire) |
| `rewards.winner-vault` | 0 | Vault money per winner player (requires Vault) |
| `rewards.loser-vault` | 0 | Vault money per loser / draw player |
| `rewards.winner-guild-bank` | 0 | Add to winning guild bank |
| `rewards.contribution-win` | 10 | Contribution points per winner (`WAR_WIN`) |
| `rewards.contribution-per-kill` | 1 | Contribution points per kill (`WAR_KILL`) |

After a match: teleport players to `world.safety.fallback-world`, then delete the arena per `world.arena.post-match`.  
Reports persist to `war_matches` / `war_match_players`; season stats via `/guildwar season`.

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
| `report [id]` | Match report (latest for self if omitted) |
| `season` | Season leaderboard GUI / list |
| `admin end <id>` | Force end |

## Events / rewards

- `WarMatchStartEvent` / `WarMatchEndEvent` (`com.guild.war.event`)
- Builtin rewards: `guild-war.rewards.*` (default off)
- See [API-World-War_EN.md](./API-World-War_EN.md)

## i18n

Message keys: `war.*` in `lang/core/{zh,en}.yml`.

## API

See [API-World-War_EN.md](./API-World-War_EN.md) (`GuildWarAPI`).
