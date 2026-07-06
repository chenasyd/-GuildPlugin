package com.guild.comm.event;

import com.guild.comm.bridge.ExtensionHandle;

/**
 * Fired when an extension deregisters from the bridge.
 */
public class ExtensionDisconnectedEvent {

    private final String extensionId;
    private final String reason;

    public ExtensionDisconnectedEvent(String extensionId, String reason) {
        this.extensionId = extensionId;
        this.reason = reason;
    }

    public String getExtensionId() { return extensionId; }
    public String getReason()      { return reason; }

    @Override
    public String toString() {
        return "ExtensionDisconnectedEvent[" + extensionId + " reason=" + reason + "]";
    }
}
