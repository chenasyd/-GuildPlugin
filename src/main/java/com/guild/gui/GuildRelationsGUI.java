package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildRelation;
import org.bukkit.Bukkit;
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
 * GUI Relacji Gildii - Zarządzanie relacjami
 */
public class GuildRelationsGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private int currentPage = 0;
    private final int itemsPerPage = 28; // 28 relacji na stronę (7 kolumn x 4 rzędy)
    private List<GuildRelation> relations = new ArrayList<>();

    public GuildRelationsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations.title", "&6Relacje gildii"));
    }

    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-relations.size", 54);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Załaduj dane relacji
        loadRelations().thenAccept(relationsList -> {
            this.relations = relationsList;

            // Upewnij się, że operacje GUI są wykonywane w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                // Wyświetl listę relacji
                displayRelations(inventory);

                // Dodaj przyciski funkcyjne
                addFunctionButtons(inventory);

                // Dodaj przyciski paginacji
                addPaginationButtons(inventory);
            });
        });
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String itemName = clickedItem.getItemMeta().getDisplayName();

        // Przycisk powrotu
        if (itemName.contains("Powrót")) {
            MainGuildGUI mainGUI = new MainGuildGUI(plugin);
            plugin.getGuiManager().openGUI(player, mainGUI);
            return;
        }

        // Przycisk tworzenia relacji
        if (itemName.contains("Stwórz relację")) {
            openCreateRelationGUI(player);
            return;
        }

        // Przyciski paginacji
        if (itemName.contains("Poprzednia strona")) {
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
            return;
        }

        if (itemName.contains("Następna strona")) {
            int maxPage = (relations.size() - 1) / itemsPerPage;
            if (currentPage < maxPage) {
                currentPage++;
                refreshInventory(player);
            }
            return;
        }

        // Kliknięcie w element relacji - sprawdź czy jest w zakresie 2-8 kolumn, 2-5 rzędów
        if (slot >= 10 && slot <= 43) {
            int row = slot / 9;
            int col = slot % 9;
            if (row >= 1 && row <= 4 && col >= 1 && col <= 7) {
                int relativeIndex = (row - 1) * 7 + (col - 1);
                int relationIndex = (currentPage * itemsPerPage) + relativeIndex;
                if (relationIndex < relations.size()) {
                    GuildRelation relation = relations.get(relationIndex);
                    handleRelationClick(player, relation, clickType);
                }
            }
        }
    }

    /**
     * Załaduj dane relacji gildii
     */
    private CompletableFuture<List<GuildRelation>> loadRelations() {
        return plugin.getGuildService().getGuildRelationsAsync(guild.getId());
    }

    /**
     * Wyświetl listę relacji
     */
    private void displayRelations(Inventory inventory) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, relations.size());

        for (int i = startIndex; i < endIndex; i++) {
            GuildRelation relation = relations.get(i);
            int relativeIndex = i - startIndex;

            // Oblicz pozycję w 2-8 kolumnach, 2-5 rzędach (sloty 10-43)
            int row = (relativeIndex / 7) + 1; // 2-5 rzędy
            int col = (relativeIndex % 7) + 1; // 2-8 kolumny
            int slot = row * 9 + col;

            ItemStack relationItem = createRelationItem(relation);
            inventory.setItem(slot, relationItem);
        }
    }

    /**
     * Utwórz przedmiot wyświetlający relację
     */
    private ItemStack createRelationItem(GuildRelation relation) {
        String otherGuildName = relation.getOtherGuildName(guild.getId());
        GuildRelation.RelationType type = relation.getType();
        GuildRelation.RelationStatus status = relation.getStatus();

        Material material = getRelationMaterial(type);
        String color = type.getColor();
        String displayName = color + otherGuildName + " - " + type.getDisplayName();

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7Typ relacji: " + color + type.getDisplayName()));
        lore.add(ColorUtils.colorize("&7Status: " + getStatusColor(status) + status.getDisplayName()));
        lore.add(ColorUtils.colorize("&7Inicjator: " + relation.getInitiatorName()));
        lore.add(ColorUtils.colorize("&7Utworzono: " + formatDateTime(relation.getCreatedAt())));

        if (relation.getExpiresAt() != null) {
            lore.add(ColorUtils.colorize("&7Wygasa: " + formatDateTime(relation.getExpiresAt())));
        }

        lore.add("");

        // Dodaj wskazówki operacji w zależności od typu i statusu relacji
        if (status == GuildRelation.RelationStatus.PENDING) {
            if (relation.getInitiatorUuid().equals(player.getUniqueId())) {
                lore.add(ColorUtils.colorize("&cPrawy przycisk: Anuluj relację"));
            } else {
                lore.add(ColorUtils.colorize("&aLewy przycisk: Zaakceptuj relację"));
                lore.add(ColorUtils.colorize("&cPrawy przycisk: Odrzuć relację"));
            }
        } else if (status == GuildRelation.RelationStatus.ACTIVE) {
            if (type == GuildRelation.RelationType.TRUCE) {
                lore.add(ColorUtils.colorize("&eLewy przycisk: Zakończ rozejm"));
            } else if (type == GuildRelation.RelationType.WAR) {
                lore.add(ColorUtils.colorize("&eLewy przycisk: Zaproponuj rozejm"));
            } else {
                lore.add(ColorUtils.colorize("&cPrawy przycisk: Usuń relację"));
            }
        }

        return createItem(material, displayName, lore.toArray(new String[0]));
    }

    /**
     * Pobierz materiał odpowiadający typowi relacji
     */
    private Material getRelationMaterial(GuildRelation.RelationType type) {
        switch (type) {
            case ALLY: return Material.GREEN_WOOL;
            case ENEMY: return Material.RED_WOOL;
            case WAR: return Material.NETHERITE_SWORD;
            case TRUCE: return Material.YELLOW_WOOL;
            case NEUTRAL: return Material.GRAY_WOOL;
            default: return Material.WHITE_WOOL;
        }
    }

    /**
     * Pobierz kolor statusu
     */
    private String getStatusColor(GuildRelation.RelationStatus status) {
        switch (status) {
            case PENDING: return "&e";
            case ACTIVE: return "&a";
            case EXPIRED: return "&7";
            case CANCELLED: return "&c";
            default: return "&f";
        }
    }

    /**
     * Formatuj datę i czas
     */
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "Nieznany";
        return dateTime.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }

    /**
     * Dodaj przyciski funkcyjne
     */
    private void addFunctionButtons(Inventory inventory) {
        // Przycisk tworzenia relacji
        ItemStack createRelation = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&aStwórz relację"),
            ColorUtils.colorize("&7Stwórz nową relację gildii"),
            ColorUtils.colorize("&7Sojusznicy, wrogowie, wojna itp.")
        );
        inventory.setItem(45, createRelation);

        // Przycisk statystyk relacji
        ItemStack statistics = createItem(
            Material.BOOK,
            ColorUtils.colorize("&eStatystyki relacji"),
            ColorUtils.colorize("&7Zobacz statystyki relacji"),
            ColorUtils.colorize("&7Liczba sojuszników, wrogów itp.")
        );
        inventory.setItem(47, statistics);
    }

    /**
     * Dodaj przyciski paginacji
     */
    private void addPaginationButtons(Inventory inventory) {
        int maxPage = (relations.size() - 1) / itemsPerPage;

        // Przycisk poprzedniej strony
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&cPoprzednia strona"),
                ColorUtils.colorize("&7Zobacz poprzednią stronę")
            );
            inventory.setItem(45, previousPage);
        }

        // Przycisk następnej strony
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&aNastępna strona"),
                ColorUtils.colorize("&7Zobacz następną stronę")
            );
            inventory.setItem(53, nextPage);
        }

        // Przycisk powrotu
        ItemStack backButton = createItem(
            Material.BARRIER,
            ColorUtils.colorize("&cPowrót"),
            ColorUtils.colorize("&7Powrót do głównego menu")
        );
        inventory.setItem(49, backButton);

        // Informacja o stronie
        ItemStack pageInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize("&eStrona " + (currentPage + 1)),
            ColorUtils.colorize("&7Wszystkich stron " + (maxPage + 1)),
            ColorUtils.colorize("&7Wszystkich relacji " + relations.size())
        );
        inventory.setItem(47, pageInfo);
    }

    /**
     * Obsługa kliknięcia relacji
     */
    private void handleRelationClick(Player player, GuildRelation relation, ClickType clickType) {
        GuildRelation.RelationStatus status = relation.getStatus();
        GuildRelation.RelationType type = relation.getType();

        if (status == GuildRelation.RelationStatus.PENDING) {
            if (relation.getInitiatorUuid().equals(player.getUniqueId())) {
                // Inicjator anuluje relację
                if (clickType == ClickType.RIGHT) {
                    cancelRelation(player, relation);
                }
            } else {
                // Druga strona przetwarza relację
                if (clickType == ClickType.LEFT) {
                    acceptRelation(player, relation);
                } else if (clickType == ClickType.RIGHT) {
                    rejectRelation(player, relation);
                }
            }
        } else if (status == GuildRelation.RelationStatus.ACTIVE) {
            if (type == GuildRelation.RelationType.TRUCE) {
                if (clickType == ClickType.LEFT) {
                    endTruce(player, relation);
                }
            } else if (type == GuildRelation.RelationType.WAR) {
                if (clickType == ClickType.LEFT) {
                    proposeTruce(player, relation);
                }
            } else {
                if (clickType == ClickType.RIGHT) {
                    deleteRelation(player, relation);
                }
            }
        }
    }

    /**
     * Zaakceptuj relację
     */
    private void acceptRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.ACTIVE)
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.accept-success", "&aZaakceptowano relację z gildią {guild}!");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.accept-failed", "&cZaakceptowanie relacji nie powiodło się!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
    }

    /**
     * Odrzuć relację
     */
    private void rejectRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.CANCELLED)
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.reject-success", "&cOdrzucono relację z gildią {guild}!");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.reject-failed", "&cOdrzucenie relacji nie powiodło się!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
    }

    /**
     * Anuluj relację
     */
    private void cancelRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.CANCELLED)
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.cancel-success", "&cAnulowano relację z gildią {guild}!");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.cancel-failed", "&cAnulowanie relacji nie powiodło się!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
    }

    /**
     * Zakończ rozejm
     */
    private void endTruce(Player player, GuildRelation relation) {
        // Zakończ rozejm, zmień na relację neutralną
        GuildRelation newRelation = new GuildRelation(
            relation.getGuild1Id(), relation.getGuild2Id(),
            relation.getGuild1Name(), relation.getGuild2Name(),
            GuildRelation.RelationType.NEUTRAL, player.getUniqueId(), player.getName()
        );

        plugin.getGuildService().createGuildRelationAsync(
            newRelation.getGuild1Id(), newRelation.getGuild2Id(),
            newRelation.getGuild1Name(), newRelation.getGuild2Name(),
            newRelation.getType(), newRelation.getInitiatorUuid(), newRelation.getInitiatorName()
        ).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    // Usuń starą relację rozejmu
                    plugin.getGuildService().deleteGuildRelationAsync(relation.getId());

                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-end", "&aRozejm z gildią {guild} zakończony, relacja zmieniona na neutralną!");
                    message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                    player.sendMessage(ColorUtils.colorize(message));
                    refreshInventory(player);
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-end-failed", "&cZakończenie rozejmu nie powiodło się!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    /**
     * Zaproponuj rozejm
     */
    private void proposeTruce(Player player, GuildRelation relation) {
        // Utwórz propozycję rozejmu
        GuildRelation truceRelation = new GuildRelation(
            relation.getGuild1Id(), relation.getGuild2Id(),
            relation.getGuild1Name(), relation.getGuild2Name(),
            GuildRelation.RelationType.TRUCE, player.getUniqueId(), player.getName()
        );

        plugin.getGuildService().createGuildRelationAsync(
            truceRelation.getGuild1Id(), truceRelation.getGuild2Id(),
            truceRelation.getGuild1Name(), truceRelation.getGuild2Name(),
            truceRelation.getType(), truceRelation.getInitiatorUuid(), truceRelation.getInitiatorName()
        ).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-proposed", "&eZaproponowano rozejm gildii {guild}!");
                    message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                    player.sendMessage(ColorUtils.colorize(message));
                    refreshInventory(player);
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-propose-failed", "&cPropozycja rozejmu nie powiodła się!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    /**
     * Usuń relację
     */
    private void deleteRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().deleteGuildRelationAsync(relation.getId())
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.delete-success", "&aUsunięto relację z gildią {guild}!");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.delete-failed", "&cUsunięcie relacji nie powiodło się!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
    }

    /**
     * Otwórz GUI tworzenia relacji
     */
    private void openCreateRelationGUI(Player player) {
        CreateRelationGUI createRelationGUI = new CreateRelationGUI(plugin, guild, player);
        plugin.getGuiManager().openGUI(player, createRelationGUI);
    }

    /**
     * Odśwież ekwipunek
     */
    private void refreshInventory(Player player) {
        if (player.isOnline()) {
            plugin.getGuiManager().refreshGUI(player);
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
