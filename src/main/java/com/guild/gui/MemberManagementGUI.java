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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI Zarządzania Członkami
 */
public class MemberManagementGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private static final int MEMBERS_PER_PAGE = 28; // 4 rzędy po 7 kolumn, bez obramowania

    public MemberManagementGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.title", "&6Zarządzanie członkami"));
    }

    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("member-management.size", 54);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Dodaj przyciski funkcyjne
        setupFunctionButtons(inventory);

        // Załaduj listę członków
        loadMembers(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // Sprawdź czy to przycisk funkcyjny
        if (isFunctionButton(slot)) {
            handleFunctionButton(player, slot);
            return;
        }

        // Sprawdź czy to przycisk paginacji
        if (isPaginationButton(slot)) {
            handlePaginationButton(player, slot);
            return;
        }

        // Sprawdź czy to slot członka
        if (isMemberSlot(slot)) {
            handleMemberClick(player, slot, clickedItem, clickType);
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
     * Skonfiguruj przyciski funkcyjne
     */
    private void setupFunctionButtons(Inventory inventory) {
        // Przycisk zaproszenia członka
        ItemStack inviteMember = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.invite-member.name", "&aZaproś członka")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.invite-member.lore.1", "&7Zaproś nowego członka"))
        );
        inventory.setItem(45, inviteMember);

        // Przycisk wyrzucenia członka
        ItemStack kickMember = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.kick-member.name", "&cWyrzuć członka")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.kick-member.lore.1", "&7Wyrzuć członka gildii"))
        );
        inventory.setItem(47, kickMember);

        // Przycisk awansu członka
        ItemStack promoteMember = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.promote-member.name", "&6Awansuj członka")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.promote-member.lore.1", "&7Awansuj członka na wyższą rangę"))
        );
        inventory.setItem(49, promoteMember);

        // Przycisk degradacji członka
        ItemStack demoteMember = createItem(
            Material.IRON_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.demote-member.name", "&7Zdegraduj członka")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.demote-member.lore.1", "&7Zdegraduj członka na niższą rangę"))
        );
        inventory.setItem(51, demoteMember);

        // Przycisk powrotu
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.back.name", "&7Powrót")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.back.lore.1", "&7Powrót do menu głównego"))
        );
        inventory.setItem(53, back);
    }

    /**
     * Załaduj listę członków
     */
    private void loadMembers(Inventory inventory) {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(members -> {
            if (members == null || members.isEmpty()) {
                // Wyświetl informację o braku członków
                ItemStack noMembers = createItem(
                    Material.BARRIER,
                    ColorUtils.colorize("&cBrak członków"),
                    ColorUtils.colorize("&7Gildia nie ma jeszcze członków")
                );
                inventory.setItem(22, noMembers);
                return;
            }

            // Oblicz paginację
            int totalPages = (members.size() - 1) / MEMBERS_PER_PAGE;
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }

            // Skonfiguruj przyciski paginacji
            setupPaginationButtons(inventory, totalPages);

            // Wyświetl członków na bieżącej stronie
            int startIndex = currentPage * MEMBERS_PER_PAGE;
            int endIndex = Math.min(startIndex + MEMBERS_PER_PAGE, members.size());

            int slotIndex = 10; // Rozpocznij od 2 rzędu, 2 kolumny
            for (int i = startIndex; i < endIndex; i++) {
                GuildMember member = members.get(i);
                if (slotIndex >= 44) break; // Unikaj wyjścia poza obszar wyświetlania

                ItemStack memberItem = createMemberItem(member);
                inventory.setItem(slotIndex, memberItem);

                slotIndex++;
                if (slotIndex % 9 == 8) { // Pomiń obramowanie
                    slotIndex += 2;
                }
            }
        });
    }

    /**
     * Skonfiguruj przyciski paginacji
     */
    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        // Przycisk poprzedniej strony
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.previous-page.name", "&cPoprzednia strona")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.previous-page.lore.1", "&7Zobacz poprzednią stronę"))
            );
            inventory.setItem(18, previousPage);
        }

        // Przycisk następnej strony
        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.next-page.name", "&aNastępna strona")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-management.items.next-page.lore.1", "&7Zobacz następną stronę"))
            );
            inventory.setItem(26, nextPage);
        }
    }

    /**
     * Utwórz przedmiot członka
     */
    private ItemStack createMemberItem(GuildMember member) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();

        switch (member.getRole()) {
            case LEADER:
                material = Material.GOLDEN_HELMET;
                name = PlaceholderUtils.replaceMemberPlaceholders("&c{member_name}", member, guild);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7Rola: &c{member_role}", member, guild));
                break;
            case OFFICER:
                material = Material.GOLDEN_HELMET;
                name = PlaceholderUtils.replaceMemberPlaceholders("&6{member_name}", member, guild);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7Rola: &6{member_role}", member, guild));
                break;
            default:
                material = Material.PLAYER_HEAD;
                name = PlaceholderUtils.replaceMemberPlaceholders("&f{member_name}", member, guild);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7Rola: &f{member_role}", member, guild));
                break;
        }

        lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7Dołączył: {member_join_time}", member, guild));
        lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7Uprawnienia: " + getRolePermissions(member.getRole()), member, guild));
        lore.add("");
        lore.add(ColorUtils.colorize("&aLewy przycisk: Zobacz szczegóły"));

        if (member.getRole() != GuildMember.Role.LEADER) {
            lore.add(ColorUtils.colorize("&cPrawy przycisk: Wyrzuć członka"));
            lore.add(ColorUtils.colorize("&6Środkowy przycisk: Awansuj/Zdegraduj"));
        }

        return createItem(material, name, lore.toArray(new String[0]));
    }

    /**
     * Pobierz opis uprawnień roli
     */
    private String getRolePermissions(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "Wszystkie uprawnienia";
            case OFFICER:
                return "Zapraszanie, Wyrzucanie";
            default:
                return "Podstawowe uprawnienia";
        }
    }

    /**
     * Sprawdź czy to przycisk funkcyjny
     */
    private boolean isFunctionButton(int slot) {
        return slot == 45 || slot == 47 || slot == 49 || slot == 51 || slot == 53;
    }

    /**
     * Sprawdź czy to przycisk paginacji
     */
    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }

    /**
     * Sprawdź czy to slot członka
     */
    private boolean isMemberSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8;
    }

    /**
     * Obsługa kliknięcia przycisku funkcyjnego
     */
    private void handleFunctionButton(Player player, int slot) {
        switch (slot) {
            case 45: // Zaproś członka
                handleInviteMember(player);
                break;
            case 47: // Wyrzuć członka
                handleKickMember(player);
                break;
            case 49: // Awansuj członka
                handlePromoteMember(player);
                break;
            case 51: // Zdegraduj członka
                handleDemoteMember(player);
                break;
            case 53: // Powrót
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                break;
        }
    }

    /**
     * Obsługa kliknięcia przycisku paginacji
     */
    private void handlePaginationButton(Player player, int slot) {
        if (slot == 18) { // Poprzednia strona
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
        } else if (slot == 26) { // Następna strona
            currentPage++;
            refreshInventory(player);
        }
    }

    /**
     * Obsługa kliknięcia członka
     */
    private void handleMemberClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // Pobierz klikniętego członka
        int memberIndex = (currentPage * MEMBERS_PER_PAGE) + (slot - 10);
        if (memberIndex % 9 == 0 || memberIndex % 9 == 8) return; // Pomiń obramowanie

        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(members -> {
            if (members != null && memberIndex < members.size()) {
                GuildMember member = members.get(memberIndex);

                if (clickType == ClickType.LEFT) {
                    // Zobacz szczegóły członka
                    showMemberDetails(player, member);
                } else if (clickType == ClickType.RIGHT) {
                    // Wyrzuć członka
                    handleKickMemberDirect(player, member);
                } else if (clickType == ClickType.MIDDLE) {
                    // Awansuj/Zdegraduj członka
                    handlePromoteDemoteMember(player, member);
                }
            }
        });
    }

    /**
     * Pokaż szczegóły członka
     */
    private void showMemberDetails(Player player, GuildMember member) {
        // Otwórz GUI szczegółów członka
        plugin.getGuiManager().openGUI(player, new MemberDetailsGUI(plugin, guild, member, player));
    }

    /**
     * Bezpośrednie wyrzucenie członka
     */
    private void handleKickMemberDirect(Player player, GuildMember member) {
        // Sprawdź uprawnienia
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || !executor.getRole().canKick()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cBrak uprawnień");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Nie można wyrzucić lidera
            if (member.getRole() == GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.cannot-kick-leader", "&cNie można wyrzucić lidera gildii");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Potwierdź wyrzucenie
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-kick", "&cCzy na pewno chcesz wyrzucić członka {member}? Wpisz &f/guild kick {member} confirm &caby potwierdzić")
                .replace("{member}", member.getPlayerName());
            player.sendMessage(ColorUtils.colorize(message));
        });
    }

    /**
     * Awansuj/Zdegraduj członka
     */
    private void handlePromoteDemoteMember(Player player, GuildMember member) {
        // Sprawdź uprawnienia
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || executor.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Nie można modyfikować lidera
            if (member.getRole() == GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.cannot-modify-leader", "&cNie można modyfikować rangi lidera gildii");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            if (member.getRole() == GuildMember.Role.OFFICER) {
                // Zdegraduj do zwykłego członka
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-demote", "&cCzy na pewno chcesz zdegradować członka {member}? Wpisz &f/guild demote {member} confirm &caby potwierdzić")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            } else {
                // Awansuj na oficera
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-promote", "&aCzy na pewno chcesz awansować członka {member} na oficera? Wpisz &f/guild promote {member} confirm &aaby potwierdzić")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }

    /**
     * Obsługa zaproszenia członka
     */
    private void handleInviteMember(Player player) {
        // Sprawdź uprawnienia
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || !member.getRole().canInvite()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cBrak uprawnień");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Otwórz GUI zaproszenia członka
            InviteMemberGUI inviteMemberGUI = new InviteMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, inviteMemberGUI);
        });
    }

    /**
     * Obsługa wyrzucenia członka
     */
    private void handleKickMember(Player player) {
        // Sprawdź uprawnienia
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || !member.getRole().canKick()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cBrak uprawnień");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Otwórz GUI wyrzucenia członka
            KickMemberGUI kickMemberGUI = new KickMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, kickMemberGUI);
        });
    }

    /**
     * Obsługa awansu członka
     */
    private void handlePromoteMember(Player player) {
        // Sprawdź uprawnienia
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || member.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Otwórz GUI awansu członka
            PromoteMemberGUI promoteMemberGUI = new PromoteMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, promoteMemberGUI);
        });
    }

    /**
     * Obsługa degradacji członka
     */
    private void handleDemoteMember(Player player) {
        // Sprawdź uprawnienia
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            if (member == null || member.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cTylko lider gildii może wykonać tę operację");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Otwórz GUI degradacji członka
            DemoteMemberGUI demoteMemberGUI = new DemoteMemberGUI(plugin, guild);
            plugin.getGuiManager().openGUI(player, demoteMemberGUI);
        });
    }

    /**
     * Odśwież ekwipunek
     */
    private void refreshInventory(Player player) {
        plugin.getGuiManager().refreshGUI(player);
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
