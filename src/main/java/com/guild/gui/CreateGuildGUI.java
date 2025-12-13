package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

import com.guild.core.utils.CompatibleScheduler;

/**
 * GUI Tworzenia Gildii
 */
public class CreateGuildGUI implements GUI {

    private final GuildPlugin plugin;
    private String guildName = "";
    private String guildTag = "";
    private String guildDescription = "";

    public CreateGuildGUI(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    public CreateGuildGUI(GuildPlugin plugin, String guildName, String guildTag, String guildDescription) {
        this.plugin = plugin;
        this.guildName = guildName;
        this.guildTag = guildTag;
        this.guildDescription = guildDescription;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.title", "&6Stwórz gildię"));
    }

    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("create-guild.size", 54);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Dodaj przyciski wprowadzania danych
        setupInputButtons(inventory);

        // Dodaj przyciski akcji (potwierdź/anuluj)
        setupActionButtons(inventory);

        // Wyświetl aktualnie wprowadzone informacje
        displayCurrentInput(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 20: // Wprowadzanie nazwy gildii
                handleNameInput(player);
                break;
            case 22: // Wprowadzanie tagu gildii
                handleTagInput(player);
                break;
            case 24: // Wprowadzanie opisu gildii
                handleDescriptionInput(player);
                break;
            case 39: // Potwierdzenie utworzenia
                handleConfirmCreate(player);
                break;
            case 41: // Anulowanie
                handleCancel(player);
                break;
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
     * Skonfiguruj przyciski wprowadzania
     */
    private void setupInputButtons(Inventory inventory) {
        // Przycisk wprowadzania nazwy gildii
        ItemStack nameInput = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.name-input.name", "&eNazwa gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.name-input.lore.1", "&7Kliknij, aby wpisać nazwę gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.name-input.lore.2", "&7Długość: 3-20 znaków"))
        );
        inventory.setItem(20, nameInput);

        // Przycisk wprowadzania tagu gildii
        ItemStack tagInput = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.name", "&eTag gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.lore.1", "&7Kliknij, aby wpisać tag gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.lore.2", "&7Długość: maks. 6 znaków")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.lore.3", "&7Opcjonalne"))
        );
        inventory.setItem(22, tagInput);

        // Przycisk wprowadzania opisu gildii
        ItemStack descriptionInput = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.name", "&eOpis gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.lore.1", "&7Kliknij, aby wpisać opis gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.lore.2", "&7Długość: maks. 100 znaków")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.lore.3", "&7Opcjonalne"))
        );
        inventory.setItem(24, descriptionInput);
    }

    /**
     * Skonfiguruj przyciski akcji
     */
    private void setupActionButtons(Inventory inventory) {
        // Pobierz koszt utworzenia
        double creationCost = plugin.getConfigManager().getMainConfig().getDouble("guild.creation-cost", 1000.0);
        String costText = String.format("%.0f", creationCost);

        // Przycisk potwierdzenia utworzenia
        String confirmName = plugin.getConfigManager().getGuiConfig().getString("create-guild.items.confirm.name", "&aPotwierdź utworzenie");
        String confirmLore1 = plugin.getConfigManager().getGuiConfig().getString("create-guild.items.confirm.lore.1", "&7Potwierdź utworzenie gildii");
        String confirmLore2 = plugin.getConfigManager().getGuiConfig().getString("create-guild.items.confirm.lore.2", "&7Koszt: {cost} monet");
        String confirmLore3 = plugin.getConfigManager().getGuiConfig().getString("create-guild.items.confirm.lore.3", "&7Twórca: {player_name}");

        // Zastąp zmienne
        confirmLore2 = confirmLore2.replace("{cost}", costText);
        confirmLore3 = confirmLore3.replace("{player_name}", "Obecny gracz");

        ItemStack confirm = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(confirmName),
            ColorUtils.colorize(confirmLore1),
            ColorUtils.colorize(confirmLore2),
            ColorUtils.colorize(confirmLore3)
        );
        inventory.setItem(39, confirm);

        // Przycisk anulowania
        ItemStack cancel = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.cancel.name", "&cAnuluj")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.cancel.lore.1", "&7Anuluj tworzenie gildii"))
        );
        inventory.setItem(41, cancel);
    }

    /**
     * Wyświetl aktualnie wprowadzone informacje
     */
    private void displayCurrentInput(Inventory inventory) {
        // Obecna nazwa gildii
        String nameDisplay = guildName.isEmpty() ? "Nie ustawiono" : guildName;
        ItemStack currentName = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize("&eObecna nazwa gildii"),
            ColorUtils.colorize("&7" + nameDisplay)
        );
        inventory.setItem(11, currentName);

        // Obecny tag gildii
        String tagDisplay = guildTag.isEmpty() ? "Nie ustawiono" : "[" + guildTag + "]";
        ItemStack currentTag = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize("&eObecny tag gildii"),
            ColorUtils.colorize("&7" + tagDisplay)
        );
        inventory.setItem(13, currentTag);

        // Obecny opis gildii
        String descriptionDisplay = guildDescription.isEmpty() ? "Nie ustawiono" : guildDescription;
        ItemStack currentDescription = createItem(
            Material.BOOK,
            ColorUtils.colorize("&eObecny opis gildii"),
            ColorUtils.colorize("&7" + descriptionDisplay)
        );
        inventory.setItem(15, currentDescription);
    }

    /**
     * Obsługa wprowadzania nazwy gildii
     */
    private void handleNameInput(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.input-name", "&aProszę wpisać nazwę gildii na czacie (3-20 znaków):");
        player.sendMessage(ColorUtils.colorize(message));

        // Wymuś zamknięcie GUI, aby gracz zobaczył komunikat
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);

        // Opóźnij ustawienie trybu wprowadzania, aby upewnić się, że GUI jest całkowicie zamknięte
        CompatibleScheduler.runTaskLater(plugin, () -> {
            // Ustaw tryb wprowadzania
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() < 3) {
                    String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("create.name-too-short", "&cNazwa gildii jest za krótka! Wymagane co najmniej {min} znaków.");
                    errorMessage = errorMessage.replace("{min}", "3");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                if (input.length() > 20) {
                    String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("create.name-too-long", "&cNazwa gildii jest za długa! Maksymalnie {max} znaków.");
                    errorMessage = errorMessage.replace("{max}", "20");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                guildName = input;
                String successMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.name-set", "&aNazwa gildii została ustawiona na: {name}");
                successMessage = successMessage.replace("{name}", guildName);
                player.sendMessage(ColorUtils.colorize(successMessage));

                // Otwórz ponownie GUI z zaktualizowaną zawartością
                plugin.getGuiManager().openGUI(player, new CreateGuildGUI(plugin, guildName, guildTag, guildDescription));
                return true;
            });
        }, 2L); // Opóźnienie 2 ticki (0.1 sekundy)
    }

    /**
     * Obsługa wprowadzania tagu gildii
     */
    private void handleTagInput(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.input-tag", "&aProszę wpisać tag gildii na czacie (maks. 6 znaków, opcjonalne):");
        player.sendMessage(ColorUtils.colorize(message));

        // Wymuś zamknięcie GUI, aby gracz zobaczył komunikat
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);

        // Opóźnij ustawienie trybu wprowadzania, aby upewnić się, że GUI jest całkowicie zamknięte
        CompatibleScheduler.runTaskLater(plugin, () -> {
            // Ustaw tryb wprowadzania
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() > 6) {
                    String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("create.tag-too-long", "&cTag gildii jest za długi! Maksymalnie {max} znaków.");
                    errorMessage = errorMessage.replace("{max}", "6");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                guildTag = input;
                String successMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.tag-set", "&aTag gildii został ustawiony na: {tag}");
                successMessage = successMessage.replace("{tag}", guildTag.isEmpty() ? "brak" : guildTag);
                player.sendMessage(ColorUtils.colorize(successMessage));

                // Otwórz ponownie GUI z zaktualizowaną zawartością
                plugin.getGuiManager().openGUI(player, new CreateGuildGUI(plugin, guildName, guildTag, guildDescription));
                return true;
            });
        }, 2L); // Opóźnienie 2 ticki (0.1 sekundy)
    }

    /**
     * Obsługa wprowadzania opisu gildii
     */
    private void handleDescriptionInput(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.input-description", "&aProszę wpisać opis gildii na czacie (maks. 100 znaków, opcjonalne):");
        player.sendMessage(ColorUtils.colorize(message));

        // Wymuś zamknięcie GUI, aby gracz zobaczył komunikat
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);

        // Opóźnij ustawienie trybu wprowadzania, aby upewnić się, że GUI jest całkowicie zamknięte
        CompatibleScheduler.runTaskLater(plugin, () -> {
            // Ustaw tryb wprowadzania
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() > 100) {
                    String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("create.description-too-long", "&cOpis gildii nie może przekraczać 100 znaków!");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                guildDescription = input;
                String successMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.description-set", "&aOpis gildii został ustawiony na: {description}");
                successMessage = successMessage.replace("{description}", guildDescription.isEmpty() ? "brak" : guildDescription);
                player.sendMessage(ColorUtils.colorize(successMessage));

                // Otwórz ponownie GUI z zaktualizowaną zawartością
                plugin.getGuiManager().openGUI(player, new CreateGuildGUI(plugin, guildName, guildTag, guildDescription));
                return true;
            });
        }, 2L); // Opóźnienie 2 ticki (0.1 sekundy)
    }

    /**
     * Obsługa potwierdzenia utworzenia
     */
    private void handleConfirmCreate(Player player) {
        // Weryfikacja danych wejściowych
        if (guildName.isEmpty()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.name-required", "&cProszę najpierw wpisać nazwę gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (guildName.length() < 3) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.name-too-short", "&cNazwa gildii jest za krótka! Wymagane co najmniej {min} znaków.");
            message = message.replace("{min}", "3");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (guildName.length() > 20) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.name-too-long", "&cNazwa gildii jest za długa! Maksymalnie {max} znaków.");
            message = message.replace("{max}", "20");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (!guildTag.isEmpty() && guildTag.length() > 6) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.tag-too-long", "&cTag gildii jest za długi! Maksymalnie {max} znaków.");
            message = message.replace("{max}", "6");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (!guildDescription.isEmpty() && guildDescription.length() > 100) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.description-too-long", "&cOpis gildii nie może przekraczać 100 znaków!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź system ekonomii
        if (!plugin.getEconomyManager().isVaultAvailable()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.economy-not-available", "&cSystem ekonomii jest niedostępny, nie można utworzyć gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Pobierz koszt utworzenia
        double creationCost = plugin.getConfigManager().getMainConfig().getDouble("guild.creation-cost", 1000.0);

        // Sprawdź saldo gracza
        if (!plugin.getEconomyManager().hasBalance(player, creationCost)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.insufficient-funds", "&cMasz za mało środków! Utworzenie gildii kosztuje {cost} monet.");
            message = message.replace("{cost}", String.format("%.0f", creationCost));
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Pobierz opłatę za utworzenie
        if (!plugin.getEconomyManager().withdraw(player, creationCost)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.payment-failed", "&cPobranie opłaty za utworzenie nie powiodło się!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Utwórz gildię
        String finalTag = guildTag.isEmpty() ? null : guildTag;
        String finalDescription = guildDescription.isEmpty() ? null : guildDescription;

        plugin.getGuildService().createGuildAsync(guildName, finalTag, finalDescription, player.getUniqueId(), player.getName()).thenAccept(success -> {
            // Upewnij się, że operacje GUI są wykonywane w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    String template = plugin.getConfigManager().getMessagesConfig().getString("create.success", "&aGildia {name} została pomyślnie utworzona!");
                    // Użyj izolacji kolorów, aby uniknąć wpływu osadzonych kolorów z {name} na dalszy tekst
                    String rendered = ColorUtils.replaceWithColorIsolation(template, "{name}", guildName);
                    player.sendMessage(rendered);

                    // Zamknij GUI i wróć do głównego menu
                    plugin.getGuiManager().closeGUI(player);
                    plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                } else {
                    // Jeśli tworzenie nie powiodło się, zwróć opłatę
                    plugin.getEconomyManager().deposit(player, creationCost);
                    String refundMessage = plugin.getConfigManager().getMessagesConfig().getString("create.payment-refunded", "&eZwrócono opłatę za utworzenie w wysokości {cost} monet.");
                    refundMessage = refundMessage.replace("{cost}", String.format("%.0f", creationCost));
                    player.sendMessage(ColorUtils.colorize(refundMessage));

                    String message = plugin.getConfigManager().getMessagesConfig().getString("create.failed", "&cTworzenie gildii nie powiodło się!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    /**
     * Obsługa anulowania
     */
    private void handleCancel(Player player) {
        plugin.getGuiManager().closeGUI(player);
        plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
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
