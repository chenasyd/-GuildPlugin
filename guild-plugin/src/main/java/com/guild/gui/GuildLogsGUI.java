package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.language.LanguageManager;
import com.guild.models.Guild;
import com.guild.models.GuildLog;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 工会日志查看GUI
 */
public class GuildLogsGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;
    private final int page;
    private final int itemsPerPage = 28; // 2-8列，2-5行
    private List<GuildLog> logs;
    private int totalLogs;
    
    public GuildLogsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this(plugin, guild, player, 0);
    }

    public GuildLogsGUI(GuildPlugin plugin, Guild guild, Player player, int page) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.page = page;
    }
    
    @Override
    public String getTitle() {
        String title = plugin.getLanguageManager().getGuiMessage(player, "gui.guild-logs.title", "&6工会日志 - {guild_name}");
        return ColorUtils.colorize(title.replace("{guild_name}", ColorUtils.stripColor(guild.getName())));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 异步加载日志数据
        loadLogsAsync().thenAccept(success -> {
            if (success) {
                // 在主线程中设置物品和完整的导航按钮
                CompatibleScheduler.runTask(plugin, () -> {
                    setupLogItems(inventory);
                    setupBasicNavigationButtons(inventory);
                    setupFullNavigationButtons(inventory);
                });
            } else {
                // 如果加载失败，在主线程中显示错误信息
                CompatibleScheduler.runTask(plugin, () -> {
                    ItemStack errorItem = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize("&c" + languageManager.getGuiMessage(player, "gui.guild-logs.load-failed", "加载失败")),
                        ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.load-error", "无法加载日志数据，请重试"))
                    );
                    inventory.setItem(22, errorItem);
                    setupBasicNavigationButtons(inventory);
                });
            }
        });
    }
    
    /**
     * 异步加载日志数据
     */
    private CompletableFuture<Boolean> loadLogsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Loading guild logs for " + guild.getName() + "...");
                
                // 检查工会ID是否有效
                if (guild.getId() <= 0) {
                    plugin.getLogger().warning("Invalid guild ID: " + guild.getId());
                    return false;
                }
                
                // 获取日志总数
                totalLogs = plugin.getGuildService().getGuildLogsCountAsync(guild.getId()).get();
                plugin.getLogger().info("Guild " + guild.getName() + " has " + totalLogs + " log records");
                
                // 获取当前页的日志
                int offset = page * itemsPerPage;
                logs = plugin.getGuildService().getGuildLogsAsync(guild.getId(), itemsPerPage, offset).get();
                plugin.getLogger().info("Loaded page " + (page + 1) + " with " + logs.size() + " log records");
                
                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading guild logs: " + e.getMessage());
                e.printStackTrace();
                
                // 设置默认值
                totalLogs = 0;
                logs = new java.util.ArrayList<>();
                
                return false;
            }
        });
    }
    
    /**
     * 设置日志物品
     */
    private void setupLogItems(Inventory inventory) {
        plugin.getLogger().info("Setting up log items, logs size: " + (logs != null ? logs.size() : "null"));
        
        if (logs == null) {
            logs = new java.util.ArrayList<>(); // 确保logs不为null
        }
        
        if (logs.isEmpty()) {
            plugin.getLogger().info("Log list is empty, showing no-logs message");
            // 显示无日志信息
            ItemStack noLogs = createItem(
                Material.BARRIER,
                ColorUtils.colorize("&c" + languageManager.getGuiMessage(player, "gui.guild-logs.no-logs", "暂无日志记录")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.no-logs-desc", "该工会还没有任何操作记录")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.wait-for-logs", "请等待工会活动产生日志"))
            );
            inventory.setItem(22, noLogs);
            return;
        }
        
        plugin.getLogger().info("Displaying " + logs.size() + " log records");
        
        // 显示日志列表
        for (int i = 0; i < Math.min(logs.size(), itemsPerPage); i++) {
            GuildLog log = logs.get(i);
            int slot = getLogSlot(i);
            
            plugin.getLogger().info("Set log item " + i + " to slot " + slot + ": " + log.getLogType().getDisplayName());
            
            ItemStack logItem = createLogItem(log);
            inventory.setItem(slot, logItem);
        }
    }
    
    /**
     * 创建日志物品
     */
    private ItemStack createLogItem(GuildLog log) {
        Material material = getLogMaterial(log.getLogType());
        String name = ColorUtils.colorize("&e" + log.getLogType().getDisplayName());

        List<String> lore = new java.util.ArrayList<>();
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.operator", "Operator") + ": &f" + ColorUtils.stripColor(log.getPlayerName())));
        if (log.getPlayerUuid() != null && !log.getPlayerUuid().equals("SYSTEM")) {
            lore.add(ColorUtils.colorize("&8UUID: " + log.getPlayerUuid().substring(0, 8) + "..."));
        }
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.time", "Time") + ": &f" + log.getSimpleTime(languageManager.getPlayerLanguage(player))));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.description", "Description") + ": &f" + ColorUtils.stripColor(log.getDescription())));

        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
            lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.details", "Details") + ": &f" + ColorUtils.stripColor(log.getDetails())));
        }

        if (log.getLogType() == GuildLog.LogType.FUND_DEPOSITED ||
            log.getLogType() == GuildLog.LogType.FUND_WITHDRAWN ||
            log.getLogType() == GuildLog.LogType.FUND_TRANSFERRED) {
            if ("SYSTEM".equals(log.getPlayerUuid())) {
                lore.add(ColorUtils.colorize(""));
                lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-logs.system-operator", "&c\u26A0 Operator is SYSTEM (may be legacy record)")));
            } else {
                lore.add(ColorUtils.colorize(""));
                lore.add(ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-logs.real-operator", "&a\u2713 Real operator recorded")));
            }
        }

        return createItem(material, name, lore.toArray(new String[0]));
    }
    
    /**
     * 根据日志类型获取物品材质
     */
    private Material getLogMaterial(GuildLog.LogType logType) {
        switch (logType) {
            case GUILD_CREATED:
                return Material.GREEN_WOOL;
            case GUILD_DISSOLVED:
                return Material.RED_WOOL;
            case MEMBER_JOINED:
                return Material.EMERALD;
            case MEMBER_LEFT:
                return Material.REDSTONE;
            case MEMBER_KICKED:
                return Material.REDSTONE;
            case MEMBER_PROMOTED:
                return Material.GOLD_INGOT;
            case MEMBER_DEMOTED:
                return Material.IRON_INGOT;
            case LEADER_TRANSFERRED:
                return Material.DIAMOND;
            case FUND_DEPOSITED:
                return Material.GOLD_NUGGET;
            case FUND_WITHDRAWN:
                return Material.IRON_NUGGET;
            case FUND_TRANSFERRED:
                return Material.EMERALD_BLOCK;
            case RELATION_CREATED:
            case RELATION_ACCEPTED:
                return Material.BLUE_WOOL;
            case RELATION_DELETED:
            case RELATION_REJECTED:
                return Material.ORANGE_WOOL;
            case GUILD_FROZEN:
                return Material.ICE;
            case GUILD_UNFROZEN:
                return Material.WATER_BUCKET;
            case GUILD_LEVEL_UP:
                return Material.EXPERIENCE_BOTTLE;
            case APPLICATION_SUBMITTED:
            case APPLICATION_ACCEPTED:
            case APPLICATION_REJECTED:
                return Material.PAPER;
            case INVITATION_SENT:
            case INVITATION_ACCEPTED:
            case INVITATION_REJECTED:
                return Material.BOOK;
            default:
                return Material.GRAY_WOOL;
        }
    }
    
    /**
     * 获取日志物品的槽位 - 修复后的计算逻辑
     */
    private int getLogSlot(int index) {
        int row = index / 7; // 7列
        int col = index % 7;
        return (row + 1) * 9 + (col + 1); // 从第1行第1列开始 (slots 10-43)
    }
    
    /**
     * 设置基本的导航按钮（不依赖日志数据）
     */
    private void setupBasicNavigationButtons(Inventory inventory) {
        // 返回按钮 - 槽位49
        ItemStack backButton = createItem(
            Material.ARROW,
            ColorUtils.colorize("&c" + languageManager.getGuiMessage(player, "gui.common.back", "返回")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.back-to-settings", "返回工会设置"))
        );
        inventory.setItem(49, backButton);
    }
    
    /**
     * 设置完整的导航按钮（依赖日志数据）
     */
    private void setupFullNavigationButtons(Inventory inventory) {
        if ((page + 1) * itemsPerPage < totalLogs) {
            ItemStack nextButton = createItem(
                Material.ARROW,
                ColorUtils.colorize("&a" + languageManager.getGuiMessage(player, "gui.common.next-page", "下一页")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.view-next", "查看下一页"))
            );
            inventory.setItem(50, nextButton);
        }

        // 页码信息
        int totalPages = (totalLogs - 1) / itemsPerPage + 1;
        ItemStack pageInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize("&e" + languageManager.getGuiMessage(player, "gui.guild-logs.page-info", "页码信息")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.current-page", "当前页: {page}", "{page}", String.valueOf(page + 1))),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.total-pages", "总页数: {total}", "{total}", String.valueOf(totalPages))),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.total-records", "总记录: {count}", "{count}", String.valueOf(totalLogs)))
        );
        inventory.setItem(46, pageInfo);

        // 刷新按钮
        ItemStack refreshButton = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&a" + languageManager.getGuiMessage(player, "gui.common.refresh", "刷新")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.refresh-logs", "刷新日志列表"))
        );
        inventory.setItem(51, refreshButton);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        // 返回工会设置 (槽位49)
        if (slot == 49) {
            GuildSettingsGUI settingsGUI = new GuildSettingsGUI(plugin, guild, player);
            plugin.getGuiManager().openGUI(player, settingsGUI);
            return;
        }

        // 下一页按钮 (槽位50)
        if (slot == 50 && (page + 1) * itemsPerPage < totalLogs) {
            GuildLogsGUI nextPageGUI = new GuildLogsGUI(plugin, guild, player, page + 1);
            plugin.getGuiManager().openGUI(player, nextPageGUI);
            return;
        }

        // 刷新按钮 (槽位51)
        if (slot == 51) {
            GuildLogsGUI refreshGUI = new GuildLogsGUI(plugin, guild, player, page);
            plugin.getGuiManager().openGUI(player, refreshGUI);
            return;
        }

        // 日志项目点击 - 检查是否在日志显示区域
        if (slot >= 10 && slot <= 43) {
            int row = slot / 9;
            int col = slot % 9;
            if (row >= 1 && row <= 4 && col >= 1 && col <= 7) {
                int relativeIndex = (row - 1) * 7 + (col - 1);
                int logIndex = (page * itemsPerPage) + relativeIndex;
                if (logIndex < logs.size()) {
                    GuildLog log = logs.get(logIndex);
                    handleLogClick(player, log);
                }
            }
        }
    }
    
    /**
     * 处理日志点击
     */
    private void handleLogClick(Player player, GuildLog log) {
        // 显示日志详细信息
        String header = ColorUtils.colorize("&6" + languageManager.getGuiMessage(player, "gui.guild-logs.details-header", "=== 日志详情 ==="));
        player.sendMessage(header);
        player.sendMessage(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.type", "类型") + ": &f" + log.getLogType().getDisplayName()));
        player.sendMessage(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.operator", "操作者") + ": &f" + ColorUtils.stripColor(log.getPlayerName())));
        player.sendMessage(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.time", "时间") + ": &f" + log.getSimpleTime(languageManager.getPlayerLanguage(player))));
        player.sendMessage(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.description", "描述") + ": &f" + ColorUtils.stripColor(log.getDescription())));
        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-logs.details", "详情") + ": &f" + ColorUtils.stripColor(log.getDetails())));
        }
        player.sendMessage(ColorUtils.colorize("&6" + languageManager.getGuiMessage(player, "gui.guild-logs.separator", "==================")));
    }
    
    @Override
    public void onClose(Player player) {
        // 关闭时的处理
    }
    
    @Override
    public void refresh(Player player) {
        // 刷新GUI
        GuildLogsGUI refreshGUI = new GuildLogsGUI(plugin, guild, player, page);
        plugin.getGuiManager().openGUI(player, refreshGUI);
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
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(java.util.Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
