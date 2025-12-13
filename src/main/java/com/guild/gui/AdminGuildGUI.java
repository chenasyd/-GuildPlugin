package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.gui.SystemSettingsGUI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI Administratora Gildii
 */
public class AdminGuildGUI implements GUI {

    private final GuildPlugin plugin;

    public AdminGuildGUI(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.title", "&4Zarządzanie Gildiami"));
    }

    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("admin-gui.size", 54);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij ramkę
        fillBorder(inventory);

        // Zarządzanie listą gildii
        ItemStack guildList = createItem(
            Material.BOOKSHELF,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.guild-list.name", "&eZarządzanie Listą Gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.guild-list.lore.1", "&7Przeglądaj i zarządzaj wszystkimi gildiami")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.guild-list.lore.2", "&7W tym usuwanie, zamrażanie itp."))
        );
        inventory.setItem(20, guildList);

        // Zarządzanie ekonomią
        ItemStack economy = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.economy.name", "&eZarządzanie Ekonomią")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.economy.lore.1", "&7Zarządzaj systemem ekonomii gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.economy.lore.2", "&7Ustaw fundusze, zobacz wkłady itp."))
        );
        inventory.setItem(22, economy);

        // Zarządzanie relacjami
        ItemStack relations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.relations.name", "&eZarządzanie Relacjami")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.relations.lore.1", "&7Zarządzaj relacjami gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.relations.lore.2", "&7Sojusznicy, wrogowie, wojny itp."))
        );
        inventory.setItem(24, relations);

        // Statystyki
        ItemStack statistics = createItem(
            Material.PAPER,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.statistics.name", "&eStatystyki")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.statistics.lore.1", "&7Zobacz statystyki gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.statistics.lore.2", "&7Liczba członków, stan ekonomii itp."))
        );
        inventory.setItem(29, statistics);

        // Ustawienia systemowe
        ItemStack settings = createItem(
            Material.COMPASS,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.settings.name", "&eUstawienia Systemowe")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.settings.lore.1", "&7Zarządzaj ustawieniami systemowymi")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.settings.lore.2", "&7Przeładuj konfigurację, ustawienia uprawnień itp."))
        );
        inventory.setItem(31, settings);

        // Przycisk powrotu
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.back.name", "&cWróć")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.back.lore.1", "&7Wróć do menu głównego"))
        );
        inventory.setItem(49, back);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 20: // Zarządzanie listą gildii
                openGuildListManagement(player);
                break;
            case 22: // Zarządzanie ekonomią
                openEconomyManagement(player);
                break;
            case 24: // Zarządzanie relacjami
                openRelationManagement(player);
                break;
            case 29: // Statystyki
                openStatistics(player);
                break;
            case 31: // Ustawienia systemowe
                openSystemSettings(player);
                break;
            case 49: // Wróć
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                break;
        }
    }

    private void openGuildListManagement(Player player) {
        // Otwórz GUI zarządzania listą gildii
        GuildListManagementGUI guildListGUI = new GuildListManagementGUI(plugin, player);
        plugin.getGuiManager().openGUI(player, guildListGUI);
    }

    private void openEconomyManagement(Player player) {
        // Otwórz GUI zarządzania ekonomią
        EconomyManagementGUI economyGUI = new EconomyManagementGUI(plugin, player);
        plugin.getGuiManager().openGUI(player, economyGUI);
    }

    private void openRelationManagement(Player player) {
        // Otwórz GUI zarządzania relacjami
        RelationManagementGUI relationGUI = new RelationManagementGUI(plugin, player);
        plugin.getGuiManager().openGUI(player, relationGUI);
    }

    private void openStatistics(Player player) {
        // Wyświetl statystyki
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            player.sendMessage(ColorUtils.colorize("&6=== Statystyki Gildii ==="));
            player.sendMessage(ColorUtils.colorize("&eŁączna liczba gildii: &f" + guilds.size()));

            if (!guilds.isEmpty()) {
                final double[] totalBalance = {0};
                final int[] frozenCount = {0};

                for (Guild guild : guilds) {
                    totalBalance[0] += guild.getBalance();
                    if (guild.isFrozen()) {
                        frozenCount[0]++;
                    }
                }

                // Pobierz całkowitą liczbę członków
                CompletableFuture<Integer>[] memberCountFutures = new CompletableFuture[guilds.size()];
                for (int i = 0; i < guilds.size(); i++) {
                    memberCountFutures[i] = plugin.getGuildService().getGuildMemberCountAsync(guilds.get(i).getId());
                }

                CompletableFuture.allOf(memberCountFutures).thenRun(() -> {
                    final int[] totalMembers = {0};
                    for (CompletableFuture<Integer> future : memberCountFutures) {
                        try {
                            totalMembers[0] += future.get();
                        } catch (Exception e) {
                            plugin.getLogger().severe("Błąd podczas pobierania liczby członków: " + e.getMessage());
                        }
                    }

                    player.sendMessage(ColorUtils.colorize("&eŁączna liczba członków: &f" + totalMembers[0]));
                    player.sendMessage(ColorUtils.colorize("&eŁączne fundusze: &f" + totalBalance[0]));
                    player.sendMessage(ColorUtils.colorize("&eZamrożone gildie: &f" + frozenCount[0]));
                    player.sendMessage(ColorUtils.colorize("&eNormalne gildie: &f" + (guilds.size() - frozenCount[0])));
                });
            }
        });
    }

    private void openSystemSettings(Player player) {
        // Otwórz GUI ustawień systemowych
        plugin.getGuiManager().openGUI(player, new SystemSettingsGUI(plugin, player));
    }

    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        // Wypełnij ramkę
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }

        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);

            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);

            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public void onClose(Player player) {
        // Obsługa przy zamknięciu
    }

    @Override
    public void refresh(Player player) {
        // Odśwież GUI
    }
}
