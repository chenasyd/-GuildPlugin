package com.guild.sdk.gui;

/**
 * Registration descriptor for module custom GUIs (enhanced version).
 * <p>
 * Encapsulates all GUI metadata in a single builder: factory, image binding,
 * layout definition, Bedrock form provider, and config override.
 * <p>
 * Usage:
 * <pre>{@code
 * api.registerCustomGUI(ModuleGUIRegistration.builder("stats-overview", factory)
 *     .moduleId("guild-stats")
 *     .imageBinding("stats-overview")
 *     .layout(GUILayoutDefinition.builder()
 *         .function("HEADER", 0,1,2,3,4,5,6,7,8)
 *         .function("CONTENT", 10,11,12,13,14,15,16)
 *         .build())
 *     .bedrockForm((player, data) -> sendMyForm(player))
 *     .config(myConfig)
 *     .build());
 * }</pre>
 * <p>
 * {@code moduleId} is <b>required</b> for hot-unload cleanup. Prefer
 * {@code registerCustomGUI(moduleId, guiId, factory)} or always call {@link Builder#moduleId(String)}.
 */
public final class ModuleGUIRegistration {

    private final String guiId;
    private final String moduleId;
    private final ModuleGUIFactory factory;
    private final String imageEntryId;
    private final GUILayoutDefinition layout;
    private final BedrockFormProvider bedrock;
    private final ModuleGUIConfig config;

    private ModuleGUIRegistration(Builder builder) {
        this.guiId = builder.guiId;
        this.moduleId = builder.moduleId;
        this.factory = builder.factory;
        this.imageEntryId = builder.imageEntryId;
        this.layout = builder.layout;
        this.bedrock = builder.bedrock;
        this.config = builder.config;
    }

    public String getGuiId() { return guiId; }
    public String getModuleId() { return moduleId; }
    public ModuleGUIFactory getFactory() { return factory; }

    /** @return ImagoCore GuiEntry ID, or null if image title not enabled */
    public String getImageEntryId() { return imageEntryId; }

    /** @return slot layout for image mode, or null if not defined */
    public GUILayoutDefinition getLayout() { return layout; }

    /** @return Bedrock form provider, or null if not registered */
    public BedrockFormProvider getBedrockFormProvider() { return bedrock; }

    /** @return runtime config override, or null if not registered */
    public ModuleGUIConfig getConfig() { return config; }

    /**
     * Create a builder for the enhanced GUI registration.
     *
     * @param guiId   unique GUI identifier
     * @param factory factory that creates the GUI instance
     * @return new builder
     */
    public static Builder builder(String guiId, ModuleGUIFactory factory) {
        return new Builder(guiId, factory);
    }

    public static final class Builder {
        private final String guiId;
        private final ModuleGUIFactory factory;
        private String moduleId;
        private String imageEntryId;
        private GUILayoutDefinition layout;
        private BedrockFormProvider bedrock;
        private ModuleGUIConfig config;

        private Builder(String guiId, ModuleGUIFactory factory) {
            if (guiId == null || guiId.isEmpty()) {
                throw new IllegalArgumentException("guiId must not be null or empty");
            }
            if (factory == null) {
                throw new IllegalArgumentException("factory must not be null");
            }
            this.guiId = guiId;
            this.factory = factory;
        }

        /**
         * Bind an ImagoCore GuiEntry ID to enable image titles for this GUI.
         *
         * @param entryId ImagoCore GuiEntry identifier (e.g. "stats-overview")
         * @return this builder
         */
        public Builder imageBinding(String entryId) {
            this.imageEntryId = entryId;
            return this;
        }

        /**
         * Define the slot layout for image mode (transparent carrier conversion).
         *
         * @param layout layout definition mapping function names to slot arrays
         * @return this builder
         */
        public Builder layout(GUILayoutDefinition layout) {
            this.layout = layout;
            return this;
        }

        /**
         * Register a Bedrock Edition Cumulus form provider.
         * When a Bedrock player opens this GUI, the provider is invoked
         * instead of the Java Inventory path.
         *
         * @param provider form provider implementation
         * @return this builder
         */
        public Builder bedrockForm(BedrockFormProvider provider) {
            this.bedrock = provider;
            return this;
        }

        /**
         * Register a runtime configuration override instance.
         * Allows server admins to customize items/text via gui-config.yml.
         *
         * @param config config override implementation
         * @return this builder
         */
        public Builder config(ModuleGUIConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Set the owning module ID (required for cleanup on module unload).
         *
         * @param moduleId module identifier
         * @return this builder
         */
        public Builder moduleId(String moduleId) {
            this.moduleId = moduleId;
            return this;
        }

        public ModuleGUIRegistration build() {
            if (moduleId == null || moduleId.isEmpty()) {
                throw new IllegalArgumentException("moduleId is required (call .moduleId(...))");
            }
            return new ModuleGUIRegistration(this);
        }
    }
}
