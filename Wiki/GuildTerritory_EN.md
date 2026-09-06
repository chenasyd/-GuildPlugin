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
| `guild.territory.admin` | op | `admin list/force-unclaim/repair-sync` |

## Commands

See the [Chinese doc](./GuildTerritory.md#命令) for the full command table (same subcommands).

## Configuration

See `modules.guild-territory` in `config.yml` — `claim.*`, `flags.*`, `home-protect.mode`, `member-sync.*`.

## Build

```bash
mvn -B package -DskipTests -Pbuild-territory-module -pl guild-plugin -am
```

Output: `guild-plugin/target/modules/guild-territory.jar`
