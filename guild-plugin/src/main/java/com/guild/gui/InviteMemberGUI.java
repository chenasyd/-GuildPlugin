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
    
    // 玩家槽位布局常量
    private static final int ROWS = 4;           // 4行
    private static final int COLS = 7;           // 7列
    private static final int ITEMS_PER_PAGE = ROWS * COLS; // 28个玩家每页
    private static final int START_ROW = 1;      // 从第2行开始（索引1）
    private static final int START_COL = 1;      // 从第2列开始（索引1）

    public InviteMemberGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        // 获取在线玩家列表，排除会长自己
        this.onlinePlayers = Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.getUniqueId().equals(guild.getLeaderUuid()))
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String getTitle() {
        String defaultTitle = "&6邀请成员 - 第" + (currentPage + 1) + "页";
        return ColorUtils.colorize(languageManager.getMessage(player, "invite-member.title",
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
        // 检查是否是玩家头像槽位
        if (isPlayerSlot(slot)) {
            int playerIndex = getPlayerIndexFromSlot(slot);
            if (playerIndex >= 0 && playerIndex < onlinePlayers.size()) {
                Player targetPlayer = onlinePlayers.get(playerIndex);
                handleInvitePlayer(player, targetPlayer);
            }
        } else if (slot == 45) {
            // 上一页
            if (currentPage > 0) {
                currentPage--;
                refresh(player);
            }
        } else if (slot == 53) {
            // 下一页
            int maxPage = getMaxPage();
            if (currentPage < maxPage) {
                currentPage++;
                refresh(player);
            }
        } else if (slot == 49) {
            // 返回
            plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
        }
    }
    
    /**
     * 检查是否是玩家槽位
     */
    private boolean isPlayerSlot(int slot) {
        // 玩家槽位范围：10-43
        if (slot < 10 || slot > 43) return false;
        int col = slot % 9;
        // 排除第1列（col == 0）和第9列（col == 8），因为那是边框
        return col != 0 && col != 8;
    }
    
    /**
     * 从槽位计算玩家索引
     */
    private int getPlayerIndexFromSlot(int slot) {
        if (!isPlayerSlot(slot)) return -1;
        
        int row = slot / 9;      // 行号：1-4（对应第2-5行）
        int col = slot % 9;      // 列号：1-7（对应第2-8列）
        
        // 转换为0-based坐标
        int rowIndex = row - START_ROW;   // 0-3
        int colIndex = col - START_COL;   // 0-6
        
        // 检查坐标范围
        if (rowIndex < 0 || rowIndex >= ROWS || colIndex < 0 || colIndex >= COLS) {
            return -1;
        }
        
        // 计算在当前页中的相对索引
        int relativeIndex = rowIndex * COLS + colIndex;
        
        // 返回全局索引
        return currentPage * ITEMS_PER_PAGE + relativeIndex;
    }
    
    /**
     * 填充边框
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        
        // 顶部和底部边框
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        
        // 左右边框
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * 显示在线玩家
     */
    private void displayOnlinePlayers(Inventory inventory) {
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, onlinePlayers.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int relativeIndex = i - startIndex;
            int row = START_ROW + (relativeIndex / COLS);     // 行：1-4
            int col = START_COL + (relativeIndex % COLS);     // 列：1-7
            int slot = row * 9 + col;                         // 计算实际槽位
            
            Player targetPlayer = onlinePlayers.get(i);
            ItemStack playerHead = createPlayerHead(targetPlayer);
            inventory.setItem(slot, playerHead);
        }
    }
    
    /**
     * 设置导航按钮
     */
    private void setupNavigationButtons(Inventory inventory) {
        int maxPage = getMaxPage();
        
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
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getMessage(player, "gui.next-page", "&e下一页")),
                ColorUtils.colorize(languageManager.getMessage(player, "gui.view-next", "&7点击查看下一页"))
            );
            inventory.setItem(53, nextPage);
        }

        // 页码显示
        if (maxPage > 0) {
            ItemStack pageInfo = createItem(
                Material.PAPER,
                ColorUtils.colorize("&e第 " + (currentPage + 1) + " / " + (maxPage + 1) + " 页"),
                ColorUtils.colorize("&7共 " + onlinePlayers.size() + " 名在线玩家")
            );
            inventory.setItem(49, pageInfo);
        }

        // 返回按钮（放在第48槽位，页码在第49）
        ItemStack back = createItem(
            Material.BARRIER,
            ColorUtils.colorize(languageManager.getMessage(player, "gui.back", "&c返回")),
            ColorUtils.colorize(languageManager.getMessage(player, "invite-member.back-to-settings", "&7返回工会设置"))
        );
        inventory.setItem(48, back);
    }
    
    /**
     * 获取最大页数
     */
    private int getMaxPage() {
        if (onlinePlayers.isEmpty()) return 0;
        return (onlinePlayers.size() - 1) / ITEMS_PER_PAGE;
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
                ColorUtils.colorize("&7" + languageManager.getMessage(this.player, "invite-member.click-invite", "点击邀请该玩家")),
                ColorUtils.colorize("&7" + languageManager.getMessage(this.player, "invite-member.join-guild", "加入工会"))
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
    
    @Override
    public void onClose(Player player) {
        // 关闭时的处理
    }
    
    @Override
    public void refresh(Player player) {
        if (player.isOnline()) {
            plugin.getGuiManager().refreshGUI(player);
        }
    }
}