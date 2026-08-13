# 内置活跃度 / 成员贡献（混合总分）

正式贡献与活跃统计在**核心插件**内实现，**不是**示例模块的 A/B 币。

## 入口

- 工会信息 GUI（`GuildInfoGUI`）模块槽按钮「成员贡献」
- 成员详情 GUI → 点击「贡献信息」打开同工会排行（返回成员详情）

## 计分

```text
economyPts  = max(0, Σ net(guild_contributions))   # WITHDRAW 为负
activityPts = 0~100（见下）
totalScore  = economyPts + activityPts × k         # k = guild-activity.score-weight-activity，默认 2.0
```

### 活跃分分量（满分 100）

| 分量 | 公式 | 上限 |
|------|------|------|
| 今日在线 | `min(40, onlineMinutesToday × 0.5)` | 40 |
| 本周活跃天 | `min(30, activeDaysThisWeek × 5)` | 30（当日在线 ≥ `daily-active-minutes` 计 1 天） |
| 近期在线 | 在线 +15；否则按 lastSeen 衰减 | 15 |
| 今日登录 | 当日有登录 +15 | 15 |

经济分来自现有存款 / 战争贡献等流水（`guild_contributions`），不读写 A/B/C 币。

## 配置（`config.yml`）

```yaml
guild-activity:
  enabled: true
  register-info-button: true
  score-weight-activity: 2.0
  tick-interval-seconds: 60
  daily-active-minutes: 5
```

## 多语言

独立模块语言包：`lang/modules/builtin-activity/{lang}.yml`（键前缀 `module.activity.*`）。  
由 `modules.yml` → `language.default` 控制默认语言，与 `lang/gui/` 核心 GUI 文案分离。

## 与示例模块的边界

| 能力 | 正式 | 示例（仅演示 SDK） |
|------|------|-------------------|
| 成员贡献 / 活跃榜 | 内置 `builtin-activity` | — |
| A 币排行 / 在线发 A 币 | — | member-rank（Demo） |
| B 币 / 旧 JSON 活跃 | — | guild-stats（Demo，请改用核心 API） |

详见 [Builtin vs Example Modules](./Builtin-vs-Example-Modules.md)。
