package com.guild.sdk;

import com.guild.sdk.placeholder.PlaceholderProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块占位符提供者注册表。
 */
public final class ModulePlaceholderRegistry {

    private final Map<String, PlaceholderProvider> placeholderProviders = new ConcurrentHashMap<>();
    private final Map<String, String> placeholderOwners = new ConcurrentHashMap<>();

    public void registerPlaceholderProvider(PlaceholderProvider provider) {
        if (provider == null || provider.getIdentifier() == null || provider.getIdentifier().trim().isEmpty()) {
            return;
        }
        placeholderProviders.put(provider.getIdentifier().toLowerCase(), provider);
    }

    public void registerPlaceholderProvider(String moduleId, PlaceholderProvider provider) {
        registerPlaceholderProvider(provider);
        if (provider != null && provider.getIdentifier() != null && !provider.getIdentifier().trim().isEmpty()) {
            placeholderOwners.put(provider.getIdentifier().toLowerCase(), moduleId);
        }
    }

    public void unregisterPlaceholderProvider(String identifier) {
        if (identifier == null) {
            return;
        }
        placeholderProviders.remove(identifier.toLowerCase());
    }

    public Map<String, PlaceholderProvider> getPlaceholderProviders() {
        return placeholderProviders;
    }

    public void clearModuleRegistrations(String moduleId) {
        placeholderOwners.entrySet().removeIf(e -> {
            if (moduleId.equals(e.getValue())) {
                placeholderProviders.remove(e.getKey());
                return true;
            }
            return false;
        });
    }

    public void clearAll() {
        placeholderProviders.clear();
        placeholderOwners.clear();
    }
}
