package com.guild.gui;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildInvitation;
import com.guild.util.InviteMessageUtils;
import com.guild.util.NotifyUtils;

/**
 * 邀请成员GUI
 */
public class InviteMemberGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;
    private int currentPage = 0;
    private List<Player> onlinePlayers;

    public InviteMemberGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.onlinePlayers = Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.getUniqueId().equals(guild.getLeaderUuid()))
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String getTitle() {
        String defaultTitle = "&6邀请成员 - 第" + (currentPage + 1) + "页";
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "invite-member.title",
                defaultTitle, "{page}", String.valueOf(currentPage + 1), "{guild}", guild.getName()));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 显示在线玩家
        displayOnlinePlayers(inventory);
        
        // 添加导航按钮
        setupNavigationButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 检查是否是玩家槽位
        int playerIndex = getIndexFromSlot(slot);
        if (playerIndex >= 0) {
            if (playerIndex < onlinePlayers.size()) {
                Player targetPlayer = onlinePlayers.get(playerIndex);
                handleInvitePlayer(player, targetPlayer);
            }
        } else if (slot == 45) {
            // 上一页
            if (currentPage > 0) {
                currentPage--;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 53) {
            // 下一页
            int maxPage = (onlinePlayers.size() - 1) / 28;
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
     * 槽位计算方法
     */
    
    /**
     * 从页内索引获取inventory槽位
     * @param index 页内索引 (0-27)
     * @return inventory槽位 (10-16, 19-25, 28-34, 37-43)
     */
    private int getSlotForIndex(int index) {
        int row = index / 7;      // 行号 (0-3)
        int col = index % 7;      // 列号 (0-6)
        return (row + 1) * 9 + col + 1; // 转换为inventory槽位
    }
    
    /**
     * 从inventory槽位获取页内索引
     * @param slot inventory槽位
     * @return 页内索引 (0-27)，或 -1 表示无效槽位
     */
    private int getIndexFromSlot(int slot) {
        int row = slot / 9;      // 行号 (1-4)
        int col = slot % 9;      // 列号 (0-8)
        
        // 检查是否在有效范围内
        if (row < 1 || row > 4 || col < 1 || col > 7) {
            return -1;
        }
        
        // 计算页内索引
        int pageIndex = (row - 1) * 7 + (col - 1);
        return currentPage * 28 + pageIndex;
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
     * 显示在线玩家
     */
    private void displayOnlinePlayers(Inventory inventory) {
        int startIndex = currentPage * 28; // 每页最多28个玩家（4行7列）
        int endIndex = Math.min(startIndex + 28, onlinePlayers.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Player targetPlayer = onlinePlayers.get(i);
            int slot = getSlotForIndex(i - startIndex);
            
            ItemStack playerHead = createPlayerHead(targetPlayer);
            inventory.setItem(slot, playerHead);
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
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.previous-page", "&e上一页")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.view-previous", "&7点击查看上一页"))
            );
            inventory.setItem(45, prevPage);
        }

        // 下一页按钮
        int maxPage = (onlinePlayers.size() - 1) / 28;
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.next-page", "&e下一页")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.view-next", "&7点击查看下一页"))
            );
            inventory.setItem(53, nextPage);
        }

        // 返回按钮
        ItemStack back = createItem(
            Material.BARRIER,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.back", "&c返回")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "invite-member.back-to-settings", "&7返回工会设置"))
        );
        inventory.setItem(49, back);
    }
    
    /**
     * 创建玩家头像
     */
    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ColorUtils.colorize("&a" + player.getName()));
            meta.setLore(Arrays.asList(
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(this.player, "invite-member.click-invite", "点击邀请该玩家")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(this.player, "invite-member.join-guild", "加入工会"))
            ));
            head.setItemMeta(meta);
        }

        return head;
    }
    
    /**
     * 处理邀请玩家
     */
    private void handleInvitePlayer(Player inviter, Player target) {
        // 检查目标玩家是否已经在工会中
        plugin.getGuildService().getGuildMemberAsync(target.getUniqueId()).thenAccept(member -> {
            if (member != null) {
                CompatibleScheduler.runTask(plugin, () -> inviter.sendMessage(InviteMessageUtils.formatAlreadyInGuild(plugin, inviter, target.getName())));
                return;
            }

            // 发送邀请
            plugin.getGuildService().sendInvitationAsync(guild.getId(), inviter.getUniqueId(), inviter.getName(), target.getUniqueId(), target.getName())
                .thenAccept(success -> {
                    // 确保在主线程发送消息
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (success) {
                            inviter.sendMessage(InviteMessageUtils.formatInviteSent(plugin, inviter, target));

                            // 创建邀请对象用于发送带点击事件的通知
                            GuildInvitation invitation = new GuildInvitation(guild.getId(), inviter.getUniqueId(), 
                                inviter.getName(), target.getUniqueId(), target.getName());
                            
                            // 给被邀请者发送带点击事件的邀请通知
                            NotifyUtils.sendInviteWithClickableAction(plugin, target, inviter, guild, invitation);
                        } else {
                            inviter.sendMessage(InviteMessageUtils.formatInviteFailed(plugin, inviter));
                        }
                    });
                });
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
