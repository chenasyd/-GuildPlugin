package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.gui.GUIManager;
import com.guild.core.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.guild.core.utils.CompatibleScheduler;

/**
 * Główne GUI Gildii - Sześć głównych opcji
 */
public class MainGuildGUI implements GUI {

    private final GuildPlugin plugin;

    public MainGuildGUI(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.title", "&6System Gildii"));
    }

    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("main-menu.size", 54);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Przycisk informacji o gildii
        ItemStack guildInfo = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-info.name", "&eInformacje o gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-info.lore.1", "&7Zobacz szczegółowe informacje")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-info.lore.2", "&7Zawiera podstawowe dane, statystyki itp."))
        );
        inventory.setItem(20, guildInfo);

        // Przycisk zarządzania członkami
        ItemStack memberManagement = createItem(
            Material.PLAYER_HEAD,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.member-management.name", "&eZarządzanie członkami")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.member-management.lore.1", "&7Zarządzaj członkami gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.member-management.lore.2", "&7Zapraszaj, wyrzucaj, zarządzaj uprawnieniami"))
        );
        inventory.setItem(22, memberManagement);

        // Przycisk zarządzania aplikacjami
        ItemStack applicationManagement = createItem(
            Material.PAPER,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.application-management.name", "&eZarządzanie aplikacjami")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.application-management.lore.1", "&7Rozpatrz prośby o dołączenie")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.application-management.lore.2", "&7Zobacz historię aplikacji"))
        );
        inventory.setItem(24, applicationManagement);

        // Przycisk ustawień gildii
        ItemStack guildSettings = createItem(
            Material.COMPASS,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-settings.name", "&eUstawienia gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-settings.lore.1", "&7Modyfikuj ustawienia gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-settings.lore.2", "&7Opis, tagi, uprawnienia itp."))
        );
        inventory.setItem(29, guildSettings);

        // Przycisk listy gildii
        ItemStack guildList = createItem(
            Material.BOOKSHELF,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-list.name", "&eLista gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-list.lore.1", "&7Zobacz wszystkie gildie")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-list.lore.2", "&7Funkcje wyszukiwania i filtrowania"))
        );
        inventory.setItem(31, guildList);

        // Przycisk relacji gildii
        ItemStack guildRelations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-relations.name", "&eRelacje gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-relations.lore.1", "&7Zarządzaj relacjami gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-relations.lore.2", "&7Sojusznicy, wrogowie, wojny itp."))
        );
        inventory.setItem(33, guildRelations);

        // Przycisk tworzenia gildii
        ItemStack createGuild = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.create-guild.name", "&aStwórz gildię")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.create-guild.lore.1", "&7Utwórz nową gildię")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.create-guild.lore.2", "&7Wymaga opłaty w monetach"))
        );
        inventory.setItem(4, createGuild);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 20: // Informacje o gildii
                openGuildInfoGUI(player);
                break;
            case 22: // Zarządzanie członkami
                openMemberManagementGUI(player);
                break;
            case 24: // Zarządzanie aplikacjami
                openApplicationManagementGUI(player);
                break;
            case 29: // Ustawienia gildii
                openGuildSettingsGUI(player);
                break;
            case 31: // Lista gildii
                openGuildListGUI(player);
                break;
            case 33: // Relacje gildii
                openGuildRelationsGUI(player);
                break;
            case 4: // Stwórz gildię
                openCreateGuildGUI(player);
                break;
        }
    }

    /**
     * Otwórz GUI informacji o gildii
     */
    private void openGuildInfoGUI(Player player) {
        // Sprawdź czy gracz ma gildię
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Upewnij się, że operacje GUI są wykonywane w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&cNie należysz do żadnej gildii");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Otwórz GUI informacji o gildii
                GuildInfoGUI guildInfoGUI = new GuildInfoGUI(plugin, player, guild);
                plugin.getGuiManager().openGUI(player, guildInfoGUI);
            });
        });
    }

    /**
     * Otwórz GUI zarządzania członkami
     */
    private void openMemberManagementGUI(Player player) {
        // Sprawdź czy gracz ma gildię
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Upewnij się, że operacje GUI są wykonywane w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&cNie należysz do żadnej gildii");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Otwórz GUI zarządzania członkami
                MemberManagementGUI memberManagementGUI = new MemberManagementGUI(plugin, guild);
                plugin.getGuiManager().openGUI(player, memberManagementGUI);
            });
        });
    }

    /**
     * Otwórz GUI zarządzania aplikacjami
     */
    private void openApplicationManagementGUI(Player player) {
        // Sprawdź czy gracz ma gildię
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Upewnij się, że operacje GUI są wykonywane w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&cNie należysz do żadnej gildii");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Sprawdź uprawnienia
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // Upewnij się, że operacje GUI są wykonywane w głównym wątku
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || !member.getRole().canInvite()) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cBrak uprawnień");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        // Otwórz GUI zarządzania aplikacjami
                        ApplicationManagementGUI applicationManagementGUI = new ApplicationManagementGUI(plugin, guild);
                        plugin.getGuiManager().openGUI(player, applicationManagementGUI);
                    });
                });
            });
        });
    }

    /**
     * Otwórz GUI ustawień gildii
     */
    private void openGuildSettingsGUI(Player player) {
        // Sprawdź czy gracz ma gildię
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Upewnij się, że operacje GUI są wykonywane w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&cNie należysz do żadnej gildii");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Sprawdź uprawnienia
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // Upewnij się, że operacje GUI są wykonywane w głównym wątku
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || member.getRole() != com.guild.models.GuildMember.Role.LEADER) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        // Otwórz GUI ustawień gildii
                        GuildSettingsGUI guildSettingsGUI = new GuildSettingsGUI(plugin, guild);
                        plugin.getGuiManager().openGUI(player, guildSettingsGUI);
                    });
                });
            });
        });
    }

    /**
     * Otwórz GUI listy gildii
     */
    private void openGuildListGUI(Player player) {
        // Otwórz GUI listy gildii
        GuildListGUI guildListGUI = new GuildListGUI(plugin);
        plugin.getGuiManager().openGUI(player, guildListGUI);
    }

    /**
     * Otwórz GUI relacji gildii
     */
    private void openGuildRelationsGUI(Player player) {
        // Sprawdź czy gracz ma gildię
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Upewnij się, że operacje GUI są wykonywane w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&cNie należysz do żadnej gildii");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Sprawdź uprawnienia
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // Upewnij się, że operacje GUI są wykonywane w głównym wątku
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || member.getRole() != com.guild.models.GuildMember.Role.LEADER) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może zarządzać relacjami");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        // Otwórz GUI relacji gildii
                        GuildRelationsGUI guildRelationsGUI = new GuildRelationsGUI(plugin, guild, player);
                        plugin.getGuiManager().openGUI(player, guildRelationsGUI);
                    });
                });
            });
        });
    }

    /**
     * Otwórz GUI tworzenia gildii
     */
    private void openCreateGuildGUI(Player player) {
        // Sprawdź czy gracz już ma gildię
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // Upewnij się, że operacje GUI są wykonywane w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild != null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("create.already-in-guild", "&cJesteś już w gildii!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Otwórz GUI tworzenia gildii
                CreateGuildGUI createGuildGUI = new CreateGuildGUI(plugin);
                plugin.getGuiManager().openGUI(player, createGuildGUI);
            });
        });
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
