package com.guild.sdk;

import com.guild.sdk.economy.CurrencyManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SDK 货币 API 门面：委托 {@link CurrencyManager}，统一字符串类型解析与非法值兜底。
 */
public final class SdkCurrencyFacade {

    private final CurrencyManager currencyManager;

    public SdkCurrencyFacade(CurrencyManager currencyManager) {
        this.currencyManager = currencyManager;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public double getCurrencyBalance(int guildId, UUID playerUuid, CurrencyManager.CurrencyType currencyType) {
        return currencyManager.getBalance(guildId, playerUuid, currencyType);
    }

    public CompletableFuture<Double> getCurrencyBalanceAsync(int guildId, UUID playerUuid,
                                                             CurrencyManager.CurrencyType currencyType) {
        return currencyManager.getBalanceAsync(guildId, playerUuid, currencyType);
    }

    public boolean depositCurrency(int guildId, UUID playerUuid, String playerName,
                                   CurrencyManager.CurrencyType currencyType, double amount) {
        return currencyManager.deposit(guildId, playerUuid, playerName, currencyType, amount);
    }

    public CompletableFuture<Boolean> depositCurrencyAsync(int guildId, UUID playerUuid, String playerName,
                                                           CurrencyManager.CurrencyType currencyType, double amount) {
        return currencyManager.depositAsync(guildId, playerUuid, playerName, currencyType, amount);
    }

    public boolean withdrawCurrency(int guildId, UUID playerUuid,
                                    CurrencyManager.CurrencyType currencyType, double amount) {
        return currencyManager.withdraw(guildId, playerUuid, currencyType, amount);
    }

    public CompletableFuture<Boolean> withdrawCurrencyAsync(int guildId, UUID playerUuid,
                                                            CurrencyManager.CurrencyType currencyType, double amount) {
        return currencyManager.withdrawAsync(guildId, playerUuid, currencyType, amount);
    }

    public double getCurrencyBalance(int guildId, UUID playerUuid, String currencyType) {
        CurrencyManager.CurrencyType parsed = parseCurrencyType(currencyType);
        if (parsed == null) {
            return 0.0;
        }
        return currencyManager.getBalance(guildId, playerUuid, parsed);
    }

    public CompletableFuture<Double> getCurrencyBalanceAsync(int guildId, UUID playerUuid, String currencyType) {
        CurrencyManager.CurrencyType parsed = parseCurrencyType(currencyType);
        if (parsed == null) {
            return CompletableFuture.completedFuture(0.0);
        }
        return currencyManager.getBalanceAsync(guildId, playerUuid, parsed);
    }

    public boolean depositCurrency(int guildId, UUID playerUuid, String playerName,
                                   String currencyType, double amount) {
        CurrencyManager.CurrencyType parsed = parseCurrencyType(currencyType);
        if (parsed == null) {
            return false;
        }
        return currencyManager.deposit(guildId, playerUuid, playerName, parsed, amount);
    }

    public CompletableFuture<Boolean> depositCurrencyAsync(int guildId, UUID playerUuid, String playerName,
                                                           String currencyType, double amount) {
        CurrencyManager.CurrencyType parsed = parseCurrencyType(currencyType);
        if (parsed == null) {
            return CompletableFuture.completedFuture(false);
        }
        return currencyManager.depositAsync(guildId, playerUuid, playerName, parsed, amount);
    }

    public boolean withdrawCurrency(int guildId, UUID playerUuid, String currencyType, double amount) {
        CurrencyManager.CurrencyType parsed = parseCurrencyType(currencyType);
        if (parsed == null) {
            return false;
        }
        return currencyManager.withdraw(guildId, playerUuid, parsed, amount);
    }

    public CompletableFuture<Boolean> withdrawCurrencyAsync(int guildId, UUID playerUuid,
                                                            String currencyType, double amount) {
        CurrencyManager.CurrencyType parsed = parseCurrencyType(currencyType);
        if (parsed == null) {
            return CompletableFuture.completedFuture(false);
        }
        return currencyManager.withdrawAsync(guildId, playerUuid, parsed, amount);
    }

    static CurrencyManager.CurrencyType parseCurrencyType(String currencyType) {
        try {
            return CurrencyManager.CurrencyType.valueOf(currencyType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
