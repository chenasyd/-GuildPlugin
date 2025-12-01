package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI Awansowania Członków
 */
public class PromoteMemberGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private List<GuildMember> members;

    public PromoteMemberGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
        // Inicjalizacja listy członków
        this.members = List.of();
        loadMembers();
    }

    private void loadMembers() {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(memberList -> {
            this.members = memberList.stream()
                .filter(member -> !member.getPlayerUuid().equals(guild.getLeaderUuid()))
                .filter(member -> !member.getRole().equals(GuildMember.Role.OFFICER)) // Pokaż tylko członków, których można awansować
                .collect(java.util.stream.Collectors.toList());
        });
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6Awansuj członka - Strona " + (currentPage + 1));
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Wyświetl listę członków
        displayMembers(inventory);

        // Dodaj przyciski nawigacyjne
        setupNavigationButtons(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot >= 9 && slot < 45) {
            // Obszar głów członków
            int memberIndex = slot - 9 + (currentPage * 36);
            if (memberIndex < members.size()) {
                GuildMember member = members.get(memberIndex);
                handlePromoteMember(player, member);
            }
        } else if (slot == 45) {
            // Poprzednia strona
            if (currentPage > 0) {
                currentPage--;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 53) {
            // Następna strona
            int maxPage = (members.size() - 1) / 36;
            if (currentPage < maxPage) {
                currentPage++;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 49) {
            // Powrót
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
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
     * Wyświetl listę członków
     */
    private void displayMembers(Inventory inventory) {
        int startIndex = currentPage * 36;
        int endIndex = Math.min(startIndex + 36, members.size());

        for (int i = startIndex; i < endIndex; i++) {
            GuildMember member = members.get(i);
            int slot = 9 + (i - startIndex);

            ItemStack memberHead = createMemberHead(member);
            inventory.setItem(slot, memberHead);
        }
    }

    /**
     * Skonfiguruj przyciski nawigacyjne
     */
    private void setupNavigationButtons(Inventory inventory) {
        // Przycisk poprzedniej strony
        if (currentPage > 0) {
            ItemStack prevPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&ePoprzednia strona"),
                ColorUtils.colorize("&7Kliknij, aby zobaczyć poprzednią stronę")
            );
            inventory.setItem(45, prevPage);
        }

        // Przycisk następnej strony
        int maxPage = (members.size() - 1) / 36;
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&eNastępna strona"),
                ColorUtils.colorize("&7Kliknij, aby zobaczyć następną stronę")
            );
            inventory.setItem(53, nextPage);
        }

        // Przycisk powrotu
        ItemStack back = createItem(
            Material.BARRIER,
            ColorUtils.colorize("&cPowrót"),
            ColorUtils.colorize("&7Powrót do ustawień gildii")
        );
        inventory.setItem(49, back);
    }

    /**
     * Utwórz głowę członka
     */
    private ItemStack createMemberHead(GuildMember member) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&6" + member.getPlayerName()));
            meta.setLore(Arrays.asList(
                ColorUtils.colorize("&7Obecna rola: &e" + member.getRole().getDisplayName()),
                ColorUtils.colorize("&7Dołączył: &e" + member.getJoinedAt()),
                ColorUtils.colorize("&6Kliknij, aby awansować na oficera")
            ));
            head.setItemMeta(meta);
        }

        return head;
    }

    /**
     * Obsługa awansu członka
     */
    private void handlePromoteMember(Player promoter, GuildMember member) {
        // Sprawdź uprawnienia
        if (!promoter.hasPermission("guild.promote")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cBrak uprawnień");
            promoter.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Awansuj członka
        plugin.getGuildService().updateMemberRoleAsync(member.getPlayerUuid(), GuildMember.Role.OFFICER, promoter.getUniqueId()).thenAccept(success -> {
            if (success) {
                String promoterMessage = plugin.getConfigManager().getMessagesConfig().getString("promote.success", "&aAwansowano &e{player} &ana oficera!")
                    .replace("{player}", member.getPlayerName());
                promoter.sendMessage(ColorUtils.colorize(promoterMessage));

                // Powiadom awansowanego gracza
                Player promotedPlayer = plugin.getServer().getPlayer(member.getPlayerUuid());
                if (promotedPlayer != null) {
                    String promotedMessage = plugin.getConfigManager().getMessagesConfig().getString("promote.promoted", "&aZostałeś awansowany na oficera w gildii &e{guild}&a!")
                        .replace("{guild}", guild.getName());
                    promotedPlayer.sendMessage(ColorUtils.colorize(promotedMessage));
                }

                // Odśwież GUI
                plugin.getGuiManager().openGUI(promoter, new PromoteMemberGUI(plugin, guild));
            } else {
                String message = plugin.getConfigManager().getMessagesConfig().getString("promote.failed", "&cAwansowanie członka nie powiodło się!");
                promoter.sendMessage(ColorUtils.colorize(message));
            }
        });
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
