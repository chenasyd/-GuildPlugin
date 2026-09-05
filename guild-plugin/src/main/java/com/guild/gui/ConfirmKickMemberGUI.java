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

/** 确认踢出成员 GUI */
public class ConfirmKickMemberGUI extends AbstractConfirmGUI {

    private final GuildMember member;

    public ConfirmKickMemberGUI(GuildPlugin plugin, Guild guild, GuildMember member, Player player, String sourceGuiType) {
        super(plugin, guild, player, sourceGuiType != null ? sourceGuiType : "MemberManagementGUI");
        this.member = member;
    }

    public ConfirmKickMemberGUI(GuildPlugin plugin, Guild guild, GuildMember member, Player player) {
        this(plugin, guild, member, player, "MemberManagementGUI");
    }

    @Override
    protected String titleKey() {
        return "gui.confirm-kick-member.title";
    }

    @Override
    protected String titleDefault() {
        return "&cConfirm Kick Member";
    }

    @Override
    protected String bedrockTitleKey() {
        return "gui.confirm-kick-member.bedrock-title";
    }

    @Override
    protected String bedrockTitleDefault() {
        return "&cConfirm Kick Member";
    }

    @Override
    protected String bedrockContentKey() {
        return "gui.confirm-kick-member.bedrock-content";
    }

    @Override
    protected String bedrockContentDefault() {
        return "&fGuild: &e{guild}\n&fMember: &e{member}\n&fAre you sure you want to kick this member?\n&cThis action cannot be undone!";
    }

    @Override
    protected String bedrockConfirmKey() {
        return "gui.confirm-kick-member.bedrock-confirm";
    }

    @Override
    protected String bedrockConfirmDefault() {
        return "&cConfirm Kick";
    }

    @Override
    protected String bedrockCancelKey() {
        return "gui.confirm-kick-member.bedrock-cancel";
    }

    @Override
    protected String bedrockCancelDefault() {
        return "&aCancel";
    }

    @Override
    protected Material confirmMaterial() {
        return Material.REDSTONE_BLOCK;
    }

    @Override
    protected String confirmButtonKey() {
        return "gui.confirm-kick-member.confirm-button";
    }

    @Override
    protected String confirmButtonDefault() {
        return "&cConfirm Kick";
    }

    @Override
    protected String confirmLoreKey() {
        return "gui.confirm-kick-member.confirm-lore";
    }

    @Override
    protected String confirmLoreDefault() {
        return "&7Click to confirm kicking member";
    }

    @Override
    protected String cancelButtonKey() {
        return "gui.confirm-kick-member.cancel-button";
    }

    @Override
    protected String cancelButtonDefault() {
        return "&aCancel";
    }

    @Override
    protected String cancelLoreKey() {
        return "gui.confirm-kick-member.cancel-lore";
    }

    @Override
    protected String cancelLoreDefault() {
        return "&7Click to cancel kicking member";
    }

    @Override
    protected ItemStack createInfoItem() {
        String guildName = ColorUtils.stripColor(guild.getName());
        String memberName = ColorUtils.stripColor(member.getPlayerName());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.confirm-kick-member.guild", "&7Guild: &e{guild}", "{guild}", guildName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.confirm-kick-member.member", "&7Member: &e{member}", "{member}", memberName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.confirm-kick-member.confirm-question", "&7Are you sure you want to kick this member?")));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.confirm-kick-member.warning", "&cThis action cannot be undone!")));

        if (meta != null) {
            meta.setOwningPlayer(member.getOfflinePlayer());
            meta.setDisplayName(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                    "gui.confirm-kick-member.info-title", "&cConfirm Kick Member")));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    protected String[] bedrockContentPlaceholders() {
        return new String[]{
                "{guild}", ColorUtils.stripColor(guild.getName()),
                "{member}", ColorUtils.stripColor(member.getPlayerName())
        };
    }

    @Override
    protected void onConfirm(Player player) {
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (executor == null || !plugin.getMembershipRules().canKick(executor)) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.common.no-permission", "&cInsufficient permission")));
                    return;
                }
                if (member.getGuildId() != guild.getId()) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.confirm-kick-member.member-left", "&cThat member is no longer in the guild!")));
                    return;
                }
                if (member.getRole() == GuildMember.Role.LEADER) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.confirm-kick-member.cannot-kick-leader", "&cCannot kick the guild leader!")));
                    return;
                }

                plugin.getGuildService().removeGuildMemberAsync(member.getPlayerUuid(), player.getUniqueId())
                        .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                            if (success) {
                                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                        "gui.confirm-kick-member.kick.success",
                                        "&aSuccessfully kicked &e{member} &a!", "{member}", member.getPlayerName())));

                                Player kickedPlayer = plugin.getServer().getPlayer(member.getPlayerUuid());
                                if (kickedPlayer != null) {
                                    kickedPlayer.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(
                                            kickedPlayer, "gui.confirm-kick-member.kick.kicked",
                                            "&cYou have been kicked from guild &e{guild} &c!",
                                            "{guild}", guild.getName())));
                                }
                                player.closeInventory();
                            } else {
                                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                        "gui.confirm-kick-member.kick.failed", "&cFailed to kick member!")));
                            }
                        }));
            });
        });
    }

    @Override
    protected void onCancel(Player player) {
        player.closeInventory();
    }
}
