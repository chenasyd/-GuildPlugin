package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.core.geyser.PlayerConnectionService;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
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
    private final LanguageManager languageManager;

    // 反射句柄：Geyser.api() → GeyserApiBase.sendForm(UUID, Form)
    private static Object geyserApiInstance;
    private static Method sendFormMethod;
    private static boolean reflectionReady = false;
    private static boolean reflectionFailed = false;

    public BedrockFormTestCommand(GuildPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
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
            sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage("general.player-only", "&cThis command can only be executed by a player!")));
            return true;
        }

        if (!PlayerConnectionService.isBedrockPlayer(player)) {
            player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.bedrock-only", "&cThis command is only for Bedrock players. You are a Java player.")));
            return true;
        }

        if (!reflectionReady) {
            player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.geyser-unavailable", "&cGeyser API unavailable, cannot send forms.")));
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
                .title(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.main-title", "&6Guild Plugin &r- &eBedrock Form Test")))
                .content(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.main-content", "&7Select a form type to preview:")))
                .button(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.main-btn-simple", "&aSimpleForm &f- Button Menu")))
                .button(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.main-btn-custom", "&bCustomForm &f- Input/Settings Form")))
                .button(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.main-btn-modal", "&cModalForm &f- Confirm Dialog")))
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
                .title(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.simple-title", "&6Guild Main Menu &r- &7SimpleForm Example")))
                .content(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.simple-content", "&7Welcome, &e{player}&7!\n&7This is a SimpleForm button menu example.", "{player}", player.getName())))
                .button(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.simple-btn-info", "&aGuild Info &f- View guild details")))
                .button(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.simple-btn-members", "&bMember Management &f- Manage guild members")))
                .button(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.simple-btn-settings", "&eGuild Settings &f- Modify guild config")))
                .button(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.simple-btn-list", "&6Guild List &f- Browse all guilds")))
                .button(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.simple-btn-quit", "&cQuit Guild &f- Leave current guild")))
                .validResultHandler(response -> {
                    String[] names = {
                            languageManager.getCoreMessage(player, "bedrock-test.simple-name-info", "Guild Info"),
                            languageManager.getCoreMessage(player, "bedrock-test.simple-name-members", "Member Management"),
                            languageManager.getCoreMessage(player, "bedrock-test.simple-name-settings", "Guild Settings"),
                            languageManager.getCoreMessage(player, "bedrock-test.simple-name-list", "Guild List"),
                            languageManager.getCoreMessage(player, "bedrock-test.simple-name-quit", "Quit Guild")
                    };
                    int id = response.clickedButtonId();
                    player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.simple-clicked",
                            "&a[SimpleForm] &7You clicked: &f{name} &7(buttonId={id})",
                            "{name}", names[id], "{id}", String.valueOf(id))));
                })
                .closedOrInvalidResultHandler(() ->
                        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.simple-closed",
                                "&7[SimpleForm] Form closed"))))
                .build();
        sendForm(player.getUniqueId(), form);
    }

    // ── CustomForm 示例（模拟工会设置）────────────────────────────

    private void sendCustomForm(Player player) {
        CustomForm form = CustomForm.builder()
                .title(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-title", "&6Guild Settings &r- &fCustomForm Example")))
                .label(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-label-basic", "&7── Basic Settings ──")))
                .input(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-input-name-label", "&fGuild Name")),
                        languageManager.getCoreMessage(player, "bedrock-test.custom-input-name-placeholder", "Enter new name..."),
                        languageManager.getCoreMessage(player, "bedrock-test.custom-input-name-default", "My Guild"))
                .input(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-input-desc-label", "&fGuild Description")),
                        languageManager.getCoreMessage(player, "bedrock-test.custom-input-desc-placeholder", "Enter description..."),
                        languageManager.getCoreMessage(player, "bedrock-test.custom-input-desc-default", "A friendly guild"))
                .label(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-label-perms", "&7── Permission Toggles ──")))
                .toggle(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-toggle-free-join", "&fAllow Free Join")), true)
                .toggle(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-toggle-allow-invite", "&fAllow Member Invite")), true)
                .toggle(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-toggle-allow-kick", "&fAllow Member Kick")), false)
                .label(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-label-other", "&7── Other ──")))
                .dropdown(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-dropdown-lang", "&fGuild Language")), Arrays.asList("Simplified Chinese", "English", "日本語"), 0)
                .slider(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-slider-max-members", "&fMax Members")), 10, 100, 5, 50)
                .validResultHandler(response -> {
                    String name = response.asInput(1);
                    String desc = response.asInput(2);
                    boolean freeJoin = response.asToggle(4);
                    boolean allowInvite = response.asToggle(5);
                    boolean allowKick = response.asToggle(6);
                    int langIdx = response.asDropdown(8);
                    int maxMembers = (int) response.asSlider(9);

                    player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-submit-success", "&a[CustomForm] &7Submitted successfully:")));
                    player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-name", "&7  Name: &f{value}", "{value}", name)));
                    player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-desc", "&7  Description: &f{value}", "{value}", desc)));
                    player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-free-join", "&7  Free join: &f{value}", "{value}", String.valueOf(freeJoin))));
                    player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-allow-invite", "&7  Allow invite: &f{value}", "{value}", String.valueOf(allowInvite))));
                    player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-allow-kick", "&7  Allow kick: &f{value}", "{value}", String.valueOf(allowKick))));
                    player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-language", "&7  Language: &f{value}", "{value}", Arrays.asList("Simplified Chinese", "English", "日本語").get(langIdx))));
                    player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-max-members", "&7  Max members: &f{value}", "{value}", String.valueOf(maxMembers))));
                })
                .closedOrInvalidResultHandler(() ->
                        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.custom-closed", "&7[CustomForm] Form closed, not saved"))))
                .build();
        sendForm(player.getUniqueId(), form);
    }

    // ── ModalForm 示例（模拟确认对话框）───────────────────────────

    private void sendModalForm(Player player) {
        ModalForm form = ModalForm.builder()
                .title(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.modal-title", "&cConfirm Action &r- &fModalForm Example")))
                .content(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.modal-content",
                        "&7Are you sure you want to quit guild &eTest Guild&7?\n\n&cThis action cannot be undone!\n&7You will need to reapply to join.")))
                .button1(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.modal-btn-confirm", "&aConfirm Quit")))
                .button2(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.modal-btn-cancel", "&cCancel")))
                .validResultHandler(response -> {
                    if (response.clickedButtonId() == 0) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.modal-confirm-quit", "&a[ModalForm] &7You chose: &cConfirm quit")));
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.modal-cancel", "&a[ModalForm] &7You chose: &aCancel")));
                    }
                })
                .closedOrInvalidResultHandler(() ->
                        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "bedrock-test.modal-closed", "&7[ModalForm] Dialog closed"))))
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
