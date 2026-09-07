package com.guild.core.utils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 插件与 Folia gworld 的 Minecraft 版本兼容声明。
 *
 * <p>官方目标版本见 {@link #OFFICIAL_VERSIONS}；未列入但满足最低版本（1.20+ 或 25+/26+ 系列）
 * 的运行时版本按 best-effort 处理，启动时自动声明、不阻断加载。
 */
public final class MinecraftVersionSupport {

    /** 插件与 gworld 均已正式对齐的目标版本（1.20+）。 */
    public static final Set<String> OFFICIAL_VERSIONS = buildOfficialVersions();

    /** 最低 API / 运行时门槛（Bukkit {@code api-version} 对齐）。 */
    public static final String MINIMUM_API_VERSION = "1.20";

    public enum CompatibilityLevel {
        /** 命中 {@link #OFFICIAL_VERSIONS}。 */
        OFFICIAL,
        /** 未在官方列表，但满足最低版本，按 best-effort 声明兼容。 */
        BEST_EFFORT,
        /** 低于最低版本，不应加载。 */
        UNSUPPORTED
    }

    private MinecraftVersionSupport() {
    }

    public static CompatibilityLevel resolve(String version) {
        if (version == null || version.isBlank()) {
            return CompatibilityLevel.UNSUPPORTED;
        }
        if (isOfficialVersion(version)) {
            return CompatibilityLevel.OFFICIAL;
        }
        if (meetsMinimumVersion(version)) {
            return CompatibilityLevel.BEST_EFFORT;
        }
        return CompatibilityLevel.UNSUPPORTED;
    }

    public static boolean isOfficialVersion(String version) {
        for (String official : OFFICIAL_VERSIONS) {
            if (matchesVersionPattern(version, official)) {
                return true;
            }
        }
        return false;
    }

    public static boolean meetsMinimumVersion(String version) {
        if (version == null || version.isBlank()) {
            return false;
        }
        String[] parts = version.split("\\.");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return false;
        }
        try {
            int major = Integer.parseInt(parts[0]);
            if (major >= 25) {
                return true;
            }
            if (major == 1 && parts.length >= 2) {
                int minor = Integer.parseInt(parts[1]);
                return minor >= 20;
            }
        } catch (NumberFormatException ignored) {
            return false;
        }
        return false;
    }

    /**
     * Folia gworld：官方列表或 best-effort 均可启用 NMS 桥接。
     */
    public static boolean isFoliaGworldCompatible(String version) {
        CompatibilityLevel level = resolve(version);
        return level == CompatibilityLevel.OFFICIAL || level == CompatibilityLevel.BEST_EFFORT;
    }

    public static String describe(CompatibilityLevel level, String version) {
        return switch (level) {
            case OFFICIAL -> "officially supported MC " + version;
            case BEST_EFFORT -> "best-effort compatibility declared for MC " + version
                    + " (not in official target list)";
            case UNSUPPORTED -> "unsupported MC " + version + " (requires " + MINIMUM_API_VERSION + "+)";
        };
    }

    public static String formatOfficialVersionList() {
        return String.join(", ", OFFICIAL_VERSIONS);
    }

    /**
     * 与 {@link ServerUtils#getMinecraftVersion()} 解析结果对齐的版本匹配。
     * 支持 {@code 26.1.x} 系列通配（仅用于文档/诊断，官方列表以精确条目为主）。
     */
    public static boolean matchesVersionPattern(String actual, String pattern) {
        if (actual == null || pattern == null) {
            return false;
        }
        if (pattern.equals(actual)) {
            return true;
        }
        if (pattern.endsWith(".x")) {
            String series = pattern.substring(0, pattern.length() - 2);
            return actual.equals(series) || actual.startsWith(series + ".");
        }
        return false;
    }

    private static Set<String> buildOfficialVersions() {
        Set<String> versions = new LinkedHashSet<>();
        List<String> listed = List.of(
                "1.20",
                "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
                "1.21",
                "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6",
                "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11",
                "26.1", "26.1.1", "26.1.2", "26.2"
        );
        versions.addAll(listed);
        return Collections.unmodifiableSet(versions);
    }
}
