package com.guild.comm.bridge;

/**
 * SDK stub for {@link ExtensionHandle}.
 */
public class ExtensionHandle {

    public enum State { PENDING, ACTIVE, DISCONNECTED, ERROR }

    public ExtensionHandle(String extensionId, String displayName,
                           String version, String... supportedTypes) {}

    public String getExtensionId() { return ""; }
    public String getDisplayName() { return ""; }
    public String getVersion()     { return ""; }
    public State  getState()       { return State.PENDING; }
    public long   getConnectedAt() { return 0; }

    public boolean supports(String messageType) { return false; }
    public java.util.Set<String> getSupportedTypes() { return java.util.Collections.emptySet(); }
}
