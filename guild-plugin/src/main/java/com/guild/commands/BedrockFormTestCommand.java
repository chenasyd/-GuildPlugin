package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.core.geyser.PlayerConnectionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 基岩版表单测试指令 — 用于预览 Cumulus 表单在基岩客户端上的渲染效果。
 *
 * <p>用法：
 * <ul>
 *   <li>{@code /bformtest} — 打开主菜单（SimpleForm）</li>
 *   <li>{@code /bformtest simple} — 直接打开 SimpleForm 示例</li>
 *   <li>{@code /bformtest custom} — 直接打开 CustomForm 示例</li>
 *   <li>{@code /bformtest modal} — 直接打开 ModalForm 示例</li>
 * </ul>
 *
 * <p>仅限基岩版玩家使用。Java 版玩家执行时会提示不支持。
 */
public class BedrockFormTestCommand implements CommandExecutor, TabCompleter {

    private final GuildPlugin plugin;

    public BedrockFormTestCommand(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此指令仅限玩家使用。");
            return true;
        }

        // 检查是否为基岩版玩家
        if (!PlayerConnectionService.isBedrockPlayer(player)) {
            player.sendMessage("§c此指令仅限基岩版玩家使用。你是 Java 版玩家。");
            return true;
        }

        // 获取 Floodgate 玩家实例
        FloodgateApi floodgateApi = FloodgateApi.getInstance();
        if (floodgateApi == null) {
            player.sendMessage("§cFloodgate 未安装或未启用。");
            return true;
        }

        FloodgatePlayer fp = floodgateApi.getPlayer(player.getUniqueId());
        if (fp == null) {
            player.sendMessage("§c无法获取 Floodgate 玩家实例。");
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase() : "";

        switch (sub) {
            case "simple" -> sendSimpleForm(player, fp);
            case "custom" -> sendCustomForm(player, fp);
            case "modal" -> sendModalForm(player, fp);
            default -> sendMainMenu(player, fp);
        }

        return true;
    }

    // ── 主菜单（SimpleForm 导航）──────────────────────────────────

    private void sendMainMenu(Player player, FloodgatePlayer fp) {
        SimpleForm form = SimpleForm.builder()
                .title("§6工会插件 §r- §e基岩版表单测试")
                .content("§7选择一个表单类型进行预览：")
                .button("§aSimpleForm\n§7按钮菜单")
                .button("§bCustomForm\n§7输入/设置表单")
                .button("§cModalForm\n§7确认对话框")
                .validResultHandler(response -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> sendSimpleForm(player, fp);
                        case 1 -> sendCustomForm(player, fp);
                        case 2 -> sendModalForm(player, fp);
                    }
                })
                .build();
        fp.sendForm(form);
    }

    // ── SimpleForm 示例（模拟工会主菜单）──────────────────────────

    private void sendSimpleForm(Player player, FloodgatePlayer fp) {
        SimpleForm form = SimpleForm.builder()
                .title("§6工会主菜单 §r- §7SimpleForm 示例")
                .content("§7欢迎, §e" + player.getName() + "§7!\n§7这是 SimpleForm 按钮菜单示例。")
                .button("§a工会信息\n§7查看工会详情")
                .button("§b成员管理\n§7管理工会成员")
                .button("§e工会设置\n§7修改工会配置")
                .button("§6工会列表\n§7浏览所有工会")
                .button("§c退出工会\n§7离开当前工会")
                .validResultHandler(response -> {
                    String[] names = {"工会信息", "成员管理", "工会设置", "工会列表", "退出工会"};
                    int id = response.clickedButtonId();
                    player.sendMessage("§a[SimpleForm] §7你点击了: §f" + names[id]
                            + " §7(buttonId=" + id + ")");
                })
                .closedOrInvalidResultHandler(() ->
                        player.sendMessage("§7[SimpleForm] 表单已关闭"))
                .build();
        fp.sendForm(form);
    }

    // ── CustomForm 示例（模拟工会设置）────────────────────────────

    private void sendCustomForm(Player player, FloodgatePlayer fp) {
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
        fp.sendForm(form);
    }

    // ── ModalForm 示例（模拟确认对话框）───────────────────────────

    private void sendModalForm(Player player, FloodgatePlayer fp) {
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
        fp.sendForm(form);
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
