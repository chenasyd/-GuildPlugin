# 内置功能 vs 示例模块

生产环境请以**核心内置**为准；`modules/*.jar` 示例主要用于 SDK 学习，文案已标明 Demo。

## 对照表

| 能力 | 正式（核心） | 示例模块 | 说明 |
|------|--------------|----------|------|
| 成员贡献 / 活跃度排行 | `builtin-activity`（GuildInfo「成员贡献」） | — | 混合总分，见 [Guild-Activity](./Guild-Activity.md) |
| 成员详情贡献数字 | `MemberDetailsGUI` ← `ActivityScoreService` | — | 不再显示「待统计」 |
| 资金流水 / 存款榜 | `guild_contributions` + `GuildFundsGUI` | stats 经济面板（读核心流水） | 示例不另建正式账本 |
| A 币排行 / 在线发 A 币 | — | **member-rank（Demo）** | 测试币，按钮名含 Demo |
| B 币 / 旧 JSON 活跃追踪 | — | **guild-stats（Demo）** | 有核心活跃时**不再启动**本地 Tracker，改读 `getMemberActivityScores` |
| 公会总览报表 UI | — | guild-stats | 演示如何做 overview / ranking |

## API

```java
api.getMemberActivityScores(guildId)
  // → List<ActivityScoreData>  economy / activity / total / rank
```

## 建议加载策略

- 正式服：可只开核心；不必安装 member-rank / guild-stats
- 开发学习：用 Maven profile 构建示例 JAR，注意按钮文案与内置「成员贡献」区分

## 相关文档

- [Guild-Activity](./Guild-Activity.md)
- [SDK Developer Guide](./SDK%20Developer-Guide.md)
