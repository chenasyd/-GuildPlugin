# 公会战实战全流程引导

> 语言: **中文** | [English](./GuildWar-Walkthrough_EN.md)

面向服主 / 管理员：从**做图**到**打完一场战**，再到**战报 / 赛季 /（可选）奖励**。  
参考：[GuildWorld](./GuildWorld.md)、[GuildWar](./GuildWar.md)、[API](./API-World-War.md)。

> 跨服战场见 [CrossServer-War](./CrossServer-War.md)：目前仅为代理消息骨架，**尚未接线生产开战**。本文只讲单服 `/guildwar`。

---

## 0. 准备检查清单

| 项 | 要求 |
|----|------|
| 插件 | GuildPlugin 已加载，无 Folia「版本不支持 gworld」警告（或 Paper/Spigot） |
| 权限 | 管理员：`guild.admin.world`；开战：`guild.war`；强制结束：`guild.war.admin` |
| 公会 | 至少 **2 个公会**，各有会长或官员在线；每边至少 1 名成员准备参赛 |
| 配置 | 打开 `plugins/GuildPlugin/config.yml`，改完执行 `/guildadmin reload` 或重启 |
| 经济（可选） | 若测 `rewards` Vault 发奖，需已装 Vault + 经济插件 |

建议测试配置（`score-to-win` 可小于默认 20，方便快速分胜负）：

```yaml
guild-war:
  enabled: true
  default-preset: "demo_arena"   # 与 §1 保存的预设名一致；也可开战时 --preset
  max-per-team: 5
  signup-seconds: 60
  countdown-seconds: 10
  challenge-timeout-seconds: 120
  default-mode: first
  score-to-win: 5                # 测试可改小；正式服常用默认 20
  timed-duration-seconds: 600
  survive-duration-seconds: 600
  friendly-fire: false
  keep-inventory: true
  max-concurrent: 3
  eliminate-to-spectator: true
  arena-protect: true            # 战场内禁破坏/放置/桶
  broadcast-report: true         # 结束后广播简短战报
  season:
    id: "default"
  rewards:
    enabled: false               # 先关着测流程；要测发奖再开并设金额 > 0
    winner-vault: 0
    loser-vault: 0
    winner-guild-bank: 0
    contribution-win: 10
    contribution-per-kill: 1

world:
  safety:
    fallback-world: "world"      # 必须是已存在的主世界名
  arena:
    post-match: destroy
```

---

## 1. 制作战场预设（管理员，约 10～30 分钟）

目标：得到 `plugins/GuildPlugin/worlds/presets/demo_arena.gws` + `demo_arena.yml`。

### 1.1 创建编辑世界并进入

```text
/guildworld edit create demo_build
```

成功后会自动传送进虚空编辑世界（名类似 `gw_demo_build`）。

### 1.2 建造地图

在脚下会有落地平台。按需搭 PVP 场地（平台、掩体、高低差均可）。  
规模：先做小图验证流程；正式图再放大。

### 1.3 开选区斧，框住整张图

```text
/guildworld edit wand
```

- **左键**方块 = Pos1  
- **右键**方块 = Pos2  
- 聊天会提示选区体积；不要超过 `world.schematic.max-volume`

也可站在角点：

```text
/guildworld edit pos1
/guildworld edit pos2
```

### 1.4 设置出生点（重要）

站在队伍 A 出生位置并面向开战方向：

```text
/guildworld edit setspawn a
```

再站到队伍 B：

```text
/guildworld edit setspawn b
```

可选观众点：

```text
/guildworld edit setspawn spectator
```

> 未设 B 点时，双方可能都落在 A/粘贴原点附近，测试时务必设好 A/B。

### 1.5 导出预设

```text
/guildworld edit save demo_arena
```

成功提示含尺寸与方块实体数量。检查：

```text
/guildworld preset info demo_arena
/guildworld preset list
```

可选自测粘贴：

```text
/guildworld create test_paste --preset demo_arena
/guildworld tp test_paste
```

确认方块与 A/B 位置正确后，可删测试世界：

```text
/guildworld delete test_paste --force
```

离开编辑世界：

```text
/guildworld edit leave
```

---

## 2. 开战前（双方公会）

### 2.1 角色

