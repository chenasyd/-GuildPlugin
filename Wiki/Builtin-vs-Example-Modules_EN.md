# Builtin vs Example Modules

Use **core builtins** in production. Example JARs under `modules/` are for SDK learning and are labeled Demo.

## Comparison

| Capability | Production (core) | Example module | Notes |
|------------|-------------------|----------------|-------|
| Member contribution / activity ranking | `builtin-activity` (GuildInfo button) | — | Hybrid score; see [Guild-Activity](./Guild-Activity_EN.md) |
| Member details contribution numbers | `MemberDetailsGUI` ← `ActivityScoreService` | — | No more “pending” placeholder |
| Bank ledger / deposit board | `guild_contributions` + `GuildFundsGUI` | stats economy panel (reads core) | Examples do not own the ledger |
| A-coin board / online A-coin awards | — | **member-rank (Demo)** | Test currency; button names say Demo |
| B-coin / legacy JSON activity tracker | — | **guild-stats (Demo)** | When core activity is on, **local Tracker stays off** and scores come from `getMemberActivityScores` |
| Guild overview report UI | — | guild-stats | Demo overview / ranking |

## API

```java
api.getMemberActivityScores(guildId)
  // → List<ActivityScoreData>  economy / activity / total / rank
```

## Loading tips

- Production: core only is enough; skip member-rank / guild-stats JARs
- Learning: build example modules via Maven profiles; keep Demo buttons distinct from builtin “Contribution”

## Related

- [Guild-Activity](./Guild-Activity_EN.md)
- [SDK Developer Guide](./SDK%20Developer-Guide.md)
