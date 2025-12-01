package com.guild.core.economy;

import com.guild.GuildPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Logger;

import com.guild.core.utils.CompatibleScheduler;

/**
 * Menedżer ekonomii - zarządza integracją z systemem ekonomii Vault
 */
public class EconomyManager {

    private final GuildPlugin plugin;
    private final Logger logger;
    private Economy economy;
    private boolean vaultAvailable = false;

    public EconomyManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        setupEconomy();
    }

    /**
     * Skonfiguruj system ekonomii
     */
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logger.warning("Nie znaleziono pluginu Vault, funkcje ekonomii zostaną wyłączone!");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.warning("Nie znaleziono dostawcy usług ekonomii, funkcje ekonomii zostaną wyłączone!");
            return;
        }

        economy = rsp.getProvider();
        if (economy == null) {
            logger.warning("Inicjalizacja dostawcy usług ekonomii nie powiodła się, funkcje ekonomii zostaną wyłączone!");
            return;
        }

        vaultAvailable = true;
        logger.info("System ekonomii zainicjalizowany pomyślnie!");
    }

    /**
     * Sprawdź czy Vault jest dostępny
     */
    public boolean isVaultAvailable() {
        return vaultAvailable && economy != null;
    }

    /**
     * Pobierz saldo gracza
     */
    public double getBalance(Player player) {
        if (!isVaultAvailable()) {
            return 0.0;
        }
        return economy.getBalance(player);
    }

    /**
     * Sprawdź czy gracz ma wystarczające środki
     */
    public boolean hasBalance(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }
        return economy.has(player, amount);
    }

    /**
     * Pobierz środki od gracza
     */
    public boolean withdraw(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Dodaj środki graczowi
     */
    public boolean deposit(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Formatuj walutę
     */
    public String format(double amount) {
        if (!isVaultAvailable()) {
            return String.format("%.2f", amount);
        }
        return economy.format(amount);
    }

    /**
     * Pobierz nazwę waluty
     */
    public String getCurrencyName() {
        if (!isVaultAvailable()) {
            return "Monety";
        }
        return economy.currencyNamePlural();
    }

    /**
     * Pobierz nazwę waluty w liczbie pojedynczej
     */
    public String getCurrencyNameSingular() {
        if (!isVaultAvailable()) {
            return "Moneta";
        }
        return economy.currencyNameSingular();
    }

    /**
     * Sprawdź czy gracz ma wystarczające środki (asynchronicznie)
     */
    public boolean hasBalanceAsync(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }

        // Upewnij się, że wykonujesz w głównym wątku
        if (!CompatibleScheduler.isPrimaryThread()) {
            return false;
        }

        return economy.has(player, amount);
    }

    /**
     * Pobierz środki od gracza (asynchronicznie)
     */
    public boolean withdrawAsync(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }

        // Upewnij się, że wykonujesz w głównym wątku
        if (!CompatibleScheduler.isPrimaryThread()) {
            return false;
        }

        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Dodaj środki graczowi (asynchronicznie)
     */
    public boolean depositAsync(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }

        // Upewnij się, że wykonujesz w głównym wątku
        if (!CompatibleScheduler.isPrimaryThread()) {
            return false;
        }

        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Pobierz instancję ekonomii
     */
    public Economy getEconomy() {
        return economy;
    }
}
