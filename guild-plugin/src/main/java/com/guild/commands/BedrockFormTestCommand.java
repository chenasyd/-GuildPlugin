package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.core.geyser.PlayerConnectionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 基岩版表单测试指令 — 用于预览 Cumulus 表单在基岩客户端上的渲染效果。
 *
 * <p>通过反射调用 {@code Geyser.api().sendForm(uuid, form)}，
 * 无需 Floodgate，仅依赖 Geyser-Spigot 运行时。
 *
 * <p>用法：
 * <ul>
 *   <li>{@code /bformtest} — 打开主菜单（SimpleForm）</li>
 *   <li>{@code /bformtest simple} — 直接打开 SimpleForm 示例</li>
 *   <li>{@code /bformtest custom} — 直接打开 CustomForm 示例</li>
 *   <li>{@code /bformtest modal} — 直接打开 ModalForm 示例</li>
 * </ul>
 */
public class BedrockFormTestCommand implements CommandExecutor, TabCompleter {

    private final GuildPlugin plugin;

    // 反射句柄：Geyser.api() → GeyserApiBase.sendForm(UUID, Form)
    private static Object geyserApiInstance;
    private static Method sendFormMethod;
    private static boolean reflectionReady = false;
    private static boolean reflectionFailed = false;

    public BedrockFormTestCommand(GuildPlugin plugin) {
        this.plugin = plugin;
        initReflection();
    }

    /**
     * 初始化反射句柄：Geyser.api() 和 GeyserApiBase.sendForm(UUID, Form)
     */
    private static void initReflection() {
        if (reflectionReady || reflectionFailed) return;
        try {
            // org.geysermc.api.Geyser.api() → GeyserApiBase
            Class<?> geyserClass = Class.forName("org.geysermc.api.Geyser");
            Method apiMethod = geyserClass.getMethod("api");
            geyserApiInstance = apiMethod.invoke(null);

            // GeyserApiBase.sendForm(UUID, Form)
            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.Form");
            sendFormMethod = geyserApiInstance.getClass().getMethod("sendForm", UUID.class, formClass);

            reflectionReady = true;
        } catch (Exception e) {
            reflectionFailed = true;
        }
    }

