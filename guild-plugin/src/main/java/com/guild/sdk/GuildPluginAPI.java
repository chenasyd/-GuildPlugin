package com.guild.sdk;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.guild.GuildPlugin;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;
import com.guild.sdk.economy.CurrencyManager;
import com.guild.sdk.event.EconomyEventHandler;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.event.MemberRoleChangeEventHandler;
import com.guild.sdk.gui.BedrockFormProvider;
import com.guild.sdk.gui.GUILayoutDefinition;
import com.guild.sdk.gui.ModuleGUIConfig;
import com.guild.sdk.gui.ModuleGUIFactory;
import com.guild.sdk.gui.ModuleGUIRegistration;
import com.guild.sdk.home.HomeProtectIntegration;
import com.guild.sdk.http.HttpClientProvider;
import com.guild.sdk.placeholder.PlaceholderProvider;

import java.io.File;

/**
 * 公会插件 SDK - 统一 API 门面
 * <p>
 * 所有模块共享同一个 API 实例（由 ModuleManager 管理），
 * 确保事件处理器和 GUI 注册的集中分发。
 */
public class GuildPluginAPI {

    private final GuildPlugin plugin;
    private final Logger logger;
    private final CurrencyManager currencyManager;
    private final SdkCurrencyFacade currency;
    private final GuildQueryFacade queries;
    private final GuildMemberCommandFacade memberCommands;
    private final ModuleEventBus events;
    private final ModuleExtensionRegistry extensions;
    private final ModuleHomeProtectCoordinator homeProtect;
    private final ModuleRuntimeTools runtime;

    public GuildPluginAPI(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = Logger.getLogger("GuildPlugin.API");
        this.currencyManager = plugin.getServiceContainer().get(CurrencyManager.class);
        this.currency = new SdkCurrencyFacade(currencyManager);
        GuildDataMapper dataMapper = new GuildDataMapper(plugin);
        this.queries = new GuildQueryFacade(plugin, dataMapper);
        this.memberCommands = new GuildMemberCommandFacade(plugin);
        this.events = new ModuleEventBus(logger);
        this.extensions = new ModuleExtensionRegistry(plugin, logger);
        this.homeProtect = new ModuleHomeProtectCoordinator(logger);
        this.runtime = new ModuleRuntimeTools(plugin, new HttpClientProvider());
    }

    // ==================== 公会查询 API ====================

    public CompletableFuture<GuildData> getGuildById(int id) {
        return queries.getGuildById(id);
    }

    public CompletableFuture<GuildData> getGuildByName(String name) {
        return queries.getGuildByName(name);
    }

    public CompletableFuture<GuildData> getPlayerGuild(UUID playerUuid) {
        return queries.getPlayerGuild(playerUuid);
    }

    public CompletableFuture<List<GuildData>> getAllGuilds() {
        return queries.getAllGuilds();
    }

    // ==================== 成员查询 API ====================

    public CompletableFuture<List<MemberData>> getGuildMembers(int guildId) {
        return queries.getGuildMembers(guildId);
    }

    public CompletableFuture<List<com.guild.sdk.data.ActivityScoreData>> getMemberActivityScores(int guildId) {
        return queries.getMemberActivityScores(guildId);
    }

    // ==================== GUI 扩展 API ====================

    public void registerGUIButton(String guiType, int slot, ItemStack item,
                                  String moduleId,
                                  GUIExtensionHook.GUIClickAction handler) {
        extensions.registerGUIButton(guiType, slot, item, moduleId, handler);
    }

    public void registerGUIButton(String guiType, int slot, ItemStack item,
                                  String moduleId,
                                  GUIExtensionHook.GUIClickAction handler,
                                  String displayNameKey, String... loreKeys) {
        extensions.registerGUIButton(guiType, slot, item, moduleId, handler, displayNameKey, loreKeys);
    }

    @Deprecated
    public void registerCustomGUI(String guiId, ModuleGUIFactory factory) {
        throw new IllegalArgumentException(
                "registerCustomGUI(guiId, factory) is removed; use registerCustomGUI(moduleId, guiId, factory) "
                        + "or ModuleGUIRegistration.builder(...).moduleId(moduleId).build()");
    }

