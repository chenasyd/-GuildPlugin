package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.models.Guild;
import com.guild.gui.SystemSettingsGUI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 管理员公会GUI
 */
public class AdminGuildGUI implements GUI {

    // ── 图像模式功能常量 ──
    public static final String FUNC_GUILD_LIST_MGMT = "GUILD_LIST_MGMT";
    public static final String FUNC_ECONOMY_MGMT = "ECONOMY_MGMT";
    public static final String FUNC_RELATION_MGMT = "RELATION_MGMT";
    public static final String FUNC_STATISTICS = "STATISTICS";
    public static final String FUNC_SYSTEM_SETTINGS = "SYSTEM_SETTINGS";
    public static final String FUNC_BACK = "BACK";

    private final GuildPlugin plugin;
    private final Player player;

    public AdminGuildGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.title", "&4Guild Admin"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);

        // 公会列表管理
        ItemStack guildList = createItem(
            Material.BOOKSHELF,
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-guild-list-name", "&eGuild List Management")),
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-guild-list-lore-1", "&7View and manage all guilds")),
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-guild-list-lore-2", "&7Including delete, freeze, etc."))
        );
        inventory.setItem(20, guildList);

        // 经济管理
        ItemStack economy = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-economy-name", "&eEconomy Management")),
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-economy-lore-1", "&7Manage guild economy system")),
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-economy-lore-2", "&7Set funds, view contributions, etc."))
        );
        inventory.setItem(22, economy);

        // 关系管理
        ItemStack relations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-relations-name", "&eRelations Management")),
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-relations-lore-1", "&7Manage guild relations")),
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-relations-lore-2", "&7Allies, enemies, wars, etc."))
        );
        inventory.setItem(24, relations);

        // 统计信息
        ItemStack statistics = createItem(
            Material.PAPER,
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-statistics-name", "&eStatistics")),
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-statistics-lore-1", "&7View guild statistics")),
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-statistics-lore-2", "&7Member count, economy status, etc."))
        );
        inventory.setItem(29, statistics);

        // 系统设置
        ItemStack settings = createItem(
            Material.COMPASS,
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-settings-name", "&eSystem Settings")),
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-settings-lore-1", "&7Manage system settings")),
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-settings-lore-2", "&7Reload config, permissions, etc."))
        );
        inventory.setItem(31, settings);

        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.common.back", "Back")),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getGuiMessage(player, "gui.common.back-to-main-menu", "Back to main menu"))
        );
        inventory.setItem(49, back);

        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 20: // 公会列表管理
                openGuildListManagement(player);
                break;
            case 22: // 经济管理
                openEconomyManagement(player);
                break;
            case 24: // 关系管理
                openRelationManagement(player);
                break;
            case 29: // 统计信息
                openStatistics(player);
                break;
            case 31: // 系统设置
                openSystemSettings(player);
                break;
            case 49: // 返回
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                break;
        }
    }
    
    private void openGuildListManagement(Player player) {
        // 打开公会列表管理GUI
        GuildListManagementGUI guildListGUI = new GuildListManagementGUI(plugin, player);
        plugin.getGuiManager().openGUI(player, guildListGUI);
    }
    
    private void openEconomyManagement(Player player) {
        // 打开经济管理GUI
        EconomyManagementGUI economyGUI = new EconomyManagementGUI(plugin, player);
        plugin.getGuiManager().openGUI(player, economyGUI);
    }
    
    private void openRelationManagement(Player player) {
        // 打开关系管理GUI
        RelationManagementGUI relationGUI = new RelationManagementGUI(plugin, player);
        plugin.getGuiManager().openGUI(player, relationGUI);
    }
    
    private void openStatistics(Player player) {
        // 显示统计信息
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            player.sendMessage(ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-statistics-title", "&6=== Guild Statistics ===")));
            player.sendMessage(ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-statistics-total-guilds", "&eTotal Guilds: &f{count}", "{count}", String.valueOf(guilds.size()))));

            if (!guilds.isEmpty()) {
                final double[] totalBalance = {0};
                final int[] frozenCount = {0};

                for (Guild guild : guilds) {
                    totalBalance[0] += guild.getBalance();
                    if (guild.isFrozen()) {
                        frozenCount[0]++;
                    }
                }

                // 获取总成员数
                CompletableFuture<Integer>[] memberCountFutures = new CompletableFuture[guilds.size()];
                for (int i = 0; i < guilds.size(); i++) {
                    memberCountFutures[i] = plugin.getGuildService().getGuildMemberCountAsync(guilds.get(i).getId());
                }

                CompletableFuture.allOf(memberCountFutures).thenRun(() -> {
                    final int[] totalMembers = {0};
                    for (CompletableFuture<Integer> future : memberCountFutures) {
                        try {
                            Integer count = future.join();
                            if (count != null) {
                                totalMembers[0] += count;
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Failed to get member count: " + e.getMessage());
                        }
                    }

                    player.sendMessage(ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-statistics-total-members", "&eTotal Members: &f{count}", "{count}", String.valueOf(totalMembers[0]))));
                    player.sendMessage(ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-statistics-total-balance", "&eTotal Balance: &f{balance}", "{balance}", String.valueOf(totalBalance[0]))));
                    player.sendMessage(ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-statistics-frozen-guilds", "&eFrozen Guilds: &f{count}", "{count}", String.valueOf(frozenCount[0]))));
                    player.sendMessage(ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.admin-gui.admin-gui-statistics-normal-guilds", "&eNormal Guilds: &f{count}", "{count}", String.valueOf(guilds.size() - frozenCount[0]))));
                });
            }
        });
    }
    
    private void openSystemSettings(Player player) {
        // 打开系统设置GUI
        plugin.getGuiManager().openGUI(player, new SystemSettingsGUI(plugin, player));
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
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(plugin.getLanguageManager().getGuiColoredMessage(player, "gui.admin-gui.bedrock-title", "&4Guild Admin"))
            .content(plugin.getLanguageManager().getGuiColoredMessage(player, "gui.admin-gui.bedrock-content", "&fSelect management function"));

        builder.button(plugin.getLanguageManager().getGuiColoredMessage(player, "gui.admin-gui.bedrock-guild-list", "&eGuild List Management"));
        builder.button(plugin.getLanguageManager().getGuiColoredMessage(player, "gui.admin-gui.bedrock-economy", "&eEconomy Management"));
        builder.button(plugin.getLanguageManager().getGuiColoredMessage(player, "gui.admin-gui.bedrock-relations", "&eRelations Management"));
        builder.button(plugin.getLanguageManager().getGuiColoredMessage(player, "gui.admin-gui.bedrock-statistics", "&eStatistics"));
        builder.button(plugin.getLanguageManager().getGuiColoredMessage(player, "gui.admin-gui.bedrock-settings", "&eSystem Settings"));
        builder.button(plugin.getLanguageManager().getGuiColoredMessage(player, "gui.admin-gui.bedrock-back", "&cBack"));

        builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
            switch (response.clickedButtonId()) {
                case 0: openGuildListManagement(player); break;
                case 1: openEconomyManagement(player); break;
                case 2: openRelationManagement(player); break;
                case 3: openStatistics(player); break;
                case 4: openSystemSettings(player); break;
                case 5: plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player)); break;
            }
        }));

        builder.closedResultHandler(response -> {});

        BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
        return true;
    }

    @Override
    public void onClose(Player player) {
        // 关闭时的处理
    }
    
    @Override
    public void refresh(Player player) {
        // 刷新GUI
    }
}
