package com.guild.core.module;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CoreActivityBridgeTest {

    @Test
    void isCoreActivityEnabled_nullContext_returnsFalse() {
        assertFalse(CoreActivityBridge.isCoreActivityEnabled(null));
    }
}
