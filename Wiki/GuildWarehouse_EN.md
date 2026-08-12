# Guild Warehouse

Standalone guild warehouse powered by **NBTAPI** (softdepend): standard chest GUI, save on close. Capacity follows the guild’s **historical peak level**; downgrades do not shrink slots.

## Dependency

- Install [Item-NBT-API](https://www.spigotmc.org/resources/item-entity-tile-nbt-api.7939/) (plugin name `NBTAPI`)
- Without it, warehouse is disabled and players are notified

## Commands

| Command | Description |
|---------|-------------|
| `/guild warehouse` (`/g warehouse`, alias `wh`) | Open page 1 |
| `/guild warehouse <page>` | Open a specific page (1-based) |
| `/guild warehouse info` | Show peak level, slots, page count, officer/member access |
| `/guild warehouse perm <officer\|member> <on\|off>` | Leader (or `guild.admin`) toggles role open access |

**Not** exposed via the main GUI / Settings screens (command only).

## Capacity & pages

Config `guild-warehouse.slots-by-level` maps `peak_level` → slot count (**multiples of 9**, no hard cap).

- Each page holds up to **54** slots; larger totals split across pages
- Example: `63` → page 1 (54) + page 2 (9); open page 2 with `/guild warehouse 2`
- DB slots are absolute 0-based indices (page 2 local slot 0 = absolute 54)
- Closing a page saves only that page’s slot range

`peak_level` updates on create/upgrade and **never decreases** on downgrade.

## Permissions

- **Leader**: always may open; cannot disable self via command
- **Officer / member**: per-guild override first, else `permissions.officer|member.can-warehouse`
- Suggested defaults: leader=true, officer=true, member=false

## Concurrency

Only one open session per guild at a time; others see “warehouse in use”.

## Config example

```yaml
guild-warehouse:
  enabled: true
  slots-by-level:
    1: 9
    2: 18
    3: 27
    4: 36
    5: 45
    6: 54
    7: 63   # two pages: 54 + 9
    8: 72
    9: 81
    10: 90

permissions:
  leader:
    can-warehouse: true
  officer:
    can-warehouse: true
  member:
    can-warehouse: false
```

## Out of scope (this phase)

- Main GUI entry
- Access audit log
- Hard dependency on NBTAPI
- Cross-server warehouse sync

中文: [GuildWarehouse.md](./GuildWarehouse.md)
