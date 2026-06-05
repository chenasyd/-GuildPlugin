# GuildPlugin SDK Developer Guide

This guide covers developing external modules for GuildPlugin using the SDK. Modules are loaded dynamically from `plugins/GuildPlugin/modules/` and support hot-reload via `/guildmodule`.

## Quick Start

### Requirements

| Item | Requirement |
|:----:|:-----------:|
| JDK | 17+ |
| Maven | 3.6+ |
| SDK | `com.guild:guild-sdk:1.3.6` (provided scope) |

### Project Setup

**pom.xml:**

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-guild-module</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.guild</groupId>
            <artifactId>guild-sdk</artifactId>
            <version>1.3.6</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.spigotmc</groupId>
            <artifactId>spigot-api</artifactId>
            <version>1.21-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

**Install the SDK:**

```bash
# From source (recommended)
git clone https://github.com/chenasyd/-GuildPlugin.git
cd GuildPlugin
mvn clean install -pl guild-sdk
```

**module.yml** (place at JAR root):

```yaml
id: my-first-module
name: "My First Module"
version: 1.0.0
author: "YourName"
description: "A simple example module"
main: com.example.guild.MyFirstModule
api-version: 1.0.0
type: mixed
depends: []
soft-depends: []
```

**Module main class:**

```java
package com.example.guild;

import com.guild.core.module.*;
import com.guild.sdk.GuildPluginAPI;

public class MyFirstModule implements GuildModule {

    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;

        GuildPluginAPI api = context.getApi();
        context.getLogger().info("[MyModule] Enabled");
    }

    @Override
    public void onDisable() {
        this.state = ModuleState.UNLOADED;
        context.getLogger().info("[MyModule] Disabled");
    }

    @Override
    public ModuleDescriptor getDescriptor() { return descriptor; }

    @Override
    public void setDescriptor(ModuleDescriptor descriptor) { this.descriptor = descriptor; }

    @Override
    public ModuleState getState() { return state; }
}
```

## Core Concepts

### Module Lifecycle

```
UNLOADED -> LOADING -> ACTIVE -> UNLOADING -> UNLOADED
                \-> ERROR (load failure)
```

| State | Description |
|:-----:|:-----------:|
| UNLOADED | Not loaded |
| LOADING | Loading in progress |
| ACTIVE | Running |
| UNLOADING | Disabling |
| ERROR | Load/runtime error |

### ModuleDescriptor Fields

| Field | Required | Description |
|:-----:|:--------:|:-----------:|
| `id` | Yes | Unique identifier (lowercase, numbers, hyphens) |
| `name` | Yes | Display name |
| `version` | Yes | Semantic version |
| `author` | Yes | Author name |
| `main` | Yes | Fully qualified main class |
| `api-version` | Yes | Required API version |
| `description` | No | Module description |
| `type` | No | `gui`, `data`, or `mixed` (default) |
| `depends` | No | Hard dependency module IDs |
| `softDepends` | No | Optional dependency module IDs |
| `configPrefix` | No | Config section prefix (default: `modules.{id}`) |

### ModuleContext

`ModuleContext` is the runtime environment object passed during `onEnable()`. Key methods:

```java
// Core services
GuildPlugin getPlugin();
GuildPluginAPI getApi();
ServiceContainer getServiceContainer();
EventBus getEventBus();
GUIManager getGuiManager();
LanguageManager getLanguageManager();

// Module-specific
ModuleDescriptor getDescriptor();
ModuleConfigSection getConfig();
Logger getLogger();

// Messaging (i18n-aware)
void sendMessage(Player player, String key, Object... args);
String getMessage(String key, Object... args);

// Scheduler (Folia/Spigot compatible)
void runSync(Runnable task);
void runAsync(Runnable task);
void runLater(long delayTicks, Runnable task);
void runTimer(long delayTicks, long periodTicks, Runnable task);

// GUI
void openGUI(Player player, GUI gui);
boolean navigateBack(Player player);
```

## API Reference

### GuildPluginAPI

Gateway for all interactions with the core plugin. Access via `context.getApi()`.

#### Data Queries (all async, return `CompletableFuture`)

```java
CompletableFuture<GuildData> getGuildById(int id);
CompletableFuture<GuildData> getGuildByName(String name);
CompletableFuture<GuildData> getPlayerGuild(UUID playerUuid);
CompletableFuture<List<GuildData>> getAllGuilds();
CompletableFuture<List<MemberData>> getGuildMembers(int guildId);
```