| 角色 | 操作 |
|------|------|
| A 公会官员 | `challenge` |
| B 公会官员 | `accept` / `deny` |
| 双方想上场的成员 | `join`（未 join 的人**不会**进场） |
| 双方官员 | 可 `ready` 跳过报名倒计时（两边都 ready 且各有 ≥1 人） |

### 2.2 发起挑战（A 官员）

```text
/guildwar challenge <B公会名或标签> --preset demo_arena --mode first --max 5 --score 5
```

常用参数：

| 参数 | 含义 |
|------|------|
| `--preset` | 预设名（若配置了 `default-preset` 可省略） |
| `--mode first` | 击杀先到 N 分 |
| `--mode timed` | 限时积分 |
| `--mode survive` | 死亡淘汰 |
| `--max` | 每队人数上限 |
| `--score` | 先到分数（first） |
| `--time` | 秒（timed / survive） |

对方公会会收到提示。超时未 accept 会自动取消（`challenge-timeout-seconds`）。

### 2.3 接受（B 官员）

```text
/guildwar accept
```

拒绝：

```text
/guildwar deny
```

### 2.4 报名（双方成员）

```text
/guildwar join
```

退出报名（仅报名阶段）：

```text
/guildwar leave
```

查看状态：

```text
/guildwar status
```

### 2.5 提前开局（可选）

两边都至少 1 人已 join 后，双方官员各执行一次：

```text
/guildwar ready
```

否则等 `signup-seconds` 结束；若某边 0 人则对局取消。

开战前取消（官员）：

```text
/guildwar cancel
```

---

## 3. 进场与战斗

系统会自动：

1. 创建 BATTLE 虚空世界并粘贴 `demo_arena`  
2. 将已报名玩家 TP 到 A / B 出生点  
3. 场内倒计时（`countdown-seconds`），期间**不能互相伤害**  
4. 提示「开战」后进入 ACTIVE  

### 3.1 战场保护（`arena-protect`）

默认开启。在 `PREPARING` / `COUNTDOWN` / `ACTIVE` 阶段：

- 非特权玩家无法破坏、放置、倒桶/装桶  
- `guild.admin` / `guild.admin.world` 可绕过  

与外交关系 `RelationType.WAR`、公会家保护（`guild.home-protect`）无关。

### 3.2 积分模式（`first` / `timed`）

- 击杀加分；友伤默认关  
- 死亡后重生回己方出生点（本场可 `keep-inventory`）  
- `first`：先到 `score-to-win` 即胜，无时限  
- `timed`：时间到比分高者胜  

### 3.3 存活模式（`survive`）

- 死亡即淘汰 → 旁观（或回城，看 `eliminate-to-spectator`）  
- 一方全灭即胜；超时按存活人数 / 击杀兜底  

### 3.4 异常处理

| 情况 | 处理 |
|------|------|
| 卡死 / 需终止 | 管理员：`/guildwar admin end <id>`（id 见 `status`） |
| 创建战场失败 | 看控制台；检查预设是否有 `.gws`、世界系统是否启用 |
| 人掉虚空 | 检查 setspawn / 粘贴高度；编辑时平台是否够大 |

---

## 4. 战后结算（重点）

结束后系统按顺序：

1. **广播**胜负与比分  
2. 若 `broadcast-report: true`，再广播简短**战报行**（含击杀明细）  
3. 触发 `WarMatchEndEvent`（外部插件 / 内置奖励可监听；见 [API](./API-World-War.md)）  
4. 异步写入数据库 `war_matches` / `war_match_players`，并更新赛季 `war_season_stats`  
5. 参赛者 TP 回 `fallback-world`  
6. 删除本场实例世界（`post-match: destroy`）  
7. 预设 `demo_arena` **保留**，可继续开下一场  

### 4.1 查看战报

```text
/guildwar report          # 本人最近一场
/guildwar report <id>     # 按战报 ID（落库主键，广播里 #{id}）
```

### 4.2 赛季排行

```text
/guildwar season
```

玩家会打开赛季 GUI（控制台则打印列表）。胜负 / 击杀计入 `guild-war.season.id`（默认 `default`）。  
Placeholder（需 PlaceholderAPI）：`%guild_war_wins%`、`%guild_war_losses%`、`%guild_war_kills%`、`%guild_war_matches%`、`%guild_war_season%` 等。

