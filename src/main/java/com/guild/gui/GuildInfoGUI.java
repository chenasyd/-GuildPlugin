package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.GUIUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 工会信息GUI
 */
public class GuildInfoGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Player player;
    private final Guild guild;
    private Inventory inventory;
    
    public GuildInfoGUI(GuildPlugin plugin, Player player, Guild guild) {
        this.plugin = plugin;
        this.player = player;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-info.title", "&6工会信息"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-info.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        this.inventory = inventory;
        
        // 获取GUI配置
        ConfigurationSection config = plugin.getConfigManager().getGuiConfig().getConfigurationSection("guild-info.items");
        if (config == null) {
            setupDefaultItems();
            return;
        }
        
        // 设置配置的物品
        for (String key : config.getKeys(false)) {
            ConfigurationSection itemConfig = config.getConfigurationSection(key);
            if (itemConfig != null) {
                setupConfigItem(itemConfig);
            }
        }
    }
    
    private void setupConfigItem(ConfigurationSection itemConfig) {
        String materialName = itemConfig.getString("material", "STONE");
        Material material = Material.valueOf(materialName.toUpperCase());
        int slot = itemConfig.getInt("slot", 0);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // 设置名称
            String name = itemConfig.getString("name", "");
            if (!name.isEmpty()) {
                // 使用GUIUtils处理变量
                GUIUtils.processGUIVariablesAsync(name, guild, player, plugin).thenAccept(processedName -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        meta.setDisplayName(processedName);
                        
                        // 设置描述
                        List<String> lore = itemConfig.getStringList("lore");
                        if (!lore.isEmpty()) {
                            GUIUtils.processGUILoreAsync(lore, guild, player, plugin).thenAccept(processedLore -> {
                                CompatibleScheduler.runTask(plugin, () -> {
                                    meta.setLore(processedLore);
                                    item.setItemMeta(meta);
                                    inventory.setItem(slot, item);
                                });
                            });
                        } else {
                            item.setItemMeta(meta);
                            inventory.setItem(slot, item);
                        }
                    });
                });
            } else {
                // 如果没有名称，直接设置描述
                List<String> lore = itemConfig.getStringList("lore");
                if (!lore.isEmpty()) {
                                    GUIUtils.processGUILoreAsync(lore, guild, player, plugin).thenAccept(processedLore -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        meta.setLore(processedLore);
                        item.setItemMeta(meta);
                        inventory.setItem(slot, item);
                    });
                });
                } else {
                    item.setItemMeta(meta);
                    inventory.setItem(slot, item);
                }
            }
        } else {
            inventory.setItem(slot, item);
        }
    }
    
    private void setupDefaultItems() {
        // 工会名称
        ItemStack nameItem = createItem(Material.NAME_TAG, "§6工会名称", 
            "§e" + guild.getName());
        inventory.setItem(10, nameItem);
        
        // 工会标签
        if (guild.getTag() != null && !guild.getTag().isEmpty()) {
            ItemStack tagItem = createItem(Material.OAK_SIGN, "§6工会标签", 
                "§e[" + guild.getTag() + "]");
            inventory.setItem(12, tagItem);
        }
        
        // 工会描述
        if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
            ItemStack descItem = createItem(Material.BOOK, "§6工会描述", 
                "§e" + guild.getDescription());
            inventory.setItem(14, descItem);
        }
        
        // 会长信息
        ItemStack leaderItem = createItem(Material.GOLDEN_HELMET, "§6会长", 
            "§e" + guild.getLeaderName());
        inventory.setItem(16, leaderItem);
        
        // 成员数量 - 使用异步方法
        plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(memberCount -> {
            CompatibleScheduler.runTask(plugin, () -> {
                ItemStack memberItem = createItem(Material.PLAYER_HEAD, "§6成员数量", 
                    "§e" + memberCount + "/" + guild.getMaxMembers() + " 人");
                inventory.setItem(28, memberItem);
            });
        });
        
        // 工会等级
        ItemStack levelItem = createItem(Material.EXPERIENCE_BOTTLE, "§6工会等级", 
            "§e等级 " + guild.getLevel(),
            "§7最大成员: " + guild.getMaxMembers() + " 人");
        inventory.setItem(30, levelItem);
        
        // 工会资金
        ItemStack balanceItem = createItem(Material.GOLD_INGOT, "§6工会资金", 
            "§e" + plugin.getEconomyManager().format(guild.getBalance()),
            "§7等级升级需要: " + getNextLevelRequirement(guild.getLevel()));
        inventory.setItem(32, balanceItem);
        
        // 创建时间（使用现实时间格式）
        String createdTime = guild.getCreatedAt() != null
            ? guild.getCreatedAt().format(com.guild.core.time.TimeProvider.FULL_FORMATTER)
            : "未知";
        ItemStack timeItem = createItem(Material.CLOCK, "§6创建时间", "§e" + createdTime);
        inventory.setItem(34, timeItem);
        
        // 工会状态
        String status = guild.isFrozen() ? "§c已冻结" : "§a正常";
        ItemStack statusItem = createItem(Material.BEACON, "§6工会状态", 
            status);
        inventory.setItem(36, statusItem);
        
        // 返回按钮
        ItemStack backItem = createItem(Material.ARROW, "§c返回", 
            "§e点击返回主菜单");
        inventory.setItem(49, backItem);
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
    
    private String replacePlaceholders(String text) {
        return PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);
    }

    private String replacePlaceholdersAsync(String text, int memberCount) {
        // 先使用PlaceholderUtils处理基础变量
        String result = PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);
        
        // 然后处理动态变量
        return result
            .replace("{member_count}", String.valueOf(memberCount))
            .replace("{online_member_count}", String.valueOf(memberCount)); // 暂时使用总成员数
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) {
            // 返回主菜单
            plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
        }
    }
    
    @Override
    public void onClose(Player player) {
        // 关闭时的处理
    }
    
    @Override
    public void refresh(Player player) {
        setupInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * 获取下一级升级所需资金
     */
    private String getNextLevelRequirement(int currentLevel) {
        if (currentLevel >= 10) {
            return "已达到最高等级";
        }
        
        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
        }
        
        return plugin.getEconomyManager().format(required);
    }

    /**
     * 获取当前等级进度
     */
    private String getLevelProgress(int currentLevel, double currentBalance) {
        if (currentLevel >= 10) {
            return "100%";
        }

        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
        }

        double percentage = (currentBalance / required) * 100;
        return String.format("%.1f%%", percentage);
    }
}
