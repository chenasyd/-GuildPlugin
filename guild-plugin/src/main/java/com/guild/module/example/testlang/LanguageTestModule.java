package com.guild.module.example.testlang;

import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.language.LanguageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 语言系统测试模块 — 完整版
 * <p>
 * 用于验证以下链路：
 * <ol>
 *   <li>mergeModuleConfig 多模块数据合并正确性 (onEnable 日志)</li>
 *   <li>context.getMessage() 在 moduleDefaultLanguage 下的行为</li>
 *   <li>3 个目标 GUI (MainGuildGUI / GuildSettingsGUI / GuildInfoGUI) 按钮注册</li>
 *   <li>reloadModule() 后按钮 ItemStack 是否重新创建 (通过实例时间戳验证)</li>
 * </ol>
 * <p>
 * 每个按钮的 lore 最后一行为 "&8实例: {timestamp}"，用于在 module-reload
 * 测试中对比前后实例是否不同。
 */
public class LanguageTestModule implements GuildModule {

    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;

    /** 实例标识 — 每次 onEnable() 生成，用于验证 reload 是否重建了对象 */
    private final String instanceId = String.valueOf(System.currentTimeMillis());

    @Override
    public ModuleDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void setDescriptor(ModuleDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public ModuleState getState() {
        return state;
    }

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;

        LanguageManager lm = context.getLanguageManager();

        // ========== 启动诊断日志 ==========
        context.getLogger().info("==============================================");
        context.getLogger().info("[LangTest] >>> onEnable() instance=" + instanceId);
        context.getLogger().info("[LangTest] Module Default Language: " + lm.getModuleDefaultLanguage());
        context.getLogger().info("[LangTest] Plugin Default Language: " + lm.getDefaultLanguage());
        context.getLogger().info("[LangTest] --- 无 Player getMessage 测试 ---");

        String[] keys = {
            "module.testlang.name",
            "module.testlang.description",
            "module.testlang.status-active",
            "module.testlang.status-inactive",
        };
        String[] fallbacks = {
            "LangTest Module", "Test language system", "ACTIVE [fallback]", "INACTIVE [fallback]"
        };

        for (int i = 0; i < keys.length; i++) {
            String result = context.getMessage(keys[i], fallbacks[i]);
            context.getLogger().info(
                String.format("[LangTest] getMessage(%s) → \"%s\"", keys[i], result));
        }

        // 强制测试各语言查找 (同时验证 mergeModuleConfig 修复是否生效)
        context.getLogger().info("[LangTest] --- 跨语言 moduleConfigs 验证 ---");
        for (String lang : new String[]{"en", "zh", "pl", "br"}) {
            String val = lm.getModuleMessage(lang, "module.testlang.name", "NOT_FOUND");
            context.getLogger().info(
                String.format("[LangTest] %s → module.testlang.name = \"%s\"", lang, val));
        }

        context.getLogger().info("==============================================");

        // ========== 注册全部 3 个 GUI 的测试按钮 ==========
        registerAllButtons();
    }

    /**
     * 构建带有实例时间戳的 lore 行
     */
    private List<String> buildLore(String line1, String line2) {
        List<String> lore = new ArrayList<>();
        lore.add(line1);
        lore.add(line2);
        lore.add("");
        lore.add(context.getMessage("module.testlang.gui.instance-id", "&8Instance: {0}")
            .replace("{0}", instanceId));
        return lore;
    }

    private void registerAllButtons() {
        GUIExtensionHook guiHook = context.getPlugin()
            .getModuleManager().getRegistry().getGuiExtensionHook();
        String moduleId = getDescriptor().getId(); // "testlang" — 必须与 module.yml 一致

        // ==== 1) GuildSettingsGUI (AUTO_SLOT — 多语言) ====
        {
            ItemStack btn = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = btn.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("LangTest: Settings");  // 回退文本，实际由 getDisplayItem 按玩家语言解析
                meta.setLore(buildLore(
                    context.getMessage("module.testlang.gui.settings.button-lore-1", "&7Test on GuildSettingsGUI"),
                    context.getMessage("module.testlang.gui.settings.button-lore-2", "&7Verify module localization")));
                btn.setItemMeta(meta);
            }
            guiHook.registerButton("GuildSettingsGUI", GUIExtensionHook.AUTO_SLOT,
                btn, moduleId,
                (player, ctx) -> {
                    player.sendMessage("§d[LangTest] GuildSettingsGUI button clicked! instance=" + instanceId);
                    runDiagnosticReport(player);
                },
                "module.testlang.gui.settings.button-name",
                "module.testlang.gui.settings.button-lore-1",
                "module.testlang.gui.settings.button-lore-2");
            context.getLogger().info("[LangTest] Registered GuildSettingsGUI button (AUTO_SLOT) with lang key + loreKeys");
        }

        // ==== 2) GuildInfoGUI (固定槽位 — 多语言) ====
        {
            ItemStack btn = new ItemStack(Material.KNOWLEDGE_BOOK);
            ItemMeta meta = btn.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("LangTest: Info");  // 回退文本
                meta.setLore(buildLore(
                    context.getMessage("module.testlang.gui.info.button-lore-1", "&7Test on GuildInfoGUI"),
                    context.getMessage("module.testlang.gui.info.button-lore-2", "&7Verify module localization")));
                btn.setItemMeta(meta);
            }
            guiHook.registerButton("GuildInfoGUI", 17,
                btn, moduleId,
                (player, ctx) -> {
                    player.sendMessage("§a[LangTest] GuildInfoGUI button clicked! instance=" + instanceId);
                    runDiagnosticReport(player);
                },
                "module.testlang.gui.info.button-name",
                "module.testlang.gui.info.button-lore-1",
                "module.testlang.gui.info.button-lore-2");
            context.getLogger().info("[LangTest] Registered GuildInfoGUI button (slot=17) with lang key + loreKeys");
        }
    }

    private void runDiagnosticReport(Player player) {
        LanguageManager lm = context.getLanguageManager();

        player.sendMessage("§6========== [LangTest] Diagnostic (instance=" + instanceId + ") ==========");
        player.sendMessage("§7Module Default: §f" + lm.getModuleDefaultLanguage());
        player.sendMessage("§7Plugin Default: §f" + lm.getDefaultLanguage());
        player.sendMessage("");

        // 测试无 Player 的 getMessage
        player.sendMessage("§e--- context.getMessage(key, fallback) ← 无 Player ---");
        String nameNoPlayer = context.getMessage("module.testlang.name", "FALLBACK");
        player.sendMessage("§7  module.testlang.name → §f" + nameNoPlayer);

        // 测试有 Player 的 getMessage
        player.sendMessage("§e--- context.getMessage(player, key, fallback) ← 有 Player ---");
        String nameWithPlayer = context.getMessage(player, "module.testlang.name", "FALLBACK");
        player.sendMessage("§7  module.testlang.name → §f" + nameWithPlayer);
        player.sendMessage("");

        // 强制各语言查找
        player.sendMessage("§e--- 强制语言查找 ---");
        for (String lang : new String[]{"en", "zh", "pl", "br"}) {
            String val = lm.getModuleMessage(lang, "module.testlang.name", "NOT_FOUND");
            player.sendMessage(String.format("§7  [%s] → §f%s", lang, val));
        }

        player.sendMessage("§6================================================");
    }

    @Override
    public void onDisable() {
        this.state = ModuleState.UNLOADED;
        if (context != null) {
            context.getLogger().info("[LangTest] <<< onDisable() instance=" + instanceId
                + " — GUI buttons should be cleaned by registry.unregister()");
        }
    }
}
