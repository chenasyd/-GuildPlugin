package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
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
 * GUI Wprowadzania Tagu Gildii
 */
public class GuildTagInputGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private String currentTag;

    public GuildTagInputGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
        this.currentTag = guild.getTag() != null ? guild.getTag() : "";
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6Zmień tag gildii");
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Wyświetl obecny tag
        displayCurrentTag(inventory);

        // Dodaj przyciski akcji
        setupButtons(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // Wprowadź tag
                handleInputTag(player);
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
     * Wyświetl obecny tag
     */
    private void displayCurrentTag(Inventory inventory) {
        ItemStack currentTagItem = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize("&eObecny tag"),
            ColorUtils.colorize("&7" + (currentTag.isEmpty() ? "Brak tagu" : "[" + currentTag + "]"))
        );
        inventory.setItem(11, currentTagItem);
    }

    /**
     * Skonfiguruj przyciski
     */
    private void setupButtons(Inventory inventory) {
        // Przycisk potwierdzenia
        ItemStack confirm = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize("&aZatwierdź zmianę"),
            ColorUtils.colorize("&7Zatwierdź zmianę tagu gildii")
        );
        inventory.setItem(15, confirm);

        // Przycisk anulowania
        ItemStack cancel = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize("&cAnuluj"),
            ColorUtils.colorize("&7Anuluj zmianę")
        );
        inventory.setItem(13, cancel);
    }

    /**
     * Obsługa wprowadzania tagu
     */
    private void handleInputTag(Player player) {
        // Zamknij GUI
        player.closeInventory();

        // Wyślij wiadomość z prośbą o wprowadzenie
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.input-tag", "&aProszę wpisać nowy tag gildii na czacie (maksymalnie 10 znaków):");
        player.sendMessage(ColorUtils.colorize(message));

        // Ustaw tryb wprowadzania dla gracza
        plugin.getGuiManager().setInputMode(player, input -> {
            if (input.length() > 10) {
                String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.tag-too-long", "&cTag jest za długi, maksymalnie 10 znaków!");
                player.sendMessage(ColorUtils.colorize(errorMessage));
                return false;
            }

            // Zaktualizuj tag
            currentTag = input;

            // Zapisz w bazie danych
            plugin.getGuildService().updateGuildAsync(guild.getId(), guild.getName(), input, guild.getDescription(), player.getUniqueId()).thenAccept(success -> {
                if (success) {
                    String successMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.tag-updated", "&aTag gildii został zaktualizowany!");
                    player.sendMessage(ColorUtils.colorize(successMessage));

                    // Bezpieczne odświeżenie GUI
                    plugin.getGuiManager().refreshGUI(player);
                } else {
                    String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.tag-update-failed", "&cAktualizacja tagu gildii nie powiodła się!");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                }
            });

            return true;
        });
    }

    /**
     * Obsługa potwierdzenia
     */
    private void handleConfirm(Player player) {
        // Powrót do GUI ustawień gildii
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
    }

    /**
     * Obsługa anulowania
     */
    private void handleCancel(Player player) {
        // Powrót do GUI ustawień gildii
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
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
