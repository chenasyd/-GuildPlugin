package com.guild.sdk;

import com.guild.sdk.home.HomeProtectIntegration;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 模块 Home 保护协调（多模块 skip/defer 聚合）。
 */
public final class ModuleHomeProtectCoordinator {

    private static final class RegisteredHomeProtectIntegration {
        final Object owner;
        final HomeProtectIntegration integration;

        RegisteredHomeProtectIntegration(Object owner, HomeProtectIntegration integration) {
            this.owner = owner;
            this.integration = integration;
        }
    }

    private final Logger logger;
    private final List<RegisteredHomeProtectIntegration> integrations = new CopyOnWriteArrayList<>();

    public ModuleHomeProtectCoordinator(Logger logger) {
        this.logger = logger;
    }

    public void register(Object moduleInstance, HomeProtectIntegration integration) {
        if (moduleInstance == null || integration == null) {
            throw new IllegalArgumentException("moduleInstance and integration cannot be null");
        }
        integrations.removeIf(entry -> entry.owner == moduleInstance);
        integrations.add(new RegisteredHomeProtectIntegration(moduleInstance, integration));
    }

    public void unregister(Object moduleInstance) {
        if (moduleInstance == null) {
            return;
        }
        integrations.removeIf(entry -> entry.owner == moduleInstance);
    }

    public void clearModuleHandlers(Object moduleInstance) {
        unregister(moduleInstance);
    }

    public boolean isHomeProtectFullyDeferred() {
        for (RegisteredHomeProtectIntegration entry : integrations) {
            try {
                if (entry.integration.deferAll()) {
                    return true;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "HomeProtectIntegration.deferAll failed", e);
            }
        }
        return false;
    }

    public boolean shouldSkipHomeProtectAt(Player player, Location location) {
        for (RegisteredHomeProtectIntegration entry : integrations) {
            try {
                if (entry.integration.skipAt(player, location)) {
                    return true;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "HomeProtectIntegration.skipAt failed", e);
            }
        }
        return false;
    }

    public boolean shouldSkipHomeProtectForGuildHome(int guildId, String worldName) {
        for (RegisteredHomeProtectIntegration entry : integrations) {
            try {
                if (entry.integration.skipHomeForGuild(guildId, worldName)) {
                    return true;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "HomeProtectIntegration.skipHomeForGuild failed", e);
            }
        }
        return false;
    }
}
