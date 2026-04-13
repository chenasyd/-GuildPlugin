# GuildPlugin SDK 开发者指南

> **版本**: v1.4.2 | **最后更新**: 2026-04-12 | **SDK 覆盖率**: **100%** (所有公开方法)

GuildPlugin SDK 是为外部模块开发者提供的编译期依赖，允许你创建功能丰富的公会系统扩展模块。本文档将引导你完成从环境搭建到发布模块的完整流程。

---

## 📑 目录导航

- [快速开始](#快速开始)
  - [环境要求](#环境要求)
  - [项目初始化](#项目初始化)
  - [第一个模块](#第一个模块)
- [核心概念](#核心概念)
  - [模块生命周期](#模块生命周期)
  - [模块描述符](#模块描述符)
  - [模块上下文](#模块上下文)
- [API 参考](#api-参考)
  - [GuildPluginAPI](#guildpluginapi)
  - [数据模型](#数据模型)
  - [事件系统](#事件系统)
  - [GUI 系统](#gui-系统)
  - [配置管理](#配置管理)
  - [命令系统](#命令系统)
  - [HTTP 客户端](#http-客户端)
- [开发指南](#开发指南)
  - [注册 GUI 按钮](#注册-gui-按钮)
  - [监听事件](#监听事件)
  - [自定义 GUI 界面](#自定义-gui-界面)
  - [数据持久化](#数据持久化)
  - [多语言支持](#多语言支持)
- [示例模块](#示例模块)
  - [公告模块 (AnnouncementModule)](#公告模块-announcementmodule)
  - [成员排名模块 (MemberRankModule)](#成员排名模块-memberrankmodule)
- [module.yml 配置](#moduleyml-配置)
- [构建与打包](#构建与打包)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

---

## 快速开始

### 环境要求

| 项目 | 要求 |
|------|------|
| **JDK** | 17 或更高版本 |
| **Maven** | 3.6+ |
| **IDE** | IntelliJ IDEA（推荐）或 Eclipse |
| **SDK** | `com.guild:guild-sdk:1.3.6`（provided） |

### 项目初始化

#### 1. 创建 Maven 项目

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-guild-module</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <repositories>
        <!-- 如果 SDK 已安装到本地仓库，可省略此配置 -->
        <repository>
            <id>local-repo</id>
            <url>file://${user.home}/.m2/repository</url>
        </repository>
    </repositories>

    <dependencies>
        <!-- GuildPlugin SDK（编译期依赖，不要打进最终 JAR） -->
        <dependency>
            <groupId>com.guild</groupId>
            <artifactId>guild-sdk</artifactId>
            <version>1.3.6</version>
            <scope>provided</scope>
        </dependency>

        <!-- Spigot API（编译期依赖） -->
        <dependency>
            <groupId>org.spigotmc</groupId>
            <artifactId>spigot-api</artifactId>
            <version>1.21-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2. 安装 SDK 到本地仓库

在获取 `guild-sdk` JAR 后：

```bash
# 方式一：从 GitHub Releases 下载后手动安装
mvn install:install-file \
    -Dfile=guild-sdk-1.3.6.jar \
    -DgroupId=com.guild \
    -DartifactId=guild-sdk \
    -Dversion=1.3.6 \
    -Dpackaging=jar

# 方式二：从源码构建（推荐）
git clone https://github.com/chenasyd/-GuildPlugin.git
cd GuildPlugin
mvn clean install -pl guild-sdk
```

### 第一个模块

创建最简单的模块：

**MyFirstModule.java**
```java
package com.example.guild;

import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.sdk.GuildPluginAPI;

public class MyFirstModule implements GuildModule {

    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;

        // 获取 API 实例
        GuildPluginAPI api = context.getApi();

        // 记录日志
        context.getLogger().info("§a[MyModule] 模块已启用！");

        // 获取配置
        String configValue = context.getConfig().getString("my-setting", "默认值");
        context.getLogger().info("§7[MyModule] 配置值: " + configValue);
    }

    @Override
    public void onDisable() {
        this.state = ModuleState.UNLOADED;
        context.getLogger().info("§e[MyModule] 模块已禁用");
    }

    @Override
    public ModuleDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void setDescriptor(ModuleDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public ModuleState getState() {
        return state;
    }
}
```

**module.yml**
```yaml
id: my-first-module
name: "我的第一个模块"
version: 1.0.0
author: "YourName"
description: "这是一个示例模块"
type: gui
api-version: 1.0.0
main: com.example.guild.MyFirstModule
depends: []
soft-depends: []
permissions: []
```

---

## 核心概念

### 模块生命周期

每个模块都会经历以下状态转换：

```
UNLOADED → LOADING → ACTIVE → UNLOADING → UNLOADED
                 ↘ ERROR (加载失败时)
```

| 状态 | 说明 |
|------|------|
| **UNLOADED** | 未加载（初始/卸载后状态） |
| **LOADING** | 正在加载中 |
| **ACTIVE** | 已激活并正常运行 |
| **UNLOADING** | 正在卸载中 |
| **ERROR** | 加载或运行时出错 |

**关键方法**：

```java
public interface GuildModule {
    // 获取模块描述符
    ModuleDescriptor getDescriptor();

    // 设置模块描述符（由框架调用）
    void setDescriptor(ModuleDescriptor descriptor);

    // 模块启用时调用（在此初始化资源）
    void onEnable(ModuleContext context) throws Exception;

    // 模块禁用时调用（在此释放资源）
    void onDisable();

    // 获取当前状态
    ModuleState getState();
}
```

### 模块描述符

`ModuleDescriptor` 定义了模块的基本元信息：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | String | ✅ | 唯一标识符（小写字母、数字、连字符） |
| `name` | String | ✅ | 显示名称 |
| `version` | String | ✅ | 版本号（语义化版本） |
| `author` | String | ✅ | 作者名称 |
| `description` | String | ❌ | 模块描述 |
| `main` | String | ✅ | 主类全限定名 |
| `api-version` | String | ✅ | 所需的 API 版本 |
| `type` | String | ❌ | 模块类型：`gui` / `data` / `mixed`（默认 mixed） |
| `depends` | List\<String\> | ❌ | 硬性依赖的模块 ID 列表 |
| `softDepends` | List\<String\> | ❌ | 软性依赖的模块 ID 列表 |
| `configPrefix` | String | ❌ | 配置文件中的前缀（默认为 modules.{id}） |
| `sourceFile` | File | ❌ | 模块 JAR 文件引用（框架自动设置） |

### 模块上下文

`ModuleContext` 是模块运行时的核心环境对象，提供了所有必要的服务：

```java
public class ModuleContext {

    // ========== 核心服务 ==========

    // 获取主插件实例
    GuildPlugin getPlugin();

    // 获取 SDK API 门面
    GuildPluginAPI getApi();

    // 获取服务容器（高级功能）
    ServiceContainer getServiceContainer();

    // 获取事件总线
    EventBus getEventBus();

    // 获取 GUI 管理器
    GUIManager getGuiManager();

    // 获取语言管理器
    LanguageManager getLanguageManager();

    // ========== 模块特定服务 ==========

    // 获取模块描述符
    ModuleDescriptor getDescriptor();

    // 获取模块专属配置
    ModuleConfigSection getConfig();

    // 获取日志记录器
    Logger getLogger();

    // ========== 消息工具 ==========

    // 向玩家发送国际化消息
    void sendMessage(Player player, String key, Object... args);

    // 获取国际化消息文本
    String getMessage(String key, Object... args);

    // ========== 任务调度 ==========

    // 在主线程同步执行任务
    void runSync(Runnable task);

    // 在异步线程执行任务
    void runAsync(Runnable task);

    // 延迟执行任务（单位：tick）
    void runLater(long delayTicks, Runnable task);

    // 循环定时任务
    void runTimer(long delayTicks, long periodTicks, Runnable task);

    // ========== GUI 操作 ==========

    // 为玩家打开 GUI 界面
    void openGUI(Player player, GUI gui);

    // 导航到上一级界面
    boolean navigateBack(Player player);
}
```

---

## API 参考

### GuildPluginAPI

`GuildPluginAPI` 是 SDK 提供的主门面类，所有与主插件的交互都通过它进行。

#### 数据查询方法（异步）

所有数据查询方法都返回 `CompletableFuture`，支持异步非阻塞操作：

```java
// 根据 ID 获取公会数据
CompletableFuture<GuildData> getGuildById(int id);

// 根据名称获取公会数据
CompletableFuture<GuildData> getGuildByName(String name);

// 获取玩家所属的公会
CompletableFuture<GuildData> getPlayerGuild(UUID playerUuid);

// 获取所有公会列表
CompletableFuture<List<GuildData>> getAllGuilds();

// 获取公会的成员列表
CompletableFuture<List<MemberData>> getGuildMembers(int guildId);
```

**使用示例**：
```java
@Override
public void onEnable(ModuleContext context) throws Exception {
    GuildPluginAPI api = context.getApi();

    // 异步查询玩家公会
    api.getPlayerGuild(player.getUniqueId())
        .thenAccept(guild -> {
            if (guild != null) {
                context.runSync(() -> {
                    player.sendMessage("§a你的公会: " + guild.getName());
                });
            }
        });
}
```

#### GUI 扩展方法

在现有 GUI 中注册自定义按钮：

```java
// 注册 GUI 按钮（自动分配槽位）
void registerGUIButton(
    String guiType,           // 目标 GUI 类型名
    int slot,                 // 槽位编号（使用 GUIExtensionHook.AUTO_SLOT 自动分配）
    ItemStack item,           // 按钮物品
    String moduleId,          // 你的模块 ID
    GUIClickAction handler    // 点击处理器
);

// 注册完全自定义的 GUI
void registerCustomGUI(String guiId, ModuleGUIFactory factory);
void unregisterCustomGUI(String guiId);

// 打开自定义 GUI
void openCustomGUI(String guiId, Player player, Map<String, Object> data);
void openCustomGUI(String guiId, Player player);
```

#### 事件监听方法

```java
// 监听公会创建事件
void onGuildCreate(GuildEventHandler handler);

// 监听公会删除事件
void onGuildDelete(GuildEventHandler handler);

// 监听成员加入事件
void onMemberJoin(MemberEventHandler handler);

// 监听成员离开事件
void onMemberLeave(MemberEventHandler handler);
```

#### 命令注册方法

```java
// 注册子命令
void registerSubCommand(
    String parentCommand,     // 父命令（如 "guild"）
    String name,              // 子命令名称
    ModuleCommandHandler handler,  // 命令处理器
    String permission         // 权限节点（可为 null）
);
```

#### 货币 API

```java
// 获取货币管理器
CurrencyManager getCurrencyManager();

// 获取玩家的货币余额
double getCurrencyBalance(int guildId, UUID playerUuid, CurrencyManager.CurrencyType currencyType);

// 增加玩家的货币
boolean depositCurrency(int guildId, UUID playerUuid, String playerName, 
                     CurrencyManager.CurrencyType currencyType, double amount);

// 减少玩家的货币
boolean withdrawCurrency(int guildId, UUID playerUuid, 
                      CurrencyManager.CurrencyType currencyType, double amount);
```

#### HTTP 客户端

用于与外部服务通信：

```java
// GET 请求
CompletableFuture<String> httpGet(String url);
CompletableFuture<String> httpGet(String url, Map<String, String> headers);

// POST 请求
CompletableFuture<String> httpPost(
    String url,
    String body,
    Map<String, String> headers
);

// 获取 HTTP 客户端实例
HttpClientProvider getHttpClient();
```

### 数据模型

#### GuildData（公会数据）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | int | 公会唯一 ID |
| `name` | String | 公会名称 |
| `masterUuid` | UUID | 会长 UUID |
| `masterName` | String | 会长玩家名 |
| `level` | int | 公会等级 |
| `experience` | long | 经验值 |
| `balance` | double | 公会资金余额 |
| `memberCount` | int | 当前成员数 |
| `maxMembers` | int | 最大成员上限 |
| `motto` | String | 公会座右铭 |
| `createTime` | long | 创建时间戳 |
| `members` | List\<MemberData\> | 成员列表 |

#### MemberData（成员数据）

| 字段 | 类型 | 说明 |
|------|------|------|
| `playerUuid` | UUID | 玩家 UUID |
| `playerName` | String | 玩家名称 |
| `role` | String | 角色（LEADER/OFFICER/MEMBER） |
| `joinTime` | long | 加入时间戳 |
| `contribution` | double | 贡献值 |
| `online` | boolean | 是否在线 |

#### GuildEventData（公会事件数据）

| 字段 | 类型 | 说明 |
|------|------|------|
| `guildId` | int | 公会 ID |
| `guildName` | String | 公会名称 |
| `guildLeaderName` | String | 公会会长名称 |

#### MemberEventData（成员事件数据）

继承自 `GuildEventData`，额外包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `playerUuid` | UUID | 成员 UUID |
| `playerName` | String | 成员名称 |

### 事件系统

SDK 提供了函数式接口来处理事件：

```java
// 公会事件处理器
@FunctionalInterface
public interface GuildEventHandler {
    void onEvent(GuildEventData data);

    default Object getModuleInstance() { return null; }
}

// 成员事件处理器
@FunctionalInterface
public interface MemberEventHandler {
    void onEvent(MemberEventData data);

    default Object getModuleInstance() { return null; }
}
```

**重要**：建议实现 `getModuleInstance()` 方法返回模块自身，以便框架正确识别事件来源和进行清理。

**使用示例**：
```java
api.onMemberJoin(new MemberEventHandler() {
    @Override
    public void onEvent(MemberEventData data) {
        context.getLogger().info("玩家 " + data.getPlayerName() +
            " 加入了公会 " + data.getGuildName());

        // 执行你的业务逻辑...
    }

    @Override
    public Object getModuleInstance() {
        return MyModule.this;  // 返回模块实例
    }
});
```

### GUI 系统

#### AbstractModuleGUI（抽象 GUI 基类）

继承此基类可快速创建标准化的 GUI 界面：

```java
public abstract class AbstractModuleGUI implements GUI {

    // ========== 常量定义 ==========
    protected static final int DEFAULT_SIZE = 54;      // 默认 GUI 大小（6行）
    protected static final int CONTENT_START = 9;       // 内容区起始槽位
    protected static final int CONTENT_END = 44;        // 内容区结束槽位
    protected static final int COLUMNS = 7;             // 内容区列数
    protected static final int CONTENT_ROWS = 4;        // 内容区行数
    protected static final int PER_PAGE = COLUMNS * CONTENT_ROWS;  // 每页容量（28）

    // ========== 边框工具 ==========

    // 填充黑色玻璃边框
    protected void fillBorder(Inventory inv);

    // 填充内容区空槽位（灰色玻璃）
    protected void fillInteriorSlots(Inventory inv);

    // 将线性索引映射到实际槽位
    protected int mapToSlot(int linearIndex);  // 0-27 → 实际槽位号

    // ========== 物品创建工具 ==========

    // 创建带颜色代码的物品
    protected ItemStack createItem(Material material, String name, String... lore);

    // 创建返回按钮
    protected ItemStack createBackButton(String name, String hint);

    // ========== 分页工具 ==========

    // 设置翻页按钮
    protected void setupPagination(Inventory inv, int currentPage, int totalPages,
                                   String prevPageKey, String nextPageKey);

    // 计算总页数
    protected int getTotalPages(int totalItems);

    // ========== 生命周期方法 ==========

    @Override
    public int getSize();       // 返回 GUI 大小（默认 54）

    @Override
    public void onClose(Player player);  // 关闭时调用

    @Override
    public void refresh(Player player);   // 刷新界面
}
```

**布局说明**：

```
  0  1  2  3  4  5  6  7  8
┌──┬──┬──┬──┬──┬──┬──┬──┬──┐ ← 边框行（0-8）
│░░│ B │ C │ C │ C │ C │ C │ C │░░│
│░░│ C │ C │ C │ C │ C │ C │ C │░░│ ← 内容区（9-44）
│░░│ C │ C │ C │ C │ C │ C │ C │░░│     7列 × 4行 = 28个槽位
│░░│ C │ C │ C │ C │ C │ C │ C │░░│
│░░│ C │ C │ C │ C │ C │ C │ C │░░│
├──┼──┼──┼──┼──┼──┼──┼──┼──┤
│◀ │░░│░░│░░│░░│░░│░░│░░│▶ │ ← 底部边框（45-53）
└──┴──┴──┴──┴──┴──┴──┴──┴──┘
  B = 边框  C = 内容  ◀▶ = 翻页按钮
```

### 配置管理

#### ModuleConfigSection

模块专属配置管理器，自动隔离到 `modules.{moduleId}` 命名空间下：

```java
public class ModuleConfigSection {

    // 获取配置路径前缀
    String getConfigPath();  // 返回 "modules.your-module-id"

    // ========== 基础类型读取 ==========

    String getString(String key, String defaultValue);
    String getString(String key);
    int getInt(String key, int defaultValue);
    boolean getBoolean(String key, boolean defaultValue);
    long getLong(String key, long defaultValue);
    double getDouble(String key, double defaultValue);

    // ========== 复杂类型读取 ==========

    List<String> getStringList(String key);
    boolean contains(String key);
    ConfigurationSection getConfigSection();
}
```

**config.yml 中的结构**：
```yaml
modules:
  your-module-id:
    some-setting: "value"
    enable-feature: true
    max-items: 10
```

**访问方式**：
```java
ModuleConfigSection config = context.getConfig();

String name = config.getString("some-setting", "default");
boolean enabled = config.getBoolean("enable-feature", false);
int maxItems = config.getInt("max-items", 20);
```

### 命令系统

#### ModuleCommandHandler

函数式接口，用于处理自定义命令：

```java
@FunctionalInterface
public interface ModuleCommandHandler {
    void handle(CommandSender sender, String[] args);
}
```

**注册示例**：
```java
context.getApi().registerSubCommand(
    "guild",              // 父命令
    "mymodule",           // 子命令名称
    (sender, args) -> {   // 处理器
        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            sender.sendMessage("§a[MyModule] 这是我的模块！");
        } else {
            sender.sendMessage("§e用法: /guild mymodule info");
        }
    },
    "guild.mymodule.use"  // 权限（可选）
);
```

玩家可通过 `/guild mymodule info` 使用该命令。

### HTTP 客户端

#### HttpClientProvider

提供异步 HTTP 请求能力：

```java
public class HttpClientProvider {

    // GET 请求（无头）
    CompletableFuture<String> httpGet(String url);

    // GET 请求（带自定义头）
    CompletableFuture<String> httpGet(String url, Map<String, String> headers);

    // POST 请求
    CompletableFuture<String> httpPost(
        String url,
        String body,
        Map<String, String> headers
    );
}
```

**使用示例**：
```java
context.getApi().httpGet("https://api.example.com/data")
    .thenAccept(response -> {
        context.getLogger().info("收到响应: " + response);
        // 解析 JSON 并处理...
    })
    .exceptionally(ex -> {
        context.getLogger().severe("请求失败: " + ex.getMessage());
        return null;
    });
```

---

## 开发指南

### 注册 GUI 按钮

在现有的 GuildPlugin GUI 中注入自定义按钮是扩展界面的主要方式。

#### 可用的目标 GUI

| GUI 类型 | 说明 | 典型用途 |
|----------|------|----------|
| `GuildSettingsGUI` | 工会设置界面 | 管理功能入口 |
| `GuildInfoGUI` | 工会信息展示 | 信息查看入口 |
| `MainGuildGUI` | 主菜单 | 快捷功能入口 |
| `MemberManagementGUI` | 成员管理 | 成员相关操作 |

#### 自动分配槽位 vs 固定槽位

```java
import com.guild.core.module.hook.GUIExtensionHook;

// 方式一：自动分配槽位（推荐用于设置界面）
api.registerGUIButton(
    "GuildSettingsGUI",
    GUIExtensionHook.AUTO_SLOT,  // 自动寻找可用位置
    myButtonItem,
    "my-module",
    (player, ctx) -> { /* 点击处理 */ }
);

// 方式二：固定槽位（适用于信息展示界面）
api.registerGUIButton(
    "GuildInfoGUI",
    12,                        // 固定槽位 12
    myInfoButton,
    "my-module",
    (player, ctx) -> { /* 点击处理 */ }
);
```

**GUIClickAction 接口**：
```java
@FunctionalInterface
public interface GUIClickAction {
    void execute(Player player, Object... context);
}
```

**context 参数**通常包含当前关联的 `Guild` 对象，可用于获取工会信息。

### 监听事件

#### 公会级别事件

```java
// 公会创建时触发
api.onGuildCreate(new GuildEventHandler() {
    @Override
    public void onEvent(GuildEventData data) {
        context.getLogger().info("新公会创建: " + data.getGuildName()
            + " (ID: " + data.getGuildId() + ")");
        // 初始化该公会的模块数据...
    }

    @Override
    public Object getModuleInstance() {
        return MyModule.this;
    }
});

// 公会解散时触发
api.onGuildDelete(new GuildEventHandler() {
    @Override
    public void onEvent(GuildEventData data) {
        context.getLogger().info("公会已删除: " + data.getGuildName());
        // 清理该公会的模块数据...
    }

    @Override
    public Object getModuleInstance() {
        return MyModule.this;
    }
});
```

#### 成员级别事件

```java
// 成员加入公会时触发
api.onMemberJoin(new MemberEventHandler() {
    @Override
    public void onEvent(MemberEventData data) {
        context.getLogger().info(data.getPlayerName()
            + " 加入了公会 " + data.getGuildName());
        // 为成员初始化数据...
    }

    @Override
    public Object getModuleInstance() {
        return MyModule.this;
    }
});

// 成员离开/被踢出时触发
api.onMemberLeave(new MemberEventHandler() {
    @Override
    public void onEvent(MemberEventData data) {
        context.getLogger().info(data.getPlayerName()
            + " 离开了公会 " + data.getGuildName());
        // 清理成员数据...
    }

    @Override
    public Object getModuleInstance() {
        return MyModule.this;
    }
});
```

### 自定义 GUI 界面

#### 完整示例

```java
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MyCustomGUI extends AbstractModuleGUI {

    private final MyModule module;
    private final Guild guild;

    public MyCustomGUI(MyModule module, Guild guild, Player player) {
        this.module = module;
        this.guild = guild;

        // 创建 54 格库存
        this.inventory = Bukkit.createInventory(null, getSize(),
            ColorUtils.colorize("&6&l我的自定义界面"));
    }

    @Override
    public void setupInventory(Inventory inv) {
        // 1. 填充边框
        fillBorder(inv);

        // 2. 填充内容区空白
        fillInteriorSlots(inv);

        // 3. 添加标题/信息物品
        inv.setItem(4, createItem(Material.BOOK,
            "&e&l" + guild.getName(),
            "&7等级: &f" + guild.getLevel(),
            "&7成员: &f" + guild.getMemberCount() + "/" + guild.getMaxMembers()));

        // 4. 添加功能按钮
        inv.setItem(19, createItem(Material.DIAMOND,
            "&a&l功能一",
            "&7点击执行操作 A"));

        inv.setItem(21, createItem(Material.GOLD_INGOT,
            "&e&l功能二",
            "&7点击执行操作 B"));

        inv.setItem(23, createItem(Material.IRON_INGOT,
            "&7&l功能三",
            "&7点击执行操作 C"));

        // 5. 添加返回按钮
        inv.setItem(40, createBackButton("&c&l返回", "&7点击返回上级菜单"));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem) {
        switch (slot) {
            case 19:
                player.sendMessage("§a你点击了功能一！");
                // 执行操作 A...
                break;
            case 21:
                player.sendMessage("§e你点击了功能二！");
                // 执行操作 B...
                break;
            case 23:
                player.sendMessage("§7你点击了功能三！");
                // 执行操作 C...
                break;
            case 40:
                // 返回上一级
                module.getContext().navigateBack(player);
                break;
        }
    }
}
```

#### 分页实现示例

```java
public class PaginatedListGUI extends AbstractModuleGUI {

    private final List<ItemStack> items;
    private int currentPage = 1;

    public PaginatedListGUI(List<ItemStack> items) {
        this.items = items;
        this.inventory = Bukkit.createInventory(null, getSize(),
            ColorUtils.colorize("&6&l分页列表"));
    }

    @Override
    public void setupInventory(Inventory inv) {
        fillBorder(inv);
        fillInteriorSlots(inv);

        // 计算分页
        int totalItems = items.size();
        int totalPages = getTotalPages(totalItems);
        int startIndex = (currentPage - 1) * PER_PAGE;
        int endIndex = Math.min(startIndex + PER_PAGE, totalItems);

        // 填充当前页的内容
        for (int i = startIndex; i < endIndex; i++) {
            int slot = mapToSlot(i - startIndex);
            if (slot != -1) {
                inv.setItem(slot, items.get(i));
            }
        }

        // 设置翻页按钮
        setupPagination(inv, currentPage, totalPages,
            "&e&l上一页", "&e&l下一页");

        // 显示页码信息
        inv.setItem(49, createItem(Material.PAPER,
            "&7第 &f" + currentPage + " &7/ &f" + totalPages + " &7页",
            "&7共 &f" + totalItems + " &7项"));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack item) {
        int totalPages = getTotalPages(items.size());

        if (slot == 45 && currentPage > 1) {
            // 上一页
            currentPage--;
            refresh(player);
        } else if (slot == 53 && currentPage < totalPages) {
            // 下一页
            currentPage++;
            refresh(player);
        }
    }
}
```

### 数据持久化

模块应自行管理数据存储。推荐使用 JSON 或 YAML 格式存储到插件的数据目录：

```java
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.concurrent.ConcurrentHashMap;

public class MyDataManager {

    private final File dataDir;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ConcurrentHashMap<String, MyData> cache = new ConcurrentHashMap<>();

    public MyDataManager(File dataDir, Logger logger) {
        this.dataDir = dataDir;
        this.logger = logger;
        // 确保目录存在
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    // 加载所有数据
    public void loadAll() {
        if (!dataDir.isDirectory()) return;

        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                MyData data = gson.fromJson(reader, MyData.class);
                cache.put(data.getId(), data);
                logger.info("加载数据: " + file.getName());
            } catch (Exception e) {
                logger.warning("无法加载文件 " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    // 保存所有数据
    public void saveAll() {
        for (MyData data : cache.values()) {
            save(data);
        }
    }

    // 保存单条数据
    public void save(MyData data) {
        File file = new File(dataDir, data.getId() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (Exception e) {
            logger.severe("保存数据失败: " + e.getMessage());
        }
    }

    // CRUD 操作...
    public MyData get(String id) { return cache.get(id); }
    public void put(MyData data) { cache.put(data.getId(), data); }
    public void remove(String id) { cache.remove(id); }
    public Collection<MyData> getAll() { return cache.values(); }
}

// 数据模型类
class MyData {
    private String id;
    private String value;
    private long timestamp;

    // 构造函数、getter/setter...
}
```

**使用方式**：
```java
@Override
public void onEnable(ModuleContext context) throws Exception {
    File dataDir = new File(context.getPlugin().getDataFolder(),
        "data" + File.separator + "my-module");

    MyDataManager dataManager = new MyDataManager(dataDir, context.getLogger());
    dataManager.loadAll();
    // 数据将被保存到 plugins/GuildPlugin/data/my-module/
}
```

### 多语言支持

利用 `ModuleContext` 的消息系统支持多语言：

**1. 在 messages_zh.yml / messages_en.yml 中添加翻译**

```yaml
# messages_zh.yml
modules:
  my-module:
    loaded: "§a[我的模块] 模块已成功加载！"
    unloaded: "§e[我的模块] 模块已关闭"
    button-name: "我的功能"
    button-desc: "点击打开功能界面"
    error-no-permission: "§c你没有权限执行此操作"

# messages_en.yml
modules:
  my-module:
    loaded: "&a[MyModule] Module loaded successfully!"
    unloaded: "&e[MyModule] Module disabled"
    button-name: "My Feature"
    button-desc: "Click to open feature panel"
    error-no-permission: "&cYou don't have permission to do this"
```

**2. 在代码中使用**

```java
@Override
public void onEnable(ModuleContext context) throws Exception {
    // 发送带参数的消息
    context.sendMessage(player, "modules.my-module.loaded");

    // 获取消息文本
    String msg = context.getMessage("modules.my-module.button-name");
    // 结果根据服务器语言设置返回对应翻译

    // 带占位符的消息
    String formatted = context.getMessage(
        "modules.my-module.some-message",
        "参数1",  // {0}
        "参数2"   // {1}
    );

    // 日志输出
    context.getLogger().info(context.getMessage(
        "modules.my-module.loaded",
        "额外信息"
    ));
}
```

---

## 示例模块

### 模块概览

| 模块名 | 功能描述 | 复杂度 | SDK 能力覆盖 |
|--------|----------|:------:|:----------:|
| AnnouncementModule | 公告发布与管理 | ★★☆☆☆ | GUI + 持久化 + 多语言 |
| MemberRankModule | 成员贡献值排名与在线追踪 | ★★★☆☆ | 成员事件 + GUI + 定时任务 |
| GuildStatsModule | 多维度数据统计面板 (16文件, ~2400行) | ★★★★☆ | 数据查询 + HTTP + 异步 + 命令 + 事件 + ServiceContainer + CustomGUI |
| **QuestModule** | 任务/任务系统 (开发中) | ★★★★★ | 状态机 + 进度追踪 + 奖励发放 + EventBus |

### 公告模块 (AnnouncementModule)

**功能概述**：
- 在 GuildSettingsGUI 注入"插件公告"管理按钮
- 在 GuildInfoGUI 注入"告示牌"查看按钮
- 支持公告的创建、编辑、删除
- 完整的 GUI 界面交互

**关键代码片段**：

```java
public class AnnouncementModule implements GuildModule {

    private ModuleContext context;
    private AnnouncementManager announcementManager;

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;

        // 初始化数据管理器
        File dataDir = new File(context.getPlugin().getDataFolder(),
            "data" + File.separator + "announcements");
        this.announcementManager = new AnnouncementManager(dataDir, context.getLogger());
        announcementManager.loadAll();

        GuildPluginAPI api = context.getApi();

        // 注册设置界面按钮（自动槽位）
        ItemStack settingsButton = createSettingsButton();
        api.registerGUIButton("GuildSettingsGUI",
            GUIExtensionHook.AUTO_SLOT,
            settingsButton,
            "announcement",
            (player, ctx) -> handleOpenAnnouncementList(player, ctx));

        // 注册信息界面按钮（固定槽位 12）
        ItemStack infoButton = createInfoButton();
        api.registerGUIButton("GuildInfoGUI",
            12,
            infoButton,
            "announcement",
            (player, ctx) -> handleOpenAnnouncementView(player, ctx));
    }

    @Override
    public void onDisable() {
        // 保存并清理数据
        if (announcementManager != null) {
            announcementManager.saveAll();
            announcementManager.clearAll();
        }
    }
}
```

**完整源码位置**：
- [AnnouncementModule.java](../guild-plugin/src/main/java/com/guild/module/example/announcement/AnnouncementModule.java)
- [AnnouncementManager.java](../guild-plugin/src/main/java/com/guild/module/example/announcement/AnnouncementManager.java)
- [Announcement.java](../guild-plugin/src/main/java/com/guild/module/example/announcement/Announcement.java)
- GUI 类位于 [gui/](../guild-plugin/src/main/java/com/guild/module/example/announcement/gui/) 目录

### 成员排名模块 (MemberRankModule)

**功能概述**：
- 在 GuildSettingsGUI 注入"贡献排名"管理按钮
- 在 GuildInfoGUI 注入"排行榜"查看按钮
- 监听成员加入/离开事件自动维护排名数据
- 在线时长追踪与贡献值计算
- 支持管理员手动调整贡献值

**关键特性**：

```java
@Override
public void onEnable(ModuleContext context) throws Exception {
    this.context = context;

    // 初始化数据管理器和活动追踪器
    File dataDir = new File(context.getPlugin().getDataFolder(),
        "data" + File.separator + "member-ranks");
    this.rankManager = new MemberRankManager(dataDir, context.getLogger());
    rankManager.loadAll();

    this.onlineActivityTracker = new OnlineActivityTracker(this);
    onlineActivityTracker.start();  // 启动在线追踪

    GuildPluginAPI api = context.getApi();

    // 监听成员加入事件 —— 自动创建排名记录
    api.onMemberJoin(new MemberEventHandler() {
        @Override
        public void onEvent(MemberEventData data) {
            rankManager.getOrCreate(
                data.getGuildId(),
                data.getPlayerUuid(),
                data.getPlayerName()
            );
        }

        @Override
        public Object getModuleInstance() {
            return MemberRankModule.this;
        }
    });

    // 监听成员离开事件 —— 移除排名记录
    api.onMemberLeave(new MemberEventHandler() {
        @Override
        public void onEvent(MemberEventData data) {
            rankManager.removeMember(data.getGuildId(), data.getPlayerUuid());
        }

        @Override
        public Object getModuleInstance() {
            return MemberRankModule.this;
        }
    });

    // 注册 GUI 按钮...
}
```

**完整源码位置**：
- [MemberRankModule.java](../guild-plugin/src/main/java/com/guild/module/example/member/rank/MemberRankModule.java)
- [MemberRankManager.java](../guild-plugin/src/main/java/com/guild/module/example/member/rank/MemberRankManager.java)
- [OnlineActivityTracker.java](../guild-plugin/src/main/java/com/guild/module/example/member/rank/OnlineActivityTracker.java)
- [MemberRank.java](../guild-plugin/src/main/java/com/guild/module/example/member/rank/MemberRank.java)

---

## module.yml 配置

每个模块必须包含 `module.yml` 描述文件，放在 JAR 的根目录下：

```yaml
# ===== 必填字段 =====

# 模块唯一标识符（只能包含小写字母、数字、连字符）
id: your-module-id

# 显示名称
name: "Your Module Name"

# 版本号（推荐语义化版本 x.y.z）
version: 1.0.0

# 作者名称
author: "Your Name"

# 主类的全限定类名
main: com.example.guild.YourModuleClass

# 所需的最低 API 版本
api-version: 1.0.0

# ===== 可选字段 =====

# 模块描述
description: "模块的功能描述文字"

# 模块类型
# - gui: 主要提供 GUI 功能
# - data: 主要处理后端逻辑
# - mixed: 混合型（默认）
type: gui

# 硬性依赖（这些模块必须先于本模块加载）
depends:
  - some-other-module

# 软性依赖（如果存在则优先加载，不存在也不影响）
soft-depends:
  - optional-module

# 自定义权限节点列表
permissions:
  - guild.admin.your-module.manage
  - guild.user.your-module.use

# 配置键前缀（默认为 modules.{id}）
# config-prefix: custom-prefix
```

**验证清单**：

- [ ] `id` 全局唯一且格式正确
- [ ] `main` 指向正确的类路径
- [ ] `api-version` 与目标 GuildPlugin 版本兼容
- [ ] 所有 `depends` 中的模块确实存在
- [ ] `permissions` 遵循命名规范 `guild.{scope}.{module}.{action}`

---

## 构建与打包

### Maven 配置

确保你的 `pom.xml` 正确配置：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>17</source>
                <target>17</target>
            </configuration>
        </plugin>

        <!-- 将 resources 目录下的文件打入 JAR -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-resources-plugin</artifactId>
            <version>3.3.1</version>
            <executions>
                <execution>
                    <id>copy-resources</id>
                    <phase>process-resources</phase>
                    <goals>
                        <goal>copy-resources</goal>
                    </goals>
                    <configuration>
                        <outputDirectory>${project.build.outputDirectory}</outputDirectory>
                        <resources>
                            <resource>
                                <directory>${project.basedir}/src/main/resources</directory>
                                <includes>
                                    <include>module.yml</include>
                                </includes>
                            </resource>
                        </resources>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>

    <!-- 确保 module.yml 被包含在 JAR 中 -->
    <resources>
        <resource>
            <directory>${project.basedir}/src/main/resources</directory>
            <includes>
                <include>**/*</include>
            </includes>
        </resource>
    </resources>
</build>
```

### 项目结构

```
your-module/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── guild/
│       │               ├── YourModule.java        # 主类
│       │               ├── gui/                   # GUI 界面
│       │               │   └── YourGUI.java
│       │               ├── manager/               # 数据管理
│       │               │   └── YourDataManager.java
│       │               └── model/                 # 数据模型
│       │                   └── YourData.java
│       └── resources/
│           └── module.yml                         # ⚠️ 必须存在
└── target/
    └── your-module-1.0.0.jar                      # 最终产物
```

### 构建命令

```bash
# 编译并打包
mvn clean package

# 输出: target/your-module-1.0.0.jar
```

### 安装模块

1. 将生成的 JAR 文件复制到服务器的 `plugins/GuildPlugin/modules/` 目录
2. 重启服务器或使用 `/guildadmin reload` 命令重载模块
3. 检查控制台日志确认模块是否成功加载

```bash
# Linux/macOS
cp target/your-module-1.0.0.jar /path/to/server/plugins/GuildPlugin/modules/

# Windows
copy target\your-module-1.0.0.jar \path\to\server\plugins\GuildPlugin\modules\
```

---

## 最佳实践

### ✅ 推荐做法

1. **资源管理**
   ```java
   @Override
   public void onEnable(ModuleContext context) throws Exception {
       // ✓ 在 onEnable 中初始化资源
       this.dataManager = new DataManager(...);
       dataManager.loadAll();
   }

   @Override
   public void onDisable() {
       // ✓ 在 onDisable 中释放资源
       if (dataManager != null) {
           dataManager.saveAll();
           dataManager.clearAll();
       }
   }
   ```

2. **异步操作**
   ```java
   // ✓ 使用异步避免阻塞主线程
   api.getPlayerGuild(player.getUniqueId())
       .thenAccept(guild -> {
           context.runSync(() -> {
               // 回到主线程更新 UI
               player.sendMessage("公会: " + guild.getName());
           });
       });
   ```

3. **事件监听器标识**
   ```java
   api.onMemberJoin(new MemberEventHandler() {
       @Override
       public void onEvent(MemberEventData data) { ... }

       @Override
       public Object getModuleInstance() {
           // ✓ 始终返回模块实例
           return MyModule.this;
       }
   });
   ```

4. **错误处理**
   ```java
   @Override
   public void onEnable(ModuleContext context) throws Exception {
       try {
           // 可能失败的操作
           riskyOperation();
       } catch (Exception e) {
           // ✓ 记录详细错误信息
           context.getLogger().severe("模块初始化失败: " + e.getMessage());
           e.printStackTrace();
           throw e;  // 重新抛出让框架知道加载失败
       }
   }
   ```

5. **配置默认值**
   ```java
   // ✓ 总是提供合理的默认值
   int maxItems = context.getConfig().getInt("max-items", 20);
   boolean debug = context.getConfig().boolean("debug-mode", false);
   ```

### ❌ 应避免

1. **阻塞主线程**
   ```java
   // ✗ 不要在主线程进行耗时操作
   @Override
   public void onEnable(ModuleContext context) {
       Thread.sleep(5000);  // 会冻结整个服务器！
   }
   ```

2. **忘记释放资源**
   ```java
   @Override
   public void onDisable() {
       // ✗ 内存泄漏！
       // dataManager.saveAll();  // 忘记调用
   }
   ```

3. **硬编码字符串**
   ```java
   // ✗ 不利于国际化和维护
   player.sendMessage("§a操作成功");

   // ✓ 使用消息系统
   context.sendMessage(player, "module.success");
   ```

4. **忽略异常**
   ```java
   // ✗ 吞掉异常会导致难以排查的问题
   try {
       doSomething();
   } catch (Exception e) {
       // 空的 catch 块
   }

   // ✓ 至少记录日志
   try {
       doSomething();
   } catch (Exception e) {
       context.getLogger().warning("操作失败: " + e.getMessage());
   }
   ```

5. **直接修改其他模块的数据**
   ```java
   // ✗ 不要越权访问
   otherModule.getDataManager().modifyData(...);

   // ✓ 通过 API 和事件系统交互
   api.onGuildCreate(handler);  // 监听事件响应
   ```

---

## 常见问题

### Q1: 模块加载失败怎么办？

**检查步骤**：
1. 查看 `module.yml` 是否存在且格式正确
2. 确认 `main` 类路径正确
3. 检查 `api-version` 是否兼容
4. 查看服务器控制台的详细错误堆栈

**常见原因**：
- 缺少 `module.yml` 文件
- `main` 类找不到或不是 `GuildModule` 的实现
- JDK 版本不匹配（需要 17+）
- 依赖的其他模块未加载

### Q2: 如何调试模块？

```java
// 开启详细日志
@Override
public void onEnable(ModuleContext context) throws Exception {
    // 在关键位置添加日志
    context.getLogger().info("[DEBUG] 开始初始化...");
    context.getLogger().info("[DEBUG] 配置值: " + context.getConfig().getString("key"));

    // 使用调试模式开关
    if (context.getConfig().getBoolean("debug", false)) {
        context.getLogger().warning("[DEBUG] 调试模式已开启");
    }
}
```

### Q3: GUI 按钮没有显示？

**可能原因及解决方案**：

1. **槽位冲突**：其他模块已占用该槽位
   - 解决：使用 `GUIExtensionHook.AUTO_SLOT` 让系统自动分配

2. **目标 GUI 名称错误**
   - 解决：确认使用正确的 GUI 类型名（如 `"GuildSettingsGUI"`）

3. **权限不足**
   - 解决：检查玩家是否有查看该 GUI 的基础权限

### Q4: 如何让模块支持热重载？

目前模块不支持真正的热重载。修改模块后需要：
1. 替换 JAR 文件
2. 使用 `/guildadmin reload` 或重启服务器

**注意**：`onDisable()` 会被调用以清理资源，然后重新加载。

### Q5: 模块之间如何通信？

**推荐方式**：

1. **通过事件系统**（松耦合）
   ```java
   // 模块A 发布事件
   api.onGuildCreate(handler);

   // 模块B 响应事件
   ```

2. **通过共享数据文件**（文件系统）
   ```
   plugins/GuildPlugin/data/shared/
   ```

3. **通过 GuildPlugin API 查询数据**（只读）
   ```java
   api.getGuildById(guildId).thenAccept(guild -> { ... });
   ```

**不推荐**：直接获取其他模块的实例进行方法调用（强耦合，脆弱）。

### Q6: 如何发布模块到社区？

1. 确保 `module.yml` 信息完整准确
2. 编写清晰的 README.md（包含截图更好）
3. 在 GitHub Releases 发布带版本标签的 JAR
4. 可以提交到 [GuildPlugin 模块市场](https://github.com/chenasyd/-GuildPlugin)（规划中）
5. 使用语义化版本号（Semantic Versioning）

### Q7: 性能优化建议？

1. **缓存频繁查询的数据**
   ```java
   private final ConcurrentHashMap<Integer, GuildData> guildCache = new ConcurrentHashMap<>();
   ```

2. **批量操作数据库**
   ```java
   // ✗ 逐条保存
   for (Data d : dataList) save(d);

   // ✓ 批量保存
   saveAll(dataList);
   ```

3. **合理使用异步**
   ```java
   // I/O 密集型操作使用异步
   context.runAsync(() -> heavyIOOperation());

   // Bukkit API 调用必须同步
   context.runSync(() -> player.openInventory(gui));
   ```

4. **避免频繁刷新 GUI**
   ```java
   // ✗ 每秒刷新 20 次
   scheduler.runTaskTimer(() -> gui.refresh(player), 0L, 1L);

   // ✓ 按需刷新或在数据变化时刷新
   gui.refresh(player);  // 仅当数据变化时调用
   ```

---

## 相关链接

- **GitHub 仓库**: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- **问题反馈**: [GitHub Issues](https://github.com/chenasyd/-GuildPlugin/issues)
- **SDK 下载**: [Releases 页面](https://github.com/chenasyd/-GuildPlugin/releases)
- **许可证**: GNU GPL v3.0

---

> 💡 **提示**：如果你在开发过程中遇到问题，欢迎在 GitHub Issues 提问，或加入社区讨论。祝开发愉快！
