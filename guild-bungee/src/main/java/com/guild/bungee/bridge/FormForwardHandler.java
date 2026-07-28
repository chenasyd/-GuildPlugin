package com.guild.bungee.bridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.guild.bungee.data.BungeeMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 代理端 Cumulus 表单转发器。
 *
 * <p>接收后端子服发来的 {@code guild.form.send} 消息，通过 Geyser-BungeeCord 的
 * ClassLoader 反序列化 Cumulus 表单，调用 Geyser API 发送给基岩版玩家。
 * 玩家操作后的响应通过 {@code guild.form.response} 回传到后端子服。
 *
 * <h3>反射策略</h3>
 * <p>
 * guild-bungee 没有 Cumulus/Geyser 编译依赖。所有 Geyser 和 Cumulus 类
 * 通过 Geyser-BungeeCord 插件的 ClassLoader 反射加载（与 PlayerTypeDetector
 * 相同的模式）。
 *
 * <h3>消息协议</h3>
 * <pre>
 * 后端 → 代理 (guild.form.send):
 *   { "uuid": "...", "formId": "...", "formType": 0, "formJson": "..." }
 *
 * 代理 → 后端 (guild.form.response):
 *   { "uuid": "...", "formId": "...", "responseData": "..."|null, "closed": bool }
 * </pre>
 */
public final class FormForwardHandler {

    private static final String GEYSER_PLUGIN_NAME = "Geyser-BungeeCord";

    private final Logger logger;
    private final CrossServerBridge bridge;

    /** 缓存的 Geyser ClassLoader（延迟获取） */
    private volatile ClassLoader geyserClassLoader;

    /** 缓存的 Geyser API 实例 */
    private volatile Object geyserApiInstance;

    /** GeyserApiBase.sendForm(UUID, Form) 方法句柄 */
    private volatile Method sendFormMethod;

    /** 是否已完成初始化检测 */
    private volatile boolean checked;

    public FormForwardHandler(Logger logger, CrossServerBridge bridge) {
        this.logger = logger;
        this.bridge = bridge;
    }

    // ── 消息处理 ────────────────────────────────────────────────

    /**
     * 处理后端发来的 guild.form.send 消息。
     *
     * @param message      消息对象
     * @param sourceServer 来源服务器（用于回传响应）
     */
    public void handleFormSend(BungeeMessage message, ServerInfo sourceServer) {
        try {
            String payload = message.getPayload();
            JsonObject data = JsonParser.parseString(payload).getAsJsonObject();

            String uuidStr = data.get("uuid").getAsString();
            String formId = data.get("formId").getAsString();
            int formType = data.get("formType").getAsInt();
            String formJson = data.get("formJson").getAsString();

            UUID uuid = UUID.fromString(uuidStr);

            // 确认玩家在线
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
            if (player == null || !player.isConnected()) {
                logger.fine("[FormForward] Player not online, discarding form: " + uuidStr);
                sendFormResponse(sourceServer, uuidStr, formId, null, true);
                return;
            }

            // 确保 Geyser 可用
            if (!ensureGeyserReady()) {
                logger.warning("[FormForward] Geyser unavailable, cannot forward form");
                sendFormResponse(sourceServer, uuidStr, formId, null, true);
                return;
            }

            // 通过 Geyser ClassLoader 反序列化表单并发送
            boolean sent = deserializeAndSend(uuid, formType, formJson, formId, sourceServer);

            if (sent) {
                logger.info("[FormForward] Forwarded form " + formId
                        + " → " + player.getName() + " (type=" + formType + ")");
            } else {
                logger.warning("[FormForward] Form forwarding failed: " + formId);
                sendFormResponse(sourceServer, uuidStr, formId, null, true);
            }

        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[FormForward] Failed to handle guild.form.send: " + e.getMessage(), e);
        }
    }

    // ── Geyser 反射 ─────────────────────────────────────────────

