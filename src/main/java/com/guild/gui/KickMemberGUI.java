package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 踢出成员GUI
 */
public class KickMemberGUI implements GUI {

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final Guild guild;
    private final Player player;
    private int currentPage = 0;
    private List<GuildMember> members;

    public KickMemberGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.player = player;
        // 初始化时获取成员列表
        this.members = List.of();
        loadMembers();
    }

    private void loadMembers() {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(memberList -> {
            this.members = memberList.stream()
                .filter(member -> !member.getPlayerUuid().equals(guild.getLeaderUuid()))
                .collect(java.util.stream.Collectors.toList());
        });
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getMessage(player, "kick-member.title",
                "&6踢出成员 - 第" + (currentPage + 1) + "页", "{page}", String.valueOf(currentPage + 1)));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 显示成员列表
        displayMembers(inventory);
        
        // 添加导航按钮
        setupNavigationButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot >= 9 && slot < 45) {
            // 成员头像区域
            int memberIndex = slot - 9 + (currentPage * 36);
            if (memberIndex < members.size()) {
                GuildMember member = members.get(memberIndex);
                handleKickMember(player, member);
            }
        } else if (slot == 45) {
            // 上一页
            if (currentPage > 0) {
                currentPage--;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 53) {
            // 下一页
            int maxPage = (members.size() - 1) / 36;
            if (currentPage < maxPage) {
                currentPage++;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 49) {
            // 返回
            plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
        }
    }
    
    /**
     * 填充边框
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * 显示成员列表
     */
    private void displayMembers(Inventory inventory) {
        int startIndex = currentPage * 36;
        int endIndex = Math.min(startIndex + 36, members.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            GuildMember member = members.get(i);
            int slot = 9 + (i - startIndex);
            
            ItemStack memberHead = createMemberHead(member);
            inventory.setItem(slot, memberHead);
        }
    }
    
    /**
     * 设置导航按钮
     */
    private void setupNavigationButtons(Inventory inventory) {
        // 上一页按钮
        if (currentPage > 0) {
            ItemStack prevPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getMessage(player, "gui.previous-page", "&e上一页")),
                ColorUtils.colorize(languageManager.getMessage(player, "gui.view-previous", "&7点击查看上一页"))
            );
            inventory.setItem(45, prevPage);
        }

        // 下一页按钮
        int maxPage = (members.size() - 1) / 36;
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getMessage(player, "gui.next-page", "&e下一页")),
                ColorUtils.colorize(languageManager.getMessage(player, "gui.view-next", "&7点击查看下一页"))
            );
            inventory.setItem(53, nextPage);
        }

        // 返回按钮
        ItemStack back = createItem(
            Material.BARRIER,
            ColorUtils.colorize(languageManager.getMessage(player, "gui.back", "&c返回")),
            ColorUtils.colorize(languageManager.getMessage(player, "member-operation.back-to-settings", "&7返回工会设置"))
        );
        inventory.setItem(49, back);
    }

    /**
     * 创建成员头像
     */
    private ItemStack createMemberHead(GuildMember member) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&c" + member.getPlayerName()));
            meta.setLore(Arrays.asList(
                ColorUtils.colorize("&7" + languageManager.getMessage(player, "member-operation.position", "职位") + ": &e" + member.getRole().getDisplayName()),
                ColorUtils.colorize("&7" + languageManager.getMessage(player, "member-operation.join-time", "加入时间") + ": &e" + member.getJoinedAt()),
                ColorUtils.colorize("&c" + languageManager.getMessage(player, "kick-member.click-kick", "点击踢出该成员"))
            ));
            head.setItemMeta(meta);
        }

        return head;
    }
    
    /**
     * 处理踢出成员
     */
    private void handleKickMember(Player kicker, GuildMember member) {
        // 检查权限
        if (!kicker.hasPermission("guild.kick")) {
            String message = languageManager.getMessage(kicker, "gui.no-permission", "&c权限不足");
            kicker.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 踢出成员
        plugin.getGuildService().removeGuildMemberAsync(member.getPlayerUuid(), kicker.getUniqueId()).thenAccept(success -> {
            if (success) {
                String kickerMessage = languageManager.getMessage(kicker, "kick.success", "&a已踢出成员 &e{player} &a！", "{player}", member.getPlayerName());
                kicker.sendMessage(ColorUtils.colorize(kickerMessage));

                // 通知被踢出的玩家
                Player kickedPlayer = plugin.getServer().getPlayer(member.getPlayerUuid());
                if (kickedPlayer != null) {
                    String kickedMessage = languageManager.getMessage(kickedPlayer, "kick.kicked", "&c你被踢出了工会 &e{guild} &c！", "{guild}", guild.getName());
                    kickedPlayer.sendMessage(ColorUtils.colorize(kickedMessage));
                }

                // 刷新GUI
                plugin.getGuiManager().openGUI(kicker, new KickMemberGUI(plugin, guild, kicker));
            } else {
                String message = languageManager.getMessage(kicker, "kick.failed", "&c踢出成员失败！");
                kicker.sendMessage(ColorUtils.colorize(message));
            }
        });
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
