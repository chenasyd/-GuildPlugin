package com.guild.war;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarTeamSide;
import com.guild.world.GuildWorldService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

/**
 * 公会战战斗规则：伤害判定、击杀计分、淘汰/重生与离线处理。
 */
public final class WarCombatRules {

    private final GuildPlugin plugin;
    private final GuildWorldService worldService;
    private final WarMatchRegistry registry;
    private final WarBroadcastHelper broadcast;
    private final WarMatchLifecycle lifecycle;
    private final Supplier<WarSettings> settings;

    public WarCombatRules(GuildPlugin plugin,
                            GuildWorldService worldService,
                            WarMatchRegistry registry,
                            WarBroadcastHelper broadcast,
                            WarMatchLifecycle lifecycle,
                            Supplier<WarSettings> settings) {
        this.plugin = plugin;
        this.worldService = worldService;
        this.registry = registry;
        this.broadcast = broadcast;
        this.lifecycle = lifecycle;
        this.settings = settings;
    }

    public boolean shouldCancelDamage(Player attacker, Player victim) {
        WarMatch match = registry.getByPlayer(victim.getUniqueId());
        if (match == null) {
            return false;
        }
        WarParticipant va = match.get(victim.getUniqueId());
        if (va == null || !va.isFighting()) {
            return true;
        }
        if (match.phase() != WarPhase.ACTIVE) {
            return true;
        }
        if (attacker == null) {
            return false;
        }
        WarParticipant aa = match.get(attacker.getUniqueId());
        if (aa == null) {
            return true;
        }
        if (!aa.isFighting()) {
            return true;
        }
        if (!settings.get().friendlyFire && aa.side() == va.side()) {
            return true;
        }
        return false;
    }

    public boolean shouldKeepInventory(Player player) {
        if (!settings.get().keepInventory) {
            return false;
        }
        WarMatch match = registry.getByPlayer(player.getUniqueId());
        return match != null && (match.phase() == WarPhase.ACTIVE
                || match.phase() == WarPhase.COUNTDOWN
                || match.phase() == WarPhase.PREPARING);
    }

    public void handleKill(Player killer, Player victim) {
        WarMatch match = registry.getByPlayer(victim.getUniqueId());
        if (match == null || match.phase() != WarPhase.ACTIVE) {
            return;
        }
        WarParticipant victimP = match.get(victim.getUniqueId());
        if (victimP == null || !victimP.isFighting()) {
            return;
        }

        if (killer != null) {
            WarParticipant killerP = match.get(killer.getUniqueId());
            if (killerP != null && killerP.side() != victimP.side()) {
                killerP.addKill();
                match.addScore(killerP.side(), 1);
                broadcast.broadcastMatch(match, "war.broadcast.kill",
                        "&e{killer} &7击杀了 &c{victim} &7| &a{a} {sa} &7: &c{sb} {b}",
                        "{killer}", killer.getName(),
                        "{victim}", victim.getName(),
                        "{a}", match.guildAName(),
                        "{sa}", String.valueOf(match.scoreA()),
                        "{sb}", String.valueOf(match.scoreB()),
                        "{b}", match.guildBName());
            }
        }

        if (match.mode() == VictoryMode.LAST_STANDING) {
            eliminate(match, victimP, victim);
            checkSurviveWin(match);
            return;
        }

        if (match.mode() == VictoryMode.FIRST_TO_SCORE) {
            if (match.scoreA() >= match.scoreToWin()) {
                lifecycle.endMatch(match, match.guildAId(), "war.reason.first-score");
            } else if (match.scoreB() >= match.scoreToWin()) {
                lifecycle.endMatch(match, match.guildBId(), "war.reason.first-score");
            }
        }
    }

