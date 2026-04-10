package com.guild.core.module;

import com.guild.GuildPlugin;
import com.guild.core.ServiceContainer;
import com.guild.core.events.EventBus;
import com.guild.core.gui.GUI;
import com.guild.core.gui.GUIManager;
import com.guild.core.language.LanguageManager;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.config.ModuleConfigSection;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class ModuleContext {
    private final GuildPlugin plugin;
    private final ModuleDescriptor descriptor;
    private final GuildPluginAPI api;
    private final ModuleConfigSection config;
    private final Logger logger;

    public ModuleContext(GuildPlugin plugin, ModuleDescriptor descriptor, GuildPluginAPI sharedApi) {
        this.plugin = plugin;
        this.descriptor = descriptor;
        this.api = sharedApi;
        this.config = new ModuleConfigSection(plugin, descriptor != null ? descriptor.getId() : "module");
        this.logger = Logger.getLogger("GuildModule");
    }

    public GuildPlugin getPlugin() { return plugin; }

    public GuildPluginAPI getApi() { return api; }

    public ServiceContainer getServiceContainer() { return null; }

    public EventBus getEventBus() { return null; }

    public GUIManager getGuiManager() { return null; }

    public LanguageManager getLanguageManager() { return null; }

    public ModuleDescriptor getDescriptor() { return descriptor; }

    public ModuleConfigSection getConfig() { return config; }

    public Logger getLogger() { return logger; }

    public void sendMessage(Player player, String key, Object... args) {
    }

    public String getMessage(String key, Object... args) {
        if (args != null && args.length > 0 && args[0] != null) {
            return args[0].toString();
        }
        return "";
    }

    public void runSync(Runnable task) {
    }

    public void runAsync(Runnable task) {
    }

    public void runLater(long delayTicks, Runnable task) {
    }

    public void runTimer(long delayTicks, long periodTicks, Runnable task) {
    }

    public void openGUI(Player player, GUI gui) {
    }

    public boolean navigateBack(Player player) {
        return false;
    }
}
