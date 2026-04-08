package com.guild.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.module.ModuleManager;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.language.LanguageManager;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guildplugin.util.FoliaTeleportUtils;

/**
 * 工会设置GUI - 支持多页布局
 * <p>
 * 布局设计：
 * <ul>
 *   <li><b>第1页</b>：保持原有布局不变（概览、文本编辑、成员管理、功能按钮等）</li>
 *   <li><b>第2页及以后</b>：展示模块注入的功能按钮（通过 {@link GUIExtensionHook} 注册的自动分配按钮）</li>
 * </ul>
 * <p>
 * 分页控制：
 * <ul>
 *   <li>槽位 45（左下角）= 上一页</li>
 *   <li>槽位 49（底部中央）= 返回主菜单</li>
 *   <li>槽位 53（右下角）= 下一页</li>
 * </ul>
 */
public class GuildSettingsGUI implements GUI {

    /** GUI 类型标识符（用于扩展点注册） */
    public static final String GUI_TYPE = "GuildSettingsGUI";

    /** 模块页面每页可容纳的最大按钮数（中间区域可用槽位数：7列x4行=28个） */
    private static final int MODULE_BUTTONS_PER_PAGE = 28;

    /** 模块按钮在额外页面上的可用槽位映射（从左到右、从上到下排列） */
    private static final int[] MODULE_SLOT_LAYOUT = {
        // 第2行 (row 1): 槽位10-16
        10, 11, 12, 13, 14, 15, 16,
        // 第3行 (row 2): 槽位19-25
        19, 20, 21, 22, 23, 24, 25,
        // 第4行 (row 3): 槽位28-34
        28, 29, 30, 31, 32, 33, 34,
        // 第5行 (row 4): 槽位37-43
        37, 38, 39, 40, 41, 42, 43
    };

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;

    /** 当前页码（从1开始，1=原始设置页） */
    private int currentPage = 1;
    /** 总页数（包含第1页 + 模块页面数） */
    private int totalPages = 1;