    /**
     * 通过反射向基岩玩家发送表单。
     */
    private boolean sendForm(UUID uuid, Form form) {
        if (!reflectionReady) return false;
        try {
            sendFormMethod.invoke(geyserApiInstance, uuid, form);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("[BFormTest] sendForm failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此指令仅限玩家使用。");
            return true;
        }

        if (!PlayerConnectionService.isBedrockPlayer(player)) {
            player.sendMessage("§c此指令仅限基岩版玩家使用。你是 Java 版玩家。");
            return true;
        }

        if (!reflectionReady) {
            player.sendMessage("§cGeyser API 不可用，无法发送表单。");
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase() : "";

        switch (sub) {
            case "simple" -> sendSimpleForm(player);
            case "custom" -> sendCustomForm(player);
            case "modal" -> sendModalForm(player);
            default -> sendMainMenu(player);
        }

        return true;
    }

    // ── 主菜单（SimpleForm 导航）──────────────────────────────────

    private void sendMainMenu(Player player) {
        SimpleForm form = SimpleForm.builder()
                .title("§6工会插件 §r- §e基岩版表单测试")
                .content("§7选择一个表单类型进行预览：")
                .button("§aSimpleForm §7- 按钮菜单")
                .button("§bCustomForm §7- 输入/设置表单")
                .button("§cModalForm §7- 确认对话框")
                .validResultHandler(response -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> sendSimpleForm(player);
                        case 1 -> sendCustomForm(player);
                        case 2 -> sendModalForm(player);
                    }
                })
                .build();
        sendForm(player.getUniqueId(), form);
    }

    // ── SimpleForm 示例（模拟工会主菜单）──────────────────────────

    private void sendSimpleForm(Player player) {
        SimpleForm form = SimpleForm.builder()
                .title("§6工会主菜单 §r- §7SimpleForm 示例")
                .content("§7欢迎, §e" + player.getName() + "§7!\n§7这是 SimpleForm 按钮菜单示例。")
                .button("§a工会信息 §7- 查看工会详情")
                .button("§b成员管理 §7- 管理工会成员")
                .button("§e工会设置 §7- 修改工会配置")
                .button("§6工会列表 §7- 浏览所有工会")
                .button("§c退出工会 §7- 离开当前工会")
                .validResultHandler(response -> {
                    String[] names = {"工会信息", "成员管理", "工会设置", "工会列表", "退出工会"};
                    int id = response.clickedButtonId();
                    player.sendMessage("§a[SimpleForm] §7你点击了: §f" + names[id]
                            + " §7(buttonId=" + id + ")");
                })
                .closedOrInvalidResultHandler(() ->
                        player.sendMessage("§7[SimpleForm] 表单已关闭"))
                .build();
        sendForm(player.getUniqueId(), form);
    }

    // ── CustomForm 示例（模拟工会设置）────────────────────────────

    private void sendCustomForm(Player player) {
        CustomForm form = CustomForm.builder()
                .title("§6工会设置 §r- §7CustomForm 示例")
                .label("§7── 基本设置 ──")
                .input("§f工会名称", "输入新名称...", "我的工会")
                .input("§f工会描述", "输入描述...", "一个友好的工会")
                .label("§7── 权限开关 ──")
                .toggle("§f允许自由加入", true)
                .toggle("§f允许成员邀请", true)
                .toggle("§f允许成员踢人", false)
                .label("§7── 其他 ──")
                .dropdown("§f工会语言", Arrays.asList("简体中文", "English", "日本語"), 0)
                .slider("§f最大成员数", 10, 100, 5, 50)
                .validResultHandler(response -> {
                    String name = response.asInput(1);
                    String desc = response.asInput(2);
                    boolean freeJoin = response.asToggle(4);
                    boolean allowInvite = response.asToggle(5);
                    boolean allowKick = response.asToggle(6);
                    int langIdx = response.asDropdown(8);
                    int maxMembers = (int) response.asSlider(9);

                    player.sendMessage("§a[CustomForm] §7提交成功:");
                    player.sendMessage("§7  名称: §f" + name);
                    player.sendMessage("§7  描述: §f" + desc);
                    player.sendMessage("§7  自由加入: §f" + freeJoin);
                    player.sendMessage("§7  允许邀请: §f" + allowInvite);
                    player.sendMessage("§7  允许踢人: §f" + allowKick);
                    player.sendMessage("§7  语言: §f" + Arrays.asList("简体中文", "English", "日本語").get(langIdx));
                    player.sendMessage("§7  最大成员: §f" + maxMembers);
                })
                .closedOrInvalidResultHandler(() ->
                        player.sendMessage("§7[CustomForm] 表单已关闭，未保存"))
                .build();
        sendForm(player.getUniqueId(), form);
    }

    // ── ModalForm 示例（模拟确认对话框）───────────────────────────

    private void sendModalForm(Player player) {
        ModalForm form = ModalForm.builder()
                .title("§c确认操作 §r- §7ModalForm 示例")
                .content("§7你确定要退出工会 §e测试工会 §7吗？\n\n"
                        + "§c此操作不可撤销！\n"
                        + "§7退出后需要重新申请加入。")
                .button1("§a确认退出")
                .button2("§c取消")
                .validResultHandler(response -> {
                    if (response.clickedButtonId() == 0) {
                        player.sendMessage("§a[ModalForm] §7你选择了: §c确认退出");
                    } else {
                        player.sendMessage("§a[ModalForm] §7你选择了: §a取消");
                    }
                })
                .closedOrInvalidResultHandler(() ->
                        player.sendMessage("§7[ModalForm] 对话框已关闭"))
                .build();
        sendForm(player.getUniqueId(), form);
    }

    // ── Tab 补全 ─────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("simple", "custom", "modal");
        }
        return Collections.emptyList();
    }
}
