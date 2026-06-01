package com.guild.module.example.apitest;

import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.data.GuildData;
import org.bukkit.entity.Player;

/**
 * 测试执行器 —— 各测试方法用独立子系统分隔。
 */
public class TestRunner {

    private final ApiTestModule module;
    private final Player player;
    private final GuildPluginAPI api;

    TestRunner(ApiTestModule module, Player player) {
        this.module = module;
        this.player = player;
        this.api = module.getContext().getApi();
    }

    private void ok(String msg) {
        player.sendMessage("§a✓ " + msg);
        module.log("TEST OK: " + msg);
    }

    private void fail(String msg) {
        player.sendMessage("§c✗ " + msg);
        module.log("TEST FAIL: " + msg);
    }

    // ==================== 数据查询测试 ====================

    void testQuery() {
        module.log("=== 数据查询测试 ===");
        player.sendMessage("§e--- 数据查询测试 ---");

        // getAllGuilds
        api.getAllGuilds().thenAccept(guilds -> {
            ok("getAllGuilds: " + guilds.size() + " 个公会");
            // getGuildById
            if (!guilds.isEmpty()) {
                GuildData g = guilds.get(0);
                api.getGuildById(g.getId()).thenAccept(g2 -> {
                    ok("getGuildById: " + (g2 != null ? g2.getName() : "null"));
                    // getGuildByName
                    api.getGuildByName(g.getName()).thenAccept(g3 -> {
                        ok("getGuildByName: " + (g3 != null ? g3.getName() : "null"));
                        // getGuildMembers
                        api.getGuildMembers(g.getId()).thenAccept(members -> {
                            ok("getGuildMembers: " + members.size() + " 人");
                            // investedBalance
                            members.stream().findFirst().ifPresent(m -> {
                                ok("investedBalance[" + m.getPlayerName() + "]: " + m.getInvestedBalance());
                            });
                        });
                    });
                });
            }
            // getPlayerGuild
            api.getPlayerGuild(player.getUniqueId()).thenAccept(pg -> {
                ok("getPlayerGuild: " + (pg != null ? pg.getName() + " (Lv" + pg.getLevel() + ")" : "无公会"));
            });
        });
    }

    // ==================== 成员管理测试 ====================

    void testMember() {
        module.log("=== 成员管理测试 ===");
        player.sendMessage("§e--- 成员管理测试 ---");

        api.getPlayerGuild(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                fail("成员管理: 无公会，跳过测试");
                return;
            }

            // 查询成员列表
            api.getGuildMembers(guild.getId()).thenAccept(members -> {
                ok("当前成员: " + members.size() + " 人");
                members.forEach(m -> player.sendMessage("  §7- " + m.getPlayerName() + " [" + m.getRole() + "] invest=" + m.getInvestedBalance()));

                // 注意: addMember/removeMember/setMemberRole 涉及真实玩家操作，不自动执行
                player.sendMessage("§7提示: 使用后台指令手动测试 add/remove/role 操作");
            });
        });
    }

    // ==================== 经济/货币测试 ====================

    void testEconomy() {
        module.log("=== 经济/货币测试 ===");
        player.sendMessage("§e--- 经济/货币测试 ---");

        api.getPlayerGuild(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                fail("无公会，跳过测试");
                return;
            }
            ok("公会余额: " + guild.getBalance());
            ok("公会等级: " + guild.getLevel());

            // 货币（A/B/C 币）
            try {
                double a = api.getCurrencyBalance(guild.getId(), player.getUniqueId(), "A_COIN");
                double b = api.getCurrencyBalance(guild.getId(), player.getUniqueId(), "B_COIN");
                double c = api.getCurrencyBalance(guild.getId(), player.getUniqueId(), "C_COIN");
                ok("货币余额: A=" + String.format("%.0f", a) + " B=" + String.format("%.0f", b) + " C=" + String.format("%.0f", c));
            } catch (Exception e) {
                fail("货币查询异常: " + e.getMessage());
            }
        });
    }

    // ==================== HTTP 测试 ====================

    void testHttp() {
        module.log("=== HTTP 测试 ===");
        player.sendMessage("§e--- HTTP 测试 ---");

        api.httpGet("https://httpbin.org/get")
                .thenAccept(resp -> ok("httpGet 成功: " + (resp != null ? resp.length() + " chars" : "null")))
                .exceptionally(e -> { fail("httpGet 失败: " + e.getMessage()); return null; });
    }

    // ==================== 占位符测试 ====================

    void testPlaceholder() {
        module.log("=== 占位符测试 ===");
        player.sendMessage("§e--- 占位符测试 ---");
        ok("guild_module_apitest_invested: " + module.new RegionCountProvider().onRequest(player, "invested"));
        ok("基本占位符注册成功 — 测试 identifier=apitest");
        player.sendMessage("§7regioncount 需 WorldGuard 依赖，当前返回 0");
    }
}
