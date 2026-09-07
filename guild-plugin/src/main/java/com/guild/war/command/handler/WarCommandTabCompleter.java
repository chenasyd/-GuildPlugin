package com.guild.war.command.handler;

import com.guild.GuildPlugin;
import com.guild.world.GuildWorldService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class WarCommandTabCompleter implements TabCompleter {

    private final GuildPlugin plugin;

    public WarCommandTabCompleter(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(Arrays.asList("challenge", "accept", "deny", "join", "leave", "ready", "cancel",
                    "status", "report", "season", "help"));
            if (sender.hasPermission(GuildWarCommandContext.PERM_ADMIN)) {
                out.add("admin");
                out.add("export");
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("challenge")) {
            if (args.length == 2) {
                out.addAll(Arrays.asList("--preset", "--mode", "--max", "--score", "--time"));
            } else {
                String prev = args[args.length - 2].toLowerCase(Locale.ROOT);
                switch (prev) {
                    case "--mode" -> out.addAll(Arrays.asList("first", "timed", "survive"));
                    case "--preset" -> {
                        GuildWorldService worldService = plugin.getGuildWorldService();
                        if (worldService != null) {
                            worldService.getPresets().list().forEach(p -> out.add(p.name()));
                        }
                    }
                    default -> {
                        if (!args[args.length - 1].startsWith("--")) {
                            out.addAll(Arrays.asList("--preset", "--mode", "--max", "--score", "--time"));
                        }
                    }
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            out.add("end");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("export")) {
            out.addAll(Arrays.asList("json", "csv"));
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(last)).toList();
    }
}
