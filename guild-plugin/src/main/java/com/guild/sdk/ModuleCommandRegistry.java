package com.guild.sdk;

import com.guild.sdk.command.ModuleCommandHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块子命令与权限注册表。
 */
public final class ModuleCommandRegistry {

    private final Map<String, Map<String, ModuleCommandHandler>> commandRegistry = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> permissionRegistry = new ConcurrentHashMap<>();
    private final Map<String, String> commandOwners = new ConcurrentHashMap<>();

    public void registerSubCommand(String parentCommand, String name,
                                   ModuleCommandHandler handler,
                                   String permission) {
        if (parentCommand == null || parentCommand.isEmpty() || name == null || name.isEmpty() || handler == null) {
            throw new IllegalArgumentException("parentCommand, name and handler cannot be null or empty");
        }

        commandRegistry.computeIfAbsent(parentCommand.toLowerCase(), k -> new ConcurrentHashMap<>())
                .put(name.toLowerCase(), handler);

        if (permission != null) {
            permissionRegistry.computeIfAbsent(parentCommand.toLowerCase(), k -> new ConcurrentHashMap<>())
                    .put(name.toLowerCase(), permission);
        }
    }

    public void registerSubCommand(String moduleId, String parentCommand, String name,
                                   ModuleCommandHandler handler,
                                   String permission) {
        registerSubCommand(parentCommand, name, handler, permission);
        commandOwners.put(parentCommand.toLowerCase() + "." + name.toLowerCase(), moduleId);
    }

    public boolean hasSubCommand(String parentCommand, String name) {
        Map<String, ModuleCommandHandler> subCommands = commandRegistry.get(parentCommand.toLowerCase());
        return subCommands != null && subCommands.containsKey(name.toLowerCase());
    }

    public ModuleCommandHandler getSubCommandHandler(String parentCommand, String name) {
        Map<String, ModuleCommandHandler> subCommands = commandRegistry.get(parentCommand.toLowerCase());
        return subCommands != null ? subCommands.get(name.toLowerCase()) : null;
    }

    public String getSubCommandPermission(String parentCommand, String name) {
        Map<String, String> permissions = permissionRegistry.get(parentCommand.toLowerCase());
        return permissions != null ? permissions.get(name.toLowerCase()) : null;
    }

    public List<String> getSubCommands(String parentCommand) {
        Map<String, ModuleCommandHandler> subCommands = commandRegistry.get(parentCommand.toLowerCase());
        return subCommands != null ? new ArrayList<>(subCommands.keySet()) : Collections.emptyList();
    }

    public void clearModuleRegistrations(String moduleId) {
        commandOwners.entrySet().removeIf(e -> {
            if (moduleId.equals(e.getValue())) {
                String[] parts = e.getKey().split("\\.", 2);
                if (parts.length == 2) {
                    Map<String, ModuleCommandHandler> subs = commandRegistry.get(parts[0]);
                    if (subs != null) {
                        subs.remove(parts[1]);
                    }
                    Map<String, String> perms = permissionRegistry.get(parts[0]);
                    if (perms != null) {
                        perms.remove(parts[1]);
                    }
                }
                return true;
            }
            return false;
        });
    }

    public void clearAll() {
        commandRegistry.clear();
        permissionRegistry.clear();
        commandOwners.clear();
    }
}
