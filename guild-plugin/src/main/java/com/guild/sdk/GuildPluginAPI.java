package com.guild.sdk;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.module.ModuleManager;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.time.TimeProvider;
import com.guild.core.utils.ConsoleLogger;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;
import com.guild.sdk.economy.CurrencyManager;
import com.guild.sdk.event.EconomyEventData;
import com.guild.sdk.event.EconomyEventHandler;
import com.guild.sdk.event.GuildEventData;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventData;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.event.MemberRoleChangeEventData;
import com.guild.sdk.event.MemberRoleChangeEventHandler;
import com.guild.sdk.gui.ModuleGUIFactory;
import com.guild.sdk.gui.ModuleGUIRegistration;
import com.guild.sdk.gui.BedrockFormProvider;
import com.guild.sdk.gui.GUILayoutDefinition;
import com.guild.sdk.gui.ModuleGUIConfig;
import com.guild.sdk.gui.AbstractModuleGUI;
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
    private final HttpClientProvider httpClient;
    private final Logger logger;
    private final CurrencyManager currencyManager;

    // 事件处理器列表（线程安全，所有模块共享）
    private final List<GuildEventHandler> onGuildCreateHandlers = new CopyOnWriteArrayList<>();
    private final List<GuildEventHandler> onGuildDeleteHandlers = new CopyOnWriteArrayList<>();
    private final List<MemberEventHandler> onMemberJoinHandlers = new CopyOnWriteArrayList<>();
    private final List<MemberEventHandler> onMemberLeaveHandlers = new CopyOnWriteArrayList<>();
    private final List<EconomyEventHandler> onEconomyDepositHandlers = new CopyOnWriteArrayList<>();
    private final List<EconomyEventHandler> onEconomyWithdrawHandlers = new CopyOnWriteArrayList<>();
    private final List<MemberRoleChangeEventHandler> onMemberRoleChangeHandlers = new CopyOnWriteArrayList<>();

    // 占位符提供者注册表
    private final Map<String, PlaceholderProvider> placeholderProviders = new java.util.concurrent.ConcurrentHashMap<>();

    // 自定义 GUI 注册表 (guiId -> factory)
    private final Map<String, ModuleGUIFactory> customGUIRegistry = new java.util.concurrent.ConcurrentHashMap<>();

    // 命令注册表 (parentCommand -> (subCommand -> handler))
    private final Map<String, Map<String, ModuleCommandHandler>> commandRegistry = new java.util.concurrent.ConcurrentHashMap<>();
    // 权限注册表 (parentCommand -> (subCommand -> permission))
    private final Map<String, Map<String, String>> permissionRegistry = new java.util.concurrent.ConcurrentHashMap<>();

    // 模块归属追踪（identifier/key → moduleId，用于按模块批量清理）
    private final Map<String, String> placeholderOwners = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> commandOwners = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> guiFactoryOwners = new java.util.concurrent.ConcurrentHashMap<>();

    public GuildPluginAPI(GuildPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = new HttpClientProvider();
        this.logger = Logger.getLogger("GuildPlugin.API");
        this.currencyManager = plugin.getServiceContainer().get(CurrencyManager.class);
    }

    // ==================== 工会查询 API ====================

    /** 根据 ID 获取工会信息（异步） */
    public CompletableFuture<GuildData> getGuildById(int id) {
        return plugin.getGuildService().getGuildByIdAsync(id).thenApply(this::convertGuild);
    }

    /** 根据名称获取工会信息（异步） */
    public CompletableFuture<GuildData> getGuildByName(String name) {
        return plugin.getGuildService().getGuildByNameAsync(name).thenApply(this::convertGuild);
    }

    /** 获取玩家所属工会（异步） */
    public CompletableFuture<GuildData> getPlayerGuild(UUID playerUuid) {
        return plugin.getGuildService().getPlayerGuildAsync(playerUuid).thenApply(this::convertGuild);
    }

    /** 获取所有工会列表（异步） */
    public CompletableFuture<List<GuildData>> getAllGuilds() {
        return plugin.getGuildService().getAllGuildsAsync().thenApply(list ->
                list.stream().map(this::convertGuild).filter(g -> g != null).toList());
    }

    // ==================== 成员查询 API ====================

    /** 获取工会成员列表（异步） */
    public CompletableFuture<List<MemberData>> getGuildMembers(int guildId) {
        return plugin.getGuildService().getGuildMembersAsync(guildId).thenApply(list ->
                list.stream().map(this::convertMember).toList());
    }

    /**
     * 只读：核心内置混合贡献 / 活跃度排行。
     * 未启用时返回空列表。
     */
    public CompletableFuture<List<com.guild.sdk.data.ActivityScoreData>> getMemberActivityScores(int guildId) {
        var service = plugin.getActivityScoreService();
        if (service == null || !service.getSettings().isEnabled()) {
            return CompletableFuture.completedFuture(List.of());
        }
        return service.getGuildScoresAsync(guildId).thenApply(list -> {
            List<com.guild.sdk.data.ActivityScoreData> out = new ArrayList<>(list.size());
            for (com.guild.activity.MemberActivityScore s : list) {
                out.add(new com.guild.sdk.data.ActivityScoreData(
                        s.getPlayerUuid(), s.getPlayerName(),
                        s.getEconomyPts(), s.getActivityPts(), s.getTotalScore(),
                        s.getRank(), s.isOnline()));
            }
            return out;
        });
    }

    // ==================== GUI 扩展 API ====================

    /**
     * 在指定 GUI 界面中注入自定义按钮（固定文本，无多语言支持）
     *
     * @param guiType  目标 GUI 类型标识符
     * @param slot     注入的槽位编号（0-based）
     * @param item     显示的物品图标
     * @param moduleId 当前模块 ID（用于卸载时自动清理）
     * @param handler  点击回调处理
     */
    public void registerGUIButton(String guiType, int slot, ItemStack item,
                                  String moduleId,
                                  GUIExtensionHook.GUIClickAction handler) {
        if (moduleId == null || moduleId.isEmpty()) {
            throw new IllegalArgumentException("moduleId 不能为空");
        }
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        mm.getRegistry().getGuiExtensionHook()
                .registerButton(guiType, slot, item, moduleId, handler);
    }

    /**
     * 在指定 GUI 界面中注入多语言自定义按钮
     * <p>
     * 使用此方法注册的按钮在 GUI 渲染时会按模块全局语言配置实时解析 displayName 和 lore，
     * 无需手动在 ItemStack 中使用 ColorUtils.colorize 设置文本。
     * 适用于支持模块重载后自动切换语言的场景。
     *
     * @param guiType        目标 GUI 类型标识符
     * @param slot           注入的槽位编号（0-based），传入 {@link GUIExtensionHook#AUTO_SLOT} 表示自动分配
     * @param item           显示物品图标（含材质和回退文本，回退文本不含颜色码）
     * @param moduleId       当前模块 ID（用于卸载时自动清理）
     * @param handler        点击回调处理
     * @param displayNameKey 显示名称的语言键（如 {@code "module.announcement.button-name"}）
     * @param loreKeys       lore 各行的语言键（按顺序对应，不含空行和动态内容）
     */
    public void registerGUIButton(String guiType, int slot, ItemStack item,
                                  String moduleId,
                                  GUIExtensionHook.GUIClickAction handler,
                                  String displayNameKey, String... loreKeys) {
        if (moduleId == null || moduleId.isEmpty()) {
            throw new IllegalArgumentException("moduleId 不能为空");
        }
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        mm.getRegistry().getGuiExtensionHook()
                .registerButton(guiType, slot, item, moduleId, handler, displayNameKey, loreKeys);
    }

    /**
     * @deprecated Use {@link #registerCustomGUI(String, String, ModuleGUIFactory)} with a non-empty moduleId.
     *             This overload always throws — unowned factories leak on module hot-unload.
     */
    @Deprecated
    public void registerCustomGUI(String guiId, ModuleGUIFactory factory) {
        throw new IllegalArgumentException(
                "registerCustomGUI(guiId, factory) is removed; use registerCustomGUI(moduleId, guiId, factory) "
                        + "or ModuleGUIRegistration.builder(...).moduleId(moduleId).build()");
    }

    /** 注册全新的自定义 GUI 页面（带模块归属追踪，卸载时自动清理） */
    public void registerCustomGUI(String moduleId, String guiId, ModuleGUIFactory factory) {
        if (moduleId == null || moduleId.isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be empty");
        }
        if (guiId == null || guiId.isEmpty()) {
            throw new IllegalArgumentException("guiId cannot be empty");
        }
        if (factory == null) {
            throw new IllegalArgumentException("factory cannot be null");
        }
        if (customGUIRegistry.containsKey(guiId)) {
            throw new IllegalArgumentException("guiId already registered: " + guiId);
        }
        customGUIRegistry.put(guiId, factory);
        guiFactoryOwners.put(guiId, moduleId);
    }

    /** 注销自定义 GUI 页面（模块卸载时调用） */
    public void unregisterCustomGUI(String guiId) {
        customGUIRegistry.remove(guiId);
        guiFactoryOwners.remove(guiId);
    }

    /** 打开已注册的自定义 GUI 页面 */
    public void openCustomGUI(String guiId, Player player, Map<String, Object> data) {
        ModuleGUIFactory factory = customGUIRegistry.get(guiId);
        if (factory == null) {
            logger.warning("custom GUI not found: " + guiId);
            return;
        }
        GUI gui = factory.create(player, data != null ? data : Map.of());
        plugin.getGuiManager().pushAndOpen(player, gui);
    }

    /** 打开已注册的自定义 GUI 页面（无额外数据） */
    public void openCustomGUI(String guiId, Player player) {
        openCustomGUI(guiId, player, null);
    }

    // ==================== 模块 GUI 增强注册 API ====================

    /**
     * 注册增强版自定义 GUI（支持图像绑定、布局定义、基岩表单、配置覆盖）。
     * {@code registration.moduleId} 必填，否则热卸载无法清理。
     */
    public void registerCustomGUI(ModuleGUIRegistration registration) {
        if (registration == null) {
            throw new IllegalArgumentException("registration cannot be null");
        }
        String owner = registration.getModuleId();
        if (owner == null || owner.isEmpty()) {
            throw new IllegalArgumentException(
                    "moduleId is required on ModuleGUIRegistration (call .moduleId(...))");
        }
        String guiId = registration.getGuiId();
        if (customGUIRegistry.containsKey(guiId)) {
            throw new IllegalArgumentException("guiId already registered: " + guiId);
        }
        customGUIRegistry.put(guiId, registration.getFactory());
        guiFactoryOwners.put(guiId, owner);
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        mm.getRegistry().registerCustomGUI(registration);
    }

    /**
     * 查询模块 GUI 是否注册了基岩表单提供者。
     */
    public BedrockFormProvider getBedrockFormProvider(String guiId) {
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        ModuleGUIRegistration reg = mm.getRegistry().getCustomGUIRegistration(guiId);
        return reg != null ? reg.getBedrockFormProvider() : null;
    }

    /**
     * 查询模块 GUI 是否注册了图像绑定。
     */
    public boolean hasModuleImageBinding(String guiId) {
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        ModuleGUIRegistration reg = mm.getRegistry().getCustomGUIRegistration(guiId);
        return reg != null && reg.getImageEntryId() != null;
    }

    /**
     * 查询模块 GUI 的布局定义。
     */
    public GUILayoutDefinition getModuleGUILayout(String guiId) {
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        ModuleGUIRegistration reg = mm.getRegistry().getCustomGUIRegistration(guiId);
        return reg != null ? reg.getLayout() : null;
    }

    /**
     * 获取模块 GUI 的配置覆盖实例。
     */
    public ModuleGUIConfig getModuleGUIConfig(String guiId) {
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        ModuleGUIRegistration reg = mm.getRegistry().getCustomGUIRegistration(guiId);
        return reg != null ? reg.getConfig() : null;
    }

    // ==================== 命令扩展 API ====================

    /** 注册子命令 */
    public void registerSubCommand(String parentCommand, String name,
                                   ModuleCommandHandler handler,
                                   String permission) {
        if (parentCommand == null || parentCommand.isEmpty() || name == null || name.isEmpty() || handler == null) {
            throw new IllegalArgumentException("parentCommand, name and handler cannot be null or empty");
        }
        
        commandRegistry.computeIfAbsent(parentCommand.toLowerCase(), k -> new java.util.concurrent.ConcurrentHashMap<>())
            .put(name.toLowerCase(), handler);
        
        if (permission != null) {
            permissionRegistry.computeIfAbsent(parentCommand.toLowerCase(), k -> new java.util.concurrent.ConcurrentHashMap<>())
                .put(name.toLowerCase(), permission);
        }
    }

    /** 注册子命令（带模块归属追踪，卸载时自动清理） */
    public void registerSubCommand(String moduleId, String parentCommand, String name,
                                   ModuleCommandHandler handler,
                                   String permission) {
        registerSubCommand(parentCommand, name, handler, permission);
        commandOwners.put(parentCommand.toLowerCase() + "." + name.toLowerCase(), moduleId);
    }

    /** 检查是否存在子命令 */
    public boolean hasSubCommand(String parentCommand, String name) {
        Map<String, ModuleCommandHandler> subCommands = commandRegistry.get(parentCommand.toLowerCase());
        return subCommands != null && subCommands.containsKey(name.toLowerCase());
    }

    /** 获取子命令处理器 */
    public ModuleCommandHandler getSubCommandHandler(String parentCommand, String name) {
        Map<String, ModuleCommandHandler> subCommands = commandRegistry.get(parentCommand.toLowerCase());
        return subCommands != null ? subCommands.get(name.toLowerCase()) : null;
    }

    /** 获取子命令权限 */
    public String getSubCommandPermission(String parentCommand, String name) {
        Map<String, String> permissions = permissionRegistry.get(parentCommand.toLowerCase());
        return permissions != null ? permissions.get(name.toLowerCase()) : null;
    }

    /** 获取所有子命令名称 */
    public List<String> getSubCommands(String parentCommand) {
        Map<String, ModuleCommandHandler> subCommands = commandRegistry.get(parentCommand.toLowerCase());
        return subCommands != null ? new ArrayList<>(subCommands.keySet()) : Collections.emptyList();
    }

    // ==================== 事件 API ====================

    /** 监听工会创建事件 */
    public void onGuildCreate(GuildEventHandler handler) {
        onGuildCreateHandlers.add(handler);
    }

    /** 监听工会解散事件 */
    public void onGuildDelete(GuildEventHandler handler) {
        onGuildDeleteHandlers.add(handler);
    }

    /** 监听成员加入工会事件 */
    public void onMemberJoin(MemberEventHandler handler) {
        onMemberJoinHandlers.add(handler);
    }

    /** 监听成员离开工会事件 */
    public void onMemberLeave(MemberEventHandler handler) {
        onMemberLeaveHandlers.add(handler);
    }

    /** 监听公会存款事件 */
    public void onEconomyDeposit(EconomyEventHandler handler) {
        onEconomyDepositHandlers.add(handler);
    }

    /** 监听公会取款事件 */
    public void onEconomyWithdraw(EconomyEventHandler handler) {
        onEconomyWithdrawHandlers.add(handler);
    }

    /** 监听成员角色变更事件 */
    public void onMemberRoleChange(MemberRoleChangeEventHandler handler) {
        onMemberRoleChangeHandlers.add(handler);
    }

    // ==================== 事件分发（供核心服务调用） ====================

    /** 分发工会创建事件 */
    public void fireGuildCreate(int guildId, String guildName, String leaderName) {
        if (onGuildCreateHandlers.isEmpty()) return;
        GuildEventData data = new GuildEventData(guildId, guildName, leaderName);
        for (GuildEventHandler handler : onGuildCreateHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onGuildCreate handler: " + e.getMessage(), e);
            }
        }
    }

    /** 分发工会解散事件 */
    public void fireGuildDelete(int guildId, String guildName, String leaderName) {
        if (onGuildDeleteHandlers.isEmpty()) return;
        GuildEventData data = new GuildEventData(guildId, guildName, leaderName);
        for (GuildEventHandler handler : onGuildDeleteHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onGuildDelete handler: " + e.getMessage(), e);
            }
        }
    }

    /** 分发成员加入事件 */
    public void fireMemberJoin(int guildId, String guildName, UUID playerUuid, String playerName) {
        if (onMemberJoinHandlers.isEmpty()) return;
        MemberEventData data = new MemberEventData(guildId, guildName, playerUuid, playerName, "JOIN");
        for (MemberEventHandler handler : onMemberJoinHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onMemberJoin handler: " + e.getMessage(), e);
            }
        }
    }

    /** 分发成员离开事件 */
    public void fireMemberLeave(int guildId, String guildName, UUID playerUuid, String playerName, String eventType) {
        if (onMemberLeaveHandlers.isEmpty()) return;
        MemberEventData data = new MemberEventData(guildId, guildName, playerUuid, playerName, eventType);
        for (MemberEventHandler handler : onMemberLeaveHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onMemberLeave handler: " + e.getMessage(), e);
            }
        }
    }

    /** 分发公会存款事件 */
    public void fireEconomyDeposit(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        if (onEconomyDepositHandlers.isEmpty()) return;
        EconomyEventData data = new EconomyEventData(guildId, guildName, playerUuid, playerName, amount, "DEPOSIT");
        for (EconomyEventHandler handler : onEconomyDepositHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onEconomyDeposit handler: " + e.getMessage(), e);
            }
        }
    }

    /** 分发公会取款事件 */
    public void fireEconomyWithdraw(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        if (onEconomyWithdrawHandlers.isEmpty()) return;
        EconomyEventData data = new EconomyEventData(guildId, guildName, playerUuid, playerName, amount, "WITHDRAW");
        for (EconomyEventHandler handler : onEconomyWithdrawHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onEconomyWithdraw handler: " + e.getMessage(), e);
            }
        }
    }

    /** 分发角色变更事件 */
    public void fireMemberRoleChange(int guildId, String guildName, UUID playerUuid, String playerName,
                                      String oldRole, String newRole) {
        if (onMemberRoleChangeHandlers.isEmpty()) return;
        MemberRoleChangeEventData data = new MemberRoleChangeEventData(guildId, guildName, playerUuid, playerName, oldRole, newRole);
        for (MemberRoleChangeEventHandler handler : onMemberRoleChangeHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onMemberRoleChange handler: " + e.getMessage(), e);
            }
        }
    }

    /** 清除指定模块注册的所有事件处理器（模块卸载时调用） */
    public void clearModuleHandlers(Object moduleInstance) {
        onGuildCreateHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onGuildDeleteHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onMemberJoinHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onMemberLeaveHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onEconomyDepositHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onEconomyWithdrawHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onMemberRoleChangeHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
    }

    /**
     * 移除指定模块注册的所有资源（占位符、子命令、自定义 GUI）。
     * 由 ModuleManager 在模块卸载时调用，配合 clearModuleHandlers 使用。
     */
    public void clearModuleRegistrations(String moduleId) {
        // 占位符提供者
        placeholderOwners.entrySet().removeIf(e -> {
            if (moduleId.equals(e.getValue())) {
                placeholderProviders.remove(e.getKey());
                return true;
            }
            return false;
        });

        // 子命令及权限
        commandOwners.entrySet().removeIf(e -> {
            if (moduleId.equals(e.getValue())) {
                String[] parts = e.getKey().split("\\.", 2);
                if (parts.length == 2) {
                    Map<String, ModuleCommandHandler> subs = commandRegistry.get(parts[0]);
                    if (subs != null) {
                        subs.remove(parts[1]);
                    }
                    Map<String, String> perms = permissionRegistry.get(parts[0]);
                    if (perms != null) {
                        perms.remove(parts[1]);
                    }
                }
                return true;
            }
            return false;
        });

        // 自定义 GUI 工厂（含 ModuleGUIRegistration 注册路径写入的归属）
        guiFactoryOwners.entrySet().removeIf(e -> {
            if (moduleId.equals(e.getValue())) {
                customGUIRegistry.remove(e.getKey());
                return true;
            }
            return false;
        });
    }

    /** 清除所有事件处理器和自定义 GUI 注册 */
    public void clearAll() {
        onGuildCreateHandlers.clear();
        onGuildDeleteHandlers.clear();
        onMemberJoinHandlers.clear();
        onMemberLeaveHandlers.clear();
        onEconomyDepositHandlers.clear();
        onEconomyWithdrawHandlers.clear();
        onMemberRoleChangeHandlers.clear();
        placeholderProviders.clear();
        customGUIRegistry.clear();
        commandRegistry.clear();
        permissionRegistry.clear();
        placeholderOwners.clear();
        commandOwners.clear();
        guiFactoryOwners.clear();
    }

    // ==================== 货币 API ====================

    /**
     * 获取货币管理器
     */
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    /**
     * 获取玩家的货币余额（缓存优先；未命中可能同步 JDBC，优先用异步版）
     */
    public double getCurrencyBalance(int guildId, UUID playerUuid, CurrencyManager.CurrencyType currencyType) {
        return currencyManager.getBalance(guildId, playerUuid, currencyType);
    }

    public CompletableFuture<Double> getCurrencyBalanceAsync(int guildId, UUID playerUuid,
                                                             CurrencyManager.CurrencyType currencyType) {
        return currencyManager.getBalanceAsync(guildId, playerUuid, currencyType);
    }

    /**
     * 增加玩家的货币（同步 JDBC；优先用异步版）
     */
    public boolean depositCurrency(int guildId, UUID playerUuid, String playerName,
                                 CurrencyManager.CurrencyType currencyType, double amount) {
        return currencyManager.deposit(guildId, playerUuid, playerName, currencyType, amount);
    }

    public CompletableFuture<Boolean> depositCurrencyAsync(int guildId, UUID playerUuid, String playerName,
                                                           CurrencyManager.CurrencyType currencyType, double amount) {
        return currencyManager.depositAsync(guildId, playerUuid, playerName, currencyType, amount);
    }

    /**
     * 减少玩家的货币（同步 JDBC；优先用异步版）
     */
    public boolean withdrawCurrency(int guildId, UUID playerUuid,
                                  CurrencyManager.CurrencyType currencyType, double amount) {
        return currencyManager.withdraw(guildId, playerUuid, currencyType, amount);
    }

    public CompletableFuture<Boolean> withdrawCurrencyAsync(int guildId, UUID playerUuid,
                                                            CurrencyManager.CurrencyType currencyType, double amount) {
        return currencyManager.withdrawAsync(guildId, playerUuid, currencyType, amount);
    }

    // 字符串版货币方法（v1.5 新增，与 SDK 桩签名一致）
    /** 获取玩家货币余额（字符串类型） */
    public double getCurrencyBalance(int guildId, UUID playerUuid, String currencyType) {
        try {
            return currencyManager.getBalance(guildId, playerUuid, CurrencyManager.CurrencyType.valueOf(currencyType.toUpperCase()));
        } catch (IllegalArgumentException e) { return 0.0; }
    }

    public CompletableFuture<Double> getCurrencyBalanceAsync(int guildId, UUID playerUuid, String currencyType) {
        try {
            return currencyManager.getBalanceAsync(guildId, playerUuid,
                    CurrencyManager.CurrencyType.valueOf(currencyType.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(0.0);
        }
    }

    /** 增加玩家货币（字符串类型） */
    public boolean depositCurrency(int guildId, UUID playerUuid, String playerName, String currencyType, double amount) {
        try {
            return currencyManager.deposit(guildId, playerUuid, playerName, CurrencyManager.CurrencyType.valueOf(currencyType.toUpperCase()), amount);
        } catch (IllegalArgumentException e) { return false; }
    }

    public CompletableFuture<Boolean> depositCurrencyAsync(int guildId, UUID playerUuid, String playerName,
                                                           String currencyType, double amount) {
        try {
            return currencyManager.depositAsync(guildId, playerUuid, playerName,
                    CurrencyManager.CurrencyType.valueOf(currencyType.toUpperCase()), amount);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    /** 减少玩家货币（字符串类型） */
    public boolean withdrawCurrency(int guildId, UUID playerUuid, String currencyType, double amount) {
        try {
            return currencyManager.withdraw(guildId, playerUuid, CurrencyManager.CurrencyType.valueOf(currencyType.toUpperCase()), amount);
        } catch (IllegalArgumentException e) { return false; }
    }

    public CompletableFuture<Boolean> withdrawCurrencyAsync(int guildId, UUID playerUuid,
                                                            String currencyType, double amount) {
        try {
            return currencyManager.withdrawAsync(guildId, playerUuid,
                    CurrencyManager.CurrencyType.valueOf(currencyType.toUpperCase()), amount);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    // ==================== 成员管理 API（v1.5 新增） ====================

    /** 向公会添加成员（异步） */
    public CompletableFuture<Boolean> addMember(int guildId, UUID playerUuid, String playerName, String role) {
        try {
            com.guild.models.GuildMember.Role r = com.guild.models.GuildMember.Role.valueOf(role.toUpperCase());
            return plugin.getGuildService().addGuildMemberAsync(guildId, playerUuid, playerName, r);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    /** 从公会移除成员（异步，使用直接管理方法跳过权限检查） */
    public CompletableFuture<Boolean> removeMember(int guildId, UUID playerUuid) {
        return plugin.getGuildService().removeGuildMemberDirectAsync(guildId, playerUuid);
    }

    /** 修改成员角色（异步，使用直接管理方法跳过权限检查） */
    public CompletableFuture<Boolean> setMemberRole(int guildId, UUID playerUuid, String role) {
        return plugin.getGuildService().updateMemberRoleDirectAsync(guildId, playerUuid, role);
    }

    // ==================== 占位符扩展 API（v1.5 新增） ====================

    /** 注册自定义占位符提供者 */
    public void registerPlaceholderProvider(PlaceholderProvider provider) {
        if (provider == null || provider.getIdentifier() == null || provider.getIdentifier().trim().isEmpty()) {
            return;
        }
        placeholderProviders.put(provider.getIdentifier().toLowerCase(), provider);
    }

    /** 注册自定义占位符提供者（带模块归属追踪，卸载时自动清理） */
    public void registerPlaceholderProvider(String moduleId, PlaceholderProvider provider) {
        registerPlaceholderProvider(provider);
        if (provider != null && provider.getIdentifier() != null && !provider.getIdentifier().trim().isEmpty()) {
            placeholderOwners.put(provider.getIdentifier().toLowerCase(), moduleId);
        }
    }

    /** 注销占位符提供者 */
    public void unregisterPlaceholderProvider(String identifier) {
        if (identifier == null) {
            return;
        }
        placeholderProviders.remove(identifier.toLowerCase());
    }

    /** 获取所有已注册的占位符提供者 */
    public Map<String, PlaceholderProvider> getPlaceholderProviders() {
        return placeholderProviders;
    }

    // ==================== HTTP 工具 API ====================

    /** 发送 GET 请求（异步，不阻塞主线程） */
    public CompletableFuture<String> httpGet(String url, Map<String, String> headers) {
        return httpClient.httpGet(url, headers);
    }

    /** 发送 GET 请求（无自定义请求头） */
    public CompletableFuture<String> httpGet(String url) {
        return httpGet(url, null);
    }

    /** 发送 POST 请求（异步，不阻塞主线程） */
    public CompletableFuture<String> httpPost(String url, String body,
                                               Map<String, String> headers) {
        return httpClient.httpPost(url, body, headers);
    }

    /** 获取 HTTP 客户端提供者（用于高级配置） */
    public HttpClientProvider getHttpClient() {
        return httpClient;
    }

    // ==================== Server time API ====================

    /**
     * Get the current server local time.
     */
    public LocalDateTime getServerTime() {
        return TimeProvider.nowLocalDateTime();
    }

    /**
     * Get the current server local time as yyyy-MM-dd HH:mm:ss.
     */
    public String getServerTimeString() {
        return TimeProvider.nowString();
    }

    /**
     * Get the current server local date as yyyy-MM-dd.
     */
    public String getServerDateString() {
        return TimeProvider.formatDate(TimeProvider.nowLocalDateTime());
    }

    /**
     * Get the server local time after adding the given minutes.
     */
    public String getServerTimePlusMinutes(int minutes) {
        return TimeProvider.plusMinutesString(minutes);
    }

    /**
     * Get the server local time after adding the given days.
     */
    public String getServerTimePlusDays(int days) {
        return TimeProvider.plusDaysString(days);
    }

    /**
     * Format a LocalDateTime using the server full formatter.
     */
    public String formatServerTime(LocalDateTime dateTime) {
        return TimeProvider.format(dateTime);
    }

    /**
     * Format a LocalDateTime using the server date-only formatter.
     */
    public String formatServerDate(LocalDateTime dateTime) {
        return TimeProvider.formatDate(dateTime);
    }

    // ==================== Console output API ====================

    /**
     * Print a green console INFO message with color codes.
     */
    public void consoleInfo(String message) {
        ConsoleLogger.info(message);
    }

    /**
     * Print a yellow console WARN message with color codes.
     */
    public void consoleWarn(String message) {
        ConsoleLogger.warn(message);
    }

    /**
     * Print a red console SEVERE message with color codes.
     */
    public void consoleSevere(String message) {
        ConsoleLogger.severe(message);
    }

    /**
     * Print a green console INFO message with indexed placeholders.
     */
    public void consoleInfo(String message, String... args) {
        ConsoleLogger.info(message, args);
    }

    /**
     * Print a yellow console WARN message with indexed placeholders.
     */
    public void consoleWarn(String message, String... args) {
        ConsoleLogger.warn(message, args);
    }

    /**
     * Print a red console SEVERE message with indexed placeholders.
     */
    public void consoleSevere(String message, String... args) {
        ConsoleLogger.severe(message, args);
    }

    // ==================== Module language resource API ====================

    /**
     * Load module language resources for the given module ID.
     * Delegates to LanguageManager which will attempt external then bundled files.
     */
    public boolean loadModuleLanguageResource(String moduleId, String lang) {
        if (moduleId == null || moduleId.trim().isEmpty()) return false;
        if (lang == null || lang.trim().isEmpty()) {
            return plugin.getLanguageManager().loadModuleLanguageResourcesForModule(moduleId);
        }
        return plugin.getLanguageManager().loadModuleLanguageResourcesForModule(moduleId, lang.toLowerCase());
    }

    /**
     * Release a bundled module language resource for a specific language to disk.
     */
    public boolean releaseModuleLanguageResource(String moduleId, String lang) {
        if (moduleId == null || moduleId.trim().isEmpty() || lang == null || lang.trim().isEmpty()) return false;
        String moduleDirName = moduleId.toLowerCase();
        String language = lang.toLowerCase();
        String resourcePath = "lang/modules/" + moduleDirName + "/" + language + ".yml";
        if (plugin.getResource(resourcePath) == null) {
            return false;
        }
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (file.exists()) return false;
        try {
            plugin.saveResource(resourcePath, false);
            plugin.getLogger().info("Extracted bundled module language file: " + resourcePath);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to extract bundled module language file " + resourcePath + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Return the File object for module language file under plugin data folder.
     */
    public File getModuleLanguageFile(String moduleId, String lang) {
        if (moduleId == null || moduleId.trim().isEmpty() || lang == null || lang.trim().isEmpty()) return null;
        String moduleDirName = moduleId.toLowerCase();
        String language = lang.toLowerCase();
        return new File(plugin.getDataFolder(), "lang/modules/" + moduleDirName + "/" + language + ".yml");
    }

    // ==================== 内部工具方法 ====================

    /**
     * 将核心 Guild 对象转换为安全的 DTO 对象
     * 映射关系：
     * - leaderUuid -> DTO masterUuid
     * - leaderName -> DTO masterName
     * - level -> DTO level (int)
     * - description -> DTO motto
     * - createdAt -> DTO createTime (long epoch)
     */
    private GuildData convertGuild(com.guild.models.Guild guild) {
        if (guild == null) return null;
        long createTimeMillis = 0L;
        try {
            LocalDateTime createdAt = guild.getCreatedAt();
            if (createdAt != null) {
                createTimeMillis = createdAt.atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
            }
        } catch (Exception ignored) {}

        int memberCount;
        try {
            memberCount = plugin.getGuildService().getGuildMemberCount(guild.getId());
        } catch (Exception ignored) {
            memberCount = 0;
        }

        return new GuildData(
                guild.getId(),
                guild.getName(),
                guild.getLeaderUuid(),
                guild.getLeaderName(),
                guild.getLevel(),
                0L,  // experience: not in core model
                guild.getBalance(),
                memberCount,
                guild.getMaxMembers(),
                guild.getDescription(),
                createTimeMillis,
                null  // members list loaded on demand
        );
    }

    /**
     * 将核心 GuildMember 对象转换为安全的 DTO 对象
     */
    private MemberData convertMember(com.guild.models.GuildMember member) {
        if (member == null) return null;
        long joinTimeMillis = 0L;
        try {
            LocalDateTime joinedAt = member.getJoinedAt();
            if (joinedAt != null) {
                joinTimeMillis = joinedAt.atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
            }
        } catch (Exception ignored) {}

        boolean online = org.bukkit.Bukkit.getPlayer(member.getPlayerUuid()) != null;

        // 查询投入资金
        double investedBalance = 0.0;
        try {
            com.guild.services.GuildInvestmentService invSvc =
                plugin.getServiceContainer().get(com.guild.services.GuildInvestmentService.class);
            if (invSvc != null) {
                investedBalance = invSvc.getInvestedBalance(member.getGuildId(), member.getPlayerUuid());
            }
        } catch (Exception ignored) {}

        // 净贡献：优先短缓存；未命中时保持 0，避免 SDK 转换路径同步打库
        double contribution = 0.0;
        try {
            var cache = plugin.getGuildPlayerDataCache();
            if (cache != null) {
                var snap = cache.get(member.getPlayerUuid());
                if (snap.contributionNet != null) {
                    contribution = snap.contributionNet;
                }
            }
        } catch (Exception ignored) {}

        return new MemberData(
                member.getPlayerUuid(),
                member.getPlayerName(),
                member.getRole().name(),
                joinTimeMillis,
                contribution,
                online,
                investedBalance
        );
    }
}