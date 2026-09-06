package com.guild.module.example.territory;

import com.guild.models.Guild;
import com.guild.sdk.placeholder.PlaceholderProvider;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

/**
 * 领地模块 PlaceholderAPI 提供者。
 * <p>
 * 用法：{@code %guild_module_territory_<params>%}，例如 {@code %guild_module_territory_has%}。
 */
public final class TerritoryPlaceholderProvider implements PlaceholderProvider {

    private final TerritoryModule module;

    public TerritoryPlaceholderProvider(TerritoryModule module) {
        this.module = module;
    }

    @Override
    public String getIdentifier() {
        return "territory";
    }

    @Override
    public String onRequest(Player player, String params) {
        if (player == null || params == null || params.isBlank()) {
            return "";
        }

        TerritoryRepository repository = module.getRepository();
        if (repository == null) {
            return "";
        }

        String key = params.toLowerCase(Locale.ROOT).trim();

        if ("server".equals(key)) {
            return repository.getLocalServerId();
        }

        if ("wg_ready".equals(key)) {
            return bool(module.isWorldGuardReady());
        }

        if ("inside".equals(key)) {
            return bool(isInsideAnyTerritory(player));
        }

        Guild guild = resolveGuild(player);
        if ("inside_own".equals(key)) {
            return bool(isInsideOwnTerritory(player, guild));
        }

        if (guild == null) {
            return defaultForNoGuild(key);
        }

        int guildId = guild.getId();
        String worldName = player.getWorld().getName();

        return switch (key) {
            case "has" -> bool(repository.getLocal(guildId, worldName).isPresent());
            case "has_any" -> bool(!repository.findByGuildId(guildId).isEmpty());
            case "count" -> String.valueOf(repository.findByGuildId(guildId).size());
            case "count_local" -> String.valueOf(repository.findByGuildIdLocal(guildId).size());
            case "world" -> repository.getLocal(guildId, worldName)
                    .map(TerritoryRecord::getWorldName)
                    .orElse("");
            case "region" -> repository.getLocal(guildId, worldName)
                    .map(TerritoryRecord::getRegionId)
                    .orElse("");
            case "sync" -> repository.getLocal(guildId, worldName)
                    .map(record -> record.getSyncState().name().toLowerCase(Locale.ROOT))
                    .orElse("none");
            case "volume" -> String.valueOf(volume(repository.getLocal(guildId, worldName)));
            case "server_id" -> repository.getLocalServerId();
            default -> "";
        };
    }

    private static String defaultForNoGuild(String key) {
        return switch (key) {
            case "has", "has_any", "inside_own" -> "False";
            case "count", "count_local", "volume" -> "0";
            case "world", "region", "sync" -> "";
            default -> "";
        };
    }

    private Guild resolveGuild(Player player) {
        if (module.getContext() == null) {
            return null;
        }
        try {
            return module.getContext().getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isInsideAnyTerritory(Player player) {
        if (!module.isWorldGuardReady()) {
            return false;
        }
        var loc = player.getLocation();
        return module.getBridge().findTerritoryAt(
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        ).isPresent();
    }

    private boolean isInsideOwnTerritory(Player player, Guild guild) {
        if (guild == null || !module.isWorldGuardReady()) {
            return false;
        }
        var loc = player.getLocation();
        Optional<TerritoryRecord> at = module.getBridge().findTerritoryAt(
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
        return at.isPresent() && at.get().getGuildId() == guild.getId();
    }

    private static long volume(Optional<TerritoryRecord> record) {
        if (record.isEmpty()) {
            return 0L;
        }
        TerritoryRecord r = record.get();
        long dx = (long) r.getMaxX() - r.getMinX() + 1;
        long dy = (long) r.getMaxY() - r.getMinY() + 1;
        long dz = (long) r.getMaxZ() - r.getMinZ() + 1;
        return dx * dy * dz;
    }

    private static String bool(boolean value) {
        return value ? "True" : "False";
    }
}
