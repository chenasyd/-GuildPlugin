package com.guild.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.module.ModuleManager;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;

/**
 * 工会信息GUI - 支持模块扩展注入
 * <p>
 * 布局设计：
 * <ul>
 *   <li><b>第1页</b>：核心信息（概览/统计/经济/状态）+ 模块预留槽位</li>
 *   <li><b>第1页模块预留区域</b>：12-16, 21-25, 30-34, 39-43（共20个槽位）</li>
 *   <li><b>后续页面</b>：全部用于展示模块扩展内容</li>
 * </ul>
 */
public class GuildInfoGUI implements GUI {

    /** GUI 类型标识符（用于扩展点注册） */
    public static final String GUI_TYPE = "GuildInfoGUI";

    /** 第1页模块预留槽位映射（按顺序排列） */
    private static final int[] PAGE1_MODULE_SLOTS = {
        // Row 2: 12-16 (跳过10=summary, 11=空)
        12, 13, 14, 15, 16,
        // Row 3: 21-25 (跳过19=stats, 20=空)
        21, 22, 23, 24, 25,
        // Row 4: 30-34 (跳过28=economy, 29=空)
        30, 31, 32, 33, 34,
        // Row 5: 39-43 (跳过36=status, 37-38=空)
        39, 40, 41, 42, 43
    };

    /** 后续页面可用槽位布局（与 GuildSettingsGUI 一致的中间区域） */
    private static final int[] EXTRA_PAGE_SLOT_LAYOUT = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private static final int MODULE_BUTTONS_PER_PAGE = EXTRA_PAGE_SLOT_LAYOUT.length; // 28

    private final GuildPlugin plugin;
    private final Player player;
    private final Guild guild;
    private Inventory inventory;
    
    /** 当前页码 */
    private int currentPage = 1;
    /** 总页数 */
    private int totalPages = 1;
    /**
     * 第1页模块固定槽位的紧凑排列映射
     * key = 实际放置到的槽位号, value = 对应的注入项
     * 每次刷新时重建，用于点击分发
     */
    private Map<Integer, GUIExtensionHook.GUIInjectionSlot> fixedSlotMap;

    public GuildInfoGUI(GuildPlugin plugin, Player player, Guild guild) {
        this.plugin = plugin;
        this.player = player;
        this.guild = guild;
        calculateTotalPages();
    }

    @Override
    public String getTitle() {
        String baseTitle = ColorUtils.colorize(plugin.getLanguageManager().getMessage(player, "guild-info.title", "&6工会信息"));
        if (totalPages > 1) {
            baseTitle += ColorUtils.colorize(" &7(" +
                    plugin.getLanguageManager().getMessage(player, "gui.page-info",
                            "第{0}页/共{1}页",
                            String.valueOf(currentPage), String.valueOf(totalPages)) + ")");
        }
        return baseTitle;
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        this.inventory = inventory;
        fillBorder(inventory);

        if (currentPage == 1) {
            setupDefaultItems();
            // 渲染第1页的模块固定槽位注入项
            renderModuleFixedSlots(inventory);
            // 设置翻页按钮（如果有多页）
            if (totalPages > 1) {
                setupPaginationButtons(inventory);
            }
        } else {
            // 后续页面：展示自动分配槽位的模块按钮
            setupExtraPage(inventory);
            setupBackButton(inventory);
            setupPaginationButtons(inventory);
        }
    }

    /**
     * 计算总页数
     */
    private void calculateTotalPages() {
        GUIExtensionHook guiHook = getGuiHook();
        if (guiHook == null) {
            totalPages = 1;
            return;
        }
        // 固定槽位注入占用第1页，自动分配槽位注入占用额外页面
        int autoSlotCount = guiHook.getAutoSlotCount(GUI_TYPE);
        if (autoSlotCount <= 0) {
            totalPages = 1;
        } else {
            int extraPages = (int) Math.ceil((double) autoSlotCount / MODULE_BUTTONS_PER_PAGE);
            totalPages = 1 + extraPages;
        }
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 翻页处理
        if (slot == 45 && currentPage > 1) {
            currentPage--;
            refresh(player);
            return;
        }
        if (slot == 53 && currentPage < totalPages) {
            currentPage++;
            refresh(player);
            return;
        }

        if (currentPage == 1) {
            // 第1页：原有逻辑 + 模块固定槽位点击分发
            if (slot == 49) {
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                return;
            }
            // 分发到模块固定槽位
            dispatchToModuleFixedSlot(slot);
        } else {
            // 额外页面：返回 + 模块点击
            if (slot == 49) {
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                return;
            }
            dispatchToModuleAutoSlot(slot);
        }
    }

