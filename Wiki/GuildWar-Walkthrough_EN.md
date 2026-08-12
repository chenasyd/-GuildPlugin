# Guild War — Operator Walkthrough

> Language: [中文](./GuildWar-Walkthrough.md) | **English**

Server-owner guide: **build a map** → **finish one match** → **report / season / optional rewards**.  
See also: [GuildWorld](./GuildWorld_EN.md), [GuildWar](./GuildWar_EN.md), [API](./API-World-War_EN.md).

> Cross-server war ([CrossServer-War](./CrossServer-War.md)) is a **proxy message skeleton only** — not production-wired. This page covers single-server `/guildwar` only.

---

## 0. Checklist

| Item | Requirement |
|------|-------------|
| Plugin | GuildPlugin loaded; no Folia “gworld unsupported” warning (or use Paper/Spigot) |
| Permissions | Admin: `guild.admin.world`; fight: `guild.war`; force-end: `guild.war.admin` |
| Guilds | At least **2 guilds**, officers online; ≥1 member per side ready to join |
| Config | Edit `plugins/GuildPlugin/config.yml`, then `/guildadmin reload` or restart |
| Economy (optional) | Vault + economy plugin if testing `rewards` payouts |

Suggested test config (`score-to-win` may be lower than the default `20`):

```yaml
guild-war:
  enabled: true
  default-preset: "demo_arena"
  max-per-team: 5
  signup-seconds: 60
  countdown-seconds: 10
  challenge-timeout-seconds: 120
  default-mode: first
  score-to-win: 5
  timed-duration-seconds: 600
  survive-duration-seconds: 600
  friendly-fire: false
  keep-inventory: true
  max-concurrent: 3
  eliminate-to-spectator: true
  arena-protect: true
  broadcast-report: true
  season:
    id: "default"
  rewards:
    enabled: false
    winner-vault: 0
    loser-vault: 0
    winner-guild-bank: 0
    contribution-win: 10
    contribution-per-kill: 1

world:
  safety:
    fallback-world: "world"
  arena:
    post-match: destroy
```

---

## 1. Build a preset (admin, ~10–30 min)

Goal: `plugins/GuildPlugin/worlds/presets/demo_arena.gws` + `demo_arena.yml`.

```text
/guildworld edit create demo_build
# build the arena on the void platform
/guildworld edit wand          # left = Pos1, right = Pos2
/guildworld edit setspawn a
/guildworld edit setspawn b
/guildworld edit setspawn spectator   # optional
/guildworld edit save demo_arena
/guildworld preset info demo_arena
```

Optional paste test:

```text
/guildworld create test_paste --preset demo_arena
/guildworld tp test_paste
/guildworld delete test_paste --force
/guildworld edit leave
```

---

## 2. Before the fight

| Role | Action |
|------|--------|
| Guild A officer | `challenge` |
| Guild B officer | `accept` / `deny` |
| Members who should fight | `join` (others **will not** enter) |
| Both officers | `ready` to skip signup wait (both ready + ≥1 per side) |

```text
/guildwar challenge <guildB> --preset demo_arena --mode first --max 5 --score 5
/guildwar accept
/guildwar join
/guildwar status
/guildwar ready          # optional
/guildwar cancel         # officers, before fight starts
```

Flags: `--preset`, `--mode first|timed|survive`, `--max`, `--score`, `--time`.

---

## 3. Arena & combat

Auto flow: create BATTLE world → paste preset → TP to A/B → countdown → ACTIVE.

### Arena protect (`arena-protect`)

Default on. During PREPARING / COUNTDOWN / ACTIVE: non-privileged players cannot break/place/use buckets. Bypass: `guild.admin` / `guild.admin.world`.

### Modes

- **first / timed**: kill scoring; respawn at team spawn; `first` has no time limit  
- **survive**: death = eliminate (spectator or fallback); wipe or timeout fallback  

Force end: `/guildwar admin end <id>` (id from `status`).

---

## 4. After the match

1. Broadcast winner / score  
2. If `broadcast-report: true`, short report lines  
3. Fire `WarMatchEndEvent` (see [API](./API-World-War_EN.md))  
4. Persist `war_matches` / `war_match_players` + season stats  
5. TP to `fallback-world` → destroy arena instance  
6. Preset `demo_arena` **kept** for the next match  

```text
/guildwar report          # your latest match
/guildwar report <id>
/guildwar season          # GUI (or list in console)
```

Optional rewards (`rewards.enabled: false` by default — events/reports still work):

```yaml
guild-war:
  rewards:
    enabled: true
    winner-vault: 100
    loser-vault: 0
    winner-guild-bank: 50
    contribution-win: 10
    contribution-per-kill: 1
```

Then `/guildadmin reload` and play another match. Custom loot: listen to `WarMatchEndEvent`.

Placeholders (PlaceholderAPI): `%guild_war_wins%`, `%guild_war_losses%`, `%guild_war_kills%`, `%guild_war_matches%`, `%guild_war_season%`.

---

## 5. Minimal 2-player script

1. Finish §1; set `default-preset: demo_arena`, `score-to-win: 3`, `rewards.enabled: false`  
2. Player A (guild A officer), Player B (guild B officer) online  
3. A: `/guildwar challenge <guildB> --mode first --max 1 --score 3`  
4. B: `/guildwar accept`  
5. Both: `/guildwar join` then `/guildwar ready`  
6. Fight until one side reaches 3 kills  
7. Confirm return + arena gone: `/guildworld list`  
8. `/guildwar report` then `/guildwar season` (winner wins +1)  

Optional: enable rewards with `winner-vault: 100` and re-test.

---

## 6. Common failures

| Symptom | Likely cause |
|---------|----------------|
| No preset | Missing `default-preset` and no `--preset` |
| Preset missing schematic | `save` failed or name mismatch |
| Officer-only errors | Members cannot challenge/accept |
| No signup match | Not accepted yet, or already fighting |
| Team full | Over `--max` / `max-per-team` |
| Folia cannot create worlds | Version not in supported list |
| No report found | No finished match yet, or wrong id |
| Empty season board | No ended matches for this `season.id` |
| Rewards on but no money | `enabled` false, amount 0, no Vault, or player offline |
| Can break blocks in arena | `arena-protect: false` or admin bypass |

---

## 7. Links

| Doc / path | Content |
|------------|---------|
| `plugins/GuildPlugin/config.yml` | `world` / `guild-war` |
| `plugins/GuildPlugin/worlds/presets/` | `.gws` + `.yml` |
| [GuildWorld_EN.md](./GuildWorld_EN.md) | Worlds |
| [GuildWar_EN.md](./GuildWar_EN.md) | War config & commands |
| [API-World-War_EN.md](./API-World-War_EN.md) | API / events |
| [CrossServer-War.md](./CrossServer-War.md) | Cross-server skeleton |

---

## 8. Acceptance checklist

| Item | Expected | OK |
|------|----------|----|
| Finish a match | Broadcast; return to fallback; arena world gone | ☐ |
| `broadcast-report` | Short report lines after end | ☐ |
| `/guildwar report` | Latest match / by id | ☐ |
| `/guildwar season` | GUI or list; winner wins +1 | ☐ |
| `rewards.enabled: false` | Events/reports, no Vault payout | ☐ |
| `rewards.enabled: true` + Vault | Winner money/contribution (amount > 0) | ☐ |
| `arena-protect` | Non-admin cannot break/bucket in ACTIVE | ☐ |
