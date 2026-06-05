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
import com.guild.listeners.PlayerListener;
import com.guild.listeners.GuildListener;
import com.guild.services.GuildService;
import com.guild.core.module.ModuleManager;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ServerUtils;
import com.guild.core.utils.TestUtils;
import com.guild.metrics.GuildMetrics;
import com.guild.update.UpdateChecker;
import com.guild.update.UpdateManager;
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
    private ModuleManager moduleManager;
    private GuildMetrics guildMetrics;
    private UpdateManager updateManager;
    private UpdateChecker updateChecker;
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
        
        // 检查API版本兼容性
        if (!ServerUtils.supportsApiVersion("1.21")) {
            logger.severe("This plugin requires 1.21 or higher! Current version: " + ServerUtils.getServerVersion());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 运行兼容性测试（使用插件日志器）
        TestUtils.testCompatibility(logger);
        TestUtils.testSchedulerCompatibility(logger);
        
        try {
            // 初始化服务容器
            serviceContainer = new ServiceContainer();
            
            // 初始化配置管理器
            configManager = new ConfigManager(this);
            serviceContainer.register(ConfigManager.class, configManager);
            
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
            // 加载等级需求配置
            loadLevelRequirements();

            // 注册工会服务
            guildService = new GuildService(this);
            serviceContainer.register(GuildService.class, guildService);
            
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
            
            // 初始化模块系统（在所有核心服务就绪后）
            moduleManager = new ModuleManager(this);
            serviceContainer.register(ModuleManager.class, moduleManager);

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
            
            // 加载所有扩展模块（在核心服务全部就绪后）
            moduleManager.loadAllModules();
            
            // 启动定时清理任务 - 清理过期邀请
            startCleanupTasks();
            
            logger.info("Guild Plugin started successfully!");
            logger.info("Compatibility mode: " + (ServerUtils.isFolia() ? "Folia" : "Spigot"));
            
        } catch (Exception e) {
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
            
            // 关闭服务
            if (serviceContainer != null) {
                serviceContainer.shutdown();
            }
            
            // 卸载所有扩展模块
            if (moduleManager != null) {
                moduleManager.unloadAllModules();
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
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GuildListener(this), this);
    }
    
    private void startServices() {
        // 启动数据库连接
        databaseManager.initialize();
        
        // 注册占位符
        placeholderManager.registerPlaceholders();
        
        // 初始化GUI系统
        guiManager.initialize();
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
    
    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public GuildMetrics getGuildMetrics() {
        return guildMetrics;
    }

    public UpdateManager getUpdateManager() {
        return updateManager;
    }

    /**
     * 从配置加载等级需求映射并提供访问方法
     */
    private void loadLevelRequirements() {
        try {
            int cfgMax = getConfig().getInt("guild.max-level", 10);
            this.maxGuildLevel = Math.max(1, cfgMax);
            for (int lvl = 1; lvl < maxGuildLevel; lvl++) {
                double val = getConfig().getDouble("guild.levels." + lvl, getDefaultRequirementForLevel(lvl));
                levelRequirements.put(lvl, val);
            }
        } catch (Exception e) {
            getLogger().warning("Failed to load level requirements config, using built-in defaults: " + e.getMessage());
            for (int lvl = 1; lvl < maxGuildLevel; lvl++) {
                levelRequirements.put(lvl, getDefaultRequirementForLevel(lvl));
            }
        }
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
