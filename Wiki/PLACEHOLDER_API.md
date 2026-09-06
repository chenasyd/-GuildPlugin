# PlaceholderAPI Variables

GuildPlugin provides full PlaceholderAPI support, allowing other plugins and chat formats to display dynamic guild-related information.

## Guild Info

| Placeholder | Description |
|:-----------:|:-----------:|
| `%guild_name%` | Guild name |
| `%guild_tag%` | Guild tag |
| `%guild_membercount%` | Current member count |
| `%guild_maxmembers%` | Maximum member capacity |
| `%guild_level%` | Guild level |
| `%guild_balance%` | Guild balance (2 decimal places) |
| `%guild_frozen%` | Guild status (Normal / Frozen / No Guild) |

## Player Info

| Placeholder | Description |
|:-----------:|:-----------:|
| `%guild_role%` | Player role (Leader / Officer / Member) |
| `%guild_joined%` | Join date |
| `%guild_contribution%` | Contribution value |
| `%guild_hasguild%` | Has a guild (Yes / No) |
| `%guild_isleader%` | Is leader (Yes / No) |
| `%guild_isofficer%` | Is officer (Yes / No) |
| `%guild_ismember%` | Is member (Yes / No) |

## Permission Checks

| Placeholder | Description |
|:-----------:|:-----------:|
| `%guild_caninvite%` | Can invite players (Yes / No) |
| `%guild_cankick%` | Can kick members (Yes / No) |
| `%guild_canpromote%` | Can promote members (Yes / No) |
| `%guild_candemote%` | Can demote members (Yes / No) |
| `%guild_cansethome%` | Can set guild home (Yes / No) |
| `%guild_canmanageeconomy%` | Can manage guild economy (Yes / No) |

## Guild Territory Module (`guild-territory` loaded)

Requires PlaceholderAPI + `plugins/GuildPlugin/modules/guild-territory.jar`.

| Placeholder | Description |
|:-----------:|:-----------:|
| `%guild_module_territory_has%` | Player's guild has a **local** territory in the current world (`True` / `False`) |
| `%guild_module_territory_has_any%` | Player's guild has any territory across all servers (`True` / `False`) |
| `%guild_module_territory_count%` | Total territory count for the guild (cross-server metadata) |
| `%guild_module_territory_count_local%` | Territory count on **this** backend server |
| `%guild_module_territory_world%` | World name of local territory in current world (empty if none) |
| `%guild_module_territory_region%` | WG region ID in current world (e.g. `guild_42`) |
| `%guild_module_territory_sync%` | Sync state: `materialized` / `pending` / `failed` / `none` |
| `%guild_module_territory_volume%` | Claim volume (blocks) in current world |
| `%guild_module_territory_server%` | This backend's persistent `server-id` |
| `%guild_module_territory_wg_ready%` | WorldGuard bridge operational (`True` / `False`) |
| `%guild_module_territory_inside%` | Player standing inside any guild WG territory |
| `%guild_module_territory_inside_own%` | Player standing inside **their guild's** territory |
