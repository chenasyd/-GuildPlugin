package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * GUI Potwierdzenia Usunięcia Gildii
 */
public class ConfirmDeleteGuildGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;

    public ConfirmDeleteGuildGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&4Potwierdź usunięcie gildii");
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Wyświetl informacje potwierdzające
        displayConfirmInfo(inventory);

        // Skonfiguruj przyciski
        setupButtons(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // Potwierdź usunięcie
                handleConfirmDelete(player);
                break;
            case 15: // Anuluj
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
     * Wyświetl informacje potwierdzające
     */
    private void displayConfirmInfo(Inventory inventory) {
        ItemStack info = createItem(
            Material.BOOK,
            ColorUtils.colorize("&4Potwierdź usunięcie gildii"),
            ColorUtils.colorize("&7Gildia: &e" + guild.getName()),
            ColorUtils.colorize("&7Czy na pewno chcesz usunąć tę gildię?"),
            ColorUtils.colorize("&cTa operacja trwale usunie gildię!"),
            ColorUtils.colorize("&cWszyscy członkowie zostaną usunięci!"),
            ColorUtils.colorize("&cTej operacji nie można cofnąć!")
        );
        inventory.setItem(13, info);
    }

    /**
     * Skonfiguruj przyciski
     */
    private void setupButtons(Inventory inventory) {
        // Przycisk potwierdzenia usunięcia
        ItemStack confirm = createItem(
            Material.TNT,
            ColorUtils.colorize("&4Potwierdź usunięcie"),
            ColorUtils.colorize("&7Kliknij, aby potwierdzić usunięcie gildii"),
            ColorUtils.colorize("&cTej operacji nie można cofnąć!")
        );
        inventory.setItem(11, confirm);

        // Przycisk anulowania
        ItemStack cancel = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize("&aAnuluj"),
            ColorUtils.colorize("&7Anuluj usuwanie gildii")
        );
        inventory.setItem(15, cancel);
    }

    /**
     * Obsługa potwierdzenia usunięcia
     */
    private void handleConfirmDelete(Player player) {
        // Sprawdź uprawnienia (tylko lider gildii może usunąć)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getGuildId() != guild.getId() || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Usuń gildię
        plugin.getGuildService().deleteGuildAsync(guild.getId(), player.getUniqueId()).thenAccept(success -> {
            if (success) {
                String template = plugin.getConfigManager().getMessagesConfig().getString("delete.success", "&aGildia &e{guild} &azostała usunięta!");
                // Wróć do głównego wątku w celu operacji na interfejsie
                CompatibleScheduler.runTask(plugin, () -> {
                    String rendered = ColorUtils.replaceWithColorIsolation(template, "{guild}", guild.getName());
                    player.sendMessage(rendered);
                    // Użyj GUIManager, aby zapewnić bezpieczne zamykanie i otwieranie w głównym wątku
                    plugin.getGuiManager().closeGUI(player);
                    plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                });
            } else {
                String message = plugin.getConfigManager().getMessagesConfig().getString("delete.failed", "&cUsunięcie gildii nie powiodło się!");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
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
