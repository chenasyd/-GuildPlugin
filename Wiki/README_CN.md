# GuildPlugin

功能完整的 Minecraft 公会/阵营系统插件，支持 Spigot、Paper、Purpur 和 Folia。

## 环境要求

- Minecraft 1.21+
- Java 17+
- Vault（可选，用于经济系统）
- PlaceholderAPI（可选）

## 安装

1. 从 [GitHub Releases](https://github.com/chenasyd/-GuildPlugin/releases) 下载最新版本
2. 将 `guild-plugin-{version}.jar` 放入服务器 `plugins/` 目录
3. 重启服务器，配置文件将自动生成
4. 根据需要编辑 `plugins/GuildPlugin/config.yml` 和语言文件
5. 执行 `/guildadmin reload` 应用更改

> ⚠️ **升级插件时**：建议先备份配置和数据，然后删除 `messages_*.yml` 文件让插件重新生成，以确保包含所有新消息并避免显示异常。

## 功能

### 公会管理
创建、解散公会，邀请、踢出、提升、降级成员。基于角色的权限系统（会长、官员、成员）。公会传送点系统和入会申请系统。

### 经济系统
公会资金管理，支持存款、取款和转账。Vault 集成。等级升级费用可通过 `config.yml` 配置。

### 关系系统
公会间支持中立、同盟、敌对、战争、停战关系。关系请求附带接受/拒绝流程，自动过期机制。

### 等级系统
公会等级提升解锁更多成员上限。等级和升级花费完全可配置。

### GUI 界面
所有公会操作均有图形化界面。基于槽位的按钮检测机制确保跨语言兼容。

### 模块化 SDK
支持外部模块开发，完整的 API 覆盖。运行时热加载/卸载/重载模块。

## 命令

### 玩家命令 (`/guild`)

| 命令 | 权限 | 说明 |
|:----:|:----:|:----:|
| `/guild` | `guild.use` | 打开主 GUI 界面 |
| `/guild create <名称> [标签] [描述]` | `guild.create` | 创建公会 |
| `/guild info` | `guild.use` | 查看公会信息 |
| `/guild members` | `guild.use` | 成员列表 |
| `/guild invite <玩家>` | `guild.invite` | 邀请玩家 |
| `/guild kick <玩家>` | `guild.kick` | 踢出成员 |
| `/guild promote <玩家>` | `guild.promote` | 提升成员为官员 |
| `/guild demote <玩家>` | `guild.demote` | 降级官员为成员 |
| `/guild accept <公会名>` | `guild.use` | 接受公会邀请 |
| `/guild decline <公会名>` | `guild.use` | 拒绝公会邀请 |
| `/guild leave` | `guild.use` | 离开公会 |
| `/guild delete` | `guild.delete` | 删除公会（打开确认 GUI） |
| `/guild delete confirm` | `guild.delete` | 确认删除公会 |
| `/guild delete cancel` | `guild.delete` | 取消删除公会 |
| `/guild sethome` | `guild.sethome` | 设置公会传送点 |
| `/guild home` | `guild.home` | 传送到公会传送点 |
| `/guild deposit <金额>` | `guild.deposit` | 存入资金 |
| `/guild withdraw <金额>` | `guild.withdraw` | 提取资金 |
| `/guild transfer <玩家> <金额>` | `guild.transfer` | 转账给其他玩家 |
| `/guild logs` | `guild.use` | 查看公会操作日志 |
| `/guild placeholder <player\|guild\|rank>` | `guild.use` | 获取占位符信息 |
| `/guild time` | `guild.use` | 查看公会创建时长 |
| `/guild help` | `guild.use` | 显示帮助信息 |

#### `/guild relation` 关系子命令

| 命令 | 权限 | 说明 |
|:----:|:----:|:----:|
| `/guild relation list` | `guild.relation` | 列出所有关系 |
| `/guild relation create <公会> [类型]` | `guild.relation` | 创建关系（默认同盟） |
| `/guild relation accept <公会>` | `guild.relation` | 接受关系请求 |
| `/guild relation reject <公会>` | `guild.relation` | 拒绝关系请求 |
| `/guild relation delete <公会>` | `guild.relation` | 删除关系 |

关系类型：`neutral`（中立）、`ally`（同盟）、`enemy`（敌对）、`war`（战争）、`truce`（停战）

#### `/guild economy` 经济子命令

| 命令 | 权限 | 说明 |
|:----:|:----:|:----:|
| `/guild economy info` | `guild.economy` | 查看经济信息 |
| `/guild economy deposit <金额>` | `guild.economy` | 存入资金（同上） |
| `/guild economy withdraw <金额>` | `guild.economy` | 提取资金（同上） |
| `/guild economy transfer <公会> <金额>` | `guild.economy` | 向其他公会转账 |

### 管理员命令 (`/guildadmin`)

| 命令 | 权限 | 说明 |
|:----:|:----:|:----:|
| `/guildadmin` | `guild.admin` | 管理员主菜单 |
| `/guildadmin reload` | `guild.admin` | 重载所有配置 |
| `/guildadmin list` | `guild.admin` | 列出所有公会 |
| `/guildadmin info <公会>` | `guild.admin` | 查看公会详情 |
| `/guildadmin delete <公会>` | `guild.admin` | 强制删除公会 |
| `/guildadmin freeze <公会>` | `guild.admin` | 冻结公会 |
| `/guildadmin unfreeze <公会>` | `guild.admin` | 解冻公会 |
| `/guildadmin transfer <公会> <新会长>` | `guild.admin` | 转让会长 |
| `/guildadmin economy <公会> <set\|add\|remove> <金额>` | `guild.admin` | 管理公会经济 |
| `/guildadmin update` | `guild.admin` | 检查更新 |
| `/guildadmin update download` | `guild.admin.update` | 下载并安装更新 |
| `/guildadmin test <gui\|economy\|relation>` | `guild.admin` | 运行测试（GUI/经济/关系） |
| `/guildadmin help` | `guild.admin` | 显示帮助信息 |

#### `/guildadmin relation` 关系管理

| 命令 | 权限 | 说明 |
|:----:|:----:|:----:|
| `/guildadmin relation gui` | `guild.admin` | 打开关系管理 GUI |
| `/guildadmin relation list` | `guild.admin` | 列出所有关系 |
| `/guildadmin relation create <公会1> <公会2> <类型>` | `guild.admin` | 创建关系 |
| `/guildadmin relation delete <公会1> <公会2>` | `guild.admin` | 删除关系 |

### 模块管理命令 (`/guildmodule`)

| 命令 | 权限 | 说明 |
|:----:|:----:|:----:|
| `/guildmodule list` | `guild.admin.module` | 列出已加载模块 |
| `/guildmodule load <文件.jar>` | `guild.admin.module` | 加载模块 |
| `/guildmodule unload <模块ID>` | `guild.admin.module` | 卸载模块 |
| `/guildmodule reload <模块ID>` | `guild.admin.module` | 重载模块 |
| `/guildmodule info <模块ID>` | `guild.admin.module` | 查看模块详情 |
| `/guildmodule cloud` | `guild.admin.module` | 列出云端可用模块 |
| `/guildmodule cloud download <模块ID>` | `guild.admin.module` | 从云端下载模块 |

别名：`/g` 对应 `/guild`，`/ga` 对应 `/guildadmin`。

### 全部权限节点

| 权限节点 | 默认值 | 说明 |
|:--------:|:------:|:----:|
| `guild.use` | true | 使用公会系统 |
| `guild.create` | true | 创建公会 |
| `guild.invite` | true | 邀请玩家 |
| `guild.kick` | true | 踢出成员 |
| `guild.promote` | true | 提升成员 |
| `guild.demote` | true | 降级成员 |
| `guild.delete` | op | 删除公会 |
| `guild.sethome` | true | 设置公会传送点 |
| `guild.home` | true | 传送至公会传送点 |
| `guild.relation` | true | 管理公会关系 |
| `guild.economy` | true | 管理公会经济 |
| `guild.deposit` | true | 存入资金 |
| `guild.withdraw` | true | 提取资金 |
| `guild.transfer` | true | 转账 |
| `guild.admin` | op | 管理员权限 |
| `guild.admin.module` | op | 模块管理 |
| `guild.admin.update` | op | 下载安装更新 |

## 配置

### config.yml（节选）

```yaml
database:
  type: sqlite     # sqlite 或 mysql
  mysql:
    host: localhost
    port: 3306
    database: guild
    username: root
    password: ""
    pool-size: 10

guild:
  min-name-length: 3
  max-name-length: 20
  max-tag-length: 6
  max-description-length: 100
  max-members: 50
  creation-cost: 1000.0
  max-level: 10
  levels:
    1: 5000.0
    2: 10000.0
    3: 20000.0
    4: 35000.0
    5: 50000.0
    6: 75000.0
    7: 100000.0
    8: 150000.0
    9: 200000.0

permissions:
  default:
    can-create: true
    can-invite: true
    can-kick: true
    can-promote: true
    can-demote: false
    can-delete: false
```

等级花费由配置驱动，编辑后执行 `/guildadmin reload` 即可应用。

## 等级与成员上限

| 等级 | 升级花费 | 最大成员数 |
|:---:|-------:|:--------:|
| 1 | 0 | 6 |
| 2 | 5,000 | 12 |
| 3 | 10,000 | 18 |
| 4 | 20,000 | 24 |
| 5 | 35,000 | 30 |
| 6 | 50,000 | 40 |
| 7 | 75,000 | 50 |
| 8 | 100,000 | 60 |
| 9 | 150,000 | 80 |
| 10 | 200,000 | 100 |

等级和花费可在 `config.yml` 中自由配置。

## PlaceholderAPI 变量

详细变量列表请参阅 [PlaceholderAPI 参考文档](PLACEHOLDER_API.md)。

## 数据库

插件通过 HikariCP 连接池支持 SQLite 和 MySQL。

### 数据表一览

| 表名 | 用途 |
|:----:|:----:|
| `guilds` | 公会主信息（名称、标签、描述、会长、传送点等） |
| `guild_members` | 公会成员（UUID、角色、加入时间） |
| `guild_applications` | 入会申请记录 |
| `guild_invites` | 邀请记录（含过期时间） |
| `guild_relations` | 公会关系（同盟/敌对/战争/停战） |
| `guild_economy` | 公会经济（余额、等级、经验值、成员上限） |
| `guild_contributions` | 成员贡献记录 |
| `guild_logs` | 操作日志 |
| `guild_currencies` | 虚拟货币（A/B/C 币，按成员+公会维度） |
| `guild_member_investments` | 成员投资追踪（累计存取款） |

## 从源码构建

要求：Java 17+，Maven 3.6+

```bash
git clone https://github.com/chenasyd/-GuildPlugin.git
cd GuildPlugin
mvn clean package -pl guild-plugin
```

输出：`guild-plugin/target/guild-plugin-{version}.jar`

包含示例模块构建：

```bash
mvn clean package -pl guild-plugin -Pbuild-announcement-module
```

## 模块 SDK

模块开发文档请参阅 [SDK 开发者指南](SDK%20Developer-Guide.md)。

## 链接

- GitHub：[chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- Issues：[提交问题](https://github.com/chenasyd/-GuildPlugin/issues)
- Wiki：[文档](https://github.com/chenasyd/-GuildPlugin/wiki)

## 许可证

GNU GPL v3.0
