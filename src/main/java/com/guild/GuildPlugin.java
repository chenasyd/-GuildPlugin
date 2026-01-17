package com.guild;

import com.guild.core.ServiceContainer;
import com.guild.core.config.ConfigManager;
import com.guild.core.database.DatabaseManager;
import com.guild.core.events.EventBus;
import com.guild.core.gui.GUIManager;
import com.guild.core.placeholder.PlaceholderManager;
import com.guild.core.permissions.PermissionManager;
import com.guild.core.economy.EconomyManager;
import com.guild.commands.GuildCommand;
import com.guild.commands.GuildAdminCommand;
import com.guild.listeners.PlayerListener;
import com.guild.listeners.GuildListener;
import com.guild.services.GuildService;
import com.guild.core.utils.ServerUtils;
import com.guild.core.utils.TestUtils;
import com.guild.utils.FoliaUtils;
import com.guild.utils.TeleportManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

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
    private GuildService guildService;
    private boolean folia = false; // Folia 标识（在 onEnable 中初始化）
    
    @Override
    public void onEnable() {
        instance = this;
        Logger logger = getLogger();
        
        logger.info("正在启动工会插件...");
        logger.info("检测到服务器类型: " + ServerUtils.getServerType());
        logger.info("服务器版本: " + ServerUtils.getServerVersion());
        
        // 检查API版本兼容性
        if (!ServerUtils.supportsApiVersion("1.21")) {
            logger.severe("此插件需要1.21或更高版本！当前版本: " + ServerUtils.getServerVersion());
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
            
            // 注册工会服务
            guildService = new GuildService(this);
            serviceContainer.register(GuildService.class, guildService);
            
            // 设置PlaceholderManager的GuildService引用
            placeholderManager.setGuildService(guildService);
            
            // 注册命令
            registerCommands();
            
            // 注册监听器
            registerListeners();
            
            // 启动服务
            startServices();
            
            // 检测是否为 Folia，并记录日志
            this.folia = FoliaUtils.isFolia();
            if (this.folia) {
                getLogger().warning("检测到 Folia 服务端，插件内的传送功能将被禁用以避免触发看门狗。");
            }
            
            logger.info("工会插件启动成功！");
            logger.info("兼容模式: " + (ServerUtils.isFolia() ? "Folia" : "Spigot"));
            
        } catch (Exception e) {
            logger.severe("工会插件启动失败: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        Logger logger = getLogger();
        logger.info("正在关闭工会插件...");
        
        try {
            // 关闭所有GUI
            if (guiManager != null) {
                guiManager.closeAllGUIs();
            }
            
            // 关闭服务
            if (serviceContainer != null) {
                serviceContainer.shutdown();
            }
            
            logger.info("工会插件已关闭");
            
        } catch (Exception e) {
            logger.severe("关闭工会插件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void registerCommands() {
        GuildCommand guildCommand = new GuildCommand(this);
        GuildAdminCommand guildAdminCommand = new GuildAdminCommand(this);
        
        getCommand("guild").setExecutor(guildCommand);
        getCommand("guild").setTabCompleter(guildCommand);
        getCommand("guildadmin").setExecutor(guildAdminCommand);
        getCommand("guildadmin").setTabCompleter(guildAdminCommand);
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
    
    public GuildService getGuildService() {
        return guildService;
    }
    
    public boolean isFolia() {
        return folia;
    }

    /**
     * 插件内部使用的安全传送封装：
     * - 如果是 Folia，则阻止并返回 false（已经通知玩家）
     * - 否则在主线程执行传送并返回 true
     */
    public boolean safeTeleport(Player player, Location target) {
        return TeleportManager.teleport(this, player, target);
    }
}
