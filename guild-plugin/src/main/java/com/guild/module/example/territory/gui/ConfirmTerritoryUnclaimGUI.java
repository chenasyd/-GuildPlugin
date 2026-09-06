package com.guild.module.example.territory.gui;

import com.guild.models.Guild;
import com.guild.module.example.territory.TerritoryCommandHandler;
import com.guild.module.example.territory.TerritoryModule;
import com.guild.module.example.territory.TerritoryRecord;
import com.guild.module.example.territory.TerritoryTexts;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** 放弃领地确认对话框。 */
public final class ConfirmTerritoryUnclaimGUI extends AbstractModuleGUI {

    public static final String GUI_ID = "territory-confirm-unclaim";

    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_CANCEL = 15;

    private final TerritoryModule module;
    private final Guild guild;
    private final Player viewer;

    public ConfirmTerritoryUnclaimGUI(TerritoryModule module, Guild guild, Player viewer) {
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
        return TerritoryBedrockForms.openUnclaimConfirm(module, guild, player);
    }

    @Override
    public String getTitle() {
        return texts().format(viewer, "module.territory.gui.confirm-unclaim-title", "&c确认放弃领地");
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void setupInventory(Inventory inv) {
        this.inventory = inv;
        fillBorder(inv);

        inv.setItem(SLOT_CONFIRM, createItem(Material.RED_WOOL,
                texts().format(viewer, "module.territory.gui.confirm", "&c确认放弃"),
                texts().format(viewer, "module.territory.gui.confirm-unclaim-button", "&7删除 WG 区域与本地记录")));

        inv.setItem(SLOT_INFO, buildInfoItem());

        inv.setItem(SLOT_CANCEL, createItem(Material.LIME_WOOL,
                texts().format(viewer, "module.territory.gui.cancel", "&a取消"),
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
                handler.unclaim(player);
            }
            TerritoryGuiNavigation.backToManagement(module, player);
            return;
        }
        if (slot == SLOT_CANCEL) {
            TerritoryGuiNavigation.backToManagement(module, player);
        }
    }

    private ItemStack buildInfoItem() {
        String worldName = viewer.getWorld().getName();
        Optional<TerritoryRecord> record = module.getRepository().get(guild.getId(), worldName);
        if (record.isEmpty() && module.getBridge().isOperational()) {
            record = module.getBridge().findTerritory(guild.getId(), worldName);
        }
        if (record.isEmpty()) {
            return createItem(Material.BARRIER,
                    texts().format(viewer, "module.territory.gui.confirm-unclaim-none", "&7当前世界无领地"),
                    texts().format(viewer, "module.territory.not-found", "&7当前世界（{0}）暂无公会领地。", worldName));
        }
        TerritoryRecord territory = record.get();
        return createItem(Material.TNT,
                texts().format(viewer, "module.territory.gui.confirm-unclaim-info", "&c放弃警告"),
                texts().format(viewer, "module.territory.info-line-world", "&7世界: &f{0}", territory.getWorldName()),
                texts().format(viewer, "module.territory.info-line-region", "&7区域: &f{0}", territory.getRegionId()),
                texts().format(viewer, "module.territory.gui.confirm-unclaim-warning",
                        "&c放弃后该区域将不再受 WorldGuard 保护"));
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
