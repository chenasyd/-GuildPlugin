package com.guild.module.example.apitest;

import com.guild.sdk.command.ModuleCommandHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * /guild apitest 命令处理。
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
                sender.sendMessage("§a[ApiTest] 模块运行中，事件日志 " + module.getTestLog().size() + " 条");
                break;
            case "log":
                module.getTestLog().forEach(sender::sendMessage);
                break;
            case "clear":
                module.clearLog();
                sender.sendMessage("§a[ApiTest] 日志已清空");
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
                        "placeholder", runner::testPlaceholder
                    );
                    Runnable test = tests.get(sub);
                    if (test != null) test.run();
                    else sender.sendMessage("§c未知命令: /guild apitest " + sub);
                }
        }
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§d===== ApiTest 命令 =====");
        s.sendMessage("§e/guild apitest query §7— 数据查询测试");
        s.sendMessage("§e/guild apitest member §7— 成员管理测试");
        s.sendMessage("§e/guild apitest economy §7— 经济/货币测试");
        s.sendMessage("§e/guild apitest http §7— HTTP 测试");
        s.sendMessage("§e/guild apitest placeholder §7— 占位符测试");
        s.sendMessage("§e/guild apitest status §7— 显示运行状态");
        s.sendMessage("§e/guild apitest log §7— 显示事件日志");
        s.sendMessage("§e/guild apitest clear §7— 清空日志");
        s.sendMessage("§e/guild apitest gui §7— 打开测试面板");
    }
}
