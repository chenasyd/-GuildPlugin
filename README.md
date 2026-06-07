<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21+-green?style=for-the-badge&logo=minecraft" alt="Minecraft 1.21+"/>
  <img src="https://img.shields.io/badge/API-Spigot%20%7C%20Folia-orange?style=for-the-badge" alt="Spigot | Folia"/>
  <img src="https://img.shields.io/badge/Version-1.5.6-blue?style=for-the-badge" alt="Version 1.5.6"/>
  <img src="https://img.shields.io/badge/License-GPL%20v3-red?style=for-the-badge" alt="GPL v3"/>
  <img src="https://img.shields.io/badge/Java-17%2B-brightgreen?style=for-the-badge" alt="Java 17+"/>
</p>

# GuildPlugin

A feature-complete Minecraft guild/faction system with economy, relations, leveling, full GUI, and modular SDK support. Supports both Spigot and Folia — free and open-source.

> Documentation: [User Guide](./Wiki/README_CN.md) | [Quick Start](./Wiki/README_EN.md) | [SDK Developer Guide](./Wiki/SDK%20Developer-Guide.md)

## Features

- **Guild Management** — create, disband, member management, role-based permission system
- **Economy System** — fund management, deposit/withdraw, Vault integration
- **Relationship System** — ally, hostile, war, truce between guilds
- **Level System** — guild growth with increasing max member caps
- **Full GUI** — intuitive graphical interface for all operations
- **Multi-language** — Chinese (简体中文), English, Polish, Brazilian Portuguese (Português)
- **Async Database** — HikariCP connection pool, MySQL/SQLite support
- **Modular SDK** — external module development with full API coverage (4 example modules)
- **CustomGUI System** — modules can independently register/open/unregister custom GUIs
- **EventBus** — loose-coupled inter-module communication
- **ServiceContainer** — modules access core system services via DI
- **Hot-load Modules** — add/remove modules at runtime via `/guildmodule`, no server restart needed

## Compatibility

| Software | Version |
|:--------:|:-------:|
| [Spigot](https://www.spigotmc.org) | 1.21+ |
| [PaperMC](https://papermc.io/downloads/paper) | 1.21+ |
| [Purpur](https://purpurmc.org) | 1.21+ |
| [Folia](https://papermc.io/software/folia) | 1.21+ |

## Integrations

| Plugin | Type |
|:------:|:----:|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Economy |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Placeholders |

## Installation

1. Download the latest release from [Releases](https://github.com/chenasyd/-GuildPlugin/releases)
2. Place `guild-plugin-1.5.3.jar` in your server's `plugins/` folder
3. Restart the server
4. Configure `plugins/GuildPlugin/config.yml` to your needs
5. Run `/guildadmin reload` to apply configuration changes

> ⚠️ **When upgrading the plugin**: Always back up your configuration and data first. It is recommended to delete `messages_*.yml` files and let the plugin regenerate them — this ensures all new messages are included and avoids display errors.

## Building from source

**Requirements:** Java 17+, Maven 3.8+

```bash
git clone https://github.com/chenasyd/-GuildPlugin.git
cd -GuildPlugin
mvn clean package -pl guild-plugin
```

The output JAR will be at `guild-plugin/target/guild-plugin-1.5.3.jar`.

To include example modules:

```bash
mvn clean package -pl guild-plugin -Pbuild-announcement-module
```

## Commands

> Aliases: `/g` → `/guild`, `/ga` → `/guildadmin`.

### Player Commands (`/guild`)

| Command | Permission | Description |
|:-------:|:----------:|:-----------:|
| `/guild` | `guild.use` | Open main GUI |
| `/guild create <name>` | `guild.create` | Create a guild |
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
| `/guild delete confirm` | `guild.delete` | Confirm guild deletion |
| `/guild delete cancel` | `guild.delete` | Cancel guild deletion |
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
| `guild.admin.update` | op | Download & install updates |

## Links

- **GitHub**: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- **Issues**: [Report a bug](https://github.com/chenasyd/-GuildPlugin/issues)
- **Wiki**: [Documentation](https://github.com/chenasyd/-GuildPlugin/wiki)

## bStats

[![bStats](https://bstats.org/signatures/bukkit/Guild%20Plugin.svg)](https://bstats.org/plugin/bukkit/Guild%20Plugin/31803)

## License

This project is licensed under the [GNU GPL v3.0](LICENSE).
