package com.guild.core.module;

import com.guild.activity.ActivityScoreService;
import com.guild.activity.ActivitySettings;
import com.guild.core.ServiceContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoreActivityBridgeTest {

    @Test
    void isCoreActivityEnabled_nullContext_returnsFalse() {
        assertFalse(CoreActivityBridge.isCoreActivityEnabled(null));
    }

    @Test
    void isCoreActivityEnabled_missingService_returnsFalse() {
        ModuleContext context = mock(ModuleContext.class);
        ServiceContainer container = mock(ServiceContainer.class);
        when(context.getServiceContainer()).thenReturn(container);
        when(container.get(ActivityScoreService.class)).thenReturn(null);

        assertFalse(CoreActivityBridge.isCoreActivityEnabled(context));
    }

    @Test
    void isCoreActivityEnabled_disabledSettings_returnsFalse() {
        ModuleContext context = mock(ModuleContext.class);
        ServiceContainer container = mock(ServiceContainer.class);
        ActivityScoreService scoreService = mock(ActivityScoreService.class);
        ActivitySettings settings = mock(ActivitySettings.class);

        when(context.getServiceContainer()).thenReturn(container);
        when(container.get(ActivityScoreService.class)).thenReturn(scoreService);
        when(scoreService.getSettings()).thenReturn(settings);
        when(settings.isEnabled()).thenReturn(false);

        assertFalse(CoreActivityBridge.isCoreActivityEnabled(context));
    }

    @Test
    void isCoreActivityEnabled_enabledService_returnsTrue() {
        ModuleContext context = mock(ModuleContext.class);
        ServiceContainer container = mock(ServiceContainer.class);
        ActivityScoreService scoreService = mock(ActivityScoreService.class);
        ActivitySettings settings = mock(ActivitySettings.class);

        when(context.getServiceContainer()).thenReturn(container);
        when(container.get(ActivityScoreService.class)).thenReturn(scoreService);
        when(scoreService.getSettings()).thenReturn(settings);
        when(settings.isEnabled()).thenReturn(true);

        assertTrue(CoreActivityBridge.isCoreActivityEnabled(context));
    }

    @Test
    void isCoreActivityEnabled_containerThrows_returnsFalse() {
        ModuleContext context = mock(ModuleContext.class);
        when(context.getServiceContainer()).thenThrow(new IllegalStateException("boom"));

        assertFalse(CoreActivityBridge.isCoreActivityEnabled(context));
    }
}
