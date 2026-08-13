# Builtin Activity / Member Contribution (Hybrid Score)

Production contribution and activity live in the **core plugin**, **not** the A/B-coin example modules.

## Entry points

- Guild Info GUI (`GuildInfoGUI`) module-slot button "Contribution"
- Member Details GUI → click contribution info to open the same ranking (returns to member details)

## Scoring

```text
economyPts  = max(0, Σ net(guild_contributions))   # WITHDRAW is negative
activityPts = 0–100 (see below)
totalScore  = economyPts + activityPts × k         # k = guild-activity.score-weight-activity (default 2.0)
```

### Activity points (max 100)

| Part | Formula | Cap |
|------|---------|-----|
| Online today | `min(40, onlineMinutesToday × 0.5)` | 40 |
| Active days this week | `min(30, activeDaysThisWeek × 5)` | 30 (≥ `daily-active-minutes` online counts as 1 day) |
| Recency | +15 if online; otherwise lastSeen decay | 15 |
| Logged in today | +15 | 15 |

Economy points come from existing deposit / war contribution rows (`guild_contributions`). A/B/C coins are never used.

## Config (`config.yml`)

```yaml
guild-activity:
  enabled: true
  register-info-button: true
  score-weight-activity: 2.0
  tick-interval-seconds: 60
  daily-active-minutes: 5
```

## Boundary vs example modules

| Capability | Production | Example (SDK demo only) |
|------------|------------|-------------------------|
| Contribution / activity ranking | Builtin `builtin-activity` | — |
| A-coin board / online A-coin awards | — | member-rank (Demo) |
| B-coin / legacy JSON activity | — | guild-stats (Demo; prefer core API) |

See [Builtin vs Example Modules](./Builtin-vs-Example-Modules_EN.md).
