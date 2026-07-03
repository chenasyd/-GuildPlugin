package com.guild.module.example.testlang;

import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.language.LanguageManager;
import com.guild.sdk.GuildPluginAPI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * 语言系统测试模块
 * <p>
 * 用途：验证模块语言加载、moduleDefaultLanguage 切换、
 *       ModuleContext.getMessage() 行为、以及 Player 感知的消息解析。
 * <p>
 * 启动时输出完整诊断日志，并注册一个 GuildSettingsGUI 测试按钮。
 */
public class LanguageTestModule implements GuildModule {

    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;

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
                String.format("[LangTest] getMessage(%s, fallback) → \"%s\"", keys[i], result));
        }

        // 强制测试各语言查找
        context.getLogger().info("[LangTest] --- 强制语言查找 ---");
        for (String lang : new String[]{"en", "zh", "pl", "br"}) {
            String val = lm.getModuleMessage(lang, "module.testlang.name", "NOT_FOUND");
            context.getLogger().info(
                String.format("[LangTest] %s → module.testlang.name = \"%s\"", lang, val));
        }

        context.getLogger().info("==============================================");

        // ========== 注册测试 GUI 按钮 ==========
        registerTestButton();
    }

    private void registerTestButton() {
        GuildPluginAPI api = context.getApi();

        ItemStack button = new ItemStack(Material.BOOK);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(
                context.getMessage("module.testlang.gui.button-name", "&b&lLanguage Test"));
            meta.setLore(Arrays.asList(
                context.getMessage("module.testlang.gui.button-lore-1", "&7Language system diagnostic"),
                context.getMessage("module.testlang.gui.button-lore-2", "&7Click to run tests"),
                "",
                context.getMessage("module.testlang.gui.button-lore-3",
                    "&8Default: " + context.getLanguageManager().getModuleDefaultLanguage())
            ));
            button.setItemMeta(meta);
        }

        api.registerGUIButton("MainGuildGUI", GUIExtensionHook.AUTO_SLOT,
            button, "lang-test",
            (player, ctx) -> {
                player.sendMessage(context.getMessage(player,
                    "module.testlang.gui.click-message", "&a[LangTest] Button clicked!"));
                runDiagnosticReport(player);
            });
    }

    private void runDiagnosticReport(org.bukkit.entity.Player player) {
        LanguageManager lm = context.getLanguageManager();

        player.sendMessage("§6========== [LangTest] Diagnostic Report ==========");
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
            context.getLogger().info("[LangTest] Module disabled.");
        }
    }
}
