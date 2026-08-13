package com.guild.gui;

import java.util.ArrayList;
import java.util.Collections;
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
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.core.language.LanguageManager;

import org.geysermc.cumulus.form.SimpleForm;
import com.guild.models.Guild;

/**
 * 工会信息GUI - 支持模块扩展注入
 * <p>
 * 布局设计：
 * <ul>
 *   <li><b>第1页</b>：核心信息（概览/统计/经济/状态）+ 模块预留槽位</li>
 *   <li><b>第1页模块预留区域</b>：12-16, 21-25, 30-34, 39-43（共20个槽位）</li>
 *   <li><b>模块排布</b>：所有模块按钮先紧凑填满第1页预留槽；占满后才分页</li>
 *   <li><b>后续页面</b>：仅展示溢出模块；未使用的 EXTRA_PAGE_SLOT_LAYOUT 槽位留空</li>
 * </ul>
 */
public class GuildInfoGUI implements GUI {

    /** GUI 类型标识符（用于扩展点注册） */
    public static final String GUI_TYPE = "GuildInfoGUI";

    // ── 图像模式功能常量 ──
    public static final String FUNC_SUMMARY = "SUMMARY";
    public static final String FUNC_STATS = "STATS";
    public static final String FUNC_ECONOMY = "ECONOMY";
    public static final String FUNC_STATUS = "STATUS";
    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_BACK = "BACK";

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
        String baseTitle = ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.title", "&6工会信息"));
        if (totalPages > 1) {
            baseTitle += ColorUtils.colorize(" &7(" +
                    plugin.getLanguageManager().getGuiIndexedMessage(player, "gui.common.page-info",
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
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;

        // 异步获取成员数量后构建表单
        plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(memberCount -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (!player.isOnline()) return;

                LanguageManager lang = plugin.getLanguageManager();

                String createdTime = guild.getCreatedAt() != null
                        ? guild.getCreatedAt().format(com.guild.core.time.TimeProvider.FULL_FORMATTER)
                        : lang.getGuiColoredMessage(player, "gui.common.unknown", "未知");
                String tagText = guild.getTag() != null ? "[" + guild.getTag() + "]" : lang.getGuiColoredMessage(player, "gui.guild-info.no-tag", "无");
                String statusText = guild.isFrozen() ? "§c" + lang.getGuiColoredMessage(player, "gui.guild-info.status-frozen", "已冻结") : "§a" + lang.getGuiColoredMessage(player, "gui.guild-info.status-normal", "正常");

                StringBuilder sb = new StringBuilder();
                sb.append("§6").append(guild.getName()).append("\n");
                sb.append("§f").append(lang.getGuiColoredMessage(player, "gui.guild-info.tag", "标签")).append(": §e").append(tagText).append("\n");
                if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
                    sb.append("§f").append(lang.getGuiColoredMessage(player, "gui.guild-info.description", "描述")).append(": ").append(guild.getDescription()).append("\n");
                }
                sb.append("§f").append(lang.getGuiColoredMessage(player, "gui.guild-info.leader", "会长")).append(": §e").append(guild.getLeaderName()).append("\n");
                sb.append("§f").append(lang.getGuiColoredMessage(player, "gui.guild-info.created-time", "创建时间")).append(": ").append(createdTime).append("\n");
                sb.append("§f").append(lang.getGuiColoredMessage(player, "gui.guild-info.level", "等级")).append(": §e").append(guild.getLevel()).append("\n");
                sb.append("§f").append(lang.getGuiColoredMessage(player, "gui.guild-info.members", "成员")).append(": §e").append(memberCount).append("/")
                        .append(guild.getMaxMembers()).append("\n");
                sb.append("§f").append(lang.getGuiColoredMessage(player, "gui.guild-info.balance", "资金")).append(": §a")
                        .append(plugin.getEconomyManager().format(guild.getBalance())).append("\n");
                sb.append("§f").append(lang.getGuiColoredMessage(player, "gui.guild-info.next-level-requirement", "下级所需")).append(": ").append(getNextLevelRequirement(guild.getLevel())).append("\n");
                sb.append("§f").append(lang.getGuiColoredMessage(player, "gui.guild-info.bedrock-status-label", "状态")).append(": ").append(statusText);

                SimpleForm form = SimpleForm.builder()
                        .title(lang.getGuiColoredMessage(player, "gui.guild-info.bedrock-title", "&6工会信息"))
                        .content(sb.toString())
                        .button(lang.getGuiColoredMessage(player, "gui.guild-info.bedrock-guild-funds", "&a工会资金"))
                        .button(lang.getGuiColoredMessage(player, "gui.guild-info.bedrock-back-to-main", "&c返回主菜单"))
                        .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                            switch (response.clickedButtonId()) {
                                case 0 -> plugin.getGuiManager().openGUI(player,
                                        new GuildFundsGUI(plugin, guild, player, 0, "GuildInfoGUI"));
                                case 1 -> plugin.getGuiManager().openGUI(player,
                                        new MainGuildGUI(plugin, player));
                            }
                        }))
                        .build();