    // ==================== 第1页布局 ====================

    private void setupDefaultItems() {
        // 合并展示：名称/标签/描述/创建时间/会长
        String createdTime = guild.getCreatedAt() != null
            ? guild.getCreatedAt().format(com.guild.core.time.TimeProvider.FULL_FORMATTER)
            : "未知";

        List<String> summaryLore = new ArrayList<>();
        summaryLore.add(ColorUtils.colorize("&7" +
            plugin.getLanguageManager().getMessage(player, "guild-info.tag", "标签") +
            ": " + (guild.getTag() != null ? "[" + guild.getTag() + "]" :
            plugin.getLanguageManager().getMessage(player, "guild-info.no-tag", "无"))));
        if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
            summaryLore.add(ColorUtils.colorize("&7" +
                plugin.getLanguageManager().getMessage(player, "guild-info.description", "描述") +
                ": " + guild.getDescription()));
        }
        summaryLore.add(ColorUtils.colorize("&7" +
            plugin.getLanguageManager().getMessage(player, "guild-info.leader", "会长") +
            ": &e" + guild.getLeaderName()));
        summaryLore.add(ColorUtils.colorize("&7" +
            plugin.getLanguageManager().getMessage(player, "guild-info.created-time", "创建时间") +
            ": " + createdTime));

        ItemStack summaryItem = createItem(Material.PAPER,
            ColorUtils.colorize("&6" + guild.getName()),
            summaryLore.toArray(new String[0]));
        inventory.setItem(10, summaryItem);