    public Location resolveRespawn(Player player) {
        WarMatch match = registry.getByPlayer(player.getUniqueId());
        if (match == null || match.worldName() == null) {
            return null;
        }
        if (match.phase() != WarPhase.ACTIVE && match.phase() != WarPhase.COUNTDOWN
                && match.phase() != WarPhase.PREPARING) {
            return null;
        }
        WarParticipant p = match.get(player.getUniqueId());
        if (p == null) {
            return null;
        }
        WarSettings current = settings.get();
        if (p.isEliminated() || p.isSpectating()) {
            if (!current.eliminateToSpectator) {
                Location fb = worldService.getFallbackLocation();
                return fb != null ? fb.clone() : null;
            }
            if (match.spectatorSpawn() != null) {
                return match.spectatorSpawn().clone();
            }
            return p.side() == WarTeamSide.A
                    ? (match.spawnA() != null ? match.spawnA().clone() : null)
                    : (match.spawnB() != null ? match.spawnB().clone() : null);
        }
        Location dest = p.side() == WarTeamSide.A ? match.spawnA() : match.spawnB();
        return dest != null ? dest.clone() : null;
    }

    public void afterRespawn(Player player) {
        WarMatch match = registry.getByPlayer(player.getUniqueId());
        if (match == null) {
            return;
        }
        WarParticipant p = match.get(player.getUniqueId());
        if (p == null) {
            return;
        }
        if (p.isEliminated() || p.isSpectating()) {
            CompatibleScheduler.runTask(plugin, player, () -> {
                player.setGameMode(GameMode.SPECTATOR);
            });
        }
    }

    public void handleQuit(Player player) {
        WarMatch match = registry.getByPlayer(player.getUniqueId());
        if (match == null) {
            return;
        }
        WarParticipant p = match.get(player.getUniqueId());
        if (p == null) {
            registry.unlinkPlayer(player.getUniqueId());
            return;
        }
        if (match.phase() == WarPhase.PENDING || match.phase() == WarPhase.SIGNUP) {
            match.participants().remove(player.getUniqueId());
            registry.unlinkPlayer(player.getUniqueId());
            broadcast.broadcastMatch(match, "war.broadcast.offline-removed",
                    "&e{player} &7离线，已移出报名",
                    "{player}", player.getName());
            return;
        }
        if (match.phase() == WarPhase.ACTIVE || match.phase() == WarPhase.COUNTDOWN
                || match.phase() == WarPhase.PREPARING) {
            if (match.mode() == VictoryMode.LAST_STANDING && p.isFighting()) {
                p.setEliminated(true);
                p.setAlive(false);
                broadcast.broadcastMatch(match, "war.broadcast.quit-eliminate",
                        "&e{player} &7退出，视为淘汰",
                        "{player}", player.getName());
                checkSurviveWin(match);
            }
        }
    }

    void checkSurviveWin(WarMatch match) {
        if (match.phase() != WarPhase.ACTIVE || match.mode() != VictoryMode.LAST_STANDING) {
            return;
        }
        int a = match.aliveCount(WarTeamSide.A);
        int b = match.aliveCount(WarTeamSide.B);
        if (a == 0 && b == 0) {
            lifecycle.endMatch(match, null, "war.reason.both-eliminated");
        } else if (a == 0) {
            lifecycle.endMatch(match, match.guildBId(), "war.reason.wipe");
        } else if (b == 0) {
            lifecycle.endMatch(match, match.guildAId(), "war.reason.wipe");
        }
    }

    private void eliminate(WarMatch match, WarParticipant p, Player player) {
        p.setEliminated(true);
        p.setAlive(false);
        p.setSpectating(true);
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!settings.get().eliminateToSpectator) {
            CompatibleScheduler.runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    worldService.teleportToFallbackWorld(player);
                    broadcast.msg(player, "war.eliminate.fallback", "&7你已被淘汰，已送回安全点");
                }
            }, 1L);
        } else {
            broadcast.msg(player, "war.eliminate.spectator", "&7你已被淘汰，重生后进入旁观");
        }
    }
}
