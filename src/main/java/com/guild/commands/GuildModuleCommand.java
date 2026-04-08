package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleManager;
import com.guild.core.module.ModuleRegistry;
import com.guild.core.module.ModuleState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模块管理命令 - 提供运行时热加载/卸载/重载/列表查看功能
 */
public class GuildModuleCommand implements CommandExecutor, TabCompleter {

    private final GuildPlugin plugin;
    private static final String PERMISSION = "guild.admin.module";

    public GuildModuleCommand(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(plugin.getLanguageManager()
                    .getMessage("command.no-permission", ""));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(sender);
            case "load" -> handleLoad(sender, args);
            case "unload" -> handleUnload(sender, args);
            case "reload" -> handleReload(sender, args);
            case "info" -> handleInfo(sender, args);
            default -> sendUsage(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            return Arrays.asList("list", "load", "unload", "reload", "info")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
            if ("unload".equals(sub) || "reload".equals(sub) || "info".equals(sub)) {
                return mm.getRegistry().getModuleIds().stream()
                        .filter(id -> id.startsWith(args[1]))
                        .collect(Collectors.toList());
            }
            if ("load".equals(sub)) {
                File dir = mm.getModulesDirectory();
                if (dir.exists()) {
                    File[] jars = dir.listFiles((d, name) ->
                            name.endsWith(".jar") && name.startsWith(args[1]));
                    if (jars != null) {
                        return Arrays.stream(jars).map(File::getName)
                                .collect(Collectors.toList());
                    }
                }
            }
        }
        return List.of();
    }

    // ==================== 子命令处理 ====================

    private void handleList(CommandSender sender) {
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        ModuleRegistry registry = mm.getRegistry();
        var lang = plugin.getLanguageManager();

        sender.sendMessage("\n&e&l=== " +
                lang.getMessage("module.command.list.title", "") + " ===");

        if (registry.size() == 0) {
            sender.sendMessage(lang.getMessage("module.command.list.empty", ""));
            return;
        }

        for (String moduleId : registry.getModuleIds()) {
            ModuleState state = registry.getState(moduleId);
            ModuleDescriptor desc = registry.getModule(moduleId).getDescriptor();

            String statusColor = switch (state) {
                case ACTIVE -> "&a";
                case LOADING, DISABLING -> "&e";
                case ERROR -> "&c";
                default -> "&7";
            };

            sender.sendMessage(
                    lang.getMessage("module.command.list.entry", "",
                            statusColor, moduleId, desc.getName(),
                            desc.getVersion(), state.name())
            );
        }

        sender.sendMessage(
                lang.getMessage("module.command.list.footer", "",
                        String.valueOf(registry.size()))
        );
    }

    private void handleLoad(CommandSender sender, String[] args) {
        var lang = plugin.getLanguageManager();

        if (args.length < 2) {
            sender.sendMessage(lang.getMessage("module.command.load.usage", ""));
            return;
        }

        String fileName = args[1];
        if (!fileName.endsWith(".jar")) {
            fileName += ".jar";
        }

        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        File moduleFile = new File(mm.getModulesDirectory(), fileName);

        if (!moduleFile.exists()) {
            sender.sendMessage(lang.getMessage("module.error.file-not-found", "", fileName));
            return;
        }

        try {
            mm.loadModule(moduleFile);
            sender.sendMessage(lang.getMessage("module.command.load.success", "", fileName));
        } catch (Exception e) {
            sender.sendMessage(lang.getMessage("module.command.load.failed",
                    "", fileName, e.getMessage()));
        }
    }

    private void handleUnload(CommandSender sender, String[] args) {
        var lang = plugin.getLanguageManager();

        if (args.length < 2) {
            sender.sendMessage(lang.getMessage("module.command.unload.usage", ""));
            return;
        }

        String moduleId = args[1];
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);

        if (!mm.getRegistry().isLoaded(moduleId)) {
            sender.sendMessage(lang.getMessage("module.error.not-loaded", "", moduleId));
            return;
        }

        boolean success = mm.unloadModule(moduleId);
        if (success) {
            sender.sendMessage(lang.getMessage("module.command.unload.success", "", moduleId));
        } else {
            sender.sendMessage(lang.getMessage("module.command.unload.failed", "", moduleId));
        }
    }

    private void handleReload(CommandSender sender, String[] args) {
        var lang = plugin.getLanguageManager();

        if (args.length < 2) {
            sender.sendMessage(lang.getMessage("module.command.reload.usage", ""));
            return;
        }

        String moduleId = args[1];
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);

        if (!mm.getRegistry().isLoaded(moduleId)) {
            sender.sendMessage(lang.getMessage("module.error.not-loaded", "", moduleId));
            return;
        }

        boolean success = mm.reloadModule(moduleId);
        if (success) {
            sender.sendMessage(lang.getMessage("module.command.reload.success", "", moduleId));
        } else {
            sender.sendMessage(lang.getMessage("module.command.reload.failed", "", moduleId));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        var lang = plugin.getLanguageManager();

        if (args.length < 2) {
            sender.sendMessage(lang.getMessage("module.command.info.usage", ""));
            return;
        }

        String moduleId = args[1];
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);

        GuildModule module = mm.getRegistry().getModule(moduleId);
        if (module == null) {
            sender.sendMessage(lang.getMessage("module.error.not-loaded", "", moduleId));
            return;
        }

        ModuleDescriptor desc = module.getDescriptor();
        ModuleState state = mm.getRegistry().getState(moduleId);

        String h = lang.getMessage("module.command.info.header", "");
        sender.sendMessage("&e&l=== " + h + " ===");
        sender.sendMessage(lang.getMessage("module.command.info.id", "", desc.getId()));
        sender.sendMessage(lang.getMessage("module.command.info.name", "", desc.getName()));
        sender.sendMessage(lang.getMessage("module.command.info.version", "", desc.getVersion()));
        sender.sendMessage(lang.getMessage("module.command.info.author", "", desc.getAuthor()));
        sender.sendMessage(lang.getMessage("module.command.info.type", "", desc.getType()));
        sender.sendMessage(lang.getMessage("module.command.info.state", "", state.name()));
        sender.sendMessage(lang.getMessage("module.command.info.description", "", desc.getDescription()));

        if (!desc.getDepends().isEmpty()) {
            sender.sendMessage(lang.getMessage("module.command.info.depends",
                    "", String.join(", ", desc.getDepends())));
        }
        if (!desc.getSoftDepends().isEmpty()) {
            sender.sendMessage(lang.getMessage("module.command.info.soft-depends",
                    "", String.join(", ", desc.getSoftDepends())));
        }
        sender.sendMessage("&e&l==========================");
    }

    private void sendUsage(CommandSender sender) {
        var lang = plugin.getLanguageManager();
        sender.sendMessage(lang.getMessage("module.command.usage-header", ""));
        sender.sendMessage(lang.getMessage("module.command.usage-list", ""));
        sender.sendMessage(lang.getMessage("module.command.usage-load", ""));
        sender.sendMessage(lang.getMessage("module.command.usage-unload", ""));
        sender.sendMessage(lang.getMessage("module.command.usage-reload", ""));
        sender.sendMessage(lang.getMessage("module.command.usage-info", ""));
    }
}
