package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 工会详情GUI
 */
public class GuildDetailGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player viewer;
    private List<GuildMember> members = new ArrayList<>();
    
    public GuildDetailGUI(GuildPlugin plugin, Guild guild, Player viewer) {
        this.plugin = plugin;
        this.guild = guild;
        this.viewer = viewer;
        loadMembers();
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6工会详情 - " + guild.getName());
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 设置工会基本信息
        setupGuildInfo(inventory);
        
        // 设置工会成员列表
        setupMembersList(inventory);
        
        // 设置操作按钮
        setupActionButtons(inventory);
    }
    
    private void setupGuildInfo(Inventory inventory) {
        // 工会名称和标签 - 放在顶部中央
        List<String> guildLore = new ArrayList<>();
        guildLore.add(ColorUtils.colorize("&7ID: " + guild.getId()));
        guildLore.add(ColorUtils.colorize("&7标签: [" + (guild.getTag() != null ? guild.getTag() : "无") + "]"));
        guildLore.add(ColorUtils.colorize("&7创建时间: " + formatTime(guild.getCreatedAt())));
        guildLore.add(ColorUtils.colorize("&7状态: " + (guild.isFrozen() ? "&c已冻结" : "&a正常")));
        
        inventory.setItem(4, createItem(Material.SHIELD, ColorUtils.colorize("&6" + guild.getName()), guildLore.toArray(new String[0])));
        
        // 工会等级和资金 - 放在第二行
        List<String> economyLore = new ArrayList<>();
        economyLore.add(ColorUtils.colorize("&7当前等级: &e" + guild.getLevel()));
        economyLore.add(ColorUtils.colorize("&7当前资金: &a" + plugin.getEconomyManager().format(guild.getBalance())));
        economyLore.add(ColorUtils.colorize("&7最大成员数: &e" + guild.getMaxMembers()));
        economyLore.add(ColorUtils.colorize("&7当前成员数: &e" + members.size()));
        
        inventory.setItem(19, createItem(Material.GOLD_INGOT, ColorUtils.colorize("&e经济信息"), economyLore.toArray(new String[0])));
        
        // 工会会长 - 放在第二行
        List<String> leaderLore = new ArrayList<>();
        leaderLore.add(ColorUtils.colorize("&7会长: &e" + guild.getLeaderName()));
        leaderLore.add(ColorUtils.colorize("&7UUID: &7" + guild.getLeaderUuid()));
        
        inventory.setItem(21, createItem(Material.GOLDEN_HELMET, ColorUtils.colorize("&6工会会长"), leaderLore.toArray(new String[0])));
        
        // 工会描述 - 放在第二行
        List<String> descLore = new ArrayList<>();
        String description = guild.getDescription();
        if (description != null && !description.isEmpty()) {
            descLore.add(ColorUtils.colorize("&7" + description));
        } else {
            descLore.add(ColorUtils.colorize("&7暂无描述"));
        }
        
        inventory.setItem(23, createItem(Material.BOOK, ColorUtils.colorize("&e工会描述"), descLore.toArray(new String[0])));
    }
    
    private void setupMembersList(Inventory inventory) {
        // 成员列表标题 - 放在第三行中央
        inventory.setItem(27, createItem(Material.PLAYER_HEAD, ColorUtils.colorize("&a工会成员"), 
            ColorUtils.colorize("&7共 " + members.size() + " 名成员")));
        
        // 显示前6个成员 - 在第三行和第四行
        int maxDisplay = Math.min(6, members.size());
        for (int i = 0; i < maxDisplay; i++) {
            GuildMember member = members.get(i);
            int slot = 28 + i;
            
            List<String> memberLore = new ArrayList<>();
            memberLore.add(ColorUtils.colorize("&7职位: " + getRoleDisplayName(member.getRole())));
            memberLore.add(ColorUtils.colorize("&7加入时间: " + formatTime(member.getJoinedAt())));
            memberLore.add(ColorUtils.colorize("&7在线状态: " + (isPlayerOnline(member.getPlayerUuid()) ? "&a在线" : "&7离线")));
            
            inventory.setItem(slot, createPlayerHead(member.getPlayerName(), memberLore.toArray(new String[0])));
        }
        
        // 如果成员超过6个，显示更多信息
        if (members.size() > 6) {
            inventory.setItem(34, createItem(Material.PAPER, ColorUtils.colorize("&e更多成员"), 
                ColorUtils.colorize("&7还有 " + (members.size() - 6) + " 名成员未显示")));
        }
    }
    
    private void setupActionButtons(Inventory inventory) {
        // 返回按钮 - 放在底部左侧
        inventory.setItem(45, createItem(Material.ARROW, ColorUtils.colorize("&c返回")));
        
        // 管理操作按钮 - 放在底部中央
        if (viewer.hasPermission("guild.admin")) {
            // 冻结/解冻按钮
            String freezeText = guild.isFrozen() ? "&a解冻工会" : "&c冻结工会";
            String freezeLore = guild.isFrozen() ? "&7点击解冻工会" : "&7点击冻结工会";
            inventory.setItem(47, createItem(Material.ICE, ColorUtils.colorize(freezeText), ColorUtils.colorize(freezeLore)));
            
            // 删除工会按钮
            inventory.setItem(49, createItem(Material.TNT, ColorUtils.colorize("&4删除工会"), 
                ColorUtils.colorize("&7点击删除工会")));
            
            // 资金管理按钮
            inventory.setItem(51, createItem(Material.GOLD_BLOCK, ColorUtils.colorize("&e资金管理"), 
                ColorUtils.colorize("&7管理工会资金")));
        }
        
        // 刷新按钮 - 放在底部右侧
        inventory.setItem(53, createItem(Material.EMERALD, ColorUtils.colorize("&a刷新信息")));
    }
    
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        
        // 填充边框
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    private void loadMembers() {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(membersList -> {
            this.members = membersList != null ? membersList : new ArrayList<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (viewer.isOnline()) {
                    refresh(viewer);
                }
            });
        });
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 45) {
            // 返回
            plugin.getGuiManager().openGUI(player, new GuildListManagementGUI(plugin, player));
        } else if (slot == 53) {
            // 刷新
            loadMembers();
        } else if (slot == 47 && player.hasPermission("guild.admin")) {
            // 冻结/解冻工会
            toggleGuildFreeze(player);
        } else if (slot == 49 && player.hasPermission("guild.admin")) {
            // 删除工会
            deleteGuild(player);
        } else if (slot == 51 && player.hasPermission("guild.admin")) {
            // 资金管理
            openEconomyManagement(player);
        }
    }
    
    private void toggleGuildFreeze(Player player) {
        boolean newStatus = !guild.isFrozen();
        plugin.getGuildService().updateGuildFrozenStatusAsync(guild.getId(), newStatus).thenAccept(success -> {
            if (success) {
                String message = newStatus ? "&a工会 " + guild.getName() + " 已被冻结！" : "&a工会 " + guild.getName() + " 已被解冻！";
                player.sendMessage(ColorUtils.colorize(message));
                // 更新本地guild对象
                guild.setFrozen(newStatus);
                refresh(player);
            } else {
                player.sendMessage(ColorUtils.colorize("&c操作失败！"));
            }
        });
    }
    
    private void deleteGuild(Player player) {
        // 确认删除
        player.sendMessage(ColorUtils.colorize("&c您确定要删除工会 " + guild.getName() + " 吗？"));
        player.sendMessage(ColorUtils.colorize("&c输入 &f/guildadmin delete " + guild.getName() + " confirm &c确认删除"));
        player.closeInventory();
    }
    
    private void openEconomyManagement(Player player) {
        // 打开资金管理GUI
        plugin.getGuiManager().openGUI(player, new EconomyManagementGUI(plugin, player));
    }
    
    private String formatTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "未知";
        return dateTime.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }
    
    private String getRoleDisplayName(GuildMember.Role role) {
        switch (role) {
            case LEADER: return "&6会长";
            case OFFICER: return "&e官员";
            case MEMBER: return "&7成员";
            default: return "&7未知";
        }
    }
    
    private boolean isPlayerOnline(java.util.UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createPlayerHead(String playerName, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&e" + playerName));
            
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            
            // 尝试设置玩家头颅
            try {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
            } catch (Exception e) {
                // 如果设置失败，使用默认头颅
            }
            
            head.setItemMeta(meta);
        }
        
        return head;
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
