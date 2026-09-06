package com.guild.module.example.territory.gui;

import com.guild.models.Guild;
import com.guild.module.example.territory.TerritoryCommandHandler;
import com.guild.module.example.territory.TerritoryModule;
import com.guild.module.example.territory.TerritoryRecord;
import com.guild.module.example.territory.TerritorySelectionManager;
import com.guild.module.example.territory.TerritorySettings;
import com.guild.module.example.territory.TerritoryWorldClaimPolicy;
import com.guild.module.example.territory.TerritoryTexts;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 公会领地管理面板：查看当前世界与其它世界领地、选区状态；管理可执行声明/放弃等操作。
 */
public final class TerritoryManagementGUI extends AbstractModuleGUI {

    public static final String GUI_ID = "territory-manage";

    private static final int SLOT_HEADER = 4;
    private static final int SLOT_CURRENT = 20;
    private static final int SLOT_SELECTION = 22;
    private static final int SLOT_WG_STATUS = 24;
    private static final int SLOT_WAND = 28;
    private static final int SLOT_POS1 = 29;
    private static final int SLOT_POS2 = 30;
    private static final int SLOT_CLAIM = 31;
    private static final int SLOT_UNCLAIM = 32;
    private static final int SLOT_BACK = 49;
    private static final int[] LIST_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private final TerritoryModule module;
    private final Guild guild;
    private final Player viewer;
    private final boolean manageMode;

    public TerritoryManagementGUI(TerritoryModule module, Guild guild, Player viewer, boolean manageMode) {
        this.module = module;
        this.guild = guild;
        this.viewer = viewer;
        this.manageMode = manageMode;
    }

    @Override
    public String getGuiType() {
        return GUI_ID;
    }

    @Override
    public boolean openBedrockForm(Player player) {
        return TerritoryBedrockForms.openManagement(module, guild, player, manageMode);
    }

    @Override
    public String getTitle() {
        return texts().format(viewer, "module.territory.gui.title",
                "&6&l公会领地 &7- {0}", guild.getName());
    }

    @Override
    public void setupInventory(Inventory inv) {
        this.inventory = inv;
        fillBorder(inv);

        inv.setItem(SLOT_HEADER, createItem(Material.GRASS_BLOCK,
                texts().format(viewer, "module.territory.gui.header", "&6&l公会领地"),
                texts().format(viewer, "module.territory.gui.header-guild", "&7公会: &f{0}", guild.getName()),
                texts().format(viewer, "module.territory.gui.header-server", "&7本机 ID: &f{0}",
                        module.getRepository().getLocalServerId()),
                texts().format(viewer, "module.territory.gui.header-world", "&7当前世界: &f{0}",
                        viewer.getWorld().getName())));

        inv.setItem(SLOT_CURRENT, buildCurrentWorldItem());
        inv.setItem(SLOT_SELECTION, buildSelectionItem());
        inv.setItem(SLOT_WG_STATUS, buildWorldGuardItem());

        renderTerritoryList(inv);

        if (manageMode) {
            inv.setItem(SLOT_WAND, createItem(Material.WOODEN_AXE,
                    texts().format(viewer, "module.territory.gui.wand", "&a选区斧"),
                    texts().format(viewer, "module.territory.gui.wand-lore", "&7左键 Pos1，右键 Pos2")));
            inv.setItem(SLOT_POS1, createItem(Material.LIME_CONCRETE,
                    texts().format(viewer, "module.territory.gui.pos1", "&aPos1"),
                    texts().format(viewer, "module.territory.gui.pos1-lore", "&7以当前位置设为 Pos1")));
            inv.setItem(SLOT_POS2, createItem(Material.YELLOW_CONCRETE,
                    texts().format(viewer, "module.territory.gui.pos2", "&ePos2"),
                    texts().format(viewer, "module.territory.gui.pos2-lore", "&7以当前位置设为 Pos2")));
            inv.setItem(SLOT_CLAIM, buildClaimButton());
            inv.setItem(SLOT_UNCLAIM, createItem(Material.REDSTONE_BLOCK,
                    texts().format(viewer, "module.territory.gui.unclaim", "&c放弃领地"),
                    texts().format(viewer, "module.territory.gui.unclaim-lore", "&7放弃当前世界领地")));
        }

        inv.setItem(SLOT_BACK, createBackButton(
                texts().format(viewer, "module.territory.gui.back", "&c返回"),
                texts().format(viewer, "module.territory.gui.back-lore", "&7返回上一界面")));
        fillInteriorSlots(inv);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        if (slot == SLOT_BACK) {
            module.getContext().navigateBack(player);
            return;
        }
        if (!manageMode) {
            return;
        }
        TerritoryCommandHandler handler = module.getCommandHandler();
        if (handler == null) {
            return;
        }
        if (slot == SLOT_WAND) {
            handler.giveWand(player);
            refresh(player);
            return;
        }
        if (slot == SLOT_POS1) {
            handler.setCorner(player, true);
            refresh(player);
            return;
        }
        if (slot == SLOT_POS2) {
            handler.setCorner(player, false);
            refresh(player);
            return;
        }
        if (slot == SLOT_CLAIM) {
            if (!isClaimAllowedInCurrentWorld()) {
                sendWorldBlockedMessage(player);
                return;
            }
            module.getContext().openGUI(player, new ConfirmTerritoryClaimGUI(module, guild, player));
            return;
        }
        if (slot == SLOT_UNCLAIM) {
            module.getContext().openGUI(player, new ConfirmTerritoryUnclaimGUI(module, guild, player));
        }
    }