    public void registerCustomGUI(String moduleId, String guiId, ModuleGUIFactory factory) {
        extensions.registerCustomGUI(moduleId, guiId, factory);
    }

    public void unregisterCustomGUI(String guiId) {
        extensions.unregisterCustomGUI(guiId);
    }

    public void openCustomGUI(String guiId, Player player, Map<String, Object> data) {
        extensions.openCustomGUI(guiId, player, data);
    }

    public void openCustomGUI(String guiId, Player player) {
        openCustomGUI(guiId, player, null);
    }

    // ==================== 模块 GUI 增强注册 API ====================

    public void registerCustomGUI(ModuleGUIRegistration registration) {
        extensions.registerCustomGUI(registration);
    }

    public BedrockFormProvider getBedrockFormProvider(String guiId) {
        return extensions.getBedrockFormProvider(guiId);
    }

    public boolean hasModuleImageBinding(String guiId) {
        return extensions.hasModuleImageBinding(guiId);
    }

    public GUILayoutDefinition getModuleGUILayout(String guiId) {
        return extensions.getModuleGUILayout(guiId);
    }

    public ModuleGUIConfig getModuleGUIConfig(String guiId) {
        return extensions.getModuleGUIConfig(guiId);
    }

    // ==================== 命令扩展 API ====================

    public void registerSubCommand(String parentCommand, String name,
                                   ModuleCommandHandler handler,
                                   String permission) {
        extensions.registerSubCommand(parentCommand, name, handler, permission);
    }

    public void registerSubCommand(String moduleId, String parentCommand, String name,
                                   ModuleCommandHandler handler,
                                   String permission) {
        extensions.registerSubCommand(moduleId, parentCommand, name, handler, permission);
    }

    public boolean hasSubCommand(String parentCommand, String name) {
        return extensions.hasSubCommand(parentCommand, name);
    }

    public ModuleCommandHandler getSubCommandHandler(String parentCommand, String name) {
        return extensions.getSubCommandHandler(parentCommand, name);
    }

    public String getSubCommandPermission(String parentCommand, String name) {
        return extensions.getSubCommandPermission(parentCommand, name);
    }

    public List<String> getSubCommands(String parentCommand) {
        return extensions.getSubCommands(parentCommand);
    }

    // ==================== 事件 API ====================

    public void onGuildCreate(GuildEventHandler handler) {
        events.onGuildCreate(handler);
    }

    public void onGuildDelete(GuildEventHandler handler) {
        events.onGuildDelete(handler);
    }

    public void onMemberJoin(MemberEventHandler handler) {
        events.onMemberJoin(handler);
    }

    public void onMemberLeave(MemberEventHandler handler) {
        events.onMemberLeave(handler);
    }

    public void onEconomyDeposit(EconomyEventHandler handler) {
        events.onEconomyDeposit(handler);
    }

    public void onEconomyWithdraw(EconomyEventHandler handler) {
        events.onEconomyWithdraw(handler);
    }

    public void onMemberRoleChange(MemberRoleChangeEventHandler handler) {
        events.onMemberRoleChange(handler);
    }

    // ==================== 事件分发（供核心服务调用） ====================

    public void fireGuildCreate(int guildId, String guildName, String leaderName) {
        events.fireGuildCreate(guildId, guildName, leaderName);
    }

    public void fireGuildDelete(int guildId, String guildName, String leaderName) {
        events.fireGuildDelete(guildId, guildName, leaderName);
    }

    public void fireMemberJoin(int guildId, String guildName, UUID playerUuid, String playerName) {
        events.fireMemberJoin(guildId, guildName, playerUuid, playerName);
    }

    public void fireMemberLeave(int guildId, String guildName, UUID playerUuid, String playerName, String eventType) {
        events.fireMemberLeave(guildId, guildName, playerUuid, playerName, eventType);
    }

    public void fireEconomyDeposit(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        events.fireEconomyDeposit(guildId, guildName, playerUuid, playerName, amount);
    }

    public void fireEconomyWithdraw(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        events.fireEconomyWithdraw(guildId, guildName, playerUuid, playerName, amount);
    }