                BedrockFormSender.sendForm(player.getUniqueId(), form);
            });
        });

        return true; // 异步处理，已接管
    }

    @Override
    public void setupInventory(Inventory inventory) {
        this.inventory = inventory;
        calculateTotalPages();
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }
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

        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }

    /**
     * 计算总页数：仅当模块按钮超过第1页预留槽位后才产生额外页。
     */
    private void calculateTotalPages() {
        GUIExtensionHook guiHook = getGuiHook();
        if (guiHook == null) {
            totalPages = 1;
            return;
        }
        int totalModules = guiHook.getInjectionCount(GUI_TYPE);
        if (totalModules <= PAGE1_MODULE_SLOTS.length) {
            totalPages = 1;
            return;
        }
        int overflow = totalModules - PAGE1_MODULE_SLOTS.length;
        int extraPages = (int) Math.ceil((double) overflow / MODULE_BUTTONS_PER_PAGE);
        totalPages = 1 + Math.max(0, extraPages);
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
            if (slot == 28) {
                plugin.getGuiManager().openGUI(player, new GuildFundsGUI(plugin, guild, player, 0, "GuildInfoGUI"));
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
            plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.tag", "标签") +
            ": " + (guild.getTag() != null ? "&7[" + guild.getTag() + "&7]" :
            plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.no-tag", "无"))));
        if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
            summaryLore.add(ColorUtils.colorize("&7" +
                plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.description", "描述") +
                ": " + guild.getDescription()));
        }
        summaryLore.add(ColorUtils.colorize("&7" +
            plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.leader", "会长") +
            ": &e" + guild.getLeaderName()));
        summaryLore.add(ColorUtils.colorize("&7" +
            plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.created-time", "创建时间") +
            ": " + createdTime));

        ItemStack summaryItem = createItem(Material.PAPER,
            ColorUtils.colorize("&6" + guild.getName()),
            summaryLore.toArray(new String[0]));
        inventory.setItem(10, summaryItem);

        // 统计
        ItemStack statsItem = createItem(
            Material.EXPERIENCE_BOTTLE,
            ColorUtils.colorize("&e" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.stats-title", "工会统计")),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.level", "等级") + ": &e" + guild.getLevel()),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.members", "成员") + ": &e" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.loading", "加载中...")),
            getProgressBar(guild.getLevel(), guild.getBalance(), 8)
        );
        inventory.setItem(19, statsItem);

        // 经济
        ItemStack economyItem = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize("&6" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.economy-title", "经济信息")),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.balance", "资金") + ": &a" + plugin.getEconomyManager().format(guild.getBalance())),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.next-level-requirement", "下级所需") + ": " + getNextLevelRequirement(guild.getLevel())),
            getProgressBar(guild.getLevel(), guild.getBalance(), 8),
            "",
            ColorUtils.colorize("&e" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.funds-hint", "单击查看成员存款详情"))
        );
        inventory.setItem(28, economyItem);

        // 状态
        String status = guild.isFrozen()
            ? "\u00a7c" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.status-frozen", "已冻结")
            : "\u00a7a" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.status-normal", "正常");
        ItemStack statusItem = createItem(Material.BEACON,
            "\u00a76" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.status", "工会状态"),
            status);
        inventory.setItem(36, statusItem);

        // 返回按钮
        ItemStack backItem = createItem(
            Material.ARROW,
            "\u00a7c" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.back", "返回"),
            "\u00a7e" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.back-hint", "点击返回主菜单")
        );
        inventory.setItem(49, backItem);

        fillInteriorSlots(inventory);

        // 异步刷新动态信息
        plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(memberCount -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (inventory == null) return;

                ItemStack updatedStats = createItem(
                    Material.EXPERIENCE_BOTTLE,
                    ColorUtils.colorize("&e" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.stats-title", "工会统计")),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.level", "等级") + ": &e" + guild.getLevel()),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.members", "成员") + ": &e" + memberCount + "/" + guild.getMaxMembers() + " " + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.people", "人")),
                    getProgressBar(guild.getLevel(), guild.getBalance(), 8)
                );
                inventory.setItem(19, updatedStats);

                ItemStack updatedEconomy = createItem(
                    Material.GOLD_INGOT,
                    ColorUtils.colorize("&6" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.economy-title", "经济信息")),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.balance", "资金") + ": &a" + plugin.getEconomyManager().format(guild.getBalance())),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.next-level-requirement", "下级所需") + ": " + getNextLevelRequirement(guild.getLevel())),
                    getProgressBar(guild.getLevel(), guild.getBalance(), 8),
                    "",
                    ColorUtils.colorize("&e" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.funds-hint", "单击查看成员存款详情"))
                );
                inventory.setItem(28, updatedEconomy);

                // 异步刷新后重新应用图像模式（将新放置的物品转换为透明载体）
                plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
            });
        });
    }

    /**
     * 渲染第1页的模块按钮（紧凑填满 PAGE1_MODULE_SLOTS）。
     * <p>
     * 固定槽位与自动槽位一并按 moduleId 排序后从左到右填充；
     * 仅当预留槽位占满后，溢出部分才进入额外分页。
     */
    private void renderModuleFixedSlots(Inventory inv) {
        GUIExtensionHook guiHook = getGuiHook();
        if (guiHook == null) return;

        List<GUIExtensionHook.GUIInjectionSlot> all = guiHook.getInjections(GUI_TYPE);
        if (all.isEmpty()) return;

        fixedSlotMap = new HashMap<>();
        int limit = Math.min(all.size(), PAGE1_MODULE_SLOTS.length);
        for (int i = 0; i < limit; i++) {
            GUIExtensionHook.GUIInjectionSlot inj = all.get(i);
            int targetSlot = PAGE1_MODULE_SLOTS[i];
            inv.setItem(targetSlot, inj.getDisplayItem(player, plugin.getLanguageManager()));
            fixedSlotMap.put(targetSlot, inj);
        }
    }

    /**
     * 分发第1页点击到模块槽位（基于紧凑排列映射）
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
     * 渲染额外页面（第1页预留槽位溢出的模块按钮）。
     * 未使用的 EXTRA_PAGE_SLOT_LAYOUT 槽位保持空白，不填充灰板。
     */
    private void setupExtraPage(Inventory inv) {
        GUIExtensionHook guiHook = getGuiHook();
        if (guiHook == null) return;

        List<GUIExtensionHook.GUIInjectionSlot> overflow = getOverflowInjections(guiHook);
        if (overflow.isEmpty()) return;

        int pageIndex = currentPage - 2; // 0 = 第一张额外页
        if (pageIndex < 0) return;

        int from = pageIndex * MODULE_BUTTONS_PER_PAGE;
        if (from >= overflow.size()) return;
        int to = Math.min(from + MODULE_BUTTONS_PER_PAGE, overflow.size());

        for (int i = from; i < to; i++) {
            int layoutIndex = i - from;
            if (layoutIndex >= EXTRA_PAGE_SLOT_LAYOUT.length) break;
            GUIExtensionHook.GUIInjectionSlot inj = overflow.get(i);
            inv.setItem(EXTRA_PAGE_SLOT_LAYOUT[layoutIndex],
                    inj.getDisplayItem(player, plugin.getLanguageManager()));
        }
        // 故意不填充未使用的 EXTRA_PAGE_SLOT_LAYOUT — 保持留空
    }

    /**
     * 分发额外页面点击到溢出模块按钮
     */
    private void dispatchToModuleAutoSlot(int slot) {
        GUIExtensionHook guiHook = getGuiHook();
        if (guiHook == null) return;

        List<GUIExtensionHook.GUIInjectionSlot> overflow = getOverflowInjections(guiHook);
        int pageIndex = currentPage - 2;
        if (pageIndex < 0) return;

        int from = pageIndex * MODULE_BUTTONS_PER_PAGE;
        if (from >= overflow.size()) return;
        int to = Math.min(from + MODULE_BUTTONS_PER_PAGE, overflow.size());

        for (int i = from; i < to; i++) {
            int layoutIndex = i - from;
            if (layoutIndex >= EXTRA_PAGE_SLOT_LAYOUT.length) break;
            if (EXTRA_PAGE_SLOT_LAYOUT[layoutIndex] == slot) {
                overflow.get(i).getAction().onClick(player, guild);
                return;
            }
        }
    }

    /** 超出第1页预留槽位后的模块注入列表（已排序）。 */
    private List<GUIExtensionHook.GUIInjectionSlot> getOverflowInjections(GUIExtensionHook guiHook) {
        List<GUIExtensionHook.GUIInjectionSlot> all = guiHook.getInjections(GUI_TYPE);
        if (all.size() <= PAGE1_MODULE_SLOTS.length) {
            return Collections.emptyList();
        }
        return all.subList(PAGE1_MODULE_SLOTS.length, all.size());
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
                plugin.getLanguageManager().getGuiMessage(player, "gui.common.previous-page", "&e&l上一页"),
                plugin.getLanguageManager().getGuiMessage(player, "gui.common.previous-page-hint", "&7点击返回上一页"));
            inventory.setItem(45, prev);
        } else {
            ItemStack disabled = createItem(Material.GRAY_DYE,
                plugin.getLanguageManager().getGuiMessage(player, "gui.common.no-previous", "&7上一页"),
                plugin.getLanguageManager().getGuiMessage(player, "gui.common.already-first-page", "&7已经是第一页"));
            inventory.setItem(45, disabled);
        }

        // 下一页 (slot 53)
        if (currentPage < totalPages) {
            ItemStack next = createItem(Material.ARROW,
                plugin.getLanguageManager().getGuiMessage(player, "gui.common.next-page", "&e&l下一页"),
                plugin.getLanguageManager().getGuiMessage(player, "gui.common.next-page-hint", "&7点击查看更多"));
            inventory.setItem(53, next);
        } else {
            ItemStack disabled = createItem(Material.GRAY_DYE,
                plugin.getLanguageManager().getGuiMessage(player, "gui.common.no-next", "&7下一页"),
                plugin.getLanguageManager().getGuiMessage(player, "gui.common.already-last-page", "&7已经是最后一页"));
            inventory.setItem(53, disabled);
        }
    }

    private void setupBackButton(Inventory inventory) {
        ItemStack back = createItem(Material.ARROW,
                "\u00a7c" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.back", "返回"),
                "\u00a7e" + plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.back-hint", "点击返回主菜单"));
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
        if (currentLevel >= plugin.getMaxGuildLevel()) {
            return plugin.getLanguageManager().getGuiMessage(player, "gui.guild-info.max-level-reached", "已达到最高等级");
        }
        double required = plugin.getRequirementForNextLevel(currentLevel);
        return plugin.getEconomyManager().format(required);
    }

    private String getLevelProgress(int currentLevel, double currentBalance) {
        if (currentLevel >= plugin.getMaxGuildLevel()) return "100%";
        double required = plugin.getRequirementForNextLevel(currentLevel);
        if (required <= 0) return "0.0%";
        double percentage = (currentBalance / required) * 100;
        if (percentage > 100) percentage = 100;
        return String.format("%.1f%%", percentage);
    }

    private String getProgressBar(int currentLevel, double currentBalance, int length) {
        double required = plugin.getRequirementForNextLevel(currentLevel);
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
