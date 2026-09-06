package com.guild.core.language;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 语言系统门面：对外 API 不变，加载/索引/玩家偏好委托给 PR4 拆分子组件。
 */
public class LanguageManager {

    public static final String LANG_EN = LanguageConstants.LANG_EN;
    public static final String LANG_ZH = LanguageConstants.LANG_ZH;
    public static final String LANG_PL = LanguageConstants.LANG_PL;
    public static final String LANG_BR = LanguageConstants.LANG_BR;

    private final GuildPlugin plugin;
    private final Logger logger;
    private final LanguageCatalog catalog;
    private final LanguageResourceLoader loader;
    private final ModuleLanguageRegistry moduleRegistry;
    private final PlayerLanguageStore playerLanguages;

    public LanguageManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.catalog = new LanguageCatalog();
        this.loader = new LanguageResourceLoader(plugin);
        this.moduleRegistry = new ModuleLanguageRegistry(plugin, catalog, loader);
        this.playerLanguages = new PlayerLanguageStore(catalog::getDefaultLanguage, catalog::isLanguageSupported);

        loader.releaseBundledLanguageFiles();
        catalog.applyPluginSnapshot(loader.readPluginLanguageResourcesSnapshot());
        moduleRegistry.loadAllModuleLanguages();

        logger.info("Language system loaded, default language: " + catalog.getDefaultLanguage()
                + ", loaded languages: " + String.join(", ", catalog.getLoadedLanguages()));
    }

    public String[] getKnownModuleLangDirs() {
        return LanguageConstants.MODULE_DIRS.clone();
    }

    public boolean isLanguageSupported(String lang) {
        return catalog.isLanguageSupported(lang);
    }

    public List<String> getLoadedLanguages() {
        return catalog.getLoadedLanguages();
    }

    public List<String> getAvailableLanguageNames() {
        return LanguageDisplayNames.formatSupportedLanguages(
                new java.util.LinkedHashSet<>(catalog.getLoadedLanguages()));
    }

    public String getPlayerLanguage(Player player) {
        return playerLanguages.get(player);
    }

    public void setPlayerLanguage(Player player, String lang) {
        playerLanguages.set(player, lang);
    }

    public void setPlayerLanguage(java.util.UUID uuid, String lang) {
        playerLanguages.set(uuid, lang);
    }

    public String getMessage(String lang, String path, String defaultValue) {
        return MessageResolver.resolve(catalog.getLegacyConfig(lang), path, defaultValue);
    }

    public String getMessage(String path, String defaultValue) {
        return getMessage(catalog.getDefaultLanguage(), path, defaultValue);
    }

    public String getMessage(Player player, String path, String defaultValue) {
        return getMessage(getPlayerLanguage(player), path, defaultValue);
    }

    public String getMessage(String lang, String path, String defaultValue, String... placeholders) {
        return MessageResolver.resolveWithPlaceholders(catalog.getLegacyConfig(lang), path, defaultValue, placeholders);
    }

    public String getMessage(Player player, String path, String defaultValue, String... placeholders) {
        return getMessage(getPlayerLanguage(player), path, defaultValue, placeholders);
    }

    public String getIndexedMessage(String lang, String path, String defaultValue, String[] args) {
        return MessageResolver.resolveWithIndexedArgs(catalog.getLegacyConfig(lang), path, defaultValue, args);
    }

    public String getIndexedMessage(String path, String defaultValue, String... args) {
        return MessageResolver.resolveWithIndexedArgs(
                catalog.getLegacyConfig(catalog.getDefaultLanguage()), path, defaultValue, args);
    }

    public String getIndexedMessage(Player player, String path, String defaultValue, String... args) {
        return MessageResolver.resolveWithIndexedArgs(
                catalog.getLegacyConfig(getPlayerLanguage(player)), path, defaultValue, args);
    }

    public String getDefaultLanguage() {
        return catalog.getDefaultLanguage();
    }

    public void setDefaultLanguage(String lang) {
        catalog.setDefaultLanguage(lang);
    }

    public void reloadLanguages() {
        catalog.applyPluginSnapshot(loader.readPluginLanguageResourcesSnapshot());
        logger.info("Reloaded plugin language files (main + core + gui), default language: "
                + catalog.getDefaultLanguage() + ", loaded languages: "
                + String.join(", ", catalog.getLoadedLanguages()));
    }

    public void reloadModuleLanguages() {
        moduleRegistry.reloadModuleLanguages();
    }

    public void reloadLanguagesAsync(Runnable callback) {
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            LanguageResourceSnapshot snapshot = loader.readPluginLanguageResourcesSnapshot();

            CompatibleScheduler.runTask(plugin, () -> {
                catalog.applyPluginSnapshot(snapshot);

                logger.info("Reloaded plugin language files asynchronously"
                        + " (core: " + snapshot.getCore().size()
                        + ", gui: " + snapshot.getGui().size()
                        + ", default: " + catalog.getDefaultLanguage() + ")");

                if (callback != null) {
                    callback.run();
                }
            });
        });
    }

    public void reloadModuleLanguagesAsync(Runnable callback) {
        moduleRegistry.reloadModuleLanguagesAsync(callback);
    }

    public FileConfiguration getLanguageConfig(String lang) {
        return catalog.getRawLanguageConfig(lang);
    }

    public FileConfiguration getGuiConfig(String lang) {
        return catalog.getGuiConfig(lang);
    }

    public String getGuiMessage(String lang, String path, String defaultValue) {
        return MessageResolver.resolve(catalog.getGuiConfig(lang), path, defaultValue);
    }

    public String getGuiMessage(String path, String defaultValue) {
        return getGuiMessage(catalog.getDefaultLanguage(), path, defaultValue);
    }

    public String getGuiMessage(Player player, String path, String defaultValue) {
        return getGuiMessage(getPlayerLanguage(player), path, defaultValue);
    }

    public String getGuiMessage(String lang, String path, String defaultValue, String... placeholders) {
        return MessageResolver.resolveWithPlaceholders(catalog.getGuiConfig(lang), path, defaultValue, placeholders);
    }

    public String getGuiMessage(Player player, String path, String defaultValue, String... placeholders) {
        return getGuiMessage(getPlayerLanguage(player), path, defaultValue, placeholders);
    }

    public String getGuiColoredMessage(String lang, String path, String defaultValue) {
        return MessageResolver.colorize(getGuiMessage(lang, path, defaultValue));
    }

    public String getGuiColoredMessage(Player player, String path, String defaultValue) {
        return MessageResolver.colorize(getGuiMessage(player, path, defaultValue));
    }

    public String getGuiColoredMessage(String lang, String path, String defaultValue, String... placeholders) {
        return MessageResolver.colorize(getGuiMessage(lang, path, defaultValue, placeholders));
    }

    public String getGuiColoredMessage(Player player, String path, String defaultValue, String... placeholders) {
        return MessageResolver.colorize(getGuiMessage(player, path, defaultValue, placeholders));
    }

    public String getModuleDefaultLanguage() {
        return catalog.getModuleDefaultLanguage();
    }

    public int getModuleConfigCount() {
        return catalog.getModuleConfigCount();
    }

    public Set<String> getModuleSupportedLanguages() {
        return catalog.getModuleSupportedLanguages();
    }

    public Set<String> getLoadedModuleIds() {
        return catalog.getLoadedModuleIds();
    }

    public boolean forceLoadBundledModule(String moduleId) {
        return moduleRegistry.forceLoadBundledModule(moduleId);
    }

    public String dumpModuleConfig(String lang) {
        return moduleRegistry.dumpModuleConfig(lang);
    }

    public String getModuleMessage(String lang, String path, String defaultValue) {
        return MessageResolver.resolve(catalog.getModuleConfig(lang), path, defaultValue);
    }

    public String getModuleMessage(String path, String defaultValue) {
        return getModuleMessage(catalog.getModuleDefaultLanguage(), path, defaultValue);
    }

    public String getModuleMessage(Player player, String path, String defaultValue) {
        return getModuleMessage(getPlayerLanguage(player), path, defaultValue);
    }

    public String getModuleMessage(String lang, String path, String defaultValue, String... placeholders) {
        return MessageResolver.resolveWithPlaceholders(catalog.getModuleConfig(lang), path, defaultValue, placeholders);
    }

    public String getModuleMessage(Player player, String path, String defaultValue, String... placeholders) {
        return getModuleMessage(getPlayerLanguage(player), path, defaultValue, placeholders);
    }

    public String getModuleIndexedMessage(String lang, String path, String defaultValue, String[] args) {
        return MessageResolver.resolveWithIndexedArgs(catalog.getModuleConfig(lang), path, defaultValue, args);
    }

    public String getModuleIndexedMessage(String path, String defaultValue, String... args) {
        return getModuleIndexedMessage(catalog.getModuleDefaultLanguage(), path, defaultValue, args);
    }

    public String getModuleIndexedMessage(Player player, String path, String defaultValue, String... args) {
        return getModuleIndexedMessage(getPlayerLanguage(player), path, defaultValue, args);
    }

    public FileConfiguration getCoreConfig(String lang) {
        return catalog.getCoreConfig(lang);
    }

    public String getCoreMessage(String lang, String path, String defaultValue) {
        return MessageResolver.resolve(catalog.getCoreConfig(lang), path, defaultValue);
    }

    public String getCoreMessage(String path, String defaultValue) {
        return getCoreMessage(catalog.getDefaultLanguage(), path, defaultValue);
    }

    public String getCoreMessage(Player player, String path, String defaultValue) {
        return getCoreMessage(getPlayerLanguage(player), path, defaultValue);
    }

    public String getCoreMessage(String lang, String path, String defaultValue, String... placeholders) {
        return MessageResolver.resolveWithPlaceholders(catalog.getCoreConfig(lang), path, defaultValue, placeholders);
    }

    public String getCoreMessage(Player player, String path, String defaultValue, String... placeholders) {
        return getCoreMessage(getPlayerLanguage(player), path, defaultValue, placeholders);
    }

    public String getCoreIndexedMessage(String lang, String path, String defaultValue, String[] args) {
        return MessageResolver.resolveWithIndexedArgs(catalog.getCoreConfig(lang), path, defaultValue, args);
    }

    public String getCoreIndexedMessage(String path, String defaultValue, String... args) {
        return getCoreIndexedMessage(catalog.getDefaultLanguage(), path, defaultValue, args);
    }

    public String getCoreIndexedMessage(Player player, String path, String defaultValue, String... args) {
        return getCoreIndexedMessage(getPlayerLanguage(player), path, defaultValue, args);
    }

    public String getGuiIndexedMessage(String lang, String path, String defaultValue, String[] args) {
        return MessageResolver.resolveWithIndexedArgs(catalog.getGuiConfig(lang), path, defaultValue, args);
    }

    public String getGuiIndexedMessage(String path, String defaultValue, String... args) {
        return getGuiIndexedMessage(catalog.getDefaultLanguage(), path, defaultValue, args);
    }

    public String getGuiIndexedMessage(Player player, String path, String defaultValue, String... args) {
        return getGuiIndexedMessage(getPlayerLanguage(player), path, defaultValue, args);
    }

    public boolean loadModuleLanguageResourcesForModule(String moduleId) {
        return moduleRegistry.loadModuleLanguageResourcesForModule(moduleId);
    }

    public boolean loadModuleLanguageResourcesForModule(String moduleId, String lang) {
        return moduleRegistry.loadModuleLanguageResourcesForModule(moduleId, lang);
    }

    public boolean releaseModuleLanguageResourcesForModule(String moduleId) {
        return moduleRegistry.releaseModuleLanguageResourcesForModule(moduleId);
    }
}
