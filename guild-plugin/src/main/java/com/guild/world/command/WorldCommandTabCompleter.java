package com.guild.world.command;

import com.guild.world.GuildWorldService;
import com.guild.world.model.GuildWorld;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** /guildworld Tab 补全。 */
public final class WorldCommandTabCompleter implements TabCompleter {

    private final GuildWorldService worldService;

    public WorldCommandTabCompleter(GuildWorldService worldService) {
        this.worldService = worldService;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission(GuildWorldCommand.PERMISSION)) {
            return completions;
        }
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "list", "info", "load", "unload",
                    "delete", "restore", "tp", "edit", "preset", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "info", "load", "unload", "delete", "tp", "goto", "enter" -> addWorldNames(completions);
                case "restore" -> completions.addAll(Arrays.asList("--run", "--list", "--load", "--delete"));
                case "edit" -> completions.addAll(Arrays.asList(
                        "wand", "pos1", "pos2", "tp", "create", "leave", "setspawn", "save", "help"));
                case "preset" -> completions.addAll(Arrays.asList(
                        "list", "info", "delete", "bind", "paste", "help"));
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "create" -> completions.addAll(Arrays.asList("--type", "--preset", "--guild"));
                case "delete" -> completions.add("--force");
                case "restore" -> {
                    if ("--load".equalsIgnoreCase(args[1]) || "--delete".equalsIgnoreCase(args[1])) {
                        addWorldNames(completions);
                    } else {
                        completions.addAll(Arrays.asList("--force", "--load", "--delete"));
                    }
                }
                case "edit" -> {
                    switch (args[1].toLowerCase()) {
                        case "tp", "enter", "goto" -> addWorldNames(completions);
                        case "save" -> addPresetNames(completions);
                        case "setspawn" -> completions.addAll(Arrays.asList("a", "b", "spectator", "main"));
                        case "create" -> { /* free text name */ }
                    }
                }
                case "preset" -> {
                    switch (args[1].toLowerCase()) {
                        case "info", "delete", "paste" -> addPresetNames(completions);
                        case "bind" -> addWorldNames(completions);
                    }
                }
            }
        } else if (args.length == 4) {
            if ("create".equalsIgnoreCase(args[0]) && "--type".equalsIgnoreCase(args[2])) {
                completions.addAll(Arrays.asList("battle", "edit", "template"));
            } else if ("edit".equalsIgnoreCase(args[0]) && "create".equalsIgnoreCase(args[1])) {
                completions.add("--preset");
            } else if ("preset".equalsIgnoreCase(args[0]) && "bind".equalsIgnoreCase(args[1])) {
                addPresetNames(completions);
            } else if ("preset".equalsIgnoreCase(args[0]) && "paste".equalsIgnoreCase(args[1])) {
                addWorldNames(completions);
            }
        }
        String last = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(last));
        return completions;
    }

    private void addWorldNames(List<String> completions) {
        for (GuildWorld gw : worldService.getWorlds()) {
            completions.add(gw.getWorldName());
        }
    }

    private void addPresetNames(List<String> completions) {
        for (var meta : worldService.getPresets().list()) {
            completions.add(meta.name());
        }
    }
}
