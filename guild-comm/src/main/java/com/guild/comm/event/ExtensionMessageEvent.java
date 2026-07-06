package com.guild.comm.event;

import com.guild.comm.bridge.MessagePacket;

/**
 * Wraps a {@link MessagePacket} as a bridge event for use with
 * Bukkit's event system or internal {@link com.guild.comm.bridge.ChannelRouter}.
 */
public class ExtensionMessageEvent {

    private final MessagePacket packet;

    public ExtensionMessageEvent(MessagePacket packet) {
        this.packet = packet;
    }

    public MessagePacket getPacket() { return packet; }

    public String getType()    { return packet.getType(); }
    public String getSource()  { return packet.getSource(); }
    public String getTarget()  { return packet.getTarget(); }
    public String getPayload() { return packet.getPayload(); }

    @Override
    public String toString() {
        return "ExtensionMessageEvent[" + packet + "]";
    }
}
