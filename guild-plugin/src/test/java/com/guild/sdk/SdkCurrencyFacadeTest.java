package com.guild.sdk;

import com.guild.sdk.economy.CurrencyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SdkCurrencyFacadeTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private CurrencyManager currencyManager;
    private SdkCurrencyFacade facade;

    @BeforeEach
    void setUp() {
        currencyManager = mock(CurrencyManager.class);
        facade = new SdkCurrencyFacade(currencyManager);
    }

    @Test
    void getCurrencyManager_returnsDelegate() {
        assertSame(currencyManager, facade.getCurrencyManager());
    }

    @Test
    void parseCurrencyType_acceptsCaseInsensitiveNames() {
        assertEquals(CurrencyManager.CurrencyType.A_COIN, SdkCurrencyFacade.parseCurrencyType("a_coin"));
        assertEquals(CurrencyManager.CurrencyType.B_COIN, SdkCurrencyFacade.parseCurrencyType("B_COIN"));
    }

    @Test
    void parseCurrencyType_invalidReturnsNull() {
        assertEquals(null, SdkCurrencyFacade.parseCurrencyType("not_a_coin"));
    }

    @Test
    void getCurrencyBalance_invalidStringType_returnsZeroWithoutCallingManager() {
        assertEquals(0.0, facade.getCurrencyBalance(1, PLAYER, "invalid"));

        verifyNoInteractions(currencyManager);
    }

    @Test
    void getCurrencyBalanceAsync_invalidStringType_returnsCompletedZero() {
        assertEquals(0.0, facade.getCurrencyBalanceAsync(1, PLAYER, "invalid").join());

        verifyNoInteractions(currencyManager);
    }

    @Test
    void depositCurrency_invalidStringType_returnsFalse() {
        assertFalse(facade.depositCurrency(1, PLAYER, "Name", "invalid", 10.0));

        verifyNoInteractions(currencyManager);
    }

    @Test
    void withdrawCurrencyAsync_invalidStringType_returnsCompletedFalse() {
        assertFalse(facade.withdrawCurrencyAsync(1, PLAYER, "invalid", 5.0).join());

        verifyNoInteractions(currencyManager);
    }

    @Test
    void getCurrencyBalance_validStringType_delegatesToManager() {
        when(currencyManager.getBalance(1, PLAYER, CurrencyManager.CurrencyType.C_COIN)).thenReturn(42.0);

        assertEquals(42.0, facade.getCurrencyBalance(1, PLAYER, "c_coin"));

        verify(currencyManager).getBalance(1, PLAYER, CurrencyManager.CurrencyType.C_COIN);
    }

    @Test
    void depositCurrencyAsync_validEnumType_delegatesToManager() {
        CompletableFuture<Boolean> expected = CompletableFuture.completedFuture(true);
        when(currencyManager.depositAsync(2, PLAYER, "P", CurrencyManager.CurrencyType.A_COIN, 1.0))
                .thenReturn(expected);

        assertTrue(facade.depositCurrencyAsync(2, PLAYER, "P", CurrencyManager.CurrencyType.A_COIN, 1.0).join());
    }
}
