package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.CoreMsg;
import com.guild.core.utils.ColorUtils;
import com.guild.war.season.WarSeasonService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** 本赛季工会战排行（轻量 GUI）。 */
public final class WarSeasonGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final List<WarSeasonService.SeasonRow> rows;
    private final String seasonId;

    public WarSeasonGUI(GuildPlugin plugin, Player player,
                        String seasonId, List<WarSeasonService.SeasonRow> rows) {
        this.plugin = plugin;
        this.player = player;
        this.seasonId = seasonId;
        this.rows = rows != null ? rows : List.of();
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(CoreMsg.raw(plugin, player, "war.season.gui-title",
                "&6赛季战绩 &7({season})", "{season}", seasonId));
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        inventory.clear();
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.setDisplayName(ColorUtils.colorize("&e" + seasonId));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7Wins · Losses · Draws · Kills"));
            im.setLore(lore);
            info.setItemMeta(im);
        }
        inventory.setItem(4, info);

        int slot = 10;
        int rank = 1;
        for (WarSeasonService.SeasonRow row : rows) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 8) {
                slot += 2;
            }
            Material mat = rank <= 3 ? Material.GOLDEN_SWORD : Material.IRON_SWORD;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize("&6#" + rank + " &f" + row.guildName()));
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.colorize("&aW " + row.wins()
                        + " &cL " + row.losses()
                        + " &7D " + row.draws()));
                lore.add(ColorUtils.colorize("&eKills: " + row.kills()
                        + " &8| Matches: " + row.matches()));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            slot++;
            rank++;
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ColorUtils.colorize("&cClose"));
            close.setItemMeta(cm);
        }
        inventory.setItem(49, close);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) {
            player.closeInventory();
        }
    }
}
