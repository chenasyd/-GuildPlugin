package com.guild.module.example.apitest;

import com.guild.core.utils.ColorUtils;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * API test panel using {@link AbstractModuleGUI} (recommended module GUI base class).
 */
public class ApiTestPanelGUI extends AbstractModuleGUI {

    private final ApiTestModule module;

    public ApiTestPanelGUI(ApiTestModule module) {
        this.module = module;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&dAPI Test Panel");
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void setupInventory(Inventory inv) {
        this.inventory = inv;
        inv.setItem(11, createItem(Material.PAPER, "&eData Query Test", "&7query all/discover"));
        inv.setItem(12, createItem(Material.PLAYER_HEAD, "&eMember Management Test", "&7add/remove/role"));
        inv.setItem(13, createItem(Material.GOLD_INGOT, "&eEconomy Test", "&7deposit/withdraw/currency"));
        inv.setItem(14, createItem(Material.REDSTONE, "&eHTTP Test", "&7http"));
        inv.setItem(15, createItem(Material.NAME_TAG, "&ePlaceholder Test", "&7placeholder"));
        inv.setItem(16, createItem(Material.CLOCK, "&eTime/Console Test", "&7server time + console output"));
        inv.setItem(22, createItem(Material.BARRIER, "&cClose", "&7Exit"));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType clickType) {
        if (slot == 22) {
            player.closeInventory();
            return;
        }
        player.closeInventory();
        switch (slot) {
            case 11 -> module.runTest(player, "query");
            case 12 -> module.runTest(player, "member");
            case 13 -> module.runTest(player, "economy");
            case 14 -> module.runTest(player, "http");
            case 15 -> module.runTest(player, "placeholder");
            case 16 -> module.runTest(player, "time");
            default -> {
            }
        }
    }
}
