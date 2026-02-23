# Guild Plugin - Feature-Complete Minecraft Guild System(需要官方中文翻译请往下滑)

Guild Plugin is a comprehensive Minecraft server plugin that provides a complete guild/clan system for your server. With this plugin, players can create and manage their own guilds, invite members, establish inter-guild relationships, and enjoy various guild features.

## Core Features

### Guild Management
- Create and customize guilds (name, tag, description)
- Manage guild members (invite, kick, promote, demote)
- Role-based permission system (Leader, Officer, Member)
- Set and teleport to guild home
- Guild application system

### Economy System
- Guild fund management (deposit, withdraw, transfer)
- Guild creation fee configuration
- Economy system integration (supports multiple economy plugins via Vault)

### Relationship System
- Inter-guild relationship management (allied, hostile, neutral, at war, truce)
- Relationship status notifications
- War status alerts

### Leveling System
- Guild level progression
- Increased member capacity
- Unlock additional guild features

### User Interface
- Complete Graphical User Interface (GUI)
- Intuitive menu system
- Customizable interface configuration

## Technical Features

- **Asynchronous Processing**: All database operations are asynchronous, ensuring no impact on server performance
- **Multi-Database Support**: Supports both SQLite and MySQL
- **Placeholder Support**: Integrated with PlaceholderAPI
- **Permission Integration**: Fully compatible with Bukkit permission system
- **High Performance**: Optimized code ensures smooth server operation

## Commands

- `/guild` - Main guild command
- `/guildadmin` - Guild administration command

## Permission Nodes

- Uses built-in permission system

## Basic Guild Information Variables

### Guild Basic Info
- `%guild_name%` - Guild name
- `%guild_tag%` - Guild tag
- `%guild_membercount%` - Current member count
- `%guild_maxmembers%` - Maximum member capacity
- `%guild_level%` - Guild level
- `%guild_balance%` - Guild balance (2 decimal places)
- `%guild_frozen%` - Guild status (Normal/Frozen/No Guild)

### Player Guild Info
- `%guild_role%` - Player's guild role (Leader/Officer/Member)
- `%guild_joined%` - When player joined the guild
- `%guild_contribution%` - Player's contribution to the guild

## Guild Status Check Variables

### Player Status
- `%guild_hasguild%` - Whether player has a guild (Yes/No)
- `%guild_isleader%` - Whether player is leader (Yes/No)
- `%guild_isofficer%` - Whether player is officer (Yes/No)
- `%guild_ismember%` - Whether player is member (Yes/No)

## Guild Permission Check Variables

### Permission Status
- `%guild_caninvite%` - Can invite players (Yes/No)
- `%guild_cankick%` - Can kick members (Yes/No)
- `%guild_canpromote%` - Can promote members (Yes/No)
- `%guild_candemote%` - Can demote members (Yes/No)
- `%guild_cansethome%` - Can set guild home (Yes/No)
- `%guild_canmanageeconomy%` - Can manage guild economy (Yes/No)

## Requirements

- Minecraft Server Version: 1.21+
- Java Version: JDK 17+
- Optional Dependencies: Vault (for economy support), PlaceholderAPI (for placeholder support)

## Installation Steps

1. Place the plugin jar file in your server's `plugins` folder
2. Start the server - the plugin will automatically generate configuration files
3. Edit configuration files as needed
4. Restart server to apply changes



## 📋 中文目录

