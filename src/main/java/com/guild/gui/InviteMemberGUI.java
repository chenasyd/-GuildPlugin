package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GUI Zapraszania Członków
 */
public class InviteMemberGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private List<Player> onlinePlayers;

    public InviteMemberGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
        // Filtruj graczy online, którzy nie są liderem tej gildii (wstępny filtr)
        // W rzeczywistości powinniśmy filtrować graczy, którzy już są w tej gildii,
        // ale zrobimy to przy wyświetlaniu lub obsłudze kliknięcia, aby nie obciążać konstruktora asynchronicznością
        this.onlinePlayers = Bukkit.getOnlinePlayers().stream()
            .filter(player -> !player.getUniqueId().equals(guild.getLeaderUuid()))
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6Zaproś członka - Strona " + (currentPage + 1));
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Wyświetl graczy online
        displayOnlinePlayers(inventory);

        // Dodaj przyciski nawigacyjne
        setupNavigationButtons(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot >= 9 && slot < 45) {
            // Obszar głów graczy
            int playerIndex = slot - 9 + (currentPage * 36);
            if (playerIndex < onlinePlayers.size()) {
                Player targetPlayer = onlinePlayers.get(playerIndex);
                handleInvitePlayer(player, targetPlayer);
            }
        } else if (slot == 45) {
            // Poprzednia strona
            if (currentPage > 0) {
                currentPage--;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 53) {
            // Następna strona
            int maxPage = (onlinePlayers.size() - 1) / 36;
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
     * Wyświetl graczy online
     */
    private void displayOnlinePlayers(Inventory inventory) {
        int startIndex = currentPage * 36;
        int endIndex = Math.min(startIndex + 36, onlinePlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            Player targetPlayer = onlinePlayers.get(i);
            int slot = 9 + (i - startIndex);

            ItemStack playerHead = createPlayerHead(targetPlayer);
            inventory.setItem(slot, playerHead);
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
        int maxPage = (onlinePlayers.size() - 1) / 36;
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
     * Utwórz głowę gracza
     */
    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ColorUtils.colorize("&a" + player.getName()));
            meta.setLore(Arrays.asList(
                ColorUtils.colorize("&7Kliknij, aby zaprosić tego gracza"),
                ColorUtils.colorize("&7do gildii")
            ));
            head.setItemMeta(meta);
        }

        return head;
    }

    /**
     * Obsługa zaproszenia gracza
     */
    private void handleInvitePlayer(Player inviter, Player target) {
        // Sprawdź czy gracz już jest w gildii (dowolnej)
        // W oryginale było: plugin.getGuildService().getGuildMemberAsync(target.getUniqueId())...
        // Powinniśmy sprawdzić czy gracz jest w JAKIEJKOLWIEK gildii.
        // Metoda getPlayerGuildAsync może być lepsza.
        plugin.getGuildService().getPlayerGuildAsync(target.getUniqueId()).thenAccept(targetGuild -> {
            if (targetGuild != null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("invite.already-in-guild", "&cTen gracz jest już w gildii!");
                inviter.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Wyślij zaproszenie
            plugin.getGuildService().sendInvitationAsync(guild.getId(), inviter.getUniqueId(), inviter.getName(), target.getUniqueId(), target.getName()).thenAccept(success -> {
                if (success) {
                    String inviterMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.sent", "&aWysłano zaproszenie do gracza &e{player}&a!")
                        .replace("{player}", target.getName());
                    inviter.sendMessage(ColorUtils.colorize(inviterMessage));

                    String targetMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.received", "&aOtrzymałeś zaproszenie do gildii &e{guild}&a!")
                        .replace("{guild}", guild.getName());
                    target.sendMessage(ColorUtils.colorize(targetMessage));
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("invite.failed", "&cWysłanie zaproszenia nie powiodło się! Gracz może mieć już aktywne zaproszenie.");
                    inviter.sendMessage(ColorUtils.colorize(message));
                }
            });
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
