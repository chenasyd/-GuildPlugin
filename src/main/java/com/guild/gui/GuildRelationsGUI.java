package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildRelation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 工会关系GUI - 管理工会关系
 */
public class GuildRelationsGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private int currentPage = 0;
    private final int itemsPerPage = 28; // 每页显示28个关系 (7列 × 4行)
    private List<GuildRelation> relations = new ArrayList<>();
    
    public GuildRelationsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations.title", "&6工会关系"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-relations.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 加载关系数据
        loadRelations().thenAccept(relationsList -> {
            this.relations = relationsList;
            
            // 确保在主线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, () -> {
                // 显示关系列表
                displayRelations(inventory);
                
                // 添加功能按钮
                addFunctionButtons(inventory);
                
                // 添加分页按钮
                addPaginationButtons(inventory);
            });
        });
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        // 返回按钮
        if (itemName.contains("返回")) {
            MainGuildGUI mainGUI = new MainGuildGUI(plugin);
            plugin.getGuiManager().openGUI(player, mainGUI);
            return;
        }
        
        // 创建关系按钮
        if (itemName.contains("创建关系")) {
            openCreateRelationGUI(player);
            return;
        }
        
        // 分页按钮
        if (itemName.contains("上一页")) {
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
            return;
        }
        
        if (itemName.contains("下一页")) {
            int maxPage = (relations.size() - 1) / itemsPerPage;
            if (currentPage < maxPage) {
                currentPage++;
                refreshInventory(player);
            }
            return;
        }
        
        // 关系项目点击 - 检查是否在2-8列，2-5行范围内
        if (slot >= 10 && slot <= 43) {
            int row = slot / 9;
            int col = slot % 9;
            if (row >= 1 && row <= 4 && col >= 1 && col <= 7) {
                int relativeIndex = (row - 1) * 7 + (col - 1);
                int relationIndex = (currentPage * itemsPerPage) + relativeIndex;
                if (relationIndex < relations.size()) {
                    GuildRelation relation = relations.get(relationIndex);
                    handleRelationClick(player, relation, clickType);
                }
            }
        }
    }
    
    /**
     * 加载工会关系数据
     */
    private CompletableFuture<List<GuildRelation>> loadRelations() {
        return plugin.getGuildService().getGuildRelationsAsync(guild.getId());
    }
    
    /**
     * 显示关系列表
     */
    private void displayRelations(Inventory inventory) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, relations.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            GuildRelation relation = relations.get(i);
            int relativeIndex = i - startIndex;
            
            // 计算在2-8列，2-5行的位置 (slots 10-43)
            int row = (relativeIndex / 7) + 1; // 2-5行
            int col = (relativeIndex % 7) + 1; // 2-8列
            int slot = row * 9 + col;
            
            ItemStack relationItem = createRelationItem(relation);
            inventory.setItem(slot, relationItem);
        }
    }
    
    /**
     * 创建关系显示物品
     */
    private ItemStack createRelationItem(GuildRelation relation) {
        String otherGuildName = relation.getOtherGuildName(guild.getId());
        GuildRelation.RelationType type = relation.getType();
        GuildRelation.RelationStatus status = relation.getStatus();
        
        Material material = getRelationMaterial(type);
        String color = type.getColor();
        String displayName = color + otherGuildName + " - " + type.getDisplayName();
        
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7关系类型: " + color + type.getDisplayName()));
        lore.add(ColorUtils.colorize("&7状态: " + getStatusColor(status) + status.getDisplayName()));
        lore.add(ColorUtils.colorize("&7发起人: " + relation.getInitiatorName()));
        lore.add(ColorUtils.colorize("&7创建时间: " + formatDateTime(relation.getCreatedAt())));
        
        if (relation.getExpiresAt() != null) {
            lore.add(ColorUtils.colorize("&7过期时间: " + formatDateTime(relation.getExpiresAt())));
        }
        
        lore.add("");
        
        // 根据关系类型和状态添加操作提示
        if (status == GuildRelation.RelationStatus.PENDING) {
            if (relation.getInitiatorUuid().equals(player.getUniqueId())) {
                lore.add(ColorUtils.colorize("&c右键: 取消关系"));
            } else {
                lore.add(ColorUtils.colorize("&a左键: 接受关系"));
                lore.add(ColorUtils.colorize("&c右键: 拒绝关系"));
            }
        } else if (status == GuildRelation.RelationStatus.ACTIVE) {
            if (type == GuildRelation.RelationType.TRUCE) {
                lore.add(ColorUtils.colorize("&e左键: 结束停战"));
            } else if (type == GuildRelation.RelationType.WAR) {
                lore.add(ColorUtils.colorize("&e左键: 提议停战"));
            } else {
                lore.add(ColorUtils.colorize("&c右键: 删除关系"));
            }
        }
        
        return createItem(material, displayName, lore.toArray(new String[0]));
    }
    
    /**
     * 获取关系类型对应的材料
     */
    private Material getRelationMaterial(GuildRelation.RelationType type) {
        switch (type) {
            case ALLY: return Material.GREEN_WOOL;
            case ENEMY: return Material.RED_WOOL;
            case WAR: return Material.NETHERITE_SWORD;
            case TRUCE: return Material.YELLOW_WOOL;
            case NEUTRAL: return Material.GRAY_WOOL;
            default: return Material.WHITE_WOOL;
        }
    }
    
    /**
     * 获取状态颜色
     */
    private String getStatusColor(GuildRelation.RelationStatus status) {
        switch (status) {
            case PENDING: return "&e";
            case ACTIVE: return "&a";
            case EXPIRED: return "&7";
            case CANCELLED: return "&c";
            default: return "&f";
        }
    }
    
    /**
     * 格式化日期时间
     */
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "未知";
        return dateTime.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }
    
    /**
     * 添加功能按钮
     */
    private void addFunctionButtons(Inventory inventory) {
        // 创建关系按钮
        ItemStack createRelation = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&a创建关系"),
            ColorUtils.colorize("&7创建新的工会关系"),
            ColorUtils.colorize("&7盟友、敌对、开战等")
        );
        inventory.setItem(45, createRelation);
        
        // 关系统计按钮
        ItemStack statistics = createItem(
            Material.BOOK,
            ColorUtils.colorize("&e关系统计"),
            ColorUtils.colorize("&7查看关系统计信息"),
            ColorUtils.colorize("&7盟友数、敌对数等")
        );
        inventory.setItem(47, statistics);
    }
    
    /**
     * 添加分页按钮
     */
    private void addPaginationButtons(Inventory inventory) {
        int maxPage = (relations.size() - 1) / itemsPerPage;
        
        // 上一页按钮
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&c上一页"),
                ColorUtils.colorize("&7查看上一页")
            );
            inventory.setItem(45, previousPage);
        }
        
        // 下一页按钮
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&a下一页"),
                ColorUtils.colorize("&7查看下一页")
            );
            inventory.setItem(53, nextPage);
        }
        
        // 返回按钮
        ItemStack backButton = createItem(
            Material.BARRIER,
            ColorUtils.colorize("&c返回"),
            ColorUtils.colorize("&7返回主菜单")
        );
        inventory.setItem(49, backButton);
        
        // 页码显示
        ItemStack pageInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize("&e第 " + (currentPage + 1) + " 页"),
            ColorUtils.colorize("&7共 " + (maxPage + 1) + " 页"),
            ColorUtils.colorize("&7总计 " + relations.size() + " 个关系")
        );
        inventory.setItem(47, pageInfo);
    }
    
    /**
     * 处理关系点击
     */
    private void handleRelationClick(Player player, GuildRelation relation, ClickType clickType) {
        GuildRelation.RelationStatus status = relation.getStatus();
        GuildRelation.RelationType type = relation.getType();
        
        if (status == GuildRelation.RelationStatus.PENDING) {
            if (relation.getInitiatorUuid().equals(player.getUniqueId())) {
                // 发起人取消关系
                if (clickType == ClickType.RIGHT) {
                    cancelRelation(player, relation);
                }
            } else {
                // 对方处理关系
                if (clickType == ClickType.LEFT) {
                    acceptRelation(player, relation);
                } else if (clickType == ClickType.RIGHT) {
                    rejectRelation(player, relation);
                }
            }
        } else if (status == GuildRelation.RelationStatus.ACTIVE) {
            if (type == GuildRelation.RelationType.TRUCE) {
                if (clickType == ClickType.LEFT) {
                    endTruce(player, relation);
                }
            } else if (type == GuildRelation.RelationType.WAR) {
                if (clickType == ClickType.LEFT) {
                    proposeTruce(player, relation);
                }
            } else {
                if (clickType == ClickType.RIGHT) {
                    deleteRelation(player, relation);
                }
            }
        }
    }
    
    /**
     * 接受关系
     */
    private void acceptRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.ACTIVE)
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.accept-success", "&a已接受与 {guild} 的关系！");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.accept-failed", "&c接受关系失败！");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
    }
    
    /**
     * 拒绝关系
     */
    private void rejectRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.CANCELLED)
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.reject-success", "&c已拒绝与 {guild} 的关系！");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.reject-failed", "&c拒绝关系失败！");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
    }
    
    /**
     * 取消关系
     */
    private void cancelRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.CANCELLED)
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.cancel-success", "&c已取消与 {guild} 的关系！");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.cancel-failed", "&c取消关系失败！");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
    }
    
    /**
     * 结束停战
     */
    private void endTruce(Player player, GuildRelation relation) {
        // 结束停战，改为中立关系
        GuildRelation newRelation = new GuildRelation(
            relation.getGuild1Id(), relation.getGuild2Id(),
            relation.getGuild1Name(), relation.getGuild2Name(),
            GuildRelation.RelationType.NEUTRAL, player.getUniqueId(), player.getName()
        );
        
        plugin.getGuildService().createGuildRelationAsync(
            newRelation.getGuild1Id(), newRelation.getGuild2Id(),
            newRelation.getGuild1Name(), newRelation.getGuild2Name(),
            newRelation.getType(), newRelation.getInitiatorUuid(), newRelation.getInitiatorName()
        ).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    // 删除旧的停战关系
                    plugin.getGuildService().deleteGuildRelationAsync(relation.getId());
                    
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-end", "&a与 {guild} 的停战已结束，关系转为中立！");
                    message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                    player.sendMessage(ColorUtils.colorize(message));
                    refreshInventory(player);
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-end-failed", "&c结束停战失败！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }
    
    /**
     * 提议停战
     */
    private void proposeTruce(Player player, GuildRelation relation) {
        // 创建停战提议
        GuildRelation truceRelation = new GuildRelation(
            relation.getGuild1Id(), relation.getGuild2Id(),
            relation.getGuild1Name(), relation.getGuild2Name(),
            GuildRelation.RelationType.TRUCE, player.getUniqueId(), player.getName()
        );
        
        plugin.getGuildService().createGuildRelationAsync(
            truceRelation.getGuild1Id(), truceRelation.getGuild2Id(),
            truceRelation.getGuild1Name(), truceRelation.getGuild2Name(),
            truceRelation.getType(), truceRelation.getInitiatorUuid(), truceRelation.getInitiatorName()
        ).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-proposed", "&e已向 {guild} 提议停战！");
                    message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                    player.sendMessage(ColorUtils.colorize(message));
                    refreshInventory(player);
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-propose-failed", "&c提议停战失败！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }
    
    /**
     * 删除关系
     */
    private void deleteRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().deleteGuildRelationAsync(relation.getId())
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.delete-success", "&a已删除与 {guild} 的关系！");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.delete-failed", "&c删除关系失败！");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
    }
    
    /**
     * 打开创建关系GUI
     */
    private void openCreateRelationGUI(Player player) {
        CreateRelationGUI createRelationGUI = new CreateRelationGUI(plugin, guild, player);
        plugin.getGuiManager().openGUI(player, createRelationGUI);
    }
    
    /**
     * 刷新库存
     */
    private void refreshInventory(Player player) {
        if (player.isOnline()) {
            plugin.getGuiManager().refreshGUI(player);
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
