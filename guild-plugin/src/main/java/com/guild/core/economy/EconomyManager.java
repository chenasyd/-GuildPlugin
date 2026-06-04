package com.guild.core.economy;

import com.guild.GuildPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Logger;

import com.guild.core.utils.CompatibleScheduler;

/**
 * 经济管理器 - 管理Vault经济系统集成
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
     * 设置经济系统
     */
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logger.warning("Vault plugin not found, economy features will be disabled!");
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.warning("No economy service provider found, economy features will be disabled!");
            return;
        }
        
        economy = rsp.getProvider();
        if (economy == null) {
            logger.warning("Economy service provider initialization failed, economy features will be disabled!");
            return;
        }
        
        vaultAvailable = true;
        logger.info("Economy system initialized successfully!");
    }
    
    /**
     * 检查Vault是否可用
     */
    public boolean isVaultAvailable() {
        return vaultAvailable && economy != null;
    }
    
    /**
     * 获取玩家余额
     */
    public double getBalance(Player player) {
        if (!isVaultAvailable()) {
            return 0.0;
        }
        return economy.getBalance(player);
    }
    
    /**
     * 检查玩家是否有足够的余额
     */
    public boolean hasBalance(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }
        return economy.has(player, amount);
    }
    
    /**
     * 扣除玩家余额
     */
    public boolean withdraw(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    /**
     * 增加玩家余额
     */
    public boolean deposit(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    
    /**
     * 格式化货币
     */
    public String format(double amount) {
        if (!isVaultAvailable()) {
            return String.format("%.2f", amount);
        }
        return economy.format(amount);
    }
    
    /**
     * 获取货币名称
     */
    public String getCurrencyName() {
        if (!isVaultAvailable()) {
            return "Coins";
        }
        return economy.currencyNamePlural();
    }
    
    /**
     * 获取货币单数名称
     */
    public String getCurrencyNameSingular() {
        if (!isVaultAvailable()) {
            return "Coin";
        }
        return economy.currencyNameSingular();
    }
    
    /**
     * 检查玩家是否有足够的余额（异步）
     */
    public boolean hasBalanceAsync(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }
        
        // 确保在主线程中执行
        if (!CompatibleScheduler.isPrimaryThread()) {
            return false;
        }
        
        return economy.has(player, amount);
    }
    
    /**
     * 扣除玩家余额（异步）
     */
    public boolean withdrawAsync(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }
        
        // 确保在主线程中执行
        if (!CompatibleScheduler.isPrimaryThread()) {
            return false;
        }
        
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    /**
     * 增加玩家余额（异步）
     */
    public boolean depositAsync(Player player, double amount) {
        if (!isVaultAvailable()) {
            return false;
        }
        
        // 确保在主线程中执行
        if (!CompatibleScheduler.isPrimaryThread()) {
            return false;
        }
        
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    
    /**
     * 检查是否启用了无经济模式（跳过所有经济要求）
     */
    public boolean isNoEconomyMode() {
        return plugin.getConfigManager().getMainConfig().getBoolean("guild.no-economy-mode", false);
    }

    /**
     * 获取经济实例
     */
    public Economy getEconomy() {
        return economy;
    }
}
