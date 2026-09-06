package com.guild.module.example.territory;

import com.guild.core.events.EventBus;
import com.guild.core.module.ModuleContext;
import com.guild.sdk.event.territory.TerritoryClaimedEventData;
import com.guild.sdk.territory.TerritoryEventSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class TerritoryEventEmitterTest {

    @Test
    void fireClaimed_publishesToEventBus() {
        EventBus bus = new EventBus();
        AtomicReference<TerritoryClaimedEventData> captured = new AtomicReference<>();
        bus.subscribe(TerritoryClaimedEventData.class, captured::set);

        ModuleContext context = Mockito.mock(ModuleContext.class);
        TerritorySettings settings = Mockito.mock(TerritorySettings.class);
        when(context.getEventBus()).thenReturn(bus);
        when(settings.isEventsEnabled()).thenReturn(true);

        TerritoryRepository repository = new TerritoryRepository(
                new java.io.File(System.getProperty("java.io.tmpdir")),
                java.util.logging.Logger.getAnonymousLogger());
        TerritoryEventEmitter emitter = new TerritoryEventEmitter(context, repository, settings);

        long now = System.currentTimeMillis();
        TerritoryRecord record = new TerritoryRecord(
                3, "G3", TerritoryRecord.defaultRegionId(3), "local-test", "world",
                0, 0, 0, 2, 2, 2, now, TerritorySyncState.MATERIALIZED, now);
        UUID actor = UUID.randomUUID();

        emitter.fireClaimed(record, actor, TerritoryEventSource.PLAYER, 100.0);

        TerritoryClaimedEventData event = captured.get();
        assertNotNull(event);
        assertEquals(3, event.getTerritory().getGuildId());
        assertEquals(actor, event.getActorUuid());
        assertEquals(TerritoryEventSource.PLAYER, event.getSource());
        assertEquals(100.0, event.getCostPaid());
    }

    @Test
    void fireClaimed_skippedWhenDisabled() {
        EventBus bus = new EventBus();
        AtomicReference<TerritoryClaimedEventData> captured = new AtomicReference<>();
        bus.subscribe(TerritoryClaimedEventData.class, captured::set);

        ModuleContext context = Mockito.mock(ModuleContext.class);
        TerritorySettings settings = Mockito.mock(TerritorySettings.class);
        when(context.getEventBus()).thenReturn(bus);
        when(settings.isEventsEnabled()).thenReturn(false);

        TerritoryEventEmitter emitter = new TerritoryEventEmitter(context, null, settings);
        emitter.fireClaimed(null, null, TerritoryEventSource.PLAYER, 0);

        assertEquals(null, captured.get());
    }
}
