package com.guild.core.language;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.logging.Logger;

/**
 * 模块语言加载、释放与异步重载编排。
 */
public final class ModuleLanguageRegistry {

    private final GuildPlugin plugin;
    private final Logger logger;
    private final LanguageCatalog catalog;
    private final LanguageResourceLoader loader;

    public ModuleLanguageRegistry(GuildPlugin plugin, LanguageCatalog catalog, LanguageResourceLoader loader) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.catalog = catalog;
        this.loader = loader;
    }

    public void loadAllModuleLanguages() {
        loadModuleLanguageConfig();
        loadExternalModuleLanguages();

        for (String moduleId : LanguageConstants.MODULE_DIRS) {
            if (!catalog.isModuleLoaded(moduleId)) {
                if (loader.loadBundledModuleLanguagesForModule(moduleId, catalog)) {
                    catalog.markModuleLoaded(moduleId);
                }
            }
        }
    }

    public void reloadModuleLanguages() {
        catalog.clearModuleData();
        loadAllModuleLanguages();
        logger.info("Reloaded module language files");
    }

    public void reloadModuleLanguagesAsync(Runnable callback) {
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            ModuleLanguageSnapshot snapshot = loader.readModuleLanguageResourcesSnapshot();

            CompatibleScheduler.runTask(plugin, () -> {
                catalog.applyModuleSnapshot(snapshot);

                logger.info("Reloaded module language files asynchronously"
                        + " (modules: " + snapshot.getModuleConfigs().size() + ")");

                if (callback != null) {
                    callback.run();
                }
            });
        });
    }

    public boolean loadModuleLanguageResourcesForModule(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return false;
        }
        String moduleDirName = LanguageConstants.resolveModuleLangDir(moduleId);
        if (catalog.isModuleLoaded(moduleDirName)) {
            return true;
        }

        File moduleDir = new File(plugin.getDataFolder(), LanguageConstants.MODULES_LANG_PATH + moduleDirName);

        boolean loaded = false;
        if (moduleDir.exists() && moduleDir.isDirectory()) {
            loaded = loader.loadExternalModuleLanguageDirectory(moduleDir, catalog);
        }

        if (!loaded) {
            loaded = loader.loadBundledModuleLanguagesForModule(moduleDirName, catalog);
        }

        if (loaded) {
            catalog.markModuleLoaded(moduleDirName);
        }
        return loaded;
    }

    public boolean loadModuleLanguageResourcesForModule(String moduleId, String lang) {
        if (moduleId == null || moduleId.trim().isEmpty() || lang == null || lang.trim().isEmpty()) {
            return false;
        }
        return loadModuleLanguageResourcesForModule(moduleId);
    }

    public boolean releaseModuleLanguageResourcesForModule(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return false;
        }
        return loader.releaseModuleLanguageResourcesForModule(
                LanguageConstants.resolveModuleLangDir(moduleId));
    }

    public boolean forceLoadBundledModule(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return false;
        }
        return loader.loadBundledModuleLanguagesForModule(moduleId.toLowerCase(), catalog);
    }

    public String dumpModuleConfig(String lang) {
        FileConfiguration config = catalog.getRawModuleConfig(lang != null ? lang : LanguageConstants.LANG_EN);
        if (config == null) {
            return "&cmoduleConfigs.get(\"" + lang + "\") == null";
        }
        java.util.Set<String> topKeys = config.getKeys(false);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("&7moduleConfigs[\"%s\"] 顶层键(&f%d&7): &f", lang, topKeys.size()));
        sb.append(topKeys.toString());

        sb.append("\n&7路径逐层检查:");
        String[] testPaths = {
                "module",
                "module.announcement",
                "module.announcement.button-name",
                "module.quest",
                "module.quest.button-name",
                "module.stats",
                "module.stats.button-name"
        };
        for (String path : testPaths) {
            boolean exists = config.contains(path);
            Object val = config.get(path);
            sb.append(String.format("\n  &7%s → %s → &f%s",
                    path,
                    exists ? "&a存在" : "&c不存在",
                    val != null ? val.toString() : "&cnull"));
        }
        return sb.toString();
    }

    private void loadModuleLanguageConfig() {
        String lang = loader.loadModuleDefaultLanguageFromConfig();
        catalog.setModuleDefaultLanguage(lang);
        logger.info("Module language default: " + catalog.getModuleDefaultLanguage());
    }

    private void loadExternalModuleLanguages() {
        File modulesRoot = new File(plugin.getDataFolder(), LanguageConstants.MODULES_LANG_PATH);
        if (!modulesRoot.exists() || !modulesRoot.isDirectory()) {
            return;
        }

        File[] moduleDirs = modulesRoot.listFiles(File::isDirectory);
        if (moduleDirs == null) {
            return;
        }
        java.util.Arrays.sort(moduleDirs, java.util.Comparator.comparing(File::getName));

        for (File moduleDir : moduleDirs) {
            loader.loadExternalModuleLanguageDirectory(moduleDir, catalog);
        }
    }
}
