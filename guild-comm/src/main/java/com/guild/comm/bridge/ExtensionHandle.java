package com.guild.comm.bridge;

import java.util.Objects;

/**
 * Represents a connected external extension registered with the
 * {@link ExtensionBridge}.
 *
 * <p>Each extension is identified by a unique {@code extensionId} and
 * carries metadata about its capabilities.
 */
public class ExtensionHandle {

    /** Unique extension identifier (e.g. "imago-core", "custom-items"). */
    private final String extensionId;

    /** Human-readable display name. */
    private final String displayName;

    /** Extension version (SemVer). */
    private final String version;

    /** Supported message types (space-efficient set). */
    private final java.util.Set<String> supportedTypes;

    /** Connection state. */
    private volatile State state;

    /** Time this handle was created (epoch millis). */
    private final long connectedAt;

    public enum State {
        /** Registered but not yet ready. */
        PENDING,
        /** Fully connected and accepting messages. */
        ACTIVE,
        /** Gracefully disconnected. */
        DISCONNECTED,
        /** Encountered an error — needs reconnect. */
        ERROR
    }

    public ExtensionHandle(String extensionId, String displayName,
                           String version, String... supportedTypes) {
        this.extensionId = Objects.requireNonNull(extensionId, "extensionId");
        this.displayName = displayName != null ? displayName : extensionId;
        this.version = version != null ? version : "0.0.0";
        this.supportedTypes = java.util.Set.of(supportedTypes);
        this.state = State.PENDING;
        this.connectedAt = System.currentTimeMillis();
    }

    // ── Accessors ────────────────────────────────────────────────

    public String getExtensionId()  { return extensionId; }
    public String getDisplayName()  { return displayName; }
    public String getVersion()      { return version; }
    public State  getState()        { return state; }
    public long   getConnectedAt()  { return connectedAt; }

    /** Check if this extension supports a given message type. */
    public boolean supports(String messageType) {
        return supportedTypes.contains(messageType);
    }

    /** @return an immutable copy of supported types. */
    public java.util.Set<String> getSupportedTypes() {
        return java.util.Collections.unmodifiableSet(supportedTypes);
    }

    // ── State management (package-private) ───────────────────────

    void setState(State newState) { this.state = newState; }

    // ── Object ───────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtensionHandle that)) return false;
        return extensionId.equals(that.extensionId);
    }

    @Override
    public int hashCode() {
        return extensionId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("ExtensionHandle[%s v%s %s]", extensionId, version, state);
    }
}
