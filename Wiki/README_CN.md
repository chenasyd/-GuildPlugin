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

> 从 v1.3.9 或更高版本升级时，建议删除 `messages_*.yml` 文件让插件重新生成，否则执行 `/guildadmin reload` 即可。

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

### 玩家命令

| 命令 | 权限 | 说明 |
|:----:|:----:|:----:|
| `/guild` | `guild.use` | 主 GUI 界面 |
| `/guild create <名称> [标签] [描述]` | `guild.create` | 创建公会 |
| `/guild info` | `guild.info` | 查看公会信息 |
| `/guild members` | `guild.members` | 成员列表 |
| `/guild invite <玩家>` | `guild.invite` | 邀请玩家 |
| `/guild kick <玩家>` | `guild.kick` | 踢出成员 |
| `/guild leave` | `guild.leave` | 离开公会 |
| `/guild delete` | `guild.delete` | 删除公会 |
| `/guild promote <玩家>` | `guild.promote` | 提升成员 |
| `/guild demote <玩家>` | `guild.demote` | 降级成员 |
| `/guild accept <玩家>` | `guild.accept` | 接受邀请 |
| `/guild decline <玩家>` | `guild.decline` | 拒绝邀请 |
| `/guild sethome` | `guild.sethome` | 设置公会传送点 |
| `/guild home` | `guild.home` | 传送到公会传送点 |
| `/guild apply <公会> [留言]` | `guild.apply` | 申请加入 |
| `/guild balance` | `guild.balance` | 查看公会余额 |
| `/guild deposit <金额>` | `guild.deposit` | 存入资金 |
| `/guild withdraw <金额>` | `guild.withdraw` | 提取资金 |

### 管理员命令

| 命令 | 权限 | 说明 |
|:----:|:----:|:----:|
| `/guildadmin` | `guild.admin` | 管理员主菜单 |
| `/guildadmin reload` | `guild.admin.reload` | 重载配置 |
| `/guildadmin list` | `guild.admin.list` | 列出所有公会 |
| `/guildadmin info <公会>` | `guild.admin.info` | 公会详情 |
| `/guildadmin delete <公会>` | `guild.admin.delete` | 强制删除公会 |
| `/guildadmin kick <玩家>` | `guild.admin.kick` | 强制踢出成员 |
| `/guildmodule` | `guild.admin.module` | 模块管理 |

别名：`/g` 对应 `/guild`，`/ga` 对应 `/guildadmin`。

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

## PlaceholderAPI 变量

详细变量列表请参阅 [PlaceholderAPI 参考文档](PLACEHOLDER_API.md)。

## 数据库

插件通过 HikariCP 连接池支持 SQLite 和 MySQL。

数据表：`guilds`、`guild_members`、`guild_applications`、`guild_relations`、`guild_economy`、`guild_logs`

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
