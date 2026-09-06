package com.guild.module.example.territory;

import com.guild.sdk.config.ModuleConfigSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 领地声明世界范围策略：全部开放、白名单或黑名单。
 * <p>
 * 优先读取 {@code claim.worlds.mode} + {@code claim.worlds.list}；
 * 未配置 mode 时兼容旧版 {@code claim.allowed-worlds} / {@code claim.denied-worlds}。
 */
public final class TerritoryWorldClaimPolicy {

    public enum Mode {
        /** 所有世界均可声明（默认）。 */
        ALL,
        /** 仅 {@link #worlds} 中的世界可声明。 */
        WHITELIST,
        /** {@link #worlds} 中的世界不可声明。 */
        BLACKLIST
    }

    private final Mode mode;
    private final List<String> worlds;

    private TerritoryWorldClaimPolicy(Mode mode, List<String> worlds) {
        this.mode = mode;
        this.worlds = worlds;
    }

    public static TerritoryWorldClaimPolicy from(ModuleConfigSection config) {
        if (config == null) {
            return new TerritoryWorldClaimPolicy(Mode.ALL, List.of());
        }

        List<String> allowed = config.getStringList("claim.allowed-worlds");
        List<String> denied = config.getStringList("claim.denied-worlds");
        String modeRaw = config.getString("claim.worlds.mode", "");
        List<String> list = normalizeList(config.getStringList("claim.worlds.list"));

        if (modeRaw != null && !modeRaw.isBlank()) {
            Mode mode = parseMode(modeRaw);
            if (mode == Mode.ALL && list.isEmpty()) {
                if (allowed != null && !allowed.isEmpty()) {
                    return new TerritoryWorldClaimPolicy(Mode.WHITELIST, normalizeList(allowed));
                }
                if (denied != null && !denied.isEmpty()) {
                    return new TerritoryWorldClaimPolicy(Mode.BLACKLIST, normalizeList(denied));
                }
            }
            return new TerritoryWorldClaimPolicy(mode, list);
        }

        if (allowed != null && !allowed.isEmpty()) {
            return new TerritoryWorldClaimPolicy(Mode.WHITELIST, normalizeList(allowed));
        }
        if (denied != null && !denied.isEmpty()) {
            return new TerritoryWorldClaimPolicy(Mode.BLACKLIST, normalizeList(denied));
        }
        return new TerritoryWorldClaimPolicy(Mode.ALL, List.of());
    }

    public Mode getMode() {
        return mode;
    }

    public List<String> getWorlds() {
        return worlds;
    }

    public boolean isAllowed(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        return switch (mode) {
            case ALL -> true;
            case WHITELIST -> worlds.stream().anyMatch(w -> w.equalsIgnoreCase(worldName));
            case BLACKLIST -> worlds.stream().noneMatch(w -> w.equalsIgnoreCase(worldName));
        };
    }

    /** 世界被禁止声明时的语言键。 */
    public String blockedMessageKey() {
        return switch (mode) {
            case WHITELIST -> "module.territory.world-blocked-whitelist";
            case BLACKLIST -> "module.territory.world-blocked-blacklist";
            case ALL -> "module.territory.world-blocked";
        };
    }

    private static Mode parseMode(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "whitelist", "allow", "allowlist", "white" -> Mode.WHITELIST;
            case "blacklist", "deny", "denylist", "black" -> Mode.BLACKLIST;
            case "all", "any", "none", "" -> Mode.ALL;
            default -> Mode.ALL;
        };
    }

    private static List<String> normalizeList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(raw.size());
        for (String entry : raw) {
            if (entry != null && !entry.isBlank()) {
                out.add(entry.trim());
            }
        }
        return out.isEmpty() ? List.of() : Collections.unmodifiableList(out);
    }
}
