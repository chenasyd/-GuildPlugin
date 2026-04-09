package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.core.module.ModuleManager;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.GuildEventData;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.event.MemberEventData;
import com.guild.sdk.gui.ModuleGUIFactory;
import com.guild.sdk.http.HttpClientProvider;
import com.guild.core.gui.GUI;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    // 事件处理器列表（线程安全，所有模块共享）
    private final List<GuildEventHandler> onGuildCreateHandlers = new CopyOnWriteArrayList<>();
    private final List<GuildEventHandler> onGuildDeleteHandlers = new CopyOnWriteArrayList<>();
    private final List<MemberEventHandler> onMemberJoinHandlers = new CopyOnWriteArrayList<>();
    private final List<MemberEventHandler> onMemberLeaveHandlers = new CopyOnWriteArrayList<>();

    // 自定义 GUI 注册表 (guiId -> factory)
    private final Map<String, ModuleGUIFactory> customGUIRegistry = new java.util.concurrent.ConcurrentHashMap<>();

    public GuildPluginAPI(GuildPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = new HttpClientProvider();
        this.logger = Logger.getLogger("GuildPlugin.API");
    }

    // ==================== 工会查询 API ====================

    /** 根据 ID 获取工会信息（异步） */
    public CompletableFuture<GuildData> getGuildById(int id) {
        return CompletableFuture.supplyAsync(() ->
                convertGuild(plugin.getGuildService().getGuildById(id))
        );
    }

    /** 根据名称获取工会信息（异步） */
    public CompletableFuture<GuildData> getGuildByName(String name) {
        return CompletableFuture.supplyAsync(() ->
                convertGuild(plugin.getGuildService().getGuildByName(name))
        );
    }

    /** 获取玩家所属工会（异步） */
    public CompletableFuture<GuildData> getPlayerGuild(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() ->
                convertGuild(plugin.getGuildService().getPlayerGuild(playerUuid))
        );
    }

    /** 获取所有工会列表（异步） */
    public CompletableFuture<List<GuildData>> getAllGuilds() {
        return CompletableFuture.supplyAsync(() ->
                plugin.getGuildService().getAllGuilds().stream()
                        .map(this::convertGuild)
                        .filter(g -> g != null).toList()
        );
    }

    // ==================== 成员查询 API ====================

    /** 获取工会成员列表（异步） */
    public CompletableFuture<List<MemberData>> getGuildMembers(int guildId) {
        return CompletableFuture.supplyAsync(() ->
                plugin.getGuildService().getGuildMembers(guildId).stream()
                        .map(this::convertMember).toList()
        );
    }

    // ==================== GUI 扩展 API ====================

    /**
     * 在指定 GUI 界面中注入自定义按钮
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

    /** 注册全新的自定义 GUI 页面 */
    public void registerCustomGUI(String guiId, ModuleGUIFactory factory) {
        if (guiId == null || guiId.isEmpty()) {
            throw new IllegalArgumentException("guiId cannot be empty");
        }
        if (customGUIRegistry.containsKey(guiId)) {
            throw new IllegalArgumentException("guiId already registered: " + guiId);
        }
        customGUIRegistry.put(guiId, factory);
    }

    /** 注销自定义 GUI 页面（模块卸载时调用） */
    public void unregisterCustomGUI(String guiId) {
        customGUIRegistry.remove(guiId);
    }

    /** 打开已注册的自定义 GUI 页面 */
    public void openCustomGUI(String guiId, Player player, Map<String, Object> data) {
        ModuleGUIFactory factory = customGUIRegistry.get(guiId);
        if (factory == null) {
            logger.warning("custom GUI not found: " + guiId);
            return;
        }
        GUI gui = factory.create(player, data != null ? data : Map.of());
        plugin.getGuiManager().openGUI(player, gui);
    }

    /** 打开已注册的自定义 GUI 页面（无额外数据） */
    public void openCustomGUI(String guiId, Player player) {
        openCustomGUI(guiId, player, null);
    }

    // ==================== 命令扩展 API ====================

    /** 注册子命令（预留接口） */
    public void registerSubCommand(String parentCommand, String name,
                                   ModuleCommandHandler handler,
                                   String permission) {
        // TODO: 实现基于反射或 Map 的子命令分发机制
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

    /** 清除指定模块注册的所有事件处理器（模块卸载时调用） */
    public void clearModuleHandlers(Object moduleInstance) {
        onGuildCreateHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onGuildDeleteHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onMemberJoinHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onMemberLeaveHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
    }

    /** 清除所有事件处理器和自定义 GUI 注册 */
    public void clearAll() {
        onGuildCreateHandlers.clear();
        onGuildDeleteHandlers.clear();
        onMemberJoinHandlers.clear();
        onMemberLeaveHandlers.clear();
        customGUIRegistry.clear();
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

        int memberCount = plugin.getGuildService().getGuildMembers(guild.getId()).size();

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

        return new MemberData(
                member.getPlayerUuid(),
                member.getPlayerName(),
                member.getRole().name(),
                joinTimeMillis,
                0.0,  // contribution: not in core model
                online
        );
    }
}
