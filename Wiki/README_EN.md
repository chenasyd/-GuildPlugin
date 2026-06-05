# GuildPlugin — Quick Start

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

> When upgrading: delete old `messages_*.yml` files to avoid display errors.

## Commands

### Players

| Command | Description |
|:-------:|:-----------:|
| `/guild` | Main GUI |
| `/guild create <name>` | Create a guild |
| `/guild info` | Guild information |
| `/guild members` | Member list |
| `/guild invite <player>` | Invite a player |
| `/guild kick <player>` | Kick a member |
| `/guild leave` | Leave guild |
| `/guild delete` | Delete guild |
| `/guild promote <player>` | Promote member |
| `/guild demote <player>` | Demote member |
| `/guild sethome` | Set guild home |
| `/guild home` | Teleport to guild home |
| `/guild balance` | View balance |
| `/guild deposit <amount>` | Deposit funds |
| `/guild withdraw <amount>` | Withdraw funds |

### Admins

| Command | Description |
|:-------:|:-----------:|
| `/guildadmin` | Admin panel |
| `/guildadmin reload` | Reload configuration |
| `/guildadmin list` | List all guilds |
| `/guildadmin delete <guild>` | Force delete guild |

Aliases: `/g` = `/guild`, `/ga` = `/guildadmin`

## Economy Commands

| Command | Description |
|:-------:|:-----------:|
| `/guild deposit <amount>` | Deposit to guild |
| `/guild withdraw <amount>` | Withdraw from guild |
| `/guild transfer <guild> <amount>` | Transfer to another guild |
| `/guild balance` | View guild balance |

## Relations

| Command | Description |
|:-------:|:-----------:|
| `/guild relation create <guild> <type>` | Create relation |
| `/guild relation accept <guild>` | Accept relation |
| `/guild relation reject <guild>` | Reject relation |
| `/guild relation cancel <guild>` | Cancel relation |

Relation types: `neutral`, `ally`, `enemy`, `war`, `truce`

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

Levels and costs are configurable in `config.yml`.

## PlaceholderAPI

See [PlaceholderAPI Reference](PLACEHOLDER_API.md) for the full variable list.

## Building

```bash
git clone https://github.com/chenasyd/-GuildPlugin.git
cd GuildPlugin
mvn clean package -pl guild-plugin
```

## Links

- GitHub: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- Issues: [Report bugs](https://github.com/chenasyd/-GuildPlugin/issues)

## License

GNU GPL v3.0
