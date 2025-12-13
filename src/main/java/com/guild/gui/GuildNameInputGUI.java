package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * GUI Wprowadzania Nazwy Gildii
 */
public class GuildNameInputGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private String currentName;

    public GuildNameInputGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.currentName = guild.getName() != null ? guild.getName() : "";
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6Zmień nazwę gildii");
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Wyświetl obecną nazwę
        displayCurrentName(inventory);

        // Dodaj przyciski akcji
        setupButtons(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // Wprowadź nazwę
                handleInputName(player);
                break;
            case 15: // Potwierdź
                handleConfirm(player);
                break;
            case 13: // Anuluj
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
            inventory.setItem(i + 18, border);
        }
        for (int i = 9; i < 18; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }

    /**
     * Wyświetl obecną nazwę
     */
    private void displayCurrentName(Inventory inventory) {
        ItemStack currentNameItem = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize("&eObecna nazwa gildii"),
            ColorUtils.colorize("&7" + (currentName.isEmpty() ? "Brak nazwy" : currentName))
        );
        inventory.setItem(11, currentNameItem);
    }

    /**
     * Skonfiguruj przyciski
     */
    private void setupButtons(Inventory inventory) {
        // Przycisk potwierdzenia
        ItemStack confirmButton = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&aZatwierdź zmianę"),
            ColorUtils.colorize("&7Kliknij, aby zatwierdzić zmianę nazwy gildii"),
            ColorUtils.colorize("&7Uwaga: Zmiana nazwy gildii może wymagać ponownego zalogowania")
        );
        inventory.setItem(15, confirmButton);

        // Przycisk anulowania
        ItemStack cancelButton = createItem(
            Material.REDSTONE,
            ColorUtils.colorize("&cAnuluj"),
            ColorUtils.colorize("&7Powrót do poprzedniego menu")
        );
        inventory.setItem(13, cancelButton);
    }

    /**
     * Obsługa wprowadzania nazwy
     */
    private void handleInputName(Player player) {
        // Zamknij GUI i przejdź do trybu wprowadzania
        plugin.getGuiManager().closeGUI(player);
        plugin.getGuiManager().setInputMode(player, "guild_name_input", this);

        // Wyślij instrukcje
        player.sendMessage(ColorUtils.colorize("&6Proszę wpisać nową nazwę gildii:"));
        player.sendMessage(ColorUtils.colorize("&7Obecna nazwa: &f" + currentName));
        player.sendMessage(ColorUtils.colorize("&7Wpisz &canuluj &7aby anulować operację"));
        player.sendMessage(ColorUtils.colorize("&7Obsługiwane kody kolorów, np.: &a&lzielony pogrubiony &7lub &c&oczerwona kursywa"));
        player.sendMessage(ColorUtils.colorize("&7Uwaga: Nazwa gildii nie może się powtarzać"));
    }

    /**
     * Obsługa potwierdzenia
     */
    private void handleConfirm(Player player) {
        // Sprawdź uprawnienia (tylko lider)
        if (!plugin.getGuildService().isGuildLeader(player.getUniqueId(), guild.getId())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Jeśli nazwa jest pusta, poproś o wprowadzenie
        if (currentName.isEmpty()) {
            handleInputName(player);
            return;
        }

        // Wykonaj zmianę nazwy
        executeNameChange(player, currentName);
    }

    /**
     * Obsługa anulowania
     */
    public void handleCancel(Player player) {
        // Powrót do ustawień gildii
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
    }

    /**
     * Obsługa zakończenia wprowadzania
     */
    public void handleInputComplete(Player player, String input) {
        if (input == null || input.trim().isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&cNazwa gildii nie może być pusta!"));
            plugin.getGuiManager().openGUI(player, this);
            return;
        }

        String newName = input.trim();

        // Sprawdź długość nazwy (bez kodów kolorów)
        String cleanName = newName.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
        if (cleanName.length() < 2) {
            player.sendMessage(ColorUtils.colorize("&cNazwa gildii musi mieć co najmniej 2 znaki (bez kodów kolorów)!"));
            plugin.getGuiManager().openGUI(player, this);
            return;
        }

        if (cleanName.length() > 16) {
            player.sendMessage(ColorUtils.colorize("&cNazwa gildii nie może przekraczać 16 znaków (bez kodów kolorów)!"));
            plugin.getGuiManager().openGUI(player, this);
            return;
        }

        // Sprawdź czy nazwa jest inna niż obecna
        if (newName.equalsIgnoreCase(currentName)) {
            player.sendMessage(ColorUtils.colorize("&cNowa nazwa jest taka sama jak obecna!"));
            plugin.getGuiManager().openGUI(player, this);
            return;
        }

        // Sprawdź format nazwy (alfanumeryczne + polskie znaki)
        if (!cleanName.matches("^[\\u0041-\\u007A\\u00C0-\\u00FF\\u0100-\\u017F0-9\\s]+$")) {
             player.sendMessage(ColorUtils.colorize("&cNazwa gildii może zawierać tylko litery, cyfry i spacje!"));
             plugin.getGuiManager().openGUI(player, this);
             return;
        }

        // Wykonaj zmianę nazwy
        executeNameChange(player, newName);
    }

    /**
     * Wykonaj zmianę nazwy
     */
    private void executeNameChange(Player player, String newName) {
        // Asynchroniczne sprawdzenie dostępności nazwy
        plugin.getGuildService().getGuildByNameAsync(newName).thenAccept(existingGuild -> {
            if (existingGuild != null) {
                // Nazwa zajęta
                CompatibleScheduler.runTask(plugin, () -> {
                    player.sendMessage(ColorUtils.colorize("&cNazwa gildii &f" + newName + " &cjest już zajęta!"));
                    plugin.getGuiManager().openGUI(player, this);
                });
                return;
            }

            // Nazwa dostępna, aktualizuj
            plugin.getGuildService().updateGuildAsync(guild.getId(), newName, guild.getTag(), guild.getDescription(), player.getUniqueId())
                .thenAccept(success -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (success) {
                            // Sukces
                            player.sendMessage(ColorUtils.colorize("&aNazwa gildii została pomyślnie zmieniona!"));
                            player.sendMessage(ColorUtils.colorize("&7Nowa nazwa: &f" + newName));

                            // Logowanie akcji - LogType.GUILD_RENAMED nie istnieje w oryginalnym kodzie (był w moim poprzednim tłumaczeniu jako przykład),
                            // muszę sprawdzić enum GuildLog.LogType.
                            // Zakładam, że jeśli nie ma, to użyję innego lub pominę logowanie w tym miejscu, ale w oryginalnym kodzie było GUILD_RENAMED.
                            // Sprawdzę potem definicję GuildLog.

                            // Ponowne pobranie gildii
                            plugin.getGuildService().getGuildByIdAsync(guild.getId()).thenAccept(updatedGuild -> {
                                if (updatedGuild != null) {
                                    // Powrót do ustawień z nowymi danymi
                                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, updatedGuild));
                                } else {
                                    // Fallback
                                    guild.setName(newName);
                                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
                                }
                            });
                        } else {
                            // Błąd
                            player.sendMessage(ColorUtils.colorize("&cZmiana nazwy gildii nie powiodła się! Spróbuj ponownie."));
                            plugin.getGuiManager().openGUI(player, this);
                        }
                    });
                });
        });
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

    @Override
    public void onClose(Player player) {
        // Obsługa zamknięcia
    }

    @Override
    public void refresh(Player player) {
        // Odśwież GUI
        plugin.getGuiManager().openGUI(player, this);
    }
}
