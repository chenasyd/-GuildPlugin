package com.guild.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.gui.base.AbstractPagedListGUI;
import com.guild.gui.base.GuiLayoutUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;

import org.geysermc.cumulus.form.SimpleForm;

/**
 * 成员管理 GUI：分页成员列表 + 底栏功能按钮。
 */
public class MemberManagementGUI extends AbstractPagedListGUI<GuildMember> {

    public static final String FUNC_INVITE = "INVITE";
    public static final String FUNC_KICK = "KICK";
    public static final String FUNC_PROMOTE = "PROMOTE";
    public static final String FUNC_DEMOTE = "DEMOTE";

    private static final int TOOLBAR_INVITE = 45;
    private static final int TOOLBAR_KICK = 47;
    private static final int TOOLBAR_PROMOTE = 49;
    private static final int TOOLBAR_DEMOTE = 51;
    private static final int TOOLBAR_BACK = 53;

    private final Guild guild;

    public MemberManagementGUI(GuildPlugin plugin, Guild guild, Player player) {
        super(plugin, player, PaginationLayout.SIDE, GuiLayoutUtils.ITEMS_PER_PAGE);
        this.guild = guild;
        loadMembers();
    }

    private void loadMembers() {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(members -> {
            setEntries(members == null ? List.of() : members);
            CompatibleScheduler.runTask(plugin, viewer, () -> {
                if (viewer.isOnline()) {
                    plugin.getGuiManager().refreshGUI(viewer);
                }
            });
        });
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.member-management.title", "&6Member Management"));
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
    }

    @Override
    protected String prevPageTitleKey() {
        return "gui.member-management.items.previous-page.name";
    }

    @Override
    protected String prevPageTitleDefault() {
        return "&cPrevious Page";
    }

    @Override
    protected String prevPageLoreKey() {
        return "gui.member-management.items.previous-page.lore.1";
    }

    @Override
    protected String prevPageLoreDefault() {
        return "&7View previous page";
    }

    @Override
    protected String nextPageTitleKey() {
        return "gui.member-management.items.next-page.name";
    }

    @Override
    protected String nextPageTitleDefault() {
        return "&aNext Page";
    }

    @Override
    protected String nextPageLoreKey() {
        return "gui.member-management.items.next-page.lore.1";
    }

    @Override
    protected String nextPageLoreDefault() {
        return "&7View next page";
    }

