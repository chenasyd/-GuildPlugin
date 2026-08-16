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
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;

import org.geysermc.cumulus.form.SimpleForm;
import com.guild.core.language.LanguageManager;
import com.guild.models.Guild;
import com.guild.models.GuildMember;

/**
 * 公会设置GUI - 支持多页布局
 * <p>
 * 布局设计：
 * <ul>
 *   <li><b>第1页</b>：保持原有布局（概览、文本编辑、转移会长、功能按钮等）</li>
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

    // ── 图像模式功能常量 ──
    public static final String FUNC_OVERVIEW = "OVERVIEW";
    public static final String FUNC_TEXT_EDIT = "TEXT_EDIT";
    public static final String FUNC_SET_HOME = "SET_HOME";
    public static final String FUNC_TRANSFER_LEADER = "TRANSFER_LEADER";
    public static final String FUNC_GUILD_FUNDS = "GUILD_FUNDS";
    public static final String FUNC_LOGS = "LOGS";
    public static final String FUNC_RESERVED = "RESERVED";
    public static final String FUNC_HOME_TELEPORT = "HOME_TELEPORT";
    public static final String FUNC_DELETE = "DELETE";
    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_BACK = "BACK";

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
        String baseTitle = ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-settings.guild-settings-title",
                "&6Guild Settings - {guild_name}", "{guild_name}",
                guild.getName() != null ? guild.getName() : "未知公会"));
        if (totalPages > 1) {
            baseTitle += ColorUtils.colorize(" &7(" +
                    languageManager.getGuiIndexedMessage(
                            languageManager.getPlayerLanguage(player),
                            "gui.common.page-info", "第{0}页/共{1}页",
                            new String[]{String.valueOf(currentPage), String.valueOf(totalPages)}) + ")");
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

        String name = guild.getName() != null ? guild.getName() : languageManager.getGuiColoredMessage(player, "gui.guild-settings.overview-no-name", "No Name");
        String tag = guild.getTag() != null ? "[" + guild.getTag() + "]" : languageManager.getGuiColoredMessage(player, "gui.guild-settings.overview-no-tag", "No Tag");
        String desc = guild.getDescription() != null ? guild.getDescription() : languageManager.getGuiColoredMessage(player, "gui.guild-settings.overview-no-desc", "No Description");
        String homeStatus = guild.hasHome() ? languageManager.getGuiColoredMessage(player, "gui.guild-settings.overview-home-set", "&aSet") : languageManager.getGuiColoredMessage(player, "gui.guild-settings.overview-home-not-set", "&cNot Set");

        String content = languageManager.getGuiColoredMessage(player, "gui.guild-settings.bedrock-content",
                "&6Guild Overview\n&fName: &e{guild_name}\n&fTag: &e{guild_tag}\n&fDescription: {guild_desc}\n&fGuild Home: {home_status}",
                "{guild_name}", name, "{guild_tag}", tag, "{guild_desc}", desc, "{home_status}", homeStatus);

        SimpleForm form = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player, "gui.guild-settings.guild-settings-title", "&6Guild Settings - {guild_name}", "{guild_name}", name))
                .content(content)
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.bedrock-change-name", "&eChange Name"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.bedrock-change-description", "&eChange Description"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.bedrock-change-tag", "&eChange Tag"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.set-home", "&bSet Guild Home"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.transfer-leader", "&cTransfer Leadership"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.bedrock-kick-member", "&cKick Member"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.bedrock-promote-demote", "&ePromote/Demote"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.guild-funds", "&aGuild Funds"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.logs", "&6Guild Logs"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.home-teleport", "&dTeleport Home"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.delete", "&4Delete Guild"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-settings.bedrock-back-to-main", "&fBack to Main Menu"))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> handleChangeName(player);
                        case 1 -> handleChangeDescription(player);
                        case 2 -> handleChangeTag(player);
                        case 3 -> handleSetHome(player);
                        case 4 -> handleTransferLeader(player);
                        case 5 -> handleKickMember(player);
                        case 6 -> plugin.getGuiManager().openGUI(player,
                                new PromoteMemberGUI(plugin, guild, player));
                        case 7 -> handleGuildFunds(player);
                        case 8 -> handleGuildLogs(player);
                        case 9 -> handleHomeTeleport(player);
                        case 10 -> handleDeleteGuild(player);
                        case 11 -> plugin.getGuiManager().openGUI(player,
                                new MainGuildGUI(plugin, player));
                    }
                }))
                .build();

        return BedrockFormSender.sendForm(player.getUniqueId(), form);
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

        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
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
            case 15: // 转移会长
                handleTransferLeader(player);
                break;
            case 13: // 设置公会家
                handleSetHome(player);
                break;
            case 28: // 公会资金
                handleGuildFunds(player);
                break;
            case 29: // 预留扩展（顶替原关系管理入口）
                break;
            case 31: // 公会日志
                if (clickType == ClickType.LEFT) handleGuildLogs(player);
                break;
            case 33: // 公会家传送
                handleHomeTeleport(player);
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

        // 将注入项按顺序放置到预定义槽位上（使用语言感知渲染）
        for (int i = 0; i < pageInjections.size() && i < MODULE_SLOT_LAYOUT.length; i++) {
            GUIExtensionHook.GUIInjectionSlot inj = pageInjections.get(i);
            int targetSlot = MODULE_SLOT_LAYOUT[i];
            inventory.setItem(targetSlot, inj.getDisplayItem(player, languageManager));
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
                    languageManager.getGuiMessage(player, "gui.common.previous-page", "&e&lPrevious Page"),
                    false,
                    languageManager.getGuiMessage(player, "gui.common.previous-page-hint", "&7Click to go to previous page"));
            inventory.setItem(45, prevItem);
        } else {
            // 第一页时显示灰色的不可用箭头
            ItemStack disabledPrev = createItem(Material.GRAY_DYE,
                    languageManager.getGuiMessage(player, "gui.common.no-previous", "&7Previous Page"), false,
                    languageManager.getGuiMessage(player, "gui.common.already-first-page", "&7Already on first page"));
            inventory.setItem(45, disabledPrev);
        }

        // 下一页按钮 (slot 53)
        if (currentPage < totalPages) {
            ItemStack nextItem = createItem(Material.ARROW,
                    languageManager.getGuiMessage(player, "gui.common.next-page", "&e&lNext Page"),
                    false,
                    languageManager.getGuiMessage(player, "gui.common.next-page-hint", "&7Click to view more"));
            inventory.setItem(53, nextItem);
        } else {
            // 最后一页时显示灰色的不可用箭头
            ItemStack disabledNext = createItem(Material.GRAY_DYE,
                    languageManager.getGuiMessage(player, "gui.common.no-next", "&7Next Page"), false,
                    languageManager.getGuiMessage(player, "gui.common.already-last-page", "&7Already on last page"));
            inventory.setItem(53, disabledNext);
        }
    }

    /**
     * 返回主菜单按钮 (slot 49)
     */
    private void setupBackButton(Inventory inventory) {
        ItemStack back = createItem(Material.ARROW,
                languageManager.getGuiMessage(player, "gui.guild-settings.back", "&7Back"), false,
                languageManager.getGuiMessage(player, "gui.guild-settings.back-desc", "&7Click &fReturn to Main Menu"));
        inventory.setItem(49, back);
    }

    /** 设置设置按钮（简约化） - 仅第1页使用 */
    private void setupSettingsButtons(Inventory inventory) {
        ItemStack textEdit = createItem(Material.WRITABLE_BOOK,
            languageManager.getGuiMessage(player, "gui.guild-settings.text-edit", "&eText Edit"), false,
            languageManager.getGuiMessage(player, "gui.guild-settings.text-edit-desc-left", "&7Left Click &fEdit Name"),
            languageManager.getGuiMessage(player, "gui.guild-settings.text-edit-desc-right", "&7Right Click &fEdit Description"),
            languageManager.getGuiMessage(player, "gui.guild-settings.text-edit-desc-shift-left", "&7Shift+Left &fEdit Tag"));
        inventory.setItem(11, textEdit);

        ItemStack transferLeader = createItem(Material.GOLD_INGOT,
            languageManager.getGuiMessage(player, "gui.guild-settings.transfer-leader", "&cTransfer Leadership"), false,
            languageManager.getGuiMessage(player, "gui.guild-settings.transfer-leader-desc", "&7Click &fTransfer leadership to a member"));
        inventory.setItem(15, transferLeader);

        ItemStack setHome = createItem(Material.COMPASS,
            languageManager.getGuiMessage(player, "gui.guild-settings.set-home", "&bSet Guild Home"), false,
            languageManager.getGuiMessage(player, "gui.guild-settings.set-home-desc", "&7Click &fSet Guild Home"));
        inventory.setItem(13, setHome);
    }

    /** 设置功能按钮 - 仅第1页使用 */
    private void setupFunctionButtons(Inventory inventory) {
        ItemStack guildFunds = createItem(Material.EMERALD,
            languageManager.getGuiMessage(player, "gui.guild-settings.guild-funds", "&aGuild Funds"), false,
            languageManager.getGuiMessage(player, "gui.guild-settings.guild-funds-desc", "&7Click &fView Member Deposits"));
        inventory.setItem(28, guildFunds);

        ItemStack reservedSlot = createItem(Material.PAPER,
            languageManager.getGuiMessage(player, "gui.guild-settings.reserved", "&7Reserved"), false,
            languageManager.getGuiMessage(player, "gui.guild-settings.reserved-desc", "&7Coming soon..."));
        inventory.setItem(29, reservedSlot);

        ItemStack guildLogs = createItem(Material.BOOK,
            languageManager.getGuiMessage(player, "gui.guild-settings.logs", "&6Guild Logs"), false,
            languageManager.getGuiMessage(player, "gui.guild-settings.logs-desc", "&7Click &fView Guild Logs"));
        inventory.setItem(31, guildLogs);

        ItemStack homeTeleport = createItem(Material.ENDER_PEARL,
            languageManager.getGuiMessage(player, "gui.guild-settings.home-teleport", "&dTeleport Home"), false,
            languageManager.getGuiMessage(player, "gui.guild-settings.home-teleport-desc", "&7Click &fTeleport Home"));
        inventory.setItem(33, homeTeleport);

        ItemStack deleteGuild = createItem(Material.TNT,
            languageManager.getGuiMessage(player, "gui.guild-settings.delete", "&4Delete Guild"), false,
            languageManager.getGuiMessage(player, "gui.guild-settings.delete-desc", "&7Click &fDelete Current Guild"));
        inventory.setItem(36, deleteGuild);

        // 返回主菜单（第1页也显示）
        setupBackButton(inventory);
    }
    
    /** 显示当前设置信息 - 仅第1页使用 */
    private void displayCurrentSettings(Inventory inventory) {
        String name = guild.getName() != null ? guild.getName() :
            languageManager.getGuiMessage(player, "gui.guild-settings.overview-no-name", "No Name");
        String tag = guild.getTag() != null ? "&7[" + guild.getTag() + "&7]" :
            languageManager.getGuiMessage(player, "gui.guild-settings.overview-no-tag", "No Tag");
        String desc = guild.getDescription() != null ? guild.getDescription() :
            languageManager.getGuiMessage(player, "gui.guild-settings.overview-no-desc", "No Description");
        String homeStatus = guild.hasHome() ?
            languageManager.getGuiMessage(player, "gui.guild-settings.overview-home-set", "&aSet") :
            languageManager.getGuiMessage(player, "gui.guild-settings.overview-home-not-set", "&cNot Set");

        ItemStack overview = createItem(Material.PAPER,
            languageManager.getGuiMessage(player, "gui.guild-settings.overview", "&6Guild Overview"), false,
            languageManager.getGuiMessage(player, "gui.guild-settings.overview-name", "&7Name: &e{name}", "{name}", name),
            languageManager.getGuiMessage(player, "gui.guild-settings.overview-tag", "&7Tag: &e{tag}", "{tag}", tag),
            languageManager.getGuiMessage(player, "gui.guild-settings.overview-desc", "&7Description: &7{desc}", "{desc}", desc),
            languageManager.getGuiMessage(player, "gui.guild-settings.overview-home", "&7Home: {home}", "{home}", homeStatus)
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
            String msg = languageManager.getGuiMessage(player, "gui.common.leader-only", "&cOnly the guild leader can perform this operation");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildNameInputGUI(plugin, guild, player));
    }

    private void handleChangeDescription(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = languageManager.getGuiMessage(player, "gui.common.leader-only", "&cOnly the guild leader can perform this operation");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildDescriptionInputGUI(plugin, guild, player));
    }

    private void handleChangeTag(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = languageManager.getGuiMessage(player, "gui.common.leader-only", "&cOnly the guild leader can perform this operation");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildTagInputGUI(plugin, guild, player));
    }

    private void handleInviteMember(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String msg = languageManager.getGuiMessage(player, "gui.common.officer-or-higher", "&cOnly officers and above can perform this operation");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new InviteMemberGUI(plugin, guild, player));
    }

    private void handleKickMember(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String msg = languageManager.getGuiMessage(player, "gui.common.officer-or-higher", "&cOnly officers and above can perform this operation");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new KickMemberGUI(plugin, guild, player));
    }

    private void handleSetHome(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = languageManager.getGuiMessage(player, "gui.common.leader-only", "&cOnly the guild leader can perform this operation");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuildService().setGuildHomeAsync(guild.getId(), player.getLocation(), player.getUniqueId()).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    if (plugin.getGuildHomeProtectListener() != null) {
                        plugin.getGuildHomeProtectListener().refreshHomesAsync();
                    }
                    String message = languageManager.getGuiMessage(player, "gui.guild-settings.sethome.success", "&aGuild home set successfully!");
                    player.sendMessage(ColorUtils.colorize(message));
                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
                } else {
                    String message = languageManager.getGuiMessage(player, "gui.guild-settings.sethome.failed", "&cFailed to set guild home!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    private void handleGuildFunds(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String msg = languageManager.getGuiMessage(player, "gui.common.no-permission", "&cInsufficient permission");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildFundsGUI(plugin, guild, player, 0, "GuildSettingsGUI"));
    }

    private void handleGuildLogs(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String msg = languageManager.getGuiMessage(player, "gui.common.no-permission", "&cInsufficient permission");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildLogsGUI(plugin, guild, player));
    }

    private void handleHomeTeleport(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String msg = languageManager.getGuiMessage(player, "gui.common.no-permission", "&cInsufficient permission");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuildService().getGuildHomeAsync(guild.getId()).thenAccept(location -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (location != null) {
                    startHomeTeleportDelay(player, location);
                } else {
                    String message = languageManager.getGuiMessage(player, "gui.guild-settings.home.not-set", "&cGuild home has not been set yet!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    private void startHomeTeleportDelay(Player player, org.bukkit.Location targetLocation) {
        com.guild.util.GuildHomeTeleport.start(plugin, player, targetLocation, true,
                () -> {
                    String message = languageManager.getGuiMessage(player, "gui.guild-settings.home.success", "&aTeleported to guild home!");
                    player.sendMessage(ColorUtils.colorize(message));
                },
                reason -> {
                    String message = languageManager.getGuiMessage(player, "gui.guild-settings.home.teleport-failed",
                            "&cTeleport failed, please try again!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
    }

    private void handleDeleteGuild(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = languageManager.getGuiMessage(player, "gui.common.leader-only", "&cOnly the guild leader can perform this operation");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild, player));
    }

    private void handleTransferLeader(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null
                || member.getGuildId() != guild.getId()
                || member.getRole() != GuildMember.Role.LEADER
                || !player.getUniqueId().equals(guild.getLeaderUuid())) {
            String msg = languageManager.getGuiMessage(player, "gui.common.leader-only",
                    "&cOnly the guild leader can perform this operation");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new TransferLeaderGUI(plugin, guild, player));
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