**Usage:**
```java
api.getPlayerGuild(player.getUniqueId())
    .thenAccept(guild -> {
        if (guild != null) {
            context.runSync(() -> player.sendMessage("Guild: " + guild.getName()));
        }
    });
```

#### GUI Extension

```java
// Register button in existing GUI
void registerGUIButton(String guiType, int slot, ItemStack item,
                       String moduleId, GUIClickAction handler);

// Custom GUI
void registerCustomGUI(String guiId, ModuleGUIFactory factory);
void unregisterCustomGUI(String guiId);
void openCustomGUI(String guiId, Player player, Map<String, Object> data);
void openCustomGUI(String guiId, Player player);
```

Use `GUIExtensionHook.AUTO_SLOT` for automatic slot assignment.

Available target GUIs: `GuildSettingsGUI`, `GuildInfoGUI`, `MainGuildGUI`, `MemberManagementGUI`

#### Events

```java
void onGuildCreate(GuildEventHandler handler);
void onGuildDelete(GuildEventHandler handler);
void onMemberJoin(MemberEventHandler handler);
void onMemberLeave(MemberEventHandler handler);
```

Important: implement `getModuleInstance()` in your handler to return the module instance for proper cleanup on unload.

#### Commands

```java
void registerSubCommand(String parentCommand, String name,
                        ModuleCommandHandler handler, String permission);
```

#### HTTP Client

```java
CompletableFuture<String> httpGet(String url);
CompletableFuture<String> httpGet(String url, Map<String, String> headers);
CompletableFuture<String> httpPost(String url, String body, Map<String, String> headers);
HttpClientProvider getHttpClient();
```

#### Currency

```java
CurrencyManager getCurrencyManager();
double getCurrencyBalance(int guildId, UUID playerUuid, CurrencyType type);
boolean depositCurrency(int guildId, UUID playerUuid, String name, CurrencyType type, double amount);
boolean withdrawCurrency(int guildId, UUID playerUuid, CurrencyType type, double amount);
```

### Data Models

#### GuildData

| Field | Type | Description |
|:-----:|:----:|:-----------:|
| `id` | int | Guild ID |
| `name` | String | Guild name |
| `masterUuid` | UUID | Leader UUID |
| `masterName` | String | Leader name |
| `level` | int | Guild level |
| `balance` | double | Guild funds |
| `memberCount` | int | Current members |
| `maxMembers` | int | Max members |
| `motto` | String | Guild motto/description |
| `createTime` | long | Creation timestamp |
| `members` | List\<MemberData\> | Member list |

#### MemberData

| Field | Type | Description |
|:-----:|:----:|:-----------:|
| `playerUuid` | UUID | Player UUID |
| `playerName` | String | Player name |
| `role` | String | LEADER / OFFICER / MEMBER |
| `joinTime` | long | Join timestamp |
| `contribution` | double | Contribution value |
| `online` | boolean | Online status |

### Event Interfaces

```java
@FunctionalInterface
public interface GuildEventHandler {
    void onEvent(GuildEventData data);
    default Object getModuleInstance() { return null; }
}

@FunctionalInterface
public interface MemberEventHandler {
    void onEvent(MemberEventData data);
    default Object getModuleInstance() { return null; }
}
```

### ModuleConfigSection

Module configuration is namespaced under `modules.{moduleId}` in `config.yml`.

```yaml
# config.yml
modules:
  my-module:
    api-key: "your-key"
    max-results: 10
    enable-feature: true
```

```java
ModuleConfigSection config = context.getConfig();
String apiKey = config.getString("api-key", "");
int maxResults = config.getInt("max-results", 10);
boolean enabled = config.getBoolean("enable-feature", false);
List<String> list = config.getStringList("some-list");
boolean hasKey = config.contains("some-key");
```

## Development Guide

### GUI Button Registration

```java
// Auto slot (recommended for settings)
api.registerGUIButton("GuildSettingsGUI",
    GUIExtensionHook.AUTO_SLOT, buttonItem,
    "my-module",
    (player, ctx) -> { /* handle click */ });

// Fixed slot (for info displays)
api.registerGUIButton("GuildInfoGUI",
    12, infoButton,
    "my-module",
    (player, ctx) -> { /* handle click */ });
```

### Event Listening

```java
api.onMemberJoin(new MemberEventHandler() {
    @Override
    public void onEvent(MemberEventData data) {
        context.getLogger().info(data.getPlayerName() + " joined " + data.getGuildName());
    }

    @Override
    public Object getModuleInstance() {
        return MyModule.this;
    }
});

api.onMemberLeave(new MemberEventHandler() {
    @Override
    public void onEvent(MemberEventData data) {
        context.getLogger().info(data.getPlayerName() + " left " + data.getGuildName());
    }

    @Override
    public Object getModuleInstance() {
        return MyModule.this;
    }
});
```

