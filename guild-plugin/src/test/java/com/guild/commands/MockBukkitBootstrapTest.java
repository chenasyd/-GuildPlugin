package com.guild.commands;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * MockBukkit 环境冒烟：确认测试 classpath 可启动模拟 Bukkit 服务器。
 */
class MockBukkitBootstrapTest {

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void mockBukkitProvidesWorkingServer() {
        ServerMock server = MockBukkit.mock();
        assertNotNull(server);
        assertNotNull(Bukkit.getServer());
    }
}
