package com.guild.comm.bridge;

/**
 * SDK stub for {@link ExtensionBridge}.
 */
public class ExtensionBridge {

    private static final ExtensionBridge INSTANCE = new ExtensionBridge();

    private ExtensionBridge() {}

    public static ExtensionBridge getInstance() { return INSTANCE; }

    public void initialize(java.util.logging.Logger logger) {}

    public void shutdown() {}

    public ExtensionBridge register(ExtensionHandle handle) { return this; }

    public boolean deregister(String extensionId) { return false; }

    public ExtensionHandle getExtension(String extensionId) { return null; }

    public boolean isConnected(String extensionId) { return false; }

    public java.util.Set<String> getExtensionIds() { return java.util.Collections.emptySet(); }

    public int getExtensionCount() { return 0; }

    public void send(MessagePacket packet) {}

    public MessagePacket sendAndAwait(MessagePacket packet, long timeoutMs) { return null; }

    public long nextSequence() { return 0; }

    public void addMessageListener(MessageListener listener) {}

    public void removeMessageListener(MessageListener listener) {}

    public int getListenerCount() { return 0; }

    public ChannelRouter getRouter() { return new ChannelRouter(); }

    @FunctionalInterface
    public interface MessageListener {
        void onMessage(MessagePacket packet);
    }
}
