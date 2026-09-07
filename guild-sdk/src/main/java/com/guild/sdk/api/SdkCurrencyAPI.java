package com.guild.sdk.api;

import java.util.UUID;

/**
 * 模块货币域 API（字符串类型标识，兼容编译期 SDK）。
 *
 * @since 1.6.7
 */
public interface SdkCurrencyAPI {

    double getCurrencyBalance(int guildId, UUID playerUuid, String currencyType);

    boolean depositCurrency(int guildId, UUID playerUuid, String playerName,
                            String currencyType, double amount);

    boolean withdrawCurrency(int guildId, UUID playerUuid, String currencyType, double amount);
}
