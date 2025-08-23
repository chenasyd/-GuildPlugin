package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.models.GuildRelation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 关系管理GUI - 管理员专用
 */
public class RelationManagementGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Player player;
    private int currentPage = 0;
    private final int itemsPerPage = 28; // 7列 × 4行
    private List<GuildRelation> allRelations = new ArrayList<>();
    private boolean isLoading = false;
    
    // 确认删除机制
    private static final Map<UUID, GuildRelation> pendingDeletions = new HashMap<>();
    private static final Map<UUID, Long> deletionTimers = new HashMap<>();
    private static final long CONFIRMATION_TIMEOUT = 10000; // 10秒确认超时
    
    public RelationManagementGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        // 检查管理员权限
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ColorUtils.colorize("&c您没有管理员权限！"));
            return;
        }
        loadRelations();
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&4关系管理 - 管理员");
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 设置关系列表
        setupRelationList(inventory);
        
        // 设置分页按钮
        setupPaginationButtons(inventory);
        
        // 设置操作按钮
        setupActionButtons(inventory);
    }
    
    private void setupRelationList(Inventory inventory) {
        if (isLoading) {
            // 显示加载中
            ItemStack loadingItem = createItem(Material.SAND, ColorUtils.colorize("&e加载中..."), 
                ColorUtils.colorize("&7正在加载关系数据..."));
            inventory.setItem(22, loadingItem);
            return;
        }
        
        if (allRelations.isEmpty()) {
            // 显示无数据
            ItemStack emptyItem = createItem(Material.BARRIER, ColorUtils.colorize("&c暂无关系数据"), 
                ColorUtils.colorize("&7没有找到任何工会关系"));
            inventory.setItem(22, emptyItem);
            return;
        }
        
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allRelations.size());
        
        for (int i = 0; i < itemsPerPage; i++) {
            if (startIndex + i < endIndex) {
                GuildRelation relation = allRelations.get(startIndex + i);
                
                // 计算在2-8列，2-5行的位置 (slots 10-43)
                int row = (i / 7) + 1; // 2-5行
                int col = (i % 7) + 1; // 2-8列
                int slot = row * 9 + col;
                
                inventory.setItem(slot, createRelationItem(relation));
            }
        }
    }
    
    private ItemStack createRelationItem(GuildRelation relation) {
        Material material = getRelationMaterial(relation.getType());
        String status = getRelationStatus(relation.getStatus());
        
        // 检查是否在待删除状态
        boolean isPendingDeletion = pendingDeletions.containsKey(player.getUniqueId()) && 
                                  pendingDeletions.get(player.getUniqueId()).getId() == relation.getId();
        
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7关系类型: " + getRelationTypeName(relation.getType())));
        lore.add(ColorUtils.colorize("&7状态: " + status));
        lore.add(ColorUtils.colorize("&7工会1: " + relation.getGuild1Name()));
        lore.add(ColorUtils.colorize("&7工会2: " + relation.getGuild2Name()));
        lore.add(ColorUtils.colorize("&7发起人: " + relation.getInitiatorName()));
        lore.add(ColorUtils.colorize("&7创建时间: " + formatDateTime(relation.getCreatedAt())));
        lore.add("");
        
        if (isPendingDeletion) {
            lore.add(ColorUtils.colorize("&4⚠ 待确认删除"));
            lore.add(ColorUtils.colorize("&c左键: 确认删除"));
            lore.add(ColorUtils.colorize("&e右键: 取消删除"));
        } else {
            lore.add(ColorUtils.colorize("&c左键: 删除关系"));
            lore.add(ColorUtils.colorize("&e右键: 查看详情"));
        }
        
        String displayName = ColorUtils.colorize("&6" + relation.getGuild1Name() + " ↔ " + relation.getGuild2Name());
        if (isPendingDeletion) {
            displayName = ColorUtils.colorize("&4" + relation.getGuild1Name() + " ↔ " + relation.getGuild2Name());
        }
        
        return createItem(material, displayName, lore.toArray(new String[0]));
    }
    
    private Material getRelationMaterial(GuildRelation.RelationType type) {
        switch (type) {
            case ALLY: return Material.GREEN_WOOL;
            case ENEMY: return Material.RED_WOOL;
            case WAR: return Material.NETHERITE_SWORD;
            case TRUCE: return Material.YELLOW_WOOL;
            case NEUTRAL: return Material.GRAY_WOOL;
            default: return Material.STONE;
        }
    }
    
    private String getRelationTypeName(GuildRelation.RelationType type) {
        switch (type) {
            case ALLY: return "盟友";
            case ENEMY: return "敌对";
            case WAR: return "开战";
            case TRUCE: return "停战";
            case NEUTRAL: return "中立";
            default: return "未知";
        }
    }
    
    private String getRelationStatus(GuildRelation.RelationStatus status) {
        switch (status) {
            case PENDING: return "待处理";
            case ACTIVE: return "活跃";
            case EXPIRED: return "已过期";
            case CANCELLED: return "已取消";
            default: return "未知";
        }
    }
    
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "未知";
        return dateTime.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }
    
    private void setupPaginationButtons(Inventory inventory) {
        int totalPages = (int) Math.ceil((double) allRelations.size() / itemsPerPage);
        
        // 上一页按钮
        if (currentPage > 0) {
            inventory.setItem(45, createItem(Material.ARROW, ColorUtils.colorize("&a上一页"), 
                ColorUtils.colorize("&7第 " + (currentPage) + " 页")));
        }
        
        // 页码信息
        inventory.setItem(49, createItem(Material.PAPER, ColorUtils.colorize("&e第 " + (currentPage + 1) + " 页，共 " + totalPages + " 页"),
            ColorUtils.colorize("&7总计 " + allRelations.size() + " 个关系")));
        
        // 下一页按钮
        if (currentPage < totalPages - 1) {
            inventory.setItem(53, createItem(Material.ARROW, ColorUtils.colorize("&a下一页"), 
                ColorUtils.colorize("&7第 " + (currentPage + 2) + " 页")));
        }
    }
    
    private void setupActionButtons(Inventory inventory) {
        // 返回按钮
        inventory.setItem(46, createItem(Material.BARRIER, ColorUtils.colorize("&c返回"),
            ColorUtils.colorize("&7返回管理员菜单")));
        
        // 刷新按钮
        inventory.setItem(52, createItem(Material.EMERALD, ColorUtils.colorize("&a刷新列表"),
            ColorUtils.colorize("&7重新加载关系数据")));
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
    
    private void loadRelations() {
        if (isLoading) return; // 防止重复加载
        
        isLoading = true;
        
        // 获取所有工会的关系
        plugin.getGuildService().getAllGuildsAsync().thenCompose(guilds -> {
            List<CompletableFuture<List<GuildRelation>>> relationFutures = new ArrayList<>();
            
            for (Guild guild : guilds) {
                relationFutures.add(plugin.getGuildService().getGuildRelationsAsync(guild.getId()));
            }
            
            return CompletableFuture.allOf(relationFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<GuildRelation> allRelationsList = new ArrayList<>();
                    for (CompletableFuture<List<GuildRelation>> future : relationFutures) {
                        try {
                            allRelationsList.addAll(future.get());
                        } catch (Exception e) {
                            plugin.getLogger().warning("加载工会关系时发生错误: " + e.getMessage());
                        }
                    }
                    return allRelationsList;
                });
        }).thenAccept(relations -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                allRelations.clear();
                allRelations.addAll(relations);
                isLoading = false;
                
                if (player.isOnline()) {
                    // 使用安全的刷新方法
                    plugin.getGuiManager().refreshGUI(player);
                }
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                isLoading = false;
                if (player.isOnline()) {
                    player.sendMessage(ColorUtils.colorize("&c加载关系数据时发生错误: " + throwable.getMessage()));
                    plugin.getGuiManager().refreshGUI(player);
                }
            });
            return null;
        });
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 检查管理员权限
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ColorUtils.colorize("&c您没有管理员权限！"));
            return;
        }
        
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        if (slot == 46) {
            // 返回
            plugin.getGuiManager().openGUI(player, new AdminGuildGUI(plugin));
        } else if (slot == 52) {
            // 刷新
            if (!isLoading) {
                loadRelations();
                player.sendMessage(ColorUtils.colorize("&a正在刷新关系列表..."));
            }
        } else if (slot == 45 && currentPage > 0) {
            // 上一页
            currentPage--;
            plugin.getGuiManager().refreshGUI(player);
        } else if (slot == 53 && currentPage < (int) Math.ceil((double) allRelations.size() / itemsPerPage) - 1) {
            // 下一页
            currentPage++;
            plugin.getGuiManager().refreshGUI(player);
        } else if (slot >= 10 && slot <= 43) {
            // 关系项目 - 检查是否在2-8列，2-5行范围内
            int row = slot / 9;
            int col = slot % 9;
            if (row >= 1 && row <= 4 && col >= 1 && col <= 7) {
                int relativeIndex = (row - 1) * 7 + (col - 1);
                int relationIndex = (currentPage * itemsPerPage) + relativeIndex;
                if (relationIndex < allRelations.size()) {
                    GuildRelation relation = allRelations.get(relationIndex);
                    handleRelationClick(player, relation, clickType);
                }
            }
        }
    }
    
    private void handleRelationClick(Player player, GuildRelation relation, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            // 左键处理
            if (pendingDeletions.containsKey(player.getUniqueId()) && 
                pendingDeletions.get(player.getUniqueId()).getId() == relation.getId()) {
                // 确认删除
                confirmDeleteRelation(player, relation);
            } else {
                // 开始删除流程
                startDeleteRelation(player, relation);
            }
        } else if (clickType == ClickType.RIGHT) {
            // 右键处理
            if (pendingDeletions.containsKey(player.getUniqueId()) && 
                pendingDeletions.get(player.getUniqueId()).getId() == relation.getId()) {
                // 取消删除
                cancelDeleteRelation(player);
            } else {
                // 查看详情
                showRelationDetails(player, relation);
            }
        }
    }
    
    private void startDeleteRelation(Player player, GuildRelation relation) {
        // 设置待删除状态
        pendingDeletions.put(player.getUniqueId(), relation);
        deletionTimers.put(player.getUniqueId(), System.currentTimeMillis());
        
        player.sendMessage(ColorUtils.colorize("&c确定要删除关系: " + relation.getGuild1Name() + " ↔ " + relation.getGuild2Name() + " 吗？"));
        player.sendMessage(ColorUtils.colorize("&c左键: 确认删除 | 右键: 取消删除"));
        player.sendMessage(ColorUtils.colorize("&e10秒后自动取消"));
        
        // 刷新GUI显示待删除状态
        plugin.getGuiManager().refreshGUI(player);
        
        // 设置超时任务
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingDeletions.containsKey(player.getUniqueId()) && 
                pendingDeletions.get(player.getUniqueId()).getId() == relation.getId()) {
                cancelDeleteRelation(player);
            }
        }, 200L); // 10秒 = 200 ticks
    }
    
    private void confirmDeleteRelation(Player player, GuildRelation relation) {
        // 清除待删除状态
        pendingDeletions.remove(player.getUniqueId());
        deletionTimers.remove(player.getUniqueId());
        
        // 执行删除
        plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(ColorUtils.colorize("&a已删除关系: " + relation.getGuild1Name() + " ↔ " + relation.getGuild2Name()));
                    // 从列表中移除
                    allRelations.remove(relation);
                    // 刷新GUI
                    plugin.getGuiManager().refreshGUI(player);
                } else {
                    player.sendMessage(ColorUtils.colorize("&c删除关系失败！"));
                }
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(ColorUtils.colorize("&c删除关系时发生错误: " + throwable.getMessage()));
            });
            return null;
        });
    }
    
    private void cancelDeleteRelation(Player player) {
        GuildRelation relation = pendingDeletions.remove(player.getUniqueId());
        deletionTimers.remove(player.getUniqueId());
        
        if (relation != null) {
            player.sendMessage(ColorUtils.colorize("&e已取消删除关系: " + relation.getGuild1Name() + " ↔ " + relation.getGuild2Name()));
            // 刷新GUI
            plugin.getGuiManager().refreshGUI(player);
        }
    }
    
    private void showRelationDetails(Player player, GuildRelation relation) {
        player.sendMessage(ColorUtils.colorize("&6=== 关系详情 ==="));
        player.sendMessage(ColorUtils.colorize("&e关系类型: " + getRelationTypeName(relation.getType())));
        player.sendMessage(ColorUtils.colorize("&e状态: " + getRelationStatus(relation.getStatus())));
        player.sendMessage(ColorUtils.colorize("&e工会1: " + relation.getGuild1Name() + " (ID: " + relation.getGuild1Id() + ")"));
        player.sendMessage(ColorUtils.colorize("&e工会2: " + relation.getGuild2Name() + " (ID: " + relation.getGuild2Id() + ")"));
        player.sendMessage(ColorUtils.colorize("&e发起人: " + relation.getInitiatorName()));
        player.sendMessage(ColorUtils.colorize("&e创建时间: " + formatDateTime(relation.getCreatedAt())));
        if (relation.getUpdatedAt() != null) {
            player.sendMessage(ColorUtils.colorize("&e更新时间: " + formatDateTime(relation.getUpdatedAt())));
        }
        if (relation.getExpiresAt() != null) {
            player.sendMessage(ColorUtils.colorize("&e过期时间: " + formatDateTime(relation.getExpiresAt())));
        }
        player.sendMessage(ColorUtils.colorize("&6=================="));
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
    
    @Override
    public void onClose(Player player) {
        // 清理资源
        allRelations.clear();
        // 清除待删除状态
        pendingDeletions.remove(player.getUniqueId());
        deletionTimers.remove(player.getUniqueId());
    }
    
    @Override
    public void refresh(Player player) {
        // 使用GUIManager的安全刷新方法
        if (player.isOnline()) {
            plugin.getGuiManager().refreshGUI(player);
        }
    }
}
