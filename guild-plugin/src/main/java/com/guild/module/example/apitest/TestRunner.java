package com.guild.module.example.apitest;

import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.data.GuildData;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;

/**
 * Test executor — each test method separated by subsystem.
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

    // ==================== Data Query Test ====================

    void testQuery() {
        module.log("=== Data Query Test ===");
        player.sendMessage("§e--- Data Query Test ---");

        // getAllGuilds
        api.getAllGuilds().thenAccept(guilds -> {
            ok("getAllGuilds: " + guilds.size() + " guild(s)");
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
                            ok("getGuildMembers: " + members.size() + " member(s)");
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
                ok("getPlayerGuild: " + (pg != null ? pg.getName() + " (Lv" + pg.getLevel() + ")" : "No guild"));
            });
        });
    }

    // ==================== Member Management Test ====================

    void testMember() {
        module.log("=== Member Management Test ===");
        player.sendMessage("§e--- Member Management Test ---");

        api.getPlayerGuild(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                fail("Member Management: No guild, test skipped");
                return;
            }

            // Query member list
            api.getGuildMembers(guild.getId()).thenAccept(members -> {
                ok("Current members: " + members.size() + " member(s)");
                members.forEach(m -> player.sendMessage("  §7- " + m.getPlayerName() + " [" + m.getRole() + "] invest=" + m.getInvestedBalance()));

                // Note: addMember/removeMember/setMemberRole involve real player actions, not executed automatically
                player.sendMessage("§7Tip: Use console commands to manually test add/remove/role operations");
            });
        });
    }

    // ==================== Economy Test ====================

    void testEconomy() {
        module.log("=== Economy Test ===");
        player.sendMessage("§e--- Economy Test ---");

        api.getPlayerGuild(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                fail("No guild, test skipped");
                return;
            }
            ok("Guild balance: " + guild.getBalance());
            ok("Guild level: " + guild.getLevel());

            // Currency (A/B/C coins)
            try {
                double a = api.getCurrencyBalance(guild.getId(), player.getUniqueId(), "A_COIN");
                double b = api.getCurrencyBalance(guild.getId(), player.getUniqueId(), "B_COIN");
                double c = api.getCurrencyBalance(guild.getId(), player.getUniqueId(), "C_COIN");
                ok("Currency balance: A=" + String.format("%.0f", a) + " B=" + String.format("%.0f", b) + " C=" + String.format("%.0f", c));
            } catch (Exception e) {
                fail("Currency query error: " + e.getMessage());
            }
        });
    }

    // ==================== HTTP Test ====================

    void testHttp() {
        module.log("=== HTTP Test ===");
        player.sendMessage("§e--- HTTP Test ---");

        api.httpGet("https://httpbin.org/get")
                .thenAccept(resp -> ok("httpGet success: " + (resp != null ? resp.length() + " chars" : "null")))
                .exceptionally(e -> { fail("httpGet failed: " + e.getMessage()); return null; });
    }

    // ==================== Placeholder Test ====================

    void testPlaceholder() {
        module.log("=== Placeholder Test ===");
        player.sendMessage("§e--- Placeholder Test ---");
        ok("guild_module_apitest_invested: " + module.new RegionCountProvider().onRequest(player, "invested"));

        // SDK placeholder registration helpers
        api.registerPlaceholderProvider(new com.guild.sdk.placeholder.PlaceholderProvider() {
            @Override public String getIdentifier() { return "apitest_temp"; }
            @Override public String onRequest(Player player, String params) { return "temp"; }
        });
        ok("registerPlaceholderProvider(temp) called");
        ok("getPlaceholderProviders contains apitest_temp: " + api.getPlaceholderProviders().containsKey("apitest_temp"));
        api.unregisterPlaceholderProvider("apitest_temp");
        ok("unregisterPlaceholderProvider(temp) called");
        ok("getPlaceholderProviders no longer contains apitest_temp: " + !api.getPlaceholderProviders().containsKey("apitest_temp"));

        ok("Basic placeholder registered — tested identifier=apitest");
        player.sendMessage("§7regioncount requires WorldGuard dependency, currently returns 0");
    }

    void testTimeAndConsole() {
        module.log("=== Server Time & Console API Test ===");
        player.sendMessage("§e--- Server Time & Console API Test ---");

        try {
            LocalDateTime now = api.getServerTime();
            ok("getServerTime: " + (now != null ? now.toString() : "null"));
            ok("getServerTimeString: " + api.getServerTimeString());
            ok("getServerDateString: " + api.getServerDateString());
            ok("getServerTimePlusMinutes(5): " + api.getServerTimePlusMinutes(5));
            ok("getServerTimePlusDays(1): " + api.getServerTimePlusDays(1));
            ok("formatServerTime: " + api.formatServerTime(now));
            ok("formatServerDate: " + api.formatServerDate(now));
        } catch (Exception e) {
            fail("Server time API test failed: " + e.getMessage());
        }

        try {
            api.consoleInfo("&a[ApiTest] consoleInfo test");
            api.consoleWarn("&e[ApiTest] consoleWarn test");
            api.consoleSevere("&c[ApiTest] consoleSevere test");
            api.consoleInfo("&a[ApiTest] consoleInfo formatted: {0}", "ok");
            api.consoleWarn("&e[ApiTest] consoleWarn formatted: {0}", "ok");
            api.consoleSevere("&c[ApiTest] consoleSevere formatted: {0}", "ok");
            ok("consoleInfo/consoleWarn/consoleSevere invoked");
        } catch (Exception e) {
            fail("Console output API test failed: " + e.getMessage());
        }
    }
}