- [功能特性](#功能特性)
- [安装指南](#安装指南)
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

## 🚀 安装指南

### 前置要求
- **Minecraft服务器**: 1.13+ (推荐1.21+)
- **Java**: JDK 8+ (推荐JDK 17+)
- **Vault**: 经济系统支持 (可选)
- **PlaceholderAPI**: 占位符支持 (可选)

### 安装步骤

1. **下载插件**
   ```bash
   # 从发布页面下载最新版本的jar文件
   # 或使用Maven编译
   mvn clean package
   ```

2. **安装到服务器**
   ```bash
   # 将编译好的jar文件放入plugins文件夹
   cp target/guild-plugin-1.0.0.jar plugins/
   ```

3. **启动服务器**
   ```bash
   # 启动服务器，插件会自动生成配置文件
   java -jar server.jar
   ```

4. **配置插件**
   ```bash
   # 编辑生成的配置文件
   nano plugins/GuildPlugin/config.yml
   nano plugins/GuildPlugin/messages_zh.yml  # 中文
   nano plugins/GuildPlugin/messages_en.yml  # 英文
   nano plugins/GuildPlugin/messages_pl.yml  # 波兰语
   nano plugins/GuildPlugin/gui_zh.yml  # 中文GUI
   nano plugins/GuildPlugin/gui_en.yml  # 英文GUI
   nano plugins/GuildPlugin/gui_pl.yml  # 波兰语GUI
   nano plugins/GuildPlugin/database.yml
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

### 消息配置文件 (messages.yml)

```yaml
# 通用消息
general:
  prefix: "&6[工会] &r"
  no-permission: "&c您没有权限执行此操作！"

# 工会创建消息
create:
  success: "&a工会 {name} 创建成功！"
  insufficient-funds: "&c您的余额不足！创建工会需要 {cost} 金币。"
```

### GUI配置文件 (gui.yml)

```yaml
# 主界面配置
main-menu:
  title: "&6工会系统"
  size: 54
  items:
    create-guild:
      slot: 4
      material: EMERALD_BLOCK
      name: "&a创建工会"
      lore:
        - "&7创建新的工会"
        - "&7需要消耗金币"
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

## 数据库

### 数据表结构

#### guilds (工会表)
```sql
CREATE TABLE guilds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(20) UNIQUE NOT NULL,
    tag VARCHAR(6),
    description TEXT,
    leader_uuid VARCHAR(36) NOT NULL,
    leader_name VARCHAR(16) NOT NULL,
    balance DOUBLE DEFAULT 0.0,
    level INTEGER DEFAULT 1,
    max_members INTEGER DEFAULT 6,
    home_world VARCHAR(64),
    home_x DOUBLE,
    home_y DOUBLE,
    home_z DOUBLE,
    home_yaw FLOAT,
    home_pitch FLOAT,
    frozen BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### guild_members (成员表)
```sql
CREATE TABLE guild_members (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id INTEGER NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(16) NOT NULL,
    role VARCHAR(20) DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
);
```

#### guild_applications (申请表)
```sql
CREATE TABLE guild_applications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id INTEGER NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(16) NOT NULL,
    message TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
);
```

#### guild_relations (关系表)
```sql
CREATE TABLE guild_relations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild1_id INTEGER NOT NULL,
    guild2_id INTEGER NOT NULL,
    relation_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    initiator_uuid VARCHAR(36) NOT NULL,
    initiator_name VARCHAR(16) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    FOREIGN KEY (guild1_id) REFERENCES guilds(id) ON DELETE CASCADE,
    FOREIGN KEY (guild2_id) REFERENCES guilds(id) ON DELETE CASCADE
);
```

#### guild_economy (经济表)
```sql
CREATE TABLE guild_economy (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id INTEGER NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(16) NOT NULL,
    amount DOUBLE NOT NULL,
    type VARCHAR(20) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
);
```

#### guild_logs (日志表)
```sql
CREATE TABLE guild_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id INTEGER NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(16) NOT NULL,
    log_type VARCHAR(50) NOT NULL,
    description TEXT,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
);
```


# GuildPlugin - 完整功能的 Minecraft 公会系统插件

GuildPlugin 是一个为 Minecraft 服务器打造的高性能公会系统插件，支持多语言，涵盖公会管理、经济、关系、等级、GUI等丰富功能，适配多种主流经济与权限插件，完全免费开源！

## 核心特性

### 公会管理
- 支持创建、解散、编辑公会（名称、标签、描述）
- 成员增删、升降职，基于“会长/官员/成员”的角色权限体系
- 公会家园设置与传送
- 公会申请/邀请机制

### 经济系统
- 公会资金管理：存款、取款、转账
- 公会创建费用可配置
- 支持通过 Vault 集成多种经济插件

### 关系系统
- 支持公会间联盟、敌对、中立、战争、停战等关系
- 状态变更和通知机制，战争警报与联盟反馈

### 等级系统
- 公会等级成长，提升成员上限
- 解锁更多公会功能

### 用户界面
- 完整 GUI 菜单与操作界面
- 可自定义配置，操作便捷

### 技术特性
- 所有数据库操作均为异步，无卡顿
- 支持 SQLite 与 MySQL 灵活切换
- 与 PlaceholderAPI 深度集成，变量丰富
- 完全兼容 Bukkit 权限体系
- 高性能优化，稳定可靠

## 快速开始

### 环境需求
- Minecraft 服务器版本：1.21+
- Java 版本：JDK 17+
- 可选依赖：Vault（经济支持）、PlaceholderAPI（变量支持）

### 安装步骤
1. 下载最新版插件 jar 文件，放入服务器 `/plugins` 目录
2. 启动服务器，插件会自动生成配置文件
3. 按需修改配置文件（config.yml/messages.yml/gui.yml/database.yml）
4. 重启服务器即可生效

### Maven 构建（开发者）
```bash
mvn clean package
```

## 主要命令一览

#### 玩家命令
| 命令 | 权限节点 | 说明 |
|------|----------|------|
| /guild                 | guild.use         | 打开主菜单 |
| /guild create ...      | guild.create      | 创建公会 |
| /guild info            | guild.info        | 查看公会信息 |
| /guild members         | guild.members     | 查看成员列表 |
| /guild invite ...      | guild.invite      | 邀请加入 |
| /guild kick ...        | guild.kick        | 移除成员 |
| /guild leave           | guild.leave       | 离开公会 |
| /guild delete          | guild.delete      | 解散公会 |
| /guild promote ...     | guild.promote     | 升职成员 |
| /guild demote ...      | guild.demote      | 降职成员 |
| /guild accept ...      | guild.accept      | 接受邀请 |
| /guild decline ...     | guild.decline     | 拒绝邀请 |
| /guild sethome         | guild.sethome     | 设置家园 |
| /guild home            | guild.home        | 传送家园 |
| /guild apply ...       | guild.apply       | 申请加入 |

#### 管理员命令
| 命令 | 权限节点 | 说明 |
|------|----------|------|
| /guildadmin              | guild.admin           | 管理主命令 |
| /guildadmin reload       | guild.admin.reload    | 重载配置文件 |
| /guildadmin list         | guild.admin.list      | 查看所有公会 |
| /guildadmin info ...     | guild.admin.info      | 查看公会详情 |
| /guildadmin delete ...   | guild.admin.delete    | 强制删除公会 |
| /guildadmin kick ... ... | guild.admin.kick      | 移除玩家 |
| /guildadmin relation     | guild.admin.relation  | 管理关系 |
| /guildadmin test         | guild.admin.test      | 测试功能 |

## 变量支持（PlaceholderAPI）

#### 公会变量
- %guild_name%：公会名称
- %guild_tag%：公会标签
- %guild_membercount%：当前成员数
- %guild_maxmembers%：最大成员数
- %guild_level%：公会等级
- %guild_balance%：资金（2位小数）
- %guild_frozen%：状态（正常/冻结/无公会）

#### 玩家变量
- %guild_role%：角色（会长/官员/成员）
- %guild_joined%：加入时间
- %guild_contribution%：贡献值

#### 状态变量
- %guild_hasguild%：是否有公会
- %guild_isleader%、%guild_isofficer%、%guild_ismember%：角色判定

#### 权限变量
- %guild_caninvite%、%guild_cankick%、%guild_canpromote%、%guild_candemote%、%guild_cansethome%、%guild_canmanageeconomy%

## 配置示例

### config.yml
```yaml
database:
  type: sqlite # 支持 sqlite 或 mysql
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
permissions:
  default:
    can-create: true
    can-invite: true
    can-kick: true
    can-promote: true
    can-demote: false
    can-delete: false
