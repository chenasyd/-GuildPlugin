package com.guild.comm.event;

import com.guild.comm.bridge.MessagePacket;

/** SDK stub for {@link ExtensionMessageEvent}. */
public class ExtensionMessageEvent {
    public ExtensionMessageEvent(MessagePacket packet) {}
    public MessagePacket getPacket() { return null; }
    public String getType()    { return ""; }
    public String getSource()  { return ""; }
    public String getTarget()  { return ""; }
    public String getPayload() { return "{}"; }
}
