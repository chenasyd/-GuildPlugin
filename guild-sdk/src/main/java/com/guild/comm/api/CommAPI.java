package com.guild.comm.api;

import com.guild.comm.bridge.ChannelRouter;
import com.guild.comm.bridge.ExtensionBridge;
import com.guild.comm.bridge.ExtensionHandle;
import com.guild.comm.bridge.MessagePacket;
import com.guild.comm.debug.TraceContext;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

/**
 * SDK stub for the Guild Communication Bridge API.
 * <p>
 * External plugins compile against this stub; at runtime the real
 * implementation is provided by the {@code guild-comm} library JAR.
 */
public class CommAPI {

    private CommAPI() {}

    public static ExtensionBridge getBridge() { return ExtensionBridge.getInstance(); }

    public static void initialize(Logger logger) {}

    public static void shutdown() {}

    public static ExtensionHandle connect(String extensionId, String displayName,
                                          String version, String... supportedTypes) {
        return null;
    }

    public static boolean disconnect(String extensionId) { return false; }

    public static boolean isConnected(String extensionId) { return false; }

    public static void send(MessagePacket packet) {}

    public static void send(String type, String source, String target, String payload) {}

    public static void addListener(ExtensionBridge.MessageListener listener) {}

    public static void on(String topic, ChannelRouter.TopicHandler handler) {}

    public static void off(String topic, ChannelRouter.TopicHandler handler) {}

    public static Set<String> getExtensions() { return Collections.emptySet(); }

    public static ExtensionHandle getHandle(String id) { return null; }

    public static int getCount() { return 0; }

    public static TraceContext trace(String operation) { return new TraceContext(operation); }
}
