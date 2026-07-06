package com.guild.comm.bridge;

/**
 * SDK stub for {@link MessagePacket}.
 */
public class MessagePacket {

    public MessagePacket(String type, String source, String target,
                         String payload, long sequence) {}

    public String getType()      { return ""; }
    public String getSource()    { return ""; }
    public String getTarget()    { return ""; }
    public String getPayload()   { return "{}"; }
    public long   getSequence()  { return 0; }
    public long   getTimestamp() { return 0; }
    public boolean isBroadcast() { return false; }

    public static Builder create(String type, String source) {
        return new Builder(type, source);
    }

    public static class Builder {
        private Builder(String type, String source) {}
        public Builder target(String target) { return this; }
        public Builder payload(String payload) { return this; }
        public Builder sequence(long seq) { return this; }
        public MessagePacket build() { return null; }
    }
}
