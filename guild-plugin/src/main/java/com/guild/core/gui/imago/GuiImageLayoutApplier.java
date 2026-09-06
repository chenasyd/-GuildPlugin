package com.guild.core.gui.imago;

import com.guild.GuildPlugin;
import com.guild.core.geyser.PlayerConnectionService;
import com.guild.core.gui.layout.GuiImageLayoutConfig;
import com.guild.sdk.gui.GUILayoutDefinition;
import com.guild.sdk.gui.ModuleGUIRegistration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 对已填充的 GUI Inventory 应用 Imago 图像布局（透明载体、移除填充玻璃板）。
 */
public final class GuiImageLayoutApplier {

    private final GuildPlugin plugin;
    private final ImagoGuiIntegration imago;

    public GuiImageLayoutApplier(GuildPlugin plugin, ImagoGuiIntegration imago) {
        this.plugin = plugin;
        this.imago = imago;
    }

    public void applyIfNeeded(Player player, Inventory inventory, String guiType) {
        if (!imago.isAvailable()) {
            return;
        }
        if (player != null && PlayerConnectionService.isBedrockPlayer(player)) {
            return;
        }
        if (!imago.isImageGuiActive(guiType)) {
            return;
        }

        ModuleGUIRegistration moduleReg = plugin.getModuleManager().getRegistry().getCustomGUIRegistration(guiType);
        if (moduleReg != null && moduleReg.getLayout() != null) {
            applyModuleLayout(inventory, moduleReg.getLayout());
            return;
        }

        GuiImageLayoutConfig imageLayoutConfig = imago.getImageLayoutConfig();
        Material transMat = imageLayoutConfig != null
                ? imageLayoutConfig.getTransparentMaterial() : Material.BARRIER;
        int modelData = imageLayoutConfig != null
                ? imageLayoutConfig.getTransparentModelData() : 10001;

        if (imageLayoutConfig != null && imageLayoutConfig.hasLayout(guiType)) {
            Set<Integer> layoutSlots = new HashSet<>();
            for (List<Integer> slots : imageLayoutConfig.getLayout(guiType).values()) {
                layoutSlots.addAll(slots);
            }

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                if (isFillerItem(item.getType())) {
                    inventory.setItem(i, null);
                    continue;
                }
                if (layoutSlots.contains(i)) {
                    inventory.setItem(i, toTransparentCarrier(item, transMat, modelData));
                }
            }
            return;
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (isFillerItem(item.getType())) {
                inventory.setItem(i, null);
                continue;
            }
            inventory.setItem(i, toTransparentCarrier(item, transMat, modelData));
        }
    }

    private void applyModuleLayout(Inventory inventory, GUILayoutDefinition layout) {
        GuiImageLayoutConfig imageLayoutConfig = imago.getImageLayoutConfig();
        Material transMat = imageLayoutConfig != null ? imageLayoutConfig.getTransparentMaterial() : Material.BARRIER;
        int modelData = imageLayoutConfig != null ? imageLayoutConfig.getTransparentModelData() : 10001;

        for (java.util.Map.Entry<String, int[]> entry : layout.getFunctions().entrySet()) {
            for (int slot : entry.getValue()) {
                if (slot < 0 || slot >= inventory.getSize()) {
                    continue;
                }
                ItemStack item = inventory.getItem(slot);
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                if (isFillerItem(item.getType())) {
                    inventory.setItem(slot, null);
                    continue;
                }
                inventory.setItem(slot, toTransparentCarrier(item, transMat, modelData));
            }
        }
    }

    static boolean isFillerItem(Material mat) {
        return mat == Material.BLACK_STAINED_GLASS_PANE
                || mat == Material.GRAY_STAINED_GLASS_PANE
                || mat == Material.WHITE_STAINED_GLASS_PANE
                || mat == Material.GLASS_PANE
                || mat.name().endsWith("_STAINED_GLASS_PANE");
    }

    private static ItemStack toTransparentCarrier(ItemStack original, Material transMat, int modelData) {
        ItemStack transparent = new ItemStack(transMat);
        org.bukkit.inventory.meta.ItemMeta oldMeta = original.getItemMeta();
        org.bukkit.inventory.meta.ItemMeta newMeta = transparent.getItemMeta();
        if (oldMeta != null && newMeta != null) {
            if (oldMeta.hasDisplayName()) {
                newMeta.setDisplayName(oldMeta.getDisplayName());
            }
            if (oldMeta.hasLore()) {
                newMeta.setLore(oldMeta.getLore());
            }
            newMeta.setCustomModelData(modelData);
            transparent.setItemMeta(newMeta);
        }
        return transparent;
    }
}
