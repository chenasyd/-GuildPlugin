# GuildPlugin SDK Developer Guide

This guide covers developing external modules for GuildPlugin using the SDK. Modules are loaded dynamically from `plugins/GuildPlugin/modules/` and support hot-reload via `/guildmodule`.

## Quick Start

### Requirements

| Item | Requirement |
|:----:|:-----------:|
| JDK | 17+ |
| Maven | 3.6+ |
| SDK | `com.guild:guild-sdk:1.5.6` (provided scope) |

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
            <version>1.5.6</version>
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
api-version: 1.5.0
type: mixed
depends: []
soft-depends: []
config-prefix: my-first-module
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
UNLOADED → LOADING → ACTIVE → DISABLING → UNLOADED
                ↘ ERROR (load failure)
```

| State | Description |
|:-----:|:-----------:|
| `UNLOADED` | Not loaded / unloaded |
| `LOADING` | Loading in progress |
| `ACTIVE` | Running |
| `DISABLING` | Disabling in progress |
| `ERROR` | Load or runtime error |

### ModuleDescriptor Fields

| Field | Required | module.yml Key | Description |
|:-----:|:--------:|:--------------:|:-----------:|
| `id` | Yes | `id` | Unique identifier (lowercase, numbers, hyphens) |
| `name` | Yes | `name` | Display name |
| `version` | Yes | `version` | Semantic version |
| `author` | Yes | `author` | Author name |
| `main` | Yes | `main` | Fully qualified main class |
| `apiVersion` | Yes | `api-version` | Required minimum API version |
| `description` | No | `description` | Module description |
| `type` | No | `type` | `gui`, `data`, or `mixed` (default) |
| `depends` | No | `depends` | Hard dependency module IDs |
| `softDepends` | No | `soft-depends` | Optional dependency module IDs |
| `configPrefix` | No | `config-prefix` | Config section prefix (default: `modules.{id}`) |

### ModuleContext

`ModuleContext` is the runtime environment passed during `onEnable()`. Key methods:

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
String getMessage(Player player, String key, Object... args);  // v1.6.5: player-language aware

// Scheduler (Folia/Spigot compatible)
void runSync(Runnable task);
void runAsync(Runnable task);
void runLater(long delayTicks, Runnable task);
void runTimer(long delayTicks, long periodTicks, Runnable task);

// GUI navigation
void openGUI(Player player, GUI gui);
boolean navigateBack(Player player);
```

#### ModuleContext I18n Details

The two `getMessage` variants behave differently:

| Method | Language Source | Use Case |
|:------:|:--------------:|:--------:|
| `getMessage(String key, Object... args)` | Module default language (`modules.yml` → `language.default`, default `"en"`) | Module-level logs, static GUI content |
| `getMessage(Player player, String key, Object... args)` | Player's language preference (from player locale or config) | Player-facing GUI text, in-game messages |

> **Important**: Always use the `Player` variant when rendering GUI content so the text follows the player's language. The no-Player variant always uses the module default language regardless of who is viewing the GUI.

**Best practice for GUI classes:**
```java
// Store the viewer in your GUI class
public class MyGUI implements GUI {
    private final Player viewer;
    // ...
    
    @Override
    public String getTitle() {
        // ✅ Player-aware: respects viewer's language
        return ColorUtils.colorize(
            context.getMessage(viewer, "module.xxx.gui.title", "&6Default Title"));
    }
}
```

## API Reference

### GuildPluginAPI

The central gateway for all interactions with the core plugin. Access via `context.getApi()`.

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

#### Member Management (v1.5+)

```java
CompletableFuture<Boolean> addMember(int guildId, UUID playerUuid, String playerName, String role);
CompletableFuture<Boolean> removeMember(int guildId, UUID playerUuid);
CompletableFuture<Boolean> setMemberRole(int guildId, UUID playerUuid, String role);
```

Role values: `"LEADER"`, `"OFFICER"`, `"MEMBER"`

#### GUI Extension

##### Button Registration (fixed text)

```java
// Register button in existing GUI (static text, no i18n)
void registerGUIButton(String guiType, int slot, ItemStack item,
                       String moduleId, GUIClickAction handler);
```

##### Button Registration (i18n-aware, v1.6.5+)

