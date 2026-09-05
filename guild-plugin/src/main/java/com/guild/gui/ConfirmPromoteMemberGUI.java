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

/** 确认提升成员 GUI（含鉴权，支持离线目标） */
public class ConfirmPromoteMemberGUI extends AbstractConfirmGUI {

    private final GuildMember member;

    public ConfirmPromoteMemberGUI(GuildPlugin plugin, Guild guild, GuildMember member, Player player, String sourceGuiType) {
        super(plugin, guild, player, sourceGuiType != null ? sourceGuiType : "PromoteMemberGUI");
        this.member = member;
    }

    public ConfirmPromoteMemberGUI(GuildPlugin plugin, Guild guild, GuildMember member, Player player) {
        this(plugin, guild, member, player, "PromoteMemberGUI");
    }

    @Override
    protected String titleKey() {
        return "gui.confirm-promote-member.title";
    }

    @Override
    protected String titleDefault() {
        return "&6Confirm Promote Member";
    }

    @Override
    protected String bedrockTitleKey() {
        return "gui.confirm-promote-member.bedrock-title";
    }

    @Override
    protected String bedrockTitleDefault() {
        return "&6Confirm Promote Member";
    }

    @Override
    protected String bedrockContentKey() {
        return "gui.confirm-promote-member.bedrock-content";
    }

    @Override
    protected String bedrockContentDefault() {
        return "&fGuild: &e{guild}\n&fMember: &e{member}\n&fPromote this member to officer?";
    }

    @Override
    protected String bedrockConfirmKey() {
        return "gui.confirm-promote-member.bedrock-confirm";
    }

    @Override
    protected String bedrockConfirmDefault() {
        return "&6Confirm Promote";
    }

    @Override
    protected String bedrockCancelKey() {
        return "gui.confirm-promote-member.bedrock-cancel";
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
        return "gui.confirm-promote-member.confirm-button";
    }

    @Override
    protected String confirmButtonDefault() {
        return "&6Confirm Promote";
    }

    @Override
    protected String confirmLoreKey() {
        return "gui.confirm-promote-member.confirm-lore";
    }

    @Override
    protected String confirmLoreDefault() {
        return "&7Click to confirm promotion";
    }

    @Override
    protected String cancelButtonKey() {
        return "gui.confirm-promote-member.cancel-button";
    }

    @Override
    protected String cancelButtonDefault() {
        return "&aCancel";
    }

    @Override
    protected String cancelLoreKey() {
        return "gui.confirm-promote-member.cancel-lore";
    }

    @Override
    protected String cancelLoreDefault() {
        return "&7Click to cancel";
    }

    @Override
    protected ItemStack createInfoItem() {
        String guildName = ColorUtils.stripColor(guild.getName());
        String memberName = ColorUtils.stripColor(member.getPlayerName());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.confirm-promote-member.guild", "&7Guild: &e{guild}", "{guild}", guildName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.confirm-promote-member.member", "&7Member: &e{member}", "{member}", memberName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.confirm-promote-member.confirm-question", "&7Promote this member to officer?")));

        if (meta != null) {
            meta.setOwningPlayer(member.getOfflinePlayer());
            meta.setDisplayName(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                    "gui.confirm-promote-member.info-title", "&6Confirm Promote Member")));
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
                if (!canPromote(executor)) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
                    return;
                }
                if (member.getGuildId() != guild.getId()) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.confirm-promote-member.member-left", "&cThat member is no longer in the guild!")));
                    return;
                }
                if (member.getRole() == GuildMember.Role.LEADER) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.confirm-promote-member.cannot-promote-leader", "&cCannot promote the guild leader!")));
                    return;
                }
                if (member.getRole() == GuildMember.Role.OFFICER) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.confirm-promote-member.already-officer", "&cThat member is already an officer!")));
                    return;
                }

                plugin.getGuildService()
                        .updateMemberRoleAsync(member.getPlayerUuid(), GuildMember.Role.OFFICER, player.getUniqueId())
                        .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                            if (success) {
                                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                        "gui.confirm-promote-member.promote.success",
                                        "&aPromoted &e{member} &ato officer!", "{member}", member.getPlayerName())));

                                Player target = plugin.getServer().getPlayer(member.getPlayerUuid());
                                if (target != null) {
                                    target.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(target,
                                            "gui.confirm-promote-member.promote.promoted",
                                            "&aYou have been promoted to officer of guild &e{guild} &a!",
                                            "{guild}", guild.getName())));
                                }
                                returnToSource(player, true);
                            } else {
                                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                        "gui.confirm-promote-member.promote.failed", "&cFailed to promote member!")));
                            }
                        }));
            });
        });
    }

    @Override
    protected void onCancel(Player player) {
        returnToSource(player, false);
    }

    private boolean canPromote(GuildMember executor) {
        return executor != null
                && executor.getGuildId() == guild.getId()
                && plugin.getMembershipRules().canPromote(executor)
                && viewer.getUniqueId().equals(guild.getLeaderUuid());
    }

    private void returnToSource(Player player, boolean afterSuccess) {
        switch (sourceGuiType) {
            case "DemoteMemberGUI" -> plugin.getGuiManager().openGUI(player, new DemoteMemberGUI(plugin, guild, player));
            case "MemberManagementGUI" ->
                    plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
            case "MemberDetailsGUI" -> {
                if (afterSuccess) {
                    plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
                } else {
                    plugin.getGuiManager().openGUI(player, new MemberDetailsGUI(plugin, guild, member, player));
                }
            }
            case "GuildCommand" -> player.closeInventory();
            default -> plugin.getGuiManager().openGUI(player, new PromoteMemberGUI(plugin, guild, player));
        }
    }
}
