package com.guild.sdk;

import com.guild.GuildPlugin;

import java.io.File;

/**
 * 模块语言资源加载与释放。
 */
public final class ModuleLanguageSupport {

    private final GuildPlugin plugin;

    public ModuleLanguageSupport(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean loadModuleLanguageResource(String moduleId, String lang) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return false;
        }
        if (lang == null || lang.trim().isEmpty()) {
            return plugin.getLanguageManager().loadModuleLanguageResourcesForModule(moduleId);
        }
        return plugin.getLanguageManager().loadModuleLanguageResourcesForModule(moduleId, lang.toLowerCase());
    }

    public boolean releaseModuleLanguageResource(String moduleId, String lang) {
        if (moduleId == null || moduleId.trim().isEmpty() || lang == null || lang.trim().isEmpty()) {
            return false;
        }
        String moduleDirName = moduleId.toLowerCase();
        String language = lang.toLowerCase();
        String resourcePath = "lang/modules/" + moduleDirName + "/" + language + ".yml";
        if (plugin.getResource(resourcePath) == null) {
            return false;
        }
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (file.exists()) {
            return false;
        }
        try {
            plugin.saveResource(resourcePath, false);
            plugin.getLogger().info("Extracted bundled module language file: " + resourcePath);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to extract bundled module language file "
                    + resourcePath + ": " + e.getMessage());
            return false;
        }
    }

    public File getModuleLanguageFile(String moduleId, String lang) {
        if (moduleId == null || moduleId.trim().isEmpty() || lang == null || lang.trim().isEmpty()) {
            return null;
        }
        String moduleDirName = moduleId.toLowerCase();
        String language = lang.toLowerCase();
        return new File(plugin.getDataFolder(), "lang/modules/" + moduleDirName + "/" + language + ".yml");
    }
}
