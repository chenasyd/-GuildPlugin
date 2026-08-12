package com.guild.war.reward;

import com.guild.GuildPlugin;
import com.guild.core.economy.EconomyManager;
import com.guild.models.GuildContribution;
import com.guild.war.WarSettings;
import com.guild.war.event.WarMatchEndEvent;
import com.guild.war.model.WarParticipantSnapshot;
import com.guild.war.model.WarReportSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.logging.Level;

/** 配置驱动的内置工会战奖励（监听 {@link WarMatchEndEvent}）。 */
public final class WarRewardListener implements Listener {

    private final GuildPlugin plugin;

    public WarRewardListener(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWarEnd(WarMatchEndEvent event) {
        WarSettings settings = plugin.getGuildWarService() != null
                ? plugin.getGuildWarService().settings() : null;
        if (settings == null || !settings.rewardsEnabled) {
            return;
        }
        WarReportSnapshot snap = event.getSnapshot();
        try {
            grantPlayerVault(snap, settings);
            grantGuildBank(snap, settings);
            grantContribution(snap, settings);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GuildWar] Builtin reward failed", e);
        }
    }

    private void grantPlayerVault(WarReportSnapshot snap, WarSettings settings) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isVaultAvailable()) {
            return;
        }
        for (WarParticipantSnapshot p : snap.participants()) {
            boolean winner = snap.winnerGuildId() != null && snap.winnerGuildId() == p.guildId();
            double amount = winner ? settings.rewardWinnerVault : settings.rewardLoserVault;
            if (amount <= 0) {
                continue;
            }
            Player online = Bukkit.getPlayer(p.uuid());
            if (online != null && online.isOnline()) {
                eco.deposit(online, amount);
            }
        }
    }

    private void grantGuildBank(WarReportSnapshot snap, WarSettings settings) {
        if (snap.winnerGuildId() == null || settings.rewardWinnerGuildBank <= 0) {
            return;
        }
        int guildId = snap.winnerGuildId();
        double add = settings.rewardWinnerGuildBank;
        plugin.getGuildService().getGuildByIdAsync(guildId).thenAccept(guild -> {
            if (guild == null) {
                return;
            }
            plugin.getGuildService().updateGuildBalanceAsync(
                    guildId, guild.getBalance() + add, null, "GuildWar");
        });
    }

    private void grantContribution(WarReportSnapshot snap, WarSettings settings) {
        String tag = "War #" + (snap.reportId() != null ? snap.reportId() : snap.runtimeMatchId());
        for (WarParticipantSnapshot p : snap.participants()) {
            boolean winner = snap.winnerGuildId() != null && snap.winnerGuildId() == p.guildId();
            if (winner && settings.rewardContributionPoints > 0) {
                plugin.getGuildService().addGuildContributionAsync(
                        p.guildId(), p.uuid(), p.name(),
                        settings.rewardContributionPoints,
                        GuildContribution.ContributionType.WAR_WIN, tag);
            }
            if (p.kills() > 0 && settings.rewardContributionPerKill > 0) {
                plugin.getGuildService().addGuildContributionAsync(
                        p.guildId(), p.uuid(), p.name(),
                        p.kills() * settings.rewardContributionPerKill,
                        GuildContribution.ContributionType.WAR_KILL, tag + " kills");
            }
        }
    }
}