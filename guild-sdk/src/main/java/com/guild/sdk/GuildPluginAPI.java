package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;
import com.guild.sdk.event.EconomyEventData;
import com.guild.sdk.event.EconomyEventHandler;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.event.MemberRoleChangeEventData;
import com.guild.sdk.event.MemberRoleChangeEventHandler;
import com.guild.sdk.gui.ModuleGUIFactory;
import com.guild.sdk.gui.ModuleGUIRegistration;
import com.guild.sdk.gui.BedrockFormProvider;
import com.guild.sdk.gui.GUILayoutDefinition;
import com.guild.sdk.gui.ModuleGUIConfig;
import com.guild.sdk.http.HttpClientProvider;
import com.guild.sdk.placeholder.PlaceholderProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 编译期 SDK 门面。
 * 运行时由主插件中的同名类提供真实实现。
 */
public class GuildPluginAPI {
    /** SDK API 版本号，与插件版本保持一致 */
    public static final String API_VERSION = "1.6.6";

    private final HttpClientProvider httpClient = new HttpClientProvider();

    public GuildPluginAPI() {
    }

    public GuildPluginAPI(GuildPlugin plugin) {
    }

    public CompletableFuture<GuildData> getGuildById(int id) {
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<GuildData> getGuildByName(String name) {
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<GuildData> getPlayerGuild(UUID playerUuid) {
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<List<GuildData>> getAllGuilds() {
        return CompletableFuture.completedFuture(List.of());
    }

    public CompletableFuture<List<MemberData>> getGuildMembers(int guildId) {
        return CompletableFuture.completedFuture(List.of());
    }

    /**
     * Read-only hybrid contribution / activity scores from the core builtin system.
     * Empty list if disabled or unavailable.
     */
    public CompletableFuture<List<com.guild.sdk.data.ActivityScoreData>> getMemberActivityScores(int guildId) {
        return CompletableFuture.completedFuture(List.of());
    }

    public void registerGUIButton(String guiType, int slot, ItemStack item, String moduleId,
                                  GUIExtensionHook.GUIClickAction handler) {
    }

    public void registerGUIButton(String guiType, int slot, ItemStack item, String moduleId,
                                  GUIExtensionHook.GUIClickAction handler,
                                  String displayNameKey, String... loreKeys) {
    }

    /**
     * @deprecated Use {@link #registerCustomGUI(String, String, ModuleGUIFactory)} with moduleId.
     *             This stub always throws when called through a real implementation.
     */
    @Deprecated
    public void registerCustomGUI(String guiId, ModuleGUIFactory factory) {
        throw new IllegalArgumentException(
                "registerCustomGUI(guiId, factory) requires moduleId; use registerCustomGUI(moduleId, guiId, factory)");
    }

    /** 注册自定义 GUI（带模块归属追踪） */
    public void registerCustomGUI(String moduleId, String guiId, ModuleGUIFactory factory) {
    }

    public void unregisterCustomGUI(String guiId) {
    }

    public void openCustomGUI(String guiId, Player player, Map<String, Object> data) {
    }

    public void openCustomGUI(String guiId, Player player) {
    }

    // ═══ Enhanced Module GUI Registration API ═══

    /**
     * Register a custom GUI with enhanced capabilities (image binding, layout,
     * Bedrock form, config override). {@code registration.moduleId} is required.
     *
     * @param registration the full registration descriptor
     */
    public void registerCustomGUI(ModuleGUIRegistration registration) {
    }

    /**
     * Query the Bedrock form provider registered for a module GUI.
     * Called internally by GUIManager; modules generally don't need this.
     *
     * @param guiId GUI identifier
     * @return provider, or null if none registered
     */
    public BedrockFormProvider getBedrockFormProvider(String guiId) {
        return null;
    }

    /**
     * Query whether a module GUI has an image binding registered.
     *
     * @param guiId GUI identifier
     * @return true if image binding exists
     */
    public boolean hasModuleImageBinding(String guiId) {
        return false;
    }

    /**
     * Query the layout definition for a module GUI.
     *
     * @param guiId GUI identifier
     * @return layout definition, or null
     */
    public GUILayoutDefinition getModuleGUILayout(String guiId) {
        return null;
    }

    /**
     * Get the config override instance for a module GUI.
     *
     * @param guiId GUI identifier
     * @return config instance, or null
     */
    public ModuleGUIConfig getModuleGUIConfig(String guiId) {
        return null;
    }

    public void registerSubCommand(String parentCommand, String name, ModuleCommandHandler handler, String permission) {
    }

    /** 注册子命令（带模块归属追踪） */
    public void registerSubCommand(String moduleId, String parentCommand, String name, ModuleCommandHandler handler, String permission) {
    }

    public void onGuildCreate(GuildEventHandler handler) {
    }

    public void onGuildDelete(GuildEventHandler handler) {
    }

    public void onMemberJoin(MemberEventHandler handler) {
    }

    public void onMemberLeave(MemberEventHandler handler) {
    }

    public CompletableFuture<String> httpGet(String url, Map<String, String> headers) {
        return httpClient.httpGet(url, headers);
    }

    public CompletableFuture<String> httpGet(String url) {
        return httpGet(url, null);
    }

    public CompletableFuture<String> httpPost(String url, String body, Map<String, String> headers) {
        return httpClient.httpPost(url, body, headers);
    }

    public HttpClientProvider getHttpClient() {
        return httpClient;
    }

    // ==================== Server time API ====================

    /**
     * Get the current server local time.
     */
    public LocalDateTime getServerTime() {
        return null;
    }

    /**
     * Get the current server local time as yyyy-MM-dd HH:mm:ss.
     */
    public String getServerTimeString() {
        return null;
    }

    /**
     * Get the current server local date as yyyy-MM-dd.
     */
    public String getServerDateString() {
        return null;
    }

    /**
     * Get the server local time after adding the given minutes.
     */
    public String getServerTimePlusMinutes(int minutes) {
        return null;
    }

    /**
     * Get the server local time after adding the given days.
     */
    public String getServerTimePlusDays(int days) {
        return null;
    }

    /**
     * Format a LocalDateTime using the server full formatter.
     */
    public String formatServerTime(LocalDateTime dateTime) {
        return null;
    }

    /**
     * Format a LocalDateTime using the server date-only formatter.
     */
    public String formatServerDate(LocalDateTime dateTime) {
        return null;
    }

    // ==================== Console output API ====================

    /**
     * Print a green console INFO message with color codes.
     */
    public void consoleInfo(String message) {
    }

    /**
     * Print a yellow console WARN message with color codes.
     */
    public void consoleWarn(String message) {
    }

    /**
     * Print a red console SEVERE message with color codes.
     */
    public void consoleSevere(String message) {
    }

    /**
     * Print a green console INFO message with indexed placeholders.
     */
    public void consoleInfo(String message, String... args) {
    }

    /**
     * Print a yellow console WARN message with indexed placeholders.
     */
    public void consoleWarn(String message, String... args) {
    }

    /**
     * Print a red console SEVERE message with indexed placeholders.
     */
    public void consoleSevere(String message, String... args) {
    }

    // ==================== Module language resource API ====================

    /**
     * Load a module language file from plugins/GuildPlugin/lang/modules/{moduleId}/{lang}.yml.
     * This only reads external module language files and falls back to bundled resources if needed.
     */
    public boolean loadModuleLanguageResource(String moduleId, String lang) {
        return false;
    }

    /**
     * Release a bundled module language file to the plugin data folder under lang/modules/{moduleId}/{lang}.yml.
     */
    public boolean releaseModuleLanguageResource(String moduleId, String lang) {
        return false;
    }

    /**
     * Get the local module language file path for the given module and language code.
     */
    public File getModuleLanguageFile(String moduleId, String lang) {
        return null;
    }

    // ==================== 成员管理 API（v1.5 新增） ====================

    /** 向公会添加成员（异步） */
    public CompletableFuture<Boolean> addMember(int guildId, UUID playerUuid, String playerName, String role) {
        return CompletableFuture.completedFuture(false);
    }

    /** 从公会移除成员（异步） */
    public CompletableFuture<Boolean> removeMember(int guildId, UUID playerUuid) {
        return CompletableFuture.completedFuture(false);
    }

    /** 修改成员角色（异步） */
    public CompletableFuture<Boolean> setMemberRole(int guildId, UUID playerUuid, String role) {
        return CompletableFuture.completedFuture(false);
    }

    // ==================== 经济事件 API（v1.5 新增） ====================

    /** 监听公会存款事件 */
    public void onEconomyDeposit(EconomyEventHandler handler) { }

    /** 监听公会取款事件 */
    public void onEconomyWithdraw(EconomyEventHandler handler) { }

    // ==================== 角色变更事件（v1.5 新增） ====================

    /** 监听成员角色变更事件 */
    public void onMemberRoleChange(MemberRoleChangeEventHandler handler) { }

    // ==================== 货币 API（v1.5 新增） ====================

    /** 获取玩家在公会中的货币余额 */
    public double getCurrencyBalance(int guildId, UUID playerUuid, String currencyType) {
        return 0.0;
    }

    /** 增加玩家货币 */
    public boolean depositCurrency(int guildId, UUID playerUuid, String playerName, String currencyType, double amount) {
        return false;
    }

    /** 减少玩家货币 */
    public boolean withdrawCurrency(int guildId, UUID playerUuid, String currencyType, double amount) {
        return false;
    }

    // ==================== 占位符扩展（v1.5 新增） ====================

    /** 注册自定义占位符提供者 */
    public void registerPlaceholderProvider(PlaceholderProvider provider) { }

    /** 注册自定义占位符提供者（带模块归属追踪） */
    public void registerPlaceholderProvider(String moduleId, PlaceholderProvider provider) { }

    /** 注销占位符提供者 */
    public void unregisterPlaceholderProvider(String identifier) { }

    // ==================== 模块清理 ====================

    /** 移除指定模块注册的所有资源（占位符、子命令、自定义 GUI） */
    public void clearModuleRegistrations(String moduleId) { }
}