    @Override
    protected void displayEmptyState(Inventory inventory) {
        inventory.setItem(22, createItem(
                Material.BARRIER,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.member-mgmt.no-members", "&cNo Members")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.member-mgmt.no-members.desc",
                        "&7There are no members in the guild yet"))));
    }

    @Override
    protected void setupToolbar(Inventory inventory) {
        inventory.setItem(TOOLBAR_INVITE, createItem(
                Material.EMERALD_BLOCK,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.items.invite-member.name", "&aInvite Member")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.items.invite-member.lore.1", "&7Invite new member"))));

        inventory.setItem(TOOLBAR_KICK, createItem(
                Material.REDSTONE_BLOCK,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.items.kick-member.name", "&cKick Member")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.items.kick-member.lore.1", "&7Kick guild member"))));

        inventory.setItem(TOOLBAR_PROMOTE, createItem(
                Material.GOLD_INGOT,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.items.promote-member.name", "&6Promote Member")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.items.promote-member.lore.1", "&7Promote member role"))));

        inventory.setItem(TOOLBAR_DEMOTE, createItem(
                Material.IRON_INGOT,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.items.demote-member.name", "&7Demote Member")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.items.demote-member.lore.1", "&7Demote member role"))));

        inventory.setItem(TOOLBAR_BACK, createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.items.back.name", "&7Back")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.member-management.items.back.lore.1", "&7Return to main menu"))));
    }

    @Override
    protected boolean handleToolbarClick(Player player, int slot) {
        switch (slot) {
            case TOOLBAR_INVITE -> {
                handleInviteMember(player);
                return true;
            }
            case TOOLBAR_KICK -> {
                handleKickMember(player);
                return true;
            }
            case TOOLBAR_PROMOTE -> {
                handlePromoteMember(player);
                return true;
            }
            case TOOLBAR_DEMOTE -> {
                handleDemoteMember(player);
                return true;
            }
            case TOOLBAR_BACK -> {
                openBackGui(player);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected ItemStack createEntryItem(GuildMember member) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        String name;
        List<String> lore = new ArrayList<>();

        switch (member.getRole()) {
            case LEADER -> {
                name = PlaceholderUtils.replaceMemberPlaceholders("&c{member_name}", member, guild, viewer);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7"
                        + languageManager.getGuiMessage(viewer, "gui.member-management.member-details.role", "Role")
                        + ": &c{member_role}", member, guild, viewer));
            }
            case OFFICER -> {
                name = PlaceholderUtils.replaceMemberPlaceholders("&6{member_name}", member, guild, viewer);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7"
                        + languageManager.getGuiMessage(viewer, "gui.member-management.member-details.role", "Role")
                        + ": &6{member_role}", member, guild, viewer));
            }
            default -> {
                name = PlaceholderUtils.replaceMemberPlaceholders("&f{member_name}", member, guild, viewer);
                lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7"
                        + languageManager.getGuiMessage(viewer, "gui.member-management.member-details.role", "Role")
                        + ": &f{member_role}", member, guild, viewer));
            }
        }

        lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7"
                + languageManager.getGuiMessage(viewer, "gui.member-management.member-details.join-time", "Join time")
                + ": {member_join_time}", member, guild, viewer));
        lore.add(PlaceholderUtils.replaceMemberPlaceholders("&7"
                + languageManager.getGuiMessage(viewer, "gui.member-management.member-details.permissions", "Permissions")
                + ": " + getRolePermissions(member.getRole()), member, guild, viewer));
        lore.add("");
        lore.add(ColorUtils.colorize("&a" + languageManager.getGuiMessage(viewer,
                "gui.member-management.member-details.view-details", "Left click: View details")));

        if (member.getRole() != GuildMember.Role.LEADER) {
            lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                    "gui.member-management.member-details.kick-member", "Right click: Kick member")));
            lore.add(ColorUtils.colorize("&6" + languageManager.getGuiMessage(viewer,
                    "gui.member-management.member-details.promote-demote", "Shift+Left click: Promote/Demote")));
        }

        if (meta != null) {
            meta.setOwningPlayer(member.getOfflinePlayer());
            meta.setDisplayName(ColorUtils.colorize(name));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    protected void onEntryClick(Player player, GuildMember member, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            plugin.getGuiManager().openGUI(player, new MemberDetailsGUI(plugin, guild, member, player));
        } else if (clickType == ClickType.RIGHT) {
            handleKickMemberDirect(player, member);
        } else if (clickType == ClickType.SHIFT_LEFT) {
            handlePromoteDemoteMember(player, member);
        }
    }

    private String getRolePermissions(GuildMember.Role role) {
        var rules = plugin.getMembershipRules();
        if (rules.roleMatrixCanInvite(role) && rules.roleMatrixCanKick(role)
                && rules.roleMatrixCanPromote(role) && rules.roleMatrixCanDeleteGuild(role)) {
            return languageManager.getGuiMessage(viewer,
                    "gui.member-management.member-mgmt.role.leader-perms", "All Permissions");
        }
        if (rules.roleMatrixCanInvite(role) || rules.roleMatrixCanKick(role)) {
            return languageManager.getGuiMessage(viewer,
                    "gui.member-management.member-mgmt.role.officer-perms", "Invite, Kick");
        }
        return languageManager.getGuiMessage(viewer,
                "gui.member-management.member-mgmt.role.member-perms", "Basic Permissions");
    }

    private void handleKickMemberDirect(Player player, GuildMember member) {
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (executor == null || !plugin.getMembershipRules().canKick(executor)) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.common.no-permission", "&cInsufficient permission")));
                    return;
                }
                if (member.getRole() == GuildMember.Role.LEADER) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.common.cannot-kick-leader", "&cCannot kick guild leader")));
                    return;
                }
                plugin.getGuiManager().openGUI(player,
                        new ConfirmKickMemberGUI(plugin, guild, member, player, "MemberManagementGUI"));
            });
        });
    }

    private void handlePromoteDemoteMember(Player player, GuildMember member) {
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (member.getRole() == GuildMember.Role.LEADER) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.common.cannot-modify-leader", "&cCannot modify the guild leader's position")));
                    return;
                }
                if (member.getRole() == GuildMember.Role.OFFICER) {
                    if (executor == null || !plugin.getMembershipRules().canDemote(executor)) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
                        return;
                    }
                    plugin.getGuiManager().openGUI(player,
                            new ConfirmDemoteMemberGUI(plugin, guild, member, player, "MemberManagementGUI"));
                } else if (member.getRole() == GuildMember.Role.MEMBER) {
                    if (executor == null || !plugin.getMembershipRules().canPromote(executor)) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
                        return;
                    }
                    plugin.getGuiManager().openGUI(player,
                            new ConfirmPromoteMemberGUI(plugin, guild, member, player, "MemberManagementGUI"));
                }
            });
        });
    }

    private void handleInviteMember(Player player) {
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (member == null || !plugin.getMembershipRules().canInvite(member)) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.common.no-permission", "&cInsufficient permission")));
                    return;
                }
                plugin.getGuiManager().openGUI(player, new InviteMemberGUI(plugin, guild, player));
            });
        });
    }

    private void openInviteGuiIfAllowed(Player player) {
        handleInviteMember(player);
    }

    private void handleKickMember(Player player) {
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (member == null || !plugin.getMembershipRules().canKick(member)) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.common.no-permission", "&cInsufficient permission")));
                    return;
                }
                plugin.getGuiManager().openGUI(player, new KickMemberGUI(plugin, guild, player));
            });
        });
    }

    private void handlePromoteMember(Player player) {
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (member == null || !plugin.getMembershipRules().canPromote(player)) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
                    return;
                }
                plugin.getGuiManager().openGUI(player, new PromoteMemberGUI(plugin, guild, player));
            });
        });
    }

    private void handleDemoteMember(Player player) {
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (member == null || !plugin.getMembershipRules().canDemote(player)) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
                    return;
                }
                plugin.getGuiManager().openGUI(player, new DemoteMemberGUI(plugin, guild, player));
            });
        });
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }
        sendBedrockMemberList(player, 0);
        return true;
    }

    private void sendBedrockMemberList(Player player, int page) {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(members -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (members == null || members.isEmpty()) {
                    SimpleForm form = SimpleForm.builder()
                            .title(languageManager.getGuiColoredMessage(player,
                                    "gui.member-management.bedrock-title", "&6Member Management"))
                            .content(languageManager.getGuiColoredMessage(player,
                                    "gui.member-management.bedrock-no-members",
                                    "&fThere are no members in the guild yet"))
                            .button(languageManager.getGuiColoredMessage(player,
                                    "gui.member-management.bedrock-invite", "&aInvite Member"))
                            .button(languageManager.getGuiColoredMessage(player,
                                    "gui.common.bedrock-back", "&cBack"))
                            .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                                if (response.clickedButtonId() == 0) {
                                    openInviteGuiIfAllowed(player);
                                } else {
                                    openBackGui(player);
                                }
                            }))
                            .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                                    () -> openBackGui(player)))
                            .build();
                    BedrockFormSender.sendForm(player.getUniqueId(), form);
                    return;
                }

                final int itemsPerPage = GuiLayoutUtils.BEDROCK_ITEMS_PER_PAGE;
                int totalPages = GuiLayoutUtils.maxPageIndex(members.size(), itemsPerPage);
                final int safePage = Math.max(0, Math.min(page, totalPages));
                final int startIndex = safePage * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, members.size());
                final int memberCount = endIndex - startIndex;

                SimpleForm.Builder builder = SimpleForm.builder()
                        .title(languageManager.getGuiColoredMessage(player,
                                "gui.member-management.bedrock-title-page",
                                "&6Member Management - Page {page}", "{page}", String.valueOf(safePage + 1)))
                        .content(languageManager.getGuiColoredMessage(player,
                                "gui.member-management.bedrock-member-list",
                                "&fMember List (Total {count})", "{count}", String.valueOf(members.size())));

                for (int i = startIndex; i < endIndex; i++) {
                    GuildMember m = members.get(i);
                    String roleColor = switch (m.getRole()) {
                        case LEADER -> "§c";
                        case OFFICER -> "§6";
                        default -> "§f";
                    };
                    builder.button(roleColor + m.getPlayerName());
                }

                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.member-management.bedrock-invite", "&aInvite Member"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.common.bedrock-prev-page", "&ePrevious Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.common.bedrock-next-page", "&eNext Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.common.bedrock-back", "&cBack"));

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int clicked = response.clickedButtonId();
                    if (clicked < memberCount) {
                        sendBedrockMemberActions(player, members.get(startIndex + clicked));
                    } else if (clicked == memberCount) {
                        openInviteGuiIfAllowed(player);
                    } else if (clicked == memberCount + 1) {
                        sendBedrockMemberList(player, safePage - 1);
                    } else if (clicked == memberCount + 2) {
                        sendBedrockMemberList(player, safePage + 1);
                    } else {
                        openBackGui(player);
                    }
                }));

                builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                        () -> openBackGui(player)));

                BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
            });
        });
    }

    private void sendBedrockMemberActions(Player player, GuildMember member) {
        String roleText = switch (member.getRole()) {
            case LEADER -> languageManager.getGuiColoredMessage(player,
                    "gui.member-management.bedrock-role-leader", "&cLeader");
            case OFFICER -> languageManager.getGuiColoredMessage(player,
                    "gui.member-management.bedrock-role-officer", "&6Officer");
            default -> languageManager.getGuiColoredMessage(player,
                    "gui.member-management.bedrock-role-member", "&fMember");
        };

        SimpleForm form = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player,
                        "gui.member-management.bedrock-actions-title",
                        "&6Member Actions - {member}", "{member}", member.getPlayerName()))
                .content(languageManager.getGuiColoredMessage(player,
                        "gui.member-management.bedrock-role-label", "&fRole: {role}", "{role}", roleText))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.member-management.bedrock-view-details", "&eView Details"))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.member-management.bedrock-kick", "&cKick Member"))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.member-management.bedrock-promote-demote", "&6Promote/Demote"))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.member-management.bedrock-back-to-list", "&cBack to List"))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> plugin.getGuiManager().openGUI(player,
                                new MemberDetailsGUI(plugin, guild, member, player));
                        case 1 -> handleKickMemberDirect(player, member);
                        case 2 -> handlePromoteDemoteMember(player, member);
                        case 3 -> sendBedrockMemberList(player, 0);
                    }
                }))
                .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                        () -> sendBedrockMemberList(player, 0)))
                .build();
        BedrockFormSender.sendForm(player.getUniqueId(), form);
    }
}
