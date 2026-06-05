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

> ⚠️ When upgrading from an older version (v1.3.9+), delete `messages_*.yml` files and let the plugin regenerate them to avoid display errors.

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

| Command | Description | Permission |
|:-------:|:-----------:|:----------:|
| `/guild` | Main guild command & GUI | `guild.use` |
| `/guild create <name>` | Create a guild | `guild.use` |
| `/guild info [guild]` | View guild info | `guild.use` |
| `/guild invite <player>` | Invite a player | `guild.use` |
| `/guild members` | List guild members | `guild.use` |
| `/guild economy` | Manage guild funds | `guild.use` |
| `/guild relation` | Manage guild relations | `guild.use` |
| `/guildadmin` | Admin commands & GUI | `guild.admin` |
| `/guildmodule` | Hot-load/unload modules | `guild.admin.module` |

> Aliases: `/g` → `/guild`, `/ga` → `/guildadmin`. See full documentation for all subcommands.

## Links

- **GitHub**: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- **Issues**: [Report a bug](https://github.com/chenasyd/-GuildPlugin/issues)
- **Wiki**: [Documentation](https://github.com/chenasyd/-GuildPlugin/wiki)

## bStats

[![bStats](https://bstats.org/signatures/bukkit/Guild%20Plugin.svg)](https://bstats.org/plugin/bukkit/Guild%20Plugin/31803)

## License

This project is licensed under the [GNU GPL v3.0](LICENSE).
