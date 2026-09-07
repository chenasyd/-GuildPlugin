package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ModuleLanguageSupportTest {

    private GuildPlugin plugin;
    private LanguageManager languageManager;
    private ModuleLanguageSupport support;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        plugin = mock(GuildPlugin.class);
        languageManager = mock(LanguageManager.class);
        when(plugin.getLanguageManager()).thenReturn(languageManager);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        support = new ModuleLanguageSupport(plugin);
    }

    @Test
    void loadModuleLanguageResource_blankModuleId_returnsFalse() {
        assertFalse(support.loadModuleLanguageResource("", "zh_cn"));

        verifyNoInteractions(languageManager);
    }

    @Test
    void loadModuleLanguageResource_withoutLang_delegatesToLanguageManager() {
        when(languageManager.loadModuleLanguageResourcesForModule("quest")).thenReturn(true);

        assertTrue(support.loadModuleLanguageResource("quest", null));

        verify(languageManager).loadModuleLanguageResourcesForModule("quest");
    }

    @Test
    void loadModuleLanguageResource_withLang_normalizesCase() {
        when(languageManager.loadModuleLanguageResourcesForModule("quest", "zh_cn")).thenReturn(true);

        assertTrue(support.loadModuleLanguageResource("quest", "ZH_CN"));

        verify(languageManager).loadModuleLanguageResourcesForModule("quest", "zh_cn");
    }

    @Test
    void releaseModuleLanguageResource_noBundledResource_returnsFalse() {
        when(plugin.getResource("lang/modules/quest/zh_cn.yml")).thenReturn(null);

        assertFalse(support.releaseModuleLanguageResource("quest", "zh_cn"));
    }

    @Test
    void getModuleLanguageFile_invalidInput_returnsNull() {
        assertNull(support.getModuleLanguageFile(null, "zh_cn"));
        assertNull(support.getModuleLanguageFile("quest", ""));
    }

    @Test
    void getModuleLanguageFile_returnsExpectedPath(@TempDir Path tempDir) {
        File file = support.getModuleLanguageFile("Quest", "ZH_CN");

        assertTrue(file.getPath().replace('\\', '/').endsWith("lang/modules/quest/zh_cn.yml"));
    }
}
