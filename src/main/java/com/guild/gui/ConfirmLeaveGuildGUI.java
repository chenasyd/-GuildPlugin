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

/**
 * GUI Potwierdzenia Opuszczenia Gildii
 */
public class ConfirmLeaveGuildGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;

    public ConfirmLeaveGuildGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&cPotwierdź opuszczenie gildii");
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
            case 11: // Potwierdź opuszczenie
                handleConfirmLeave(player);
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
            ColorUtils.colorize("&cPotwierdź opuszczenie gildii"),
            ColorUtils.colorize("&7Gildia: &e" + guild.getName()),
            ColorUtils.colorize("&7Czy na pewno chcesz opuścić tę gildię?"),
            ColorUtils.colorize("&cTej operacji nie można cofnąć!")
        );
        inventory.setItem(13, info);
    }

    /**
     * Skonfiguruj przyciski
     */
    private void setupButtons(Inventory inventory) {
        // Przycisk potwierdzenia opuszczenia
        ItemStack confirm = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize("&cPotwierdź opuszczenie"),
            ColorUtils.colorize("&7Kliknij, aby potwierdzić opuszczenie gildii")
        );
        inventory.setItem(11, confirm);

        // Przycisk anulowania
        ItemStack cancel = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize("&aAnuluj"),
            ColorUtils.colorize("&7Anuluj opuszczenie gildii")
        );
        inventory.setItem(15, cancel);
    }

    /**
     * Obsługa potwierdzenia opuszczenia
     */
    private void handleConfirmLeave(Player player) {
        // Sprawdź, czy gracz jest liderem
        if (player.getUniqueId().equals(guild.getLeaderUuid())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("leave.leader-cannot-leave", "&cLider gildii nie może opuścić gildii! Proszę najpierw przekazać przywództwo lub usunąć gildię.");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Opuść gildię
        plugin.getGuildService().removeGuildMemberAsync(player.getUniqueId(), player.getUniqueId()).thenAccept(success -> {
            if (success) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("leave.success", "&aPomyślnie opuściłeś gildię &e{guild} &a!")
                    .replace("{guild}", guild.getName());
                player.sendMessage(ColorUtils.colorize(message));

                // Zamknij GUI
                player.closeInventory();
            } else {
                String message = plugin.getConfigManager().getMessagesConfig().getString("leave.failed", "&cOpuszczenie gildii nie powiodło się!");
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