### Custom GUI with AbstractModuleGUI

Extend `AbstractModuleGUI` for standardized GUI creation with built-in border, pagination, and item utilities.

```java
public class MyCustomGUI extends AbstractModuleGUI {

    public MyCustomGUI(Player player, Guild guild) {
        this.inventory = Bukkit.createInventory(null, getSize(),
            "My Custom GUI");
    }

    @Override
    public void setupInventory(Inventory inv) {
        fillBorder(inv);
        fillInteriorSlots(inv);

        inv.setItem(4, createItem(Material.BOOK,
            "&e" + guild.getName(),
            "&7Level: &f" + guild.getLevel()));

        inv.setItem(19, createItem(Material.DIAMOND,
            "&aFeature A", "&7Click to use"));

        inv.setItem(40, createBackButton("&cBack", "&7Return"));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack item) {
        switch (slot) {
            case 19: /* handle */ break;
            case 40: context.navigateBack(player); break;
        }
    }
}
```

### Data Persistence

Modules should manage their own data storage. Recommended: JSON files under `plugins/GuildPlugin/data/{module-name}/`.

```java
File dataDir = new File(context.getPlugin().getDataFolder(),
    "data" + File.separator + "my-module");
if (!dataDir.exists()) dataDir.mkdirs();

// Use Gson for serialization
Gson gson = new GsonBuilder().setPrettyPrinting().create();
```

### Thread Safety

| Operation | Thread | Method |
|:---------:|:------:|:------:|
| Bukkit API (GUI, entities) | Main | `context.runSync()` |
| HTTP, file I/O | Async | `context.runAsync()` |

Always use `context.runSync()` / `context.runAsync()` instead of `Bukkit.getScheduler()` for Folia compatibility.

## module.yml Reference

```yaml
id: your-module-id              # Required: unique identifier
name: "Your Module Name"        # Required: display name
version: 1.0.0                  # Required: semantic version
author: "Your Name"             # Required
main: com.example.YourModule    # Required: fully qualified main class
api-version: 1.0.0              # Required: minimum API version

description: "Brief description" # Optional
type: mixed                      # Optional: gui | data | mixed
depends: []                      # Optional: hard dependencies
soft-depends: []                 # Optional: soft dependencies
config-prefix: your-module       # Optional: config section prefix
permissions:                     # Optional: custom permission nodes
  - guild.admin.your-module.manage
```

## Build & Deploy

### Project Structure

```
my-module/
├── pom.xml
├── src/main/java/com/example/guild/
│   ├── YourModule.java
│   ├── gui/
│   ├── manager/
│   └── model/
└── src/main/resources/
    └── module.yml    (required at JAR root)
```

### Build

```bash
mvn clean package
# Output: target/your-module-1.0.0.jar
```

### Deploy

Copy the JAR to `plugins/GuildPlugin/modules/`, then:

```bash
/guildmodule list                  # List loaded modules
/guildmodule load YourModule.jar   # Load new module
/guildmodule reload your-module    # Hot-reload
/guildmodule unload your-module    # Unload
/guildmodule info your-module      # Module details
```

## Best Practices

1. Initialize in `onEnable()`, clean up in `onDisable()`
2. Always provide default values when reading config
3. Use async for I/O operations, sync for Bukkit API
4. Implement `getModuleInstance()` in event handlers
5. Catch exceptions and log errors; re-throw to let framework know loading failed
6. Use `context.getMessage()` and `context.sendMessage()` for i18n instead of hardcoded strings
7. Do not directly access other modules' internals — use events and API

## FAQ

**Module fails to load?** Check that `module.yml` exists at JAR root, `main` class is correct, and `api-version` is compatible. Review console error stack trace.

**GUI button not showing?** Use `GUIExtensionHook.AUTO_SLOT` to avoid slot conflicts. Verify the GUI type name is correct.

**How to hot-reload?** Replace the JAR in `modules/` then run `/guildmodule reload <id>`. `onDisable()` is called to clean up before re-loading.

**Inter-module communication?** Use the event system (recommended) or shared data files. Avoid direct instance access.

## Links

- GitHub: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- Issues: [Report bugs](https://github.com/chenasyd/-GuildPlugin/issues)
- License: GNU GPL v3.0
