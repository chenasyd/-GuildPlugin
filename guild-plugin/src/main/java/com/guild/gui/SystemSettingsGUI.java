package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.module.ModuleManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.geyser.PlayerConnectionService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统设置GUI
 */
public class SystemSettingsGUI implements GUI {

    // ── 图像模式功能常量 ──
    public static final String FUNC_DEBUG_TOGGLE = "DEBUG_TOGGLE";
    public static final String FUNC_AUTO_SAVE = "AUTO_SAVE";
    public static final String FUNC_ECONOMY_TOGGLE = "ECONOMY_TOGGLE";
    public static final String FUNC_RELATION_TOGGLE = "RELATION_TOGGLE";
    public static final String FUNC_LEVEL_SYSTEM = "LEVEL_SYSTEM";
    public static final String FUNC_APPLICATION_TOGGLE = "APPLICATION_TOGGLE";
    public static final String FUNC_INVITE_TOGGLE = "INVITE_TOGGLE";
    public static final String FUNC_GUILD_HOME = "GUILD_HOME";
    public static final String FUNC_RELOAD = "RELOAD";
    public static final String FUNC_DB_MAINT = "DB_MAINT";
    public static final String FUNC_BACKUP = "BACKUP";
    public static final String FUNC_SAVE = "SAVE";
    public static final String FUNC_BACK = "BACK";

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
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-title", "&4System Settings"));
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

        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }
    
    private void setupSettingsOptions(Inventory inventory) {
        // 详细后台信息显示开关
        boolean debugMode = plugin.getConfigManager().getMainConfig().getBoolean("debug.enabled", false);
        Material debugMaterial = debugMode ? Material.LIME_WOOL : Material.RED_WOOL;
        String debugStatus = ColorUtils.colorize(debugMode
                ? languageManager.getGuiMessage(player, "gui.system-settings.status-enabled", "&aEnabled")
                : languageManager.getGuiMessage(player, "gui.system-settings.status-disabled", "&cDisabled"));
        
        ItemStack debugToggle = createItem(
            debugMaterial,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-debug-toggle", "&eVerbose Debug Info")),
            ColorUtils.colorize(languageManager.getGuiIndexedMessage(player, "gui.system-settings.current-status", "&7Current Status: {0}", debugStatus)),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-debug-toggle-lore-1", "&7Enables detailed console output")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-debug-toggle-lore-2", "&7Shows detailed debug information")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-toggle", "&eClick to toggle"))
        );
        inventory.setItem(10, debugToggle);
        
        // 自动保存设置
        boolean autoSave = plugin.getConfigManager().getMainConfig().getBoolean("auto-save.enabled", true);
        Material autoSaveMaterial = autoSave ? Material.LIME_WOOL : Material.RED_WOOL;
        String autoSaveStatus = ColorUtils.colorize(autoSave
                ? languageManager.getGuiMessage(player, "gui.system-settings.status-enabled", "&aEnabled")
                : languageManager.getGuiMessage(player, "gui.system-settings.status-disabled", "&cDisabled"));
        
        ItemStack autoSaveToggle = createItem(
            autoSaveMaterial,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-auto-save", "&eAuto Save Data")),
            ColorUtils.colorize(languageManager.getGuiIndexedMessage(player, "gui.system-settings.current-status", "&7Current Status: {0}", autoSaveStatus)),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-auto-save-lore-1", "&7Periodically auto-saves guild data")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-auto-save-lore-2", "&7Prevents data loss")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-toggle", "&eClick to toggle"))
        );
        inventory.setItem(12, autoSaveToggle);
        
        // 经济系统开关
        boolean economyEnabled = plugin.getConfigManager().getMainConfig().getBoolean("economy.enabled", true);
        Material economyMaterial = economyEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String economyStatus = ColorUtils.colorize(economyEnabled
                ? languageManager.getGuiMessage(player, "gui.system-settings.status-enabled", "&aEnabled")
                : languageManager.getGuiMessage(player, "gui.system-settings.status-disabled", "&cDisabled"));
        
        ItemStack economyToggle = createItem(
            economyMaterial,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-economy", "&eEconomy System")),
            ColorUtils.colorize(languageManager.getGuiIndexedMessage(player, "gui.system-settings.current-status", "&7Current Status: {0}", economyStatus)),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-economy-lore-1", "&7Guild economy features toggle")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-economy-lore-2", "&7Including deposit, withdraw, transfer, etc.")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-toggle", "&eClick to toggle"))
        );
        inventory.setItem(14, economyToggle);
        
        // 关系系统开关
        boolean relationEnabled = plugin.getConfigManager().getMainConfig().getBoolean("relations.enabled", true);
        Material relationMaterial = relationEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String relationStatus = ColorUtils.colorize(relationEnabled
                ? languageManager.getGuiMessage(player, "gui.system-settings.status-enabled", "&aEnabled")
                : languageManager.getGuiMessage(player, "gui.system-settings.status-disabled", "&cDisabled"));
        
        ItemStack relationToggle = createItem(
            relationMaterial,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-relations", "&eGuild Relations")),
            ColorUtils.colorize(languageManager.getGuiIndexedMessage(player, "gui.system-settings.current-status", "&7Current Status: {0}", relationStatus)),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-relations-lore-1", "&7Guild relations features toggle")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-relations-lore-2", "&7Including allies, enemies, wars, etc.")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-toggle", "&eClick to toggle"))
        );
        inventory.setItem(16, relationToggle);
        
        // 等级系统开关
        boolean levelEnabled = plugin.getConfigManager().getMainConfig().getBoolean("level-system.enabled", true);
        Material levelMaterial = levelEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String levelStatus = ColorUtils.colorize(levelEnabled
                ? languageManager.getGuiMessage(player, "gui.system-settings.status-enabled", "&aEnabled")
                : languageManager.getGuiMessage(player, "gui.system-settings.status-disabled", "&cDisabled"));
        
        ItemStack levelToggle = createItem(
            levelMaterial,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-level-system", "&eLevel System")),
            ColorUtils.colorize(languageManager.getGuiIndexedMessage(player, "gui.system-settings.current-status", "&7Current Status: {0}", levelStatus)),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-level-system-lore-1", "&7Guild level features toggle")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-level-system-lore-2", "&7Including auto-levelup, member limits, etc.")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-toggle", "&eClick to toggle"))
        );
        inventory.setItem(19, levelToggle);

        // 申请系统开关
        boolean applicationEnabled = plugin.getConfigManager().getMainConfig().getBoolean("applications.enabled", true);
        Material applicationMaterial = applicationEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String applicationStatus = ColorUtils.colorize(applicationEnabled
                ? languageManager.getGuiMessage(player, "gui.system-settings.status-enabled", "&aEnabled")
                : languageManager.getGuiMessage(player, "gui.system-settings.status-disabled", "&cDisabled"));


        ItemStack applicationToggle = createItem(
            applicationMaterial,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-applications", "&eApplication System")),
            ColorUtils.colorize(languageManager.getGuiIndexedMessage(player, "gui.system-settings.current-status", "&7Current Status: {0}", applicationStatus)),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-applications-lore-1", "&7Application to join toggle")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-applications-lore-2", "&7Players must apply to join a guild")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-toggle", "&eClick to toggle"))
        );
        inventory.setItem(21, applicationToggle);

        // 邀请系统开关
        boolean inviteEnabled = plugin.getConfigManager().getMainConfig().getBoolean("invites.enabled", true);
        Material inviteMaterial = inviteEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String inviteStatus = ColorUtils.colorize(inviteEnabled
                ? languageManager.getGuiMessage(player, "gui.system-settings.status-enabled", "&aEnabled")
                : languageManager.getGuiMessage(player, "gui.system-settings.status-disabled", "&cDisabled"));


        ItemStack inviteToggle = createItem(
            inviteMaterial,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-invites", "&eInvite System")),
            ColorUtils.colorize(languageManager.getGuiIndexedMessage(player, "gui.system-settings.current-status", "&7Current Status: {0}", inviteStatus)),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-invites-lore-1", "&7Guild invite features toggle")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-invites-lore-2", "&7Leaders can invite players to join")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-toggle", "&eClick to toggle"))
        );
        inventory.setItem(23, inviteToggle);

        // 工会家系统开关
        boolean homeEnabled = plugin.getConfigManager().getMainConfig().getBoolean("guild-home.enabled", true);
        Material homeMaterial = homeEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String homeStatus = ColorUtils.colorize(homeEnabled
                ? languageManager.getGuiMessage(player, "gui.system-settings.status-enabled", "&aEnabled")
                : languageManager.getGuiMessage(player, "gui.system-settings.status-disabled", "&cDisabled"));

        ItemStack homeToggle = createItem(
            homeMaterial,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-guild-home", "&eGuild Home")),
            ColorUtils.colorize(languageManager.getGuiIndexedMessage(player, "gui.system-settings.current-status", "&7Current Status: {0}", homeStatus)),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-guild-home-lore-1", "&7Guild home features toggle")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-guild-home-lore-2", "&7Including setting up and teleporting to guild home")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-toggle", "&eClick to toggle"))
        );
        inventory.setItem(25, homeToggle);
    }
    
    private void setupActionButtons(Inventory inventory) {
        // 重载配置按钮
        ItemStack reload = createItem(
            Material.EMERALD,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-reload", "&aReload Config")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-reload-lore-1", "&7Reload all configuration files")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-reload-lore-2", "&7Including messages.yml, gui.yml, etc.")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-reload", "&eClick to reload"))
        );
        inventory.setItem(28, reload);

        // 数据库维护按钮
        ItemStack database = createItem(
            Material.BOOK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-database", "&bDatabase Maintenance")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-database-lore-1", "&7Show status and optimize the database")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-database-lore-2", "&7SQLite VACUUM / MySQL OPTIMIZE")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-maintain", "&eClick to maintain"))
        );
        inventory.setItem(30, database);

        // 备份数据按钮
        ItemStack backup = createItem(
            Material.CHEST,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-backup", "&6Backup Data")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-backup-lore-1", "&7Backup guild data to backup/")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-backup-lore-2", "&7SQLite=zip, MySQL=sql.gz")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-backup", "&eClick to backup"))
        );
        inventory.setItem(32, backup);

        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.back", "&cBack")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.back-lore", "&7Return to admin menu"))
        );
        inventory.setItem(49, back);

        // 保存设置按钮
        ItemStack save = createItem(
            Material.GREEN_WOOL,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-save", "&aSave Settings")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-save-lore-1", "&7Save all current settings")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-save-lore-2", "&7Apply to configuration files")),
            "",
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.system-settings-click-to-save", "&eClick to save"))
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
            case 10: // 详细后台信息显示开关
                toggleDebugMode(player);
                break;
            case 12: // 自动保存开关
                toggleAutoSave(player);
                break;
            case 14: // 经济系统开关
                toggleEconomy(player);
                break;
            case 16: // 关系系统开关
                toggleRelations(player);
                break;
            case 19: // 等级系统开关
                toggleLevelSystem(player);
                break;
            case 21: // 申请系统开关
                toggleApplications(player);
                break;
            case 23: // 邀请系统开关
                toggleInvites(player);
                break;
            case 25: // 工会家系统开关
                toggleGuildHome(player);
                break;
            case 28: // 重载配置
                reloadConfigs(player);
                break;
            case 30: // 数据库维护
                maintainDatabase(player);
                break;
            case 32: // 备份数据
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
        
        String message = newValue ? languageManager.getGuiMessage(player, "gui.system-settings.debug-enabled", "&aDebug info display enabled!")
                                   : languageManager.getGuiMessage(player, "gui.system-settings.debug-disabled", "&cDebug info display disabled!");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }
    
    private void toggleAutoSave(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("auto-save.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("auto-save.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();
        
        String message = newValue ? languageManager.getGuiMessage(player, "gui.system-settings.auto-save-enabled", "&aAuto save data enabled!")
                                   : languageManager.getGuiMessage(player, "gui.system-settings.auto-save-disabled", "&cAuto save data disabled!");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleEconomy(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("economy.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("economy.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getGuiMessage(player, "gui.system-settings.economy-enabled", "&aEconomy system enabled!")
                                   : languageManager.getGuiMessage(player, "gui.system-settings.economy-disabled", "&cEconomy system disabled!");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleRelations(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("relations.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("relations.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getGuiMessage(player, "gui.system-settings.relations-enabled", "&aGuild relations enabled!")
                                   : languageManager.getGuiMessage(player, "gui.system-settings.relations-disabled", "&cGuild relations disabled!");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleLevelSystem(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("level-system.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("level-system.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getGuiMessage(player, "gui.system-settings.level-system-enabled", "&aGuild level system enabled!")
                                   : languageManager.getGuiMessage(player, "gui.system-settings.level-system-disabled", "&cGuild level system disabled!");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleApplications(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("applications.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("applications.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getGuiMessage(player, "gui.system-settings.applications-enabled", "&aApplication system enabled!")
                                   : languageManager.getGuiMessage(player, "gui.system-settings.applications-disabled", "&cApplication system disabled!");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleInvites(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("invites.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("invites.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getGuiMessage(player, "gui.system-settings.invites-enabled", "&aInvite system enabled!")
                                   : languageManager.getGuiMessage(player, "gui.system-settings.invites-disabled", "&cInvite system disabled!");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }

    private void toggleGuildHome(Player player) {
        boolean current = plugin.getConfigManager().getMainConfig().getBoolean("guild-home.enabled", true);
        boolean newValue = !current;
        plugin.getConfigManager().getMainConfig().set("guild-home.enabled", newValue);
        plugin.getConfigManager().saveMainConfig();

        String message = newValue ? languageManager.getGuiMessage(player, "gui.system-settings.guild-home-enabled", "&aGuild home enabled!")
                                   : languageManager.getGuiMessage(player, "gui.system-settings.guild-home-disabled", "&cGuild home disabled!");
        player.sendMessage(ColorUtils.colorize(message));
        refresh(player);
    }
    
    private void reloadConfigs(Player player) {
        try {
            plugin.reloadRuntimeConfiguration();

            // 插件本体语言（core/gui）异步重载 — 与模块语言完全独立
            plugin.getLanguageManager().reloadLanguagesAsync(() -> {
                // 刷新所有打开的 GUI
                try {
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (plugin.getGuiManager().hasOpenGUI(p)) plugin.getGuiManager().refreshGUI(p);
                    }
                } catch (Exception ignored) {}

                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(
                        player, "gui.system-settings.reload-success", "&aConfig reload successful!")));
            });

            // 模块语言异步重载 — 与插件本体并行执行
            plugin.getLanguageManager().reloadModuleLanguagesAsync(() -> {
                try {
                    var lm = plugin.getLanguageManager();
                    for (String dir : lm.getKnownModuleLangDirs()) {
                        try {
                            lm.loadModuleLanguageResourcesForModule(dir);
                        } catch (Exception ignored) {}
                    }
                    ModuleManager mm = plugin.getModuleManager();
                    var api = mm.getSharedApi();
                    for (String moduleId : mm.getRegistry().getModuleIds()) {
                        try { api.loadModuleLanguageResource(moduleId, null); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                try {
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (plugin.getGuiManager().hasOpenGUI(p)) {
                            plugin.getGuiManager().refreshGUI(p);
                        }
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(
                    player, "gui.system-settings.reload-failed", "&cConfig reload failed: {0}", e.getMessage())));
        }
    }

    private void maintainDatabase(Player player) {
        var backupService = plugin.getDatabaseBackupService();
        if (backupService == null) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.system-settings.backup-unavailable", "&cBackup service is not ready.")));
            return;
        }
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.system-settings.database-running", "&eRunning database maintenance, please wait...")));
        backupService.runMaintenanceAsync().thenAccept(result ->
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (result.success()) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.system-settings.database-done", "&aDatabase maintenance finished:")));
                        for (String line : result.summary().split("\n")) {
                            if (!line.isBlank()) {
                                player.sendMessage(ColorUtils.colorize("&7" + line));
                            }
                        }
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                        "gui.system-settings.database-failed", "&cDatabase maintenance failed: {0}")
                                .replace("{0}", result.summary() != null ? result.summary() : "unknown")));
                    }
                }));
    }

    private void backupData(Player player) {
        var backupService = plugin.getDatabaseBackupService();
        if (backupService == null) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.system-settings.backup-unavailable", "&cBackup service is not ready.")));
            return;
        }
        if (!backupService.isEnabled()) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.system-settings.backup-disabled", "&cBackup is disabled (backup.enabled=false).")));
            return;
        }
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.system-settings.backup-running", "&eCreating database backup, please wait...")));
        backupService.backupAsync(com.guild.core.backup.DatabaseBackupService.BackupReason.MANUAL)
                .thenAccept(result -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (result.success()) {
                        String name = result.file() != null ? result.file().getName() : "";
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                        "gui.system-settings.backup-success", "&aBackup created: &f{file}")
                                .replace("{file}", name)));
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                        "gui.system-settings.backup-path", "&7Directory: &f{path}")
                                .replace("{path}", backupService.getBackupDirectory().getAbsolutePath())));
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                        "gui.system-settings.backup-failed", "&cBackup failed: {0}")
                                .replace("{0}", result.message() != null ? result.message() : "unknown")));
                    }
                }));
    }

    private void saveSettings(Player player) {
        try {
            plugin.getConfigManager().saveMainConfig();
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.system-settings.save-success", "&aSettings saved successfully!")));
        } catch (Exception e) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player, "gui.system-settings.save-failed", "&cFailed to save settings: {0}", e.getMessage())));
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
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;
        sendBedrockSettings(player);
        return true;
    }

    private void sendBedrockSettings(Player player) {
        boolean debug = plugin.getConfigManager().getMainConfig().getBoolean("debug.enabled", false);
        boolean autoSave = plugin.getConfigManager().getMainConfig().getBoolean("auto-save.enabled", true);
        boolean economy = plugin.getConfigManager().getMainConfig().getBoolean("economy.enabled", true);
        boolean relations = plugin.getConfigManager().getMainConfig().getBoolean("relations.enabled", true);
        boolean level = plugin.getConfigManager().getMainConfig().getBoolean("level-system.enabled", true);
        boolean applications = plugin.getConfigManager().getMainConfig().getBoolean("applications.enabled", true);
        boolean invites = plugin.getConfigManager().getMainConfig().getBoolean("invites.enabled", true);
        boolean home = plugin.getConfigManager().getMainConfig().getBoolean("guild-home.enabled", true);

        String on = languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-on", "&aON");
        String off = languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-off", "&cOFF");

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-title", "&4System Settings"))
            .content(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-content", "&fClick to toggle switch status"));

        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-debug", "&eDebug Info: {status}", "{status}", debug ? on : off));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-auto-save", "&eAuto Save: {status}", "{status}", autoSave ? on : off));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-economy", "&eEconomy System: {status}", "{status}", economy ? on : off));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-relations", "&eRelations System: {status}", "{status}", relations ? on : off));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-level", "&eLevel System: {status}", "{status}", level ? on : off));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-applications", "&eApplication System: {status}", "{status}", applications ? on : off));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-invites", "&eInvite System: {status}", "{status}", invites ? on : off));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-guild-home", "&eGuild Home: {status}", "{status}", home ? on : off));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-reload", "&aReload Config"));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-database", "&bDatabase Maintenance"));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-backup", "&6Backup Data"));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-save", "&aSave Settings"));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.system-settings.bedrock-back", "&cBack"));

        builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
            switch (response.clickedButtonId()) {
                case 0: toggleDebugMode(player); break;
                case 1: toggleAutoSave(player); break;
                case 2: toggleEconomy(player); break;
                case 3: toggleRelations(player); break;
                case 4: toggleLevelSystem(player); break;
                case 5: toggleApplications(player); break;
                case 6: toggleInvites(player); break;
                case 7: toggleGuildHome(player); break;
                case 8: reloadConfigs(player); break;
                case 9: maintainDatabase(player); break;
                case 10: backupData(player); break;
                case 11: saveSettings(player); break;
                case 12:
                    plugin.getGuiManager().openGUI(player, new AdminGuildGUI(plugin, player));
                    return;
            }
            // 切换/操作后重新发送表单
            sendBedrockSettings(player);
        }));

        builder.closedResultHandler(response -> {});

        BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
    }

    @Override
    public void onClose(Player player) {
        // 关闭时的处理
    }
    
    @Override
    public void refresh(Player player) {
        if (player.isOnline()) {
            // 基岩版玩家由 sendBedrockSettings 自行刷新，
            // 跳过 GUIManager.refreshGUI 避免与 Cumulus 表单冲突
            if (PlayerConnectionService.isBedrockPlayer(player)) return;
            plugin.getGuiManager().refreshGUI(player);
        }
    }
}
