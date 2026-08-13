package com.guild.war.report;

import com.guild.war.model.WarParticipantSnapshot;
import com.guild.war.model.WarReportSnapshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Formats and writes war reports to JSON / CSV under the plugin data folder.
 */
public final class WarReportExporter {

    private WarReportExporter() {
    }

    public static String toJson(WarReportSnapshot snap) {
        StringBuilder sb = new StringBuilder(512);
        sb.append('{');
        append(sb, "reportId", snap.reportId(), true);
        append(sb, "runtimeMatchId", snap.runtimeMatchId(), false);
        append(sb, "guildAId", snap.guildAId(), false);
        append(sb, "guildAName", snap.guildAName(), false);
        append(sb, "guildBId", snap.guildBId(), false);
        append(sb, "guildBName", snap.guildBName(), false);
        append(sb, "winnerGuildId", snap.winnerGuildId(), false);
        append(sb, "winnerName", snap.winnerName(), false);
        append(sb, "mode", snap.mode() != null ? snap.mode().name() : null, false);
        append(sb, "scoreA", snap.scoreA(), false);
        append(sb, "scoreB", snap.scoreB(), false);
        append(sb, "scoreToWin", snap.scoreToWin(), false);
        append(sb, "presetName", snap.presetName(), false);
        append(sb, "endReason", snap.endReason(), false);
        append(sb, "startedAt", snap.startedAt(), false);
        append(sb, "endedAt", snap.endedAt(), false);
        append(sb, "durationMs", snap.durationMs(), false);
        append(sb, "seasonId", snap.seasonId(), false);
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '{') {
            sb.append(',');
        }
        sb.append("\"participants\":[");
        List<WarParticipantSnapshot> parts = snap.participants();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            WarParticipantSnapshot p = parts.get(i);
            sb.append('{');
            append(sb, "uuid", p.uuid() != null ? p.uuid().toString() : null, true);
            append(sb, "name", p.name(), false);
            append(sb, "side", p.side() != null ? p.side().name() : null, false);
            append(sb, "guildId", p.guildId(), false);
            append(sb, "kills", p.kills(), false);
            append(sb, "eliminated", p.eliminated(), false);
            sb.append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    public static String toCsv(WarReportSnapshot snap) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("report_id,runtime_match_id,guild_a,guild_b,winner,mode,score_a,score_b,")
                .append("score_to_win,preset,end_reason,started_at,ended_at,duration_ms,season_id,")
                .append("player_uuid,player_name,side,player_guild_id,kills,eliminated\n");
        String id = snap.reportId() != null ? String.valueOf(snap.reportId()) : "";
        String base = csv(id) + ','
                + snap.runtimeMatchId() + ','
                + csv(snap.guildAName()) + ','
                + csv(snap.guildBName()) + ','
                + csv(snap.winnerName()) + ','
                + csv(snap.mode() != null ? snap.mode().name() : "") + ','
                + snap.scoreA() + ','
                + snap.scoreB() + ','
                + snap.scoreToWin() + ','
                + csv(snap.presetName()) + ','
                + csv(snap.endReason()) + ','
                + snap.startedAt() + ','
                + snap.endedAt() + ','
                + snap.durationMs() + ','
                + csv(snap.seasonId());
        List<WarParticipantSnapshot> parts = snap.participants();
        if (parts.isEmpty()) {
            sb.append(base).append(",,,,,,\n");
            return sb.toString();
        }
        for (WarParticipantSnapshot p : parts) {
            sb.append(base).append(',')
                    .append(csv(p.uuid() != null ? p.uuid().toString() : "")).append(',')
                    .append(csv(p.name())).append(',')
                    .append(csv(p.side() != null ? p.side().name() : "")).append(',')
                    .append(p.guildId()).append(',')
                    .append(p.kills()).append(',')
                    .append(p.eliminated()).append('\n');
        }
        return sb.toString();
    }

    /**
     * @param format {@code json} or {@code csv} (default json)
     * @return written file path
     */
    public static Path write(Path directory, WarReportSnapshot snap, String format) throws IOException {
        Files.createDirectories(directory);
        String fmt = format == null ? "json" : format.toLowerCase(Locale.ROOT);
        String id = snap.reportId() != null ? String.valueOf(snap.reportId()) : "runtime-" + snap.runtimeMatchId();
        if ("csv".equals(fmt)) {
            Path file = directory.resolve("war-report-" + id + ".csv");
            Files.writeString(file, toCsv(snap), StandardCharsets.UTF_8);
            return file;
        }
        Path file = directory.resolve("war-report-" + id + ".json");
        Files.writeString(file, toJson(snap), StandardCharsets.UTF_8);
        return file;
    }

    private static void append(StringBuilder sb, String key, Object value, boolean first) {
        if (!first) {
            sb.append(',');
        }
        sb.append('"').append(key).append("\":");
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else {
            sb.append('"').append(escapeJson(String.valueOf(value))).append('"');
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String csv(String s) {
        if (s == null) {
            return "";
        }
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
