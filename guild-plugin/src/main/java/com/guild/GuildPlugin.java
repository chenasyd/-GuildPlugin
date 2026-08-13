package com.guild;

import com.guild.core.ServiceContainer;
import com.guild.core.config.ConfigManager;
import com.guild.core.database.DatabaseManager;
import com.guild.core.events.EventBus;
import com.guild.core.gui.GUIManager;
import com.guild.core.placeholder.PlaceholderManager;
import com.guild.core.permissions.PermissionManager;
import com.guild.core.economy.EconomyManager;
import com.guild.core.language.LanguageManager;
import com.guild.sdk.economy.CurrencyManager;
import com.guild.commands.GuildCommand;
import com.guild.commands.GuildAdminCommand;
import com.guild.commands.GuildModuleCommand;
import com.guild.commands.BedrockFormTestCommand;
import com.guild.listeners.PlayerListener;
import com.guild.listeners.GuildListener;
import com.guild.listeners.GuildHomeProtectListener;
import com.guild.services.GuildService;
import com.guild.comm.api.BungeeClientAPI;
import com.guild.comm.api.CommAPI;
import com.guild.core.gui.GUI;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.geyser.GeyserAPI;
import com.guild.core.geyser.PlayerConnectionService;
import com.guild.core.module.ModuleManager;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.DebugLog;
import com.guild.core.utils.PluginFileLogger;
import com.guild.core.utils.ServerUtils;
import com.guild.core.utils.TestUtils;
import com.guild.metrics.GuildMetrics;
import com.guild.update.UpdateChecker;
import com.guild.update.UpdateManager;
import com.guild.world.GuildWorldService;
import com.guild.world.api.GuildWorldAPI;
import com.guild.world.api.GuildWorldAPIImpl;
import com.guild.world.command.GuildWorldCommand;
import com.guild.world.selection.SelectionListener;
import com.guild.war.GuildWarListener;
import com.guild.war.GuildWarService;
import com.guild.war.api.GuildWarAPI;
import com.guild.war.api.GuildWarAPIImpl;
import com.guild.war.command.GuildWarCommand;
import com.guild.war.reward.WarRewardListener;
import com.guild.war.season.WarSeasonService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;

public class GuildPlugin extends JavaPlugin {
    
    private static GuildPlugin instance;
    private ServiceContainer serviceContainer;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private EventBus eventBus;
    private GUIManager guiManager;
    private PlaceholderManager placeholderManager;
    private PermissionManager permissionManager;
    private EconomyManager economyManager;
    private LanguageManager languageManager;
    private GuildService guildService;
    private com.guild.services.GuildInvestmentService guildInvestmentService;
    private com.guild.chat.GuildChatManager guildChatManager;
    private ModuleManager moduleManager;
    private GuildWorldService guildWorldService;
    private GuildWorldAPI guildWorldAPI;
    private GuildWarService guildWarService;
    private GuildWarAPI guildWarAPI;
    private WarSeasonService warSeasonService;
    private com.guild.warehouse.GuildWarehouseService guildWarehouseService;
    private com.guild.core.backup.DatabaseBackupService databaseBackupService;
    private com.guild.activity.ActivityBootstrap activityBootstrap;
    private volatile boolean modulesUnloaded = false;
    private GuildMetrics guildMetrics;
    private UpdateManager updateManager;
    private UpdateChecker updateChecker;
    private PluginFileLogger fileLogger;
    private com.guild.module.cloud.CloudModuleRepository cloudModuleRepository;
    private com.guild.core.cache.GuildPlayerDataCache guildPlayerDataCache;
    // 等级需求配置（key = 当前等级 -> 所需金额达到下一等级）
    private Map<Integer, Double> levelRequirements = new HashMap<>();
    private int maxGuildLevel = 10;
    
