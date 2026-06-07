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
 * 提升成员GUI
 */
public class PromoteMemberGUI implements GUI {

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final Guild guild;
    private final Player player;
    private int currentPage = 0;
    private List<GuildMember> members;

    public PromoteMemberGUI(GuildPlugin plugin, Guild guild, Player player) {
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
                .filter(member -> !member.getRole().equals(GuildMember.Role.OFFICER)) // 只显示可以提升的成员
                .collect(java.util.stream.Collectors.toList());
        });
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "promote-member.title",
                "&6提升成员 - 第" + (currentPage + 1) + "页", "{page}", String.valueOf(currentPage + 1)));
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
        // 检查是否是成员槽位
        int memberIndex = getIndexFromSlot(slot);
        if (memberIndex >= 0) {
            if (memberIndex < members.size()) {
                GuildMember member = members.get(memberIndex);
                handlePromoteMember(player, member);
            }
        } else if (slot == 45) {
            // 上一页
            if (currentPage > 0) {
                currentPage--;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 53) {
            // 下一页
            int maxPage = (members.size() - 1) / 28;
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
     * 显示成员列表
     */
    private void displayMembers(Inventory inventory) {
        int startIndex = currentPage * 28; // 每页最多28个成员（4行7列）
        int endIndex = Math.min(startIndex + 28, members.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            GuildMember member = members.get(i);
            int slot = getSlotForIndex(i - startIndex);
            
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
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.previous-page", "&e上一页")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.view-previous", "&7点击查看上一页"))
            );
            inventory.setItem(45, prevPage);
        }

        // 下一页按钮
        int maxPage = (members.size() - 1) / 28;
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
            ColorUtils.colorize(languageManager.getGuiMessage(player, "member-operation.back-to-settings", "&7返回工会设置"))
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
            meta.setOwningPlayer(member.getOfflinePlayer());
            meta.setDisplayName(ColorUtils.colorize("&6" + member.getPlayerName()));
            meta.setLore(Arrays.asList(
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "member-operation.current-position", "当前职位") + ": &e" + member.getRole().getDisplayName()),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "member-operation.join-time", "加入时间") + ": &e" + member.getJoinedAt()),
                ColorUtils.colorize("&6" + languageManager.getGuiMessage(player, "promote-member.click-promote", "点击提升为官员"))
            ));
            head.setItemMeta(meta);
        }

        return head;
    }
    
    /**
     * 处理提升成员
     */
    private void handlePromoteMember(Player promoter, GuildMember member) {
        // 检查权限
        if (!promoter.hasPermission("guild.promote")) {
            String message = languageManager.getGuiMessage(promoter, "gui.no-permission", "&c权限不足");
            promoter.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 提升成员
        plugin.getGuildService().updateMemberRoleAsync(member.getPlayerUuid(), GuildMember.Role.OFFICER, promoter.getUniqueId()).thenAccept(success -> {
            if (success) {
                String promoterMessage = languageManager.getGuiMessage(promoter, "promote.success", "&a已提升 &e{player} &a为官员！", "{player}", member.getPlayerName());
                promoter.sendMessage(ColorUtils.colorize(promoterMessage));

                // 通知被提升的玩家
                Player promotedPlayer = plugin.getServer().getPlayer(member.getPlayerUuid());
                if (promotedPlayer != null) {
                    String promotedMessage = languageManager.getGuiMessage(promotedPlayer, "promote.promoted", "&a你被提升为工会 &e{guild} &a的官员！", "{guild}", guild.getName());
                    promotedPlayer.sendMessage(ColorUtils.colorize(promotedMessage));
                }

                // 刷新GUI
                plugin.getGuiManager().openGUI(promoter, new PromoteMemberGUI(plugin, guild, promoter));
            } else {
                String message = languageManager.getGuiMessage(promoter, "promote.failed", "&c提升成员失败！");
                promoter.sendMessage(ColorUtils.colorize(message));
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
