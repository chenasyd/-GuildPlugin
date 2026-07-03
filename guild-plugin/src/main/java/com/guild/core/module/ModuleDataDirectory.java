package com.guild.core.module;

import com.guild.GuildPlugin;

import java.io.File;

/**
 * 模块数据目录工具类，统一模块持久化数据的保存根路径。
 */
public final class ModuleDataDirectory {

    private static final String MODULES_ROOT = "modules";
    private static final String DATA_FOLDER = "data";

    private ModuleDataDirectory() {
        // utils only
    }

    public static File getModuleDataRoot(GuildPlugin plugin, String moduleId) {
        if (plugin == null || moduleId == null || moduleId.isBlank()) {
            throw new IllegalArgumentException("plugin and moduleId must not be null or empty");
        }
        return new File(new File(plugin.getDataFolder(), MODULES_ROOT), moduleId.toLowerCase() + File.separator + DATA_FOLDER);
    }

    public static File getModuleDataRoot(ModuleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        return getModuleDataRoot(context.getPlugin(), context.getDescriptor().getId());
    }
}
