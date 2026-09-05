package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.base.AbstractConfirmGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/** 确认转移会长 GUI（含鉴权，支持管理员强制转让） */
public class ConfirmTransferLeaderGUI extends AbstractConfirmGUI {

    private final GuildMember target;
    /** true = 管理员强制转让（GuildDetailGUI 等管理入口） */
    private final boolean adminForce;

    public ConfirmTransferLeaderGUI(GuildPlugin plugin, Guild guild, GuildMember target,
                                    Player player, String sourceGuiType, boolean adminForce) {
        super(plugin, guild, player, sourceGuiType != null ? sourceGuiType : "GuildSettingsGUI");
        this.target = target;
        this.adminForce = adminForce;
    }

    public ConfirmTransferLeaderGUI(GuildPlugin plugin, Guild guild, GuildMember target, Player player) {
        this(plugin, guild, target, player, "TransferLeaderGUI", false);
    }

    @Override
    protected String titleKey() {
        return "gui.confirm-transfer-leader.title";
    }

    @Override
    protected String titleDefault() {
        return "&cConfirm Transfer Leadership";
    }

    @Override
    protected String bedrockTitleKey() {
        return "gui.confirm-transfer-leader.bedrock-title";
    }

    @Override
    protected String bedrockTitleDefault() {
        return "&cConfirm Transfer Leadership";
    }

    @Override
    protected String bedrockContentKey() {
        return "gui.confirm-transfer-leader.bedrock-content";
    }

    @Override
    protected String bedrockContentDefault() {
        return "&fGuild: &e{guild}\n&fNew Leader: &e{member}\n&fAre you sure you want to transfer leadership?\n&cYou will become a regular member!\n&cThis action cannot be undone!";
    }

    @Override
    protected String bedrockConfirmKey() {
        return "gui.confirm-transfer-leader.bedrock-confirm";
    }

    @Override
    protected String bedrockConfirmDefault() {
        return "&cConfirm Transfer";
    }

    @Override
    protected String bedrockCancelKey() {
        return "gui.confirm-transfer-leader.bedrock-cancel";
    }

    @Override
    protected String bedrockCancelDefault() {
        return "&aCancel";
    }

    @Override
    protected Material confirmMaterial() {
        return Material.GOLD_BLOCK;
    }

    @Override
    protected String confirmButtonKey() {
        return "gui.confirm-transfer-leader.confirm-button";
    }

    @Override
    protected String confirmButtonDefault() {
        return "&cConfirm Transfer";
    }

    @Override
    protected String confirmLoreKey() {
        return "gui.confirm-transfer-leader.confirm-lore";
    }

    @Override
    protected String confirmLoreDefault() {
        return "&7Click to confirm leadership transfer";
    }

    @Override
    protected String cancelButtonKey() {
        return "gui.confirm-transfer-leader.cancel-button";
    }

    @Override
    protected String cancelButtonDefault() {
        return "&aCancel";
    }

    @Override
    protected String cancelLoreKey() {
        return "gui.confirm-transfer-leader.cancel-lore";
    }

    @Override
    protected String cancelLoreDefault() {
        return "&7Click to cancel leadership transfer";
    }

    @Override
    protected ItemStack createInfoItem() {
        String guildName = ColorUtils.stripColor(guild.getName());
        String targetName = ColorUtils.stripColor(target.getPlayerName());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.confirm-transfer-leader.guild", "&7Guild: &e{guild}", "{guild}", guildName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.confirm-transfer-leader.member", "&7New Leader: &e{member}", "{member}", targetName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.confirm-transfer-leader.confirm-question",
                "&7Are you sure you want to transfer leadership?")));
        if (!adminForce) {
            lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                    "gui.confirm-transfer-leader.warning-demote",
                    "&cYou will become a regular member!")));
        }
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.confirm-transfer-leader.warning", "&cThis action cannot be undone!")));

        if (meta != null) {
            meta.setOwningPlayer(target.getOfflinePlayer());
            meta.setDisplayName(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                    "gui.confirm-transfer-leader.info-title", "&cConfirm Transfer Leadership")));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    protected String[] bedrockContentPlaceholders() {
        return new String[]{
                "{guild}", ColorUtils.stripColor(guild.getName()),
                "{member}", ColorUtils.stripColor(target.getPlayerName())
        };
    }

    @Override
    protected void onConfirm(Player player) {
        if (adminForce) {
            if (!player.hasPermission("guild.admin")) {
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.common.no-permission", "&cInsufficient permission")));
                return;
            }
        } else {
            if (!plugin.getMembershipRules().isLeaderOf(player, guild.getId())) {
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
                return;
            }
        }

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

                        Player newLeader = plugin.getServer().getPlayer(target.getPlayerUuid());
                        if (newLeader != null) {
                            newLeader.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(newLeader,
                                    "gui.confirm-transfer-leader.notify-new",
                                    "&aYou are now the leader of guild &e{guild}&a!",
                                    "{guild}", guild.getName())));
                        }
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

    @Override
    protected void onCancel(Player player) {
        returnToSource(player, false);
    }

    private void returnToSource(Player player, boolean afterSuccess) {
        if ("GuildDetailGUI".equals(sourceGuiType)) {
            plugin.getGuiManager().openGUI(player, new GuildDetailGUI(plugin, guild, player));
        } else if (afterSuccess) {
            plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
        } else if ("TransferLeaderGUI".equals(sourceGuiType)) {
            plugin.getGuiManager().openGUI(player, new TransferLeaderGUI(plugin, guild, player));
        } else {
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
        }
    }
}
