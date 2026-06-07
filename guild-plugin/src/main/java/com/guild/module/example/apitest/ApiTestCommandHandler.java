package com.guild.module.example.apitest;

import com.guild.sdk.command.ModuleCommandHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * /guild apitest command handler.
 */
public class ApiTestCommandHandler implements ModuleCommandHandler {

    private final ApiTestModule module;

    ApiTestCommandHandler(ApiTestModule module) { this.module = module; }

    @Override
    public void handle(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "status":
                module.log("module active, " + module.getTestLog().size() + " log entries");
                sender.sendMessage("§a[ApiTest] Module running, event log: " + module.getTestLog().size() + " entries");
                break;
            case "log":
                module.getTestLog().forEach(sender::sendMessage);
                break;
            case "clear":
                module.clearLog();
                sender.sendMessage("§a[ApiTest] Log cleared");
                break;
            case "gui":
                if (sender instanceof Player p) module.openTestGUI(p);
                break;
            default:
                if (sender instanceof Player p) {
                    TestRunner runner = new TestRunner(module, p);
                    Map<String, Runnable> tests = Map.of(
                        "query", runner::testQuery,
                        "member", runner::testMember,
                        "economy", runner::testEconomy,
                        "http", runner::testHttp,
                        "placeholder", runner::testPlaceholder,
                        "time", runner::testTimeAndConsole
                    );
                    Runnable test = tests.get(sub);
                    if (test != null) test.run();
                    else sender.sendMessage("§cUnknown command: /guild apitest " + sub);
                }
        }
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§d===== ApiTest Commands =====");
        s.sendMessage("§e/guild apitest query §7- Data query test");
        s.sendMessage("§e/guild apitest member §7- Member management test");
        s.sendMessage("§e/guild apitest economy §7- Economy test");
        s.sendMessage("§e/guild apitest http §7- HTTP test");
        s.sendMessage("§e/guild apitest placeholder §7- Placeholder test");
        s.sendMessage("§e/guild apitest time §7- Server time and console output test");
        s.sendMessage("§e/guild apitest status §7- Show running status");
        s.sendMessage("§e/guild apitest log §7- Show event log");
        s.sendMessage("§e/guild apitest clear §7- Clear log");
        s.sendMessage("§e/guild apitest gui §7- Open test panel");
    }
}
