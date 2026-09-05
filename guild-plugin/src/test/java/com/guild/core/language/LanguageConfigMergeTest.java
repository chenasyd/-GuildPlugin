package com.guild.core.language;

import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LanguageConfigMergeTest {

    @Test
    void mergeLeafKeys_preservesExistingModulesWhenAddingNewModule() {
        FileConfiguration target = LanguageConfigMerge.loadYaml("""
                module-a:
                  title: "A"
                """);
        FileConfiguration source = LanguageConfigMerge.loadYaml("""
                module-b:
                  title: "B"
                """);

        LanguageConfigMerge.mergeLeafKeys(target, source);

        assertEquals("A", target.getString("module-a.title"));
        assertEquals("B", target.getString("module-b.title"));
    }

    @Test
    void mergeLeafKeys_overwritesLeafWithoutDroppingSiblingKeys() {
        FileConfiguration target = LanguageConfigMerge.loadYaml("""
                quest:
                  title: "Old"
                  desc: "Keep"
                stats:
                  title: "Stats"
                """);
        FileConfiguration source = LanguageConfigMerge.loadYaml("""
                quest:
                  title: "New"
                """);

        LanguageConfigMerge.mergeLeafKeys(target, source);

        assertEquals("New", target.getString("quest.title"));
        assertEquals("Keep", target.getString("quest.desc"));
        assertEquals("Stats", target.getString("stats.title"));
    }

    @Test
    void mergeIntoLanguageMap_insertsWhenLanguageMissing() {
        Map<String, FileConfiguration> map = new HashMap<>();
        FileConfiguration source = LanguageConfigMerge.loadYaml("hello: world");

        LanguageConfigMerge.mergeIntoLanguageMap("en", source, map);

        assertSame(source, map.get("en"));
        assertEquals("world", map.get("en").getString("hello"));
    }

    @Test
    void mergeIntoLanguageMap_mergesWhenLanguageExists() {
        Map<String, FileConfiguration> map = new HashMap<>();
        map.put("en", LanguageConfigMerge.loadYaml("""
                a: 1
                """));
        FileConfiguration source = LanguageConfigMerge.loadYaml("""
                b: 2
                """);

        LanguageConfigMerge.mergeIntoLanguageMap("en", source, map);

        FileConfiguration merged = map.get("en");
        assertNotNull(merged);
        assertEquals(1, merged.getInt("a"));
        assertEquals(2, merged.getInt("b"));
    }
}