    @Override
    public void onEnable() {
        instance = this;
        Logger logger = getLogger();
        
        logger.info("Starting Guild Plugin...");
        logger.info("Detected server type: " + ServerUtils.getServerType());
        logger.info("Server version: " + ServerUtils.getServerVersion());
        logger.info("Minecraft version: " + ServerUtils.getMinecraftVersion());
        if (ServerUtils.isFolia()) {
            logger.info("Folia world bridge supported: " + ServerUtils.isFoliaVersionSupported());
        }
        
        // 检查API版本兼容性
        if (!ServerUtils.supportsApiVersion("1.20")) {
            logger.severe("This plugin requires 1.20 or higher! Current version: " + ServerUtils.getServerVersion());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 运行兼容性测试（使用插件日志器）
        TestUtils.testCompatibility(logger);
        
        try {
            // 初始化服务容器
            serviceContainer = new ServiceContainer();
            
            // 初始化配置管理器
            configManager = new ConfigManager(this);
            serviceContainer.register(ConfigManager.class, configManager);

            // 初始化文件日志管理器（创建 logs/ 目录，按日分割日志文件）
            fileLogger = new PluginFileLogger(getDataFolder(), getLogger());
            fileLogger.logSystem("Plugin starting... Server type: " + ServerUtils.getServerType()
                    + ", Version: " + ServerUtils.getServerVersion());
            
            // 初始化数据库管理器
            databaseManager = new DatabaseManager(this);
            serviceContainer.register(DatabaseManager.class, databaseManager);
            
            // 初始化事件总线
            eventBus = new EventBus();
            serviceContainer.register(EventBus.class, eventBus);
            
            // 初始化GUI管理器
            guiManager = new GUIManager(this);
            serviceContainer.register(GUIManager.class, guiManager);
            
            // 初始化占位符管理器
            placeholderManager = new PlaceholderManager(this);
            serviceContainer.register(PlaceholderManager.class, placeholderManager);
            
            // 初始化权限管理器
            permissionManager = new PermissionManager(this);
            serviceContainer.register(PermissionManager.class, permissionManager);
            
            // 初始化经济管理器
            economyManager = new EconomyManager(this);
            serviceContainer.register(EconomyManager.class, economyManager);

            // 初始化语言管理器
            languageManager = new LanguageManager(this);
            serviceContainer.register(LanguageManager.class, languageManager);

            // ── 通信与基岩版检测层（隔离初始化 — 非致命，失败不影响插件核心功能）──
            // 注意：BungeeClientAPI 初始化失败时，PlayerConnectionService 会在首次玩家检测时懒初始化重试
            try {
                // 初始化 CommAPI 桥接器（生命周期由 GuildPlugin 自身管理）
                CommAPI.initialize(logger);
            } catch (Throwable e) {
                logger.warning("[Init] CommAPI initialization failed (cross-server bridge unavailable): " + e.getMessage());
            }
            try {
                // 初始化 BungeeCord 客户端 API（跨服通信子服端，注册 Plugin Messaging 通道）
                BungeeClientAPI.initialize(this);
            } catch (Throwable e) {
                logger.warning("[Init] BungeeClientAPI initialization failed (BungeeCord proxy detection unavailable): " + e.getMessage());
            }
            // 注册代理连接信息回调：代理推送到达后，若玩家已打开 GUI 则刷新为基岩版模式
            // 解决时序竞争：PlayerJoinEvent 时代理消息可能尚未到达，GUI 按 Java 模式打开
            BungeeClientAPI.setConnectionInfoCallback(info -> {
                if (!info.isBedrock()) return;
                Player player = Bukkit.getPlayer(info.getUuid());
                if (player == null || !player.isOnline()) return;
                if (guiManager == null || !guiManager.hasOpenGUI(player)) return;

                // 在实体线程上刷新 GUI（Folia 兼容）
                CompatibleScheduler.runTask(this, player, () -> {
                    if (player.isOnline() && guiManager.hasOpenGUI(player)) {
                        guiManager.refreshGUI(player);
                        DebugLog.info(logger, "[PlayerConnection] Proxy push received, refreshing "
                                + player.getName() + "'s GUI to Bedrock mode");
                    }
                });
            });
            try {
                // 初始化 GeyserAPI（基岩版玩家检测，Geyser 未安装时静默降级）
                GeyserAPI.initialize(logger);
            } catch (Throwable e) {
                logger.warning("[Init] GeyserAPI initialization failed (local Geyser detection unavailable): " + e.getMessage());
            }
            try {
                // 初始化统一连接服务（整合 GeyserAPI + BungeeClientAPI）
                PlayerConnectionService.initialize(logger);
            } catch (Throwable e) {
                logger.warning("[Init] PlayerConnectionService initialization failed: " + e.getMessage());
            }
            try {
                // 初始化基岩版表单发送器（通过 Geyser API 反射）
                BedrockFormSender.initialize(logger);
            } catch (Throwable e) {
                logger.warning("[Init] BedrockFormSender initialization failed (Bedrock forms unavailable): " + e.getMessage());
            }
            // 注册代理表单响应回调：代理转发的基岩版表单响应 → BedrockFormSender 触发原始 handler
            BungeeClientAPI.setFormResponseCallback((formId, responseData) ->
                    BedrockFormSender.handleFormResponse(formId, responseData));

            // 加载等级需求配置
            loadLevelRequirements();

            // 注册工会服务
            guildService = new GuildService(this);
            serviceContainer.register(GuildService.class, guildService);
            guildPlayerDataCache = new com.guild.core.cache.GuildPlayerDataCache(this, 3000L);
            
            // 设置PlaceholderManager的GuildService引用
            placeholderManager.setGuildService(guildService);
            
            // 启动服务（确保数据库连接在模块加载前初始化）
            startServices();
            
            // 初始化货币管理器（数据库连接初始化后）
            CurrencyManager currencyManager = new CurrencyManager(this);
            serviceContainer.register(CurrencyManager.class, currencyManager);

            // 初始化投资记录服务
            guildInvestmentService = new com.guild.services.GuildInvestmentService(this);
            serviceContainer.register(com.guild.services.GuildInvestmentService.class, guildInvestmentService);

            // 初始化公会聊天管理器
            guildChatManager = new com.guild.chat.GuildChatManager(this);

            // 初始化世界管理系统（虚空世界 + 意外恢复，须在模块加载前就绪）
            guildWorldService = new GuildWorldService(this);
            serviceContainer.register(GuildWorldService.class, guildWorldService);
            guildWorldAPI = new GuildWorldAPIImpl(guildWorldService);
            serviceContainer.register(GuildWorldAPI.class, guildWorldAPI);
            guildWorldService.load();
            if (!guildWorldService.isEnabled()) {
                logger.warning("[World] " + guildWorldService.unsupportedMessage()
                        + " — create/load/unload/delete 与启动恢复已禁用"
                        + "（支持: " + ServerUtils.getFoliaSupportedVersions() + "）");
            } else {
                getServer().getPluginManager().registerEvents(
                        new SelectionListener(this, guildWorldService.getSelections(),
                                guildWorldService.getWandMaterial()), this);
            }

            // 工会战（依赖世界预设系统）
            guildWarService = new GuildWarService(this, guildService, guildWorldService);
            guildWarAPI = new GuildWarAPIImpl(guildWarService);
            warSeasonService = new WarSeasonService(this);
            serviceContainer.register(GuildWarService.class, guildWarService);
            serviceContainer.register(GuildWarAPI.class, guildWarAPI);
            serviceContainer.register(WarSeasonService.class, warSeasonService);
            if (guildWarService.isEnabled()) {
                getServer().getPluginManager().registerEvents(new GuildWarListener(guildWarService), this);
                getServer().getPluginManager().registerEvents(new WarRewardListener(this), this);
                getServer().getPluginManager().registerEvents(warSeasonService, this);
            } else {
                logger.info("[GuildWar] Disabled: " + guildWarService.unavailableReason());
            }

            // 公会仓库（NBTAPI softdepend）
            guildWarehouseService = new com.guild.warehouse.GuildWarehouseService(this);
            serviceContainer.register(com.guild.warehouse.GuildWarehouseService.class, guildWarehouseService);
            getServer().getPluginManager().registerEvents(
                    new com.guild.warehouse.WarehouseListener(guildWarehouseService), this);

            // 内置活跃度 / 混合贡献
            activityBootstrap = new com.guild.activity.ActivityBootstrap(this);
            serviceContainer.register(com.guild.activity.ActivityScoreService.class, activityBootstrap.getScoreService());
            activityBootstrap.start();

            // 初始化模块系统（在所有核心服务就绪后）
            moduleManager = new ModuleManager(this);
            serviceContainer.register(ModuleManager.class, moduleManager);

            // 模块扩展点就绪后注册内置 GuildInfoGUI 按钮
            activityBootstrap.registerInfoButton();

            // 初始化 bStats 数据统计
            int bstatsPluginId = 31803;
            guildMetrics = new GuildMetrics(this, bstatsPluginId);

            // 启动版本检测（GitHub + Modrinth 双源，每日检查）
            updateManager = new UpdateManager(this);
            updateChecker = new UpdateChecker(this, updateManager);
            updateChecker.start();
            
            // 注册命令
            registerCommands();
            
            // 注册监听器
            registerListeners();

            // Register plugin disable protection (before modules are loaded)
            getServer().getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onPluginDisable(PluginDisableEvent event) {
                    if (event.getPlugin() == GuildPlugin.this) {
                        // Bukkit is disabling us (e.g., /reload or plugin manager)
                        // Ensure modules are properly unloaded before Bukkit removes our listeners
                        if (moduleManager != null && !modulesUnloaded) {
                            modulesUnloaded = true;
                            getLogger().info("[Module] Plugin disable detected — unloading all modules");
                            moduleManager.unloadAllModules();
                        }
                    }
                }
            }, this);
            
            // 加载所有扩展模块（在核心服务全部就绪后）
            moduleManager.loadAllModules();
            
            // 启动定时清理任务 - 清理过期邀请
            startCleanupTasks();

            // 启动世界恢复自检（延迟到服务器 RUNNING 状态执行，Folia 兼容）
            if (guildWorldService != null) {
                guildWorldService.scheduleRecovery();
            }
            
            logger.info("Guild Plugin started successfully!");
            logger.info("Compatibility mode: " + (ServerUtils.isFolia() ? "Folia" : "Spigot"));
            if (fileLogger != null) {
                fileLogger.logSystem("Plugin started successfully (compatibility mode: "
                        + (ServerUtils.isFolia() ? "Folia" : "Spigot") + ")");
            }
            
        } catch (Throwable e) {
            logger.severe("Guild Plugin failed to start: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        Logger logger = getLogger();
        logger.info("Shutting down Guild Plugin...");
        
        try {
            // 关闭所有GUI
            if (guiManager != null) {
                guiManager.closeAllGUIs();
            }

            // 优雅结束工会战，再卸载受管世界
            if (guildWarService != null) {
                guildWarService.shutdown();
            }
            if (activityBootstrap != null) {
                activityBootstrap.shutdown();
            }
            if (guildWorldService != null) {
                guildWorldService.shutdown();
            }
            
            // 关闭服务
            if (serviceContainer != null) {
                serviceContainer.shutdown();
            }

            // 关闭 CommAPI 桥接器
            CommAPI.shutdown();
            BungeeClientAPI.shutdown();

            // 关闭 GeyserAPI
            GeyserAPI.shutdown();

            // 关闭基岩版表单发送器（清理待响应表单）
            BedrockFormSender.shutdown();
            
            // 卸载所有扩展模块
            if (moduleManager != null && !modulesUnloaded) {
                modulesUnloaded = true;
                moduleManager.unloadAllModules();
            }

            // 关闭文件日志管理器
            if (fileLogger != null) {
                fileLogger.logSystem("Plugin shutting down...");
                fileLogger.shutdown();
            }

            logger.info("Guild Plugin has been shut down");
            
        } catch (Exception e) {
            logger.severe("Error shutting down Guild Plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void registerCommands() {
        GuildCommand guildCommand = new GuildCommand(this);
        GuildAdminCommand guildAdminCommand = new GuildAdminCommand(this);
        GuildModuleCommand guildModuleCommand = new GuildModuleCommand(this);
        
        getCommand("guild").setExecutor(guildCommand);
        getCommand("guild").setTabCompleter(guildCommand);
        getCommand("guildadmin").setExecutor(guildAdminCommand);
        getCommand("guildadmin").setTabCompleter(guildAdminCommand);
        getCommand("guildmodule").setExecutor(guildModuleCommand);
        getCommand("guildmodule").setTabCompleter(guildModuleCommand);

        // 世界管理命令（/guildworld）
        if (getCommand("guildworld") != null) {
            GuildWorldCommand guildWorldCommand = new GuildWorldCommand(this, guildWorldService);
            getCommand("guildworld").setExecutor(guildWorldCommand);
            getCommand("guildworld").setTabCompleter(guildWorldCommand);
        }

        // 工会战（/guildwar）
        if (getCommand("guildwar") != null && guildWarService != null) {
            GuildWarCommand guildWarCommand = new GuildWarCommand(this, guildWarService);
            getCommand("guildwar").setExecutor(guildWarCommand);
            getCommand("guildwar").setTabCompleter(guildWarCommand);
        }

        // 仅在 Geyser/Cumulus 可用时注册基岩表单测试指令（避免 NoClassDefFoundError）
        if (BedrockFormSender.isAvailable()) {
            BedrockFormTestCommand bedrockFormTestCommand = new BedrockFormTestCommand(this);
            getCommand("bformtest").setExecutor(bedrockFormTestCommand);
            getCommand("bformtest").setTabCompleter(bedrockFormTestCommand);
        }
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GuildListener(this), this);
        getServer().getPluginManager().registerEvents(new GuildHomeProtectListener(this), this);
    }
    
    private void startServices() {
        // 启动数据库连接
        databaseManager.initialize();

        // 数据库备份服务（启动日备 / 版本变化自动备份）
        databaseBackupService = new com.guild.core.backup.DatabaseBackupService(this);
        serviceContainer.register(com.guild.core.backup.DatabaseBackupService.class, databaseBackupService);
        databaseBackupService.maybeAutoBackupOnStartup();
        
        // 注册占位符
        placeholderManager.registerPlaceholders();
        
        // 初始化GUI系统
        guiManager.initialize();

        // 初始化 ImagoCore 图片集成（软依赖，ImagoCore 不存在时静默跳过）
        guiManager.initializeImagoHook();
    }
    
    /**
     * 启动定时清理任务
     */
    private void startCleanupTasks() {
        // 每10分钟清理一次过期邀请（6000 ticks = 5分钟, 乘以2 = 10分钟）
        // 72000 ticks = 1小时
        CompatibleScheduler.runTaskTimer(this, () -> {
            guildService.cleanupExpiredInvitationsAsync()
                .thenAccept(count -> {
                    if (count > 0) {
                        getLogger().info("[Cleanup] Cleaned up " + count + " expired guild invitations");
                    }
                });
            
            // 每24小时清理一次旧的已处理邀请记录（保留30天）
            // 1728000 ticks = 24小时
        }, 1200L, 72000L); // 延迟1分钟启动，之后每5分钟执行一次
    }
    
    public static GuildPlugin getInstance() {
        return instance;
    }
    
    public ServiceContainer getServiceContainer() {
        return serviceContainer;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public EventBus getEventBus() {
        return eventBus;
    }
    
    public GUIManager getGuiManager() {
        return guiManager;
    }
    
    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }
    
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public GuildService getGuildService() {
        return guildService;
    }

    public com.guild.services.GuildInvestmentService getGuildInvestmentService() {
        return guildInvestmentService;
    }

    public com.guild.chat.GuildChatManager getGuildChatManager() {
        return guildChatManager;
    }

    public GuildWorldService getGuildWorldService() {
        return guildWorldService;
    }

    public GuildWorldAPI getGuildWorldAPI() {
        return guildWorldAPI;
    }

    public GuildWarService getGuildWarService() {
        return guildWarService;
    }

    public GuildWarAPI getGuildWarAPI() {
        return guildWarAPI;
    }

    public com.guild.warehouse.GuildWarehouseService getGuildWarehouseService() {
        return guildWarehouseService;
    }

    public com.guild.core.backup.DatabaseBackupService getDatabaseBackupService() {
        return databaseBackupService;
    }

    public com.guild.activity.ActivityScoreService getActivityScoreService() {
        return activityBootstrap != null ? activityBootstrap.getScoreService() : null;
    }

    public WarSeasonService getWarSeasonService() {
        return warSeasonService;
    }
    
    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public GuildMetrics getGuildMetrics() {
        return guildMetrics;
    }

    public UpdateManager getUpdateManager() {
        return updateManager;
    }

    public PluginFileLogger getFileLogger() {
        return fileLogger;
    }

    /**
     * 从配置加载等级需求映射并提供访问方法
     */
    private void loadLevelRequirements() {
        reloadLevelRequirements();
    }

    /**
     * 热重载公会等级上限与升级资金需求表（ConfigManager）。
     */
    public void reloadLevelRequirements() {
        try {
            levelRequirements.clear();
            var cfg = getConfigManager() != null ? getConfigManager().getMainConfig() : getConfig();
            int cfgMax = cfg.getInt("guild.max-level", 10);
            this.maxGuildLevel = Math.max(1, cfgMax);
            for (int lvl = 1; lvl < maxGuildLevel; lvl++) {
                double val = cfg.getDouble("guild.levels." + lvl, getDefaultRequirementForLevel(lvl));
                levelRequirements.put(lvl, val);
            }
        } catch (Exception e) {
            getLogger().warning("Failed to load level requirements config, using built-in defaults: " + e.getMessage());
            for (int lvl = 1; lvl < maxGuildLevel; lvl++) {
                levelRequirements.put(lvl, getDefaultRequirementForLevel(lvl));
            }
        }
    }

    /**
     * 完善热重载：刷新 ConfigManager、同步 Bukkit getConfig、并通知各运行时服务。
     * <p>不重连数据库 / Bungee；语言文件由调用方异步重载。
     */
    public void reloadRuntimeConfiguration() {
        if (configManager != null) {
            configManager.reloadAllConfigs();
        }
        // 同步 Bukkit 配置快照，避免遗留 plugin.getConfig() 调用读到启动时缓存
        try {
            reloadConfig();
        } catch (Exception e) {
            getLogger().warning("[Reload] Bukkit reloadConfig failed: " + e.getMessage());
        }

        if (permissionManager != null) {
            permissionManager.reloadFromConfig();
        }
        if (guildPlayerDataCache != null) {
            guildPlayerDataCache.invalidateAll();
        }
        reloadLevelRequirements();
        com.guild.core.utils.PlaceholderUtils.reloadRoleConfigCache();

        if (guildChatManager != null) {
            guildChatManager.reloadConfig();
        }
        if (guildWorldService != null) {
            guildWorldService.reloadSettings();
        }
        if (guildWarService != null) {
            guildWarService.reloadSettings();
        }
        if (guildWarehouseService != null) {
            guildWarehouseService.reload();
        }
        if (activityBootstrap != null) {
            activityBootstrap.reload();
        }
        if (guiManager != null) {
            guiManager.reloadImagoConfig();
        }
        if (cloudModuleRepository != null) {
            cloudModuleRepository.reloadFromConfig();
        }

        // 模块 GUI 外观覆盖 gui-config.yml
        if (moduleManager != null) {
            for (com.guild.sdk.gui.ModuleGUIRegistration reg
                    : moduleManager.getRegistry().getCustomGUIRegistrations()) {
                com.guild.sdk.gui.ModuleGUIConfig cfg = reg.getConfig();
                if (cfg instanceof com.guild.core.module.config.DefaultModuleGUIConfig dmc) {
                    dmc.reload();
                }
            }
        }

        if (fileLogger != null) {
            fileLogger.logSystem("Runtime configuration reloaded");
        }
    }

    public void setCloudModuleRepository(com.guild.module.cloud.CloudModuleRepository repo) {
        this.cloudModuleRepository = repo;
    }

    public com.guild.module.cloud.CloudModuleRepository getCloudModuleRepository() {
        return cloudModuleRepository;
    }

    public com.guild.core.cache.GuildPlayerDataCache getGuildPlayerDataCache() {
        return guildPlayerDataCache;
    }

    public com.guild.activity.ActivityBootstrap getActivityBootstrap() {
        return activityBootstrap;
    }

    private double getDefaultRequirementForLevel(int level) {
        switch (level) {
            case 1: return 5000;
            case 2: return 10000;
            case 3: return 20000;
            case 4: return 35000;
            case 5: return 50000;
            case 6: return 75000;
            case 7: return 100000;
            case 8: return 150000;
            case 9: return 200000;
            default: return 0;
        }
    }

    public int getMaxGuildLevel() {
        return maxGuildLevel;
    }

    public double getRequirementForNextLevel(int currentLevel) {
        if (currentLevel >= maxGuildLevel) return 0;
        return levelRequirements.getOrDefault(currentLevel, getDefaultRequirementForLevel(currentLevel));
    }

}
