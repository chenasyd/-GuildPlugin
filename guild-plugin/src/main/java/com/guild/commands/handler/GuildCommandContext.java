package com.guild.commands.handler;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import com.guild.core.module.ModuleManager;
import com.guild.sdk.GuildPluginAPI;
import com.guild.services.GuildService;

/**
 * 子命令 Handler 共享依赖。
 */
public final class GuildCommandContext {

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final GuildService guildService;
    private final GuildPluginAPI api;

    public GuildCommandContext(GuildPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guildService = plugin.getGuildService();
        this.api = plugin.getServiceContainer().get(ModuleManager.class).getSharedApi();
    }

    public GuildPlugin plugin() {
        return plugin;
    }

    public LanguageManager languageManager() {
        return languageManager;
    }

    public GuildService guildService() {
        return guildService;
    }

    public GuildPluginAPI api() {
        return api;
    }
}
