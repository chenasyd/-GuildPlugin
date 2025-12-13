package com.guild.core.gui;

import com.guild.GuildPlugin;
import com.guild.gui.GuildNameInputGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.function.Function;

import com.guild.core.utils.CompatibleScheduler;

/**
 * Menedżer GUI - zarządza wszystkimi interfejsami GUI
 */
public class GUIManager implements Listener {

    private final GuildPlugin plugin;
    private final Logger logger;
    private final Map<UUID, GUI> openGuis = new HashMap<>();
    private final Map<UUID, Function<String, Boolean>> inputModes = new HashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>(); // Zapobieganie szybkiemu klikaniu

    public GUIManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Zainicjuj menedżera GUI
     */
    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        logger.info("Menedżer GUI zainicjalizowany pomyślnie");
    }

    /**
     * Otwórz GUI
     */
    public void openGUI(Player player, GUI gui) {
        // Upewnij się, że wykonujesz w głównym wątku
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> openGUI(player, gui));
            return;
        }

        try {
            // Zamknij aktualnie otwarte GUI gracza
            closeGUI(player);

            // Utwórz nowe GUI
            Inventory inventory = Bukkit.createInventory(null, gui.getSize(), gui.getTitle());

            // Ustaw zawartość GUI
            gui.setupInventory(inventory);

            // Otwórz GUI
            player.openInventory(inventory);

            // Zarejestruj otwarte GUI
            openGuis.put(player.getUniqueId(), gui);

            logger.info("Gracz " + player.getName() + " otworzył GUI: " + gui.getClass().getSimpleName());
        } catch (Exception e) {
            logger.severe("Błąd podczas otwierania GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Zamknij GUI
     */
    public void closeGUI(Player player) {
        // Upewnij się, że wykonujesz w głównym wątku
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> closeGUI(player));
            return;
        }

        try {
            GUI gui = openGuis.remove(player.getUniqueId());
            if (gui != null) {
                // Zamknij ekwipunek
                if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null) {
                    player.closeInventory();
                }

                logger.info("Gracz " + player.getName() + " zamknął GUI: " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.severe("Błąd podczas zamykania GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Pobierz aktualnie otwarte GUI gracza
     */
    public GUI getOpenGUI(Player player) {
        return openGuis.get(player.getUniqueId());
    }

    /**
     * Sprawdź, czy gracz ma otwarte GUI
     */
    public boolean hasOpenGUI(Player player) {
        return openGuis.containsKey(player.getUniqueId());
    }

    /**
     * Obsłuż zdarzenie kliknięcia w ekwipunku
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        GUI gui = openGuis.get(player.getUniqueId());
        if (gui == null) {
            return;
        }

        // Zapobiegaj szybkiemu klikaniu
        long currentTime = System.currentTimeMillis();
        Long lastClick = lastClickTime.get(player.getUniqueId());
        if (lastClick != null && currentTime - lastClick < 200) { // 200ms anti-shake
            event.setCancelled(true);
            return;
        }
        lastClickTime.put(player.getUniqueId(), currentTime);

        try {
            // Zapobiegaj przenoszeniu przedmiotów przez gracza
            event.setCancelled(true);

            // Obsłuż kliknięcie GUI
            int slot = event.getRawSlot();
            ItemStack clickedItem = event.getCurrentItem();

            // Dodaj log debugowania
            logger.info("Gracz " + player.getName() + " kliknął GUI: " + gui.getClass().getSimpleName() + " slot: " + slot);

            // Obsłuż wszystkie kliknięcia, w tym kliknięcia pustych przedmiotów
            gui.onClick(player, slot, clickedItem, event.getClick());
        } catch (Exception e) {
            logger.severe("Błąd podczas obsługi kliknięcia GUI: " + e.getMessage());
            e.printStackTrace();
            // Zamknij GUI w przypadku błędu
            closeGUI(player);
        }
    }

    /**
     * Obsłuż zdarzenie zamknięcia ekwipunku
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        try {
            GUI gui = openGuis.remove(player.getUniqueId());
            if (gui != null) {
                // Czyść tylko jeśli gracz jest w trybie wprowadzania
                if (inputModes.containsKey(player.getUniqueId())) {
                    clearInputMode(player);
                }

                gui.onClose(player);
                logger.info("Gracz " + player.getName() + " zamknął GUI: " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.severe("Błąd podczas zamykania GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Odśwież GUI
     */
    public void refreshGUI(Player player) {
        // Upewnij się, że wykonujesz w głównym wątku
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> refreshGUI(player));
            return;
        }

        try {
            GUI gui = openGuis.get(player.getUniqueId());
            if (gui != null) {
                // Zamknij bieżące GUI
                closeGUI(player);

                // Otwórz GUI ponownie
                openGUI(player, gui);

                logger.info("Odświeżono GUI dla gracza " + player.getName() + ": " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.severe("Błąd podczas odświeżania GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Zamknij wszystkie GUI
     */
    public void closeAllGUIs() {
        // Upewnij się, że wykonujesz w głównym wątku
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, this::closeAllGUIs);
            return;
        }

        try {
            for (UUID playerUuid : openGuis.keySet()) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    closeGUI(player);
                }
            }
            openGuis.clear();
            logger.info("Zamknięto wszystkie GUI");
        } catch (Exception e) {
            logger.severe("Błąd podczas zamykania wszystkich GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Pobierz liczbę otwartych GUI
     */
    public int getOpenGUICount() {
        return openGuis.size();
    }

    /**
     * Ustaw tryb wprowadzania dla gracza
     */
    public void setInputMode(Player player, Function<String, Boolean> inputHandler) {
        // Upewnij się, że wykonujesz w głównym wątku
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> setInputMode(player, inputHandler));
            return;
        }

        try {
            inputModes.put(player.getUniqueId(), inputHandler);
            logger.info("Gracz " + player.getName() + " wszedł w tryb wprowadzania");
        } catch (Exception e) {
            logger.severe("Błąd podczas ustawiania trybu wprowadzania: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ustaw tryb wprowadzania dla gracza (z obiektem GUI)
     */
    public void setInputMode(Player player, String mode, GUI gui) {
        // Upewnij się, że wykonujesz w głównym wątku
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> setInputMode(player, mode, gui));
            return;
        }

        try {
            // Utwórz specjalny handler wejścia dla wprowadzania nazwy gildii
            if ("guild_name_input".equals(mode) && gui instanceof GuildNameInputGUI) {
                GuildNameInputGUI nameInputGUI = (GuildNameInputGUI) gui;
                inputModes.put(player.getUniqueId(), input -> {
                    if ("Anuluj".equals(input.trim()) || "anuluj".equals(input.trim()) || "cancel".equals(input.trim())) {
                        nameInputGUI.handleCancel(player);
                        return true;
                    }
                    nameInputGUI.handleInputComplete(player, input);
                    return true;
                });
                logger.info("Gracz " + player.getName() + " wszedł w tryb wprowadzania nazwy gildii");
            } else {
                logger.warning("Nieznany tryb wprowadzania: " + mode);
            }
        } catch (Exception e) {
            logger.severe("Błąd podczas ustawiania trybu wprowadzania: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Wyczyść tryb wprowadzania dla gracza
     */
    public void clearInputMode(Player player) {
        // Upewnij się, że wykonujesz w głównym wątku
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> clearInputMode(player));
            return;
        }

        try {
            inputModes.remove(player.getUniqueId());
            logger.info("Gracz " + player.getName() + " opuścił tryb wprowadzania");
        } catch (Exception e) {
            logger.severe("Błąd podczas czyszczenia trybu wprowadzania: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sprawdź, czy gracz jest w trybie wprowadzania
     */
    public boolean isInInputMode(Player player) {
        return inputModes.containsKey(player.getUniqueId());
    }

    /**
     * Obsłuż wejście gracza
     */
    public boolean handleInput(Player player, String input) {
        try {
            Function<String, Boolean> handler = inputModes.get(player.getUniqueId());
            if (handler != null) {
                boolean result = handler.apply(input);
                if (result) {
                    inputModes.remove(player.getUniqueId());
                }
                return result;
            }
            return false;
        } catch (Exception e) {
            logger.severe("Błąd podczas obsługi wejścia gracza: " + e.getMessage());
            e.printStackTrace();
            // Wyczyść tryb wprowadzania w przypadku błędu
            clearInputMode(player);
            return false;
        }
    }
}
