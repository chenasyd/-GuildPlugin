package com.guild.commands.handler.admin;

import com.guild.core.language.LanguageManager;
import com.guild.core.module.ModuleManager;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.utils.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuildAdminTestLangHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        LanguageManager lm = ctx.languageManager();
        String subAction = args.length >= 3 ? args[2].toLowerCase() : "overview";

        switch (subAction) {
            case "lookup" -> handleLangLookup(sender, args, lm);
            case "files" -> handleLangFiles(ctx, sender, lm);
            case "module-context" -> handleLangModuleContext(sender, lm);
            case "force-load" -> handleLangForceLoad(sender, lm);
            case "dump" -> handleLangDump(sender, args, lm);
            case "button-state" -> handleLangButtonState(ctx, sender, args);
            case "module-reload" -> handleLangModuleReload(ctx, sender, args);
            default -> handleLangOverview(sender, lm);
        }
    }

    /** 全景概览 — 显示语言系统完整状态 */
    private void handleLangOverview(CommandSender sender, LanguageManager lm) {
        sender.sendMessage(ColorUtils.colorize("&6========== 语言系统诊断 =========="));
        sender.sendMessage("");

        // 1. 默认语言
        sender.sendMessage(ColorUtils.colorize("&e[1] 默认语言:"));
        sender.sendMessage(ColorUtils.colorize("&7  插件本体 (config.yml): &f" + lm.getDefaultLanguage()));
        sender.sendMessage(ColorUtils.colorize("&7  模块 (modules.yml):    &f" + lm.getModuleDefaultLanguage()));
        sender.sendMessage("");

        // 2. moduleConfigs 内存状态（最关键）
        int configCount = lm.getModuleConfigCount();
        Set<String> supportedLangs = lm.getModuleSupportedLanguages();
        Set<String> loadedIds = lm.getLoadedModuleIds();

        sender.sendMessage(ColorUtils.colorize("&e[2] moduleConfigs 内存状态:"));
        sender.sendMessage(ColorUtils.colorize(
                String.format("&7  已加载语言配置数: &f%d &7(应为 ≥1, 至少 en 必须存在)", configCount)));
        sender.sendMessage(ColorUtils.colorize(
                String.format("&7  支持的语言: &f%s &7(%s)",
                        supportedLangs.isEmpty() ? "&c空!" : supportedLangs.toString(),
                        supportedLangs.isEmpty() ? "&c✗ 致命问题" : "&a✓")));
        sender.sendMessage(ColorUtils.colorize(
                String.format("&7  已标记加载的模块: &f%s &7(%s)",
                        loadedIds.isEmpty() ? "&c空!" : loadedIds.toString(),
                        loadedIds.isEmpty() ? "&c✗ 致命问题" : "&a✓")));

        if (configCount == 0) {
            sender.sendMessage(ColorUtils.colorize("&c&l  ⚠ moduleConfigs 为空！模块语言文件未被加载。"));
            sender.sendMessage(ColorUtils.colorize("&c  原因: 服务器数据目录可能缺少 lang/modules/*/ 下的 YML 文件。"));
            sender.sendMessage(ColorUtils.colorize("&c  尝试: /guildadmin reload 后重试，或检查服务器日志。"));
        }
        sender.sendMessage("");

        // 3. 模块消息查找测试
        String modDefLang = lm.getModuleDefaultLanguage();
        sender.sendMessage(ColorUtils.colorize("&e[3] 模块键查找 (使用 moduleDefaultLanguage=&f" + modDefLang + "&e):"));

        String[][] testKeys = {
                {"announcement", "module.announcement.button-name"},
                {"quest", "module.quest.button-name"},
                {"stats", "module.stats.button-name"},
                {"member-rank", "module.member-rank.button-name"},
                {"apitest", "module.apitest.button-name"},
        };

        for (String[] test : testKeys) {
            String modResult = lm.getModuleMessage(modDefLang, test[1], "&c[NOT FOUND]");
            String enResult = lm.getModuleMessage("en", test[1], "&c[NOT FOUND]");
            String zhResult = lm.getModuleMessage("zh", test[1], "&c[NOT FOUND]");

            boolean zhOk = !"&c[NOT FOUND]".equals(zhResult);
            boolean enOk = !"&c[NOT FOUND]".equals(enResult);

            sender.sendMessage(ColorUtils.colorize(
                    String.format("  &7[%s] modDef → &f%s", test[0], modResult)));
            sender.sendMessage(ColorUtils.colorize(
                    String.format("  &7       en → &f%s &7(%s)    zh → &f%s &7(%s)",
                            enResult, enOk ? "&a✓" : "&c✗",
                            zhResult, zhOk ? "&a✓" : "&c✗")));
        }
        sender.sendMessage("");

        // 4. 强制 zh 查找
        sender.sendMessage(ColorUtils.colorize("&e[4] 强制 zh 查找所有模块按钮名:"));
        for (String[] test : testKeys) {
            String zhVal = lm.getModuleMessage("zh", test[1], "&c[NOT FOUND]");
            sender.sendMessage(ColorUtils.colorize(
                    String.format("  &7[%s] module.xxx.button-name → &f%s", test[0], zhVal)));
        }
        sender.sendMessage("");

        // 5. 强制 en 查找
        sender.sendMessage(ColorUtils.colorize("&e[5] 强制 en 查找（对比）:"));
        for (String[] test : testKeys) {
            String enVal = lm.getModuleMessage("en", test[1], "&c[NOT FOUND]");
            sender.sendMessage(ColorUtils.colorize(
                    String.format("  &7[%s] module.xxx.button-name → &f%s", test[0], enVal)));
        }
        sender.sendMessage("");

        sender.sendMessage(ColorUtils.colorize("&6======================================"));
        sender.sendMessage(ColorUtils.colorize("&7子命令: &f/guildadmin test lang lookup <模块> <键> <语言>"));
        sender.sendMessage(ColorUtils.colorize("&7        &f/guildadmin test lang files &7— 检查文件存在性"));
        sender.sendMessage(ColorUtils.colorize("&7        &f/guildadmin test lang module-context &7— 模拟 ModuleContext 调用链"));
        sender.sendMessage(ColorUtils.colorize("&7        &f/guildadmin test lang force-load &7— 强制从 JAR 加载模块语言"));
    }

    /** 单个键值精确查找 */
    private void handleLangLookup(CommandSender sender, String[] args, LanguageManager lm) {
        if (args.length < 5) {
            sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin test lang lookup <module> <key> <lang>"));
            sender.sendMessage(ColorUtils.colorize("&7示例: /guildadmin test lang lookup stats module.stats.button-name zh"));
            return;
        }
        String module = args[3].toLowerCase();
        String key = args[4];
        String lang = args.length >= 6 ? args[5].toLowerCase() : lm.getModuleDefaultLanguage();

        sender.sendMessage(ColorUtils.colorize("&6--- 键查找: " + key + " ---"));
        sender.sendMessage(ColorUtils.colorize("&7模块: &f" + module + "  &7语言: &f" + lang));

        String result = lm.getModuleMessage(lang, key, "&c[KEY NOT FOUND]");
        sender.sendMessage(ColorUtils.colorize("&7结果: &f" + result));

        // 也显示其他语言的结果
        for (String altLang : new String[]{"en", "zh", "pl", "br"}) {
            if (altLang.equals(lang)) {
                continue;
            }
            String altResult = lm.getModuleMessage(altLang, key, "&c[N/A]");
            sender.sendMessage(ColorUtils.colorize(String.format("  &7%s → &f%s", altLang, altResult)));
        }
    }

    /** 检查所有模块语言文件是否存在 */
    private void handleLangFiles(GuildAdminCommandContext ctx, CommandSender sender, LanguageManager lm) {
        sender.sendMessage(ColorUtils.colorize("&6--- 模块语言文件存在性检查 ---"));

        String[] dirs = {"announcement", "quest", "stats", "member-rank", "apitest"};
        String[] langs = {"en", "zh", "pl", "br"};

        File dataFolder = ctx.plugin().getDataFolder();

        for (String dir : dirs) {
            sender.sendMessage(ColorUtils.colorize("&e[" + dir + "]:"));
            for (String lang : langs) {
                File f = new File(dataFolder, "lang/modules/" + dir + "/" + lang + ".yml");
                boolean exists = f.exists();
                sender.sendMessage(ColorUtils.colorize(
                        String.format("  &7%s.yml → %s", lang, exists ? "&aEXISTS" : "&cMISSING")));
            }
        }
    }

    /** 模拟 ModuleContext.getMessage() 的完整调用链 */
    private void handleLangModuleContext(CommandSender sender, LanguageManager lm) {
        sender.sendMessage(ColorUtils.colorize("&6--- 模拟 ModuleContext.getMessage() 调用链 ---"));
        String modDefault = lm.getModuleDefaultLanguage();
        sender.sendMessage(ColorUtils.colorize("&7moduleDefaultLanguage: &f" + modDefault));
        sender.sendMessage("");

        // 模拟 formatMessage 调用 getModuleIndexedMessage(无Player)
        String[] testKeys = {
                "module.announcement.button-name",
                "module.quest.button-name",
                "module.stats.button-name",
                "module.member-rank.button-name",
                "module.apitest.button-name",
                "module.quest.daily_hunter.name",
        };
        String[] fallbacks = {
                "公告牌", "公会任务", "数据统计", "贡献排名", "API测试面板", "每日狩猎"
        };

        for (int i = 0; i < testKeys.length; i++) {
            // 这就是 context.getMessage(key, fallback) 实际执行的逻辑
            String result = lm.getModuleIndexedMessage(testKeys[i], fallbacks[i]);
            sender.sendMessage(ColorUtils.colorize(
                    String.format("  &7getModuleIndexedMessage(\"%s\", \"%s\")", testKeys[i], fallbacks[i])));
            sender.sendMessage(ColorUtils.colorize(
                    String.format("  &7  → moduleDefaultLanguage(%s) 查找 → &f%s", modDefault, result)));
        }
        sender.sendMessage("");
        sender.sendMessage(ColorUtils.colorize("&e关键结论: 无论玩家怎么设置语言,"));
        sender.sendMessage(ColorUtils.colorize("&econtext.getMessage(key, default) 永远用 moduleDefaultLanguage."));
        sender.sendMessage(ColorUtils.colorize("&e要支持玩家语言,必须用 context.getMessage(player, key, default)."));
    }

    /** 强制从 JAR 内置资源加载模块语言（不依赖磁盘文件） */
    private void handleLangForceLoad(CommandSender sender, LanguageManager lm) {
        String[] dirs = {"announcement", "quest", "stats", "member-rank", "apitest", "testlang"};
        sender.sendMessage(ColorUtils.colorize("&6--- 强制加载内置模块语言 ---"));

        for (String moduleId : dirs) {
            boolean loaded = lm.forceLoadBundledModule(moduleId);
            sender.sendMessage(ColorUtils.colorize(
                    String.format("  &7[%s] %s",
                            moduleId,
                            loaded ? "&a✓ 已加载" : "&c✗ 加载失败 (JAR 中无此模块语言文件)")));
        }

        sender.sendMessage("");
        sender.sendMessage(ColorUtils.colorize("&e模块语言配置数: &f" + lm.getModuleConfigCount()));
        sender.sendMessage(ColorUtils.colorize("&e支持的语言: &f" + lm.getModuleSupportedLanguages()));
        sender.sendMessage(ColorUtils.colorize("&7重新运行 /guildadmin test lang 查看结果。"));
    }

    /** dump 指定语言的 moduleConfig 顶层键和路径逐层探测 */
    private void handleLangDump(CommandSender sender, String[] args, LanguageManager lm) {
        String lang = args.length >= 4 ? args[3].toLowerCase() : "en";
        sender.sendMessage(ColorUtils.colorize("&6--- Raw Dump: moduleConfigs[\"" + lang + "\"] ---"));
        String dump = lm.dumpModuleConfig(lang);
        for (String line : dump.split("\n")) {
            sender.sendMessage(ColorUtils.colorize(line));
        }
        sender.sendMessage(ColorUtils.colorize("&7用法: /guildadmin test lang dump [en|zh|pl|br]"));
    }

    /**
     * 检查指定 GUI 上所有已注册的模块按钮 ItemStack 状态。
     * <p>
     * 用法: /guildadmin test lang button-state <guiType>
     * guiType 可选: GuildSettingsGUI, GuildInfoGUI, MainGuildGUI
     * <p>
     * 用于验证:
     * <ol>
     *   <li>mergeModuleConfig 修复后按钮是否使用了正确的翻译文本</li>
     *   <li>reloadModule() 后按钮 ItemStack 是否已刷新 (对比实例时间戳)</li>
     * </ol>
     */
    private void handleLangButtonState(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin test lang button-state <guiType>"));
            sender.sendMessage(ColorUtils.colorize("&7guiType: GuildSettingsGUI, GuildInfoGUI, MainGuildGUI"));
            return;
        }

        String guiType = args[3];
        ModuleManager mm = ctx.plugin().getModuleManager();
        if (mm == null) {
            sender.sendMessage(ColorUtils.colorize("&cModuleManager 未初始化"));
            return;
        }
        GUIExtensionHook guiHook = mm.getRegistry().getGuiExtensionHook();
        List<GUIExtensionHook.GUIInjectionSlot> injections = guiHook.getInjections(guiType);

        sender.sendMessage(ColorUtils.colorize("&6========== GUI 按钮状态: " + guiType + " =========="));
        sender.sendMessage(ColorUtils.colorize("&7总注册数: &f" + injections.size()));
        sender.sendMessage("");

        if (injections.isEmpty()) {
            sender.sendMessage(ColorUtils.colorize("&c该 GUI 上没有任何模块按钮注册。"));
            sender.sendMessage(ColorUtils.colorize("&6============================================="));
            return;
        }

        for (GUIExtensionHook.GUIInjectionSlot inj : injections) {
            ItemStack item = inj.getItem();
            ItemMeta meta = item != null ? item.getItemMeta() : null;
            String displayName = meta != null && meta.hasDisplayName()
                    ? meta.getDisplayName() : "&8<N/A>";
            List<String> lore = meta != null && meta.hasLore()
                    ? meta.getLore() : Collections.emptyList();

            // 判断是否为英文回退 (displayName 里不含中文且在常见模块键范围内)
            String rawName = ChatColor.stripColor(displayName);
            boolean looksLikeEnglishFallback = displayName.contains("Language Test")
                    || displayName.contains("LangTest")
                    || displayName.contains("Guild Quests")
                    || displayName.contains("Guild Stats")
                    || displayName.contains("Announcements")
                    || displayName.contains("A-Coin")
                    || displayName.contains("API Test");
            boolean hasChinese = containsCJK(rawName);

            String statusIcon;
            if (hasChinese) {
                statusIcon = "&a✓ ZH";
            } else if (looksLikeEnglishFallback) {
                statusIcon = "&c⚠ EN 回退"; // 按钮可能被冻结在英文状态
            } else {
                statusIcon = "&7? UNKNOWN";
            }

            sender.sendMessage(ColorUtils.colorize(
                    "&e┌─ " + statusIcon
                            + " &f[" + inj.getModuleId() + "] &8slot=" + inj.getSlot()));
            sender.sendMessage(ColorUtils.colorize(
                    "&e│  DisplayName: " + displayName));
            if (!lore.isEmpty()) {
                for (int i = 0; i < Math.min(lore.size(), 4); i++) {
                    String line = lore.get(i);
                    boolean isInstanceLine = line.contains("Instance:") || line.contains("实例:");
                    String marker = isInstanceLine ? " &a◄" : "";
                    sender.sendMessage(ColorUtils.colorize(
                            "&e│  Lore[" + i + "]: " + line + marker));
                }
            }
            sender.sendMessage(ColorUtils.colorize("&e└──────────────────────"));
        }

        // 摘要
        sender.sendMessage("");
        LanguageManager lm = ctx.languageManager();
        sender.sendMessage(ColorUtils.colorize("&7模块默认语言: &f" + lm.getModuleDefaultLanguage()));
        sender.sendMessage(ColorUtils.colorize("&7提示: 如果按钮标记为 'EN 回退'，"
                + "说明 ItemStack 在 mergeModuleConfig 修复前创建，需要 reloadModule 或重启服务器刷新。 &8[&6●&8=&6当前实例时间戳&8]"));
        sender.sendMessage(ColorUtils.colorize("&6============================================="));
    }

    /** 检测字符串是否包含 CJK 字符 */
    private boolean containsCJK(String s) {
        if (s == null) {
            return false;
        }
        for (char c : s.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }

    /**
     * 测试模块重载是否刷新了 GUI 按钮 ItemStack。
     * <p>
     * 用法: /guildadmin test lang module-reload <moduleId>
     * <p>
     * 流程:
     * <ol>
     *   <li>显示重载前该模块在所有 GUI 上的按钮 ItemStack 状态</li>
     *   <li>调用 ModuleManager.reloadModule(moduleId)</li>
     *   <li>显示重载后按钮 ItemStack 状态</li>
     *   <li>对比 displayName 和 instance 时间戳以确认刷新</li>
     * </ol>
     */
    private void handleLangModuleReload(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&c用法: /guildadmin test lang module-reload <moduleId>"));
            sender.sendMessage(ColorUtils.colorize("&7示例: /guildadmin test lang module-reload lang-test"));
            return;
        }

        String moduleId = args[3];
        ModuleManager mm = ctx.plugin().getModuleManager();
        if (mm == null) {
            sender.sendMessage(ColorUtils.colorize("&cModuleManager 未初始化"));
            return;
        }

        // ===== 重载前快照 =====
        sender.sendMessage(ColorUtils.colorize("&6========== 模块重载测试: " + moduleId + " =========="));
        sender.sendMessage(ColorUtils.colorize("&e>>> 重载前按钮状态 <<<"));

        GUIExtensionHook guiHook = mm.getRegistry().getGuiExtensionHook();
        String[] guiTypes = {"GuildSettingsGUI", "GuildInfoGUI", "MainGuildGUI"};
        Map<String, String> preSnapshots = new LinkedHashMap<>();

        for (String guiType : guiTypes) {
            List<GUIExtensionHook.GUIInjectionSlot> all = guiHook.getInjections(guiType);
            for (GUIExtensionHook.GUIInjectionSlot inj : all) {
                if (!inj.getModuleId().equals(moduleId)) {
                    continue;
                }
                ItemStack item = inj.getItem();
                ItemMeta meta = item != null ? item.getItemMeta() : null;
                String name = meta != null && meta.hasDisplayName()
                        ? ChatColor.stripColor(meta.getDisplayName()) : "N/A";
                List<String> lore = meta != null && meta.hasLore()
                        ? meta.getLore() : Collections.emptyList();
                String instanceLine = "";
                for (String l : lore) {
                    if (l.contains("Instance:") || l.contains("实例:")) {
                        instanceLine = ChatColor.stripColor(l);
                        break;
                    }
                }
                String snapshot = guiType + " | name=\"" + name + "\" | " + instanceLine;
                preSnapshots.put(guiType, snapshot);
                sender.sendMessage(ColorUtils.colorize("&7  " + guiType + ": &f" + name));
                if (!instanceLine.isEmpty()) {
                    sender.sendMessage(ColorUtils.colorize("&7    &8" + instanceLine));
                }
            }
        }

        if (preSnapshots.isEmpty()) {
            sender.sendMessage(ColorUtils.colorize("&c模块 " + moduleId + " 当前未注册任何 GUI 按钮。"));
            sender.sendMessage(ColorUtils.colorize("&6============================================="));
            return;
        }

        // ===== 执行重载 =====
        sender.sendMessage("");
        sender.sendMessage(ColorUtils.colorize("&e>>> 执行 reloadModule(\"" + moduleId + "\") <<<"));

        boolean success = mm.reloadModule(moduleId);
        if (success) {
            sender.sendMessage(ColorUtils.colorize("&a✓ reloadModule 成功"));
        } else {
            sender.sendMessage(ColorUtils.colorize("&c✗ reloadModule 失败"));
            sender.sendMessage(ColorUtils.colorize("&6============================================="));
            return;
        }

        // ===== 重载后对比 =====
        sender.sendMessage("");
        sender.sendMessage(ColorUtils.colorize("&e>>> 重载后按钮状态 <<<"));

        int changed = 0;
        int unchanged = 0;
        int newButtons = 0;

        for (String guiType : guiTypes) {
            String preSnapshot = preSnapshots.get(guiType);
            List<GUIExtensionHook.GUIInjectionSlot> all = guiHook.getInjections(guiType);
            boolean found = false;
            for (GUIExtensionHook.GUIInjectionSlot inj : all) {
                if (!inj.getModuleId().equals(moduleId)) {
                    continue;
                }
                found = true;
                ItemStack item = inj.getItem();
                ItemMeta meta = item != null ? item.getItemMeta() : null;
                String name = meta != null && meta.hasDisplayName()
                        ? ChatColor.stripColor(meta.getDisplayName()) : "N/A";
                List<String> lore = meta != null && meta.hasLore()
                        ? meta.getLore() : Collections.emptyList();
                String instanceLine = "";
                for (String l : lore) {
                    if (l.contains("Instance:") || l.contains("实例:")) {
                        instanceLine = ChatColor.stripColor(l);
                        break;
                    }
                }

                boolean isDifferent = preSnapshot == null || !preSnapshot.contains(instanceLine);
                String icon = isDifferent ? "&a✓ 已刷新" : "&c⚠ 未变化";
                sender.sendMessage(ColorUtils.colorize("  " + icon + " &f" + guiType + ": \"" + name + "\""));
                if (!instanceLine.isEmpty()) {
                    sender.sendMessage(ColorUtils.colorize("    &8" + instanceLine));
                }

                if (preSnapshot == null) {
                    newButtons++;
                } else if (isDifferent) {
                    changed++;
                } else {
                    unchanged++;
                }
            }
            if (preSnapshot != null && !found) {
                sender.sendMessage(ColorUtils.colorize("  &4✗ " + guiType + ": 按钮消失!"));
                unchanged++;
            }
        }

        // ===== 结论 =====
        sender.sendMessage("");
        sender.sendMessage(ColorUtils.colorize("&6--- 结论 ---"));
        sender.sendMessage(ColorUtils.colorize("&a已刷新: &f" + changed + " &7| &c未变化: &f" + unchanged + " &7| &e新增: &f" + newButtons));

        if (changed > 0) {
            sender.sendMessage(ColorUtils.colorize("&a✓ reloadModule() 成功刷新了 " + changed + " 个按钮的 ItemStack。"));
            sender.sendMessage(ColorUtils.colorize("&7  验证了: onDisable → registry.unregister → onEnable → 重建 ItemStack 链路完整。"));
        } else if (unchanged > 0) {
            sender.sendMessage(ColorUtils.colorize("&c⚠ 按钮未刷新 — 可能原因:"));
            sender.sendMessage(ColorUtils.colorize("&7  1) mergeModuleConfig 修复后翻译文本已经是正确的"));
            sender.sendMessage(ColorUtils.colorize("&7  2) 模块 ID 不匹配"));
            sender.sendMessage(ColorUtils.colorize("&7  3) reloadModule 失败（见上方错误信息）"));
        } else {
            sender.sendMessage(ColorUtils.colorize("&e请验证: /guildadmin test lang button-state GuildSettingsGUI"));
        }

        sender.sendMessage(ColorUtils.colorize("&6============================================="));
    }
}