    public GuildSettingsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        // 初始化时计算总页数
        calculateTotalPages();
    }

    @Override
    public String getTitle() {
        String baseTitle = ColorUtils.colorize(languageManager.getMessage(player, "guild-settings-title",
                "&6工会设置 - {guild_name}", "{guild_name}",
                guild.getName() != null ? guild.getName() : "未知工会"));
        if (totalPages > 1) {
            baseTitle += ColorUtils.colorize(" &7(" +
                    languageManager.getIndexedMessage(
                            languageManager.getPlayerLanguage(player),
                            "gui.page-info", "第{0}页/共{1}页",
                            new String[]{String.valueOf(currentPage), String.valueOf(totalPages)}) + ")");
        }
        return baseTitle;
    }

    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        fillBorder(inventory);

        if (currentPage == 1) {
            // ===== 第1页：原始布局（保持不变） =====
            setupSettingsButtons(inventory);
            displayCurrentSettings(inventory);
            setupFunctionButtons(inventory);
            setupPaginationButtons(inventory); // 仅当有多页时显示翻页按钮
            fillInteriorSlots(inventory);
        } else {
            // ===== 模块页面：展示模块注入按钮 =====
            setupModulePage(inventory);
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
        int autoSlotCount = guiHook.getAutoSlotCount(GUI_TYPE);
        if (autoSlotCount <= 0) {
            totalPages = 1;
        } else {
            int modulePages = (int) Math.ceil((double) autoSlotCount / MODULE_BUTTONS_PER_PAGE);
            totalPages = 1 + modulePages; // 第1页(原有设置) + 模块页
        }
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 翻页按钮处理（优先级最高）
        if (slot == 45 && currentPage > 1) {
            currentPage--;
            plugin.getGuiManager().openGUI(player, this);
            return;
        }
        if (slot == 53 && currentPage < totalPages) {
            currentPage++;
            plugin.getGuiManager().openGUI(player, this);
            return;
        }

        // 返回主菜单按钮（所有页面通用）
        if (slot == 49) {
            plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
            return;
        }

        if (currentPage == 1) {
            // ===== 第1页：原有按钮处理逻辑 =====
            handleFirstPageClick(slot, clickType);
        } else {
            // ===== 模块页面：分发到对应模块按钮 =====
            handleModulePageClick(slot);
        }
    }

    // ==================== 第1页逻辑 ====================

    private void handleFirstPageClick(int slot, ClickType clickType) {
        switch (slot) {
            case 11: // 文本编辑
                if (clickType == ClickType.LEFT) handleChangeName(player);
                else if (clickType == ClickType.RIGHT) handleChangeDescription(player);
                else if (clickType == ClickType.SHIFT_LEFT) handleChangeTag(player);
                break;
            case 15: // 成员管理
                if (clickType == ClickType.LEFT) handleInviteMember(player);
                else if (clickType == ClickType.RIGHT) handleKickMember(player);
                else if (clickType == ClickType.SHIFT_LEFT) plugin.getGuiManager().openGUI(player, new PromoteMemberGUI(plugin, guild, player));
                break;
            case 13: // 设置工会家
                handleSetHome(player);
                break;
            case 28: // 申请管理
                if (clickType == ClickType.LEFT) handleApplications(player);
                break;
            case 29: // 关系管理
                if (clickType == ClickType.LEFT) handleRelations(player);
                break;
            case 31: // 工会日志
                if (clickType == ClickType.LEFT) handleGuildLogs(player);
                break;
            case 33: // 工会家传送
                handleHomeTeleport(player);
                break;
            case 34: // 离开
                handleLeaveGuild(player);
                break;
            case 36: // 删除
                handleDeleteGuild(player);
                break;
        }
    }

    // ==================== 模块页面逻辑 ====================

    /**
     * 渲染模块页面：展示当前页的所有模块注入按钮
     */
    private void setupModulePage(Inventory inventory) {
        GUIExtensionHook guiHook = getGuiHook();
        if (guiHook == null) return;

        // 获取当前页的自动分配注入项
        List<GUIExtensionHook.GUIInjectionSlot> pageInjections =
                guiHook.getPageInjections(GUI_TYPE, currentPage - 1, MODULE_BUTTONS_PER_PAGE);

        // 将注入项按顺序放置到预定义槽位上
        for (int i = 0; i < pageInjections.size() && i < MODULE_SLOT_LAYOUT.length; i++) {
            GUIExtensionHook.GUIInjectionSlot inj = pageInjections.get(i);
            int targetSlot = MODULE_SLOT_LAYOUT[i];
            inventory.setItem(targetSlot, inj.getItem());
        }

        // 用灰色玻璃填充未使用的槽位
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = pageInjections.size(); i < MODULE_SLOT_LAYOUT.length; i++) {
            int targetSlot = MODULE_SLOT_LAYOUT[i];
            if (inventory.getItem(targetSlot) == null) {
                inventory.setItem(targetSlot, filler);
            }
        }
    }

    /**
     * 处理模块页面上的点击事件
     */
    private void handleModulePageClick(int slot) {
        GUIExtensionHook guiHook = getGuiHook();
        if (guiHook == null) return;

        // 获取当前页的注入项
        List<GUIExtensionHook.GUIInjectionSlot> pageInjections =
                guiHook.getPageInjections(GUI_TYPE, currentPage - 1, MODULE_BUTTONS_PER_PAGE);

        // 查找点击的槽位对应的注入项
        for (int i = 0; i < pageInjections.size() && i < MODULE_SLOT_LAYOUT.length; i++) {
            if (MODULE_SLOT_LAYOUT[i] == slot) {
                GUIExtensionHook.GUIInjectionSlot inj = pageInjections.get(i);
                // 调用模块注册的点击回调，传入 guild 作为上下文
                inj.getAction().onClick(player, guild);
                return;
            }
        }
    }

    // ==================== 共用UI组件 ====================

    /** 填充边框 */
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
     * 设置翻页按钮（仅当总页数 > 1 时才显示）
     * - 槽位45: 上一页（第1页时显示为禁用状态或隐藏）
     * - 槽位53: 下一页（最后一页时显示为禁用状态或隐藏）
     */
    private void setupPaginationButtons(Inventory inventory) {
        if (totalPages <= 1) return;

        // 上一页按钮 (slot 45)
        if (currentPage > 1) {
            ItemStack prevItem = createItem(Material.ARROW,
                    languageManager.getMessage(player, "gui.previous-page", "&e&l上一页"),
                    false,
                    languageManager.getMessage(player, "gui.previous-page-hint", "&7点击返回上一页"));
            inventory.setItem(45, prevItem);
        } else {
            // 第一页时显示灰色的不可用箭头
            ItemStack disabledPrev = createItem(Material.GRAY_DYE,
                    languageManager.getMessage(player, "gui.no-previous", "&7上一页"), false,
                    languageManager.getMessage(player, "gui.already-first-page", "&7已经是第一页"));
            inventory.setItem(45, disabledPrev);
        }

        // 下一页按钮 (slot 53)
        if (currentPage < totalPages) {
            ItemStack nextItem = createItem(Material.ARROW,
                    languageManager.getMessage(player, "gui.next-page", "&e&l下一页"),
                    false,
                    languageManager.getMessage(player, "gui.next-page-hint", "&7点击查看更多功能"));
            inventory.setItem(53, nextItem);
        } else {
            // 最后一页时显示灰色的不可用箭头
            ItemStack disabledNext = createItem(Material.GRAY_DYE,
                    languageManager.getMessage(player, "gui.no-next", "&7下一页"), false,
                    languageManager.getMessage(player, "gui.already-last-page", "&7已经是最后一页"));
            inventory.setItem(53, disabledNext);
        }
    }

    /**
     * 返回主菜单按钮 (slot 49)
     */
    private void setupBackButton(Inventory inventory) {
        ItemStack back = createItem(Material.ARROW,
                languageManager.getMessage(player, "guild-settings.back", "&7返回"), false,
                languageManager.getMessage(player, "guild-settings.back-desc", "&7单击 &f返回主菜单"));
        inventory.setItem(49, back);
    }

    /** 设置设置按钮（简约化） - 仅第1页使用 */
    private void setupSettingsButtons(Inventory inventory) {
        ItemStack textEdit = createItem(Material.WRITABLE_BOOK,
            languageManager.getMessage(player, "guild-settings.text-edit", "&e文本编辑"), false,
            languageManager.getMessage(player, "guild-settings.text-edit-desc-left", "&7左键 &f修改名称"),
            languageManager.getMessage(player, "guild-settings.text-edit-desc-right", "&7右键 &f修改描述"),
            languageManager.getMessage(player, "guild-settings.text-edit-desc-shift-left", "&7Shift+左键 &f修改标签"));
        inventory.setItem(11, textEdit);

        ItemStack memberMgmt = createItem(Material.SHIELD,
            languageManager.getMessage(player, "guild-settings.member-mgmt", "&a成员管理"), false,
            languageManager.getMessage(player, "guild-settings.member-mgmt-desc-left", "&7左键 &f邀请成员"),
            languageManager.getMessage(player, "guild-settings.member-mgmt-desc-right", "&7右键 &f踢出成员"),
            languageManager.getMessage(player, "guild-settings.member-mgmt-desc-shift-left", "&7Shift+左键 &f提升/降级"));
        inventory.setItem(15, memberMgmt);

        ItemStack setHome = createItem(Material.COMPASS,
            languageManager.getMessage(player, "guild-settings.set-home", "&b设置工会家"), false,
            languageManager.getMessage(player, "guild-settings.set-home-desc", "&7单击 &f设置工会家"));
        inventory.setItem(13, setHome);
    }

    /** 设置功能按钮 - 仅第1页使用 */
    private void setupFunctionButtons(Inventory inventory) {
        ItemStack applications = createItem(Material.PAPER,
            languageManager.getMessage(player, "guild-settings.applications", "&e申请管理"), false,
            languageManager.getMessage(player, "guild-settings.applications-desc", "&7单击 &f处理加入申请"));
        inventory.setItem(28, applications);

        ItemStack relations = createItem(Material.RED_WOOL,
            languageManager.getMessage(player, "guild-settings.relations", "&e关系管理"), false,
            languageManager.getMessage(player, "guild-settings.relations-desc", "&7单击 &f管理工会关系"));
        inventory.setItem(29, relations);

        ItemStack guildLogs = createItem(Material.BOOK,
            languageManager.getMessage(player, "guild-settings.logs", "&6工会日志"), false,
            languageManager.getMessage(player, "guild-settings.logs-desc", "&7单击 &f查看工会日志"));
        inventory.setItem(31, guildLogs);

        ItemStack homeTeleport = createItem(Material.ENDER_PEARL,
            languageManager.getMessage(player, "guild-settings.home-teleport", "&d工会家传送"), false,
            languageManager.getMessage(player, "guild-settings.home-teleport-desc", "&7单击 &f传送到工会家"));
        inventory.setItem(33, homeTeleport);

        ItemStack leaveGuild = createItem(Material.BARRIER,
            languageManager.getMessage(player, "guild-settings.leave", "&c离开工会"), false,
            languageManager.getMessage(player, "guild-settings.leave-desc", "&7单击 &f离开当前工会"));
        inventory.setItem(34, leaveGuild);

        ItemStack deleteGuild = createItem(Material.TNT,
            languageManager.getMessage(player, "guild-settings.delete", "&4删除工会"), false,
            languageManager.getMessage(player, "guild-settings.delete-desc", "&7单击 &f删除当前工会"));
        inventory.setItem(36, deleteGuild);

        // 返回主菜单（第1页也显示）
        setupBackButton(inventory);
    }
    
    /** 显示当前设置信息 - 仅第1页使用 */
    private void displayCurrentSettings(Inventory inventory) {
        String name = guild.getName() != null ? guild.getName() :
            languageManager.getMessage(player, "guild-settings.overview-no-name", "无名称");
        String tag = guild.getTag() != null ? "[" + guild.getTag() + "]" :
            languageManager.getMessage(player, "guild-settings.overview-no-tag", "无标签");
        String desc = guild.getDescription() != null ? guild.getDescription() :
            languageManager.getMessage(player, "guild-settings.overview-no-desc", "无描述");
        String homeStatus = guild.hasHome() ?
            languageManager.getMessage(player, "guild-settings.overview-home-set", "&a已设置") :
            languageManager.getMessage(player, "guild-settings.overview-home-not-set", "&c未设置");

        ItemStack overview = createItem(Material.PAPER,
            languageManager.getMessage(player, "guild-settings.overview", "&6工会概览"), false,
            languageManager.getMessage(player, "guild-settings.overview-name", "&7名称: &e{name}", "{name}", name),
            languageManager.getMessage(player, "guild-settings.overview-tag", "&7标签: &e{tag}", "{tag}", tag),
            languageManager.getMessage(player, "guild-settings.overview-desc", "&7描述: &7{desc}", "{desc}", desc),
            languageManager.getMessage(player, "guild-settings.overview-home", "&7工会家: {home}", "{home}", homeStatus)
        );
        inventory.setItem(10, overview);
    }

    /** 填充内部空槽 - 仅第1页使用 */
    private void fillInteriorSlots(Inventory inventory) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 9; slot <= 44; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) continue;
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }

    // ==================== 业务处理方法（原有逻辑不变） ====================

    private void handleChangeName(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = languageManager.getMessage(player, "gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildNameInputGUI(plugin, guild, player));
    }

    private void handleChangeDescription(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = languageManager.getMessage(player, "gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildDescriptionInputGUI(plugin, guild, player));
    }

    private void handleChangeTag(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = languageManager.getMessage(player, "gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildTagInputGUI(plugin, guild, player));
    }

    private void handleInviteMember(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String msg = languageManager.getMessage(player, "gui.officer-or-higher", "&c需要官员或更高权限");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new InviteMemberGUI(plugin, guild, player));
    }

    private void handleKickMember(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String msg = languageManager.getMessage(player, "gui.officer-or-higher", "&c需要官员或更高权限");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new KickMemberGUI(plugin, guild, player));
    }

    private void handleSetHome(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = languageManager.getMessage(player, "gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuildService().setGuildHomeAsync(guild.getId(), player.getLocation(), player.getUniqueId()).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    String message = languageManager.getMessage(player, "sethome.success", "&a工会家设置成功！");
                    player.sendMessage(ColorUtils.colorize(message));
                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
                } else {
                    String message = languageManager.getMessage(player, "sethome.failed", "&c工会家设置失败！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    private void handleApplications(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String msg = languageManager.getMessage(player, "gui.officer-or-higher", "&c需要官员或更高权限");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new ApplicationManagementGUI(plugin, guild, player));
    }

    private void handleRelations(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = languageManager.getMessage(player, "relation.only-leader", "&c只有工会会长才能管理工会关系！");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildRelationsGUI(plugin, guild, player));
    }

    private void handleGuildLogs(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String msg = languageManager.getMessage(player, "gui.no-permission", "&c权限不足");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildLogsGUI(plugin, guild, player));
    }

    private void handleHomeTeleport(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String msg = languageManager.getMessage(player, "gui.no-permission", "&c权限不足");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuildService().getGuildHomeAsync(guild.getId()).thenAccept(location -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (location != null) {
                    FoliaTeleportUtils.safeTeleport(plugin, player, location);
                    String message = languageManager.getMessage(player, "home.success", "&a已传送到工会家！");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = languageManager.getMessage(player, "home.not-set", "&c工会家未设置！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    private void handleLeaveGuild(Player player) {
        plugin.getGuiManager().openGUI(player, new ConfirmLeaveGuildGUI(plugin, guild, player));
    }

    private void handleDeleteGuild(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = languageManager.getMessage(player, "gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild, player));
    }

    // ==================== 工具方法 ====================

    /**
     * 获取 GUI 扩展点 Hook 实例
     * 安全地获取，如果模块系统未初始化则返回null
     */
    private GUIExtensionHook getGuiHook() {
        ModuleManager moduleManager = plugin.getModuleManager();
        if (moduleManager == null) return null;
        return moduleManager.getRegistry().getGuiExtensionHook();
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        return createItem(material, name, false, lore);
    }

    private ItemStack createItem(Material material, String name, boolean glowing, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ColorUtils.colorize(line));
            }
            meta.setLore(coloredLore);
            if (glowing) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
