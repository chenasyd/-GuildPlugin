package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.GUIUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI Informacji o Gildii
 */
public class GuildInfoGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final Guild guild;
    private Inventory inventory;

    public GuildInfoGUI(GuildPlugin plugin, Player player, Guild guild) {
        this.plugin = plugin;
        this.player = player;
        this.guild = guild;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-info.title", "&6Informacje o gildii"));
    }

    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-info.size", 54);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        this.inventory = inventory;

        // Pobierz konfigurację GUI
        ConfigurationSection config = plugin.getConfigManager().getGuiConfig().getConfigurationSection("guild-info.items");
        if (config == null) {
            setupDefaultItems();
            return;
        }

        // Ustaw skonfigurowane przedmioty
        for (String key : config.getKeys(false)) {
            ConfigurationSection itemConfig = config.getConfigurationSection(key);
            if (itemConfig != null) {
                setupConfigItem(itemConfig);
            }
        }
    }

    private void setupConfigItem(ConfigurationSection itemConfig) {
        String materialName = itemConfig.getString("material", "STONE");
        Material material = Material.valueOf(materialName.toUpperCase());
        int slot = itemConfig.getInt("slot", 0);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Ustaw nazwę
            String name = itemConfig.getString("name", "");
            if (!name.isEmpty()) {
                // Użyj GUIUtils do przetwarzania zmiennych
                GUIUtils.processGUIVariablesAsync(name, guild, player, plugin).thenAccept(processedName -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        meta.setDisplayName(processedName);

                        // Ustaw opis
                        List<String> lore = itemConfig.getStringList("lore");
                        if (!lore.isEmpty()) {
                            GUIUtils.processGUILoreAsync(lore, guild, player, plugin).thenAccept(processedLore -> {
                                CompatibleScheduler.runTask(plugin, () -> {
                                    meta.setLore(processedLore);
                                    item.setItemMeta(meta);
                                    inventory.setItem(slot, item);
                                });
                            });
                        } else {
                            item.setItemMeta(meta);
                            inventory.setItem(slot, item);
                        }
                    });
                });
            } else {
                // Jeśli brak nazwy, ustaw bezpośrednio opis
                List<String> lore = itemConfig.getStringList("lore");
                if (!lore.isEmpty()) {
                                    GUIUtils.processGUILoreAsync(lore, guild, player, plugin).thenAccept(processedLore -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        meta.setLore(processedLore);
                        item.setItemMeta(meta);
                        inventory.setItem(slot, item);
                    });
                });
                } else {
                    item.setItemMeta(meta);
                    inventory.setItem(slot, item);
                }
            }
        } else {
            inventory.setItem(slot, item);
        }
    }

    private void setupDefaultItems() {
        // Nazwa gildii
        ItemStack nameItem = createItem(Material.NAME_TAG, "§6Nazwa gildii",
            "§e" + guild.getName());
        inventory.setItem(10, nameItem);

        // Tag gildii
        if (guild.getTag() != null && !guild.getTag().isEmpty()) {
            ItemStack tagItem = createItem(Material.OAK_SIGN, "§6Tag gildii",
                "§e[" + guild.getTag() + "]");
            inventory.setItem(12, tagItem);
        }

        // Opis gildii
        if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
            ItemStack descItem = createItem(Material.BOOK, "§6Opis gildii",
                "§e" + guild.getDescription());
            inventory.setItem(14, descItem);
        }

        // Informacje o liderze
        ItemStack leaderItem = createItem(Material.GOLDEN_HELMET, "§6Lider",
            "§e" + guild.getLeaderName());
        inventory.setItem(16, leaderItem);

        // Liczba członków - użyj metody asynchronicznej
        plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(memberCount -> {
            CompatibleScheduler.runTask(plugin, () -> {
                ItemStack memberItem = createItem(Material.PLAYER_HEAD, "§6Liczba członków",
                    "§e" + memberCount + "/" + guild.getMaxMembers() + " osób");
                inventory.setItem(28, memberItem);
            });
        });

        // Poziom gildii
        ItemStack levelItem = createItem(Material.EXPERIENCE_BOTTLE, "§6Poziom gildii",
            "§ePoziom " + guild.getLevel(),
            "§7Maksymalna liczba członków: " + guild.getMaxMembers() + " osób");
        inventory.setItem(30, levelItem);

        // Fundusze gildii
        ItemStack balanceItem = createItem(Material.GOLD_INGOT, "§6Fundusze gildii",
            "§e" + plugin.getEconomyManager().format(guild.getBalance()),
            "§7Wymagane do awansu: " + getNextLevelRequirement(guild.getLevel()));
        inventory.setItem(32, balanceItem);

        // Data utworzenia (użyj formatu czasu rzeczywistego)
        String createdTime = guild.getCreatedAt() != null
            ? guild.getCreatedAt().format(com.guild.core.time.TimeProvider.FULL_FORMATTER)
            : "Nieznana";
        ItemStack timeItem = createItem(Material.CLOCK, "§6Data utworzenia", "§e" + createdTime);
        inventory.setItem(34, timeItem);

        // Status gildii
        String status = guild.isFrozen() ? "§cZamrożona" : "§aAktywna";
        ItemStack statusItem = createItem(Material.BEACON, "§6Status gildii",
            status);
        inventory.setItem(36, statusItem);

        // Przycisk powrotu
        ItemStack backItem = createItem(Material.ARROW, "§cPowrót",
            "§eKliknij, aby wrócić do głównego menu");
        inventory.setItem(49, backItem);
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

    private String replacePlaceholders(String text) {
        return PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);
    }

    private String replacePlaceholdersAsync(String text, int memberCount) {
        // Najpierw użyj PlaceholderUtils do podstawowych zmiennych
        String result = PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);

        // Następnie przetwórz zmienne dynamiczne
        return result
            .replace("{member_count}", String.valueOf(memberCount))
            .replace("{online_member_count}", String.valueOf(memberCount)); // Tymczasowo użyj całkowitej liczby członków
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) {
            // Powrót do głównego menu
            plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
        }
    }

    @Override
    public void onClose(Player player) {
        // Obsługa przy zamknięciu
    }

    @Override
    public void refresh(Player player) {
        setupInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Pobierz wymagane fundusze do następnego poziomu
     */
    private String getNextLevelRequirement(int currentLevel) {
        if (currentLevel >= 10) {
            return "Osiągnięto maksymalny poziom";
        }

        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
        }

        return plugin.getEconomyManager().format(required);
    }

    /**
     * Pobierz postęp obecnego poziomu
     */
    private String getLevelProgress(int currentLevel, double currentBalance) {
        if (currentLevel >= 10) {
            return "100%";
        }

        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
        }

        double percentage = (currentBalance / required) * 100;
        return String.format("%.1f%%", percentage);
    }
}
