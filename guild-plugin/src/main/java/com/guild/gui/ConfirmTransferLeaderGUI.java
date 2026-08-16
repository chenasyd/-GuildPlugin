package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.geysermc.cumulus.form.SimpleForm;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 确认转移会长 GUI（含鉴权）
 * <ul>
 *   <li>会长路径：操作者必须是当前公会会长</li>
 *   <li>管理员路径：操作者必须拥有 {@code guild.admin}</li>
 * </ul>
 */
public class ConfirmTransferLeaderGUI implements GUI {

    public static final String FUNC_CONFIRM = "CONFIRM";
    public static final String FUNC_INFO = "INFO";
    public static final String FUNC_CANCEL = "CANCEL";

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final Guild guild;
    private final GuildMember target;
    private final Player player;
    private final String sourceGuiType;
    /** true = 管理员强制转让（GuildDetailGUI 等管理入口） */
    private final boolean adminForce;

    public ConfirmTransferLeaderGUI(GuildPlugin plugin, Guild guild, GuildMember target,
                                    Player player, String sourceGuiType, boolean adminForce) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.target = target;
        this.player = player;
        this.sourceGuiType = sourceGuiType != null ? sourceGuiType : "GuildSettingsGUI";
        this.adminForce = adminForce;
    }

    public ConfirmTransferLeaderGUI(GuildPlugin plugin, Guild guild, GuildMember target, Player player) {
        this(plugin, guild, target, player, "TransferLeaderGUI", false);
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-transfer-leader.title",
                "&cConfirm Transfer Leadership"));
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;

        String guildName = ColorUtils.stripColor(guild.getName());
        String targetName = ColorUtils.stripColor(target.getPlayerName());
        String content = languageManager.getGuiColoredMessage(player, "gui.confirm-transfer-leader.bedrock-content",
                "&fGuild: &e{guild}\n&fNew Leader: &e{member}\n&fAre you sure you want to transfer leadership?\n&cYou will become a regular member!\n&cThis action cannot be undone!",
                "{guild}", guildName,
                "{member}", targetName);

        SimpleForm form = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player, "gui.confirm-transfer-leader.bedrock-title",
                        "&cConfirm Transfer Leadership"))
                .content(content)
                .button(languageManager.getGuiColoredMessage(player, "gui.confirm-transfer-leader.bedrock-confirm",
                        "&cConfirm Transfer"))
                .button(languageManager.getGuiColoredMessage(player, "gui.confirm-transfer-leader.bedrock-cancel",
                        "&aCancel"))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (response.clickedButtonId() == 0) {
                        handleConfirm(player);
                    } else {
                        handleCancel(player);
                    }
                }))
                .closedResultHandler(() -> CompatibleScheduler.runTask(plugin, player, () ->
                        handleCancel(player)))
                .build();

        return BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        fillBorder(inventory);
        displayConfirmInfo(inventory);
        setupButtons(inventory);
        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11 -> handleConfirm(player);
            case 15 -> handleCancel(player);
        }
    }

    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 18, border);
        }
        for (int i = 9; i < 18; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }

    private void displayConfirmInfo(Inventory inventory) {
        String guildName = ColorUtils.stripColor(guild.getName());
        String targetName = ColorUtils.stripColor(target.getPlayerName());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.confirm-transfer-leader.guild", "&7Guild: &e{guild}", "{guild}", guildName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.confirm-transfer-leader.member", "&7New Leader: &e{member}", "{member}", targetName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.confirm-transfer-leader.confirm-question",
                "&7Are you sure you want to transfer leadership?")));
        if (!adminForce) {
            lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.confirm-transfer-leader.warning-demote",
                    "&cYou will become a regular member!")));
        }
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.confirm-transfer-leader.warning", "&cThis action cannot be undone!")));

        if (meta != null) {
            meta.setOwningPlayer(target.getOfflinePlayer());
            meta.setDisplayName(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.confirm-transfer-leader.info-title", "&cConfirm Transfer Leadership")));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        inventory.setItem(13, head);
    }

    private void setupButtons(Inventory inventory) {
        ItemStack confirm = createItem(
                Material.GOLD_BLOCK,
                ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.confirm-transfer-leader.confirm-button", "&cConfirm Transfer")),
                ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.confirm-transfer-leader.confirm-lore",
                        "&7Click to confirm leadership transfer"))
        );
        inventory.setItem(11, confirm);

        ItemStack cancel = createItem(
                Material.EMERALD_BLOCK,
                ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.confirm-transfer-leader.cancel-button", "&aCancel")),
                ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.confirm-transfer-leader.cancel-lore",
                        "&7Click to cancel leadership transfer"))
        );
        inventory.setItem(15, cancel);
    }

    private void handleConfirm(Player player) {
        // 鉴权
        if (adminForce) {
            if (!player.hasPermission("guild.admin")) {
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.common.no-permission", "&cInsufficient permission")));
                return;
            }
        } else {
            GuildMember executor = plugin.getGuildService().getGuildMember(player.getUniqueId());
            if (executor == null
                    || executor.getGuildId() != guild.getId()
                    || executor.getRole() != GuildMember.Role.LEADER
                    || !player.getUniqueId().equals(guild.getLeaderUuid())) {
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
                return;
            }
        }

        // 目标校验
        if (target.getPlayerUuid().equals(guild.getLeaderUuid())) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.confirm-transfer-leader.transfer-self",
                    "&cCannot transfer leadership to the current leader")));
            return;
        }
        if (target.getGuildId() != guild.getId()) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.confirm-transfer-leader.not-member",
                    "&cThat player is not a member of this guild!")));
            return;
        }

        plugin.getGuildService()
                .transferGuildLeadershipAsync(guild.getId(), target.getPlayerUuid(), target.getPlayerName(),
                        player.getUniqueId())
                .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        String msg = languageManager.getGuiMessage(player,
                                        "gui.confirm-transfer-leader.success",
                                        "&aLeadership transferred to &e{name}&a!")
                                .replace("{name}", target.getPlayerName());
                        player.sendMessage(ColorUtils.colorize(msg));

                        // 通知新会长
                        Player newLeader = plugin.getServer().getPlayer(target.getPlayerUuid());
                        if (newLeader != null) {
                            newLeader.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(newLeader,
                                    "gui.confirm-transfer-leader.notify-new",
                                    "&aYou are now the leader of guild &e{guild}&a!",
                                    "{guild}", guild.getName())));
                        }
                        // 通知旧会长（非管理员操作时，操作者本身就是旧会长）
                        if (adminForce) {
                            Player oldLeader = plugin.getServer().getPlayer(guild.getLeaderUuid());
                            if (oldLeader != null) {
                                oldLeader.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(oldLeader,
                                        "gui.confirm-transfer-leader.notify-old",
                                        "&cLeadership of guild &e{guild} &chas been transferred to &e{name}&c!",
                                        "{guild}", guild.getName(),
                                        "{name}", target.getPlayerName())));
                            }
                        }

                        guild.setLeaderUuid(target.getPlayerUuid());
                        guild.setLeaderName(target.getPlayerName());
                        returnToSource(player, true);
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.confirm-transfer-leader.failed",
                                "&cLeadership transfer failed!")));
                    }
                }));
    }

    private void handleCancel(Player player) {
        returnToSource(player, false);
    }

    private void returnToSource(Player player, boolean afterSuccess) {
        if ("GuildDetailGUI".equals(sourceGuiType)) {
            plugin.getGuiManager().openGUI(player, new GuildDetailGUI(plugin, guild, player));
        } else if (afterSuccess) {
            // 会长转让成功后已不再是会长，返回主菜单
            plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
        } else if ("TransferLeaderGUI".equals(sourceGuiType)) {
            plugin.getGuiManager().openGUI(player, new TransferLeaderGUI(plugin, guild, player));
        } else {
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
