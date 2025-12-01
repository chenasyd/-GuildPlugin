package com.guild.listeners;

import com.guild.GuildPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

/**
 * Słuchacz zdarzeń gildii
 */
public class GuildListener implements Listener {
    
    private final GuildPlugin plugin;
    
    public GuildListener(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Zdarzenie czatu gracza (może być użyte do funkcji czatu gildii)
     */
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        // Tutaj można dodać funkcję czatu gildii
        // Na przykład wykrywanie prefiksu gildii, obsługa czatu gildii itp.
    }
}