### 4.3 可选：内置奖励

默认 `rewards.enabled: false`：仍有事件与战报，**不会**发 Vault/金库。

要测发奖时：

```yaml
guild-war:
  rewards:
    enabled: true
    winner-vault: 100      # 需 Vault；金额为 0 则跳过该项
    loser-vault: 0
    winner-guild-bank: 50
    contribution-win: 10
    contribution-per-kill: 1
```

然后 `/guildadmin reload`，再打一场。胜方在线玩家应收 Vault；贡献类型为 `WAR_WIN` / `WAR_KILL`。  
复杂任务 / 自定义掉落请监听 `WarMatchEndEvent`，不要依赖配置掉落表。

---

## 5. 推荐最小测试剧本（2 人）

1. 管理员做完 §1，配置 `default-preset: demo_arena`，`score-to-win: 3`，`rewards.enabled: false`  
2. 玩家甲（公会甲官员）、玩家乙（公会乙官员）在线  
3. 甲：`/guildwar challenge <乙公会> --mode first --max 1 --score 3`  
4. 乙：`/guildwar accept`  
5. 甲乙：`/guildwar join`  
6. 甲乙：`/guildwar ready`（或等报名结束）  
7. 进场倒计时后互殴，先杀满 3 次的一方胜  
8. 确认回城且战场世界消失：`/guildworld list`  
9. 甲或乙：`/guildwar report`（应看到刚打完的战报）  
10. 任一方：`/guildwar season`（胜方 wins 应 +1）  

可选第 11 步：打开 `rewards.enabled` 并设 `winner-vault: 100`，再打一场核对余额。

---

## 6. 常见失败原因

| 现象 | 可能原因 |
|------|----------|
| 「未指定预设」 | 未配 `default-preset` 且未加 `--preset` |
| 「预设不存在或缺少 schematic」 | 只写了 yml 没 `save` 成功，或名字不一致 |
| 「只有会长或官员可以…」 | 普通成员不能 challenge/accept |
| 「当前没有可报名的公会战」 | 还没 accept，或已开战 |
| 「本队报名已满」 | 超过 `--max` / `max-per-team` |
| Folia 无法建世界 | 版本不在支持列表，见启动日志 |
| 英文服看到中文 | 玩家语言未切 en；确认 `lang/core/en.yml` 已含 `war.*` |
| 「没有找到战报」 | 尚未打完一场，或查了错误 ID；先 `report` 无参试本人最近一场 |
| 赛季排行空白 | 本 `season.id` 下还没有结束过的对局 |
| 开了 rewards 但没钱 | `enabled` 仍为 false，或金额为 0，或未装 Vault / 玩家已离线 |
| 战场里能拆方块 | `arena-protect: false`，或持有 admin 绕过权限 |

---

## 7. 相关文档与路径

| 路径 / 文档 | 内容 |
|-------------|------|
| `plugins/GuildPlugin/config.yml` | `world` / `guild-war` |
| `plugins/GuildPlugin/worlds/presets/` | `.gws` + `.yml` |
| `plugins/GuildPlugin/lang/core/zh.yml` | `world.*` / `war.*` 文案 |
| [GuildWorld.md](./GuildWorld.md) | 世界管理 |
| [GuildWar.md](./GuildWar.md) | 公会战配置与命令 |
| [API-World-War.md](./API-World-War.md) | API / 事件 |
| [CrossServer-War.md](./CrossServer-War.md) | 跨服协议骨架（未接线生产） |

---

## 8. 验收勾选表

跑完一场后对照勾选：

| 项 | 预期 | 通过 |
|----|------|------|
| 打完一场 | 广播胜负；回 `fallback-world`；战场世界消失 | ☐ |
| `broadcast-report` | 结束后有简短战报行（含击杀） | ☐ |
| `/guildwar report` | 无 id 出本人最近一场；有 id 出对应战报 | ☐ |
| `/guildwar season` | 打开排行 GUI 或列表；胜方 wins+1 | ☐ |
| `rewards.enabled: false` | 仍有事件/战报，无 Vault 发奖 | ☐ |
| `rewards.enabled: true` + Vault | 胜方有金额/贡献（金额配置 > 0） | ☐ |
| `arena-protect` | ACTIVE 中非 admin 无法破坏/放桶 | ☐ |