        // 统计
        ItemStack statsItem = createItem(
            Material.EXPERIENCE_BOTTLE,
            ColorUtils.colorize("&e" + plugin.getLanguageManager().getMessage(player, "guild-info.stats-title", "工会统计")),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.level", "等级") + ": &e" + guild.getLevel()),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.members", "成员") + ": &e" + plugin.getLanguageManager().getMessage(player, "guild-info.loading", "加载中...")),
            getProgressBar(guild.getLevel(), guild.getBalance(), 8)
        );
        inventory.setItem(19, statsItem);

        // 经济
        ItemStack economyItem = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize("&6" + plugin.getLanguageManager().getMessage(player, "guild-info.economy-title", "经济信息")),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.balance", "资金") + ": &a" + plugin.getEconomyManager().format(guild.getBalance())),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.next-level-requirement", "下级所需") + ": " + getNextLevelRequirement(guild.getLevel())),
            getProgressBar(guild.getLevel(), guild.getBalance(), 8)
        );
        inventory.setItem(28, economyItem);

        // 状态
        String status = guild.isFrozen()
            ? "\u00a7c" + plugin.getLanguageManager().getMessage(player, "guild-info.status-frozen", "已冻结")
            : "\u00a7a" + plugin.getLanguageManager().getMessage(player, "guild-info.status-normal", "正常");
        ItemStack statusItem = createItem(Material.BEACON,
            "\u00a76" + plugin.getLanguageManager().getMessage(player, "guild-info.status", "工会状态"),
            status);
        inventory.setItem(36, statusItem);

        // 返回按钮
        ItemStack backItem = createItem(
            Material.ARROW,
            "\u00a7c" + plugin.getLanguageManager().getMessage(player, "guild-info.back", "返回"),
            "\u00a7e" + plugin.getLanguageManager().getMessage(player, "guild-info.back-hint", "点击返回主菜单")
        );
        inventory.setItem(49, backItem);

        fillInteriorSlots(inventory);

        // 异步刷新动态信息
        plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(memberCount -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (inventory == null) return;

                ItemStack updatedStats = createItem(
                    Material.EXPERIENCE_BOTTLE,
                    ColorUtils.colorize("&e" + plugin.getLanguageManager().getMessage(player, "guild-info.stats-title", "工会统计")),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.level", "等级") + ": &e" + guild.getLevel()),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.members", "成员") + ": &e" + memberCount + "/" + guild.getMaxMembers() + " " + plugin.getLanguageManager().getMessage(player, "guild-info.people", "人")),
                    getProgressBar(guild.getLevel(), guild.getBalance(), 8)
                );
                inventory.setItem(19, updatedStats);

                ItemStack updatedEconomy = createItem(
                    Material.GOLD_INGOT,
                    ColorUtils.colorize("&6" + plugin.getLanguageManager().getMessage(player, "guild-info.economy-title", "经济信息")),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.balance", "资金") + ": &a" + plugin.getEconomyManager().format(guild.getBalance())),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.next-level-requirement", "下级所需") + ": " + getNextLevelRequirement(guild.getLevel())),
                    getProgressBar(guild.getLevel(), guild.getBalance(), 8)
                );
                inventory.setItem(28, updatedEconomy);
            });
        });
    }

    /**
     * 渲染第1页的模块固定槽位注入项（紧凑排列）
     * <p>
     * 剩余模块从 PAGE1_MODULE_SLOTS[0] 开始向前紧凑排列，
     * 模块卸载后不会留下空隙，后续槽位由 fillInteriorSlots 填充背景
     */
    private void renderModuleFixedSlots(Inventory inv) {
        GUIExtensionHook guiHook = getGuiHook();
        if (guiHook == null) return;

        List<GUIExtensionHook.GUIInjectionSlot> fixedInjections =
                guiHook.getFixedSlotInjections(GUI_TYPE);

        if (fixedInjections.isEmpty()) return;

        fixedSlotMap = new HashMap<>();
        int slotIndex = 0;
        for (GUIExtensionHook.GUIInjectionSlot inj : fixedInjections) {
            if (slotIndex >= PAGE1_MODULE_SLOTS.length) break;
            int targetSlot = PAGE1_MODULE_SLOTS[slotIndex];
            inv.setItem(targetSlot, inj.getItem());
            fixedSlotMap.put(targetSlot, inj);
            slotIndex++;
        }
    }

    /**
     * 分发第1页点击到模块固定槽位（基于紧凑排列映射）
     */
    private void dispatchToModuleFixedSlot(int slot) {
        if (fixedSlotMap == null || fixedSlotMap.isEmpty()) return;
        GUIExtensionHook.GUIInjectionSlot inj = fixedSlotMap.get(slot);
        if (inj != null) {
            inj.getAction().onClick(player, guild);
        }
    }

    // ==================== 额外页面布局 ====================

    /**
     * 渲染额外页面（模块扩展内容）
     */
    private void setupExtraPage(Inventory inv) {
        GUIExtensionHook guiHook = getGuiHook();
        if (guiHook == null) return;

        // 当前页对应的自动分配注入项索引偏移
        int pageIndex = currentPage - 2; // 减去第1页
        List<GUIExtensionHook.GUIInjectionSlot> pageInjections =
                guiHook.getPageInjections(GUI_TYPE, pageIndex + 1, MODULE_BUTTONS_PER_PAGE);

        // 放置到预定义槽位上
        for (int i = 0; i < pageInjections.size() && i < EXTRA_PAGE_SLOT_LAYOUT.length; i++) {
            GUIExtensionHook.GUIInjectionSlot inj = pageInjections.get(i);
            inv.setItem(EXTRA_PAGE_SLOT_LAYOUT[i], inj.getItem());
        }

        // 填充未使用槽位
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = pageInjections.size(); i < EXTRA_PAGE_SLOT_LAYOUT.length; i++) {
            if (inv.getItem(EXTRA_PAGE_SLOT_LAYOUT[i]) == null) {
                inv.setItem(EXTRA_PAGE_SLOT_LAYOUT[i], filler);
            }
        }
    }

    /**
     * 分发额外页面点击到模块自动槽位
     */
    private void dispatchToModuleAutoSlot(int slot) {
        GUIExtensionHook guiHook = getGuiHook();
        if (guiHook == null) return;

        int pageIndex = currentPage - 2;
        List<GUIExtensionHook.GUIInjectionSlot> pageInjections =
                guiHook.getPageInjections(GUI_TYPE, pageIndex + 1, MODULE_BUTTONS_PER_PAGE);

        for (int i = 0; i < pageInjections.size() && i < EXTRA_PAGE_SLOT_LAYOUT.length; i++) {
            if (EXTRA_PAGE_SLOT_LAYOUT[i] == slot) {
                pageInjections.get(i).getAction().onClick(player, guild);
                return;
            }
        }
    }

    // ==================== 共用UI组件 ====================

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

    private void fillInteriorSlots(Inventory inventory) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 9; slot <= 44; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) continue;
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }

    private void setupPaginationButtons(Inventory inventory) {
        // 上一页 (slot 45)
        if (currentPage > 1) {
            ItemStack prev = createItem(Material.ARROW,
                plugin.getLanguageManager().getMessage(player, "gui.previous-page", "&e&l上一页"),
                plugin.getLanguageManager().getMessage(player, "gui.previous-page-hint", "&7点击返回上一页"));
            inventory.setItem(45, prev);
        } else {
            ItemStack disabled = createItem(Material.GRAY_DYE,
                plugin.getLanguageManager().getMessage(player, "gui.no-previous", "&7上一页"),
                plugin.getLanguageManager().getMessage(player, "gui.already-first-page", "&7已经是第一页"));
            inventory.setItem(45, disabled);
        }

        // 下一页 (slot 53)
        if (currentPage < totalPages) {
            ItemStack next = createItem(Material.ARROW,
                plugin.getLanguageManager().getMessage(player, "gui.next-page", "&e&l下一页"),
                plugin.getLanguageManager().getMessage(player, "gui.next-page-hint", "&7点击查看更多"));
            inventory.setItem(53, next);
        } else {
            ItemStack disabled = createItem(Material.GRAY_DYE,
                plugin.getLanguageManager().getMessage(player, "gui.no-next", "&7下一页"),
                plugin.getLanguageManager().getMessage(player, "gui.already-last-page", "&7已经是最后一页"));
            inventory.setItem(53, disabled);
        }
    }

    private void setupBackButton(Inventory inventory) {
        ItemStack back = createItem(Material.ARROW,
                "\u00a7c" + plugin.getLanguageManager().getMessage(player, "guild-info.back", "返回"),
                "\u00a7e" + plugin.getLanguageManager().getMessage(player, "guild-info.back-hint", "点击返回主菜单"));
        inventory.setItem(49, back);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ColorUtils.colorize(line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    @Override
    public void onClose(Player player) {}
    
    @Override
    public void refresh(Player player) {
        setupInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }

    // ==================== 工具方法 ====================

    private GUIExtensionHook getGuiHook() {
        ModuleManager moduleManager = plugin.getModuleManager();
        if (moduleManager == null) return null;
        return moduleManager.getRegistry().getGuiExtensionHook();
    }

    private String replacePlaceholders(String text) {
        return PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);
    }

    private String replacePlaceholdersAsync(String text, int memberCount) {
        String result = PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);
        return result
            .replace("{member_count}", String.valueOf(memberCount))
            .replace("{online_member_count}", String.valueOf(memberCount)); 
    }
    
    private String getNextLevelRequirement(int currentLevel) {
        if (currentLevel >= 10) {
            return plugin.getLanguageManager().getMessage(player, "guild-info.max-level-reached", "已达到最高等级");
        }
        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
        }
        return plugin.getEconomyManager().format(required);
    }

    private String getLevelProgress(int currentLevel, double currentBalance) {
        if (currentLevel >= 10) return "100%";
        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break; case 2: required = 10000; break;
            case 3: required = 20000; break; case 4: required = 35000; break;
            case 5: required = 50000; break; case 6: required = 75000; break;
            case 7: required = 100000; break; case 8: required = 150000; break;
            case 9: required = 200000; break;
        }
        if (required <= 0) return "0.0%";
        double percentage = (currentBalance / required) * 100;
        if (percentage > 100) percentage = 100;
        return String.format("%.1f%%", percentage);
    }

    private String getProgressBar(int currentLevel, double currentBalance, int length) {
        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break; case 2: required = 10000; break;
            case 3: required = 20000; break; case 4: required = 35000; break;
            case 5: required = 50000; break; case 6: required = 75000; break;
            case 7: required = 100000; break; case 8: required = 150000; break;
            case 9: required = 200000; break;
            default: required = 1; break;
        }
        if (required <= 0) required = 1;
        double percent = Math.min(100.0, (currentBalance / required) * 100.0);
        int filled = (int) Math.round((percent / 100.0) * length);
        StringBuilder sb = new StringBuilder();
        sb.append(ColorUtils.colorize("&7["));
        for (int i = 0; i < length; i++) {
            if (i < filled) sb.append("\u00a7a\u25a0"); else sb.append("\u00a77\u25a0");
        }
        sb.append(ColorUtils.colorize("&7] "));
        sb.append(String.format("%.1f%%", percent));
        return sb.toString();
    }
}