```java
// Register button with language keys for automatic i18n resolution
void registerGUIButton(String guiType, int slot, ItemStack item,
                       String moduleId, GUIClickAction handler,
                       String displayNameKey, String... loreKeys);
```

This overload stores language keys in the GUI slot. During rendering, the display name and lore are resolved from the module's language files using the viewer's preferred language. The `displayNameKey` and each `loreKeys` entry are looked up via `LanguageManager.getModuleMessage(player, key, fallback)` where the fallback is the text already set on the `ItemStack`.

**Example:**
```java
ItemStack btn = new ItemStack(Material.OAK_SIGN);
ItemMeta meta = btn.getItemMeta();
meta.setDisplayName("Announcements"); // fallback if key not found
btn.setItemMeta(meta);

api.registerGUIButton(
    "GuildSettingsGUI",
    GUIExtensionHook.AUTO_SLOT,  // or fixed slot number
    btn,
    "my-module",
    (player, ctx) -> { /* handle click */ },
    "module.my-module.button-name",      // displayNameKey
    "module.my-module.button-desc"       // loreKeys (varargs, one per line)
);
```

**Language file layout** (`lang/modules/my-module/en.yml`):
```yaml
module:
  my-module:
    button-name: "&e&lMy Feature"
    button-desc: "&7Click to open feature settings"
```

> **Note**: Items created via `createItem()` that set displayName/lore directly are treated as fallback only. The actual rendered text comes from the language file resolved per-player at render time. This enables language switching via `/guildadmin test lang` without recreating the module.

##### Custom GUI

```java
void registerCustomGUI(String guiId, ModuleGUIFactory factory);
void unregisterCustomGUI(String guiId);
void openCustomGUI(String guiId, Player player, Map<String, Object> data);
void openCustomGUI(String guiId, Player player);
```

Use `GUIExtensionHook.AUTO_SLOT` (`-1`) for automatic slot assignment.

Available target GUIs: `GuildSettingsGUI`, `GuildInfoGUI`, `MainGuildGUI`, `MemberManagementGUI`

#### Commands

```java
void registerSubCommand(String parentCommand, String name,
                        ModuleCommandHandler handler, String permission);
boolean hasSubCommand(String parentCommand, String name);
ModuleCommandHandler getSubCommandHandler(String parentCommand, String name);
String getSubCommandPermission(String parentCommand, String name);
List<String> getSubCommands(String parentCommand);
```

#### Events

All event listeners automatically cleaned up on module unload when `getModuleInstance()` returns the module instance.

```java
// Guild lifecycle
void onGuildCreate(GuildEventHandler handler);
void onGuildDelete(GuildEventHandler handler);

// Member lifecycle
void onMemberJoin(MemberEventHandler handler);
void onMemberLeave(MemberEventHandler handler);

// Economy (v1.5+)
void onEconomyDeposit(EconomyEventHandler handler);
void onEconomyWithdraw(EconomyEventHandler handler);

// Role changes (v1.5+)
void onMemberRoleChange(MemberRoleChangeEventHandler handler);
```

#### Currency (v1.5+)

**Enum-based API:**

```java
CurrencyManager getCurrencyManager();
double getCurrencyBalance(int guildId, UUID playerUuid, CurrencyType type);
boolean depositCurrency(int guildId, UUID playerUuid, String playerName, CurrencyType type, double amount);
boolean withdrawCurrency(int guildId, UUID playerUuid, CurrencyType type, double amount);
```

**String-based API (v1.5+):**

```java
double getCurrencyBalance(int guildId, UUID playerUuid, String currencyType);
boolean depositCurrency(int guildId, UUID playerUuid, String playerName, String currencyType, double amount);
boolean withdrawCurrency(int guildId, UUID playerUuid, String currencyType, double amount);
```

`CurrencyType` enum values:

| Enum | Display Name | Module | DB Column |
|:----:|:------------:|:------:|:---------:|
| `A_COIN` | ACoin | member_rank | a_coin |
| `B_COIN` | BCoin | guild_stats | b_coin |
| `C_COIN` | CCoin | guild_quest | c_coin |

#### Placeholder Extension (v1.5+)

```java
void registerPlaceholderProvider(PlaceholderProvider provider);
void unregisterPlaceholderProvider(String identifier);
```

Placeholder format: `%guild_module_<identifier>_<params>_<fallback>%`

