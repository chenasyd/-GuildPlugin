package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统设置GUI
 */
public class SystemSettingsGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;

    public SystemSettingsGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public String getTitle() {
        return languageManager.getGuiColoredMessage(player, "system-settings.title",
                ColorUtils.colorize("&4系统设置"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 设置系统设置选项
        setupSettingsOptions(inventory);
        
        // 设置操作按钮
        setupActionButtons(inventory);
    }
    
    private void setupSettingsOptions(Inventory inventory) {
        // 详细后台信息显示开关
        boolean debugMode = plugin.getConfigManager().getMainConfig().getBoolean("debug.enabled", false);
        Material debugMaterial = debugMode ? Material.LIME_WOOL : Material.RED_WOOL;
        String debugStatus = debugMode ? "&a已启用" : "&c已禁用";
        
        ItemStack debugToggle = createItem(
            debugMaterial,
            ColorUtils.colorize(languageManager.getMessage("system-settings.debug-toggle", "&e详细后台信息显示")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.current-status", "&7当前状态: {0}", debugStatus)),
            ColorUtils.colorize(languageManager.getMessage("system-settings.debug-toggle.lore1", "&7启用后会在控制台显示")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.debug-toggle.lore2", "&7详细的调试信息")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-toggle", "&e点击切换状态"))
        );
        inventory.setItem(19, debugToggle);
        
        // 自动保存设置
        boolean autoSave = plugin.getConfigManager().getMainConfig().getBoolean("auto-save.enabled", true);
        Material autoSaveMaterial = autoSave ? Material.LIME_WOOL : Material.RED_WOOL;
        String autoSaveStatus = autoSave ? "&a已启用" : "&c已禁用";
        
        ItemStack autoSaveToggle = createItem(
            autoSaveMaterial,
            ColorUtils.colorize(languageManager.getMessage("system-settings.auto-save", "&e自动保存数据")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.current-status", "&7当前状态: {0}", autoSaveStatus)),
            ColorUtils.colorize(languageManager.getMessage("system-settings.auto-save.lore1", "&7定期自动保存工会数据")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.auto-save.lore2", "&7防止数据丢失")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-toggle", "&e点击切换状态"))
        );
        inventory.setItem(21, autoSaveToggle);
        
        // 经济系统开关
        boolean economyEnabled = plugin.getConfigManager().getMainConfig().getBoolean("economy.enabled", true);
        Material economyMaterial = economyEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String economyStatus = economyEnabled ? "&a已启用" : "&c已禁用";
        
        ItemStack economyToggle = createItem(
            economyMaterial,
            ColorUtils.colorize(languageManager.getMessage("system-settings.economy", "&e经济系统")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.current-status", "&7当前状态: {0}", economyStatus)),
            ColorUtils.colorize(languageManager.getMessage("system-settings.economy.lore1", "&7工会经济功能开关")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.economy.lore2", "&7包括存款、取款、转账等")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-toggle", "&e点击切换状态"))
        );
        inventory.setItem(23, economyToggle);
        
        // 关系系统开关
        boolean relationEnabled = plugin.getConfigManager().getMainConfig().getBoolean("relations.enabled", true);
        Material relationMaterial = relationEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String relationStatus = relationEnabled ? "&a已启用" : "&c已禁用";
        
        ItemStack relationToggle = createItem(
            relationMaterial,
            ColorUtils.colorize(languageManager.getMessage("system-settings.relations", "&e工会关系系统")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.current-status", "&7当前状态: {0}", relationStatus)),
            ColorUtils.colorize(languageManager.getMessage("system-settings.relations.lore1", "&7工会关系功能开关")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.relations.lore2", "&7包括盟友、敌对、开战等")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-toggle", "&e点击切换状态"))
        );
        inventory.setItem(25, relationToggle);
        
        // 等级系统开关
        boolean levelEnabled = plugin.getConfigManager().getMainConfig().getBoolean("level-system.enabled", true);
        Material levelMaterial = levelEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String levelStatus = levelEnabled ? "&a已启用" : "&c已禁用";
        
        ItemStack levelToggle = createItem(
            levelMaterial,
            ColorUtils.colorize(languageManager.getMessage("system-settings.level-system", "&e工会等级系统")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.current-status", "&7当前状态: {0}", levelStatus)),
            ColorUtils.colorize(languageManager.getMessage("system-settings.level-system.lore1", "&7工会等级功能开关")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.level-system.lore2", "&7包括自动升级、成员限制等")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-toggle", "&e点击切换状态"))
        );
        inventory.setItem(28, levelToggle);

        // 申请系统开关
        boolean applicationEnabled = plugin.getConfigManager().getMainConfig().getBoolean("applications.enabled", true);
        Material applicationMaterial = applicationEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String applicationStatus = applicationEnabled ? "&a已启用" : "&c已禁用";

        ItemStack applicationToggle = createItem(
            applicationMaterial,
            ColorUtils.colorize(languageManager.getMessage("system-settings.applications", "&e申请加入系统")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.current-status", "&7当前状态: {0}", applicationStatus)),
            ColorUtils.colorize(languageManager.getMessage("system-settings.applications.lore1", "&7申请加入工会功能开关")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.applications.lore2", "&7玩家需要申请才能加入工会")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-toggle", "&e点击切换状态"))
        );
        inventory.setItem(30, applicationToggle);

        // 邀请系统开关
        boolean inviteEnabled = plugin.getConfigManager().getMainConfig().getBoolean("invites.enabled", true);
        Material inviteMaterial = inviteEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String inviteStatus = inviteEnabled ? "&a已启用" : "&c已禁用";

        ItemStack inviteToggle = createItem(
            inviteMaterial,
            ColorUtils.colorize(languageManager.getMessage("system-settings.invites", "&e邀请系统")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.current-status", "&7当前状态: {0}", inviteStatus)),
            ColorUtils.colorize(languageManager.getMessage("system-settings.invites.lore1", "&7工会邀请功能开关")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.invites.lore2", "&7会长可以邀请玩家加入工会")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-toggle", "&e点击切换状态"))
        );
        inventory.setItem(32, inviteToggle);

        // 工会家系统开关
        boolean homeEnabled = plugin.getConfigManager().getMainConfig().getBoolean("guild-home.enabled", true);
        Material homeMaterial = homeEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String homeStatus = homeEnabled ? "&a已启用" : "&c已禁用";

        ItemStack homeToggle = createItem(
            homeMaterial,
            ColorUtils.colorize(languageManager.getMessage("system-settings.guild-home", "&e工会家系统")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.current-status", "&7当前状态: {0}", homeStatus)),
            ColorUtils.colorize(languageManager.getMessage("system-settings.guild-home.lore1", "&7工会家功能开关")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.guild-home.lore2", "&7包括设置和传送到工会家")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-toggle", "&e点击切换状态"))
        );
        inventory.setItem(34, homeToggle);
    }
    
    private void setupActionButtons(Inventory inventory) {
        // 重载配置按钮
        ItemStack reload = createItem(
            Material.EMERALD,
            ColorUtils.colorize(languageManager.getMessage("system-settings.reload", "&a重载配置")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.reload.lore1", "&7重新加载所有配置文件")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.reload.lore2", "&7包括messages.yml、gui.yml等")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-reload", "&e点击重载配置"))
        );
        inventory.setItem(37, reload);

        // 数据库维护按钮
        ItemStack database = createItem(
            Material.BOOK,
            ColorUtils.colorize(languageManager.getMessage("system-settings.database", "&b数据库维护")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.database.lore1", "&7数据库维护和优化")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.database.lore2", "&7清理过期数据、优化性能")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-maintain", "&e点击进行维护"))
        );
        inventory.setItem(39, database);

        // 备份数据按钮
        ItemStack backup = createItem(
            Material.CHEST,
            ColorUtils.colorize(languageManager.getMessage("system-settings.backup", "&6备份数据")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.backup.lore1", "&7备份工会数据")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.backup.lore2", "&7创建数据备份文件")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-backup", "&e点击备份数据"))
        );
        inventory.setItem(41, backup);

        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(languageManager.getMessage("system-settings.back", "&c返回")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.back-lore", "&7返回管理菜单"))
        );
        inventory.setItem(49, back);

        // 保存设置按钮
        ItemStack save = createItem(
            Material.GREEN_WOOL,
            ColorUtils.colorize(languageManager.getMessage("system-settings.save", "&a保存设置")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.save.lore1", "&7保存当前所有设置")),
            ColorUtils.colorize(languageManager.getMessage("system-settings.save.lore2", "&7应用到配置文件")),
            "",
            ColorUtils.colorize(languageManager.getMessage("system-settings.click-to-save", "&e点击保存设置"))
        );
        inventory.setItem(51, save);
    }
    
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        
        // 填充边框
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 19: // 详细后台信息显示开关
                toggleDebugMode(player);
                break;
            case 21: // 自动保存开关
                toggleAutoSave(player);
                break;
            case 23: // 经济系统开关
                toggleEconomy(player);
                break;
            case 25: // 关系系统开关
                toggleRelations(player);
                break;
            case 28: // 等级系统开关
                toggleLevelSystem(player);
                break;
            case 30: // 申请系统开关
                toggleApplications(player);
                break;
            case 32: // 邀请系统开关
                toggleInvites(player);
                break;
            case 34: // 工会家系统开关
                toggleGuildHome(player);
                break;
            case 37: // 重载配置
                reloadConfigs(player);
                break;
            case 39: // 数据库维护
                maintainDatabase(player);
                break;
            case 41: // 备份数据
                backupData(player);
                break;
            case 49: // 返回
                plugin.getGuiManager().openGUI(player, new AdminGuildGUI(plugin, player));
                break;
            case 51: // 保存设置
                saveSettings(player);
                break;
        }
    }
    
    private void toggleDebugMode(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("debug.enabled", false);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("debug.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();
        
        String message = newValue ? languageManager.getMessage("system-settings.debug-enabled", "&a详细后台信息显示已启用！")
                                   : languageManager.getMessage("system-settings.debug-disabled", "&c详细后台信息显示已禁用！");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }
    
    private void toggleAutoSave(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("auto-save.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("auto-save.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();
        
        String message = newValue ? languageManager.getMessage("system-settings.auto-save-enabled", "&a自动保存数据已启用！")
                                   : languageManager.getMessage("system-settings.auto-save-disabled", "&c自动保存数据已禁用！");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleEconomy(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("economy.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("economy.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getMessage("system-settings.economy-enabled", "&a经济系统已启用！")
                                   : languageManager.getMessage("system-settings.economy-disabled", "&c经济系统已禁用！");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleRelations(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("relations.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("relations.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getMessage("system-settings.relations-enabled", "&a工会关系系统已启用！")
                                   : languageManager.getMessage("system-settings.relations-disabled", "&c工会关系系统已禁用！");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleLevelSystem(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("level-system.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("level-system.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getMessage("system-settings.level-system-enabled", "&a工会等级系统已启用！")
                                   : languageManager.getMessage("system-settings.level-system-disabled", "&c工会等级系统已禁用！");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleApplications(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("applications.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("applications.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getMessage("system-settings.applications-enabled", "&a申请加入系统已启用！")
                                   : languageManager.getMessage("system-settings.applications-disabled", "&c申请加入系统已禁用！");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleInvites(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("invites.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("invites.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getMessage("system-settings.invites-enabled", "&a邀请系统已启用！")
                                   : languageManager.getMessage("system-settings.invites-disabled", "&c邀请系统已禁用！");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleGuildHome(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("guild-home.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("guild-home.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getMessage("system-settings.guild-home-enabled", "&a工会家系统已启用！")
                                   : languageManager.getMessage("system-settings.guild-home-disabled", "&c工会家系统已禁用！");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }
    
    private void reloadConfigs(Player player) {
        try {
            plugin.getConfigManager().reloadAllConfigs();
            player.sendMessage(ColorUtils.colorize(languageManager.getMessage("system-settings.reload-success", "&a配置重载成功！")));
        } catch (Exception e) {
            player.sendMessage(ColorUtils.colorize(languageManager.getMessage("system-settings.reload-failed", "&c配置重载失败：{0}", e.getMessage())));
        }
    }

    private void maintainDatabase(Player player) {
        player.sendMessage(ColorUtils.colorize(languageManager.getMessage("system-settings.database-dev", "&e数据库维护功能开发中...")));
        // TODO: 实现数据库维护功能
        // 包括清理过期数据、优化性能等
    }

    private void backupData(Player player) {
        player.sendMessage(ColorUtils.colorize(languageManager.getMessage("system-settings.backup-dev", "&e数据备份功能开发中...")));
        // TODO: 实现数据备份功能
        // 创建数据备份文件
    }

    private void saveSettings(Player player) {
        try {
            plugin.getConfigManager().saveMainConfig();
            player.sendMessage(ColorUtils.colorize(languageManager.getMessage("system-settings.save-success", "&a设置保存成功！")));
        } catch (Exception e) {
            player.sendMessage(ColorUtils.colorize(languageManager.getMessage("system-settings.save-failed", "&c设置保存失败：{0}", e.getMessage())));
        }
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @Override
    public void onClose(Player player) {
        // 关闭时的处理
    }
    
    @Override
    public void refresh(Player player) {
        if (player.isOnline()) {
            plugin.getGuiManager().refreshGUI(player);
        }
    }
}