    public void fireMemberRoleChange(int guildId, String guildName, UUID playerUuid, String playerName,
                                     String oldRole, String newRole) {
        events.fireMemberRoleChange(guildId, guildName, playerUuid, playerName, oldRole, newRole);
    }

    public void clearModuleHandlers(Object moduleInstance) {
        events.clearModuleHandlers(moduleInstance);
        homeProtect.clearModuleHandlers(moduleInstance);
    }

    // ==================== Home 保护协调 ====================

    public void registerHomeProtectIntegration(Object moduleInstance, HomeProtectIntegration integration) {
        homeProtect.register(moduleInstance, integration);
    }

    public void unregisterHomeProtectIntegration(Object moduleInstance) {
        homeProtect.unregister(moduleInstance);
    }

    public boolean isHomeProtectFullyDeferred() {
        return homeProtect.isHomeProtectFullyDeferred();
    }

    public boolean shouldSkipHomeProtectAt(Player player, Location location) {
        return homeProtect.shouldSkipHomeProtectAt(player, location);
    }

    public boolean shouldSkipHomeProtectForGuildHome(int guildId, String worldName) {
        return homeProtect.shouldSkipHomeProtectForGuildHome(guildId, worldName);
    }

    public com.guild.sdk.territory.TerritoryAPI getTerritoryAPI() {
        if (plugin.getServiceContainer().has(com.guild.sdk.territory.TerritoryAPI.class)) {
            return plugin.getServiceContainer().get(com.guild.sdk.territory.TerritoryAPI.class);
        }
        return null;
    }

    public void clearModuleRegistrations(String moduleId) {
        extensions.clearModuleRegistrations(moduleId);
    }

    public void clearAll() {
        events.clearAll();
        extensions.clearAll();
    }

    // ==================== 货币 API ====================

    public CurrencyManager getCurrencyManager() {
        return currency.getCurrencyManager();
    }

    public double getCurrencyBalance(int guildId, UUID playerUuid, CurrencyManager.CurrencyType currencyType) {
        return currency.getCurrencyBalance(guildId, playerUuid, currencyType);
    }

    public CompletableFuture<Double> getCurrencyBalanceAsync(int guildId, UUID playerUuid,
                                                             CurrencyManager.CurrencyType currencyType) {
        return currency.getCurrencyBalanceAsync(guildId, playerUuid, currencyType);
    }

    public boolean depositCurrency(int guildId, UUID playerUuid, String playerName,
                                   CurrencyManager.CurrencyType currencyType, double amount) {
        return currency.depositCurrency(guildId, playerUuid, playerName, currencyType, amount);
    }

    public CompletableFuture<Boolean> depositCurrencyAsync(int guildId, UUID playerUuid, String playerName,
                                                           CurrencyManager.CurrencyType currencyType, double amount) {
        return currency.depositCurrencyAsync(guildId, playerUuid, playerName, currencyType, amount);
    }

    public boolean withdrawCurrency(int guildId, UUID playerUuid,
                                    CurrencyManager.CurrencyType currencyType, double amount) {
        return currency.withdrawCurrency(guildId, playerUuid, currencyType, amount);
    }

    public CompletableFuture<Boolean> withdrawCurrencyAsync(int guildId, UUID playerUuid,
                                                            CurrencyManager.CurrencyType currencyType, double amount) {
        return currency.withdrawCurrencyAsync(guildId, playerUuid, currencyType, amount);
    }

    public double getCurrencyBalance(int guildId, UUID playerUuid, String currencyType) {
        return currency.getCurrencyBalance(guildId, playerUuid, currencyType);
    }

    public CompletableFuture<Double> getCurrencyBalanceAsync(int guildId, UUID playerUuid, String currencyType) {
        return currency.getCurrencyBalanceAsync(guildId, playerUuid, currencyType);
    }

    public boolean depositCurrency(int guildId, UUID playerUuid, String playerName, String currencyType, double amount) {
        return currency.depositCurrency(guildId, playerUuid, playerName, currencyType, amount);
    }

