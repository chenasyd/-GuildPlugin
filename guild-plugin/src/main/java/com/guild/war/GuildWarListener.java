package com.guild.war;

import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarPhase;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.projectiles.ProjectileSource;

/** 工会战战斗监听。 */
public final class GuildWarListener implements Listener {

    private final GuildWarService warService;

    public GuildWarListener(GuildWarService warService) {
        this.warService = warService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event);
        if (warService.shouldCancelDamage(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        WarMatch match = warService.getMatchByPlayer(victim.getUniqueId());
        if (match == null) {
            return;
        }
        WarParticipant p = match.get(victim.getUniqueId());
        if (p == null) {
            return;
        }
        if (match.phase() != WarPhase.ACTIVE
                && match.phase() != WarPhase.COUNTDOWN
                && match.phase() != WarPhase.PREPARING) {
            return;
        }

        if (warService.shouldKeepInventory(victim)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setKeepLevel(true);
        }

        Player killer = victim.getKiller();
        warService.handleKill(killer, victim);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Location dest = warService.resolveRespawn(event.getPlayer());
        if (dest != null) {
            event.setRespawnLocation(dest);
            warService.afterRespawn(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        warService.handleQuit(event.getPlayer());
    }

    private static Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) {
            return p;
        }
        if (event.getDamager() instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) {
                return p;
            }
        }
        return null;
    }
}
