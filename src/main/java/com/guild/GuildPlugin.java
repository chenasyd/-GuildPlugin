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

    @Override
    public void onEnable() {
        instance = this;
        Logger logger = getLogger();

        logger.info("Uruchamianie pluginu Gildie...");
        logger.info("Wykryto typ serwera: " + ServerUtils.getServerType());
        logger.info("Wersja serwera: " + ServerUtils.getServerVersion());

        // Sprawdź kompatybilność wersji API
        if (!ServerUtils.supportsApiVersion("1.21")) {
            logger.severe("Ten plugin wymaga wersji 1.21 lub nowszej! Obecna wersja: " + ServerUtils.getServerVersion());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Uruchom testy kompatybilności (używając loggera pluginu)
        TestUtils.testCompatibility(logger);
        TestUtils.testSchedulerCompatibility(logger);

        try {
            // Inicjalizacja kontenera usług
            serviceContainer = new ServiceContainer();

            // Inicjalizacja menedżera konfiguracji
            configManager = new ConfigManager(this);
            serviceContainer.register(ConfigManager.class, configManager);

            // Inicjalizacja menedżera bazy danych
            databaseManager = new DatabaseManager(this);
            serviceContainer.register(DatabaseManager.class, databaseManager);

            // Inicjalizacja szyny zdarzeń
            eventBus = new EventBus();
            serviceContainer.register(EventBus.class, eventBus);

            // Inicjalizacja menedżera GUI
            guiManager = new GUIManager(this);
            serviceContainer.register(GUIManager.class, guiManager);

            // Inicjalizacja menedżera placeholderów
            placeholderManager = new PlaceholderManager(this);
            serviceContainer.register(PlaceholderManager.class, placeholderManager);

            // Inicjalizacja menedżera uprawnień
            permissionManager = new PermissionManager(this);
            serviceContainer.register(PermissionManager.class, permissionManager);

            // Inicjalizacja menedżera ekonomii
            economyManager = new EconomyManager(this);
            serviceContainer.register(EconomyManager.class, economyManager);

            // Rejestracja usługi gildii
            guildService = new GuildService(this);
            serviceContainer.register(GuildService.class, guildService);

            // Ustaw referencję GuildService w PlaceholderManager
            placeholderManager.setGuildService(guildService);

            // Rejestracja komend
            registerCommands();

            // Rejestracja listenerów
            registerListeners();

            // Uruchomienie usług
            startServices();

            logger.info("Plugin Gildie został pomyślnie uruchomiony!");
            logger.info("Tryb kompatybilności: " + (ServerUtils.isFolia() ? "Folia" : "Spigot"));

        } catch (Exception e) {
            logger.severe("Nie udało się uruchomić pluginu Gildie: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        Logger logger = getLogger();
        logger.info("Zamykanie pluginu Gildie...");

        try {
            // Zamknij wszystkie GUI
            if (guiManager != null) {
                guiManager.closeAllGUIs();
            }

            // Zamknij usługi
            if (serviceContainer != null) {
                serviceContainer.shutdown();
            }

            logger.info("Plugin Gildie został wyłączony");

        } catch (Exception e) {
            logger.severe("Wystąpił błąd podczas zamykania pluginu Gildie: " + e.getMessage());
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
        // Uruchom połączenie z bazą danych
        databaseManager.initialize();

        // Zarejestruj placeholdery
        placeholderManager.registerPlaceholders();

        // Inicjalizacja systemu GUI
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
}
