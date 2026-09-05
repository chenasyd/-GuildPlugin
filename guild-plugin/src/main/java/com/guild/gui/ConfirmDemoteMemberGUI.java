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
 * 确认降级成员 GUI（含鉴权，支持离线目标）
 */
public class ConfirmDemoteMemberGUI implements GUI {

    public static final String FUNC_CONFIRM = "CONFIRM";
    public static final String FUNC_INFO = "INFO";
    public static final String FUNC_CANCEL = "CANCEL";

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final Guild guild;
    private final GuildMember member;
    private final Player player;
    private final String sourceGuiType;

    public ConfirmDemoteMemberGUI(GuildPlugin plugin, Guild guild, GuildMember member, Player player, String sourceGuiType) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.member = member;
        this.player = player;
        this.sourceGuiType = sourceGuiType != null ? sourceGuiType : "DemoteMemberGUI";
    }

    public ConfirmDemoteMemberGUI(GuildPlugin plugin, Guild guild, GuildMember member, Player player) {
        this(plugin, guild, member, player, "DemoteMemberGUI");
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-demote-member.title",
                "&7Confirm Demote Member"));
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;

        String guildName = ColorUtils.stripColor(guild.getName());
        String memberName = ColorUtils.stripColor(member.getPlayerName());
        String content = languageManager.getGuiColoredMessage(player, "gui.confirm-demote-member.bedrock-content",
                "&fGuild: &e{guild}\n&fMember: &e{member}\n&fDemote this officer to member?",
                "{guild}", guildName, "{member}", memberName);

        SimpleForm form = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player, "gui.confirm-demote-member.bedrock-title",
                        "&7Confirm Demote Member"))
                .content(content)
                .button(languageManager.getGuiColoredMessage(player, "gui.confirm-demote-member.bedrock-confirm",
                        "&7Confirm Demote"))
                .button(languageManager.getGuiColoredMessage(player, "gui.confirm-demote-member.bedrock-cancel",
                        "&aCancel"))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (response.clickedButtonId() == 0) {
                        handleConfirm(player);
                    } else {
                        handleCancel(player);
                    }
                }))
                .closedResultHandler(() -> CompatibleScheduler.runTask(plugin, player, () -> handleCancel(player)))
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
        String memberName = ColorUtils.stripColor(member.getPlayerName());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.confirm-demote-member.guild", "&7Guild: &e{guild}", "{guild}", guildName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.confirm-demote-member.member", "&7Member: &e{member}", "{member}", memberName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.confirm-demote-member.confirm-question", "&7Demote this officer to member?")));

        if (meta != null) {
            meta.setOwningPlayer(member.getOfflinePlayer());
            meta.setDisplayName(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.confirm-demote-member.info-title", "&7Confirm Demote Member")));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        inventory.setItem(13, head);
    }

    private void setupButtons(Inventory inventory) {
        inventory.setItem(11, createItem(Material.IRON_BLOCK,
                ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.confirm-demote-member.confirm-button", "&7Confirm Demote")),
                ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.confirm-demote-member.confirm-lore", "&7Click to confirm demotion"))));

        inventory.setItem(15, createItem(Material.EMERALD_BLOCK,
                ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.confirm-demote-member.cancel-button", "&aCancel")),
                ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.confirm-demote-member.cancel-lore", "&7Click to cancel"))));
    }

    private void handleConfirm(Player player) {
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (!canDemote(executor)) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
                    return;
                }
                if (member.getGuildId() != guild.getId()) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.confirm-demote-member.member-left", "&cThat member is no longer in the guild!")));
                    return;
                }
                if (member.getRole() == GuildMember.Role.LEADER) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.confirm-demote-member.cannot-demote-leader",
                            "&cCannot demote the guild leader!")));
                    return;
                }
                if (member.getRole() != GuildMember.Role.OFFICER) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.confirm-demote-member.not-officer",
                            "&cThat member is not an officer!")));
                    return;
                }

                plugin.getGuildService()
                        .updateMemberRoleAsync(member.getPlayerUuid(), GuildMember.Role.MEMBER, player.getUniqueId())
                        .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                            if (success) {
                                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                        "gui.confirm-demote-member.demote.success",
                                        "&aDemoted &e{member} &ato member!", "{member}", member.getPlayerName())));

                                Player target = plugin.getServer().getPlayer(member.getPlayerUuid());
                                if (target != null) {
                                    target.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(target,
                                            "gui.confirm-demote-member.demote.demoted",
                                            "&aYou have been demoted to member of guild &e{guild} &a!",
                                            "{guild}", guild.getName())));
                                }
                                returnToSource(player, true);
                            } else {
                                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                        "gui.confirm-demote-member.demote.failed",
                                        "&cFailed to demote member!")));
                            }
                        }));
            });
        });
    }

    private boolean canDemote(GuildMember executor) {
        return executor != null
                && executor.getGuildId() == guild.getId()
                && executor.getRole() == GuildMember.Role.LEADER
                && player.getUniqueId().equals(guild.getLeaderUuid());
    }

    private void handleCancel(Player player) {
        returnToSource(player, false);
    }

    private void returnToSource(Player player, boolean afterSuccess) {
        switch (sourceGuiType) {
            case "PromoteMemberGUI" -> plugin.getGuiManager().openGUI(player, new PromoteMemberGUI(plugin, guild, player));
            case "MemberManagementGUI" -> plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
            case "MemberDetailsGUI" -> {
                if (afterSuccess) {
                    plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
                } else {
                    plugin.getGuiManager().openGUI(player, new MemberDetailsGUI(plugin, guild, member, player));
                }
            }
            case "GuildCommand" -> player.closeInventory();
            default -> plugin.getGuiManager().openGUI(player, new DemoteMemberGUI(plugin, guild, player));
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
