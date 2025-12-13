package com.guild.core.placeholder;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import com.guild.core.utils.PlaceholderUtils;

import com.guild.core.time.TimeProvider;
import java.util.concurrent.CompletableFuture;

/**
 * Rozszerzenie PlaceholderAPI dla pluginu Guild
 * Zapewnia pełne wsparcie zmiennych danych gildii
 */
public class GuildPlaceholderExpansion extends PlaceholderExpansion {

    private final GuildPlugin plugin;
    private final GuildService guildService;

    public GuildPlaceholderExpansion(GuildPlugin plugin, GuildService guildService) {
        this.plugin = plugin;
        this.guildService = guildService;
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
                // Podstawowe informacje o gildii
                case "name":
                    return getGuildName(player);
                case "tag":
                    return getGuildTag(player);
                case "description":
                    return getGuildDescription(player);
                case "leader":
                    return getGuildLeader(player);
                case "membercount":
                    return getGuildMemberCount(player);
                case "maxmembers":
                    return getGuildMaxMembers(player);
                case "level":
                    return getGuildLevel(player);
                case "balance":
                    return getGuildBalance(player);
                case "frozen":
                    return getGuildFrozenStatus(player);

                // Informacje o graczu w gildii
                case "role":
                    return getPlayerRoleColored(player);
                case "roleraw":
                    return getPlayerRoleRaw(player);
                case "rolecolor":
                    return getPlayerRoleColor(player);
                case "rolecolored":
                    return getPlayerRoleColored(player);
                case "roleprefix":
                    return getPlayerRolePrefix(player);
                case "joined":
                    return getPlayerJoinedTime(player);
                case "contribution":
                    return getPlayerContribution(player);

                // Sprawdzanie statusu gildii
                case "hasguild":
                    return hasGuild(player);
                case "isleader":
                    return isLeader(player);
                case "isofficer":
                    return isOfficer(player);
                case "ismember":
                    return isMember(player);

                // Uprawnienia gildii
                case "caninvite":
                    return canInvite(player);
                case "cankick":
                    return canKick(player);
                case "canpromote":
                    return canPromote(player);
                case "candemote":
                    return canDemote(player);
                case "cansethome":
                    return canSetHome(player);
                case "canmanageeconomy":
                    return canManageEconomy(player);

                default:
                    return "";
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd podczas przetwarzania placeholdera: " + e.getMessage());
            return "";
        }
    }

    // ==================== Podstawowe informacje o gildii ====================

    private String getGuildName(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getName() : "Brak gildii";
        } catch (Exception e) {
            return "Brak gildii";
        }
    }

    private String getGuildTag(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getTag() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getGuildDescription(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getDescription() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getGuildLeader(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getLeaderName() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getGuildMemberCount(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            if (guild == null) return "0";

            CompletableFuture<Integer> future = guildService.getGuildMemberCountAsync(guild.getId());
            return String.valueOf(future.get());
        } catch (Exception e) {
            return "0";
        }
    }

    private String getGuildMaxMembers(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? String.valueOf(guild.getMaxMembers()) : "0";
        } catch (Exception e) {
            return "0";
        }
    }

    private String getGuildLevel(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? String.valueOf(guild.getLevel()) : "0";
        } catch (Exception e) {
            return "0";
        }
    }

    private String getGuildBalance(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? String.format("%.2f", guild.getBalance()) : "0.00";
        } catch (Exception e) {
            return "0.00";
        }
    }

    private String getGuildFrozenStatus(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? (guild.isFrozen() ? "Zamrożona" : "Normalna") : "Brak gildii";
        } catch (Exception e) {
            return "Brak gildii";
        }
    }

    // ==================== Informacje o graczu w gildii ====================

    private String getPlayerRoleRaw(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null ? member.getRole().getDisplayName() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getPlayerRoleColor(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "";
            return PlaceholderUtils.getRoleColorCode(member.getRole());
        } catch (Exception e) {
            return "";
        }
    }

    private String getPlayerRoleColored(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "";
            return PlaceholderUtils.getColoredRoleDisplay(member.getRole());
        } catch (Exception e) {
            return "";
        }
    }

    private String getPlayerRolePrefix(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            GuildMember.Role role = member != null ? member.getRole() : null;
            return PlaceholderUtils.getRoleSeparator(role);
        } catch (Exception e) {
            return "";
        }
    }

    private String getPlayerJoinedTime(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null || member.getJoinedAt() == null) return "";
            return member.getJoinedAt().format(TimeProvider.FULL_FORMATTER);
        } catch (Exception e) {
            return "";
        }
    }

    private String getPlayerContribution(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            // Tymczasowo zwracamy 0, ponieważ klasa GuildMember nie ma jeszcze pola contribution
            return member != null ? "0" : "0";
        } catch (Exception e) {
            return "0";
        }
    }

    // ==================== Sprawdzanie statusu gildii ====================

    private String hasGuild(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? "Tak" : "Nie";
        } catch (Exception e) {
            return "Nie";
        }
    }

    private String isLeader(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null && member.getRole() == GuildMember.Role.LEADER ? "Tak" : "Nie";
        } catch (Exception e) {
            return "Nie";
        }
    }

    private String isOfficer(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null && member.getRole() == GuildMember.Role.OFFICER ? "Tak" : "Nie";
        } catch (Exception e) {
            return "Nie";
        }
    }

    private String isMember(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null ? "Tak" : "Nie";
        } catch (Exception e) {
            return "Nie";
        }
    }

    // ==================== Uprawnienia gildii ====================

    private String canInvite(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "Nie";

            return (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER) ? "Tak" : "Nie";
        } catch (Exception e) {
            return "Nie";
        }
    }

    private String canKick(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "Nie";

            return (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER) ? "Tak" : "Nie";
        } catch (Exception e) {
            return "Nie";
        }
    }

    private String canPromote(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "Nie";

            return member.getRole() == GuildMember.Role.LEADER ? "Tak" : "Nie";
        } catch (Exception e) {
            return "Nie";
        }
    }

    private String canDemote(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "Nie";

            return member.getRole() == GuildMember.Role.LEADER ? "Tak" : "Nie";
        } catch (Exception e) {
            return "Nie";
        }
    }

    private String canSetHome(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "Nie";

            return (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER) ? "Tak" : "Nie";
        } catch (Exception e) {
            return "Nie";
        }
    }

    private String canManageEconomy(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "Nie";

            return (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER) ? "Tak" : "Nie";
        } catch (Exception e) {
            return "Nie";
        }
    }
}
