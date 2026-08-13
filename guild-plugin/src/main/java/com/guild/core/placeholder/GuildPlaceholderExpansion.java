package com.guild.core.placeholder;

import com.guild.GuildPlugin;
import com.guild.core.cache.GuildPlayerDataCache;
import com.guild.core.time.TimeProvider;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import com.guild.war.season.WarSeasonService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guild 插件 PlaceholderAPI 扩展（identifier = {@code guild}）。
 * <p>
 * 通过 {@link GuildPlayerDataCache} 降低主线程同步 JDBC 频率。
 */
public class GuildPlaceholderExpansion extends PlaceholderExpansion {

    private final GuildPlugin plugin;
    private final GuildService guildService;

    private final ConcurrentHashMap<String, CachedWarRow> warCache = new ConcurrentHashMap<>();
    private static final long WAR_TTL_MS = 5_000L;

    private record CachedWarRow(WarSeasonService.SeasonRow row, String season, long at) {}

    public GuildPlaceholderExpansion(GuildPlugin plugin, GuildService guildService) {
        this.plugin = plugin;
        this.guildService = guildService;
        PlaceholderUtils.setLanguageManager(plugin.getLanguageManager());
    }

    @Override
    public @NotNull String getIdentifier() {
        return "guild";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GuildTeam";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    private GuildPlayerDataCache.Snapshot snap(Player player) {
        GuildPlayerDataCache cache = plugin.getGuildPlayerDataCache();
        if (cache != null) {
            return cache.get(player.getUniqueId());
        }
        // Fallback without cache
        try {
            Guild g = guildService.getPlayerGuild(player.getUniqueId());
            GuildMember m = guildService.getGuildMember(player.getUniqueId());
            return new GuildPlayerDataCache.Snapshot(g, m, null, null, System.currentTimeMillis());
        } catch (Exception e) {
            return new GuildPlayerDataCache.Snapshot(null, null, null, null, 0L);
        }
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        String[] args = params.split("_");
        if (args.length == 0) {
            return "";
        }

        try {
            switch (args[0].toLowerCase()) {
                case "name" -> {
                    Guild g = snap(player).guild;
                    return g != null ? g.getName() : "-";
                }
                case "tag" -> {
                    Guild g = snap(player).guild;
                    return g != null && g.getTag() != null ? g.getTag() : "";
                }
                case "description" -> {
                    Guild g = snap(player).guild;
                    return g != null && g.getDescription() != null ? g.getDescription() : "";
                }
                case "leader" -> {
                    Guild g = snap(player).guild;
                    return g != null && g.getLeaderName() != null ? g.getLeaderName() : "";
                }
                case "membercount" -> {
                    GuildPlayerDataCache.Snapshot s = snap(player);
                    if (s.guild == null) return "0";
                    if (s.memberCount != null) return String.valueOf(s.memberCount);
                    return "0"; // async fill; next tick/refresh will show
                }
                case "maxmembers" -> {
                    Guild g = snap(player).guild;
                    return g != null ? String.valueOf(g.getMaxMembers()) : "0";
                }
                case "level" -> {
                    Guild g = snap(player).guild;
                    return g != null ? String.valueOf(g.getLevel()) : "0";
                }
                case "balance" -> {
                    Guild g = snap(player).guild;
                    return g != null ? String.format("%.2f", g.getBalance()) : "0.00";
                }
                case "frozen" -> {
                    Guild g = snap(player).guild;
                    return g != null ? (g.isFrozen() ? "True" : "False") : "-";
                }
                case "role" -> {
                    return getPlayerRoleColored(player);
                }
                case "roleraw" -> {
                    GuildMember m = snap(player).member;
                    return m != null ? m.getRole().getDisplayName() : "";
                }
                case "rolecolor" -> {
                    GuildMember m = snap(player).member;
                    return m != null ? PlaceholderUtils.getRoleColorCode(m.getRole()) : "";
                }
                case "rolecolored" -> {
                    return getPlayerRoleColored(player);
                }
                case "roleprefix" -> {
                    GuildMember m = snap(player).member;
                    return PlaceholderUtils.getRoleSeparator(m != null ? m.getRole() : null);
                }
                case "joined" -> {
                    GuildMember m = snap(player).member;
                    if (m == null || m.getJoinedAt() == null) return "";
                    return m.getJoinedAt().format(TimeProvider.FULL_FORMATTER);
                }
                case "contribution" -> {
                    GuildPlayerDataCache.Snapshot s = snap(player);
                    if (s.guild == null || s.member == null) return "0";
                    if (s.contributionNet != null) {
                        return String.format("%.2f", s.contributionNet);
                    }
                    return "0";
                }
                case "hasguild" -> {
                    return snap(player).guild != null ? "True" : "False";
                }
                case "isleader" -> {
                    GuildMember m = snap(player).member;
                    return m != null && m.getRole() == GuildMember.Role.LEADER ? "True" : "False";
                }
                case "isofficer" -> {
                    GuildMember m = snap(player).member;
                    return m != null && m.getRole() == GuildMember.Role.OFFICER ? "True" : "False";
                }
                case "ismember" -> {
                    // 普通成员职位（不含会长/官员）
                    GuildMember m = snap(player).member;
                    return m != null && m.getRole() == GuildMember.Role.MEMBER ? "True" : "False";
                }
                case "isinguild" -> {
                    // 是否已加入任意公会（与 hasguild 同义，便于脚本）
                    return snap(player).guild != null ? "True" : "False";
                }
                case "caninvite" -> {
                    return boolPerm(player, "guild.invite");
                }
                case "cankick" -> {
                    return boolPerm(player, "guild.kick");
                }
                case "canpromote" -> {
                    return boolPerm(player, "guild.promote");
                }
                case "candemote" -> {
                    return boolPerm(player, "guild.demote");
                }
                case "cansethome" -> {
                    GuildMember m = snap(player).member;
                    if (m == null) return "False";
                    return (m.getRole() == GuildMember.Role.LEADER
                            || m.getRole() == GuildMember.Role.OFFICER) ? "True" : "False";
                }
                case "canmanageeconomy" -> {
                    GuildMember m = snap(player).member;
                    if (m == null) return "False";
                    return (m.getRole() == GuildMember.Role.LEADER
                            || m.getRole() == GuildMember.Role.OFFICER) ? "True" : "False";
                }
                case "war" -> {
                    return handleWarPlaceholder(player, args);
                }
                case "module" -> {
                    return handleModulePlaceholder(player, args);
                }
                default -> {
                    return "";
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Placeholder error: " + e.getMessage());
            return "";
        }
    }

    private String boolPerm(Player player, String permission) {
        try {
            return plugin.getPermissionManager().hasPermission(player, permission) ? "True" : "False";
        } catch (Exception e) {
            return "False";
        }
    }

    private String getPlayerRoleColored(Player player) {
        GuildMember m = snap(player).member;
        if (m == null) return "";
        String lang = plugin.getLanguageManager().getPlayerLanguage(player);
        return PlaceholderUtils.getColoredRoleDisplay(m.getRole(), lang);
    }

    private String handleWarPlaceholder(Player player, String[] args) {
        if (args.length < 2 || plugin.getWarSeasonService() == null) {
            return "0";
        }
        try {
            Guild guild = snap(player).guild;
            if (guild == null) {
                return "0";
            }
            String season = plugin.getWarSeasonService().currentSeasonId();
            String key = guild.getId() + "|" + season;
            long now = System.currentTimeMillis();
            CachedWarRow cached = warCache.get(key);
            WarSeasonService.SeasonRow row;
            if (cached != null && now - cached.at < WAR_TTL_MS) {
                row = cached.row;
                season = cached.season;
            } else {
                row = plugin.getWarSeasonService().getGuildStats(guild.getId(), season);
                warCache.put(key, new CachedWarRow(row, season, now));
            }
            return switch (args[1].toLowerCase()) {
                case "wins" -> String.valueOf(row.wins());
                case "losses" -> String.valueOf(row.losses());
                case "draws" -> String.valueOf(row.draws());
                case "kills" -> String.valueOf(row.kills());
                case "matches" -> String.valueOf(row.matches());
                case "season" -> season;
                default -> "0";
            };
        } catch (Exception e) {
            return "0";
        }
    }

    private String handleModulePlaceholder(Player player, String[] args) {
        if (args.length < 2 || plugin.getModuleManager() == null) {
            return "";
        }

        var providers = plugin.getModuleManager().getSharedApi().getPlaceholderProviders();
        if (providers == null || providers.isEmpty()) {
            return "";
        }

        String identifier = args[1].toLowerCase();
        var provider = providers.get(identifier);
        if (provider == null) {
            return "";
        }

        String params = "";
        if (args.length > 2) {
            params = String.join("_", Arrays.copyOfRange(args, 2, args.length));
        }

        try {
            return provider.onRequest(player, params);
        } catch (Exception e) {
            plugin.getLogger().warning("Placeholder provider '" + identifier + "' failed: " + e.getMessage());
            return "";
        }
    }
}