```

## 数据库结构（主表）

- guilds（公会表）
- guild_members（成员表）
- guild_applications（申请表）
- guild_relations（关系表）
- guild_economy（经济表）
- guild_logs（日志表）- 新增

SQL 示例见 plugins/database.sql。

## FAQ 常见问题

- 插件无法启动？请确认服务器版本、JDK、依赖是否齐全，配置文件格式是否正确。
- 经济系统不工作？请确认 Vault 和经济插件已安装，config.yml 配置正确。
- 数据库连接失败？检查数据库配置、MySQL运行状态、账号权限等。
- GUI界面异常？检查配置文件格式与变量替换。
- GUI按钮点击无反应？可能是语言设置问题，已通过槽位检测优化修复。
- 公会创建失败？检查玩家资金、名称是否重复或过长，玩家是否已加入其他公会等。

## 更新日志

### v1.3.1 (最新版本)
-  完整多语言支持系统（中文、英文、波兰语）
-  GUI 槽位检测优化，解决多语言环境下按钮失效问题
-  添加 guild_logs 数据表，完整记录公会活动日志
-  改进 GuildRelationsGUI 多语言支持
-  改进 GuildLogsGUI 多语言支持
-  改进 RelationManagementGUI 多语言支持
-  修复 GUI 返回按钮在非中文环境下失效的 bug
-  新增 78 个多语言翻译条目

### v1.2.3
- 基础功能发布
- 完全正确的逻辑处理
- 完全支持插件扩展
- 完全实现GUI
- 完全实现folia支持
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

### 计划功能
- [ ] 公会战争系统（部分实现）
- [ ] 公会商店
- [ ] 公会任务系统
- [ ] 公会排行榜
- [ ] 公会活动系统
- [ ] 公会仓库
- [ ] 公会公告系统
- [ ] 插件扩展市场（创意工坊）
- [ ] 快速获取资源更新
- [ ] 报错快捷反馈
- [ ] 报错提示代码位置或具体问题
- [ ] 插件单独记录详细的日志
- [ ] 更多安全处理，便于排查可以漏洞
- [ ] 更完整的插件系统逻辑

## 项目主页与支持

- GitHub: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)

## 许可证

GuildPlugin 遵循 [GNU GPL v3.0](https://github.com/chenasyd/-GuildPlugin/blob/main/LICENSE) 开源协议，欢迎二次开发与贡献！

---

# GuildPlugin - Feature-Complete Minecraft Guild System (English)

GuildPlugin is a comprehensive and high-performance Minecraft server plugin that delivers a full-featured guild/faction system, including management, economy, relations, leveling, GUI, and more. Free and open-source!

## Core Features

- **Guild Management**: Create and manage guilds (name, tag, description)
- **Role-based System**: Leader, Officer, Member roles with hierarchical permissions
- **Member Management**: Invite, kick, promote, demote members
- **Guild Home**: Set and teleport to guild home
- **Application System**: Player applications and invitations
- **Economy System**: Guild funds, deposit, withdraw, transfer (Vault support)
- **Relation System**: Inter-guild relations (ally, enemy, neutral, war, truce)
- **Leveling System**: Guild progression and member capacity
- **Full GUI**: Complete graphical user interface
- **Multi-language**: Complete support for Chinese, English, and Polish
- **Asynchronous**: All database operations are async, no lag
- **Multi-Database**: SQLite and MySQL support
- **PlaceholderAPI**: Full integration with rich placeholders

## Technical Features

- **Slot-based Detection**: GUI interactions use slot detection, ensuring buttons work in all languages
- **Comprehensive Logging**: Guild activity log system (guild_logs table)
- **Permission Integration**: Full Bukkit permission system compatibility
- **High Performance**: Optimized for smooth server operation

## Installation

### Requirements
- Minecraft Server Version: 1.13+ (Recommended 1.21+)
- Java Version: JDK 8+ (Recommended JDK 17+)
- Optional: Vault (for economy support), PlaceholderAPI (for placeholder support)

### Steps
1. Download the latest plugin jar file
2. Place it in the server's `plugins` folder
3. Start the server - plugin will automatically generate configuration files
4. Edit configuration files as needed
5. Restart server to apply changes

## Configuration

### Supported Languages
- 🇨🇳 Chinese (Simplified) - `zh`
- 🇬🇧 English - `en`
- 🇵🇱 Polish - `pl`

### Configuration Files
- `config.yml` - Main plugin configuration
- `messages_zh.yml` / `messages_en.yml` / `messages_pl.yml` - Language files
- `gui_zh.yml` / `gui_en.yml` / `gui_pl.yml` - GUI text files
- `database.yml` - Database settings

## Commands

### Player Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/guild` | `guild.use` | Open main menu |
| `/guild create <name> [tag] [desc]` | `guild.create` | Create a guild |
| `/guild info` | `guild.info` | View guild information |
| `/guild members` | `guild.members` | View guild members |
| `/guild invite <player>` | `guild.invite` | Invite player |
| `/guild kick <player>` | `guild.kick` | Kick member |
| `/guild leave` | `guild.leave` | Leave guild |
| `/guild delete` | `guild.delete` | Delete guild |
| `/guild promote <player>` | `guild.promote` | Promote member |
| `/guild demote <player>` | `guild.demote` | Demote member |
| `/guild accept <player>` | `guild.accept` | Accept invitation |
| `/guild decline <player>` | `guild.decline` | Decline invitation |
| `/guild sethome` | `guild.sethome` | Set guild home |
| `/guild home` | `guild.home` | Teleport to guild home |
| `/guild apply <guild> [message]` | `guild.apply` | Apply to join guild |

