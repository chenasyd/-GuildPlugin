package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildApplication;
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
 * 申请管理GUI
 */
public class ApplicationManagementGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private static final int APPLICATIONS_PER_PAGE = 28; // 4行7列，除去边框
    private boolean showingHistory = false; // false=待处理申请, true=申请历史
    
    public ApplicationManagementGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.title", "&6申请管理"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("application-management.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 添加功能按钮
        setupFunctionButtons(inventory);
        
        // 加载申请列表
        loadApplications(inventory);
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
        
        // 检查是否是申请按钮
        if (isApplicationSlot(slot)) {
            handleApplicationClick(player, slot, clickedItem, clickType);
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
        // 异步获取待处理申请数量
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            int pendingCount = applications != null ? applications.size() : 0;
            
            // 待处理申请按钮
            ItemStack pendingApplications = createItem(
                Material.PAPER,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.pending-applications.name", "&e待处理申请")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.pending-applications.lore.1", "&7查看待处理的申请")),
                ColorUtils.colorize("&f" + pendingCount + " 个申请")
            );
            inventory.setItem(20, pendingApplications);
        });
        
        // 申请历史按钮
        ItemStack applicationHistory = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.application-history.name", "&e申请历史")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.application-history.lore.1", "&7查看申请历史记录"))
        );
        inventory.setItem(24, applicationHistory);
        
        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.back.name", "&7返回")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.back.lore.1", "&7返回主菜单"))
        );
        inventory.setItem(49, back);
    }
    
    /**
     * 加载申请列表
     */
    private void loadApplications(Inventory inventory) {
        if (showingHistory) {
            loadApplicationHistory(inventory);
        } else {
            loadPendingApplications(inventory);
        }
    }
    
    /**
     * 加载待处理申请
     */
    private void loadPendingApplications(Inventory inventory) {
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                // 显示无申请信息
                ItemStack noApplications = createItem(
                    Material.BARRIER,
                    ColorUtils.colorize("&a没有待处理的申请"),
                    ColorUtils.colorize("&7当前没有待处理的申请")
                );
                inventory.setItem(22, noApplications);
                return;
            }
            
            // 计算分页
            int totalPages = (applications.size() - 1) / APPLICATIONS_PER_PAGE;
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }
            
            // 设置分页按钮
            setupPaginationButtons(inventory, totalPages);
            
            // 显示当前页的申请
            displayApplications(inventory, applications);
        });
    }
    
    /**
     * 加载申请历史
     */
    private void loadApplicationHistory(Inventory inventory) {
        plugin.getGuildService().getApplicationHistoryAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                // 显示无历史信息
                ItemStack noHistory = createItem(
                    Material.BARRIER,
                    ColorUtils.colorize("&a没有申请历史"),
                    ColorUtils.colorize("&7当前没有申请历史记录")
                );
                inventory.setItem(22, noHistory);
                return;
            }
            
            // 计算分页
            int totalPages = (applications.size() - 1) / APPLICATIONS_PER_PAGE;
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }
            
            // 设置分页按钮
            setupPaginationButtons(inventory, totalPages);
            
            // 显示当前页的申请
            displayApplications(inventory, applications);
        });
    }
    
    /**
     * 显示申请列表
     */
    private void displayApplications(Inventory inventory, List<GuildApplication> applications) {
        int startIndex = currentPage * APPLICATIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + APPLICATIONS_PER_PAGE, applications.size());
        
        int slotIndex = 10; // 从第2行第2列开始
        for (int i = startIndex; i < endIndex; i++) {
            GuildApplication application = applications.get(i);
            if (slotIndex >= 44) break; // 避免超出显示区域
            
            ItemStack applicationItem = createApplicationItem(application);
            inventory.setItem(slotIndex, applicationItem);
            
            slotIndex++;
            if (slotIndex % 9 == 8) { // 跳过边框
                slotIndex += 2;
            }
        }
    }
    
    /**
     * 设置分页按钮
     */
    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        // 上一页按钮
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.previous-page.name", "&c上一页")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.previous-page.lore.1", "&7查看上一页"))
            );
            inventory.setItem(18, previousPage);
        }
        
        // 下一页按钮
        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.next-page.name", "&a下一页")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.next-page.lore.1", "&7查看下一页"))
            );
            inventory.setItem(26, nextPage);
        }
    }
    
    /**
     * 创建申请物品
     */
    private ItemStack createApplicationItem(GuildApplication application) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();
        
        switch (application.getStatus()) {
            case PENDING:
                material = Material.YELLOW_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&e{applicant_name} 的申请", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7状态: &e待处理"));
                lore.add(PlaceholderUtils.replaceApplicationPlaceholders("&7申请时间: {apply_time}", application.getPlayerName(), guild.getName(), application.getCreatedAt()));
                lore.add(ColorUtils.colorize("&7消息: " + application.getMessage()));
                lore.add("");
                lore.add(ColorUtils.colorize("&a左键: 接受"));
                lore.add(ColorUtils.colorize("&c右键: 拒绝"));
                break;
            case APPROVED:
                material = Material.GREEN_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&a{applicant_name} 的申请", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7状态: &a已通过"));
                break;
            case REJECTED:
                material = Material.RED_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&c{applicant_name} 的申请", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7状态: &c已拒绝"));
                break;
            default:
                material = Material.GRAY_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&7{applicant_name} 的申请", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7状态: &7未知"));
                break;
        }
        
        return createItem(material, name, lore.toArray(new String[0]));
    }
    
    /**
     * 检查是否是功能按钮
     */
    private boolean isFunctionButton(int slot) {
        return slot == 20 || slot == 24 || slot == 49;
    }
    
    /**
     * 检查是否是分页按钮
     */
    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }
    
    /**
     * 检查是否是申请槽位
     */
    private boolean isApplicationSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8;
    }
    
    /**
     * 处理功能按钮点击
     */
    private void handleFunctionButton(Player player, int slot) {
        switch (slot) {
            case 20: // 待处理申请
                showingHistory = false;
                currentPage = 0;
                refreshInventory(player);
                break;
            case 24: // 申请历史
                showingHistory = true;
                currentPage = 0;
                refreshInventory(player);
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
     * 处理申请点击
     */
    private void handleApplicationClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (showingHistory) {
            // 历史记录只能查看，不能操作
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-history-view-only", "&7这是历史记录，只能查看");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 待处理申请可以接受或拒绝
        if (clickType == ClickType.LEFT) {
            // 接受申请
            handleAcceptApplication(player, slot);
        } else if (clickType == ClickType.RIGHT) {
            // 拒绝申请
            handleRejectApplication(player, slot);
        }
    }
    
    /**
     * 处理接受申请
     */
    private void handleAcceptApplication(Player player, int slot) {
        // 获取当前页的申请列表
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-pending-applications", "&c没有待处理的申请");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 计算申请在列表中的索引
            int applicationIndex = currentPage * APPLICATIONS_PER_PAGE + (slot - 10);
            if (applicationIndex >= 0 && applicationIndex < applications.size()) {
                GuildApplication application = applications.get(applicationIndex);
                
                // 处理申请
                plugin.getGuildService().processApplicationAsync(application.getId(), GuildApplication.ApplicationStatus.APPROVED, player.getUniqueId()).thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-accepted", "&a申请已接受！");
                        player.sendMessage(ColorUtils.colorize(message));
                        
                        // 刷新GUI
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-accept-failed", "&c接受申请失败！");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            }
        });
    }
    
    /**
     * 处理拒绝申请
     */
    private void handleRejectApplication(Player player, int slot) {
        // 获取当前页的申请列表
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-pending-applications", "&c没有待处理的申请");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 计算申请在列表中的索引
            int applicationIndex = currentPage * APPLICATIONS_PER_PAGE + (slot - 10);
            if (applicationIndex >= 0 && applicationIndex < applications.size()) {
                GuildApplication application = applications.get(applicationIndex);
                
                // 处理申请
                plugin.getGuildService().processApplicationAsync(application.getId(), GuildApplication.ApplicationStatus.REJECTED, player.getUniqueId()).thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-rejected", "&c申请已拒绝！");
                        player.sendMessage(ColorUtils.colorize(message));
                        
                        // 刷新GUI
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-reject-failed", "&c拒绝申请失败！");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            }
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
