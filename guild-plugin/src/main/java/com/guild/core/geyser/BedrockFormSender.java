package com.guild.core.geyser;

import com.guild.comm.api.BungeeClientAPI;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.impl.FormDefinitions;
import org.geysermc.cumulus.form.util.FormType;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * 基岩版表单发送工具 — 支持本地 Geyser 直发和 BungeeCord 代理转发两种模式。
 *
 * <h3>传输模式</h3>
 * <ol>
 *   <li><b>本地 Geyser</b>（单服模式）：表单序列化为 JSON → 通过 Geyser ClassLoader
 *       反序列化为 Geyser 的 Cumulus Form → 调用 Geyser API 发送。
 *       使用 JSON 桥接避免 shade 重定位后的 ClassLoader 不匹配。</li>
 *   <li><b>代理转发</b>（BungeeCord 模式）：表单序列化为 JSON → 通过 BungeeClientAPI
 *       发送到代理 → 代理用其 Geyser 实例反序列化并发送 → 玩家响应回传后端。</li>
 * </ol>
 *
 * <h3>响应处理</h3>
 * <p>
 * 发送时原始 Form 对象（含 validResultHandler/closedResultHandler）存入
 * {@code pendingForms} 映射。响应到达后通过 Cumulus 的
 * {@code FormDefinitions.definitionFor(form).handleFormResponse(form, data)}
 * 触发原始 handler。
 *
 * <p>在 GuildPlugin#onEnable 中调用 {@link #initialize(Logger)} 初始化。
 */
public final class BedrockFormSender {

    /** 本地 Geyser API 实例（Geyser 未安装时为 null） */
    private static Object localApiInstance;
    /** 本地 Geyser sendForm(UUID, Form) 方法句柄 */
    private static Method localSendFormMethod;
    /** 本地 Geyser 插件的 ClassLoader（用于加载 Geyser 侧的 Cumulus 类） */
    private static ClassLoader localGeyserClassLoader;
    /** 本地 Geyser 是否可用 */
    private static boolean localGeyserReady = false;

    private static Logger logger;

    /**
     * 待响应表单映射：formId → 原始 Form 对象（含 handler）。
     * 发送时存入，响应到达时移除并触发 handler。
     */
    private static final ConcurrentHashMap<String, Form> pendingForms = new ConcurrentHashMap<>();

    private BedrockFormSender() {}

    // ── 初始化 ──────────────────────────────────────────────────

    /**
     * 初始化表单发送器。检测本地 Geyser 可用性。
     * 代理转发模式通过 BungeeClientAPI.isInitialized() 动态检测。
     */
    public static void initialize(Logger pluginLogger) {
        logger = pluginLogger;
        initLocalGeyser();
    }

    /**
     * 检测本地 Geyser-Spigot 并获取 API 句柄。
     * 使用 Geyser 插件的 ClassLoader 加载其 API 类（避免 ClassLoader 隔离问题）。
     */
    private static void initLocalGeyser() {
        try {
            Plugin geyserPlugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (geyserPlugin == null) {
                logger.info("[BedrockForm] Local Geyser not installed - using proxy forwarding mode.");
                return;
            }

            ClassLoader geyserLoader = geyserPlugin.getClass().getClassLoader();

            // 通过 Geyser ClassLoader 加载 API 类
            Class<?> geyserClass = Class.forName("org.geysermc.api.Geyser", true, geyserLoader);
            Method apiMethod = geyserClass.getMethod("api");
            localApiInstance = apiMethod.invoke(null);

            // 获取 sendForm(UUID, Form) 方法 — Form 类也来自 Geyser 的 ClassLoader
            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.Form", true, geyserLoader);
            localSendFormMethod = localApiInstance.getClass()
                    .getMethod("sendForm", UUID.class, formClass);

            localGeyserClassLoader = geyserLoader;
            localGeyserReady = true;
            logger.info("[BedrockForm] Local Geyser detected - direct send mode enabled.");
        } catch (ClassNotFoundException e) {
            logger.info("[BedrockForm] Geyser API class unavailable - using proxy forwarding mode.");
        } catch (Exception e) {
            logger.info("[BedrockForm] Local Geyser initialization failed ("
                    + e.getClass().getSimpleName() + ") - using proxy forwarding mode.");
        }
    }

    // ── 公共 API ────────────────────────────────────────────────

    /**
     * 表单发送是否可用（本地 Geyser 或代理转发任一可用）。
     */
    public static boolean isAvailable() {
        return localGeyserReady || BungeeClientAPI.isInitialized();
    }