The `fallback` suffix is optional — when no guild or data exists, the fallback text is returned instead.

Example:
```java
api.registerPlaceholderProvider(new PlaceholderProvider() {
    @Override
    public String getIdentifier() {
        return "regioncount";
    }

    @Override
    public String onRequest(Player player, String params) {
        // params = "invested" for %guild_module_regioncount_invested%
        return String.valueOf(getRegionCountFor(player, params));
    }
});
```
Use in PlaceholderAPI: `%guild_module_regioncount_invested%`

> Requires the PlaceholderAPI plugin installed on the server. SDK modules can still register providers, but placeholders only resolve when PlaceholderAPI is available.

#### HTTP Client

```java
CompletableFuture<String> httpGet(String url);
CompletableFuture<String> httpGet(String url, Map<String, String> headers);
CompletableFuture<String> httpPost(String url, String body, Map<String, String> headers);
HttpClientProvider getHttpClient();
```

`HttpClientProvider` also supports custom timeouts:
```java
new HttpClientProvider(int connectTimeout, int readTimeout);
```

#### Server time API

```java
LocalDateTime getServerTime();
String getServerTimeString();
String getServerDateString();
String getServerTimePlusMinutes(int minutes);
String getServerTimePlusDays(int days);
String formatServerTime(LocalDateTime dateTime);
String formatServerDate(LocalDateTime dateTime);
```

Use these methods when you need shared server-local time formatting in modules.

#### Console output API

```java
void consoleInfo(String message);
void consoleWarn(String message);
void consoleSevere(String message);

void consoleInfo(String message, String... args);
void consoleWarn(String message, String... args);
void consoleSevere(String message, String... args);
```

The console methods support Minecraft-style color codes via `&` and indexed placeholders `{0}`, `{1}`, etc.

Example:
```java
api.consoleInfo("&a[MyModule] loaded successfully");
api.consoleWarn("&e[MyModule] missing config: {0}", "settings.yml");
api.consoleSevere("&c[MyModule] fatal error: {0}", e.getMessage());
```

#### Module Language System (i18n)

Modules can provide multi-language support for all player-facing text. The language system supports 3 independent domains:

| Domain | Language File | Default Language Config | API |
|:------:|:------------:|:-----------------------:|:---:|
| **Core** | `lang/core/{lang}.yml` | `config.yml` → `language.default` | `getCoreMessage()` |
| **GUI** | `lang/gui/{lang}.yml` | `config.yml` → `language.default` | `getGuiMessage()` |
| **Module** | `lang/modules/{moduleId}/{lang}.yml` | `modules.yml` → `language.default` | `getModuleMessage()`, `context.getMessage()` |

> **Warning**: Core/GUI and Module language defaults are configured in **different files** (`config.yml` vs `modules.yml`). Changing one does not affect the other.

##### Language File Setup

Module language files are placed at `lang/modules/{moduleId}/{lang}.yml` inside your JAR:

```
src/main/resources/
└── lang/
    └── modules/
        └── my-module/
            ├── en.yml
            ├── zh.yml
            ├── pl.yml
            └── br.yml
```

**Example `en.yml`:**
```yaml
module:
  my-module:
    gui:
      title: "&6&lMy Feature"
    button-name: "&eMy Feature"
    button-desc: "&7Open feature settings"
    info-button-name: "&eFeature Info"
    info-button-desc: "&7View current status"
    info-button-hint: "&7Click to view details"
    loaded: "[MyModule] Feature enabled"
    unloaded: "[MyModule] Feature disabled"
```

Built-in language codes: `en` (English), `zh` (Chinese), `pl` (Polish), `br` (Brazilian Portuguese).

##### LanguageManager API (via `context.getLanguageManager()`)

**Core Messages** (from `lang/core/`):
```java
LanguageManager lang = context.getLanguageManager();

// Get core message for a player (follows player's language)
String msg = lang.getCoreMessage(player, "guild.create.success", "Guild created");

// Get core message with placeholders
String msg = lang.getIndexedMessage(player, "guild.join.notify",
    "Player {0} joined guild {1}", playerName, guildName);
```