### Admin Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/guildadmin` | `guild.admin` | Admin main command |
| `/guildadmin reload` | `guild.admin.reload` | Reload configuration |
| `/guildadmin list` | `guild.admin.list` | List all guilds |
| `/guildadmin info <guild>` | `guild.admin.info` | View guild details |
| `/guildadmin delete <guild>` | `guild.admin.delete` | Force delete guild |
| `/guildadmin kick <player> <guild>` | `guild.admin.kick` | Kick player from guild |
| `/guildadmin relation` | `guild.admin.relation` | Manage relations |
| `/guildadmin test` | `guild.admin.test` | Test features |

## Placeholders

### Guild Variables
- `%guild_name%` - Guild name
- `%guild_tag%` - Guild tag
- `%guild_membercount%` - Current member count
- `%guild_maxmembers%` - Maximum member capacity
- `%guild_level%` - Guild level
- `%guild_balance%` - Guild balance (2 decimal places)
- `%guild_frozen%` - Guild status (Normal/Frozen/No Guild)

### Player Variables
- `%guild_role%` - Player's role (Leader/Officer/Member)
- `%guild_joined%` - When player joined guild
- `%guild_contribution%` - Player's contribution to guild

### Status Variables
- `%guild_hasguild%` - Whether player has a guild (Yes/No)
- `%guild_isleader%` - Whether player is leader (Yes/No)
- `%guild_isofficer%` - Whether player is officer (Yes/No)
- `%guild_ismember%` - Whether player is member (Yes/No)

