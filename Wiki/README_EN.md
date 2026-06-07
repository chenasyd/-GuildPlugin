# GuildPlugin — Quick Start

A feature-complete Minecraft guild/faction system. Supports Spigot, Paper, Purpur, and Folia.

## Compatibility

| Software | Version |
|:--------:|:-------:|
| Spigot | 1.21+ |
| Paper | 1.21+ |
| Purpur | 1.21+ |
| Folia | 1.21+ |

Integrations: Vault (economy), PlaceholderAPI

## Installation

1. Download from [Releases](https://github.com/chenasyd/-GuildPlugin/releases)
2. Place the JAR in `plugins/`
3. Restart the server
4. Configure `plugins/GuildPlugin/config.yml`
5. Run `/guildadmin reload`

> ⚠️ **When upgrading**: Always back up your configuration and data first. It is recommended to delete `messages_*.yml` files and let the plugin regenerate them — this ensures all new messages are included and avoids display errors.

## Commands

### Player Commands (`/guild`)

| Command | Permission | Description |
|:-------:|:----------:|:-----------:|
| `/guild` | `guild.use` | Open main GUI |
| `/guild create <name> [tag] [desc]` | `guild.create` | Create a guild |
| `/guild info` | `guild.use` | View guild info |
| `/guild members` | `guild.use` | List members |
| `/guild invite <player>` | `guild.invite` | Invite a player |
| `/guild kick <player>` | `guild.kick` | Kick a member |
| `/guild promote <player>` | `guild.promote` | Promote to officer |
| `/guild demote <player>` | `guild.demote` | Demote officer |
| `/guild accept <guild>` | `guild.use` | Accept invitation |
| `/guild decline <guild>` | `guild.use` | Decline invitation |
| `/guild leave` | `guild.use` | Leave guild |
| `/guild delete` | `guild.delete` | Delete guild (opens confirm GUI) |
| `/guild delete confirm` | `guild.delete` | Confirm deletion |
| `/guild delete cancel` | `guild.delete` | Cancel deletion |
| `/guild sethome` | `guild.sethome` | Set guild home |
| `/guild home` | `guild.home` | Teleport to guild home |
| `/guild deposit <amount>` | `guild.deposit` | Deposit funds |
| `/guild withdraw <amount>` | `guild.withdraw` | Withdraw funds |
| `/guild transfer <player> <amount>` | `guild.transfer` | Transfer to player |
| `/guild logs` | `guild.use` | View guild operation logs |
| `/guild placeholder <player\|guild\|rank>` | `guild.use` | Get placeholders |
| `/guild time` | `guild.use` | View guild age |
| `/guild help` | `guild.use` | Show help |

#### `/guild relation` — Relations

| Command | Permission | Description |
|:-------:|:----------:|:-----------:|
| `/guild relation list` | `guild.relation` | List all relations |
| `/guild relation create <guild> [type]` | `guild.relation` | Create relation (default: ally) |
| `/guild relation accept <guild>` | `guild.relation` | Accept request |
| `/guild relation reject <guild>` | `guild.relation` | Reject request |
| `/guild relation delete <guild>` | `guild.relation` | Delete relation |

Relation types: `neutral`, `ally`, `enemy`, `war`, `truce`

#### `/guild economy` — Economy

| Command | Permission | Description |
|:-------:|:----------:|:-----------:|
| `/guild economy info` | `guild.economy` | View economy info |
| `/guild economy deposit <amount>` | `guild.economy` | Deposit funds |
| `/guild economy withdraw <amount>` | `guild.economy` | Withdraw funds |
| `/guild economy transfer <guild> <amount>` | `guild.economy` | Transfer to another guild |

### Admin Commands (`/guildadmin`)

| Command | Permission | Description |
|:-------:|:----------:|:-----------:|
| `/guildadmin` | `guild.admin` | Admin panel |
| `/guildadmin reload` | `guild.admin` | Reload all configs |
| `/guildadmin list` | `guild.admin` | List all guilds |
| `/guildadmin info <guild>` | `guild.admin` | Guild details |
| `/guildadmin delete <guild>` | `guild.admin` | Force delete guild |
| `/guildadmin freeze <guild>` | `guild.admin` | Freeze guild |
| `/guildadmin unfreeze <guild>` | `guild.admin` | Unfreeze guild |
| `/guildadmin transfer <guild> <player>` | `guild.admin` | Transfer leadership |
| `/guildadmin economy <guild> <set\|add\|remove> <amount>` | `guild.admin` | Manage guild economy |
| `/guildadmin update` | `guild.admin` | Check for updates |
| `/guildadmin update download` | `guild.admin.update` | Download & install update |
| `/guildadmin test <gui\|economy\|relation>` | `guild.admin` | Run admin tests |
| `/guildadmin help` | `guild.admin` | Show help |

#### `/guildadmin relation`

| Command | Permission | Description |
|:-------:|:----------:|:-----------:|
| `/guildadmin relation gui` | `guild.admin` | Open relation management GUI |
| `/guildadmin relation list` | `guild.admin` | List all relations |
| `/guildadmin relation create <g1> <g2> <type>` | `guild.admin` | Create relation |
| `/guildadmin relation delete <g1> <g2>` | `guild.admin` | Delete relation |

### Module Management (`/guildmodule`)

| Command | Permission | Description |
|:-------:|:----------:|:-----------:|
| `/guildmodule list` | `guild.admin.module` | List loaded modules |
| `/guildmodule load <file.jar>` | `guild.admin.module` | Load a module |
| `/guildmodule unload <moduleId>` | `guild.admin.module` | Unload a module |
| `/guildmodule reload <moduleId>` | `guild.admin.module` | Reload a module |
| `/guildmodule info <moduleId>` | `guild.admin.module` | Module details |
| `/guildmodule cloud` | `guild.admin.module` | List cloud modules |
| `/guildmodule cloud download <moduleId>` | `guild.admin.module` | Download from cloud |

Aliases: `/g` = `/guild`, `/ga` = `/guildadmin`

### All Permission Nodes

| Permission | Default | Description |
|:----------:|:-------:|:-----------:|
| `guild.use` | true | Use guild system |
| `guild.create` | true | Create a guild |
| `guild.invite` | true | Invite players |
| `guild.kick` | true | Kick members |
| `guild.promote` | true | Promote members |
| `guild.demote` | true | Demote members |
| `guild.delete` | op | Delete guild |
| `guild.sethome` | true | Set guild home |
| `guild.home` | true | Teleport to guild home |
| `guild.relation` | true | Manage relations |
| `guild.economy` | true | Manage economy |
| `guild.deposit` | true | Deposit funds |
| `guild.withdraw` | true | Withdraw funds |
| `guild.transfer` | true | Transfer funds |
| `guild.admin` | op | Admin privileges |
| `guild.admin.module` | op | Module management |
| `guild.admin.update` | op | Download updates |

## Configuration

### config.yml (excerpt)

```yaml
database:
  type: sqlite     # sqlite or mysql
  mysql:
    host: localhost
    port: 3306
    database: guild
    username: root
    password: ""
    pool-size: 10

guild:
  min-name-length: 3
  max-name-length: 20
  max-tag-length: 6
  max-description-length: 100
  max-members: 50
  creation-cost: 1000.0
  max-level: 10
  levels:
    1: 5000.0
    2: 10000.0
    3: 20000.0
    4: 35000.0
    5: 50000.0
    6: 75000.0
    7: 100000.0
    8: 150000.0
    9: 200000.0

permissions:
  default:
    can-create: true
    can-invite: true
    can-kick: true
    can-promote: true
    can-demote: false
    can-delete: false
```

Run `/guildadmin reload` after editing for changes to take effect.

## Level Requirements

| Level | Cost | Max Members |
|:-----:|-----:|:-----------:|
| 1 | 0 | 6 |
| 2 | 5,000 | 12 |
| 3 | 10,000 | 18 |
| 4 | 20,000 | 24 |
| 5 | 35,000 | 30 |
| 6 | 50,000 | 40 |
| 7 | 75,000 | 50 |
| 8 | 100,000 | 60 |
| 9 | 150,000 | 80 |
| 10 | 200,000 | 100 |

Levels and costs are fully configurable in `config.yml`.

## PlaceholderAPI

See [PlaceholderAPI Reference](PLACEHOLDER_API.md) for the full variable list.

## Database

The plugin uses HikariCP connection pool with SQLite and MySQL support.

### Table Overview

| Table | Purpose |
|:-----:|:-------:|
| `guilds` | Guild master data (name, tag, description, leader, home, etc.) |
| `guild_members` | Guild members (UUID, role, join time) |
| `guild_applications` | Join applications |
| `guild_invites` | Invitation records (with expiration) |
| `guild_relations` | Guild relations (ally, enemy, war, truce) |
| `guild_economy` | Guild economy (balance, level, experience, max members) |
| `guild_contributions` | Member contribution records |
| `guild_logs` | Activity logs |
| `guild_currencies` | Virtual currencies (A/B/C coins, per member per guild) |
| `guild_member_investments` | Member investment tracking (deposits & withdrawals) |

## Building

Requirements: Java 17+, Maven 3.6+

```bash
git clone https://github.com/chenasyd/-GuildPlugin.git
cd GuildPlugin
mvn clean package -pl guild-plugin
```

Output: `guild-plugin/target/guild-plugin-{version}.jar`

To include example modules:

```bash
mvn clean package -pl guild-plugin -Pbuild-announcement-module
```

## Module SDK

See [SDK Developer Guide](SDK%20Developer-Guide.md) for module development documentation.

## Links

- GitHub: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- Issues: [Report bugs](https://github.com/chenasyd/-GuildPlugin/issues)
- Wiki: [Documentation](https://github.com/chenasyd/-GuildPlugin/wiki)

## License

GNU GPL v3.0