**GUI Messages** (from `lang/gui/`):
```java
// GUI common elements (pagination, navigation, etc.)
String prev = lang.getGuiMessage(player, "gui.previous-page", "&e&lPrevious");
String next = lang.getGuiMessage(player, "gui.next-page", "&e&lNext");
String page = lang.getGuiMessage(player, "gui.page-info", "Page {0}/{1}", "1", "5");
```

**Module Messages** (from `lang/modules/{moduleId}/`):
```java
// Get message by player's language
String title = lang.getModuleMessage(player, "module.my-module.gui.title", "&6Default Title");

// Get message with indexed placeholders {0}, {1}, {2}...
String msg = lang.getModuleIndexedMessage(player, "module.my-module.welcome",
    "Welcome {0} to {1}", playerName, guildName);

// Get message using a specific language code (for admin tools)
String enMsg = lang.getModuleMessage("en", "module.my-module.gui.title", "&6Title");
String zhMsg = lang.getModuleMessage("zh", "module.my-module.gui.title", "&6标题");
```

##### Module language resource API

```java
// Extract bundled module language file from JAR to disk
boolean released = api.releaseModuleLanguageResource("my-module", "zh");

// Load module language resources (external first, then bundled fallback)
boolean loaded = api.loadModuleLanguageResource("my-module", "zh");

// Get the on-disk File reference for a module language file
File langFile = api.getModuleLanguageFile("my-module", "zh");
```

Notes:
- `releaseModuleLanguageResource` extracts a bundled language file to the plugin data folder.
- `loadModuleLanguageResource` delegates to `LanguageManager` to load all available languages for the module.
- `getModuleLanguageFile` returns the expected disk path; the file may not exist until released.

##### Complete Usage Example

```java
@Override
public void onEnable(ModuleContext context) throws Exception {
    this.context = context;
    LanguageManager lang = context.getLanguageManager();
    GuildPluginAPI api = context.getApi();

    // Register GUI button with i18n keys
    ItemStack btn = new ItemStack(Material.BOOK);
    ItemMeta meta = btn.getItemMeta();
    meta.setDisplayName("My Feature"); // fallback
    btn.setItemMeta(meta);

    api.registerGUIButton("GuildSettingsGUI",
        GUIExtensionHook.AUTO_SLOT, btn,
        "my-module",
        (player, ctx) -> {
            // Player-aware message lookup
            String msg = lang.getModuleMessage(player,
                "module.my-module.clicked",
                "&aFeature opened!");
            player.sendMessage(ColorUtils.colorize(msg));
        },
        "module.my-module.button-name",      // displayNameKey
        "module.my-module.button-desc"       // loreKey
    );

    // Console log (no player context, uses module default language)
    ConsoleLogger.info(context.getMessage("module.my-module.loaded",
        "[MyModule] Feature system enabled"));
}

### Data Models

#### GuildData

| Field | Type | Getter | Description |
|:-----:|:----:|:------:|:-----------:|
| `id` | `int` | `getId()` | Guild ID |
| `name` | `String` | `getName()` | Guild name |
| `masterUuid` | `UUID` | `getMasterUuid()` | Leader UUID |
| `masterName` | `String` | `getMasterName()` | Leader name |
| `level` | `int` | `getLevel()` | Guild level |
| `experience` | `long` | `getExperience()` | Experience points |
| `balance` | `double` | `getBalance()` | Guild funds |
| `memberCount` | `int` | `getMemberCount()` | Current members |
| `maxMembers` | `int` | `getMaxMembers()` | Max member capacity |
| `motto` | `String` | `getMotto()` | Guild description/motto |
| `createTime` | `long` | `getCreateTime()` | Creation timestamp (epoch ms) |
| `members` | `List<MemberData>` | `getMembers()` | Member list (loaded on demand) |

#### MemberData

| Field | Type | Getter | Description |
|:-----:|:----:|:------:|:-----------:|
| `playerUuid` | `UUID` | `getPlayerUuid()` | Player UUID |
| `playerName` | `String` | `getPlayerName()` | Player name |
| `role` | `String` | `getRole()` | `LEADER` / `OFFICER` / `MEMBER` |
| `joinTime` | `long` | `getJoinTime()` | Join timestamp (epoch ms) |
| `contribution` | `double` | `getContribution()` | Contribution value |
| `online` | `boolean` | `isOnline()` | Online status |
| `investedBalance` | `double` | `getInvestedBalance()` | Total deposited funds (v1.5+) |

Two constructors available — the 6-param constructor defaults `investedBalance` to `0.0` for backwards compatibility.

#### PlayerRecordData

Player disciplinary record data model (v1.5+):

| Field | Type | Getter | Description |
|:-----:|:----:|:------:|:-----------:|
| `playerUuid` | `UUID` | `getPlayerUuid()` | Player UUID |
| `playerName` | `String` | `getPlayerName()` | Player name |
| `recordType` | `String` | `getRecordType()` | Record type |
| `reason` | `String` | `getReason()` | Reason for record |
| `sourceServer` | `String` | `getSourceServer()` | Originating server |
| `operatorName` | `String` | `getOperatorName()` | Issuing operator |
| `timestamp` | `long` | `getTimestamp()` | When issued (epoch ms) |
| `expiryTime` | `long` | `getExpiryTime()` | Expiry (epoch ms, -1 = permanent) |
| — | `boolean` | `isPermanent()` | True if `expiryTime == -1` |

### Event Interfaces

All event handlers MUST implement `getModuleInstance()` to return the module instance — this enables automatic cleanup on unload.

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

public interface EconomyEventHandler {
    void onEvent(EconomyEventData data);
    Object getModuleInstance();
}

public interface MemberRoleChangeEventHandler {
    void onEvent(MemberRoleChangeEventData data);
    default Object getModuleInstance() { return null; }
}
```

