package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildApplication;
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
 * GUI Zarządzania Aplikacjami
 */
public class ApplicationManagementGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private static final int APPLICATIONS_PER_PAGE = 28; // 4 wiersze po 7 kolumn, z wyłączeniem ramki
    private boolean showingHistory = false; // false=oczekujące aplikacje, true=historia aplikacji

    public ApplicationManagementGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.title", "&6Zarządzanie Aplikacjami"));
    }

    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("application-management.size", 54);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij ramkę
        fillBorder(inventory);

        // Ustaw przyciski funkcyjne
        setupFunctionButtons(inventory);

        // Załaduj listę aplikacji
        loadApplications(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // Sprawdź, czy to przycisk funkcyjny
        if (isFunctionButton(slot)) {
            handleFunctionButton(player, slot);
            return;
        }

        // Sprawdź, czy to przycisk paginacji
        if (isPaginationButton(slot)) {
            handlePaginationButton(player, slot);
            return;
        }

        // Sprawdź, czy to slot aplikacji
        if (isApplicationSlot(slot)) {
            handleApplicationClick(player, slot, clickedItem, clickType);
        }
    }

    /**
     * Wypełnij ramkę
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
     * Ustaw przyciski funkcyjne
     */
    private void setupFunctionButtons(Inventory inventory) {
        // Asynchronicznie pobierz liczbę oczekujących aplikacji
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            int pendingCount = applications != null ? applications.size() : 0;

            // Przycisk oczekujących aplikacji
            ItemStack pendingApplications = createItem(
                Material.PAPER,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.pending-applications.name", "&eOczekujące Aplikacje")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.pending-applications.lore.1", "&7Zobacz oczekujące aplikacje")),
                ColorUtils.colorize("&f" + pendingCount + " aplikacji")
            );
            inventory.setItem(20, pendingApplications);
        });

        // Przycisk historii aplikacji
        ItemStack applicationHistory = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.application-history.name", "&eHistoria Aplikacji")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.application-history.lore.1", "&7Zobacz historię aplikacji"))
        );
        inventory.setItem(24, applicationHistory);

        // Przycisk powrotu
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.back.name", "&7Wróć")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.back.lore.1", "&7Wróć do menu głównego"))
        );
        inventory.setItem(49, back);
    }

    /**
     * Załaduj listę aplikacji
     */
    private void loadApplications(Inventory inventory) {
        if (showingHistory) {
            loadApplicationHistory(inventory);
        } else {
            loadPendingApplications(inventory);
        }
    }

    /**
     * Załaduj oczekujące aplikacje
     */
    private void loadPendingApplications(Inventory inventory) {
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                // Wyświetl informację o braku aplikacji
                ItemStack noApplications = createItem(
                    Material.BARRIER,
                    ColorUtils.colorize("&aBrak oczekujących aplikacji"),
                    ColorUtils.colorize("&7Obecnie brak oczekujących aplikacji")
                );
                inventory.setItem(22, noApplications);
                return;
            }

            // Oblicz strony
            int totalPages = (applications.size() - 1) / APPLICATIONS_PER_PAGE;
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }

            // Ustaw przyciski paginacji
            setupPaginationButtons(inventory, totalPages);

            // Wyświetl aplikacje na bieżącej stronie
            displayApplications(inventory, applications);
        });
    }

    /**
     * Załaduj historię aplikacji
     */
    private void loadApplicationHistory(Inventory inventory) {
        plugin.getGuildService().getApplicationHistoryAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                // Wyświetl informację o braku historii
                ItemStack noHistory = createItem(
                    Material.BARRIER,
                    ColorUtils.colorize("&aBrak historii aplikacji"),
                    ColorUtils.colorize("&7Obecnie brak historii aplikacji")
                );
                inventory.setItem(22, noHistory);
                return;
            }

            // Oblicz strony
            int totalPages = (applications.size() - 1) / APPLICATIONS_PER_PAGE;
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }

            // Ustaw przyciski paginacji
            setupPaginationButtons(inventory, totalPages);

            // Wyświetl aplikacje na bieżącej stronie
            displayApplications(inventory, applications);
        });
    }

    /**
     * Wyświetl listę aplikacji
     */
    private void displayApplications(Inventory inventory, List<GuildApplication> applications) {
        int startIndex = currentPage * APPLICATIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + APPLICATIONS_PER_PAGE, applications.size());

        int slotIndex = 10; // Zacznij od 2 wiersza, 2 kolumny
        for (int i = startIndex; i < endIndex; i++) {
            GuildApplication application = applications.get(i);
            if (slotIndex >= 44) break; // Unikaj wyjścia poza obszar wyświetlania

            ItemStack applicationItem = createApplicationItem(application);
            inventory.setItem(slotIndex, applicationItem);

            slotIndex++;
            if (slotIndex % 9 == 8) { // Pomiń ramkę
                slotIndex += 2;
            }
        }
    }

    /**
     * Ustaw przyciski paginacji
     */
    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        // Przycisk poprzedniej strony
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.previous-page.name", "&cPoprzednia strona")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.previous-page.lore.1", "&7Zobacz poprzednią stronę"))
            );
            inventory.setItem(18, previousPage);
        }

        // Przycisk następnej strony
        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.next-page.name", "&aNastępna strona")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.next-page.lore.1", "&7Zobacz następną stronę"))
            );
            inventory.setItem(26, nextPage);
        }
    }

    /**
     * Utwórz przedmiot aplikacji
     */
    private ItemStack createApplicationItem(GuildApplication application) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();

        switch (application.getStatus()) {
            case PENDING:
                material = Material.YELLOW_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&eAplikacja {applicant_name}", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7Status: &eOczekująca"));
                lore.add(PlaceholderUtils.replaceApplicationPlaceholders("&7Czas aplikacji: {apply_time}", application.getPlayerName(), guild.getName(), application.getCreatedAt()));
                lore.add(ColorUtils.colorize("&7Wiadomość: " + application.getMessage()));
                lore.add("");
                lore.add(ColorUtils.colorize("&aLPM: Akceptuj"));
                lore.add(ColorUtils.colorize("&cPPM: Odrzuć"));
                break;
            case APPROVED:
                material = Material.GREEN_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&aAplikacja {applicant_name}", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7Status: &aZatwierdzona"));
                break;
            case REJECTED:
                material = Material.RED_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&cAplikacja {applicant_name}", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7Status: &cOdrzucona"));
                break;
            default:
                material = Material.GRAY_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&7Aplikacja {applicant_name}", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7Status: &7Nieznany"));
                break;
        }

        return createItem(material, name, lore.toArray(new String[0]));
    }

    /**
     * Sprawdź, czy to przycisk funkcyjny
     */
    private boolean isFunctionButton(int slot) {
        return slot == 20 || slot == 24 || slot == 49;
    }

    /**
     * Sprawdź, czy to przycisk paginacji
     */
    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }

    /**
     * Sprawdź, czy to slot aplikacji
     */
    private boolean isApplicationSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8;
    }

    /**
     * Obsługa kliknięcia przycisku funkcyjnego
     */
    private void handleFunctionButton(Player player, int slot) {
        switch (slot) {
            case 20: // Oczekujące aplikacje
                showingHistory = false;
                currentPage = 0;
                refreshInventory(player);
                break;
            case 24: // Historia aplikacji
                showingHistory = true;
                currentPage = 0;
                refreshInventory(player);
                break;
            case 49: // Wróć
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
     * Obsługa kliknięcia aplikacji
     */
    private void handleApplicationClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (showingHistory) {
            // Historia jest tylko do odczytu
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-history-view-only", "&7To jest historia, tylko do odczytu");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Oczekujące aplikacje można zaakceptować lub odrzucić
        if (clickType == ClickType.LEFT) {
            // Akceptuj aplikację
            handleAcceptApplication(player, slot);
        } else if (clickType == ClickType.RIGHT) {
            // Odrzuć aplikację
            handleRejectApplication(player, slot);
        }
    }

    /**
     * Obsługa akceptacji aplikacji
     */
    private void handleAcceptApplication(Player player, int slot) {
        // Pobierz listę aplikacji na bieżącej stronie
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-pending-applications", "&cBrak oczekujących aplikacji");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Oblicz indeks aplikacji
            int applicationIndex = currentPage * APPLICATIONS_PER_PAGE + (slot - 10);
            if (applicationIndex >= 0 && applicationIndex < applications.size()) {
                GuildApplication application = applications.get(applicationIndex);

                // Przetwórz aplikację
                plugin.getGuildService().processApplicationAsync(application.getId(), GuildApplication.ApplicationStatus.APPROVED, player.getUniqueId()).thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-accepted", "&aAplikacja zaakceptowana!");
                        player.sendMessage(ColorUtils.colorize(message));

                        // Odśwież GUI
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-accept-failed", "&cAkceptacja aplikacji nie powiodła się!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            }
        });
    }

    /**
     * Obsługa odrzucenia aplikacji
     */
    private void handleRejectApplication(Player player, int slot) {
        // Pobierz listę aplikacji na bieżącej stronie
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-pending-applications", "&cBrak oczekujących aplikacji");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Oblicz indeks aplikacji
            int applicationIndex = currentPage * APPLICATIONS_PER_PAGE + (slot - 10);
            if (applicationIndex >= 0 && applicationIndex < applications.size()) {
                GuildApplication application = applications.get(applicationIndex);

                // Przetwórz aplikację
                plugin.getGuildService().processApplicationAsync(application.getId(), GuildApplication.ApplicationStatus.REJECTED, player.getUniqueId()).thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-rejected", "&cAplikacja odrzucona!");
                        player.sendMessage(ColorUtils.colorize(message));

                        // Odśwież GUI
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-reject-failed", "&cOdrzucenie aplikacji nie powiodło się!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            }
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
