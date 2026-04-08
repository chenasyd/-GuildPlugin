package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.core.module.ModuleManager;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.gui.ModuleGUIFactory;
import com.guild.sdk.http.HttpClientProvider;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 公会插件 SDK - 统一 API 门面
 */
public class GuildPluginAPI {

    private final GuildPlugin plugin;
    private final HttpClientProvider httpClient;

    // 事件处理器列表（线程安全）
    private final List<GuildEventHandler> onGuildCreateHandlers = new CopyOnWriteArrayList<>();
    private final List<GuildEventHandler> onGuildDeleteHandlers = new CopyOnWriteArrayList<>();
    private final List<MemberEventHandler> onMemberJoinHandlers = new CopyOnWriteArrayList<>();
    private final List<MemberEventHandler> onMemberLeaveHandlers = new CopyOnWriteArrayList<>();

    public GuildPluginAPI(GuildPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = new HttpClientProvider();
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

    /** 注册全新的自定义 GUI 页面（预留接口） */
    public void registerCustomGUI(String guiId, ModuleGUIFactory factory) {
        // TODO: 在后续迭代中实现自定义 GUI 注册表
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

        return new GuildData(
                guild.getId(),
                guild.getName(),
                guild.getLeaderUuid(),
                guild.getLeaderName(),
                guild.getLevel(),
                0L,  // experience 字段在核心模型中不存在，暂用0
                guild.getBalance(),
                0,   // memberCount 通过成员列表 size() 获取
                guild.getMaxMembers(),
                guild.getDescription(),  // description 映射为 motto
                createTimeMillis,
                null  // members 列表可按需加载
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

        return new MemberData(
                member.getPlayerUuid(),
                member.getPlayerName(),
                member.getRole().name(),
                joinTimeMillis,
                0.0,  // contribution 在核心模型中不存在
                false  // online 在核心模型中不存在
        );
    }
}
