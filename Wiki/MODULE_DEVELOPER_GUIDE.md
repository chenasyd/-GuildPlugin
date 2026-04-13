# GuildPlugin 模块化扩展系统 - 开发者文档

> **版本**: v1.4.2 | **API版本**: 1.0.0 | **最后更新**: 2026-04-12 | **SDK 覆盖率**: **100%**
>
> 本文档面向所有希望为 GuildPlugin 开发扩展模块的开发者。

---

## 目录

- [1. 架构概览](#1-架构概览)
- [2. 文件结构总览](#2-文件结构总览)
- [3. 核心类说明](#3-核心类说明)
- [4. SDK API 完整参考](#4-sdk-api-完整参考)
- [5. 扩展点（Hook）系统](#5-扩展点hook系统)
- [6. 开发规则与规范](#6-开发规则与规范)
- [7. 快速上手：创建你的第一个模块](#7-快速上手创建你的第一个模块)
- [8. 高级示例](#8-高级示例)
- [9. 附录](#9-附录)

---

## 1. 架构概览

### 1.1 设计目标

| 目标 | 实现方式 |
|------|----------|
| **对核心插件改动极小** | 仅 `GuildPlugin.java` 增加 ~15 行，其他原文件零修改 |
| **模块热插拔** | 独立 URLClassLoader 隔离，`/guildmodule load/unload/reload` 命令即时生效 |
| **开发者友好** | 类似 Bukkit 插件开发体验，清晰的 SDK API 门面 |
| **安全隔离** | DTO 只读数据视图 + 配置段隔离 + ClassLoader 隔离 |
| **硬编码禁止** | 所有文本从 `messages_*.yml` 中读取 |

### 1.2 整体架构图

```
GuildPlugin (主插件)
├── ServiceContainer (服务容器)
│   ├── ModuleManager (★ 新增) ──── 模块生命周期管理
│   │   ├── ModuleLoader          ─── Jar热加载 (URLClassLoader隔离)
│   │   ├── ModuleRegistry        ─── 注册表 + 扩展点管理
│   │   └── 已加载模块实例 Map     ─── moduleId → GuildModule
│   │
│   ├── GUIManager               ─── 原有GUI系统 (不变)
│   ├── EventBus                 ─── 原有事件总线 (不变)
│   ├── GuildService              ─── 原有工会服务 (不变)
│   └── ...其他核心服务            ─── 全部不变
│
├── /guildmodule 命令             ─── ★ 新增: list/load/unload/reload/info
│
plugins/GuildPlugin/modules/       ─── 模块目录 (运行时自动扫描)
├── my-module.jar                  ─── 扩展模块A (可热插拔)
├── another-module.jar             ─── 扩展模块B (可热插拔)
└── ...
```

### 1.3 模块加载流程

```
[启动] → ModuleManager.loadAllModules()
         ↓
    扫描 modules/*.jar
         ↓
    解析每个 jar 的 module.yml → ModuleDescriptor
         ↓
    检查依赖 & 冲突 & 兼容性
         ↓
    ModuleLoader.instantiateModule() → 独立ClassLoader → 反射实例化
         ↓
    创建 ModuleContext (SDK入口) → 调用 module.onEnable(context)
         ↓
    模块注册 GUI按钮 / 命令 / 事件监听等
         ↓
    注册到 ModuleRegistry → ACTIVE状态 ✓
```

### 1.4 热卸载流程

```
[/guildmodule unload xxx]
    ↓
检查是否有其他模块依赖此模块 → 有则拒绝
    ↓
ModuleRegistry.unregister(moduleId) → 自动清理所有注册
    ↓
module.onDisable()
    ↓
ModuleLoader.unloadClassloader() → close() 触发 GC
    ↓
移除引用 → UNLOADED ✓
```

---

## 2. 文件结构总览

### 2.1 新增文件清单（共27个文件）

#### A. 核心模块系统 `com.guild.core.module.*`

| 文件路径 | 类型 | 说明 |
|---------|------|------|
| `core/module/GuildModule.java` | 接口 | **必实现** - 所有扩展模块的主接口 |
| `core/module/ModuleContext.java` | 类 | 模块上下文 - SDK能力统一入口 |
| `core/module/ModuleDescriptor.java` | 类 | 模块描述符 - 从 module.yml 解析的元数据 |
| `core/module/ModuleState.java` | 枚举 | 模块生命周期状态 |
| `core/module/ModuleManager.java` | 类 | **核心** - 模块生命周期管理器 |
| `core/module/ModuleLoader.java` | 类 | Jar热加载器 (URLClassLoader) |
| `core/module/ModuleRegistry.java` | 类 | 模块注册表 + 扩展点管理中心 |

#### B. 异常体系 `com.guild.core.module.exception.*`

| 文件路径 | 说明 |
|---------|------|
| `exception/ModuleLoadException.java` | 加载过程通用异常基类 |
| `exception/ModuleDependencyException.java` | 依赖缺失异常（含缺失列表） |
| `exception/ModuleConflictException.java` | ID冲突或循环依赖异常 |

#### C. 扩展点系统 `com.guild.core.module.hook.*`

| 文件路径 | 类型 | 说明 |
|---------|------|------|
| `hook/HookPoint.java` | 接口 | 扩展点标记接口（所有Hook的父接口） |
| `hook/GUIExtensionHook.java` | 类 | **GUI按钮注入** - 在现有GUI中注入自定义按钮 |

#### D. SDK层 `com.guild.sdk.*` （开发者主要使用的API）

| 文件路径 | 类型 | 说明 |
|---------|------|------|
| `sdk/GuildPluginAPI.java` | 类 | **API门面** - 统一对外接口 |
| `sdk/data/GuildData.java` | 类 | 工会只读DTO（不可变） |
| `sdk/data/MemberData.java` | 类 | 成员只读DTO（不可变） |
| `sdk/data/PlayerRecordData.java` | 类 | 玩家记录DTO（用于外部API对接） |
| `sdk/config/ModuleConfigSection.java` | 类 | 模块私有配置读取器 |
| `sdk/gui/ModuleGUIFactory.java` | 接口 | 自定义GUI工厂接口 |
| `sdk/command/ModuleCommandHandler.java` | 接口 | 子命令处理器函数式接口 |
| `sdk/event/GuildEventHandler.java` | 接口 | 工会创建/删除事件回调 |
| `sdk/event/MemberEventHandler.java` | 接口 | 成员加入/离开事件回调 |
| `sdk/event/GuildEventData.java` | 类 | 工会事件数据载体 |
| `sdk/event/MemberEventData.java` | 类 | 成员事件数据载体 |
| `sdk/event/ModuleEventListener.java` | 类 | 事件包标记（包信息） |
| `sdk/http/HttpClientProvider.java` | 类 | 异步HTTP客户端工具 |

#### E. 命令

| 文件路径 | 说明 |
|---------|------|
| `commands/GuildModuleCommand.java` | `/guildmodule` 管理命令处理器 |

### 2.2 修改的原有文件

| 文件 | 改动内容 | 改动量 |
|------|---------|--------|
| `GuildPlugin.java` | 导入ModuleManager、初始化字段、onEnable中loadAllModules、onDisable中unloadAllModules、注册guildmodule命令、添加getter | +15行 |
| `plugin.yml` | 添加 `guildmodule` 命令声明和 `guild.admin.module` 权限 | +5行 |
| `messages_zh.yml` | 添加60+条模块相关消息键值对（前缀 `module.`） | +65行 |

### 2.3 模块开发者无需接触的核心文件

以下文件属于框架内部实现，模块开发者通过 SDK 间接使用：

```
com.guild.core.module.ModuleManager      ← 内部调度，不暴露给模块
com.guild.core.module.ModuleLoader       ← ClassLoader管理，不暴露
com.guild.core.module.ModuleRegistry     ← 通过API间接使用
com.guild.commands.GuildModuleCommand    ← 管理员专用命令
```

---

## 3. 核心类说明

### 3.1 GuildModule（模块接口）

```java
package com.guild.core.module;
public interface GuildModule {
    // 获取模块描述符（由框架自动填充）
    ModuleDescriptor getDescriptor();
    
    // 设置描述符（框架内部调用）
    void setDescriptor(ModuleDescriptor descriptor);
    
    /** 模块启用回调 */
    void onEnable(ModuleContext context) throws Exception;
    
    /** 模块禁用回调 */
    void onDisable();
    
    /** 获取当前状态 */
    ModuleState getState();
}
```

**开发规范**：
- 这是唯一必须实现的接口
- 不要在构造函数中进行初始化（此时描述符尚未设置）
- `onEnable()` 是初始化一切的地方
- `onDisable()` 必须做好防御性清理（即使框架会额外清理）

### 3.2 ModuleDescriptor（模块元数据）

从 `module.yml` 解析得到，包含以下字段：

| 字段 | YAML key | 必须 | 示例 |
|------|---------|:----:|------|
| id | `id` | ✅ | `ndpr-black-history` |
| name | `name` | ❌ | `NDPR黑历史查询` |
| version | `version` | ❌ | `1.0.0` |
| author | `author` | ❌ | `YourName` |
| description | `description` | ❌ | `查询玩家封禁记录` |
| main | `main` | ✅ | `com.example.MyModule` |
| api-version | `api-version` | ❌ | `1.0.0` |
| type | `type` | ❌ | `gui \| command \| event \| api \| mixed` |
| depends | `depends` | ❌ | `["other-module"]` |
| soft-depends | `soft-depends` | ❌ | `["optional-mod"]` |
| config-prefix | `config-prefix` | ❌ | `ndpr`（配置段前缀） |
| sourceFile | *(自动)* | - | jar文件路径（自动设置） |

### 3.3 ModuleContext（模块上下文）

这是模块与框架交互的**唯一入口对象**，在 `onEnable()` 时传入。提供以下能力分组：

| 能力组 | 方法 | 说明 |
|-------|------|------|
| **核心访问** | `getPlugin()` | 插件实例 |
| | `getApi()` | **SDK API门面**（推荐主入口） |
| | `getServiceContainer()` | 服务容器（直接获取核心服务） |
| | `getEventBus()` | 事件总线 |
| | `getGuiManager()` | GUI管理器 |
| | `getLanguageManager()` | 语言管理器 |
| | `getDescriptor()` | 自身描述符 |
| | `getConfig()` | 私有配置读取器 |
| **消息发送** | `sendMessage(Player, key, args)` | 发送本地化消息 |
| | `getMessage(key, args)` | 获取本地化文本 |
| **线程调度** | `runSync(Runnable)` | 主线程任务 |
| | `runAsync(Runnable)` | 异步任务 |
| | `runLater(ticks, Runnable)` | 延迟主线程任务 |
| | `runTimer(delay, period, Runnable)` | 定时周期任务 |
| **日志** | `getLogger()` | 专属 Logger（名称为 `GuildModule.{模块名}`） |

### 3.4 ModuleState（状态枚举）

```
UNLOADED  → 未加载（jar存在但未初始化）
LOADING   → 正在加载
ACTIVE    → 运行中
DISABLING → 正在禁用
ERROR     → 加载/运行出错
```

### 3.5 异常类型

| 异常类 | 抛出时机 | 包含信息 |
|--------|---------|---------|
| `ModuleLoadException` | 加载任意环节失败 | message + cause |
| `ModuleDependencyException` | 硬依赖缺失 | moduleId + missingDependencies 列表 |
| `ModuleConflictException` | ID重复或有依赖者 | message |

---

## 4. SDK API 完整参考

### 4.1 GuildPluginAPI — 统一API门面

通过 `context.getApi()` 或 `new GuildPluginAPI(plugin)` 获取。

#### 4.1.1 工会查询 API（异步，全部返回 CompletableFuture）

| 方法签名 | 说明 |
|---------|------|
| `CompletableFuture<GuildData> getGuildById(int id)` | 按ID查工会 |
| `CompletableFuture<GuildData> getGuildByName(String name)` | 按名称查工会 |
| `CompletableFuture<GuildData> getPlayerGuild(UUID uuid)` | 查玩家所属工会 |
| `CompletableFuture<List<GuildData>> getAllGuilds()` | 查所有工会列表 |
| `CompletableFuture<List<MemberData>> getGuildMembers(int guildId)` | 查工会成员列表 |

**注意**：所有查询方法在异步线程执行，结果需通过 `.thenAccept()` 处理。

#### 4.1.2 GUI 扩展 API

| 方法签名 | 说明 |
|---------|------|
| `void registerGUIButton(String guiType, int slot, ItemStack item, String moduleId, GUIClickAction handler)` | 在现有GUI中注入按钮 |
| `void registerCustomGUI(String guiId, ModuleGUIFactory factory)` | 注册自定义GUI页面（预留） |

**内置 guiType 标识符**：
- `"GuildListManagement"` — 工会列表管理
- `"GuildDetail"` — 工会详情
- `"MemberList"` — 成员列表
- `"MemberDetails"` — 成员详情
- `"AdminPanel"` — 管理员面板

#### 4.1.3 命令扩展 API（预留）

| 方法签名 | 说明 |
|---------|------|
| `void registerSubCommand(String parentCmd, String name, ModuleCommandHandler handler, String permission)` | 注册子命令（后续版本实现） |

#### 4.1.4 事件 API

| 方法签名 | 说明 |
|---------|------|
| `void onGuildCreate(GuildEventHandler handler)` | 监听工会创建 |
| `void onGuildDelete(GuildEventHandler handler)` | 监听工会解散 |
| `void onMemberJoin(MemberEventHandler handler)` | 监听成员加入 |
| `void onMemberLeave(MemberEventHandler handler)` | 监听成员离开/被踢 |

#### 4.1.5 HTTP 工具 API

| 方法签名 | 说明 |
|---------|------|
| `CompletableFuture<String> httpGet(String url, Map headers)` | GET请求（异步） |
| `CompletableFuture<String> httpGet(String url)` | GET请求（无自定义头） |
| `CompletableFuture<String> httpPost(String url, String body, Map headers)` | POST请求（异步） |
| `HttpClientProvider getHttpClient()` | 获取HTTP客户端（自定义超时等） |

**默认配置**：连接超时 5000ms，读取超时 10000ms。

### 4.2 数据传输对象（DTO）

#### GuildData — 工会只读数据

| 字段 | 类型 | 来源映射 |
|------|------|---------|
| `id` | `int` | `Guild.getId()` |
| `name` | `String` | `Guild.getName()` |
| `masterUuid` | `UUID` | `Guild.getLeaderUuid()` |
| `masterName` | `String` | `Guild.getLeaderName()` |
| `level` | `int` | `Guild.getLevel()` |
| `experience` | `long` | *暂为0（核心模型无此字段）* |
| `balance` | `double` | `Guild.getBalance()` |
| `memberCount` | `int` | *暂为0（可通过 getMembers().size() 获取）* |
| `maxMembers` | `int` | `Guild.getMaxMembers()` |
| `motto` | `String` | `Guild.getDescription()` |
| `createTime` | `long` (epoch ms) | `Guild.getCreatedAt()` |
| `members` | `List<MemberData>` | 可按需加载 |

#### MemberData — 成员只读数据

| 字段 | 类型 | 来源映射 |
|------|------|---------|
| `playerUuid` | `UUID` | `GuildMember.getPlayerUuid()` |
| `playerName` | `String` | `GuildMember.getPlayerName()` |
| `role` | `String` | `GuildMember.getRole().name()` |
| `joinTime` | `long` (epoch ms) | `GuildMember.getJoinedAt()` |
| `contribution` | `double` | *暂为0（核心模型无此字段）* |
| `online` | `boolean` | *暂为false（核心模型无此字段）* |

#### PlayerRecordData — 玩家记录DTO（用于外部API对接）

| 字段 | 类型 | 说明 |
|------|------|------|
| `playerUuid` | `UUID` | 玩家UUID |
| `playerName` | `String` | 玩家名 |
| `recordType` | `String` | 记录类型（如 BAN/KICK/WARN） |
| `reason` | `String` | 原因 |
| `sourceServer` | `String` | 来源服务器 |
| `operatorName` | `String` | 操作者 |
| `timestamp` | `long` | 时间戳 |
| `expiryTime` | `long` | 过期时间（-1=永久） |

### 4.3 配置系统 — ModuleConfigSection

模块可以在 `config.yml` 的 `modules.{moduleId}` 下拥有私有配置空间：

```yaml
modules:
  my-module:
    api-key: "your-api-key"
    endpoint: "https://api.example.com"
    max-results: 10
    enable-feature: true
```

**可用方法**：

| 方法 | 返回 | 说明 |
|------|------|------|
| `getString(key, defaultVal)` | `String` | 取字符串 |
| `getInt(key, defaultVal)` | `int` | 取整数 |
| `getBoolean(key, defaultVal)` | `boolean` | 取布尔值 |
| `getLong(key, defaultVal)` | `long` | 取长整数 |
| `getDouble(key, defaultVal)` | `double` | 取浮点数 |
| `getStringList(key)` | `List<String>` | 取字符串列表 |
| `contains(key)` | `boolean` | 键是否存在 |

### 4.4 HTTP客户端 — HttpClientProvider

封装了 Java 原生 HttpURLConnection 的异步调用：

```java
// 基本用法
context.getApi().httpGet("https://api.example.com/player/" + uuid,
    Map.of("Authorization", "Bearer " + apiKey))
    .thenAccept(response -> {
        // response 为响应体文本（JSON字符串）
        // 注意：此处仍在异步线程！如需操作GUI需切回主线程
        context.runSync(() -> showResult(player, response));
    });

// POST请求
context.getApi().httpPost(url, jsonBody, headers).thenAccept(...);

// 自定义超时配置
HttpClientProvider customClient = new HttpClientProvider(3000, 15000);
customClient.httpGet(url, headers)...;
```

---

## 5. 扩展点（Hook）系统

### 5.1 HookPoint 接口

所有扩展点的基础接口，定义两个契约方法：

```java
public interface HookPoint {
    void unregisterByModule(String moduleId);  // 按模块ID批量清理
    void unregisterAll();                     // 清理所有（关闭时调用）
}
```

### 5.2 GUIExtensionHook — GUI按钮注入

**用途**：在已有的 GUI 界面中的指定槽位注入自定义按钮。

**注册方式**：
```java
context.getApi().registerGUIButton(
    "MemberDetails",       // 目标GUI类型
    36,                    // 注入槽位号（0-based）
    buttonItem,           // ItemStack 物品图标
    "my-module-id",       // 当前模块ID（用于卸载时清理）
    (player, contextArgs) -> {  // 点击回调
        // contextArgs 由具体GUI决定，通常是相关的数据对象
        UUID targetUuid = (UUID) contextArgs[0];
        openCustomUI(player, targetUuid);
    }
);
```

**数据结构**：

```java
public class GUIInjectionSlot {
    private final String moduleId;           // 所属模块ID
    private final int slot;                 // 槽位编号
    private final ItemStack item;            // 显示物品
    private final GUIClickAction action;     // 点击回调
}

@FunctionalInterface
public interface GUIClickAction {
    void onClick(Player player, Object... context);
}
```

**生命周期保证**：当模块被卸载时，该模块的所有注入项会自动清理，无需手动注销。

---

## 6. 开发规则与规范

### 规则一：【强制】禁止硬编码

**规则内容**：所有用户可见文本必须从 `messages_*.yml` 中读取。

✅ 正确做法：
```java
// 使用 ModuleContext 发送消息
context.sendMessage(player, "module.my-module.welcome", playerName);

// 获取纯文本
String title = context.getMessage("module.my-module.gui-title");
```

❌ 错误做法：
```java
// 硬编码中文
player.sendMessage("§c模块加载失败");
// 硬编码英文
logger.info("Module loaded successfully");
```

**例外情况**（允许硬编码的场景）：
- Java 包名和全限定类名（技术标识符）
- 正则表达式模式字符串
- 日志 debug 级别的内部调试输出
- `module.yml` 中的 `id` 字段（技术标识）

### 规则二：【强制】消息键命名规范

模块消息必须使用 `module.{moduleId}.{category}.{action}` 格式的命名空间：

```
module.my-module.load.success       # 加载成功提示
module.my-module.error.api-fail     # API调用失败错误
module.my-module.gui.title          # GUI标题
module.my-module.gui.btn-history    # 按钮名称
module.my-module.gui.btn-hist-lore  # 按钮描述
module.my-module.info.found         # 查询结果提示
```

**占位符格式**：使用 `{0}`, `{1}`, `{player}`, `{guild}` 等。

### 规则三：【推荐】线程安全规范

| 操作类型 | 应在线程 | 使用方式 |
|---------|---------|---------|
| 操作 Bukkit API（GUI/实体/物品） | **主线程** | `context.runSync(() -> { ... })` |
| HTTP 请求 / 数据库查询 / 文件IO | **异步线程** | `context.runAsync(() -> { ... })` |
| 典型模式 | 先异步查询后主线程更新 | 见下方示例 |

```java
// ★ 推荐：异步查询 + 主线程更新 的经典模式
context.runAsync(() -> {
    var records = apiClient.queryPlayerHistory(uuid);  // 异步网络请求
    context.runSync(() -> {
        new BlackHistoryGUI(player, records, context).open();  // 主线程开GUI
    });
});
```

### 规则四：【推荐】资源释放规范

```java
@Override
public void onDisable() {
    // 1. 关闭外部连接（HTTP客户端、数据库连接池等）
    if (apiClient != null) apiClient.close();
    if (schedulerTask != null) schedulerTask.cancel();
    
    // 2. GUI/命令/事件的注册由 ModuleRegistry 自动清理
    //    但建议在此做防御性 null 检查
    
    context.getLogger().info(context.getMessage(
        "module.my-module.disabled"));
}
```

### 规则五：【推荐】module.yml 编写规范

```yaml
id: your-module-unique-id        # 小写字母+连字符，全局唯一
name: "显示名"                   # 支持多语言显示
version: "1.0.0"                 # 语义版本号
author: "作者名"                  # 作者信息
main: "com.example.YourMain"     # 全限定类名
api-version: "1.0.0"             # 要求的最小API版本
type: mixed                       # gui/command/event/api/mixed

depends: []                      # 硬依赖列表（缺失则不加载）
soft-depends: []                 # 可选依赖（缺失仅警告）
config-prefix: your-module        # 配置段前缀（对应 config.yml modules.xxx 下）
```

### 规则六：【安全】数据访问限制

| 能力 | 是否允许 | 说明 |
|-----|:-------:|------|
| 读取工会数据 | ✅ | 通过 DTO 只读访问 |
| 修改工会数据 | ❌ | 不提供写入API（保护数据安全） |
| 访问玩家对象 | ⚠️ | 仅限当前在线玩家 |
| 调用Bukkit API | ⚠️ | 必须在主线程 |
| 发送HTTP请求 | ✅ | 通过 SDK 工具（已封装） |
| 反射访问内部字段 | ❌ | 可能导致不兼容升级 |
| 直接操作数据库 | ❌ | 存在安全隐患 |

### 规则七：【兼容性】Folia/Spigot 双平台

框架底层使用 `CompatibleScheduler` 封装调度差异，模块只需：
- 使用 `context.runSync()` / `context.runAsync()` 即可在 Folia 和 Spigot 上正确运行
- **不要** 直接使用 `Bukkit.getScheduler()` 或 `FoliaScheduler`
- **不要** 直接操作 `ChunkAccess` 等 Folia 特有 API（除非明确需要且自行处理兼容）

---

## 7. 快速上手：创建你的第一个模块

### 步骤一：创建项目结构

```
MyFirstModule/
├── pom.xml                          # Maven构建文件
├── src/main/java/com/example/
│   └── MyFirstModule.java            # 模块主类
├── module.yml                       # 模块描述文件
└── README.md                        # 可选
```

### 步骤二：pom.xml 配置

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-first-module</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <!-- SDK 作为 provided 依赖（打包时不包含进去） -->
        <dependency>
            <groupId>com.guild</groupId>
            <artifactId>guild-plugin-sdk</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- Spigot API -->
        <dependency>
            <groupId>org.spigotmc</groupId>
            <artifactId>spigot-api</artifactId>
            <version>1.21-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>${project.name}</finalName>
        <!-- 打成普通jar（不是plugin jar！）-->
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 步骤三：module.yml

```yaml
id: my-first-module
name: 我的第一个模块
version: 1.0.0
author: YourName
description: 一个简单的示例模块
main: com.example.MyFirstModule
api-version: 1.0.0
type: command
config-prefix: my-module
```

### 步骤四：编写模块主类

```java
package com.example;

import com.guild.core.module.*;
import org.bukkit.entity.Player;

public class MyFirstModule implements GuildModule {

    private ModuleContext context;

    @Override
    public void onEnable(ModuleContext ctx) throws Exception {
        this.context = ctx;

        // 1. 读取配置
        boolean featureEnabled = ctx.getConfig().getBoolean("enabled", true);

        // 2. 注册一个测试命令处理（预留接口）
        ctx.getApi().registerSubCommand("guild", "test",
            (sender, args) -> {
                sender.sendMessage("Hello from MyFirstModule!");
            }, null);  // null 表示不需要权限

        // 3. 日志
        context.getLogger().info(context.getMessage(
            "module.my-first.enabled"));
    }

    @Override
    public void onDisable() {
        context.getLogger().info(context.getMessage(
            "module.my-first.disabled"));
    }
}
```

### 步骤五：打包部署

```bash
mvn clean package
# 将生成的 MyFirstModule.jar 放入:
#   plugins/GuildPlugin/modules/MyFirstModule.jar
```

### 步骤六：管理模块

```bash
# 列出所有模块
/guildmodule list

# 加载新模块
/guildmodule load MyFirstModule.jar

# 重载模块（修改代码后重新编译放入再执行此命令）
/guildmodule reload my-first-module

# 卸载模块
/guildmodule unload my-first-module

# 查看详情
/guildmodule info my-first-module
```

---

## 8. 高级示例

### 8.1 NDPR 黑历史查询模块（完整示例）

这是一个典型的混合型模块（GUI + 外部API集成），展示了：
- GUI 按钮注入到 MemberDetails 页面
- 异步 HTTP 调用外部 API
- 自定义 GUI 展示查询结果
- 配置文件读取
- 本地化消息使用

```yaml
# module.yml
id: ndpr-black-history
name: NDPR黑历史查询
version: 1.0.0
author: NDPR-Team
description: "通过NDPR联合封禁系统查询玩家黑历史"
main: com.ndpr.module.NDPRBlackHistoryModule
api-version: 1.0.0
type: mixed
config-prefix: ndpr
```

```java
package com.ndpr.module;

import com.guild.core.module.*;
import com.guild.sdk.*;
import com.guild.sdk.data.PlayerRecordData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * NDPR 黑历史查询模块
 * 功能：在成员详情页注入"查看黑历史"按钮，点击后查询NDPR API并展示结果
 */
public class NDPRBlackHistoryModule implements GuildModule {

    private ModuleContext context;
    private HttpClientProvider apiClient;

    @Override
    public void onEnable(ModuleContext ctx) throws Exception {
        this.context = ctx;

        // ===== 1. 从配置读取 API 参数 =====
        String apiKey = ctx.getConfig().getString("api-key", "");
        String endpoint = ctx.getConfig().getString("endpoint",
                "https://ndpr-api.example.com");
        int timeout = ctx.getConfig().getInt("timeout", 10000);

        this.apiClient = new HttpClientProvider(5000, timeout);

        // ===== 2. 在 MemberDetails GUI 注入按钮 =====
        ItemStack btnItem = createItem(Material.BOOK,
                ctx.getMessage("module.ndpr.btn-name"),
                ctx.getMessage("module.ndpr.btn-lore"));

        ctx.getApi().registerGUIButton(
                "MemberDetails",    // 注入到成员详情页面
                36,                 // 第36号槽位（通常在第5行中间）
                btnItem,
                "ndpr-black-history",  // 模块ID（用于卸载时清理）
                (player, contextArgs) -> {
                    // contextArgs[0] 是目标玩家的 UUID
                    UUID targetUuid = (UUID) contextArgs[0];
                    queryAndShow(player, targetUuid);
                }
        );

        // ===== 3. 注册子命令 /guild blackhistory <玩家名> =====
        ctx.getApi().registerSubCommand("guild", "blackhistory",
                (sender, args) -> {
                    if (!(sender instanceof Player player)) return;
                    if (args.length == 0) {
                        ctx.sendMessage(player,
                                "module.ndpr.cmd.usage");
                        return;
                    }
                    // 异步解析玩家名→UUID
                    UUID target = resolvePlayer(args[0]);
                    if (target == null) {
                        ctx.sendMessage(player,
                                "module.ndpr.player-not-found", args[0]);
                        return;
                    }
                    queryAndShow(player, target);
                },
                "guild.admin"
        );

        context.getLogger().info(ctx.getMessage("module.ndpr.enabled"));
    }

    @Override
    public void onDisable() {
        // apiClient 无状态无需关闭
        context.getLogger().info(ctx.getMessage("module.ndpr.disabled"));
    }

    /**
     * 查询并展示黑历史（异步查询 + 主线程展示）
     */
    private void queryAndShow(Player player, UUID targetUuid) {
        String targetName = targetName(targetUuid);
        String endpoint = context.getConfig().getString("endpoint");
        String apiKey = context.getConfig().getString("api-key");

        // 发送进度提示
        context.sendMessage(player, "module.ndpr.queries", targetName);

        // ★ 异步 HTTP 请求
        context.getApi().httpGet(
                endpoint + "/player/" + targetUuid.toString(),
                Map.of("Authorization", "Bearer " + apiKey)
        ).thenAccept(responseBody -> {
            // 解析JSON响应
            List<PlayerRecordData> records = parseResponse(responseBody);

            // ★ 切回主线程打开 GUI
            context.runSync(() -> {
                if (records.isEmpty()) {
                    context.sendMessage(player,
                            "module.ndpr.no-records", targetName);
                } else {
                    new BlackHistoryGUI(player, targetName, records, context)
                            .open();
                }
            });
        }).exceptionally(ex -> {
            // 错误处理
            context.runSync(() -> {
                context.sendMessage(player,
                        "module.ndpr.query-error", ex.getMessage());
            });
            return null;
        });
    }

    /** 解析 NDPR API JSON 响应为 PlayerRecordData 列表 */
    private List<PlayerRecordData> parseResponse(String json) {
        List<PlayerRecordData> records = new ArrayList<>();
        // 此处省略 JSON 解析逻辑...
        // 使用 Gson/Jackson 解析后构造 PlayerRecordData 对象
        return records;
    }

    /** 创建物品图标 */
    private ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private UUID resolvePlayer(String name) {
        // 简化的玩家名→UUID解析
        Player online = org.bukkit.Bukkit.getPlayer(name);
        return online != null ? online.getUniqueId() : null;
    }

    private String targetName(UUID uuid) {
        Player p = org.bukkit.Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : "未知";
    }
}
```

### 8.2 messages_zh.yml 中对应的键值

```yaml
# NDPR 黑历史模块消息
module.ndpr.enabled: "&a[NDPR] 黑历史查询模块已启用"
module.ndpr.disabled: "&7[NDPR] 黑历史查询模块已禁用"
module.ndpr.btn-name: "&c&l查看黑历史"
module.ndpr.btn-lore: "&7点击查看该玩家的封禁记录"
module.ndpr.queries: "&e正在查询 {0} 的黑历史..."
module.ndpr.no-records: "&a{0} 没有找到任何记录"
module.ndpr.query-error: "&c查询失败: {0}"
module.ndpr.player-not-found: "&c找不到玩家: {0}"
module.ndpr.cmd.usage: "&c用法: /guild blackhistory <玩家名>"
```

### 8.3 config.yml 中的配置段

```yaml
modules:
  ndpr:
    api-key: "your-secret-key-here"
    endpoint: "https://ndpr-api.example.com/v1"
    timeout: 15000
```

---

## 9. 附录

### 附录A：模块目录结构

```
plugins/GuildPlugin/
├── config.yml              # 主配置（含 modules.xxx 配置段）
├── database.yml            # 数据库配置
├── messages_zh.yml        # 中文语言（含 module.xxx 消息键）
├── messages_en.yml        # 英文语言
└── modules/               # ★ 扩展模块存放目录
    ├── ndpr-black-history.jar
    ├── guild-query-enhancer.jar
    └── your-module.jar
```

### 附录B：权限节点

| 权限节点 | 默认值 | 说明 |
|---------|:------:|------|
| `guild.admin.module` | op | 使用 `/guildmodule` 命令管理模块 |

### 附录C：命令一览

| 命令 | 说明 | 权限 |
|------|------|------|
| `/guildmodule list` | 列出所有已加载模块 | guild.admin.module |
| `/guildmodule load <jar>` | 加载指定模块jar | guild.admin.module |
| `/guildmodule unload <id>` | 卸载指定模块 | guild.admin.module |
| `/guildmodule reload <id>` | 重载指定模块 | guild.admin.module |
| `/guildmodule info <id>` | 查看模块详细信息 | guild.admin.module |

### 附录D：module.yml 完整字段参考

```yaml
# ==================== 模块描述文件 (module.yml) ====================
# 此文件必须放置于模块 jar 的根目录下

# --- 必填字段 ---

id: example-module              # 模块唯一标识符（小写字母+数字+连字符）
main: com.example.ExampleModule  # 主类全限定名（必须实现 GuildModule 接口）

# --- 可选字段 ---

name: "示例模块"                # 显示名称
version: "1.0.0"                # 版本号
author: "DeveloperName"          # 作者
description: "模块功能简介"       # 描述文字

# API版本要求（暂未强制校验，预留未来兼容性检查）
api-version: "1.0.0"

# 模块类型分类（用于管理和展示）
# 可选值: gui | command | event | api | mixed
type: mixed

# 硬依赖（缺失时模块无法加载）
depends: []

# 软依赖（缺失时仅警告，不影响加载）
soft-depends: []

# 配置段前缀（用于 config.yml 中 modules.xxx 的 xxx 部分）
# 默认值为 id 字段的值
config-prefix: example-module
```

### 附录E：常见问题 FAQ

**Q: 模块能访问数据库吗？**
A: 目前不直接提供数据库写入权限。如需持久化数据，建议：
1. 使用模块自身的配置文件存储简单数据
2. 通过 HTTP API 与外部服务通信
3. 后续版本可能考虑开放安全的数据库读写接口

**Q: 模块之间可以互相通信吗？**
A: 可以通过事件系统间接通信。模块A发布事件，模块B监听。也可以通过共享的外部API服务交互。

**Q: 更新模块后是否需要重启服务器？**
A: 不需要。将新 jar 替换到 `modules/` 目录后执行 `/guildmodule reload <id>` 即可。

**Q: 如果模块 onEnable() 抛出异常会怎样？**
A: 模块进入 ERROR 状态，不会影响其他模块或核心功能。控制台会打印详细错误日志。

**Q: 如何调试模块？**
A: 使用 `context.getLogger()` 输出日志，所有日志以 `GuildPlugin.{模块名}` 为前缀，便于过滤。

---

> **文档维护**: 此文档随框架版本同步更新。如有疑问请查阅源码注释或提交 Issue。