### Permission Variables
- `%guild_caninvite%` - Can invite players (Yes/No)
- `%guild_cankick%` - Can kick members (Yes/No)
- `%guild_canpromote%` - Can promote members (Yes/No)
- `%guild_candemote%` - Can demote members (Yes/No)
- `%guild_cansethome%` - Can set guild home (Yes/No)
- `%guild_canmanageeconomy%` - Can manage guild economy (Yes/No)

## Changelog

### v1.3.1 (Latest)
-  Full multi-language support system (Chinese, English, Polish)
-  GUI slot detection optimization, fixing button issues in non-Chinese languages
-  Added guild_logs table for complete guild activity tracking
-  Improved GuildRelationsGUI multi-language support
-  Improved GuildLogsGUI multi-language support
-  Improved RelationManagementGUI multi-language support
-  Fixed GUI return button failure in non-Chinese environments
-  Added 78 new translation entries

### v1.2.3
- Initial functional release
- Complete logic processing
- Full plugin extension support
- Full GUI implementation
- Full Folia support
- Multi-database support
- Built-in permission system

### v1.0.0
- Initial release
- Complete guild management system
- Economy system integration
- Guild relation management
- Leveling system
- Full GUI interface
- Multi-database support
- Permission system
- PlaceholderAPI integration

## Planned Features
- [ ] Guild war system (partially implemented)
- [ ] Guild shop
- [ ] Guild quest system
- [ ] Guild leaderboard
- [ ] Guild event system
- [ ] Guild storage
- [ ] Guild announcement system
- [ ] Plugin extension marketplace
- [ ] Quick resource updates
- [ ] Quick error feedback
- [ ] Error reporting with code locations
- [ ] Detailed plugin logging
- [ ] Enhanced security features
- [ ] Complete plugin system logic

## Project Links

- **GitHub**: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- **Issues**: [Report bugs here](https://github.com/chenasyd/-GuildPlugin/issues)
- **Wiki**: [Documentation](https://github.com/chenasyd/-GuildPlugin/wiki)

## License

GuildPlugin is licensed under [GNU GPL v3.0](https://github.com/chenasyd/-GuildPlugin/blob/main/LICENSE). Free and open-source, contributions welcome!
