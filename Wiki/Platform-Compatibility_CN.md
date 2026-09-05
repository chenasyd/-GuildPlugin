# 平台兼容矩阵

GuildPlugin **编译与运行时以 Spigot API（Bukkit）为基准**，可在仅提供 Bukkit API 的混合核心上部署。Paper / Folia 专有类**不得**出现在 `src/main/java` 的 import 中；需要时使用 `Class.forName` 反射并按能力降级。

完整快速入门见 [README_CN](./README_CN.md)。

## 核心约束

| 约束 | 说明 |
|------|------|
| 编译 API | `spigot-api`（`provided`） |
| 禁止 | `paper-api` 进入 compile / runtime / provided（Maven Enforcer + `SpigotApiComplianceTest`） |
| Paper/Folia 特性 | 反射探测（如 `ServerUtils`、`FoliaWorldCreator`），探测失败则禁用对应功能 |
| Java | 17+ |
| MC 版本 | 1.20.1+（`plugin.yml` `api-version: '1.20'`） |

## 服务端软件

| 平台 | 核心功能 | `/guildworld` 多世界 NMS | 调度器 |
|------|----------|---------------------------|--------|
| **Spigot** | 完整支持 | 不支持（非 Folia） | Bukkit 同步调度 |
| **Paper / Purpur** | 完整支持（Bukkit 兼容层） | 不支持（非 Folia） | Bukkit + 插件内 Folia 兼容封装 |
| **Folia** | 完整支持 | 见下方版本白名单 | `CompatibleScheduler` 适配区域线程 |
| **混合核心（仅 Bukkit API）** | **推荐目标平台** | 通常不支持 | 取决于核心实现 |

> Paper/Purpur 用户无需额外配置；插件不依赖 Paper 独有 NMS 作为硬前提。

## Folia 与 `/guildworld`

`ServerUtils.FOLIA_SUPPORTED_VERSIONS` 定义了已对齐 NMS 桥接的 Folia 版本。当前白名单包括：

`1.19.4`、`1.20.4`、`1.20.6`、`1.21.1`–`1.21.8`、`1.21.11`、`26.1.x`

**刻意不包含 1.20.1 Folia**：官方无对应多世界桥接源码，在该版本上 `/guildworld` 保持禁用，其余公会功能仍可运行。

检测逻辑：

1. `Class.forName("io.papermc.paper.threadedregions.RegionizedServer")` → 判定为 Folia
2. `ServerUtils.isFoliaVersionSupported()` → 决定是否启用 `/guildworld` NMS 路径

## 可选软依赖

| 插件 | 作用 | 缺失时 |
|------|------|--------|
| Vault | 个人/公会经济 | 可配置无经济模式 |
| PlaceholderAPI | 占位符 | 占位符不可用 |
| NBTAPI | 公会仓库物品序列化 | 仓库功能禁用 |
| ImagoCore | GUI 图片模式 | 回退普通物品 GUI |
| Geyser-Spigot | 基岩版表单 | Java 版 GUI 仍可用 |

## 跨服（Bungee / Waterfall）

`guild-bungee` 模块提供跨服通信骨架；跨服公会战等为 **P3 代理骨架**，生产开战流程见 [CrossServer-War](./CrossServer-War.md)。

## CI 质量门禁

GitHub Actions 工作流 [`.github/workflows/maven-publish.yml`](../.github/workflows/maven-publish.yml) 在每次 push/PR 执行：

1. `mvn verify`（含 `validate` 阶段 Enforcer、`test` 阶段 77+ 单测）
2. 示例模块 profile 打包（跳过测试）

本地等效命令：

```bash
mvn verify -pl guild-sdk,guild-comm,guild-plugin,guild-bungee -am
```

## 开发者说明

- **权限**：业务鉴权统一走 `GuildMembershipRules`；Bukkit 节点（`guild.admin` 等）与 config 角色矩阵分离。
- **单测**：命令/GUI 权限用 Mockito；MockBukkit 仅 bootstrap 冒烟，不引入 `paper-api` 到主工程。
- **新增 Paper 特性**：先在 issue/PR 说明是否可改为反射；若必须 compile-time Paper API，需单独讨论并更新本矩阵。

英文版：[Platform-Compatibility_EN.md](./Platform-Compatibility_EN.md)
