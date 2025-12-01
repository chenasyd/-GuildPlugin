package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI Listy Gildii
 */
public class GuildListGUI implements GUI {

    private final GuildPlugin plugin;
    private int currentPage = 0;
    private static final int GUILDS_PER_PAGE = 28; // 4 rzędy po 7 kolumn, bez obramowania
    private String searchQuery = "";
    private String filterType = "all"; // all, name, tag

    public GuildListGUI(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    public GuildListGUI(GuildPlugin plugin, String searchQuery, String filterType) {
        this.plugin = plugin;
        this.searchQuery = searchQuery;
        this.filterType = filterType;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.title", "&6Lista gildii"));
    }

    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-list.size", 54);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Dodaj przyciski funkcyjne
        setupFunctionButtons(inventory);

        // Załaduj listę gildii
        loadGuilds(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // Sprawdź czy to przycisk funkcyjny
        if (isFunctionButton(slot)) {
            handleFunctionButton(player, slot);
            return;
        }

        // Sprawdź czy to przycisk paginacji
        if (isPaginationButton(slot)) {
            handlePaginationButton(player, slot);
            return;
        }

        // Sprawdź czy to slot gildii
        if (isGuildSlot(slot)) {
            handleGuildClick(player, slot, clickedItem, clickType);
        }
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
     * Skonfiguruj przyciski funkcyjne
     */
    private void setupFunctionButtons(Inventory inventory) {
        // Przycisk wyszukiwania
        ItemStack search = createItem(
            Material.COMPASS,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.search.name", "&eSzukaj gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.search.lore.1", "&7Wyszukaj konkretną gildię")),
            ColorUtils.colorize("&7Obecne wyszukiwanie: " + (searchQuery.isEmpty() ? "Brak" : searchQuery))
        );
        inventory.setItem(45, search);

        // Przycisk filtrowania
        ItemStack filter = createItem(
            Material.HOPPER,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.filter.name", "&eFiltrowanie")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.filter.lore.1", "&7Filtruj gildie według kryteriów")),
            ColorUtils.colorize("&7Obecny filtr: " + getFilterDisplayName())
        );
        inventory.setItem(47, filter);

        // Przycisk powrotu
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.back.name", "&7Powrót")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.back.lore.1", "&7Powrót do menu głównego"))
        );
        inventory.setItem(49, back);
    }

    /**
     * Załaduj listę gildii
     */
    private void loadGuilds(Inventory inventory) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            // Upewnij się, że aktualizacja GUI odbywa się w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                if (guilds == null || guilds.isEmpty()) {
                    // Wyświetl informację o braku gildii
                    ItemStack noGuilds = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize("&cBrak gildii"),
                        ColorUtils.colorize("&7Na serwerze nie ma jeszcze żadnych gildii")
                    );
                    inventory.setItem(22, noGuilds);
                    return;
                }

                // Zastosuj wyszukiwanie i filtrowanie
                List<Guild> filteredGuilds = filterGuilds(guilds);

                if (filteredGuilds.isEmpty()) {
                    // Wyświetl brak wyników wyszukiwania
                    ItemStack noResults = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize("&cBrak wyników"),
                        ColorUtils.colorize("&7Nie znaleziono pasujących gildii")
                    );
                    inventory.setItem(22, noResults);
                    return;
                }

                // Oblicz paginację
                int totalPages = (filteredGuilds.size() - 1) / GUILDS_PER_PAGE;
                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }

                // Skonfiguruj przyciski paginacji
                setupPaginationButtons(inventory, totalPages);

                // Wyświetl gildie na bieżącej stronie
                displayGuilds(inventory, filteredGuilds);
            });
        });
    }

    /**
     * Filtruj gildie
     */
    private List<Guild> filterGuilds(List<Guild> guilds) {
        List<Guild> filtered = new ArrayList<>();

        for (Guild guild : guilds) {
            boolean matches = true;

            // Zastosuj wyszukiwanie
            if (!searchQuery.isEmpty()) {
                switch (filterType) {
                    case "name":
                        matches = guild.getName().toLowerCase().contains(searchQuery.toLowerCase());
                        break;
                    case "tag":
                        if (guild.getTag() != null) {
                            matches = guild.getTag().toLowerCase().contains(searchQuery.toLowerCase());
                        } else {
                            matches = false;
                        }
                        break;
                    default: // all
                        matches = guild.getName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                (guild.getTag() != null && guild.getTag().toLowerCase().contains(searchQuery.toLowerCase()));
                        break;
                }
            }

            if (matches) {
                filtered.add(guild);
            }
        }

        return filtered;
    }

    /**
     * Wyświetl listę gildii
     */
    private void displayGuilds(Inventory inventory, List<Guild> guilds) {
        int startIndex = currentPage * GUILDS_PER_PAGE;
        int endIndex = Math.min(startIndex + GUILDS_PER_PAGE, guilds.size());

        // Lista asynchronicznych zadań dla wszystkich gildii
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        int slotIndex = 10; // Rozpocznij od 2 rzędu, 2 kolumny
        for (int i = startIndex; i < endIndex; i++) {
            Guild guild = guilds.get(i);
            if (slotIndex >= 44) break; // Unikaj wyjścia poza obszar wyświetlania

            final int finalSlotIndex = slotIndex;

            // Asynchronicznie pobierz liczbę członków i utwórz przedmiot
            CompletableFuture<Void> future = plugin.getGuildService().getGuildMemberCountAsync(guild.getId())
                .thenAccept(memberCount -> {
                    // Aktualizuj GUI w głównym wątku
                    CompatibleScheduler.runTask(plugin, () -> {
                        ItemStack guildItem = createGuildItemWithMemberCount(guild, memberCount);
                        inventory.setItem(finalSlotIndex, guildItem);
                    });
                });

            futures.add(future);

            slotIndex++;
            if (slotIndex % 9 == 8) { // Pomiń obramowanie
                slotIndex += 2;
            }
        }

        // Poczekaj na zakończenie wszystkich zadań asynchronicznych
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Skonfiguruj przyciski paginacji
     */
    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        // Przycisk poprzedniej strony
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.previous-page.name", "&cPoprzednia strona")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.previous-page.lore.1", "&7Zobacz poprzednią stronę"))
            );
            inventory.setItem(18, previousPage);
        }

        // Przycisk następnej strony
        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.next-page.name", "&aNastępna strona")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.next-page.lore.1", "&7Zobacz następną stronę"))
            );
            inventory.setItem(26, nextPage);
        }
    }

    /**
     * Utwórz przedmiot gildii (z liczbą członków)
     */
    private ItemStack createGuildItemWithMemberCount(Guild guild, int memberCount) {
        List<String> lore = new ArrayList<>();
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7Tag: {guild_tag}", guild, null));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7Lider: {leader_name}", guild, null));
        lore.add(ColorUtils.colorize("&7Członkowie: " + memberCount));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7Data utworzenia: {guild_created_time}", guild, null));
        lore.add("");
        lore.add(ColorUtils.colorize("&aLewy przycisk: Zobacz szczegóły"));
        lore.add(ColorUtils.colorize("&ePrawy przycisk: Aplikuj"));

        return createItem(
            Material.SHIELD,
            PlaceholderUtils.replaceGuildPlaceholders("&e{guild_name}", guild, null),
            lore.toArray(new String[0])
        );
    }

    /**
     * Utwórz przedmiot gildii (oryginalna metoda dla kompatybilności)
     */
    private ItemStack createGuildItem(Guild guild) {
        return createGuildItemWithMemberCount(guild, 0); // Użyj wartości domyślnej
    }

    /**
     * Pobierz wyświetlaną nazwę filtra
     */
    private String getFilterDisplayName() {
        switch (filterType) {
            case "name":
                return "Według nazwy";
            case "tag":
                return "Według tagu";
            default:
                return "Wszystkie";
        }
    }

    /**
     * Sprawdź czy to przycisk funkcyjny
     */
    private boolean isFunctionButton(int slot) {
        return slot == 45 || slot == 47 || slot == 49;
    }

    /**
     * Sprawdź czy to przycisk paginacji
     */
    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }

    /**
     * Sprawdź czy to slot gildii
     */
    private boolean isGuildSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8;
    }

    /**
     * Obsługa kliknięcia przycisku funkcyjnego
     */
    private void handleFunctionButton(Player player, int slot) {
        switch (slot) {
            case 45: // Szukaj
                handleSearch(player);
                break;
            case 47: // Filtruj
                handleFilter(player);
                break;
            case 49: // Powrót
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                break;
        }
    }

    /**
     * Obsługa kliknięcia przycisku paginacji
     */
    private void handlePaginationButton(Player player, int slot) {
        if (slot == 18) { // Poprzednia strona
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
        } else if (slot == 26) { // Następna strona
            currentPage++;
            refreshInventory(player);
        }
    }

    /**
     * Obsługa kliknięcia gildii
     */
    private void handleGuildClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            // Zobacz szczegóły
            handleViewGuildDetails(player, slot);
        } else if (clickType == ClickType.RIGHT) {
            // Aplikuj o dołączenie
            handleApplyToGuild(player, slot);
        }
    }

    /**
     * Obsługa wyszukiwania
     */
    private void handleSearch(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.search-dev", "&aFunkcja wyszukiwania jest w trakcie tworzenia...");
        player.sendMessage(ColorUtils.colorize(message));
    }

    /**
     * Obsługa filtrowania
     */
    private void handleFilter(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.filter-dev", "&aFunkcja filtrowania jest w trakcie tworzenia...");
        player.sendMessage(ColorUtils.colorize(message));
    }

    /**
     * Obsługa przeglądania szczegółów gildii
     */
    private void handleViewGuildDetails(Player player, int slot) {
        // Pobierz listę gildii na bieżącej stronie
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            // Upewnij się, że operacje GUI są wykonywane w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                if (guilds == null || guilds.isEmpty()) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guilds", "&cNie znaleziono gildii");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Oblicz indeks gildii na liście
                int guildIndex = currentPage * GUILDS_PER_PAGE + (slot - 10);
                if (guildIndex >= 0 && guildIndex < guilds.size()) {
                    Guild guild = guilds.get(guildIndex);

                    // Otwórz GUI informacji o gildii
                    GuildInfoGUI guildInfoGUI = new GuildInfoGUI(plugin, player, guild);
                    plugin.getGuiManager().openGUI(player, guildInfoGUI);
                }
            });
        });
    }

    /**
     * Obsługa aplikacji o dołączenie do gildii
     */
    private void handleApplyToGuild(Player player, int slot) {
        // Sprawdź czy gracz już należy do gildii
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(playerGuild -> {
            // Upewnij się, że operacje GUI są wykonywane w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                if (playerGuild != null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("create.already-in-guild", "&cJesteś już w gildii!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Pobierz listę gildii na bieżącej stronie
                plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                    // Upewnij się, że operacje GUI są wykonywane w głównym wątku
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (guilds == null || guilds.isEmpty()) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guilds", "&cNie znaleziono gildii");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        // Oblicz indeks gildii na liście
                        int guildIndex = currentPage * GUILDS_PER_PAGE + (slot - 10);
                        if (guildIndex >= 0 && guildIndex < guilds.size()) {
                            Guild guild = guilds.get(guildIndex);

                            // Sprawdź czy już wysłano aplikację
                            plugin.getGuildService().hasPendingApplicationAsync(player.getUniqueId(), guild.getId()).thenAccept(hasPending -> {
                                // Upewnij się, że operacje GUI są wykonywane w głównym wątku
                                CompatibleScheduler.runTask(plugin, () -> {
                                    if (hasPending) {
                                        String message = plugin.getConfigManager().getMessagesConfig().getString("apply.already-applied", "&cJuż aplikowałeś do tej gildii!");
                                        player.sendMessage(ColorUtils.colorize(message));
                                        return;
                                    }

                                    // Wyślij aplikację
                                    plugin.getGuildService().submitApplicationAsync(guild.getId(), player.getUniqueId(), player.getName(), "").thenAccept(success -> {
                                        // Upewnij się, że operacje GUI są wykonywane w głównym wątku
                                        CompatibleScheduler.runTask(plugin, () -> {
                                            if (success) {
                                                String message = plugin.getConfigManager().getMessagesConfig().getString("apply.success", "&aAplikacja wysłana!");
                                                player.sendMessage(ColorUtils.colorize(message));
                                            } else {
                                                String message = plugin.getConfigManager().getMessagesConfig().getString("apply.failed", "&cWysłanie aplikacji nie powiodło się!");
                                                player.sendMessage(ColorUtils.colorize(message));
                                            }
                                        });
                                    });
                                });
                            });
                        }
                    });
                });
            });
        });
    }

    /**
     * Odśwież ekwipunek
     */
    private void refreshInventory(Player player) {
        plugin.getGuiManager().refreshGUI(player);
    }

    /**
     * Utwórz przedmiot
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }

        return item;
    }
}
