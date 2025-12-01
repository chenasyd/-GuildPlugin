package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * GUI Ustawień Gildii
 */
public class GuildSettingsGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;

    public GuildSettingsGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.title", "&6Ustawienia gildii - {guild_name}")
            .replace("{guild_name}", guild.getName() != null ? guild.getName() : "Nieznana gildia"));
    }

    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-settings.size", 54);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Dodaj przyciski ustawień
        setupSettingsButtons(inventory);

        // Wyświetl aktualne ustawienia
        displayCurrentSettings(inventory);

        // Dodaj przyciski funkcji
        setupFunctionButtons(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 10: // Zmień nazwę
                handleChangeName(player);
                break;
            case 11: // Zmień opis
                handleChangeDescription(player);
                break;
            case 12: // Zmień tag
                handleChangeTag(player);
                break;
            case 14: // Ustaw dom gildii
                handleSetHome(player);
                break;
            case 16: // Ustawienia uprawnień
                handlePermissions(player);
                break;
            case 20: // Zaproś członka
                handleInviteMember(player);
                break;
            case 22: // Wyrzuć członka
                handleKickMember(player);
                break;
            case 24: // Awansuj członka
                handlePromoteMember(player);
                break;
            case 26: // Zdegraduj członka
                handleDemoteMember(player);
                break;
            case 30: // Zarządzanie aplikacjami
                handleApplications(player);
                break;
            case 31: // Zarządzanie relacjami
                handleRelations(player);
                break;
            case 32: // Logi gildii
                handleGuildLogs(player);
                break;
            case 33: // Teleport do domu gildii
                handleHomeTeleport(player);
                break;
            case 34: // Opuść gildię
                handleLeaveGuild(player);
                break;
            case 36: // Usuń gildię
                handleDeleteGuild(player);
                break;
            case 49: // Powrót
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
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
     * Skonfiguruj przyciski ustawień
     */
    private void setupSettingsButtons(Inventory inventory) {
        // Przycisk zmiany nazwy
        ItemStack changeName = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-name.name", "&eZmień nazwę")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-name.lore.1", "&7Zmień nazwę gildii"))
        );
        inventory.setItem(10, changeName);

        // Przycisk zmiany opisu
        ItemStack changeDescription = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-description.name", "&eZmień opis")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-description.lore.1", "&7Zmień opis gildii"))
        );
        inventory.setItem(11, changeDescription);

        // Przycisk zmiany tagu
        ItemStack changeTag = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-tag.name", "&eZmień tag")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-tag.lore.1", "&7Zmień tag gildii"))
        );
        inventory.setItem(12, changeTag);

        // Przycisk ustawienia domu gildii
        ItemStack setHome = createItem(
            Material.COMPASS,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.set-home.name", "&eUstaw dom gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.set-home.lore.1", "&7Ustaw punkt teleportacji gildii"))
        );
        inventory.setItem(14, setHome);

        // Przycisk ustawień uprawnień
        ItemStack permissions = createItem(
            Material.SHIELD,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.permissions.name", "&eUstawienia uprawnień")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.permissions.lore.1", "&7Zarządzaj uprawnieniami członków"))
        );
        inventory.setItem(16, permissions);
    }

    /**
     * Skonfiguruj przyciski funkcji
     */
    private void setupFunctionButtons(Inventory inventory) {
        // Przycisk zaproszenia członka
        ItemStack inviteMember = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.invite-member", "&aZaproś członka")),
            ColorUtils.colorize("&7Zaproś nowego członka do gildii")
        );
        inventory.setItem(20, inviteMember);

        // Przycisk wyrzucenia członka
        ItemStack kickMember = createItem(
            Material.REDSTONE,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.kick-member", "&cWyrzuć członka")),
            ColorUtils.colorize("&7Wyrzuć członka z gildii")
        );
        inventory.setItem(22, kickMember);

        // Przycisk awansu członka
        ItemStack promoteMember = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.promote-member", "&6Awansuj członka")),
            ColorUtils.colorize("&7Awansuj członka na wyższą rangę")
        );
        inventory.setItem(24, promoteMember);

        // Przycisk degradacji członka
        ItemStack demoteMember = createItem(
            Material.IRON_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.demote-member", "&7Zdegraduj członka")),
            ColorUtils.colorize("&7Zdegraduj członka na niższą rangę")
        );
        inventory.setItem(26, demoteMember);

        // Przycisk zarządzania aplikacjami
        ItemStack applications = createItem(
            Material.PAPER,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.application-management", "&eZarządzanie aplikacjami")),
            ColorUtils.colorize("&7Rozpatrz prośby o dołączenie")
        );
        inventory.setItem(30, applications);

        // Przycisk zarządzania relacjami
        ItemStack relations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.name", "&eZarządzanie relacjami")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.lore.1", "&7Zarządzaj relacjami gildii")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.lore.2", "&7Sojusznicy, wrogowie, itp."))
        );
        inventory.setItem(31, relations);

        // Przycisk logów gildii
        ItemStack guildLogs = createItem(
            Material.BOOK,
            ColorUtils.colorize("&6Logi gildii"),
            ColorUtils.colorize("&7Zobacz historię operacji gildii"),
            ColorUtils.colorize("&7Rejestr wszystkich ważnych działań")
        );
        inventory.setItem(32, guildLogs);

        // Przycisk teleportacji do domu gildii
        ItemStack homeTeleport = createItem(
            Material.ENDER_PEARL,
            ColorUtils.colorize("&bTeleport do domu gildii"),
            ColorUtils.colorize("&7Przenieś się do ustawionego domu gildii")
        );
        inventory.setItem(33, homeTeleport);

        // Przycisk opuszczenia gildii
        ItemStack leaveGuild = createItem(
            Material.BARRIER,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leave-guild", "&cOpuść gildię")),
            ColorUtils.colorize("&7Opuść obecną gildię")
        );
        inventory.setItem(34, leaveGuild);

        // Przycisk usunięcia gildii
        ItemStack deleteGuild = createItem(
            Material.TNT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.delete-guild", "&4Usuń gildię")),
            ColorUtils.colorize("&7Usuń całą gildię"),
            ColorUtils.colorize("&cTej operacji nie można cofnąć!")
        );
        inventory.setItem(36, deleteGuild);

        // Przycisk powrotu
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.back.name", "&7Powrót")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.back.lore.1", "&7Powrót do menu głównego"))
        );
        inventory.setItem(49, back);
    }

    /**
     * Wyświetl aktualne ustawienia
     */
    private void displayCurrentSettings(Inventory inventory) {
        // Aktualna nazwa
        ItemStack currentName = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize("&eObecna nazwa"),
            ColorUtils.colorize("&7" + (guild.getName() != null ? guild.getName() : "Brak nazwy"))
        );
        inventory.setItem(10, currentName);

        // Aktualny opis
        ItemStack currentDescription = createItem(
            Material.BOOK,
            ColorUtils.colorize("&eObecny opis"),
            ColorUtils.colorize("&7" + (guild.getDescription() != null ? guild.getDescription() : "Brak opisu"))
        );
        inventory.setItem(11, currentDescription);

        // Aktualny tag
        ItemStack currentTag = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize("&eObecny tag"),
            ColorUtils.colorize("&7" + (guild.getTag() != null ? "[" + guild.getTag() + "]" : "Brak tagu"))
        );
        inventory.setItem(13, currentTag);

        // Status domu gildii
        String homeStatus = guild.hasHome() ? "&aUstawiony" : "&cNieustawiony";
        ItemStack currentHome = createItem(
            Material.COMPASS,
            ColorUtils.colorize("&eStatus domu gildii"),
            ColorUtils.colorize("&7Status: " + homeStatus)
        );
        inventory.setItem(15, currentHome);

        // Aktualne ustawienia uprawnień
        ItemStack currentPermissions = createItem(
            Material.SHIELD,
            ColorUtils.colorize("&eObecne uprawnienia"),
            ColorUtils.colorize("&7Lider: Wszystkie uprawnienia"),
            ColorUtils.colorize("&7Oficer: Zapraszanie, Wyrzucanie"),
            ColorUtils.colorize("&7Członek: Podstawowe uprawnienia")
        );
        inventory.setItem(17, currentPermissions);
    }

    /**
     * Obsługa zmiany nazwy
     */
    private void handleChangeName(Player player) {
        // Sprawdź uprawnienia (tylko lider może zmienić nazwę)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI wprowadzania nazwy
        plugin.getGuiManager().openGUI(player, new GuildNameInputGUI(plugin, guild, player));
    }

    /**
     * Obsługa zmiany opisu
     */
    private void handleChangeDescription(Player player) {
        // Sprawdź uprawnienia (tylko lider może zmienić opis)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI wprowadzania opisu
        plugin.getGuiManager().openGUI(player, new GuildDescriptionInputGUI(plugin, guild));
    }

    /**
     * Obsługa zmiany tagu
     */
    private void handleChangeTag(Player player) {
        // Sprawdź uprawnienia (tylko lider może zmienić tag)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI wprowadzania tagu
        plugin.getGuiManager().openGUI(player, new GuildTagInputGUI(plugin, guild));
    }

    /**
     * Obsługa ustawienia domu gildii
     */
    private void handleSetHome(Player player) {
        // Sprawdź uprawnienia (tylko lider może ustawić dom)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Ustaw dom gildii
        plugin.getGuildService().setGuildHomeAsync(guild.getId(), player.getLocation(), player.getUniqueId()).thenAccept(success -> {
            if (success) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("sethome.success", "&aDom gildii został ustawiony pomyślnie!");
                player.sendMessage(ColorUtils.colorize(message));

                // Odśwież GUI
                plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
            } else {
                String message = plugin.getConfigManager().getMessagesConfig().getString("sethome.failed", "&cUstawienie domu gildii nie powiodło się!");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }

    /**
     * Obsługa ustawień uprawnień
     */
    private void handlePermissions(Player player) {
        // Sprawdź uprawnienia (tylko lider może zarządzać uprawnieniami)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI ustawień uprawnień
        plugin.getGuiManager().openGUI(player, new GuildPermissionsGUI(plugin, guild));
    }

    /**
     * Obsługa zapraszania członka
     */
    private void handleInviteMember(Player player) {
        // Sprawdź uprawnienia (oficer lub lider może zapraszać)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cWymagana ranga Oficera lub wyższa");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI zapraszania członka
        plugin.getGuiManager().openGUI(player, new InviteMemberGUI(plugin, guild));
    }

    /**
     * Obsługa wyrzucania członka
     */
    private void handleKickMember(Player player) {
        // Sprawdź uprawnienia (oficer lub lider może wyrzucać)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cWymagana ranga Oficera lub wyższa");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI wyrzucania członka
        plugin.getGuiManager().openGUI(player, new KickMemberGUI(plugin, guild));
    }

    /**
     * Obsługa awansowania członka
     */
    private void handlePromoteMember(Player player) {
        // Sprawdź uprawnienia (tylko lider może awansować)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI awansowania członka
        plugin.getGuiManager().openGUI(player, new PromoteMemberGUI(plugin, guild));
    }

    /**
     * Obsługa degradowania członka
     */
    private void handleDemoteMember(Player player) {
        // Sprawdź uprawnienia (tylko lider może degradować)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI degradowania członka
        plugin.getGuiManager().openGUI(player, new DemoteMemberGUI(plugin, guild));
    }

    /**
     * Obsługa zarządzania aplikacjami
     */
    private void handleApplications(Player player) {
        // Sprawdź uprawnienia (oficer lub lider może zarządzać aplikacjami)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&cWymagana ranga Oficera lub wyższa");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI zarządzania aplikacjami
        plugin.getGuiManager().openGUI(player, new ApplicationManagementGUI(plugin, guild));
    }

    /**
     * Obsługa zarządzania relacjami
     */
    private void handleRelations(Player player) {
        // Sprawdź uprawnienia (tylko lider może zarządzać relacjami)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("relation.only-leader", "&cTylko lider gildii może zarządzać relacjami gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI zarządzania relacjami
        plugin.getGuiManager().openGUI(player, new GuildRelationsGUI(plugin, guild, player));
    }

    /**
     * Obsługa przeglądania logów gildii
     */
    private void handleGuildLogs(Player player) {
        // Sprawdź uprawnienia (członkowie gildii mogą przeglądać logi)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cBrak uprawnień");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI logów gildii
        plugin.getGuiManager().openGUI(player, new GuildLogsGUI(plugin, guild, player));
    }

    /**
     * Obsługa teleportacji do domu gildii
     */
    private void handleHomeTeleport(Player player) {
        // Sprawdź uprawnienia (członkowie gildii mogą teleportować się do domu)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cBrak uprawnień");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Teleport do domu gildii
        plugin.getGuildService().getGuildHomeAsync(guild.getId()).thenAccept(location -> {
            // Upewnij się, że teleportacja odbywa się w głównym wątku
            CompatibleScheduler.runTask(plugin, () -> {
                if (location != null) {
                    player.teleport(location);
                    String message = plugin.getConfigManager().getMessagesConfig().getString("home.success", "&aPrzeteleportowano do domu gildii!");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("home.not-set", "&cDom gildii nie jest ustawiony!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    /**
     * Obsługa opuszczenia gildii
     */
    private void handleLeaveGuild(Player player) {
        // Otwórz GUI potwierdzenia opuszczenia
        plugin.getGuiManager().openGUI(player, new ConfirmLeaveGuildGUI(plugin, guild));
    }

    /**
     * Obsługa usunięcia gildii
     */
    private void handleDeleteGuild(Player player) {
        // Sprawdź uprawnienia (tylko lider może usunąć gildię)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Otwórz GUI potwierdzenia usunięcia
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild));
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
