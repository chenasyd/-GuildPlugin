# Platform Compatibility Matrix

GuildPlugin is **built and shipped against Spigot API (Bukkit)** so it runs on hybrid cores that expose only the Bukkit API. Paper/Folia-specific classes must **not** appear as imports in `src/main/java`; use `Class.forName` reflection and degrade gracefully when unavailable.

Quick start: [README_EN](./README_EN.md).

## Core constraints

| Constraint | Detail |
|------------|--------|
| Compile API | `spigot-api` (`provided`) |
| Forbidden | `paper-api` in compile / runtime / provided (Maven Enforcer + `SpigotApiComplianceTest`) |
| Paper/Folia features | Reflection probes (`ServerUtils`, `FoliaWorldCreator`); disabled if probe fails |
| Java | 17+ |
| MC version | 1.20.1+ (`plugin.yml` `api-version: '1.20'`) |

## Server software

| Platform | Core guild features | `/guildworld` NMS | Scheduling |
|----------|---------------------|-------------------|------------|
| **Spigot** | Full | Not available (non-Folia) | Bukkit scheduler |
| **Paper / Purpur** | Full (Bukkit-compatible) | Not available (non-Folia) | Bukkit + in-plugin Folia-safe wrappers |
| **Folia** | Full | See version whitelist below | `CompatibleScheduler` |
| **Hybrid cores (Bukkit API only)** | **Primary target** | Usually unavailable | Depends on core |

> Paper/Purpur need no extra setup; the plugin does not hard-require Paper NMS.

## Folia and `/guildworld`

`ServerUtils.FOLIA_SUPPORTED_VERSIONS` lists Folia versions with an aligned NMS bridge:

`1.19.4`, `1.20.4`, `1.20.6`, `1.21.1`–`1.21.8`, `1.21.11`, `26.1.x`

**1.20.1 Folia is intentionally excluded** (no aligned multi-world bridge). Other guild features still work; `/guildworld` stays disabled.

Detection:

1. `Class.forName("io.papermc.paper.threadedregions.RegionizedServer")` → Folia
2. `ServerUtils.isFoliaVersionSupported()` → enables `/guildworld` NMS path

## Optional soft dependencies

| Plugin | Purpose | If missing |
|--------|---------|------------|
| Vault | Economy | No-economy mode available |
| PlaceholderAPI | Placeholders | Placeholders unavailable |
| NBTAPI | Warehouse item serialization | Warehouse disabled |
| ImagoCore | GUI image mode | Standard item GUI |
| Geyser-Spigot | Bedrock forms | Java GUI still works |

## Cross-server (Bungee / Waterfall)

The `guild-bungee` module provides messaging scaffolding; cross-server war is a **P3 proxy skeleton** only — see [CrossServer-War](./CrossServer-War.md).

## CI quality gates

Workflow [`.github/workflows/maven-publish.yml`](../.github/workflows/maven-publish.yml) on push/PR:

1. `mvn verify` (Enforcer on `validate`, 77+ unit tests on `test`)
2. Example module profiles package with `-DskipTests`

Local equivalent:

```bash
mvn verify -pl guild-sdk,guild-comm,guild-plugin,guild-bungee -am
```

## Developer notes

- **Permissions**: use `GuildMembershipRules` for member actions; Bukkit nodes (`guild.admin`, etc.) stay separate from the config role matrix.
- **Tests**: command/GUI permissions via Mockito; MockBukkit bootstrap smoke only — no `paper-api` in main artifacts.
- **New Paper APIs**: prefer reflection; compile-time Paper dependencies require an explicit compatibility review and doc update.

中文版：[Platform-Compatibility_CN.md](./Platform-Compatibility_CN.md)
