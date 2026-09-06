package com.guild.war;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipantSnapshot;
import com.guild.war.model.WarReportSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;

/** 公会战消息广播与玩家私信。 */
public final class WarBroadcastHelper {

    private final GuildPlugin plugin;
    private final GuildService guildService;

    public WarBroadcastHelper(GuildPlugin plugin, GuildService guildService) {
        this.plugin = plugin;
        this.guildService = guildService;
    }

    public void broadcastMatch(WarMatch match, String key, String def, String... ph) {
        broadcastGuild(match.guildAId(), key, def, ph);
        broadcastGuild(match.guildBId(), key, def, ph);
    }

    public void broadcastGuild(int guildId, String key, String def, String... ph) {
        guildService.getGuildMembersAsync(guildId).thenAccept(members -> {
            if (members == null) {
                return;
            }
            CompatibleScheduler.runTask(plugin, () -> {
                for (GuildMember member : members) {
                    Player player = Bukkit.getPlayer(member.getPlayerUuid());
                    if (player != null && player.isOnline()) {
                        send(player, key, def, localizePlaceholders(player, ph));
                    }
                }
            });
        });
    }

    public void broadcastReportLines(WarReportSnapshot snapshot) {
        String id = snapshot.reportId() != null
                ? String.valueOf(snapshot.reportId())
                : String.valueOf(snapshot.runtimeMatchId());
        String header = CoreMsg.rawDefault(plugin, "war.report.broadcast",
                "&6[战报 #{id}] &f{a} &a{sa} &7: &c{sb} &f{b} &7→ &e{winner}",
                "{id}", id,
                "{a}", snapshot.guildAName(),
                "{b}", snapshot.guildBName(),
                "{sa}", String.valueOf(snapshot.scoreA()),
                "{sb}", String.valueOf(snapshot.scoreB()),
                "{winner}", snapshot.winnerName());
        Bukkit.broadcastMessage(ColorUtils.colorize(header));
        for (WarParticipantSnapshot participant : snapshot.participants()) {
            if (participant.kills() <= 0) {
                continue;
            }
            String line = CoreMsg.rawDefault(plugin, "war.report.kill-line",
                    "&7  · &f{player} &7kills: &e{kills}",
                    "{player}", participant.name(),
                    "{kills}", String.valueOf(participant.kills()));
            Bukkit.broadcastMessage(ColorUtils.colorize(line));
        }
    }

    public void msg(Player player, String key, String def, String... ph) {
        send(player, key, def, ph);
    }

    private void send(Player player, String key, String def, String... ph) {
        String prefix = CoreMsg.raw(plugin, player, "war.prefix", "&c[公会战] &r");
        String body = CoreMsg.raw(plugin, player, key, def, ph);
        player.sendMessage(ColorUtils.colorize(prefix + body));
    }

    /** Resolve placeholder values that are lang keys (war.* / world.*) per recipient. */
    private String[] localizePlaceholders(Player player, String... ph) {
        if (ph == null || ph.length == 0) {
            return ph != null ? ph : new String[0];
        }
        String[] out = Arrays.copyOf(ph, ph.length);
        for (int i = 0; i + 1 < out.length; i += 2) {
            String value = out[i + 1];
            if (value != null && (value.startsWith("war.") || value.startsWith("world."))) {
                out[i + 1] = CoreMsg.raw(plugin, player, value, defaultForKey(value), ph);
            }
        }
        return out;
    }

    private static String defaultForKey(String key) {
        return switch (key) {
            case "war.mode.first" -> "积分先到";
            case "war.mode.timed" -> "限时积分";
            case "war.mode.survive" -> "最终存活";
            case "war.draw" -> "平局";
            case "war.reason.admin-end" -> "管理员强制结束";
            case "war.reason.first-score" -> "先达到 {score} 分";
            case "war.reason.timed-win" -> "限时结束，积分更高";
            case "war.reason.timed-draw" -> "限时结束，平局";
            case "war.reason.survive-alive" -> "时间到，存活人数更多";
            case "war.reason.survive-kills" -> "时间到，击杀更多";
            case "war.reason.survive-draw" -> "时间到，平局";
            case "war.reason.both-eliminated" -> "双方全灭";
            case "war.reason.wipe" -> "歼灭对方";
            case "war.reason.plugin-shutdown" -> "插件关闭";
            default -> key;
        };
    }
}
