# GuildWorld — Void Worlds & Presets

> Language: [中文](./GuildWorld.md) | **English**

Admins build guild-war maps in void worlds, export presets (`.gws` + `.yml`), and spawn battle instances from those presets.

## Prerequisites

- Permission: `guild.admin.world`
- Command: `/guildworld` (aliases: `gworld` / `工会世界`)
- Folia: only versions listed in `ServerUtils.FOLIA_SUPPORTED_VERSIONS`; otherwise create/load/unload/delete are disabled
- Paper/Spigot: always available

## Config (`config.yml` → `world`)

| Key | Description |
|-----|-------------|
| `name-prefix` | Managed world name prefix (default `gw_`) |
| `safety.fallback-world` | Fallback world on unload / after a match |
| `edit.wand-material` | Selection wand (default `WOODEN_AXE`) |
| `schematic.max-volume` | Max selection volume |
| `schematic.ignore-air` | Skip air when pasting |
| `schematic.include-block-entities` | Export tile entities (falls back to blocks-only on failure) |
| `arena.post-match` | Post-match policy: `destroy` (default) \| `reset` (currently same as destroy) |

## Map authoring flow

```text
/guildworld edit create <name>
/guildworld edit wand          # left-click Pos1 / right-click Pos2
# build the map…
/guildworld edit setspawn a|b|spectator
/guildworld edit save <preset>
```

You can also run `/guildworld create <arena> --preset <preset>` to create a BATTLE world and paste in one step.

## Subcommands

| Command | Purpose |
|---------|---------|
| `create / list / info / load / unload / delete` | World lifecycle |
| `tp <world>` | Folia-safe teleport |
| `restore …` | Crash / stale recovery |
| `edit …` | Edit mode & selection export |
| `preset list\|info\|paste\|delete\|bind` | Preset management |

## Preset format

- Directory: `plugins/GuildPlugin/worlds/presets/`
- `<name>.gws`: GZIP+JSON schematic (custom format; no WE/FAWE dependency)
- `<name>.yml`: metadata, size, anchors (A/B/spectator offsets relative to paste origin)

## i18n

Message keys: `world.*` in `lang/core/{zh,en}.yml`.

## API

See [API-World-War_EN.md](./API-World-War_EN.md) (`GuildWorldAPI`).