    private ItemStack buildCurrentWorldItem() {
        String worldName = viewer.getWorld().getName();
        Optional<TerritoryRecord> record = findTerritory(worldName);
        if (record.isEmpty()) {
            return createItem(Material.BARRIER,
                    texts().format(viewer, "module.territory.gui.current-none", "&7当前世界无领地"),
                    texts().format(viewer, "module.territory.gui.current-none-lore",
                            "&7使用选区工具声明后可在此世界保护公会区域"));
        }
        TerritoryRecord territory = record.get();
        return createItem(Material.MAP,
                texts().format(viewer, "module.territory.gui.current-claimed", "&a已声明领地"),
                texts().format(viewer, "module.territory.info-line-region", "&7区域: &f{0}",
                        territory.getRegionId()),
                texts().format(viewer, "module.territory.info-line-bounds",
                        "&7范围: &f({0},{1},{2}) &7→ &f({3},{4},{5})",
                        territory.getMinX(), territory.getMinY(), territory.getMinZ(),
                        territory.getMaxX(), territory.getMaxY(), territory.getMaxZ()),
                texts().format(viewer, "module.territory.gui.volume-line", "&7体积: &f{0} 方块",
                        formatVolume(territory)));
    }

    private ItemStack buildSelectionItem() {
        TerritorySelectionManager selections = module.getSelectionManager();
        if (selections == null || !selections.hasCompleteSelection(viewer)) {
            String pos1 = formatPos(selections, true);
            String pos2 = formatPos(selections, false);
            return createItem(Material.GRAY_STAINED_GLASS,
                    texts().format(viewer, "module.territory.gui.selection-incomplete", "&7选区未完成"),
                    texts().format(viewer, "module.territory.gui.selection-pos1", "&7Pos1: &f{0}", pos1),
                    texts().format(viewer, "module.territory.gui.selection-pos2", "&7Pos2: &f{0}", pos2));
        }
        long volume = selections.selectionVolume(viewer);
        TerritorySelectionManager.Session session = selections.of(viewer);
        return createItem(Material.LIME_STAINED_GLASS,
                texts().format(viewer, "module.territory.gui.selection-ready", "&a选区就绪"),
                texts().format(viewer, "module.territory.gui.selection-pos1", "&7Pos1: &f{0}",
                        formatLocation(session.pos1)),
                texts().format(viewer, "module.territory.gui.selection-pos2", "&7Pos2: &f{0}",
                        formatLocation(session.pos2)),
                texts().format(viewer, "module.territory.selection-volume", "&7选区体积: &f{0} 方块", volume));
    }

    private ItemStack buildWorldGuardItem() {
        if (module.isWorldGuardReady()) {
            return createItem(Material.SHIELD,
                    texts().format(viewer, "module.territory.gui.wg-ready", "&aWorldGuard 就绪"),
                    texts().format(viewer, "module.territory.gui.wg-ready-lore", "&7可正常声明与管理领地"));
        }
        String missing = module.getAvailability() != null
                ? module.getAvailability().describeMissing()
                : "WorldGuard";
        return createItem(Material.IRON_BARS,
                texts().format(viewer, "module.territory.gui.wg-degraded", "&eWorldGuard 不可用"),
                texts().format(viewer, "module.territory.gui.wg-degraded-lore", "&7缺少: &f{0}", missing));
    }

