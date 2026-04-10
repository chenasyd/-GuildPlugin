package com.guild;

import com.guild.core.ServiceContainer;
import com.guild.core.events.EventBus;
import com.guild.core.gui.GUIManager;
import com.guild.core.language.LanguageManager;
import com.guild.services.GuildService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 编译期占位类型。
 * 运行时由主插件中的同名类提供真实实现。
 */
public class GuildPlugin extends JavaPlugin {

    public ServiceContainer getServiceContainer() {
        return null;
    }

    public EventBus getEventBus() {
        return null;
    }

    public GUIManager getGuiManager() {
        return null;
    }

    public LanguageManager getLanguageManager() {
        return null;
    }

    public GuildService getGuildService() {
        return null;
    }
}
