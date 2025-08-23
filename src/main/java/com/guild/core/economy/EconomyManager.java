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
            logger.warning("Vault插件未找到，经济功能将被禁用！");
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.warning("未找到经济服务提供者，经济功能将被禁用！");
            return;
        }
        
        economy = rsp.getProvider();
        if (economy == null) {
            logger.warning("经济服务提供者初始化失败，经济功能将被禁用！");
            return;
        }
        
        vaultAvailable = true;
        logger.info("经济系统初始化成功！");
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
            return "金币";
        }
        return economy.currencyNamePlural();
    }
    
    /**
     * 获取货币单数名称
     */
    public String getCurrencyNameSingular() {
        if (!isVaultAvailable()) {
            return "金币";
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
     * 获取经济实例
     */
    public Economy getEconomy() {
        return economy;
    }
}