    private void renderTerritoryList(Inventory inv) {
        List<TerritoryRecord> territories = module.getRepository().findByGuildId(guild.getId());
        for (int i = 0; i < LIST_SLOTS.length; i++) {
            if (i >= territories.size()) {
                break;
            }
            TerritoryRecord record = territories.get(i);
            Material icon = record.getWorldName().equalsIgnoreCase(viewer.getWorld().getName())
                    && module.getRepository().isLocalRecord(record)
                    ? Material.GREEN_BANNER
                    : Material.WHITE_BANNER;
            inv.setItem(LIST_SLOTS[i], createItem(icon,
                    texts().format(viewer, "module.territory.gui.list-item", "&f{0} @ {1}",
                            record.getServerId(), record.getWorldName()),
                    texts().format(viewer, "module.territory.info-line-region", "&7区域: &f{0}",
                            record.getRegionId()),
                    texts().format(viewer, "module.territory.gui.volume-line", "&7体积: &f{0} 方块",
                            formatVolume(record))));
        }
    }

    private ItemStack buildClaimButton() {
        boolean allowed = isClaimAllowedInCurrentWorld();
        Material material = allowed ? Material.EMERALD_BLOCK : Material.GRAY_STAINED_GLASS;
        String titleKey = allowed ? "module.territory.gui.claim" : "module.territory.gui.claim-disabled";
        String defaultTitle = allowed ? "&a声明领地" : "&8声明不可用";
        return createItem(material,
                texts().format(viewer, titleKey, defaultTitle),
                claimButtonLore(allowed));
    }

    private boolean isClaimAllowedInCurrentWorld() {
        TerritorySettings settings = module.getSettings();
        return settings == null || settings.isWorldAllowed(viewer.getWorld().getName());
    }

    private void sendWorldBlockedMessage(Player player) {
        TerritorySettings settings = module.getSettings();
        if (settings == null) {
            texts().send(player, "module.territory.world-blocked", "&c此世界不允许声明公会领地。");
            return;
        }
        TerritoryWorldClaimPolicy policy = settings.getWorldClaimPolicy();
        texts().send(player, policy.blockedMessageKey(), "&c此世界不允许声明公会领地。");
    }

    private String[] claimButtonLore(boolean allowed) {
        if (!allowed) {
            return new String[]{
                    texts().format(viewer, "module.territory.gui.world-blocked", "&c此世界不可声明领地")
            };
        }
        TerritorySettings settings = module.getSettings();
        double cost = settings != null ? settings.getClaimCost() : 0;
        if (cost > 0) {
            return new String[]{
                    texts().format(viewer, "module.territory.gui.claim-lore", "&7声明当前选区为公会领地"),
                    texts().format(viewer, "module.territory.gui.claim-cost", "&7费用: &f{0}", formatMoney(cost))
            };
        }
        return new String[]{
                texts().format(viewer, "module.territory.gui.claim-lore", "&7声明当前选区为公会领地")
        };
    }

    private Optional<TerritoryRecord> findTerritory(String worldName) {
        Optional<TerritoryRecord> record = module.getRepository().get(guild.getId(), worldName);
        if (record.isEmpty() && module.getBridge().isOperational()) {
            record = module.getBridge().findTerritory(guild.getId(), worldName);
        }
        return record;
    }

    private static long formatVolume(TerritoryRecord record) {
        long dx = record.getMaxX() - record.getMinX() + 1L;
        long dy = record.getMaxY() - record.getMinY() + 1L;
        long dz = record.getMaxZ() - record.getMinZ() + 1L;
        return dx * dy * dz;
    }

    private String formatPos(TerritorySelectionManager selections, boolean pos1) {
        if (selections == null) {
            return "-";
        }
        TerritorySelectionManager.Session session = selections.of(viewer);
        return formatLocation(pos1 ? session.pos1 : session.pos2);
    }

    private static String formatLocation(org.bukkit.Location loc) {
        if (loc == null) {
            return "-";
        }
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
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
}
