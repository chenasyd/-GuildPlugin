package com.guild.listeners;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUIManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.guild.core.utils.CompatibleScheduler;

/**
 * Słuchacz zdarzeń gracza
 */
public class PlayerListener implements Listener {

    private final GuildPlugin plugin;

    public PlayerListener(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Zdarzenie dołączenia gracza do serwera
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Sprawdź status wojny gildii
        checkWarStatus(event.getPlayer());
    }

    /**
     * Sprawdź status wojny gildii i wyślij powiadomienie
     */
    private void checkWarStatus(org.bukkit.entity.Player player) {
        // Asynchronicznie sprawdź gildię gracza
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild != null) {
                // Sprawdź wszystkie relacje gildii
                plugin.getGuildService().getGuildRelationsAsync(guild.getId()).thenAccept(relations -> {
                    // Upewnij się, że wykonujesz w głównym wątku
                    CompatibleScheduler.runTask(plugin, () -> {
                        for (com.guild.models.GuildRelation relation : relations) {
                            if (relation.isWar()) {
                                String message = plugin.getConfigManager().getMessagesConfig().getString("relations.war-notification", "&4[Wojna Gildii] &cTwoja gildia jest w stanie wojny z {guild}!");
                                message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                                player.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                            }
                        }
                    });
                });
            }
        });
    }

    /**
     * Zdarzenie opuszczenia serwera przez gracza
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Wyczyść stan GUI gracza
        GUIManager guiManager = plugin.getGuiManager();
        if (guiManager != null) {
            guiManager.closeGUI(event.getPlayer());
        }
    }

    /**
     * Obsłuż zdarzenie wejścia czatu (używane w trybie wprowadzania GUI)
     */
    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        GUIManager guiManager = plugin.getGuiManager();

        if (guiManager != null && guiManager.isInInputMode(event.getPlayer())) {
            // Anuluj zdarzenie, aby zapobiec wysyłaniu wiadomości na czat
            event.setCancelled(true);

            // Przetwórz wejście - wykonaj w głównym wątku
            String input = event.getMessage();
            CompatibleScheduler.runTask(plugin, () -> {
                try {
                    guiManager.handleInput(event.getPlayer(), input);
                } catch (Exception e) {
                    plugin.getLogger().severe("Błąd podczas przetwarzania wejścia GUI: " + e.getMessage());
                    e.printStackTrace();
                    // Wyczyść tryb wprowadzania w przypadku błędu
                    guiManager.clearInputMode(event.getPlayer());
                }
            });
        }
    }
}
