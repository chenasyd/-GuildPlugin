package com.guild.sdk;

import com.guild.sdk.event.GuildEventData;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventHandler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleEventBusTest {

    @Test
    void fireGuildCreate_invokesRegisteredHandler() {
        ModuleEventBus bus = new ModuleEventBus(Logger.getLogger("test"));
        AtomicInteger calls = new AtomicInteger();
        bus.onGuildCreate(data -> calls.incrementAndGet());

        bus.fireGuildCreate(1, "TestGuild", "Leader");

        assertEquals(1, calls.get());
    }

    @Test
    void fireGuildCreate_passesEventData() {
        ModuleEventBus bus = new ModuleEventBus(Logger.getLogger("test"));
        AtomicInteger guildId = new AtomicInteger();
        bus.onGuildCreate(data -> guildId.set(data.getGuildId()));

        bus.fireGuildCreate(42, "Name", "Leader");

        assertEquals(42, guildId.get());
    }

    @Test
    void fireGuildCreate_skipsWhenNoHandlers() {
        ModuleEventBus bus = new ModuleEventBus(Logger.getLogger("test"));
        bus.fireGuildCreate(1, "TestGuild", "Leader");
    }

    @Test
    void fireGuildCreate_continuesAfterHandlerException() {
        ModuleEventBus bus = new ModuleEventBus(Logger.getLogger("test"));
        AtomicInteger calls = new AtomicInteger();
        bus.onGuildCreate(data -> {
            throw new RuntimeException("boom");
        });
        bus.onGuildCreate(data -> calls.incrementAndGet());

        bus.fireGuildCreate(1, "TestGuild", "Leader");

        assertEquals(1, calls.get());
    }

    @Test
    void clearModuleHandlers_removesMatchingHandlers() {
        ModuleEventBus bus = new ModuleEventBus(Logger.getLogger("test"));
        Object moduleA = new Object();
        Object moduleB = new Object();
        AtomicInteger calls = new AtomicInteger();

        bus.onGuildCreate(handlerFor(moduleA));
        bus.onGuildCreate(handlerFor(moduleB));
        bus.onMemberJoin(handlerFor(moduleA, calls));

        bus.clearModuleHandlers(moduleA);

        bus.fireGuildCreate(1, "G", "L");
        bus.fireMemberJoin(1, "G", java.util.UUID.randomUUID(), "P");

        assertEquals(0, calls.get());
    }

    private static GuildEventHandler handlerFor(Object moduleInstance) {
        return new GuildEventHandler() {
            @Override
            public void onEvent(GuildEventData data) {
            }

            @Override
            public Object getModuleInstance() {
                return moduleInstance;
            }
        };
    }

    private static MemberEventHandler handlerFor(Object moduleInstance, AtomicInteger calls) {
        return new MemberEventHandler() {
            @Override
            public void onEvent(com.guild.sdk.event.MemberEventData data) {
                calls.incrementAndGet();
            }

            @Override
            public Object getModuleInstance() {
                return moduleInstance;
            }
        };
    }

    @Test
    void clearAll_removesAllHandlers() {
        ModuleEventBus bus = new ModuleEventBus(Logger.getLogger("test"));
        AtomicInteger calls = new AtomicInteger();
        bus.onGuildCreate(data -> calls.incrementAndGet());

        bus.clearAll();
        bus.fireGuildCreate(1, "G", "L");

        assertEquals(0, calls.get());
    }
}
