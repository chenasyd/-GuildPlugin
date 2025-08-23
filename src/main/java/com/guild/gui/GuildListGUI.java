package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
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
 * 工会列表GUI
 */
public class GuildListGUI implements GUI {
    
    private final GuildPlugin plugin;
    private int currentPage = 0;
    private static final int GUILDS_PER_PAGE = 28; // 4行7列，除去边框
    private String searchQuery = "";
    private String filterType = "all"; // all, name, tag
    
    public GuildListGUI(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    public GuildListGUI(GuildPlugin plugin, String searchQuery, String filterType) {
        this.plugin = plugin;
        this.searchQuery = searchQuery;
        this.filterType = filterType;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.title", "&6工会列表"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-list.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 添加功能按钮
        setupFunctionButtons(inventory);
        
        // 加载工会列表
        loadGuilds(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 检查是否是功能按钮
        if (isFunctionButton(slot)) {
            handleFunctionButton(player, slot);
            return;
        }
        
        // 检查是否是分页按钮
        if (isPaginationButton(slot)) {
            handlePaginationButton(player, slot);
            return;
        }
        
        // 检查是否是工会按钮
        if (isGuildSlot(slot)) {
            handleGuildClick(player, slot, clickedItem, clickType);
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
     * 设置功能按钮
     */
    private void setupFunctionButtons(Inventory inventory) {
        // 搜索按钮
        ItemStack search = createItem(
            Material.COMPASS,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.search.name", "&e搜索工会")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.search.lore.1", "&7搜索特定工会")),
            ColorUtils.colorize("&7当前搜索: " + (searchQuery.isEmpty() ? "无" : searchQuery))
        );
        inventory.setItem(45, search);
        
        // 筛选按钮
        ItemStack filter = createItem(
            Material.HOPPER,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.filter.name", "&e筛选")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.filter.lore.1", "&7按条件筛选工会")),
            ColorUtils.colorize("&7当前筛选: " + getFilterDisplayName())
        );
        inventory.setItem(47, filter);
        
        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.back.name", "&7返回")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.back.lore.1", "&7返回主菜单"))
        );
        inventory.setItem(49, back);
    }
    
    /**
     * 加载工会列表
     */
    private void loadGuilds(Inventory inventory) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            // 确保在主线程中更新GUI
            CompatibleScheduler.runTask(plugin, () -> {
                if (guilds == null || guilds.isEmpty()) {
                    // 显示无工会信息
                    ItemStack noGuilds = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize("&c暂无工会"),
                        ColorUtils.colorize("&7服务器中还没有工会")
                    );
                    inventory.setItem(22, noGuilds);
                    return;
                }
                
                // 应用搜索和筛选
                List<Guild> filteredGuilds = filterGuilds(guilds);
                
                if (filteredGuilds.isEmpty()) {
                    // 显示无搜索结果
                    ItemStack noResults = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize("&c无搜索结果"),
                        ColorUtils.colorize("&7没有找到匹配的工会")
                    );
                    inventory.setItem(22, noResults);
                    return;
                }
                
                // 计算分页
                int totalPages = (filteredGuilds.size() - 1) / GUILDS_PER_PAGE;
                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }
                
                // 设置分页按钮
                setupPaginationButtons(inventory, totalPages);
                
                // 显示当前页的工会
                displayGuilds(inventory, filteredGuilds);
            });
        });
    }
    
    /**
     * 筛选工会
     */
    private List<Guild> filterGuilds(List<Guild> guilds) {
        List<Guild> filtered = new ArrayList<>();
        
        for (Guild guild : guilds) {
            boolean matches = true;
            
            // 应用搜索
            if (!searchQuery.isEmpty()) {
                switch (filterType) {
                    case "name":
                        matches = guild.getName().toLowerCase().contains(searchQuery.toLowerCase());
                        break;
                    case "tag":
                        if (guild.getTag() != null) {
                            matches = guild.getTag().toLowerCase().contains(searchQuery.toLowerCase());
                        } else {
                            matches = false;
                        }
                        break;
                    default: // all
                        matches = guild.getName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                (guild.getTag() != null && guild.getTag().toLowerCase().contains(searchQuery.toLowerCase()));
                        break;
                }
            }
            
            if (matches) {
                filtered.add(guild);
            }
        }
        
        return filtered;
    }
    
    /**
     * 显示工会列表
     */
    private void displayGuilds(Inventory inventory, List<Guild> guilds) {
        int startIndex = currentPage * GUILDS_PER_PAGE;
        int endIndex = Math.min(startIndex + GUILDS_PER_PAGE, guilds.size());
        
        // 创建所有工会的异步任务
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        int slotIndex = 10; // 从第2行第2列开始
        for (int i = startIndex; i < endIndex; i++) {
            Guild guild = guilds.get(i);
            if (slotIndex >= 44) break; // 避免超出显示区域
            
            final int finalSlotIndex = slotIndex;
            
            // 异步获取成员数量并创建物品
            CompletableFuture<Void> future = plugin.getGuildService().getGuildMemberCountAsync(guild.getId())
                .thenAccept(memberCount -> {
                    // 在主线程中更新GUI
                    CompatibleScheduler.runTask(plugin, () -> {
                        ItemStack guildItem = createGuildItemWithMemberCount(guild, memberCount);
                        inventory.setItem(finalSlotIndex, guildItem);
                    });
                });
            
            futures.add(future);
            
            slotIndex++;
            if (slotIndex % 9 == 8) { // 跳过边框
                slotIndex += 2;
            }
        }
        
        // 等待所有异步任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    /**
     * 设置分页按钮
     */
    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        // 上一页按钮
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.previous-page.name", "&c上一页")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.previous-page.lore.1", "&7查看上一页"))
            );
            inventory.setItem(18, previousPage);
        }
        
        // 下一页按钮
        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.next-page.name", "&a下一页")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.next-page.lore.1", "&7查看下一页"))
            );
            inventory.setItem(26, nextPage);
        }
    }
    
    /**
     * 创建工会物品（带成员数量）
     */
    private ItemStack createGuildItemWithMemberCount(Guild guild, int memberCount) {
        List<String> lore = new ArrayList<>();
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7标签: {guild_tag}", guild, null));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7会长: {leader_name}", guild, null));
        lore.add(ColorUtils.colorize("&7成员: " + memberCount));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7创建时间: {guild_created_time}", guild, null));
        lore.add("");
        lore.add(ColorUtils.colorize("&a左键: 查看详情"));
        lore.add(ColorUtils.colorize("&e右键: 申请加入"));
        
        return createItem(
            Material.SHIELD,
            PlaceholderUtils.replaceGuildPlaceholders("&e{guild_name}", guild, null),
            lore.toArray(new String[0])
        );
    }
    
    /**
     * 创建工会物品（原始方法，用于兼容性）
     */
    private ItemStack createGuildItem(Guild guild) {
        return createGuildItemWithMemberCount(guild, 0); // 使用默认值
    }
    
    /**
     * 获取筛选显示名称
     */
    private String getFilterDisplayName() {
        switch (filterType) {
            case "name":
                return "按名称";
            case "tag":
                return "按标签";
            default:
                return "全部";
        }
    }
    
    /**
     * 检查是否是功能按钮
     */
    private boolean isFunctionButton(int slot) {
        return slot == 45 || slot == 47 || slot == 49;
    }
    
    /**
     * 检查是否是分页按钮
     */
    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }
    
    /**
     * 检查是否是工会槽位
     */
    private boolean isGuildSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8;
    }
    
    /**
     * 处理功能按钮点击
     */
    private void handleFunctionButton(Player player, int slot) {
        switch (slot) {
            case 45: // 搜索
                handleSearch(player);
                break;
            case 47: // 筛选
                handleFilter(player);
                break;
            case 49: // 返回
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                break;
        }
    }
    
    /**
     * 处理分页按钮点击
     */
    private void handlePaginationButton(Player player, int slot) {
        if (slot == 18) { // 上一页
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
        } else if (slot == 26) { // 下一页
            currentPage++;
            refreshInventory(player);
        }
    }
    
    /**
     * 处理工会点击
     */
    private void handleGuildClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            // 查看详情
            handleViewGuildDetails(player, slot);
        } else if (clickType == ClickType.RIGHT) {
            // 申请加入
            handleApplyToGuild(player, slot);
        }
    }
    
    /**
     * 处理搜索
     */
    private void handleSearch(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.search-dev", "&a搜索功能正在开发中...");
        player.sendMessage(ColorUtils.colorize(message));
    }
    
    /**
     * 处理筛选
     */
    private void handleFilter(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.filter-dev", "&a筛选功能正在开发中...");
        player.sendMessage(ColorUtils.colorize(message));
    }
    
    /**
     * 处理查看工会详情
     */
    private void handleViewGuildDetails(Player player, int slot) {
        // 获取当前页的工会列表
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            // 确保在主线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, () -> {
                if (guilds == null || guilds.isEmpty()) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guilds", "&c没有找到工会");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 计算工会在列表中的索引
                int guildIndex = currentPage * GUILDS_PER_PAGE + (slot - 10);
                if (guildIndex >= 0 && guildIndex < guilds.size()) {
                    Guild guild = guilds.get(guildIndex);
                    
                    // 打开工会信息GUI
                    GuildInfoGUI guildInfoGUI = new GuildInfoGUI(plugin, player, guild);
                    plugin.getGuiManager().openGUI(player, guildInfoGUI);
                }
            });
        });
    }
    
    /**
     * 处理申请加入工会
     */
    private void handleApplyToGuild(Player player, int slot) {
        // 检查玩家是否已有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(playerGuild -> {
            // 确保在主线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, () -> {
                if (playerGuild != null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("create.already-in-guild", "&c您已经在一个工会中了！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 获取当前页的工会列表
                plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                    // 确保在主线程中执行GUI操作
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (guilds == null || guilds.isEmpty()) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guilds", "&c没有找到工会");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        
                        // 计算工会在列表中的索引
                        int guildIndex = currentPage * GUILDS_PER_PAGE + (slot - 10);
                        if (guildIndex >= 0 && guildIndex < guilds.size()) {
                            Guild guild = guilds.get(guildIndex);
                            
                            // 检查是否已有待处理申请
                            plugin.getGuildService().hasPendingApplicationAsync(player.getUniqueId(), guild.getId()).thenAccept(hasPending -> {
                                // 确保在主线程中执行GUI操作
                                CompatibleScheduler.runTask(plugin, () -> {
                                    if (hasPending) {
                                        String message = plugin.getConfigManager().getMessagesConfig().getString("apply.already-applied", "&c您已经申请过这个工会了！");
                                        player.sendMessage(ColorUtils.colorize(message));
                                        return;
                                    }
                                    
                                    // 提交申请
                                    plugin.getGuildService().submitApplicationAsync(guild.getId(), player.getUniqueId(), player.getName(), "").thenAccept(success -> {
                                        // 确保在主线程中执行GUI操作
                                        CompatibleScheduler.runTask(plugin, () -> {
                                            if (success) {
                                                String message = plugin.getConfigManager().getMessagesConfig().getString("apply.success", "&a申请已提交！");
                                                player.sendMessage(ColorUtils.colorize(message));
                                            } else {
                                                String message = plugin.getConfigManager().getMessagesConfig().getString("apply.failed", "&c申请提交失败！");
                                                player.sendMessage(ColorUtils.colorize(message));
                                            }
                                        });
                                    });
                                });
                            });
                        }
                    });
                });
            });
        });
    }
    
    /**
     * 刷新库存
     */
    private void refreshInventory(Player player) {
        plugin.getGuiManager().refreshGUI(player);
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
