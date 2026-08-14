# 公会仓库（Guild Warehouse）

基于 **NBTAPI**（softdepend）的独立公会仓库：标准箱子 GUI，关闭时保存；容量由公会**历史最高等级**决定，降级不缩小。

## 依赖

- 服务端需安装 [Item-NBT-API](https://www.spigotmc.org/resources/item-entity-tile-nbt-api.7939/)（插件名 `NBTAPI`）
- 未安装时仓库功能禁用，并提示玩家

## 命令

| 命令 | 说明 |
|------|------|
| `/guild warehouse`（`/g warehouse`，别名 `wh`） | 打开第 1 页 |
| `/guild warehouse <页码>` | 打开指定页（1 起） |
| `/guild warehouse info` | 查看 peak 等级、槽位数、页数、官员/成员开仓开关 |
| `/guild warehouse perm <officer\|member> <on\|off>` | 会长（或 `guild.admin`）设置角色开仓权限 |

**不会**出现在主 GUI / 设置界面入口（仅指令）。

## 容量与分页

配置节 `guild-warehouse.slots-by-level`：按 `peak_level` 映射槽位数（须为 **9 的倍数**，无上限）。

- 每页最多 **54** 槽；超出则拆成多页箱子
- 例：`63` → 第 1 页 54 槽 + 第 2 页 9 槽；用 `/guild warehouse 2` 打开第二页
- 数据库槽位为全局 0-based 索引（第 2 页本地槽 0 对应绝对槽 54）
- 关闭某一页时只保存该页槽位，不影响其它页

`peak_level` 在创建与升级时更新，**降级不会降低**。

## 权限

- **会长**：始终可开仓，不可通过指令关闭自己
- **官员 / 成员**：优先使用会长设置的 per-guild 覆盖；否则回退 `permissions.officer|member.can-warehouse`
- 建议默认：leader=true，officer=true，member=false

## 并发

同一公会同一时间只允许一个打开会话；他人会收到「仓库正被使用」。
关箱后异步写库期间仍占用会话；玩家退出也不会提前放锁，避免与进行中的保存竞态。加载失败会释放会话并提示。

## 可选存取流水

配置 `guild-warehouse.access-log: true`（默认 `false`）后，打开与成功保存会写入表 `guild_warehouse_access_log`：
- `OPEN`：打开页码与槽位数
- `SAVE`：相对打开快照的 `put=` / `take=` 材料数量摘要（不含 NBT）

## 配置示例

```yaml
guild-warehouse:
  enabled: true
  access-log: false
  slots-by-level:
    1: 9
    2: 18
    3: 27
    4: 36
    5: 45
    6: 54
    7: 63   # 两页：54 + 9
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

## 本期不做

- 主 GUI 入口
- 硬依赖 NBTAPI
- 跨服仓库同步

English: [GuildWarehouse_EN.md](./GuildWarehouse_EN.md)
