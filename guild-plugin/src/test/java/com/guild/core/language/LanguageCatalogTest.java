package com.guild.core.language;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageCatalogTest {

    private LanguageCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new LanguageCatalog();
    }

    @Test
    void applyPluginSnapshot_registersSupportedLanguages() {
        LanguageResourceSnapshot snapshot = new LanguageResourceSnapshot();
        snapshot.getCore().put("en", YamlConfiguration.loadConfiguration(new java.io.StringReader("hello: world")));
        snapshot.getGui().put("en", YamlConfiguration.loadConfiguration(new java.io.StringReader("title: GUI")));
        snapshot.getLanguages().add("en");
        snapshot.setDefaultLanguage("en");

        catalog.applyPluginSnapshot(snapshot);

        assertTrue(catalog.isLanguageSupported("en"));
        assertEquals("en", catalog.getDefaultLanguage());
        assertNotNull(catalog.getCoreConfig("en"));
        assertNotNull(catalog.getGuiConfig("en"));
        assertNotNull(catalog.getLegacyConfig("en"));
    }

    @Test
    void mergeModuleConfig_accumulatesLeafKeys() {
        var first = YamlConfiguration.loadConfiguration(new java.io.StringReader(
                "module:\n  quest:\n    title: Quest"));
        var second = YamlConfiguration.loadConfiguration(new java.io.StringReader(
                "module:\n  stats:\n    title: Stats"));

        catalog.mergeModuleConfig("en", first);
        catalog.mergeModuleConfig("en", second);
        catalog.registerModuleLanguage("en");

        assertEquals("Quest", catalog.getModuleConfig("en").getString("module.quest.title"));
        assertEquals("Stats", catalog.getModuleConfig("en").getString("module.stats.title"));
    }

    @Test
    void moduleLanguageFallback_usesModuleDefaultThenEnglish() {
        catalog.registerModuleLanguage("zh");
        catalog.setModuleDefaultLanguage("zh");
        catalog.mergeModuleConfig("zh", YamlConfiguration.loadConfiguration(
                new java.io.StringReader("key: 中文")));

        assertEquals("中文", catalog.getModuleConfig("zh").getString("key"));
        assertEquals("中文", catalog.getModuleConfig(null).getString("key"));
    }

    @Test
    void applyModuleSnapshot_replacesPreviousModuleData() {
        catalog.mergeModuleConfig("en", YamlConfiguration.loadConfiguration(
                new java.io.StringReader("old: \"yes\"")));
        catalog.markModuleLoaded("territory");

        ModuleLanguageSnapshot snapshot = new ModuleLanguageSnapshot();
        snapshot.getModuleConfigs().put("en", YamlConfiguration.loadConfiguration(
                new java.io.StringReader("new: \"yes\"")));
        snapshot.getModuleSupportedLanguages().add("en");
        snapshot.setModuleDefaultLanguage("en");
        snapshot.getLoadedModuleIds().add("quest");

        catalog.applyModuleSnapshot(snapshot);

        assertEquals("yes", catalog.getModuleConfig("en").getString("new"));
        assertFalse(catalog.getLoadedModuleIds().contains("territory"));
        assertTrue(catalog.getLoadedModuleIds().contains("quest"));
    }

    @Test
    void setDefaultLanguage_onlyWhenSupported() {
        LanguageResourceSnapshot snapshot = new LanguageResourceSnapshot();
        snapshot.getLanguages().add("en");
        snapshot.setDefaultLanguage("en");
        catalog.applyPluginSnapshot(snapshot);

        catalog.setDefaultLanguage("fr");
        assertEquals("en", catalog.getDefaultLanguage());

        snapshot.getLanguages().add("fr");
        catalog.applyPluginSnapshot(snapshot);
        catalog.setDefaultLanguage("fr");
        assertEquals("fr", catalog.getDefaultLanguage());
    }
}
