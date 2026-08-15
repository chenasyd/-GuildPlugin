package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;

import org.geysermc.cumulus.form.SimpleForm;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * 确认踢出成员GUI
 */
public class ConfirmKickMemberGUI implements GUI {

    // ── 图像模式功能常量 ──
    public static final String FUNC_CONFIRM = "CONFIRM";
    public static final String FUNC_INFO = "INFO";
    public static final String FUNC_CANCEL = "CANCEL";

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final Guild guild;
    private final GuildMember member;
    private final Player player;
    private final String sourceGuiType;

    public ConfirmKickMemberGUI(GuildPlugin plugin, Guild guild, GuildMember member, Player player, String sourceGuiType) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.member = member;
        this.player = player;
        this.sourceGuiType = sourceGuiType != null ? sourceGuiType : "MemberManagementGUI";
    }

    public ConfirmKickMemberGUI(GuildPlugin plugin, Guild guild, GuildMember member, Player player) {
        this(plugin, guild, member, player, "MemberManagementGUI");
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-kick-member.title",
                "&cConfirm Kick Member"));
    }
    
    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;

        String memberName = ColorUtils.stripColor(member.getPlayerName());
        String content = languageManager.getGuiColoredMessage(player, "gui.confirm-kick-member.bedrock-content",
                "&fGuild: &e{guild}\n&fMember: &e{member}\n&fAre you sure you want to kick this member?\n&cThis action cannot be undone!",
                "{guild}", ColorUtils.stripColor(guild.getName()),
                "{member}", memberName);

        SimpleForm form = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player, "gui.confirm-kick-member.bedrock-title", "&cConfirm Kick Member"))
                .content(content)
                .button(languageManager.getGuiColoredMessage(player, "gui.confirm-kick-member.bedrock-confirm", "&cConfirm Kick"))
                .button(languageManager.getGuiColoredMessage(player, "gui.confirm-kick-member.bedrock-cancel", "&aCancel"))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (response.clickedButtonId() == 0) {
                        handleConfirmKick(player);
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
        // 填充边框
        fillBorder(inventory);
        
        // 显示确认信息
        displayConfirmInfo(inventory);
        
        // 添加确认和取消按钮
        setupButtons(inventory);

        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // 确认踢出
                handleConfirmKick(player);
                break;
            case 15: // 取消
                handleCancel(player);
                break;
        }
    }
    
    /**
     * 填充边框
     */
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
    
    /**
     * 显示确认信息
     */
    private void displayConfirmInfo(Inventory inventory) {
        String guildName = ColorUtils.stripColor(guild.getName());
        String memberName = ColorUtils.stripColor(member.getPlayerName());
        
        // 创建玩家头像
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-kick-member.guild", "&7Guild: &e{guild}", "{guild}", guildName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-kick-member.member", "&7Member: &e{member}", "{member}", memberName)));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-kick-member.confirm-question", "&7Are you sure you want to kick this member?")));
        lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-kick-member.warning", "&cThis action cannot be undone!")));
        
        if (meta != null) {
            meta.setOwningPlayer(member.getOfflinePlayer());
            meta.setDisplayName(ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-kick-member.info-title", "&cConfirm Kick Member")));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        
        inventory.setItem(13, head);
    }
    
    /**
     * 设置按钮
     */
    private void setupButtons(Inventory inventory) {
        // 确认踢出按钮
        ItemStack confirm = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-kick-member.confirm-button", "&cConfirm Kick")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-kick-member.confirm-lore", "&7Click to confirm kicking member"))
        );
        inventory.setItem(11, confirm);
        
        // 取消按钮
        ItemStack cancel = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-kick-member.cancel-button", "&aCancel")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.confirm-kick-member.cancel-lore", "&7Click to cancel kicking member"))
        );
        inventory.setItem(15, cancel);
    }
    
    /**
     * 处理确认踢出
     */
    private void handleConfirmKick(Player player) {
        // 再次检查权限
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (executor == null || !executor.getRole().canKick()) {
                    String message = languageManager.getGuiMessage(player, "gui.common.no-permission", "&cInsufficient permission");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 检查成员是否仍在同一公会中
                if (member.getGuildId() != guild.getId()) {
                    String message = languageManager.getGuiMessage(player, "gui.confirm-kick-member.member-left", "&cThat member is no longer in the guild!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 不能踢出会长
                if (member.getRole() == GuildMember.Role.LEADER) {
                    String message = languageManager.getGuiMessage(player, "gui.confirm-kick-member.cannot-kick-leader", "&cCannot kick the guild leader!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 执行踢出操作
                plugin.getGuildService().removeGuildMemberAsync(member.getPlayerUuid(), player.getUniqueId()).thenAccept(success -> {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        if (success) {
                            String kickerMessage = languageManager.getGuiMessage(player, "gui.confirm-kick-member.kick.success", "&aSuccessfully kicked &e{member} &a!", "{member}", member.getPlayerName());
                            player.sendMessage(ColorUtils.colorize(kickerMessage));

                            // 通知被踢出的玩家
                            Player kickedPlayer = plugin.getServer().getPlayer(member.getPlayerUuid());
                            if (kickedPlayer != null) {
                                String kickedMessage = languageManager.getGuiMessage(kickedPlayer, "gui.confirm-kick-member.kick.kicked", "&cYou have been kicked from guild &e{guild} &c!", "{guild}", guild.getName());
                                kickedPlayer.sendMessage(ColorUtils.colorize(kickedMessage));
                            }

                            // 关闭GUI
                            player.closeInventory();
                        } else {
                            String message = languageManager.getGuiMessage(player, "gui.confirm-kick-member.kick.failed", "&cFailed to kick member!");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                    });
                });
            });
        });
    }
    
    /**
     * 处理取消
     */
    private void handleCancel(Player player) {
        player.closeInventory();
    }

    /**
     * 创建物品
     */
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
