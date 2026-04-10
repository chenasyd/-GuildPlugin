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
        return ColorUtils.colorize(languageManager.getMessage(player, "promote-member.title",
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
        if (slot >= 9 && slot < 45) {
            // 成员头像区域
            int memberIndex = slot - 9 + (currentPage * 36);
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
            meta.setDisplayName(ColorUtils.colorize("&6" + member.getPlayerName()));
            meta.setLore(Arrays.asList(
                ColorUtils.colorize("&7" + languageManager.getMessage(player, "member-operation.current-position", "当前职位") + ": &e" + member.getRole().getDisplayName()),
                ColorUtils.colorize("&7" + languageManager.getMessage(player, "member-operation.join-time", "加入时间") + ": &e" + member.getJoinedAt()),
                ColorUtils.colorize("&6" + languageManager.getMessage(player, "promote-member.click-promote", "点击提升为官员"))
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
            String message = languageManager.getMessage(promoter, "gui.no-permission", "&c权限不足");
            promoter.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 提升成员
        plugin.getGuildService().updateMemberRoleAsync(member.getPlayerUuid(), GuildMember.Role.OFFICER, promoter.getUniqueId()).thenAccept(success -> {
            if (success) {
                String promoterMessage = languageManager.getMessage(promoter, "promote.success", "&a已提升 &e{player} &a为官员！", "{player}", member.getPlayerName());
                promoter.sendMessage(ColorUtils.colorize(promoterMessage));

                // 通知被提升的玩家
                Player promotedPlayer = plugin.getServer().getPlayer(member.getPlayerUuid());
                if (promotedPlayer != null) {
                    String promotedMessage = languageManager.getMessage(promotedPlayer, "promote.promoted", "&a你被提升为工会 &e{guild} &a的官员！", "{guild}", guild.getName());
                    promotedPlayer.sendMessage(ColorUtils.colorize(promotedMessage));
                }

                // 刷新GUI
                plugin.getGuiManager().openGUI(promoter, new PromoteMemberGUI(plugin, guild, promoter));
            } else {
                String message = languageManager.getMessage(promoter, "promote.failed", "&c提升成员失败！");
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
