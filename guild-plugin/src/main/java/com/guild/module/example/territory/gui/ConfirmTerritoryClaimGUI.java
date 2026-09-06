package com.guild.module.example.territory.gui;

import com.guild.models.Guild;
import com.guild.module.example.territory.TerritoryCommandHandler;
import com.guild.module.example.territory.TerritoryModule;
import com.guild.module.example.territory.TerritorySelectionManager;
import com.guild.module.example.territory.TerritorySettings;
import com.guild.module.example.territory.TerritoryTexts;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** 声明领地确认对话框。 */
public final class ConfirmTerritoryClaimGUI extends AbstractModuleGUI {

    public static final String GUI_ID = "territory-confirm-claim";

    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_CANCEL = 15;

    private final TerritoryModule module;
    private final Guild guild;
    private final Player viewer;

    public ConfirmTerritoryClaimGUI(TerritoryModule module, Guild guild, Player viewer) {
        this.module = module;
        this.guild = guild;
        this.viewer = viewer;
    }

    @Override
    public String getGuiType() {
        return GUI_ID;
    }

    @Override
    public boolean openBedrockForm(Player player) {
        return TerritoryBedrockForms.openClaimConfirm(module, guild, player);
    }

    @Override
    public String getTitle() {
        return texts().format(viewer, "module.territory.gui.confirm-claim-title", "&a确认声明领地");
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void setupInventory(Inventory inv) {
        this.inventory = inv;
        fillBorder(inv);

        inv.setItem(SLOT_CONFIRM, createItem(Material.LIME_WOOL,
                texts().format(viewer, "module.territory.gui.confirm", "&a确认"),
                texts().format(viewer, "module.territory.gui.confirm-claim-button", "&7扣除金库并创建 WG 区域")));

        inv.setItem(SLOT_INFO, buildInfoItem());

        inv.setItem(SLOT_CANCEL, createItem(Material.RED_WOOL,
                texts().format(viewer, "module.territory.gui.cancel", "&c取消"),
                texts().format(viewer, "module.territory.gui.cancel-lore", "&7返回领地面板")));
        fillInteriorSlots(inv);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        if (slot == SLOT_CONFIRM) {
            TerritoryCommandHandler handler = module.getCommandHandler();
            if (handler != null) {
                handler.claim(player);
            }
            module.getContext().openGUI(player,
                    new TerritoryManagementGUI(module, guild, player, true));
            return;
        }
        if (slot == SLOT_CANCEL) {
            module.getContext().openGUI(player,
                    new TerritoryManagementGUI(module, guild, player, true));
        }
    }

    private ItemStack buildInfoItem() {
        TerritorySelectionManager selections = module.getSelectionManager();
        TerritorySettings settings = module.getSettings();
        long volume = selections != null ? selections.selectionVolume(viewer) : 0L;
        double cost = settings != null ? settings.getClaimCost() : 0;

        String worldName = viewer.getWorld().getName();
        if (selections != null && selections.hasCompleteSelection(viewer)) {
            worldName = selections.of(viewer).pos1.getWorld().getName();
        }

        if (cost > 0) {
            return createItem(Material.PAPER,
                    texts().format(viewer, "module.territory.gui.confirm-claim-info", "&6声明详情"),
                    texts().format(viewer, "module.territory.info-line-world", "&7世界: &f{0}", worldName),
                    texts().format(viewer, "module.territory.selection-volume", "&7选区体积: &f{0} 方块", volume),
                    texts().format(viewer, "module.territory.gui.claim-cost", "&7费用: &f{0}", formatMoney(cost)),
                    texts().format(viewer, "module.territory.gui.confirm-claim-warning",
                            "&c此操作不可撤销，请确认选区正确"));
        }
        return createItem(Material.PAPER,
                texts().format(viewer, "module.territory.gui.confirm-claim-info", "&6声明详情"),
                texts().format(viewer, "module.territory.info-line-world", "&7世界: &f{0}", worldName),
                texts().format(viewer, "module.territory.selection-volume", "&7选区体积: &f{0} 方块", volume),
                texts().format(viewer, "module.territory.gui.confirm-claim-warning",
                        "&c此操作不可撤销，请确认选区正确"));
    }

    private String formatMoney(double amount) {
        if (module.getContext().getPlugin().getEconomyManager().isVaultAvailable()) {
            return module.getContext().getPlugin().getEconomyManager().format(amount);
        }
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    private TerritoryTexts texts() {
        return module.getTexts();
    }

    @Override
    protected void fillBorder(Inventory inv) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }
    }

    @Override
    protected void fillInteriorSlots(Inventory inv) {
        ItemStack filler = createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < inv.getSize(); slot++) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, filler);
            }
        }
    }
}
