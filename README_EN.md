# GuildPlugin - Feature-Complete Minecraft Guild System

GuildPlugin is a comprehensive and high-performance Minecraft server plugin that delivers a full-featured guild/faction system, including management, economy, relations, leveling, GUI, and more. Free and open-source!

## Notice for Use

When using this plugin, please follow the steps below to update your configuration:

1. Delete the `messages_*.yml` files from the old version of the plugin to allow the plugin to regenerate them.
2. If you prefer not to delete, you can manually replace the content of the corresponding `messages_*.yml` files with the latest configuration.
3. After completing the above steps, run the following command to apply the configuration: `/guildadmin reload`

> ⚠️ **Tip**: If you are using v1.3.9 or higher, please be sure to update the plugin configuration to the latest version; otherwise, the plugin will experience numerous display errors that affect the player experience.

## Core Features

### Guild Management
- Create and customize guilds (name, tag, description)
- Manage guild members (invite, kick, promote, demote)
- Role-based permission system (Leader, Officer, Member)
- Set and teleport to guild home
- Guild application system

### Economy System
- Guild fund management (deposit, withdraw, transfer)
- Guild creation fee configuration
- Economy system integration (supports multiple economy plugins via Vault)

### Relationship System
- Inter-guild relationship management (allied, hostile, neutral, at war, truce)
- Relationship status notifications
- War status alerts

### Leveling System
- Guild level progression
- Increased member capacity
- Unlock additional guild features

### User Interface
- Complete Graphical User Interface (GUI)
- Intuitive menu system
- Customizable interface configuration

## Technical Features

- **Asynchronous Processing**: All database operations are asynchronous, ensuring no impact on server performance
- **Multi-Database Support**: Supports both SQLite and MySQL
- **Placeholder Support**: Integrated with PlaceholderAPI
- **Permission Integration**: Fully compatible with Bukkit permission system
- **High Performance**: Optimized code ensures smooth server operation
- **Slot-based Detection**: GUI interactions use slot detection, ensuring buttons work in all languages
- **Comprehensive Logging**: Guild activity log system (guild_logs table)

## Commands

- `/guild` - Main guild command
- `/guildadmin` - Guild administration command

## Permission Nodes

- Uses built-in permission system

## Basic Guild Information Variables

### Guild Basic Info
- `%guild_name%` - Guild name
- `%guild_tag%` - Guild tag
- `%guild_membercount%` - Current member count
- `%guild_maxmembers%` - Maximum member capacity
- `%guild_level%` - Guild level
- `%guild_balance%` - Guild balance (2 decimal places)
- `%guild_frozen%` - Guild status (Normal/Frozen/No Guild)

### Player Guild Info
- `%guild_role%` - Player's guild role (Leader/Officer/Member)
- `%guild_joined%` - When player joined the guild
- `%guild_contribution%` - Player's contribution to the guild

## Guild Status Check Variables

### Player Status
- `%guild_hasguild%` - Whether player has a guild (Yes/No)
- `%guild_isleader%` - Whether player is leader (Yes/No)
- `%guild_isofficer%` - Whether player is officer (Yes/No)
- `%guild_ismember%` - Whether player is member (Yes/No)

## Guild Permission Check Variables

### Permission Status
- `%guild_caninvite%` - Can invite players (Yes/No)
- `%guild_cankick%` - Can kick members (Yes/No)
- `%guild_canpromote%` - Can promote members (Yes/No)
- `%guild_candemote%` - Can demote members (Yes/No)
- `%guild_cansethome%` - Can set guild home (Yes/No)
- `%guild_canmanageeconomy%` - Can manage guild economy (Yes/No)

## Requirements

- **Minecraft Server Version**: 1.21+
- **Java Version**: JDK 17+
- **Optional Dependencies**: Vault (for economy support), PlaceholderAPI (for placeholder support)

## Installation Steps

1. Place the plugin jar file in your server's `plugins` folder
2. Start the server - the plugin will automatically generate configuration files
3. Edit configuration files as needed:
   - `config.yml` - Main plugin configuration
   - `messages_zh.yml` / `messages_en.yml` / `messages_pl.yml` - Language files
   - `database.yml` - Database settings
4. Restart server to apply changes

## Supported Languages

- 🇨🇳 Chinese (Simplified) - `zh`
- 🇬🇧 English - `en`
- 🇵🇱 Polish - `pl`

## Commands Reference

### Player Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/guild` | `guild.use` | Open main menu |
| `/guild create <name> [tag] [desc]` | `guild.create` | Create a guild |
| `/guild info` | `guild.info` | View guild information |
| `/guild members` | `guild.members` | View guild members |
| `/guild invite <player>` | `guild.invite` | Invite player |
| `/guild kick <player>` | `guild.kick` | Kick member |
| `/guild leave` | `guild.leave` | Leave guild |
| `/guild delete` | `guild.delete` | Delete guild |
| `/guild promote <player>` | `guild.promote` | Promote member |
| `/guild demote <player>` | `guild.demote` | Demote member |
| `/guild accept <player>` | `guild.accept` | Accept invitation |
| `/guild decline <player>` | `guild.decline` | Decline invitation |
| `/guild sethome` | `guild.sethome` | Set guild home |
| `/guild home` | `guild.home` | Teleport to guild home |
| `/guild apply <guild> [message]` | `guild.apply` | Apply to join guild |

### Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/guildadmin` | `guild.admin` | Admin main command |
| `/guildadmin reload` | `guild.admin.reload` | Reload configuration |
| `/guildadmin list` | `guild.admin.list` | List all guilds |
| `/guildadmin info <guild>` | `guild.admin.info` | View guild details |
| `/guildadmin delete <guild>` | `guild.admin.delete` | Force delete guild |
| `/guildadmin kick <player> <guild>` | `guild.admin.kick` | Kick player from guild |
| `/guildadmin relation` | `guild.admin.relation` | Manage relations |
| `/guildadmin test` | `guild.admin.test` | Test features |

## Build Instructions

This project uses Maven multi-module structure. To build:

```bash
# Build all modules
mvn clean install

# Build only SDK (for external module developers)
mvn clean install -pl guild-sdk

# Build Plugin and package example modules
mvn clean package -pl guild-plugin -Pbuild-member-rank-module
```

**Build Output:**
- `guild-sdk/target/guild-sdk-{version}.jar` - SDK JAR for module development
- `guild-plugin/target/guild-plugin-{version}.jar` - Main plugin JAR (shaded with dependencies)
- `guild-plugin/target/modules/member-rank.jar` - Example module (with profile)

## Configuration Example

### config.yml

```yaml
# Database configuration
database:
  type: sqlite  # sqlite or mysql
  mysql:
    host: localhost
    port: 3306
    database: guild
    username: root
    password: ""
    pool-size: 10

# Guild configuration
guild:
  min-name-length: 3
  max-name-length: 20
  max-tag-length: 6
  max-description-length: 100
  max-members: 50
  creation-cost: 1000.0  # Guild creation cost

# Permission configuration
permissions:
  default:
    can-create: true
    can-invite: true
    can-kick: true
    can-promote: true
    can-demote: false
    can-delete: false
```

### database.yml

```yaml
# SQLite configuration
sqlite:
  file: "plugins/GuildPlugin/guild.db"

# MySQL configuration
mysql:
  host: localhost
  port: 3306
  database: guild
  username: root
  password: ""
  pool-size: 10
  min-idle: 5
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

## Database Schema

The plugin uses the following tables:
- `guilds` - Guild information
- `guild_members` - Member data
- `guild_applications` - Join applications
- `guild_relations` - Inter-guild relationships
- `guild_economy` - Financial transactions
- `guild_logs` - Activity logs

See `plugins/database.sql` for complete SQL schema.

## Changelog

### v1.3.9 (Latest) - SDK Deep Enhancement
- **SDK Coverage: 96%** (50+ public methods)
- **Phase A (v1.3.7)**: 7 improvements — member events, unified messaging, async IO, config-driven, GUI tools, HTTP GET, name-based query
- **Phase B (v1.3.8)**: 4 improvements — CustomGUI lifecycle (register/open/unregister), cross-GUI navigation
- **Phase C (v1.3.9)**: 5 improvements — delayed init, dynamic language, descriptor logging, EventBus, GUI nav stack
- **Core Fix**: ModuleContext.formatMessage() unified colorization
- **3 Example Modules Complete**: AnnouncementModule / MemberRankModule / GuildStatsModule

### v1.3.6 - GuildStatsModule Complete
- Third example module: statistics dashboard (activity/economy/ranking/web reporting)
- SDK coverage improved from 45% to 82%

### v1.3.5
- Improved the SDK module loader

### v1.3.4
- Added new API and module loader framework

### v1.3.3
- Fixed error handling for asynchronous threads when Folia shuts down

### v1.3.2
- Migrated gui.yml to messages.yml file
- Fully translated languages: Chinese, English, Polish

### v1.3.1
- Full multi-language support system (Chinese, English, Polish)
- GUI slot detection optimization, fixing button issues in non-Chinese languages
- Added guild_logs table for complete guild activity tracking
- Improved GUI multi-language support
- Added 78 new translation entries

### v1.2.3
- Initial functional release
- Complete logic processing
- Full plugin extension support
- Full GUI implementation
- Full Folia support
- Multi-database support
- Built-in permission system

### v1.0.0
- Initial release
- Complete guild management system
- Economy system integration
- Guild relation management
- Leveling system
- Full GUI interface
- Multi-database support
- Permission system
- PlaceholderAPI integration

## Planned Features

- [ ] Guild war system (partially implemented)
- [x] ~~Guild announcement system~~ ✅ (AnnouncementModule, v1.3.x)
- [x] ~~Guild leaderboard~~ ✅ (GuildStatsModule-GuildRankingGUI, v1.3.x)
- [ ] Guild quest/mission system **← Next: QuestModule**
- [ ] Guild event system
- [ ] Guild storage
- [x] ~~Plugin extension module SDK~~ ✅ (Full SDK + 3 example modules, 96% coverage, v1.3.9)
- [ ] Quick resource updates
- [ ] Quick error feedback
- [ ] Error reporting with code locations
- [ ] Detailed plugin logging
- [ ] Enhanced security features
- [ ] Complete plugin system logic

## Project Links

- **GitHub**: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- **Issues**: [Report bugs here](https://github.com/chenasyd/-GuildPlugin/issues)
- **Wiki**: [Documentation](https://github.com/chenasyd/-GuildPlugin/wiki)

## License

GuildPlugin is licensed under [GNU GPL v3.0](https://github.com/chenasyd/-GuildPlugin/blob/main/LICENSE). Free and open-source, contributions welcome!
