package com.guild.comm.event;

import com.guild.comm.bridge.ExtensionHandle;

/**
 * Fired when an external extension registers with the bridge.
 */
public class ExtensionConnectedEvent {

    private final ExtensionHandle handle;

    public ExtensionConnectedEvent(ExtensionHandle handle) {
        this.handle = handle;
    }

    public ExtensionHandle getHandle() {
        return handle;
    }

    public String getExtensionId() {
        return handle.getExtensionId();
    }

    @Override
    public String toString() {
        return "ExtensionConnectedEvent[" + handle.getExtensionId() + "]";
    }
}
