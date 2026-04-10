# GuildPlugin - 完整功能的 Minecraft 公会系统插件

GuildPlugin 是一个为 Minecraft 服务器打造的高性能公会系统插件，支持多语言，涵盖公会管理、经济、关系、等级、GUI等丰富功能，适配多种主流经济与权限插件，完全免费开源！

## 使用须知

使用本插件时，请按以下步骤更新配置：

1. 删除旧版本插件的 `messages_*.yml` 文件，让插件重新生成
2. 如果不想删除，可以手动将对应的 `messages_*.yml` 文件内容替换为最新配置
3. 完成上述步骤后，运行以下命令应用配置：`/guildadmin reload`

> ⚠️ **提示**：如果您使用 v1.3.5 或更高版本，请务必更新插件配置到最新版本；否则插件会出现大量显示错误，影响玩家体验。

## 目录

- [功能特性](#功能特性)
- [安装指南](#安装指南)
- [构建说明](#构建说明)
- [配置说明](#配置说明)
- [命令列表](#命令列表)
- [权限节点](#权限节点)
- [GUI界面](#gui界面)
- [经济系统](#经济系统)
- [工会关系](#工会关系)
- [等级系统](#等级系统)
- [数据库](#数据库)
- [常见问题](#常见问题)
- [更新日志](#更新日志)

## ✨ 功能特性

### 核心功能
- **工会创建与管理**: 支持工会创建、删除、信息查看
- **成员管理**: 邀请、踢出、提升、降级成员
- **权限系统**: 基于角色的权限管理（会长、官员、成员）
- **GUI界面**: 完整的图形用户界面，操作便捷
- **经济系统**: 工会资金管理、存款、取款、转账
- **等级系统**: 工会等级提升，增加成员上限
- **关系管理**: 工会间关系（盟友、敌对、中立、开战、停战）
- **工会家**: 设置和传送到工会家
- **申请系统**: 玩家申请加入工会

### 高级功能
- **异步处理**: 所有数据库操作均为异步，不影响服务器性能
- **多数据库支持**: 支持SQLite和MySQL
- **占位符支持**: 集成PlaceholderAPI
- **经济集成**: 通过Vault支持多种经济插件
- **权限集成**: 与Bukkit权限系统完全集成
- **完整多语言支持**: 支持中文、英文、波兰语，GUI与消息完全国际化
- **槽位检测**: GUI交互使用槽位检测，确保多语言环境下按钮功能正常

### 技术特性
- **异步处理**: 所有数据库操作均为异步，无卡顿
- **多数据库支持**: SQLite 与 MySQL 灵活切换
- **PlaceholderAPI 集成**: 变量丰富，完全兼容
- **权限系统**: 完全兼容 Bukkit 权限体系
- **高性能优化**: 代码优化，稳定可靠

## 🚀 安装指南

### 前置要求
- **Minecraft服务器**: 1.21+
- **Java**: JDK 17+
- **Vault**: 经济系统支持 (可选)
- **PlaceholderAPI**: 占位符支持 (可选)

### 安装步骤

1. **下载插件**
   ```bash
   # 从发布页面下载最新版本的jar文件
   # 或使用 Maven 编译（见下方构建说明）
   ```

2. **安装到服务器**
   ```bash
   # 将编译好的 jar 文件放入 plugins 文件夹
   # 文件名: guild-plugin-{version}.jar
   ```

3. **启动服务器**
   ```bash
   # 启动服务器，插件会自动生成配置文件
   java -jar server.jar
   ```

4. **配置插件**
   ```bash
   # 编辑生成的配置文件
   nano plugins/GuildPlugin/config.yml           # 主配置
   nano plugins/GuildPlugin/messages_zh.yml      # 中文消息
   nano plugins/GuildPlugin/messages_en.yml      # 英文消息
   nano plugins/GuildPlugin/messages_pl.yml      # 波兰语消息
   nano plugins/GuildPlugin/database.yml         # 数据库配置
   ```

5. **重启服务器**
   ```bash
   # 重启服务器使配置生效
   restart
   ```

### 支持的语言

- 🇨🇳 简体中文 (zh)
- 🇺🇸 英文 (en)
- 🇵🇱 波兰语 (pl)

## 🔨 构建说明

本项目采用 Maven 多模块结构。构建命令：

```bash
# 构建所有模块
mvn clean install

# 仅构建 SDK（供外部模块开发者使用）
mvn clean install -pl guild-sdk

# 构建 Plugin 并打包示例模块
mvn clean package -pl guild-plugin -Pbuild-member-rank-module
```

**构建产物：**
- `guild-sdk/target/guild-sdk-{version}.jar` - SDK JAR，用于模块开发
- `guild-plugin/target/guild-plugin-{version}.jar` - 主插件 JAR（包含所有依赖）
- `guild-plugin/target/modules/member-rank.jar` - 示例模块（需使用 profile）

## ⚙️ 配置说明

### 主配置文件 (config.yml)

```yaml
# 数据库配置
database:
  type: sqlite  # sqlite 或 mysql
  mysql:
    host: localhost
    port: 3306
    database: guild
    username: root
    password: ""
    pool-size: 10

# 工会配置
guild:
  min-name-length: 3
  max-name-length: 20
  max-tag-length: 6
  max-description-length: 100
  max-members: 50
  creation-cost: 1000.0  # 创建工会费用

# 权限配置
permissions:
  default:
    can-create: true
    can-invite: true
    can-kick: true
    can-promote: true
    can-demote: false
    can-delete: false
```

### 数据库配置文件 (database.yml)

```yaml
# SQLite配置
sqlite:
  file: "plugins/GuildPlugin/guild.db"

# MySQL配置
mysql:
  host: localhost
  port: 3306
  database: guild
  username: root
  password: ""
  pool-size: 10
  min-idle: 5
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

## 命令列表

### 玩家命令

| 命令 | 权限 | 描述 |
|------|------|------|
| `/guild` | `guild.use` | 打开工会主界面 |
| `/guild create <名称> [标签] [描述]` | `guild.create` | 创建工会 |
| `/guild info` | `guild.info` | 查看工会信息 |
| `/guild members` | `guild.members` | 查看工会成员 |
| `/guild invite <玩家>` | `guild.invite` | 邀请玩家加入工会 |
| `/guild kick <玩家>` | `guild.kick` | 踢出工会成员 |
| `/guild leave` | `guild.leave` | 离开工会 |
| `/guild delete` | `guild.delete` | 删除工会 |
| `/guild promote <玩家>` | `guild.promote` | 提升成员职位 |
| `/guild demote <玩家>` | `guild.demote` | 降级成员职位 |
| `/guild accept <邀请者>` | `guild.accept` | 接受工会邀请 |
| `/guild decline <邀请者>` | `guild.decline` | 拒绝工会邀请 |
| `/guild sethome` | `guild.sethome` | 设置工会家 |
| `/guild home` | `guild.home` | 传送到工会家 |
| `/guild apply <工会> [消息]` | `guild.apply` | 申请加入工会 |

### 管理员命令

| 命令 | 权限 | 描述 |
|------|------|------|
| `/guildadmin` | `guild.admin` | 管理员主命令 |
| `/guildadmin reload` | `guild.admin.reload` | 重载配置文件 |
| `/guildadmin list` | `guild.admin.list` | 列出所有工会 |
| `/guildadmin info <工会>` | `guild.admin.info` | 查看工会详细信息 |
| `/guildadmin delete <工会>` | `guild.admin.delete` | 强制删除工会 |
| `/guildadmin kick <玩家> <工会>` | `guild.admin.kick` | 从工会踢出玩家 |
| `/guildadmin relation` | `guild.admin.relation` | 关系管理 |
| `/guildadmin test` | `guild.admin.test` | 测试功能 |

## GUI界面

### 主界面
- **创建工会**: 创建新的工会
- **工会信息**: 查看当前工会信息
- **成员管理**: 管理工会成员
- **申请管理**: 处理加入申请
- **工会设置**: 修改工会设置
- **工会列表**: 查看所有工会
- **工会关系**: 管理工会关系

### 创建工会界面
- **工会名称输入**: 设置工会名称（3-20字符）
- **工会标签输入**: 设置工会标签（最多6字符，可选）
- **工会描述输入**: 设置工会描述（最多100字符，可选）
- **确认创建**: 支付费用创建工会
- **取消**: 返回主界面

### 成员管理界面
- **成员列表**: 显示所有成员
- **邀请成员**: 邀请新成员
- **踢出成员**: 踢出现有成员
- **提升成员**: 提升成员职位
- **降级成员**: 降级成员职位

## 经济系统

### 功能特性
- **工会资金**: 每个工会独立的资金账户
- **存款系统**: 成员可以向工会存款
- **取款系统**: 成员可以从工会取款
- **转账系统**: 工会间资金转账
- **贡献记录**: 记录每个成员的贡献
- **等级升级**: 资金达到要求自动升级

### 等级系统

| 等级 | 资金要求 | 最大成员数 |
|------|----------|------------|
| 1 | 0-5,000 | 6 |
| 2 | 5,000-10,000 | 12 |
| 3 | 10,000-20,000 | 18 |
| 4 | 20,000-35,000 | 24 |
| 5 | 35,000-50,000 | 30 |
| 6 | 50,000-75,000 | 40 |
| 7 | 75,000-100,000 | 50 |
| 8 | 100,000-150,000 | 60 |
| 9 | 150,000-200,000 | 80 |
| 10 | 200,000+ | 100 |

### 经济命令
- `/guild deposit <金额>` - 向工会存款
- `/guild withdraw <金额>` - 从工会取款
- `/guild transfer <工会> <金额>` - 向其他工会转账
- `/guild balance` - 查看工会余额

## 工会关系

### 关系类型
- **中立 (Neutral)**: 默认关系，无特殊效果
- **盟友 (Ally)**: 友好关系，显示为绿色
- **敌对 (Enemy)**: 敌对关系，显示为红色
- **开战 (War)**: 战争状态，登录时通知
- **停战 (Truce)**: 临时停战，需要双方同意结束

### 关系管理
- **创建关系**: 工会会长可以创建关系
- **接受关系**: 目标工会需要接受关系
- **拒绝关系**: 目标工会可以拒绝关系
- **取消关系**: 可以取消已建立的关系
- **关系过期**: 关系有自动过期机制

### 关系命令
- `/guild relation create <工会> <类型>` - 创建关系
- `/guild relation accept <工会>` - 接受关系
- `/guild relation reject <工会>` - 拒绝关系
- `/guild relation cancel <工会>` - 取消关系

## 变量支持（PlaceholderAPI）

### 公会变量
- `%guild_name%`：公会名称
- `%guild_tag%`：公会标签
- `%guild_membercount%`：当前成员数
- `%guild_maxmembers%`：最大成员数
- `%guild_level%`：公会等级
- `%guild_balance%`：资金（2位小数）
- `%guild_frozen%`：状态（正常/冻结/无公会）

### 玩家变量
- `%guild_role%`：角色（会长/官员/成员）
- `%guild_joined%`：加入时间
- `%guild_contribution%`：贡献值

### 状态变量
- `%guild_hasguild%`：是否有公会
- `%guild_isleader%、%guild_isofficer%、%guild_ismember%`：角色判定

### 权限变量
- `%guild_caninvite%、%guild_cankick%、%guild_canpromote%、%guild_candemote%、%guild_cansethome%、%guild_canmanageeconomy%`

## 数据库

### 数据表结构

插件使用以下数据表：
- `guilds`（工会表）- 工会基本信息
- `guild_members`（成员表）- 成员数据
- `guild_applications`（申请表）- 加入申请
- `guild_relations`（关系表）- 工会间关系
- `guild_economy`（经济表）- 资金交易记录
- `guild_logs`（日志表）- 活动日志

完整 SQL 示例见 `plugins/database.sql`。

## ❓ 常见问题

- **插件无法启动？** 请确认服务器版本（1.21+）、JDK（17+）、依赖是否齐全，配置文件格式是否正确。
- **经济系统不工作？** 请确认 Vault 和经济插件已安装，config.yml 配置正确。
- **数据库连接失败？** 检查数据库配置、MySQL 运行状态、账号权限等。
- **GUI 界面异常？** 检查配置文件格式与变量替换。
- **GUI 按钮点击无反应？** 可能是语言设置问题，已通过槽位检测优化修复。
- **公会创建失败？** 检查玩家资金、名称是否重复或过长，玩家是否已加入其他公会等。

## 📝 更新日志

### v1.3.6 (最新版本)
- 进一步完善了 SDK 模块加载器

### v1.3.5
- 完善了 SDK 模块加载器

### v1.3.4
- 增加了新的 API 和模块加载器框架

### v1.3.3
- 修复了 Folia 关闭时异步线程的错误处理

### v1.3.2
- 迁移 gui.yml 至 messages.yml 文件
- 完全翻译的语言有中文、英文、波兰语

### v1.3.1
- 完整多语言支持系统（中文、英文、波兰语）
- GUI 槽位检测优化，解决多语言环境下按钮失效问题
- 添加 guild_logs 数据表，完整记录公会活动日志
- 改进 GuildRelationsGUI 多语言支持
- 改进 GuildLogsGUI 多语言支持
- 改进 RelationManagementGUI 多语言支持
- 修复 GUI 返回按钮在非中文环境下失效的 bug
- 新增 78 个多语言翻译条目

### v1.2.3
- 基础功能发布
- 完全正确的逻辑处理
- 完全支持插件扩展
- 完全实现 GUI
- 完全实现 folia 支持
- 支持多数据库
- 完全使用内置权限系统

### v1.0.0
- 初始版本发布
- 完整公会管理系统
- 经济系统集成
- 公会关系管理
- 等级系统
- 完整 GUI 界面
- 支持多数据库
- 权限系统
- PlaceholderAPI 集成

## 🚀 计划功能

- [ ] 公会战争系统（部分实现）
- [ ] 公会商店
- [ ] 公会任务系统
- [ ] 公会排行榜
- [ ] 公会活动系统
- [ ] 公会仓库
- [ ] 公会公告系统
- [ ] 插件扩展市场（创意工坊）(正在开发)
- [ ] 快速获取资源更新
- [ ] 报错快捷反馈
- [ ] 报错提示代码位置或具体问题
- [ ] 插件单独记录详细的日志
- [ ] 更多安全处理，便于排查可以漏洞
- [ ] 更完整的插件系统逻辑

## 🔗 项目主页与支持

- **GitHub**: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- **Issues**: [报告问题](https://github.com/chenasyd/-GuildPlugin/issues)
- **Wiki**: [文档](https://github.com/chenasyd/-GuildPlugin/wiki)

## 📄 许可证

GuildPlugin 遵循 [GNU GPL v3.0](https://github.com/chenasyd/-GuildPlugin/blob/main/LICENSE) 开源协议，欢迎二次开发与贡献！
