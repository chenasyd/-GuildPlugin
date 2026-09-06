package com.guild.core.language;

/**
 * 语言系统路径、已知语言码与模块目录常量。
 */
public final class LanguageConstants {

    public static final String LANG_EN = "en";
    public static final String LANG_ZH = "zh";
    public static final String LANG_PL = "pl";
    public static final String LANG_BR = "br";

    public static final String GUI_LANG_PATH = "lang/gui/";
    public static final String CORE_LANG_PATH = "lang/core/";
    public static final String MODULES_LANG_PATH = "lang/modules/";
    public static final String LANG_FILE_SUFFIX = ".yml";

    public static final String[] KNOWN_LANGS = {
            "en", "zh", "pl", "br",
            "de", "fr", "ru", "zh_tw", "ms",
            "ja", "ko", "es", "pt", "it", "nl", "sv", "tr",
            "vi", "th", "cs", "uk", "ro", "hu", "da", "fi", "no"
    };

    public static final String[] MODULE_DIRS = {
            "announcement", "apitest", "builtin-activity", "member-rank",
            "quest", "stats", "territory", "testlang"
    };

    private LanguageConstants() {
    }

    public static String resolveModuleLangDir(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return moduleId;
        }
        String id = moduleId.trim().toLowerCase();
        return switch (id) {
            case "guild-quest" -> "quest";
            case "guild-territory" -> "territory";
            default -> id;
        };
    }
}
