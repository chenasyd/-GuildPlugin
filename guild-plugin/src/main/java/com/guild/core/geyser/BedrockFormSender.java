package com.guild.core.geyser;

import org.geysermc.cumulus.form.Form;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 基岩版表单发送工具 — 通过反射调用 Geyser API 发送 Cumulus 表单。
 * <p>
 * 零编译依赖于 Geyser/Floodgate，运行时通过反射获取
 * {@code org.geysermc.api.Geyser.api().sendForm(UUID, Form)}。
 * <p>
 * 在 GuildPlugin#onEnable 中调用 {@link #initialize(Logger)} 初始化。
 */
public final class BedrockFormSender {

    private static Object apiInstance;
    private static Method sendFormMethod;
    private static boolean ready = false;
    private static Logger logger;

    private BedrockFormSender() {}

    /**
     * 初始化反射句柄。应在 GeyserAPI.initialize() 之后调用。
     */
    public static void initialize(Logger pluginLogger) {
        logger = pluginLogger;
        try {
            Class<?> geyserClass = Class.forName("org.geysermc.api.Geyser");
            Method apiMethod = geyserClass.getMethod("api");
            apiInstance = apiMethod.invoke(null);

            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.Form");
            sendFormMethod = apiInstance.getClass().getMethod("sendForm", UUID.class, formClass);

            ready = true;
            logger.info("[BedrockForm] Form sender initialized via Geyser API.");
        } catch (ClassNotFoundException e) {
            logger.info("[BedrockForm] Geyser not found, Bedrock form support disabled.");
        } catch (Exception e) {
            logger.info("[BedrockForm] Failed to initialize (" + e.getClass().getSimpleName()
                    + "), Bedrock form support disabled.");
        }
    }

    /**
     * 表单发送是否可用。
     */
    public static boolean isAvailable() {
        return ready;
    }

    /**
     * 向基岩玩家发送 Cumulus 表单。
     *
     * @param uuid 玩家 UUID
     * @param form Cumulus 表单实例（SimpleForm / CustomForm / ModalForm）
     * @return true 发送成功
     */
    public static boolean sendForm(UUID uuid, Form form) {
        if (!ready) return false;
        try {
            sendFormMethod.invoke(apiInstance, uuid, form);
            return true;
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("[BedrockForm] sendForm failed: " + e.getMessage());
            }
            return false;
        }
    }
}
