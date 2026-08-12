package com.guild.war.command;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import com.guild.core.language.LocalizedException;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.WarSeasonGUI;
import com.guild.war.GuildWarService;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarParticipantSnapshot;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarReportSnapshot;
import com.guild.war.model.WarTeamSide;
import com.guild.war.season.WarSeasonService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /guildwar — 小型固定地图工会战。
 *
 * <pre>
 * /guildwar challenge &lt;工会名|标签&gt; [--preset] [--mode first|timed|survive] [--max N] [--score N] [--time SEC]
 * /guildwar accept|deny|join|leave|ready|cancel|status|report|season|help
 * /guildwar admin end &lt;matchId&gt;
 * </pre>
 */
public final class GuildWarCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "guild.war";
    private static final String PERM_ADMIN = "guild.war.admin";

    private final GuildPlugin plugin;
    private final GuildWarService war;

    public GuildWarCommand(GuildPlugin plugin, GuildWarService war) {
        this.plugin = plugin;
        this.war = war;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            send(sender, "war.no-permission", "&c你没有权限使用工会战");
            return true;
        }
        if (!war.isEnabled()) {
            send(sender, "war.unavailable", "&c工会战不可用: {reason}",
                    "{reason}", war.unavailableReason());
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "challenge", "c" -> challenge(sender, args);
            case "accept", "a" -> requirePlayer(sender, p -> war.accept(p).whenComplete((m, e) ->
                    reply(p, e, "war.accept.ok", "&a已接受挑战，开始报名")));
            case "deny", "d" -> requirePlayer(sender, p -> war.deny(p).whenComplete((v, e) ->
                    reply(p, e, "war.deny.ok", "&e已拒绝挑战")));
            case "join", "j" -> requirePlayer(sender, p -> war.join(p).whenComplete((v, e) ->
                    reply(p, e, "war.join.ok", "&a报名成功")));
            case "leave", "l" -> requirePlayer(sender, p -> war.leave(p).whenComplete((v, e) ->
                    reply(p, e, "war.leave.ok", "&e已退出报名")));
            case "ready", "r" -> requirePlayer(sender, p -> war.ready(p).whenComplete((v, e) ->
                    reply(p, e, "war.ready.ok", "&a已标记准备就绪")));
            case "cancel" -> requirePlayer(sender, p -> war.cancel(p).whenComplete((v, e) ->
                    reply(p, e, "war.cancel.ok", "&e已取消")));
            case "status", "s" -> status(sender);
            case "report" -> report(sender, args);
            case "season" -> season(sender);
            case "admin" -> admin(sender, args);
            case "help", "?" -> help(sender);
            default -> help(sender);
        }
        return true;
    }

    private void challenge(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "war.player-only", "&c仅玩家可用");
            return;
        }
        if (args.length < 2) {
            send(sender, "war.challenge.usage",
                    "&c用法: /guildwar challenge <工会名|标签> [--preset x] [--mode first|timed|survive] [--max N] [--score N] [--time SEC]");
            return;
        }
        String target = args[1];
        String preset = flag(args, "--preset");
        VictoryMode mode = VictoryMode.parse(flag(args, "--mode"));
        Integer max = intFlag(args, "--max");
        Integer score = intFlag(args, "--score");
        Integer time = intFlag(args, "--time");

        war.challenge(player, target, preset, mode, max, score, time)
                .whenComplete((match, err) -> {
                    if (err != null) {
                        String body = LocalizedException.resolveThrowable(plugin, player, err);
                        String prefix = CoreMsg.raw(plugin, player, "war.prefix", "&c[工会战] &r");
                        player.sendMessage(ColorUtils.colorize(prefix + body));
                    } else {
                        send(player, "war.challenge.success",
                                "&a已向 &f{guild} &a发起挑战（#{id}，{mode}）",
                                "{guild}", match.guildBName(),
                                "{id}", String.valueOf(match.id()),
                                "{mode}", match.mode().displayName(plugin, player));
                    }
                });
    }

    private void status(CommandSender sender) {
        if (sender instanceof Player player) {
            WarMatch mine = war.getMatchByPlayer(player.getUniqueId());
            if (mine != null) {
                printMatch(sender, mine);
                return;
            }
        }
        var active = war.getActiveMatches();
        if (active.isEmpty()) {
            send(sender, "war.status.none", "&7当前没有进行中的工会战");
            return;
        }
        for (WarMatch m : active) {
            printMatch(sender, m);
        }
    }

    private void report(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            try {
                int id = Integer.parseInt(args[1]);
                war.reports().getByReportIdAsync(id).thenAccept(snap ->
                        CompatibleScheduler.runTask(plugin, () -> showReport(sender, snap)));
            } catch (NumberFormatException e) {
                send(sender, "war.report.bad-id", "&c战报 ID 无效");
            }
            return;
        }
        if (!(sender instanceof Player player)) {
            send(sender, "war.report.need-id", "&c控制台请使用 /guildwar report <id>");
            return;
        }
        war.reports().getLatestForPlayerAsync(player.getUniqueId()).thenAccept(snap ->
                CompatibleScheduler.runTask(plugin, () -> showReport(sender, snap)));
    }

    private void showReport(CommandSender sender, WarReportSnapshot snap) {
        if (snap == null) {
            send(sender, "war.report.none", "&7没有找到战报");
            return;
        }
        String id = snap.reportId() != null ? String.valueOf(snap.reportId()) : "?";
        send(sender, "war.report.header",
                "&6── 战报 #{id} ── &f{a} &a{sa}&7:&c{sb} &f{b} &7→ &e{winner}",
                "{id}", id,
                "{a}", snap.guildAName(),
                "{b}", snap.guildBName(),
                "{sa}", String.valueOf(snap.scoreA()),
                "{sb}", String.valueOf(snap.scoreB()),
                "{winner}", snap.winnerName());
        send(sender, "war.report.meta",
                "&7模式: &f{mode} &7原因: &f{reason} &7耗时: &f{sec}s",
                "{mode}", snap.mode().name(),
                "{reason}", snap.endReason() != null ? snap.endReason() : "-",
                "{sec}", String.valueOf(snap.durationMs() / 1000));
        for (WarParticipantSnapshot p : snap.participants()) {
            send(sender, "war.report.player",
                    "&7  {side} &f{name} &7kills=&e{kills}{elim}",
                    "{side}", p.side().name(),
                    "{name}", p.name(),
                    "{kills}", String.valueOf(p.kills()),
                    "{elim}", p.eliminated() ? " &8(out)" : "");
        }
    }

    private void season(CommandSender sender) {
        WarSeasonService seasonService = plugin.getWarSeasonService();
        if (seasonService == null) {
            send(sender, "war.season.unavailable", "&c赛季系统未就绪");
            return;
        }
        String seasonId = seasonService.currentSeasonId();
        seasonService.getLeaderboardAsync(seasonId, 27).thenAccept(rows -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (sender instanceof Player player && plugin.getGuiManager() != null) {
                    plugin.getGuiManager().openGUI(player,
                            new WarSeasonGUI(plugin, player, seasonId, rows));
                } else {
                    send(sender, "war.season.header",
                            "&6── 赛季 &f{season} &6排行 ──", "{season}", seasonId);
                    int i = 1;
                    for (WarSeasonService.SeasonRow row : rows) {
                        send(sender, "war.season.row",
                                "&e#{rank} &f{name} &a{w}&7/&c{l}&7/&8{d} &7kills=&e{k}",
                                "{rank}", String.valueOf(i++),
                                "{name}", row.guildName(),
                                "{w}", String.valueOf(row.wins()),
                                "{l}", String.valueOf(row.losses()),
                                "{d}", String.valueOf(row.draws()),
                                "{k}", String.valueOf(row.kills()));
                    }
                    if (rows.isEmpty()) {
                        send(sender, "war.season.empty", "&7本赛季暂无数据");
                    }
                }
            });
        });
    }

    private void printMatch(CommandSender sender, WarMatch m) {
        send(sender, "war.status.title", "&6── 工会战 #{id} ──",
                "{id}", String.valueOf(m.id()));
        send(sender, "war.status.phase-mode", "&7阶段: &f{phase} &7模式: &f{mode}",
                "{phase}", phaseName(sender, m.phase()),
                "{mode}", m.mode().displayName(plugin, sender));
        send(sender, "war.status.teams",
                "&a{a} &7({ac}/{max}) &fvs &c{b} &7({bc}/{max})",
                "{a}", m.guildAName(),
                "{ac}", String.valueOf(m.countSide(WarTeamSide.A)),
                "{max}", String.valueOf(m.maxPerTeam()),
                "{b}", m.guildBName(),
                "{bc}", String.valueOf(m.countSide(WarTeamSide.B)));
        send(sender, "war.status.score",
                "&7比分: &a{sa} &7: &c{sb} &7预设: &f{preset}",
                "{sa}", String.valueOf(m.scoreA()),
                "{sb}", String.valueOf(m.scoreB()),
                "{preset}", m.presetName());
        if (m.phase() == WarPhase.ACTIVE || m.phase() == WarPhase.COUNTDOWN) {
            StringBuilder sb = new StringBuilder();
            for (WarParticipant p : m.participantList()) {
                if (p.isFighting()) {
                    sb.append(p.side() == WarTeamSide.A ? "&a" : "&c").append(p.name()).append(" ");
                }
            }
            send(sender, "war.status.alive", "&7存活: {list}", "{list}", sb.toString());
        }
    }

    private String phaseName(CommandSender sender, WarPhase phase) {
        return switch (phase) {
            case PENDING -> CoreMsg.raw(plugin, sender, "war.phase.pending", "等待接受");
            case SIGNUP -> CoreMsg.raw(plugin, sender, "war.phase.signup", "报名中");
            case PREPARING -> CoreMsg.raw(plugin, sender, "war.phase.preparing", "准备中");
            case COUNTDOWN -> CoreMsg.raw(plugin, sender, "war.phase.countdown", "倒计时");
            case ACTIVE -> CoreMsg.raw(plugin, sender, "war.phase.active", "激战中");
            case ENDED -> CoreMsg.raw(plugin, sender, "war.phase.ended", "已结束");
        };
    }

    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            send(sender, "war.admin-permission", "&c需要 guild.war.admin");
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("end")) {
            send(sender, "war.admin.end.usage", "&c用法: /guildwar admin end <matchId>");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            send(sender, "war.admin.end.invalid-id", "&c无效 ID");
            return;
        }
        war.forceEnd(id, "war.reason.admin-end").whenComplete((v, err) -> {
            if (err != null) {
                String body = LocalizedException.resolveThrowable(plugin, sender, err);
                String prefix = CoreMsg.raw(plugin, sender, "war.prefix", "&c[工会战] &r");
                sender.sendMessage(ColorUtils.colorize(prefix + body));
            } else {
                send(sender, "war.admin.end.ok", "&a已结束对局 #{id}",
                        "{id}", String.valueOf(id));
            }
        });
    }

    private void help(CommandSender sender) {
        send(sender, "war.help.title", "&6── 工会战帮助 ──");
        send(sender, "war.help.challenge",
                "&e/guildwar challenge <工会> [--preset] [--mode first|timed|survive] [--max] [--score] [--time]");
        send(sender, "war.help.accept-deny", "&e/guildwar accept|deny &7- 接受/拒绝挑战（官员）");
        send(sender, "war.help.join-leave", "&e/guildwar join|leave &7- 报名/退出");
        send(sender, "war.help.ready", "&e/guildwar ready &7- 报名阶段提前开局（官员，双方都 ready）");
        send(sender, "war.help.cancel", "&e/guildwar cancel &7- 取消未开战对局（官员）");
        send(sender, "war.help.status", "&e/guildwar status &7- 查看状态");
        send(sender, "war.help.report", "&e/guildwar report [id] &7- 查看战报");
        send(sender, "war.help.season", "&e/guildwar season &7- 本赛季排行");
        if (sender.hasPermission(PERM_ADMIN)) {
            send(sender, "war.help.admin", "&e/guildwar admin end <id> &7- 强制结束");
        }
    }

    private interface PlayerAction {
        void run(Player player);
    }

    private void requirePlayer(CommandSender sender, PlayerAction action) {
        if (!(sender instanceof Player player)) {
            send(sender, "war.player-only", "&c仅玩家可用");
            return;
        }
        action.run(player);
    }

    private void reply(Player player, Throwable err, String okPath, String okDef) {
        if (err != null) {
            String body = LocalizedException.resolveThrowable(plugin, player, err);
            String prefix = CoreMsg.raw(plugin, player, "war.prefix", "&c[工会战] &r");
            player.sendMessage(ColorUtils.colorize(prefix + body));
        } else {
            send(player, okPath, okDef);
        }
    }

    private static String flag(String[] args, String name) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase(name)) {
                return args[i + 1];
            }
        }
        return null;
    }

    private static Integer intFlag(String[] args, String name) {
        String v = flag(args, name);
        if (v == null) {
            return null;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void send(CommandSender sender, String path, String def, String... ph) {
        String prefix = CoreMsg.raw(plugin, sender, "war.prefix", "&c[工会战] &r");
        String body = CoreMsg.raw(plugin, sender, path, def, ph);
        sender.sendMessage(ColorUtils.colorize(prefix + body));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(Arrays.asList("challenge", "accept", "deny", "join", "leave", "ready", "cancel",
                    "status", "report", "season", "help"));
            if (sender.hasPermission(PERM_ADMIN)) {
                out.add("admin");
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("challenge")) {
            if (args.length == 2) {
                // 不枚举全部工会名（可能很多），给 flag 提示
                out.addAll(Arrays.asList("--preset", "--mode", "--max", "--score", "--time"));
            } else {
                String prev = args[args.length - 2].toLowerCase(Locale.ROOT);
                switch (prev) {
                    case "--mode" -> out.addAll(Arrays.asList("first", "timed", "survive"));
                    case "--preset" -> {
                        if (plugin.getGuildWorldService() != null) {
                            plugin.getGuildWorldService().getPresets().list()
                                    .forEach(p -> out.add(p.name()));
                        }
                    }
                    default -> {
                        if (!args[args.length - 1].startsWith("--")) {
                            out.addAll(Arrays.asList("--preset", "--mode", "--max", "--score", "--time"));
                        }
                    }
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            out.add("end");
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(last)).toList();
    }
}