#### Event Data Classes

**GuildEventData** — `getGuildId()`, `getGuildName()`, `getGuildLeaderName()`

**MemberEventData** — `getGuildId()`, `getGuildName()`, `getPlayerUuid()`, `getPlayerName()`, `getEventType()` (e.g. `"JOIN"`, `"LEAVE"`, `"KICK"`)

**EconomyEventData** — `getGuildId()`, `getGuildName()`, `getPlayerUuid()`, `getPlayerName()`, `getAmount()`, `getEventType()` (`"DEPOSIT"` / `"WITHDRAW"`)

**MemberRoleChangeEventData** — `getGuildId()`, `getGuildName()`, `getPlayerUuid()`, `getPlayerName()`, `getOldRole()`, `getNewRole()`

> **Note**: `EconomyEventHandler.getModuleInstance()` has no default implementation — you must implement it explicitly.

### GUI Interface

The `GUI` interface contract:

```java
public interface GUI {
    String getTitle();
    int getSize();
    void setupInventory(Inventory inventory);
    void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType);
    default void onClose(Player player) {}
    default void refresh(Player player) {}
    default boolean isValid() { return true; }
    default String getGuiType() { return this.getClass().getSimpleName(); }
}
```

> **Note**: `onClick` now includes `ClickType clickType` for distinguishing left/right/shift clicks.

### PlaceholderProvider (v1.5+)

```java
public interface PlaceholderProvider {
    /** Identifier for %guild_module_{identifier}_...% routing */
    String getIdentifier();

    /**
     * Handle placeholder request.
     * @param player The requesting player (may be null)
     * @param params Parameters (fallback suffix already stripped by framework)
     * @return Replacement text, null to indicate unhandled
     */
    String onRequest(Player player, String params);
}
```

### ModuleConfigSection

Module configuration is namespaced under `modules.{moduleId}` in `config.yml` (or custom prefix via `config-prefix` in `module.yml`).

```yaml
# config.yml
modules:
  my-module:
    api-key: "your-key"
    max-results: 10
    enable-feature: true
    cooldown: 5.5
    whitelist:
      - player1
      - player2
```

```java
ModuleConfigSection config = context.getConfig();

String apiKey = config.getString("api-key", "");
int maxResults = config.getInt("max-results", 10);
boolean enabled = config.getBoolean("enable-feature", false);
long timeout = config.getLong("timeout", 3000L);
double cooldown = config.getDouble("cooldown", 1.0);

List<String> whitelist = config.getStringList("whitelist");
boolean hasKey = config.contains("some-key");

ConfigurationSection raw = config.getConfigSection();
String path = config.getConfigPath(); // e.g. "modules.my-module"
```

## Development Guide

### GUI Button Registration

