package com.guild.core.language;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

/**
 * 语言 YAML 合并工具：只写入叶子键，避免 section 覆盖导致其它模块键丢失。
 */
public final class LanguageConfigMerge {

    private LanguageConfigMerge() {
    }

    /**
     * 将 {@code source} 的叶子键合并进 {@code target}（原地修改 target）。
     */
    public static void mergeLeafKeys(FileConfiguration target, FileConfiguration source) {
        if (target == null || source == null) {
            return;
        }
        for (String key : source.getKeys(true)) {
            if (source.isConfigurationSection(key)) {
                continue;
            }
            target.set(key, source.get(key));
        }
    }

    /**
     * 将 {@code source} 合并进 {@code targetMap} 中对应语言的配置；语言不存在则直接放入 map。
     */
    public static void mergeIntoLanguageMap(String lang, FileConfiguration source,
                                            Map<String, FileConfiguration> targetMap) {
        if (lang == null || source == null || targetMap == null) {
            return;
        }
        FileConfiguration existing = targetMap.get(lang);
        if (existing == null) {
            targetMap.put(lang, source);
            return;
        }
        mergeLeafKeys(existing, source);
    }

    /** 从 YAML 字符串加载配置（测试与工具用途）。 */
    public static FileConfiguration loadYaml(String yaml) {
        return YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
    }
}
