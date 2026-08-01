package com.guild.sdk.gui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Defines the slot layout for a module GUI in image mode.
 * <p>
 * Function names (e.g. "HEADER", "CONTENT", "BACK") map to inventory slot arrays.
 * When ImagoCore image mode is active, {@code GUIManager.applyImageModeIfNeeded}
 * replaces items in configured slots with transparent carriers (preserving name/lore).
 *
 * <pre>{@code
 * GUILayoutDefinition layout = GUILayoutDefinition.builder()
 *     .function("HEADER", 0, 1, 2, 3, 4, 5, 6, 7, 8)
 *     .function("CONTENT", 10, 11, 12, 13, 14, 15, 16)
 *     .function("FOOTER", 45, 46, 47, 48, 49, 50, 51, 52, 53)
 *     .build();
 * }</pre>
 */
public final class GUILayoutDefinition {

    private final Map<String, int[]> functions;

    private GUILayoutDefinition(Map<String, int[]> functions) {
        this.functions = Collections.unmodifiableMap(functions);
    }

    /**
     * @return unmodifiable map of function name → slot array
     */
    public Map<String, int[]> getFunctions() {
        return functions;
    }

    /**
     * @param function function name (e.g. "HEADER")
     * @return slot array, or empty array if not defined
     */
    public int[] getSlots(String function) {
        return functions.getOrDefault(function, new int[0]);
    }

    /**
     * @param function function name
     * @return true if this layout defines the given function
     */
    public boolean hasFunction(String function) {
        return functions.containsKey(function);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, int[]> functions = new LinkedHashMap<>();

        /**
         * Add a function-to-slots mapping.
         *
         * @param name  function name (e.g. "HEADER", "CONTENT", "BACK")
         * @param slots inventory slot indices
         * @return this builder
         */
        public Builder function(String name, int... slots) {
            functions.put(name, slots);
            return this;
        }

        public GUILayoutDefinition build() {
            return new GUILayoutDefinition(new LinkedHashMap<>(functions));
        }
    }
}
