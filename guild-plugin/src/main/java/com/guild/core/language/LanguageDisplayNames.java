package com.guild.core.language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 语言代码 → 显示名称映射。 */
public final class LanguageDisplayNames {

    private static final Map<String, String> CODE_TO_NAME = new HashMap<>();

    static {
        CODE_TO_NAME.put("en", "English");
        CODE_TO_NAME.put("zh", "\u4e2d\u6587");
        CODE_TO_NAME.put("pl", "Polski");
        CODE_TO_NAME.put("br", "Portugu\u00eas (BR)");
        CODE_TO_NAME.put("de", "Deutsch");
        CODE_TO_NAME.put("fr", "Fran\u00e7ais");
        CODE_TO_NAME.put("es", "Espa\u00f1ol");
        CODE_TO_NAME.put("ja", "\u65e5\u672c\u8a9e");
        CODE_TO_NAME.put("ko", "\ud55c\uad6d\uc5b4");
        CODE_TO_NAME.put("ru", "\u0420\u0443\u0441\u0441\u043a\u0438\u0439");
        CODE_TO_NAME.put("zh_tw", "\u7e41\u9ad4\u4e2d\u6587");
        CODE_TO_NAME.put("ms", "Bahasa Melayu");
        CODE_TO_NAME.put("it", "Italiano");
        CODE_TO_NAME.put("nl", "Nederlands");
        CODE_TO_NAME.put("sv", "Svenska");
        CODE_TO_NAME.put("tr", "T\u00fcrk\u00e7e");
        CODE_TO_NAME.put("vi", "Ti\u1ebfng Vi\u1ec7t");
        CODE_TO_NAME.put("th", "\u0e44\u0e17\u0e22");
        CODE_TO_NAME.put("cs", "\u010ce\u0161tina");
        CODE_TO_NAME.put("pt", "Portugu\u00eas");
        CODE_TO_NAME.put("uk", "\u0423\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430");
        CODE_TO_NAME.put("ro", "Rom\u00e2n\u0103");
        CODE_TO_NAME.put("hu", "Magyar");
        CODE_TO_NAME.put("da", "Dansk");
        CODE_TO_NAME.put("fi", "Suomi");
        CODE_TO_NAME.put("no", "Norsk");
    }

    private LanguageDisplayNames() {
    }

    public static List<String> formatSupportedLanguages(Set<String> supportedLanguages) {
        List<String> names = new ArrayList<>();
        for (String code : supportedLanguages) {
            String displayName = CODE_TO_NAME.getOrDefault(code, code.toUpperCase());
            names.add(code + " (" + displayName + ")");
        }
        Collections.sort(names);
        return names;
    }
}