    /**
     * 延迟检测 Geyser 可用性并缓存反射句柄。
     */
    private boolean ensureGeyserReady() {
        if (checked && geyserApiInstance != null) return true;
        if (checked) return false; // 已检测过且不可用

        synchronized (this) {
            if (checked && geyserApiInstance != null) return true;
            if (checked) return false;

            try {
                net.md_5.bungee.api.plugin.Plugin geyserPlugin =
                        ProxyServer.getInstance().getPluginManager().getPlugin(GEYSER_PLUGIN_NAME);
                if (geyserPlugin == null) {
                    logger.warning("[FormForward] " + GEYSER_PLUGIN_NAME + " not found");
                    checked = true;
                    return false;
                }

                ClassLoader loader = geyserPlugin.getClass().getClassLoader();

                // 获取 Geyser API 实例
                Class<?> geyserClass = Class.forName("org.geysermc.api.Geyser", true, loader);
                boolean registered = (boolean) geyserClass.getMethod("isRegistered").invoke(null);
                if (!registered) {
                    logger.warning("[FormForward] Geyser API not registered");
                    checked = true;
                    return false;
                }

                geyserApiInstance = geyserClass.getMethod("api").invoke(null);

                // 获取 sendForm(UUID, Form) 方法
                Class<?> formClass = Class.forName(
                        "org.geysermc.cumulus.form.Form", true, loader);
                sendFormMethod = geyserApiInstance.getClass()
                        .getMethod("sendForm", UUID.class, formClass);

                geyserClassLoader = loader;
                checked = true;

                logger.info("[FormForward] Geyser API ready — form forwarding enabled.");
                return true;

            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "[FormForward] Geyser initialization failed: " + e.getMessage(), e);
                checked = true;
                return false;
            }
        }
    }

    /**
     * 通过 Geyser ClassLoader 反序列化 Cumulus 表单并发送。
     * <p>
     * 附加响应转发 handler：玩家操作后将原始响应回传到后端子服。
     */
    private boolean deserializeAndSend(UUID uuid, int formType, String formJson,
                                        String formId, ServerInfo sourceServer) {
        try {
            ClassLoader loader = geyserClassLoader;

            // 加载 Cumulus 类
            Class<?> formsClass = Class.forName(
                    "org.geysermc.cumulus.Forms", true, loader);
            Class<?> formTypeClass = Class.forName(
                    "org.geysermc.cumulus.form.util.FormType", true, loader);

            // 获取 FormType 枚举值
            Object type = formTypeClass.getMethod("fromOrdinal", int.class)
                    .invoke(null, formType);

            // 响应转发 handler：玩家操作后回传到后端
            String uuidStr = uuid.toString();
            BiConsumer<Object, String> responseHandler = (form, responseData) ->
                    sendFormResponse(sourceServer, uuidStr, formId, responseData,
                            responseData == null);

            // 反序列化表单
            Method fromJson = formsClass.getMethod(
                    "fromJson", String.class, formTypeClass, BiConsumer.class);
            Object form = fromJson.invoke(null, formJson, type, responseHandler);

            // 通过 Geyser API 发送给玩家
            return (boolean) sendFormMethod.invoke(geyserApiInstance, uuid, form);

        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[FormForward] Deserialization/send failed: " + e.getMessage(), e);
            return false;
        }
    }

    // ── 响应回传 ────────────────────────────────────────────────

    /**
     * 将表单响应回传到后端子服。
     *
     * @param targetServer 目标后端服务器
     * @param uuid         玩家 UUID 字符串
     * @param formId       表单唯一标识
     * @param responseData 原始响应 JSON（null 表示表单关闭）
     * @param closed       是否为关闭操作
     */
    private void sendFormResponse(ServerInfo targetServer, String uuid,
                                   String formId, String responseData, boolean closed) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("uuid", uuid);
            payload.addProperty("formId", formId);
            if (responseData != null) {
                payload.addProperty("responseData", responseData);
            }
            payload.addProperty("closed", closed);

            BungeeMessage responseMsg = BungeeMessage.create(
                    "guild.form.response", "guild-bungee")
                    .payload(payload.toString())
                    .build();

            bridge.forwardToServerPublic(targetServer, responseMsg);

            logger.fine("[FormForward] Response forwarded: formId=" + formId
                    + " → " + targetServer.getName()
                    + " (closed=" + closed + ")");
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[FormForward] Response forwarding failed: " + e.getMessage(), e);
        }
    }
}
