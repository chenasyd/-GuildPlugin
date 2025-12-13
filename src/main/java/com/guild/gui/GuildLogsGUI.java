package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildLog;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI Logów Gildii
 */
public class GuildLogsGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final int page;
    private final int itemsPerPage = 28; // 2-8 kolumny, 2-5 rzędy
    private List<GuildLog> logs;
    private int totalLogs;

    public GuildLogsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this(plugin, guild, player, 0);
    }

    public GuildLogsGUI(GuildPlugin plugin, Guild guild, Player player, int page) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.page = page;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-logs.title", "&6Logi Gildii - {guild_name}")
            .replace("{guild_name}", guild.getName()));
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Asynchroniczne ładowanie logów
        loadLogsAsync().thenAccept(success -> {
            if (success) {
                // Ustaw elementy i pełną nawigację w głównym wątku
                CompatibleScheduler.runTask(plugin, () -> {
                    setupLogItems(inventory);
                    setupBasicNavigationButtons(inventory);
                    setupFullNavigationButtons(inventory);
                });
            } else {
                // W przypadku błędu, pokaż komunikat w głównym wątku
                CompatibleScheduler.runTask(plugin, () -> {
                    ItemStack errorItem = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize("&cBłąd ładowania"),
                        ColorUtils.colorize("&7Nie udało się załadować logów, spróbuj ponownie")
                    );
                    inventory.setItem(22, errorItem);
                    setupBasicNavigationButtons(inventory);
                });
            }
        });
    }

    /**
     * Asynchroniczne ładowanie logów
     */
    private CompletableFuture<Boolean> loadLogsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Rozpoczęto ładowanie logów gildii " + guild.getName() + "...");

                // Sprawdź czy ID gildii jest prawidłowe
                if (guild.getId() <= 0) {
                    plugin.getLogger().warning("Nieprawidłowe ID gildii: " + guild.getId());
                    return false;
                }

                // Pobierz całkowitą liczbę logów
                totalLogs = plugin.getGuildService().getGuildLogsCountAsync(guild.getId()).get();
                plugin.getLogger().info("Gildia " + guild.getName() + " ma " + totalLogs + " wpisów w logach");

                // Pobierz logi dla bieżącej strony
                int offset = page * itemsPerPage;
                logs = plugin.getGuildService().getGuildLogsAsync(guild.getId(), itemsPerPage, offset).get();
                plugin.getLogger().info("Pomyślnie załadowano " + logs.size() + " wpisów logów dla strony " + (page + 1));

                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("Wystąpił błąd podczas ładowania logów gildii: " + e.getMessage());
                e.printStackTrace();

                // Ustaw wartości domyślne
                totalLogs = 0;
                logs = new java.util.ArrayList<>();

                return false;
            }
        });
    }

    /**
     * Ustaw przedmioty logów
     */
    private void setupLogItems(Inventory inventory) {
        plugin.getLogger().info("Ustawianie przedmiotów logów, rozmiar logs: " + (logs != null ? logs.size() : "null"));

        if (logs == null) {
            logs = new java.util.ArrayList<>(); // Upewnij się, że logs nie jest null
        }

        if (logs.isEmpty()) {
            plugin.getLogger().info("Lista logów jest pusta, wyświetlanie informacji o braku logów");
            // Wyświetl informację o braku logów
            ItemStack noLogs = createItem(
                Material.BARRIER,
                ColorUtils.colorize("&cBrak logów"),
                ColorUtils.colorize("&7Ta gildia nie ma jeszcze żadnych wpisów"),
                ColorUtils.colorize("&7Poczekaj na aktywność gildii")
            );
            inventory.setItem(22, noLogs);
            return;
        }

        plugin.getLogger().info("Rozpoczęcie wyświetlania " + logs.size() + " wpisów logów");

        // Wyświetl listę logów
        for (int i = 0; i < Math.min(logs.size(), itemsPerPage); i++) {
            GuildLog log = logs.get(i);
            int slot = getLogSlot(i);

            plugin.getLogger().info("Ustawianie wpisu " + i + " w slocie " + slot + ": " + log.getLogType().getDisplayName());

            ItemStack logItem = createLogItem(log);
            inventory.setItem(slot, logItem);
        }
    }

    /**
     * Utwórz przedmiot logu
     */
    private ItemStack createLogItem(GuildLog log) {
        Material material = getLogMaterial(log.getLogType());
        String name = ColorUtils.colorize("&e" + log.getLogType().getDisplayName());

        List<String> lore = new java.util.ArrayList<>();
        lore.add(ColorUtils.colorize("&7Wykonawca: &f" + log.getPlayerName()));
        lore.add(ColorUtils.colorize("&7Czas: &f" + log.getSimpleTime()));
        lore.add(ColorUtils.colorize("&7Opis: &f" + log.getDescription()));

        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
            lore.add(ColorUtils.colorize("&7Szczegóły: &f" + log.getDetails()));
        }

        return createItem(material, name, lore.toArray(new String[0]));
    }

    /**
     * Pobierz materiał na podstawie typu logu
     */
    private Material getLogMaterial(GuildLog.LogType logType) {
        switch (logType) {
            case GUILD_CREATED:
                return Material.GREEN_WOOL;
            case GUILD_DISSOLVED:
                return Material.RED_WOOL;
            case MEMBER_JOINED:
                return Material.EMERALD;
            case MEMBER_LEFT:
                return Material.REDSTONE;
            case MEMBER_KICKED:
                return Material.REDSTONE;
            case MEMBER_PROMOTED:
                return Material.GOLD_INGOT;
            case MEMBER_DEMOTED:
                return Material.IRON_INGOT;
            case LEADER_TRANSFERRED:
                return Material.DIAMOND;
            case FUND_DEPOSITED:
                return Material.GOLD_NUGGET;
            case FUND_WITHDRAWN:
                return Material.IRON_NUGGET;
            case FUND_TRANSFERRED:
                return Material.EMERALD_BLOCK;
            case RELATION_CREATED:
            case RELATION_ACCEPTED:
                return Material.BLUE_WOOL;
            case RELATION_DELETED:
            case RELATION_REJECTED:
                return Material.ORANGE_WOOL;
            case GUILD_FROZEN:
                return Material.ICE;
            case GUILD_UNFROZEN:
                return Material.WATER_BUCKET;
            case GUILD_LEVEL_UP:
                return Material.EXPERIENCE_BOTTLE;
            case APPLICATION_SUBMITTED:
            case APPLICATION_ACCEPTED:
            case APPLICATION_REJECTED:
                return Material.PAPER;
            case INVITATION_SENT:
            case INVITATION_ACCEPTED:
            case INVITATION_REJECTED:
                return Material.BOOK;
            default:
                return Material.GRAY_WOOL;
        }
    }

    /**
     * Pobierz slot dla wpisu logu
     */
    private int getLogSlot(int index) {
        int row = index / 7; // 7 kolumn
        int col = index % 7;
        return (row + 1) * 9 + (col + 1); // Od 2 rzędu, 2 kolumny (sloty 10-43)
    }

    /**
     * Ustaw podstawowe przyciski nawigacyjne (niezależne od danych)
     */
    private void setupBasicNavigationButtons(Inventory inventory) {
        // Przycisk powrotu - w slocie 49, spójnie z innymi GUI
        ItemStack backButton = createItem(
            Material.ARROW,
            ColorUtils.colorize("&cPowrót"),
            ColorUtils.colorize("&7Powrót do poprzedniego menu")
        );
        inventory.setItem(49, backButton);
    }

    /**
     * Ustaw pełne przyciski nawigacyjne (zależne od danych)
     */
    private void setupFullNavigationButtons(Inventory inventory) {
        // Przyciski stron
        if (page > 0) {
            ItemStack prevButton = createItem(
                Material.ARROW,
                ColorUtils.colorize("&ePoprzednia strona"),
                ColorUtils.colorize("&7Zobacz poprzednią stronę")
            );
            inventory.setItem(45, prevButton);
        }

        if ((page + 1) * itemsPerPage < totalLogs) {
            ItemStack nextButton = createItem(
                Material.ARROW,
                ColorUtils.colorize("&eNastępna strona"),
                ColorUtils.colorize("&7Zobacz następną stronę")
            );
            inventory.setItem(53, nextButton);
        }

        // Informacja o stronie
        ItemStack pageInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize("&6Informacja o stronie"),
            ColorUtils.colorize("&7Obecna strona: &f" + (page + 1)),
            ColorUtils.colorize("&7Wszystkich stron: &f" + ((totalLogs - 1) / itemsPerPage + 1)),
            ColorUtils.colorize("&7Wszystkich wpisów: &f" + totalLogs)
        );
        inventory.setItem(47, pageInfo);

        // Przycisk odświeżania
        ItemStack refreshButton = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&aOdśwież"),
            ColorUtils.colorize("&7Odśwież listę logów")
        );
        inventory.setItem(51, refreshButton);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String itemName = clickedItem.getItemMeta().getDisplayName();

        // Przycisk powrotu
        if (itemName.contains("Powrót")) {
            // Wróć do GUI informacji o gildii
            GuildInfoGUI guildInfoGUI = new GuildInfoGUI(plugin, player, guild);
            plugin.getGuiManager().openGUI(player, guildInfoGUI);
            return;
        }

        // Poprzednia strona
        if (itemName.contains("Poprzednia strona")) {
            if (page > 0) {
                GuildLogsGUI prevPageGUI = new GuildLogsGUI(plugin, guild, player, page - 1);
                plugin.getGuiManager().openGUI(player, prevPageGUI);
            }
            return;
        }

        // Następna strona
        if (itemName.contains("Następna strona")) {
            if ((page + 1) * itemsPerPage < totalLogs) {
                GuildLogsGUI nextPageGUI = new GuildLogsGUI(plugin, guild, player, page + 1);
                plugin.getGuiManager().openGUI(player, nextPageGUI);
            }
            return;
        }

        // Przycisk odświeżania
        if (itemName.contains("Odśwież")) {
            GuildLogsGUI refreshGUI = new GuildLogsGUI(plugin, guild, player, page);
            plugin.getGuiManager().openGUI(player, refreshGUI);
            return;
        }

        // Kliknięcie w log - sprawdź czy w obszarze logów
        if (slot >= 10 && slot <= 43) {
            int row = slot / 9;
            int col = slot % 9;
            if (row >= 1 && row <= 4 && col >= 1 && col <= 7) {
                int relativeIndex = (row - 1) * 7 + (col - 1);
                int logIndex = (page * itemsPerPage) + relativeIndex;
                if (logIndex < logs.size()) {
                    GuildLog log = logs.get(logIndex);
                    handleLogClick(player, log);
                }
            }
        }
    }

    /**
     * Obsługa kliknięcia w log
     */
    private void handleLogClick(Player player, GuildLog log) {
        // Pokaż szczegóły logu
        String message = ColorUtils.colorize("&6=== Szczegóły Logu ===");
        player.sendMessage(message);
        player.sendMessage(ColorUtils.colorize("&7Typ: &f" + log.getLogType().getDisplayName()));
        player.sendMessage(ColorUtils.colorize("&7Wykonawca: &f" + log.getPlayerName()));
        player.sendMessage(ColorUtils.colorize("&7Czas: &f" + log.getSimpleTime()));
        player.sendMessage(ColorUtils.colorize("&7Opis: &f" + log.getDescription()));
        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&7Szczegóły: &f" + log.getDetails()));
        }
        player.sendMessage(ColorUtils.colorize("&6=================="));
    }

    @Override
    public void onClose(Player player) {
        // Obsługa przy zamknięciu
    }

    @Override
    public void refresh(Player player) {
        // Odśwież GUI
        GuildLogsGUI refreshGUI = new GuildLogsGUI(plugin, guild, player, page);
        plugin.getGuiManager().openGUI(player, refreshGUI);
    }

    /**
     * Wypełnij obramowanie
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }

    /**
     * Utwórz przedmiot
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(java.util.Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }

        return item;
    }
}
