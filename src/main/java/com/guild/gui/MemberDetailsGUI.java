package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI Szczegółów Członka
 */
public class MemberDetailsGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private final GuildMember member;
    private final Player viewer;

    public MemberDetailsGUI(GuildPlugin plugin, Guild guild, GuildMember member, Player viewer) {
        this.plugin = plugin;
        this.guild = guild;
        this.member = member;
        this.viewer = viewer;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-details.title", "&6Szczegóły członka")
            .replace("{member_name}", member.getPlayerName()));
    }

    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("member-details.size", 54);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Ustaw głowę członka
        setupMemberHead(inventory);

        // Ustaw podstawowe informacje
        setupBasicInfo(inventory);

        // Ustaw informacje o uprawnieniach
        setupPermissionInfo(inventory);

        // Ustaw przyciski akcji
        setupActionButtons(inventory);

        // Ustaw przycisk powrotu
        setupBackButton(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // Sprawdź czy to przycisk akcji
        if (isActionButton(slot)) {
            handleActionButton(player, slot);
            return;
        }

        // Sprawdź czy to przycisk powrotu
        if (slot == 49) {
            plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild));
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
     * Ustaw głowę członka
     */
    private void setupMemberHead(Inventory inventory) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            // Ustaw nazwę w zależności od roli
            String displayName;
            switch (member.getRole()) {
                case LEADER:
                    displayName = ColorUtils.colorize("&c" + member.getPlayerName() + " &7(Lider)");
                    break;
                case OFFICER:
                    displayName = ColorUtils.colorize("&6" + member.getPlayerName() + " &7(Oficer)");
                    break;
                default:
                    displayName = ColorUtils.colorize("&f" + member.getPlayerName() + " &7(Członek)");
                    break;
            }

            meta.setDisplayName(displayName);

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7UUID: &f" + member.getPlayerUuid()));
            lore.add(ColorUtils.colorize("&7Rola: &f" + member.getRole().getDisplayName()));

            // Formatuj datę dołączenia
            if (member.getJoinedAt() != null) {
                String joinTime = member.getJoinedAt().format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
                lore.add(ColorUtils.colorize("&7Dołączył: &f" + joinTime));
            } else {
                lore.add(ColorUtils.colorize("&7Dołączył: &fNieznana"));
            }

            meta.setLore(lore);
            head.setItemMeta(meta);
        }

        inventory.setItem(13, head);
    }

    /**
     * Ustaw podstawowe informacje
     */
    private void setupBasicInfo(Inventory inventory) {
        // Tytuł informacji podstawowych
        ItemStack infoTitle = createItem(
            Material.BOOK,
            ColorUtils.colorize("&6Podstawowe informacje"),
            ColorUtils.colorize("&7Szczegóły członka")
        );
        inventory.setItem(20, infoTitle);

        // Informacje o roli
        ItemStack roleInfo = createItem(
            Material.GOLDEN_HELMET,
            ColorUtils.colorize("&eInformacje o roli"),
            ColorUtils.colorize("&7Obecna rola: &f" + member.getRole().getDisplayName()),
            ColorUtils.colorize("&7Poziom roli: &f" + getRoleLevel(member.getRole())),
            ColorUtils.colorize("&7Status online: &f" + (isPlayerOnline(member.getPlayerUuid()) ? "&aTak" : "&cNie"))
        );
        inventory.setItem(21, roleInfo);

        // Informacje o czasie
        ItemStack timeInfo = createItem(
            Material.CLOCK,
            ColorUtils.colorize("&eInformacje czasowe"),
            ColorUtils.colorize("&7Data dołączenia: &f" + formatTime(member.getJoinedAt())),
            ColorUtils.colorize("&7Staż w gildii: &f" + getGuildDuration(member.getJoinedAt()))
        );
        inventory.setItem(22, timeInfo);

        // Informacje o wkładzie
        ItemStack contributionInfo = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&eInformacje o wkładzie"),
            ColorUtils.colorize("&7Wkład w gildię: &f" + getMemberContribution()),
            ColorUtils.colorize("&7Aktywność: &f" + getMemberActivity())
        );
        inventory.setItem(23, contributionInfo);
    }

    /**
     * Ustaw informacje o uprawnieniach
     */
    private void setupPermissionInfo(Inventory inventory) {
        // Tytuł informacji o uprawnieniach
        ItemStack permissionTitle = createItem(
            Material.SHIELD,
            ColorUtils.colorize("&6Informacje o uprawnieniach"),
            ColorUtils.colorize("&7Obecne uprawnienia")
        );
        inventory.setItem(29, permissionTitle);

        // Lista uprawnień
        List<String> permissions = getRolePermissions(member.getRole());
        ItemStack permissionList = createItem(
            Material.PAPER,
            ColorUtils.colorize("&eLista uprawnień"),
            permissions.toArray(new String[0])
        );
        inventory.setItem(30, permissionList);

        // Poziom uprawnień
        ItemStack permissionLevel = createItem(
            Material.EXPERIENCE_BOTTLE,
            ColorUtils.colorize("&ePoziom uprawnień"),
            ColorUtils.colorize("&7Obecny poziom: &f" + getPermissionLevel(member.getRole())),
            ColorUtils.colorize("&7Dostępne akcje: &f" + getExecutableActions(member.getRole()))
        );
        inventory.setItem(31, permissionLevel);
    }

    /**
     * Ustaw przyciski akcji
     */
    private void setupActionButtons(Inventory inventory) {
        // Sprawdź czy przeglądający ma uprawnienia
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), viewer.getUniqueId()).thenAccept(viewerMember -> {
            if (viewerMember == null) return;

            // Nie można modyfikować samego siebie
            if (member.getPlayerUuid().equals(viewer.getUniqueId())) {
                return;
            }

            // Nie można modyfikować lidera
            if (member.getRole() == GuildMember.Role.LEADER) {
                return;
            }

            // Przycisk wyrzucenia (wymaga uprawnień do wyrzucania)
            if (viewerMember.getRole().canKick()) {
                ItemStack kickButton = createItem(
                    Material.REDSTONE_BLOCK,
                    ColorUtils.colorize("&cWyrzuć członka"),
                    ColorUtils.colorize("&7Wyrzuć członka z gildii"),
                    ColorUtils.colorize("&7Kliknij, aby potwierdzić")
                );
                inventory.setItem(37, kickButton);
            }

            // Przycisk awansu/degradacji (tylko lider)
            if (viewerMember.getRole() == GuildMember.Role.LEADER) {
                if (member.getRole() == GuildMember.Role.OFFICER) {
                    // Przycisk degradacji
                    ItemStack demoteButton = createItem(
                        Material.IRON_INGOT,
                        ColorUtils.colorize("&7Zdegraduj członka"),
                        ColorUtils.colorize("&7Zdegraduj oficera do rangi członka"),
                        ColorUtils.colorize("&7Kliknij, aby potwierdzić")
                    );
                    inventory.setItem(39, demoteButton);
                } else {
                    // Przycisk awansu
                    ItemStack promoteButton = createItem(
                        Material.GOLD_INGOT,
                        ColorUtils.colorize("&6Awansuj członka"),
                        ColorUtils.colorize("&7Awansuj członka na oficera"),
                        ColorUtils.colorize("&7Kliknij, aby potwierdzić")
                    );
                    inventory.setItem(39, promoteButton);
                }
            }

            // Przycisk wiadomości
            ItemStack messageButton = createItem(
                Material.PAPER,
                ColorUtils.colorize("&eWyślij wiadomość"),
                ColorUtils.colorize("&7Wyślij prywatną wiadomość"),
                ColorUtils.colorize("&7Kliknij, aby otworzyć czat")
            );
            inventory.setItem(41, messageButton);
        });
    }

    /**
     * Ustaw przycisk powrotu
     */
    private void setupBackButton(Inventory inventory) {
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize("&7Powrót"),
            ColorUtils.colorize("&7Powrót do zarządzania członkami")
        );
        inventory.setItem(49, back);
    }

    /**
     * Sprawdź czy to przycisk akcji
     */
    private boolean isActionButton(int slot) {
        return slot == 37 || slot == 39 || slot == 41;
    }

    /**
     * Obsługa przycisków akcji
     */
    private void handleActionButton(Player player, int slot) {
        switch (slot) {
            case 37: // Wyrzuć członka
                handleKickMember(player);
                break;
            case 39: // Awansuj/Zdegraduj członka
                handlePromoteDemoteMember(player);
                break;
            case 41: // Wyślij wiadomość
                handleSendMessage(player);
                break;
        }
    }

    /**
     * Obsługa wyrzucenia członka
     */
    private void handleKickMember(Player player) {
        // Sprawdź uprawnienia
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || !executor.getRole().canKick()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cBrak uprawnień");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Potwierdź wyrzucenie
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-kick", "&cCzy na pewno chcesz wyrzucić członka {member}? Wpisz &f/guild kick {member} confirm &caby potwierdzić")
                .replace("{member}", member.getPlayerName());
            player.sendMessage(ColorUtils.colorize(message));
            player.closeInventory();
        });
    }

    /**
     * Obsługa awansu/degradacji członka
     */
    private void handlePromoteDemoteMember(Player player) {
        // Sprawdź uprawnienia
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || executor.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            if (member.getRole() == GuildMember.Role.OFFICER) {
                // Degradacja
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-demote", "&cCzy na pewno chcesz zdegradować członka {member}? Wpisz &f/guild demote {member} confirm &caby potwierdzić")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            } else {
                // Awans
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-promote", "&aCzy na pewno chcesz awansować członka {member} na oficera? Wpisz &f/guild promote {member} confirm &aaby potwierdzić")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            }
            player.closeInventory();
        });
    }

    /**
     * Obsługa wysyłania wiadomości
     */
    private void handleSendMessage(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.open-chat", "&eWpisz wiadomość do {member}:")
            .replace("{member}", member.getPlayerName());
        player.sendMessage(ColorUtils.colorize(message));
        player.closeInventory();

        // TODO: Zaimplementować system prywatnych wiadomości
    }

    /**
     * Pobierz poziom roli
     */
    private String getRoleLevel(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "Najwyższy";
            case OFFICER:
                return "Wysoki";
            default:
                return "Normalny";
        }
    }

    /**
     * Sprawdź czy gracz jest online
     */
    private boolean isPlayerOnline(java.util.UUID playerUuid) {
        Player player = plugin.getServer().getPlayer(playerUuid);
        return player != null && player.isOnline();
    }

    /**
     * Formatuj czas
     */
    private String formatTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "Nieznany";
        return dateTime.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }

    /**
     * Pobierz staż w gildii
     */
    private String getGuildDuration(java.time.LocalDateTime joinDateTime) {
        if (joinDateTime == null) return "Nieznany";

        java.time.LocalDateTime currentTime = java.time.LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(joinDateTime, currentTime);

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return days + " dni " + hours + " godz.";
        } else if (hours > 0) {
            return hours + " godz. " + minutes + " min.";
        } else {
            return minutes + " min.";
        }
    }

    /**
     * Pobierz wkład członka
     */
    private String getMemberContribution() {
        // TODO: Zaimplementować system statystyk wkładu
        return "Do obliczenia";
    }

    /**
     * Pobierz aktywność członka
     */
    private String getMemberActivity() {
        // TODO: Zaimplementować system statystyk aktywności
        return "Do obliczenia";
    }

    /**
     * Pobierz listę uprawnień roli
     */
    private List<String> getRolePermissions(GuildMember.Role role) {
        List<String> permissions = new ArrayList<>();

        switch (role) {
            case LEADER:
                permissions.add(ColorUtils.colorize("&7✓ Wszystkie uprawnienia"));
                permissions.add(ColorUtils.colorize("&7✓ Zapraszanie"));
                permissions.add(ColorUtils.colorize("&7✓ Wyrzucanie"));
                permissions.add(ColorUtils.colorize("&7✓ Awans/Degradacja"));
                permissions.add(ColorUtils.colorize("&7✓ Zarządzanie"));
                permissions.add(ColorUtils.colorize("&7✓ Rozwiązanie"));
                break;
            case OFFICER:
                permissions.add(ColorUtils.colorize("&7✓ Zapraszanie"));
                permissions.add(ColorUtils.colorize("&7✓ Wyrzucanie"));
                permissions.add(ColorUtils.colorize("&7✗ Awans/Degradacja"));
                permissions.add(ColorUtils.colorize("&7✗ Zarządzanie"));
                permissions.add(ColorUtils.colorize("&7✗ Rozwiązanie"));
                break;
            default:
                permissions.add(ColorUtils.colorize("&7✗ Zapraszanie"));
                permissions.add(ColorUtils.colorize("&7✗ Wyrzucanie"));
                permissions.add(ColorUtils.colorize("&7✗ Awans/Degradacja"));
                permissions.add(ColorUtils.colorize("&7✗ Zarządzanie"));
                permissions.add(ColorUtils.colorize("&7✗ Rozwiązanie"));
                break;
        }

        return permissions;
    }

    /**
     * Pobierz poziom uprawnień
     */
    private String getPermissionLevel(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "Poziom 3 (Max)";
            case OFFICER:
                return "Poziom 2";
            default:
                return "Poziom 1";
        }
    }

    /**
     * Pobierz dostępne akcje
     */
    private String getExecutableActions(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "Wszystkie akcje";
            case OFFICER:
                return "Zapraszanie, Wyrzucanie";
            default:
                return "Podstawowe akcje";
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