```java
// Auto slot (recommended for settings)
api.registerGUIButton("GuildSettingsGUI",
    GUIExtensionHook.AUTO_SLOT, buttonItem,
    "my-module",
    (player, ctx) -> { /* handle click */ });

// Fixed slot
api.registerGUIButton("GuildInfoGUI",
    12, infoButton,
    "my-module",
    (player, ctx) -> { /* handle click */ });
```

### Event Listening

```java
// v1.5+: 7 event types available

api.onGuildCreate(new GuildEventHandler() {
    @Override
    public void onEvent(GuildEventData data) {
        context.getLogger().info("New guild: " + data.getGuildName());
    }
    @Override
    public Object getModuleInstance() { return MyModule.this; }
});

api.onMemberJoin(new MemberEventHandler() {
    @Override
    public void onEvent(MemberEventData data) {
        context.getLogger().info(data.getPlayerName() + " joined " + data.getGuildName());
    }
    @Override
    public Object getModuleInstance() { return MyModule.this; }
});

api.onEconomyDeposit(new EconomyEventHandler() {
    @Override
    public void onEvent(EconomyEventData data) {
        context.getLogger().info(data.getPlayerName() + " deposited " + data.getAmount());
    }
    @Override
    public Object getModuleInstance() { return MyModule.this; }
});

api.onMemberRoleChange(new MemberRoleChangeEventHandler() {
    @Override
    public void onEvent(MemberRoleChangeEventData data) {
        context.getLogger().info(data.getPlayerName() + ": " +
            data.getOldRole() + " → " + data.getNewRole());
    }
    @Override
    public Object getModuleInstance() { return MyModule.this; }
});
```

### Custom GUI with AbstractModuleGUI

Extend `AbstractModuleGUI` for standardized GUI creation with built-in border, pagination, and item utilities.

**Constants:**

| Constant | Value | Description |
|:--------:|:-----:|:-----------:|
| `DEFAULT_SIZE` | 54 | Default inventory size (6 rows) |
| `CONTENT_START` | 9 | First content slot (row 2, slot 0) |
| `CONTENT_END` | 44 | Last content slot (row 5, slot 8) |
| `COLUMNS` | 7 | Content columns per row |
| `PER_PAGE` | 28 | Items per page (7×4) |

```java
public class MyCustomGUI extends AbstractModuleGUI {

    public MyCustomGUI(Player player, Guild guild) {
        this.inventory = Bukkit.createInventory(null, getSize(), "My Custom GUI");
    }

    @Override
    public void setupInventory(Inventory inv) {
        fillBorder(inv);         // Black border top/bottom/left/right
        fillInteriorSlots(inv);  // Gray filler in content area

        // Title item
        inv.setItem(4, createItem(Material.BOOK,
            "&e" + guild.getName(),
            "&7Level: &f" + guild.getLevel()));

        // Content item at mapped slot
        inv.setItem(mapToSlot(0), createItem(Material.DIAMOND,
            "&aFeature A", "&7Click to use"));

        // Pagination
        setupPagination(inv, currentPage, totalPages,
            "&e← Previous", "&e→ Next");

        // Back button
        inv.setItem(49, createBackButton("&cBack", "&7Return to previous"));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType clickType) {
        if (slot == 49) {
            context.navigateBack(player);
        }
        // Check click type
        if (clickType == ClickType.RIGHT) {
            // right-click specific behavior
        }
    }
}
```

**Key utility methods:**

| Method | Purpose |
|:------:|:-------:|
| `fillBorder(Inventory)` | Black glass border (top/bottom rows + side columns) |
| `fillInteriorSlots(Inventory)` | Gray glass filler in empty content slots |
| `mapToSlot(int linearIndex)` | Map linear index (0–27) to inventory slot in content grid |
| `createItem(Material, name, lore...)` | Create item with colored name/lore (`&` codes supported) |
| `createBackButton(name, hint)` | Standard back button (Arrow material) |
| `setupPagination(inv, page, total, prevText, nextText)` | Add prev/next buttons (slot 45/53) |
| `getTotalPages(int totalItems)` | Calculate total pages needed |

### Module Command Registration