    /**
     * 向基岩玩家发送 Cumulus 表单。
     * <p>
     * 表单先序列化为 JSON，然后根据可用传输模式发送：
     * 优先本地 Geyser 直发，回退到 BungeeCord 代理转发。
     *
     * @param uuid 玩家 UUID
     * @param formObj Cumulus 表单实例（SimpleForm / CustomForm），以 Object 传递
     * @return true 发送成功
     */
    public static boolean sendForm(UUID uuid, Object formObj) {
        if (!(formObj instanceof Form)) {
            if (logger != null) {
                logger.warning("[BedrockForm] sendForm: invalid form object: "
                        + (formObj != null ? formObj.getClass().getName() : "null"));
            }
            return false;
        }

        Form form = (Form) formObj;
        String formId = UUID.randomUUID().toString();

        // 序列化表单为 JSON（使用 shade 重定位后的 Cumulus）
        String json;
        int typeOrdinal;
        try {
            json = FormDefinitions.instance().codecFor(form).jsonData(form);
            // Form 接口无 type() 方法，通过 instanceof 判断类型
            if (form instanceof SimpleForm) {
                typeOrdinal = FormType.SIMPLE_FORM.ordinal();
            } else if (form instanceof ModalForm) {
                typeOrdinal = FormType.MODAL_FORM.ordinal();
            } else if (form instanceof CustomForm) {
                typeOrdinal = FormType.CUSTOM_FORM.ordinal();
            } else {
                if (logger != null) {
                    logger.warning("[BedrockForm] Unknown form type: " + form.getClass().getName());
                }
                return false;
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("[BedrockForm] Form serialization failed: " + e.getMessage());
            }
            return false;
        }

        // 存入待响应映射（响应到达时触发原始 handler）
        pendingForms.put(formId, form);

        // 优先本地 Geyser 直发
        if (localGeyserReady) {
            if (sendViaLocalGeyser(uuid, formId, json, typeOrdinal)) {
                return true;
            }
        }

        // 回退到代理转发
        if (BungeeClientAPI.isInitialized()) {
            if (sendViaProxy(uuid, formId, json, typeOrdinal)) {
                return true;
            }
        }

        // 所有传输方式均失败
        pendingForms.remove(formId);
        if (logger != null) {
            logger.warning("[BedrockForm] No available transport (localGeyser="
                    + localGeyserReady + ", proxy=" + BungeeClientAPI.isInitialized() + ")");
        }
        return false;
    }

    /**
     * 处理表单响应（由代理回传或本地 Geyser 桥接触发）。
     * <p>
     * 查找原始 Form 对象，通过 Cumulus 的 handleFormResponse 触发
     * 原始 validResultHandler / closedResultHandler。
     *
     * @param formId       表单唯一标识
     * @param responseData 原始响应 JSON（null 或空字符串表示表单被关闭）
     */
    public static void handleFormResponse(String formId, String responseData) {
        Form form = pendingForms.remove(formId);
        if (form == null) {
            if (logger != null) {
                logger.warning("[BedrockForm] No matching pending form: " + formId);
            }
            return;
        }
        try {
            // 空字符串触发 closedResultHandler，有效 JSON 触发 validResultHandler
            String data = (responseData != null) ? responseData : "";
            FormDefinitions.instance().definitionFor(form).handleFormResponse(form, data);
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("[BedrockForm] Response handling failed: " + e.getMessage());
            }
        }
    }

    /**
     * 清理资源（onDisable 时调用）。
     */
    public static void shutdown() {
        pendingForms.clear();
        localGeyserReady = false;
        localApiInstance = null;
        localSendFormMethod = null;
        localGeyserClassLoader = null;
    }

    /**
     * @return 当前待响应表单数量（调试用）
     */
    public static int getPendingFormCount() {
        return pendingForms.size();
    }

    // ── 传输实现 ────────────────────────────────────────────────

    /**
     * 本地 Geyser 直发：JSON → Geyser ClassLoader 反序列化 → Geyser API 发送。
     * <p>
     * 反序列化时附加响应转发 handler，将原始响应回传到本类的
     * {@link #handleFormResponse(String, String)}。
     */
    private static boolean sendViaLocalGeyser(UUID uuid, String formId,
                                               String json, int typeOrdinal) {
        try {
            // 通过 Geyser ClassLoader 加载 Cumulus 类
            Class<?> formsClass = Class.forName(
                    "org.geysermc.cumulus.Forms", true, localGeyserClassLoader);
            Class<?> formTypeClass = Class.forName(
                    "org.geysermc.cumulus.form.util.FormType", true, localGeyserClassLoader);

            // 获取 FormType 枚举值
            Object formType = formTypeClass.getMethod("fromOrdinal", int.class)
                    .invoke(null, typeOrdinal);

            // 响应转发 handler：Geyser 侧收到玩家响应后回调此 handler
            BiConsumer<Object, String> responseHandler = (geyserForm, responseData) ->
                    handleFormResponse(formId, responseData);

            // 反序列化为 Geyser 侧的 Form 对象
            Method fromJson = formsClass.getMethod(
                    "fromJson", String.class, formTypeClass, BiConsumer.class);
            Object geyserForm = fromJson.invoke(null, json, formType, responseHandler);

            // 通过 Geyser API 发送
            localSendFormMethod.invoke(localApiInstance, uuid, geyserForm);
            return true;
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("[BedrockForm] Local Geyser send failed: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * 代理转发：JSON → BungeeClientAPI → 代理端 Geyser 发送。
     * <p>
     * 消息类型 {@code guild.form.send}，代理端收到后反序列化并通过
     * 其 Geyser 实例发送。玩家响应通过 {@code guild.form.response} 回传。
     */
    private static boolean sendViaProxy(UUID uuid, String formId,
                                         String json, int typeOrdinal) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("uuid", uuid.toString());
            payload.addProperty("formId", formId);
            payload.addProperty("formType", typeOrdinal);
            payload.addProperty("formJson", json);

            BungeeClientAPI.sendToBungee("guild.form.send", payload.toString());
            return true;
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("[BedrockForm] Proxy forwarding failed: " + e.getMessage());
            }
            return false;
        }
    }
}
