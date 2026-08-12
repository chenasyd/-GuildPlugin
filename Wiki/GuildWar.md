# GuildWar — 固定地图工会战

> 语言: **中文** | [English](./GuildWar_EN.md)

小型团队 PVP：发起 → 接受 → 报名 → 进场倒计时 → 激战 → 结算 → 回收战场。  
**不涉及领地/Claim**；与外交 `RelationType.WAR` **不挂钩**。

实战跑通：[工会战实战全流程引导](./GuildWar-Walkthrough.md)（[EN](./GuildWar-Walkthrough_EN.md)）。

## 前置

- 权限：`guild.war`（玩家）、`guild.war.admin`（强制结束）
- 命令：`/guildwar`（别名 `gwar` / `工会战`）
- 依赖：`GuildWorld` 可用，且存在带 schematic 的预设
- 发起/接受/取消/ready：会长或官员

## 配置（`config.yml` → `guild-war`）

| 项 | 默认 | 说明 |
|----|------|------|
| `enabled` | true | 总开关 |
| `default-preset` | `""` | 默认预设；空则 challenge 必须 `--preset` |
| `max-per-team` | 5 | 每队报名上限 |
| `signup-seconds` | 60 | 报名窗口 |
| `countdown-seconds` | 10 | 场内开战倒计时 |
| `challenge-timeout-seconds` | 120 | 挑战超时 |
| `default-mode` | first | `first` / `timed` / `survive` |
| `score-to-win` | 20 | 积分先到 |
| `timed-duration-seconds` | 600 | 限时积分时长 |
| `survive-duration-seconds` | 600 | 存活模式时限（兜底） |
| `friendly-fire` | false | 友伤 |
| `keep-inventory` | true | 本场死亡不掉落 |
| `max-concurrent` | 3 | 同时对局上限 |
| `eliminate-to-spectator` | true | 淘汰进旁观；false 则送回 fallback |
| `arena-protect` | true | PREPARING/COUNTDOWN/ACTIVE 禁破坏/放置/桶 |
| `broadcast-report` | true | 结束后广播简短战报行 |
| `season.id` | `"default"` | 当前赛季 ID（写入 `war_season_stats`） |
| `rewards.enabled` | false | 内置奖励总开关（事件始终触发） |
| `rewards.winner-vault` | 0 | 胜方每人 Vault 金额（需 Vault） |
| `rewards.loser-vault` | 0 | 败方/平局每人 Vault 金额 |
| `rewards.winner-guild-bank` | 0 | 胜方公会金库加款 |
| `rewards.contribution-win` | 10 | 胜方每人贡献点数（`WAR_WIN`） |
| `rewards.contribution-per-kill` | 1 | 每人每击杀贡献点数（`WAR_KILL`） |

战后：先 TP 回 `world.safety.fallback-world`，再按 `world.arena.post-match` 删除战场世界。  
战报落库到 `war_matches` / `war_match_players`；赛季统计见 `/guildwar season`。

## 流程

```text
A官员: /guildwar challenge <工会名|标签> [--preset] [--mode first|timed|survive] [--max] [--score] [--time]
B官员: /guildwar accept
双方成员: /guildwar join
（超时或双方官员 /guildwar ready）→ 创建 BATTLE 世界并粘贴 → TP 到 A/B 出生点
场内倒计时 → ACTIVE
结束 → 回城 → 删世界
```

## 胜负模式

| 模式 | 说明 |
|------|------|
| `first` | 击杀计分，先到 N 分；**无时限** |
| `timed` | 击杀计分，到时分高者胜 |
| `survive` | 死亡淘汰（旁观/回城）；最后存活方胜；有时限兜底 |

积分模式死亡后重生到己方出生点；存活模式淘汰后不可再战。

## 子命令

| 命令 | 说明 |
|------|------|
| `challenge / accept / deny` | 发起与应答 |
| `join / leave` | 报名（仅 PENDING/SIGNUP） |
| `ready` | 双方官员就绪后提前开局 |
| `cancel` | 开战前取消 |
| `status` | 查看对局 |
| `report [id]` | 战报（省略 id 则取本人最近一场） |
| `season` | 本赛季排行 GUI / 列表 |
| `admin end <id>` | 强制结束 |

## 事件与奖励

- `WarMatchStartEvent` / `WarMatchEndEvent`（包 `com.guild.war.event`）
- 内置奖励：`guild-war.rewards.*`（默认关闭）
- 详见 [API-World-War.md](./API-World-War.md)

## 多语言

文案键：`lang/core/{zh,en}.yml` 中 `war.*`。

## API

见 [API-World-War.md](./API-World-War.md)（中文）/ [API-World-War_EN.md](./API-World-War_EN.md)（English）中的 `GuildWarAPI`。