```java
// Register subcommand under /guild
api.registerSubCommand("guild", "mytool",
    (sender, args) -> {
        if (sender instanceof Player player) {
            // Handle your custom logic
        }
    },
    "guild.mytool.use"  // permission node (null for no check)
);

// Check if a subcommand exists
if (api.hasSubCommand("guild", "mytool")) { ... }
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
| Bukkit API (GUI, entities, messages) | Main / Sync | `context.runSync()` |
| HTTP, file I/O, database queries | Async | `context.runAsync()` |
| Delayed tasks | — | `context.runLater(long delayTicks, Runnable)` |
| Repeating tasks | — | `context.runTimer(long delay, long period, Runnable)` |

Always use `context.runSync()` / `context.runAsync()` instead of `Bukkit.getScheduler()` for Folia compatibility.

### Inter-Module Communication

Use the event system (recommended) — modules listen to events and react, rather than directly accessing each other's internals.

## module.yml Reference

```yaml
# Required
id: your-module-id              # Unique identifier (lowercase, numbers, hyphens)
name: "Your Module Name"        # Display name
version: 1.0.0                  # Semantic version
author: "Your Name"             # Author
main: com.example.YourModule    # Fully qualified main class
api-version: 1.5.0              # Minimum API version

# Optional
description: "Brief description" # Module description
type: mixed                      # gui | data | mixed (default)
depends: []                      # Hard dependencies (module IDs)
soft-depends: []                 # Soft dependencies (module IDs)
config-prefix: your-module       # Custom config.yml section prefix
                                 # Default: modules.{id}
permissions:                     # Custom permission nodes
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
/guildmodule list                    # List loaded modules
/guildmodule load YourModule.jar     # Load new module
/guildmodule reload your-module-id   # Hot-reload
/guildmodule unload your-module-id   # Unload
/guildmodule info your-module-id     # Module details
/guildmodule cloud                   # List cloud modules
/guildmodule cloud download <id>     # Download from cloud
```

## Best Practices

1. Initialize in `onEnable()`, clean up in `onDisable()`
2. Always provide default values when reading config
3. Use async for I/O operations, sync for Bukkit API
4. Implement `getModuleInstance()` in all event handlers (return `this`)
5. Catch exceptions and log errors; re-throw to let framework know loading failed
6. Use `context.getMessage(player, key, fallback)` for player-facing i18n, `LanguageManager.getCoreMessage()/getGuiMessage()/getModuleMessage()` for domain-specific needs
7. Do not directly access other modules' internals — use events and API
8. **New (v1.5+)**: Use the string-based currency API for runtime flexibility (e.g. `"A_COIN"` vs `CurrencyType.A_COIN`)

## FAQ

**Module fails to load?** Check that `module.yml` exists at JAR root, `main` class is correct, and `api-version` is compatible. Review console error stack trace.

**GUI button not showing?** Use `GUIExtensionHook.AUTO_SLOT` to avoid slot conflicts. Verify the GUI type name is correct.

**How to hot-reload?** Replace the JAR in `modules/` then run `/guildmodule reload <id>`. `onDisable()` is called to clean up before re-loading.

**Inter-module communication?** Use the event system (recommended) or share data via the currency/placeholder APIs. Avoid direct instance access.

**How to handle both left and right clicks in GUI?** The `onClick` method now receives `ClickType clickType` — check `clickType == ClickType.RIGHT` for right-click behavior.

**Why does my EconomyEventHandler need getModuleInstance()?** Unlike other handlers, `EconomyEventHandler.getModuleInstance()` has no default implementation — you must implement it explicitly for proper cleanup on unload.

**How to add multi-language support to my module?** Create language files under `lang/modules/{moduleId}/{en,zh,pl,br}.yml` in your JAR resources. Use `context.getMessage(player, key, fallback)` for player-aware text and the i18n `registerGUIButton()` overload with `displayNameKey`/`loreKeys` for GUI buttons.

**Module GUI always shows English despite player language?** Check that you are using `context.getMessage(player, key, fallback)` (with Player parameter) instead of `context.getMessage(key, fallback)` (without Player). The no-Player variant always uses the module default language from `modules.yml`.

**How does `/guildadmin test lang` help debug language issues?** Run subcommands like `overview` (all modules), `lookup <module> <key>` (single key), `files` (file existence check), or `module-context` (trace call chain). See `DEV-GUIDE.md` §"多语言系统" for full details.

## Links

- GitHub: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- Issues: [Report bugs](https://github.com/chenasyd/-GuildPlugin/issues)
- License: GNU GPL v3.0