    public CompletableFuture<Boolean> depositCurrencyAsync(int guildId, UUID playerUuid, String playerName,
                                                           String currencyType, double amount) {
        return currency.depositCurrencyAsync(guildId, playerUuid, playerName, currencyType, amount);
    }

    public boolean withdrawCurrency(int guildId, UUID playerUuid, String currencyType, double amount) {
        return currency.withdrawCurrency(guildId, playerUuid, currencyType, amount);
    }

    public CompletableFuture<Boolean> withdrawCurrencyAsync(int guildId, UUID playerUuid,
                                                            String currencyType, double amount) {
        return currency.withdrawCurrencyAsync(guildId, playerUuid, currencyType, amount);
    }

    // ==================== 成员管理 API（v1.5 新增） ====================

    public CompletableFuture<Boolean> addMember(int guildId, UUID playerUuid, String playerName, String role) {
        return memberCommands.addMember(guildId, playerUuid, playerName, role);
    }

    public CompletableFuture<Boolean> removeMember(int guildId, UUID playerUuid) {
        return memberCommands.removeMember(guildId, playerUuid);
    }

    public CompletableFuture<Boolean> setMemberRole(int guildId, UUID playerUuid, String role) {
        return memberCommands.setMemberRole(guildId, playerUuid, role);
    }

    // ==================== 占位符扩展 API（v1.5 新增） ====================

    public void registerPlaceholderProvider(PlaceholderProvider provider) {
        extensions.registerPlaceholderProvider(provider);
    }

    public void registerPlaceholderProvider(String moduleId, PlaceholderProvider provider) {
        extensions.registerPlaceholderProvider(moduleId, provider);
    }

    public void unregisterPlaceholderProvider(String identifier) {
        extensions.unregisterPlaceholderProvider(identifier);
    }

    public Map<String, PlaceholderProvider> getPlaceholderProviders() {
        return extensions.getPlaceholderProviders();
    }

    // ==================== HTTP 工具 API ====================

    public CompletableFuture<String> httpGet(String url, Map<String, String> headers) {
        return runtime.httpGet(url, headers);
    }

    public CompletableFuture<String> httpGet(String url) {
        return httpGet(url, null);
    }

    public CompletableFuture<String> httpPost(String url, String body, Map<String, String> headers) {
        return runtime.httpPost(url, body, headers);
    }

    public HttpClientProvider getHttpClient() {
        return runtime.getHttpClient();
    }

    // ==================== Server time API ====================

    public LocalDateTime getServerTime() {
        return runtime.getServerTime();
    }

    public String getServerTimeString() {
        return runtime.getServerTimeString();
    }

    public String getServerDateString() {
        return runtime.getServerDateString();
    }

    public String getServerTimePlusMinutes(int minutes) {
        return runtime.getServerTimePlusMinutes(minutes);
    }

    public String getServerTimePlusDays(int days) {
        return runtime.getServerTimePlusDays(days);
    }

    public String formatServerTime(LocalDateTime dateTime) {
        return runtime.formatServerTime(dateTime);
    }

    public String formatServerDate(LocalDateTime dateTime) {
        return runtime.formatServerDate(dateTime);
    }

    // ==================== Console output API ====================

    public void consoleInfo(String message) {
        runtime.consoleInfo(message);
    }

    public void consoleWarn(String message) {
        runtime.consoleWarn(message);
    }

    public void consoleSevere(String message) {
        runtime.consoleSevere(message);
    }

    public void consoleInfo(String message, String... args) {
        runtime.consoleInfo(message, args);
    }

    public void consoleWarn(String message, String... args) {
        runtime.consoleWarn(message, args);
    }

    public void consoleSevere(String message, String... args) {
        runtime.consoleSevere(message, args);
    }

    // ==================== Module language resource API ====================

    public boolean loadModuleLanguageResource(String moduleId, String lang) {
        return runtime.loadModuleLanguageResource(moduleId, lang);
    }

    public boolean releaseModuleLanguageResource(String moduleId, String lang) {
        return runtime.releaseModuleLanguageResource(moduleId, lang);
    }

    public File getModuleLanguageFile(String moduleId, String lang) {
        return runtime.getModuleLanguageFile(moduleId, lang);
    }
}
