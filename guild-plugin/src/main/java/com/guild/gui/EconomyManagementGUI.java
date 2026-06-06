package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 经济管理GUI
 */
public class EconomyManagementGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;
    private int currentPage = 0;
    private final int itemsPerPage = 28; // 7列 × 4行
    private static final int PREVIOUS_PAGE_SLOT = 48;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int BACK_SLOT = 46;
    private static final int REFRESH_SLOT = 52;
    private List<Guild> allGuilds = new ArrayList<>();

    public EconomyManagementGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        loadGuilds();
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getMessage(player, "economy-management-title", "&e经济管理"));
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
        
        for (int i = 0; i < itemsPerPage; i++) {
            if (startIndex + i < endIndex) {
                Guild guild = allGuilds.get(startIndex + i);
                
                // 计算在2-8列，2-5行的位置 (slots 10-43)
                int row = (i / 7) + 1; // 2-5行
                int col = (i % 7) + 1; // 2-8列
                int slot = row * 9 + col;
                
                inventory.setItem(slot, createGuildItem(guild));
            }
        }
    }
    
    private ItemStack createGuildItem(Guild guild) {
        Material material = Material.GOLD_INGOT;
        
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "gui.guild-name", "工会名称") + ": " + guild.getName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "gui.leader", "会长") + ": " + guild.getLeaderName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "economy-management.level", "等级") + ": " + guild.getLevel()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "economy-management.current-balance", "当前资金") + ": " + plugin.getEconomyManager().format(guild.getBalance())));
        lore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "economy-management.max-members", "最大成员") + ": " + guild.getMaxMembers()));
        lore.add("");
        lore.add(ColorUtils.colorize("&e" + languageManager.getMessage(player, "economy-management.left-click-set", "Left click: Set balance")));
        lore.add(ColorUtils.colorize("&a" + languageManager.getMessage(player, "economy-management.right-click-add", "Right click: Add balance")));
        lore.add(ColorUtils.colorize("&c" + languageManager.getMessage(player, "economy-management.middle-click-remove", "Middle click: Remove balance")));
        
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
                ColorUtils.colorize(languageManager.getMessage(player, "economy-management.previous-page", "&a上一页")),
                ColorUtils.colorize(languageManager.getMessage(player, "economy-management.previous-page.desc", "&7第 {page} 页", "{page}", String.valueOf(currentPage)))));
        }

        // 页码信息
        inventory.setItem(PAGE_INFO_SLOT, createItem(Material.PAPER,
            ColorUtils.colorize(languageManager.getMessage(player, "economy-management.page-info", "&e第 {current} 页，共 {total} 页", "{current}", String.valueOf(currentPage + 1), "{total}", String.valueOf(totalPages)))));

        // 下一页按钮
        if (currentPage < totalPages - 1) {
            inventory.setItem(NEXT_PAGE_SLOT, createItem(Material.ARROW,
                ColorUtils.colorize(languageManager.getMessage(player, "economy-management.next-page", "&a下一页")),
                ColorUtils.colorize(languageManager.getMessage(player, "economy-management.next-page.desc", "&7第 {page} 页", "{page}", String.valueOf(currentPage + 2)))));
        }
    }

    private void setupActionButtons(Inventory inventory) {
        // 返回按钮
        inventory.setItem(46, createItem(Material.BARRIER,
            ColorUtils.colorize(languageManager.getMessage(player, "economy-management.back", "&c返回"))));

        // 刷新按钮
        inventory.setItem(52, createItem(Material.EMERALD,
            ColorUtils.colorize(languageManager.getMessage(player, "economy-management.refresh", "&a刷新列表"))));
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
        if (slot == 46) {
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
            // 工会项目 - 检查是否在2-8列，2-5行范围内
            int row = slot / 9;
            int col = slot % 9;
            if (row >= 1 && row <= 4 && col >= 1 && col <= 7) {
                int relativeIndex = (row - 1) * 7 + (col - 1);
                int guildIndex = (currentPage * itemsPerPage) + relativeIndex;
                if (guildIndex < allGuilds.size()) {
                    Guild guild = allGuilds.get(guildIndex);
                    handleGuildClick(player, guild, clickType);
                }
            }
        }
    }
    
    private void handleGuildClick(Player player, Guild guild, ClickType clickType) {
        if (clickType == ClickType.MIDDLE) {
            // 中键：直接打开确认GUI（不需要输入金额）
            ConfirmChangeFundsGUI confirmGUI = new ConfirmChangeFundsGUI(
                    plugin, guild, player, "remove",
                    guild.getBalance());
            String msg = languageManager.getMessage(player,
                    "economy-management.middle-click-desc",
                    "&c即将清空 &e{guild} &c的资金，请确认", "{guild}", guild.getName());
            player.sendMessage(ColorUtils.colorize(msg));
            plugin.getGuiManager().openGUI(player, confirmGUI);
            return;
        }

        // 左键：设置资金 / 右键：增加资金 — 先关闭GUI并进入输入模式
        String operationType;
        String promptKey;
        if (clickType == ClickType.LEFT) {
            operationType = "set";
            promptKey = "economy-management.set-prompt";
        } else if (clickType == ClickType.RIGHT) {
            operationType = "add";
            promptKey = "economy-management.add-prompt";
        } else {
            return;
        }

        // 关闭当前GUI
        plugin.getGuiManager().closeGUI(player);

        // 发送提示
        String prompt = languageManager.getMessage(player, promptKey,
                "&e请输入金额（在聊天框输入数字）:");
        player.sendMessage(ColorUtils.colorize(prompt));

        // 设置输入模式：捕获玩家输入的金额
        plugin.getGuiManager().setInputMode(player, input -> {
            try {
                double amount = Double.parseDouble(input.trim());
                if (amount <= 0) {
                    player.sendMessage(ColorUtils.colorize(
                            languageManager.getMessage(player,
                                    "economy-management.invalid-amount",
                                    "&c金额必须大于0！")));
                    return false; // 继续等待有效输入
                }
                // 打开确认GUI
                ConfirmChangeFundsGUI confirmGUI = new ConfirmChangeFundsGUI(
                        plugin, guild, player, operationType, amount);
                plugin.getGuiManager().openGUI(player, confirmGUI);
                return true;
            } catch (NumberFormatException e) {
                if (input.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ColorUtils.colorize(
                            languageManager.getMessage(player,
                                    "economy-management.input-cancelled",
                                    "&7已取消操作")));
                    return true; // 退出输入模式
                }
                player.sendMessage(ColorUtils.colorize(
                        languageManager.getMessage(player,
                                "economy-management.invalid-number",
                                "&c无效的数字！请输入有效金额或输入 cancel 取消")));
                return false;
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
