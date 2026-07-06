package com.guild.comm.bridge;

/**
 * SDK stub for {@link ChannelRouter}.
 */
public class ChannelRouter {

    public ChannelRouter() {}

    public void subscribe(String topic, TopicHandler handler) {}
    public void unsubscribe(String topic, TopicHandler handler) {}
    public void route(MessagePacket packet) {}
    public int getTopicCount() { return 0; }
    public void clear() {}

    @FunctionalInterface
    public interface TopicHandler {
        void onMessage(MessagePacket packet);
    }
}
