package com.guild.module.example.apitest;

import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;
import com.guild.sdk.event.*;
import com.guild.sdk.placeholder.PlaceholderProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * API 测试模块 —— 验证所有 SDK 公开 API 的可用性。
 * <p>
 * 通过 /guild apitest 子命令调用各项测试。
 */
public class ApiTestModule implements GuildModule {

    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;
    private final List<String> testLog = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;
        GuildPluginAPI api = context.getApi();

        // 1. 注册测试命令
        api.registerSubCommand("guild", "apitest",
                new ApiTestCommandHandler(this), "guild.apitest.use");

        // 2. 注册 GUI 按钮（在 GuildSettingsGUI 自动找一个槽位）
        ItemStack btn = new ItemStack(Material.ENCHANTED_BOOK);
        org.bukkit.inventory.meta.ItemMeta meta = btn.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§lAPI 测试面板");
            meta.setLore(List.of("§7点击打开 SDK 测试界面"));
            btn.setItemMeta(meta);
        }
        api.registerGUIButton("GuildSettingsGUI", GUIExtensionHook.AUTO_SLOT,
                btn, "api-test", (player, ctx) -> openTestGUI(player));

        // 3. 注册事件监听器
        registerAllEventHandlers(api);

        // 4. 注册占位符提供者
        api.registerPlaceholderProvider(new RegionCountProvider());

        context.getLogger().info("§d[ApiTest] 模块已启用 — 使用 /guild apitest help 查看测试命令");
    }

    @Override
    public void onDisable() {
        this.state = ModuleState.UNLOADED;
    }

    @Override
    public ModuleDescriptor getDescriptor() { return descriptor; }
    @Override
    public void setDescriptor(ModuleDescriptor descriptor) { this.descriptor = descriptor; }
    @Override
    public ModuleState getState() { return state; }

    ModuleContext getContext() { return context; }

    void log(String msg) {
        String entry = "[ApiTest] " + msg;
        testLog.add(entry);
        context.getLogger().info(entry);
    }

    List<String> getTestLog() { return new ArrayList<>(testLog); }
    void clearLog() { testLog.clear(); }

    // ==================== 事件处理器注册 ====================

    private void registerAllEventHandlers(GuildPluginAPI api) {

        api.onGuildCreate(new GuildEventHandler() {
            @Override public void onEvent(GuildEventData d) { log("公会创建: " + d.getGuildName() + " (ID=" + d.getGuildId() + ")"); }
            @Override public Object getModuleInstance() { return ApiTestModule.this; }
        });

        api.onGuildDelete(new GuildEventHandler() {
            @Override public void onEvent(GuildEventData d) { log("公会删除: " + d.getGuildName()); }
            @Override public Object getModuleInstance() { return ApiTestModule.this; }
        });

        api.onMemberJoin(new MemberEventHandler() {
            @Override public void onEvent(MemberEventData d) { log("成员加入: " + d.getPlayerName() + " → " + d.getGuildName()); }
            @Override public Object getModuleInstance() { return ApiTestModule.this; }
        });

        api.onMemberLeave(new MemberEventHandler() {
            @Override public void onEvent(MemberEventData d) { log("成员离开: " + d.getPlayerName() + " (" + d.getEventType() + ")"); }
            @Override public Object getModuleInstance() { return ApiTestModule.this; }
        });

        api.onEconomyDeposit(new EconomyEventHandler() {
            @Override public void onEvent(EconomyEventData d) { log(d.getPlayerName() + " 存款 " + d.getAmount() + " → " + d.getGuildName()); }
            @Override public Object getModuleInstance() { return ApiTestModule.this; }
        });

        api.onEconomyWithdraw(new EconomyEventHandler() {
            @Override public void onEvent(EconomyEventData d) { log(d.getPlayerName() + " 取款 " + d.getAmount() + " ← " + d.getGuildName()); }
            @Override public Object getModuleInstance() { return ApiTestModule.this; }
        });

        api.onMemberRoleChange(new MemberRoleChangeEventHandler() {
            @Override public void onEvent(MemberRoleChangeEventData d) { log(d.getPlayerName() + " 角色: " + d.getOldRole() + " → " + d.getNewRole()); }
            @Override public Object getModuleInstance() { return ApiTestModule.this; }
        });
    }

    // ==================== GUI ====================

    void openTestGUI(Player player) {
        context.runSync(() -> {
            Inventory inv = Bukkit.createInventory(null, 27, "§dAPI 测试面板");
            inv.setItem(11, makeItem(Material.PAPER, "§e数据查询测试", "§7query all/discover"));
            inv.setItem(12, makeItem(Material.PLAYER_HEAD, "§e成员管理测试", "§7add/remove/role"));
            inv.setItem(13, makeItem(Material.GOLD_INGOT, "§e经济/货币测试", "§7deposit/withdraw/currency"));
            inv.setItem(14, makeItem(Material.REDSTONE, "§eHTTP 测试", "§7http"));
            inv.setItem(15, makeItem(Material.NAME_TAG, "§e占位符测试", "§7placeholder"));
            inv.setItem(22, makeItem(Material.BARRIER, "§c关闭", "§7退出"));
            context.openGUI(player, new com.guild.core.gui.GUI() {
                @Override public String getTitle() { return "§dAPI 测试面板"; }
                @Override public int getSize() { return 27; }
                @Override public void setupInventory(Inventory i) { i.setContents(inv.getContents()); }
                @Override public void onClick(Player p, int slot, ItemStack item, org.bukkit.event.inventory.ClickType clickType) {
                    p.closeInventory();
                    switch (slot) {
                        case 11: runTest(p, "query"); break;
                        case 12: runTest(p, "member"); break;
                        case 13: runTest(p, "economy"); break;
                        case 14: runTest(p, "http"); break;
                        case 15: runTest(p, "placeholder"); break;
                    }
                }
            });
        });
    }

    private void runTest(Player player, String test) {
        TestRunner runner = new TestRunner(this, player);
        Map<String, Runnable> tests = Map.of(
            "query", runner::testQuery,
            "member", runner::testMember,
            "economy", runner::testEconomy,
            "http", runner::testHttp,
            "placeholder", runner::testPlaceholder
        );
        tests.getOrDefault(test, () -> player.sendMessage("§c未知测试: " + test)).run();
    }

    private static ItemStack makeItem(Material m, String name, String lore) {
        ItemStack item = new ItemStack(m);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); meta.setLore(List.of(lore)); item.setItemMeta(meta); }
        return item;
    }

    // ==================== Placeholder 演示: Region 人数统计 ====================

    class RegionCountProvider implements PlaceholderProvider {
        @Override
        public String getIdentifier() { return "apitest"; }

        @Override
        public String onRequest(Player player, String params) {
            if (player == null) return "N/A";
            // 格式: regioncount_{guildName}
            if (params.startsWith("regioncount_")) {
                return "0"; // 需要 WorldGuard 依赖才能实现实际逻辑
            }
            if (params.equals("invested")) {
                try {
                    GuildData g = context.getApi().getPlayerGuild(player.getUniqueId()).get();
                    if (g == null) return "0";
                    // 随机找一个 member 展示 investedBalance
                    List<MemberData> members = context.getApi().getGuildMembers(g.getId()).get();
                    MemberData self = members.stream()
                            .filter(m -> m.getPlayerUuid().equals(player.getUniqueId()))
                            .findFirst().orElse(null);
                    return String.format("%.0f", self != null ? self.getInvestedBalance() : 0.0);
                } catch (Exception ignored) { return "0"; }
            }
            return "§7[ApiTest:" + params + "]";
        }
    }
}
