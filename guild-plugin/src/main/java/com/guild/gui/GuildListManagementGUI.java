package com.guild.gui;

import java.util.ArrayList;
import java.util.List;

import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;

/**
 * 工会列表管理GUI
 */
public class GuildListManagementGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;
    private int currentPage = 0;
    private final int itemsPerPage = 12; // 从 28 减少到 12，界面更简洁
    private static final int PREVIOUS_PAGE_SLOT = 48;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int BACK_SLOT = 46;
    private static final int REFRESH_SLOT = 52;
    private List<Guild> allGuilds = new ArrayList<>();

    public GuildListManagementGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        loadGuilds();
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list-management.guild-list-management-title",
                "&4工会列表管理"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 设置工会列表
        setupGuildList(inventory);
        
        // 设置分页按钮
        setupPaginationButtons(inventory);
        
        // 设置操作按钮
        setupActionButtons(inventory);
    }
    
    private void setupGuildList(Inventory inventory) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allGuilds.size());
        int cols = 3; // 三列布局，从列2开始（列 2,3,4）

        for (int i = 0; i < itemsPerPage; i++) {
            if (startIndex + i < endIndex) {
                Guild guild = allGuilds.get(startIndex + i);

                int row = (i / cols) + 1; // 1..4 对应 GUI 的行 2..5
                int col = (i % cols) + 1; // 列 2,3,4 从槽位10开始
                int slot = row * 9 + col;

                inventory.setItem(slot, createGuildItem(guild));
            }
        }
    }
    
    private ItemStack createGuildItem(Guild guild) {
        Material material = guild.isFrozen() ? Material.RED_WOOL : Material.GREEN_WOOL;

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.leader", "会长") + ": &e" + guild.getLeaderName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-list.level", "等级") + ": &e" + guild.getLevel() + "  &7" + languageManager.getGuiMessage(player, "gui.guild-list.balance", "资金") + ": &a" + plugin.getEconomyManager().format(guild.getBalance())));
        lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(player, "gui.guild-list.left-click-view", "左键: 查看") + "  &c" + languageManager.getGuiMessage(player, "gui.guild-list.right-click-delete", "右键: 删除") + "  &6" + languageManager.getGuiMessage(player, "gui.guild-list.shift-right-freeze", "Shift+右键: 冻结/解冻")));

        return createItem(material, ColorUtils.colorize("&6" + guild.getName()), lore.toArray(new String[0]));
    }
    
    private void setupPaginationButtons(Inventory inventory) {
        int totalPages = Math.max(1, (int) Math.ceil((double) allGuilds.size() / itemsPerPage));
        if (currentPage > totalPages - 1) {
            currentPage = totalPages - 1;
        }

        // 上一页按钮
        if (currentPage > 0) {
            inventory.setItem(PREVIOUS_PAGE_SLOT, createItem(Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.previous-page", "&a上一页")),
                ColorUtils.colorize("&7" + languageManager.getGuiIndexedMessage(player, "gui.common.page-info", "第 {0} 页，共 {1} 页", String.valueOf(currentPage), String.valueOf(totalPages)))));
        }

        // 页码信息
        inventory.setItem(PAGE_INFO_SLOT, createItem(Material.PAPER,
            ColorUtils.colorize("&e" + languageManager.getGuiIndexedMessage(player, "gui.common.page-info", "第 {0} 页/共 {1} 页", String.valueOf(currentPage + 1), String.valueOf(totalPages)))));

        // 下一页按钮
        if (currentPage < totalPages - 1) {
            inventory.setItem(NEXT_PAGE_SLOT, createItem(Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.next-page", "&a下一页")),
                ColorUtils.colorize("&7" + languageManager.getGuiIndexedMessage(player, "gui.common.page-info", "第 {0} 页，共 {1} 页", String.valueOf(currentPage + 2), String.valueOf(totalPages)))));
        }
    }

    private void setupActionButtons(Inventory inventory) {
        // 返回按钮
        inventory.setItem(46, createItem(Material.BARRIER,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.back", "&c返回"))));

        // 刷新按钮
        inventory.setItem(52, createItem(Material.EMERALD,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list-management.gui-refresh", "&a刷新列表"))));
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
    
    private void loadGuilds() {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            this.allGuilds = guilds;
            CompatibleScheduler.runTask(plugin, () -> {
                if (player.isOnline()) {
                    refresh(player);
                }
            });
        });
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == BACK_SLOT) {
            // 返回
            plugin.getGuiManager().openGUI(player, new AdminGuildGUI(plugin, player));
        } else if (slot == REFRESH_SLOT) {
            // 刷新
            loadGuilds();
        } else if (slot == PREVIOUS_PAGE_SLOT && currentPage > 0) {
            // 上一页
            currentPage--;
            refresh(player);
        } else if (slot == NEXT_PAGE_SLOT && currentPage < (int) Math.ceil((double) allGuilds.size() / itemsPerPage) - 1) {
            // 下一页
            currentPage++;
            refresh(player);
        } else if (slot >= 10 && slot <= 43) {
            // 工会项目 - 检查是否在3列布局，列 2,3,4，行 1..4 范围内
            int row = slot / 9;
            int col = slot % 9;
            if (row >= 1 && row <= 4 && col >= 1 && col <= 3) {
                int cols = 3;
                int relativeIndex = (row - 1) * cols + (col - 1);
                int guildIndex = (currentPage * itemsPerPage) + relativeIndex;
                if (guildIndex < allGuilds.size()) {
                    Guild guild = allGuilds.get(guildIndex);
                    handleGuildClick(player, guild, clickType);
                }
            }
        }
    }
    
    private void handleGuildClick(Player player, Guild guild, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            // 查看详情
            openGuildDetailGUI(player, guild);
        } else if (clickType == ClickType.RIGHT) {
            // 删除工会
            deleteGuild(player, guild);
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            // 冻结/解冻工会（Shift+右键）
            toggleGuildFreeze(player, guild);
        }
    }
    
    private void openGuildDetailGUI(Player player, Guild guild) {
        // 打开工会详情GUI
        plugin.getGuiManager().openGUI(player, new GuildDetailGUI(plugin, guild, player));
    }
    
    private void deleteGuild(Player player, Guild guild) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ColorUtils.colorize("&c您没有权限执行此操作！"));
            return;
        }
        // 打开统一的确认删除GUI
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild, player));
    }
    
    private void toggleGuildFreeze(Player player, Guild guild) {
        boolean newStatus = !guild.isFrozen();
        plugin.getGuildService().updateGuildFrozenStatusAsync(guild.getId(), newStatus).thenAccept(success -> {
            if (success) {
                String message = newStatus ? "&a工会 " + guild.getName() + " 已被冻结！" : "&a工会 " + guild.getName() + " 已被解冻！";
                player.sendMessage(ColorUtils.colorize(message));
                loadGuilds(); // 刷新列表
            } else {
                player.sendMessage(ColorUtils.colorize("&c操作失败！"));
            }
        });
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
        // 关闭时的处理
    }
    
    @Override
    public void refresh(Player player) {
        if (player.isOnline()) {
            plugin.getGuiManager().refreshGUI(player);
        }
    }
}
