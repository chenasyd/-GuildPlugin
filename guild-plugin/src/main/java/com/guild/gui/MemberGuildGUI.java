package com.guild.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ServerUtils;
import com.guild.core.language.LanguageManager;
import com.guild.models.Guild;
import com.guild.models.GuildMember;

/**
 * 普通成员工会GUI - 为普通成员提供有限的工会功能。
 * <p>
 * 包含功能：
 * <ul>
 *   <li>工会家传送</li>
 *   <li>离开工会</li>
 * </ul>
 * <p>
 * 该GUI与 {@link GuildSettingsGUI} 分离，避免普通成员看到会长专属的设置项，
 * 同时保持 {@code GuildSettingsGUI} 的槽位注册兼容性不受影响。
 */
public class MemberGuildGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;

    public MemberGuildGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.member-guild-gui.title",
                "&6我的工会 - {guild_name}", "{guild_name}",
                guild.getName() != null ? guild.getName() : "未知工会"));
    }

    @Override
    public int getSize() {
        return 27; // 3行，简洁布局
    }

    @Override
    public void setupInventory(Inventory inventory) {
        fillBorder(inventory);
        displayGuildInfo(inventory);
        setupButtons(inventory);
        fillEmptySlots(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // 工会家传送
                handleHomeTeleport(player);
                break;
            case 15: // 离开工会
                handleLeaveGuild(player);
                break;
        }
    }

    // ==================== UI 布局 ====================

    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        // 顶行和底行
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 18, border);
        }
        // 左右列
        for (int i = 9; i < 18; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }

    private void displayGuildInfo(Inventory inventory) {
        String name = guild.getName() != null ? guild.getName() :
                languageManager.getGuiMessage(player, "gui.member-guild-gui.no-name", "无名称");
        String tag = guild.getTag() != null ? "&7[" + guild.getTag() + "&7]" :
                languageManager.getGuiMessage(player, "gui.member-guild-gui.no-tag", "无标签");
        String homeStatus = guild.hasHome() ?
                languageManager.getGuiMessage(player, "gui.member-guild-gui.home-set", "&a已设置") :
                languageManager.getGuiMessage(player, "gui.member-guild-gui.home-not-set", "&c未设置");

        ItemStack info = createItem(Material.PAPER,
                languageManager.getGuiMessage(player, "gui.member-guild-gui.info-title", "&6工会信息"),
                languageManager.getGuiMessage(player, "gui.member-guild-gui.info-name", "&7名称: &e{name}", "{name}", name),
                languageManager.getGuiMessage(player, "gui.member-guild-gui.info-tag", "&7标签: &e{tag}", "{tag}", tag),
                languageManager.getGuiMessage(player, "gui.member-guild-gui.info-home", "&7工会家: {status}", "{status}", homeStatus));
        inventory.setItem(13, info);
    }

    private void setupButtons(Inventory inventory) {
        // 工会家传送
        ItemStack homeTeleport = createItem(Material.ENDER_PEARL,
                languageManager.getGuiMessage(player, "gui.member-guild-gui.home-teleport", "&d工会家传送"),
                languageManager.getGuiMessage(player, "gui.member-guild-gui.home-teleport-desc", "&7单击 &f传送到工会家"));
        inventory.setItem(11, homeTeleport);

        // 离开工会
        ItemStack leaveGuild = createItem(Material.BARRIER,
                languageManager.getGuiMessage(player, "gui.member-guild-gui.leave", "&c离开工会"),
                languageManager.getGuiMessage(player, "gui.member-guild-gui.leave-desc", "&7单击 &f离开当前工会"));
        inventory.setItem(15, leaveGuild);
    }

    private void fillEmptySlots(Inventory inventory) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 9; slot <= 17; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) continue;
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }

    // ==================== 业务逻辑 ====================

    private void handleHomeTeleport(Player player) {
        if (ServerUtils.isFolia()) {
            String message = languageManager.getGuiMessage(player, "gui.guild-settings.home.folia-disabled", "&c传送功能在Folia环境下暂不可用！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String msg = languageManager.getGuiMessage(player, "gui.common.no-permission", "&c权限不足");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuildService().getGuildHomeAsync(guild.getId()).thenAccept(location -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (location != null) {
                    player.teleport(location);
                    String message = languageManager.getGuiMessage(player, "gui.guild-settings.home.success", "&a已传送到工会家！");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = languageManager.getGuiMessage(player, "gui.guild-settings.home.not-set", "&c工会家未设置！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    private void handleLeaveGuild(Player player) {
        plugin.getGuiManager().openGUI(player,
                new ConfirmLeaveGuildGUI(plugin, guild, player, "MemberGuildGUI"));
    }

    // ==================== 工具方法 ====================

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ColorUtils.colorize(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
