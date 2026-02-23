package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 创建工会关系GUI
 */
public class CreateRelationGUI implements GUI {

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final Guild guild;
    private final Player player;
    private GuildRelation.RelationType selectedType = null;
    private String targetGuildName = null;
    private int currentPage = 0;
    private final int itemsPerPage = 28;
    private List<Guild> availableGuilds = new ArrayList<>();
    
    public CreateRelationGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.player = player;
    }
    
    @Override
    public String getTitle() {
        return languageManager.getGuiColoredMessage(player, "create-relation.title",
                ColorUtils.colorize("&6创建工会关系"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 加载可用工会列表
        loadAvailableGuilds().thenAccept(guilds -> {
            this.availableGuilds = guilds;
            
            // 确保在主线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, () -> {
                // 显示关系类型选择
                displayRelationTypes(inventory);
                
                // 显示目标工会选择
                displayTargetGuilds(inventory);
                
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
        String backText = languageManager.getGuiColoredMessage(player, "gui.back", "&7返回");
        String confirmText = languageManager.getGuiColoredMessage(player, "create-relation.confirm-button", "&a确认创建");
        String prevPageText = languageManager.getGuiColoredMessage(player, "gui.previous-page", "&c上一页");
        String nextPageText = languageManager.getGuiColoredMessage(player, "gui.next-page", "&a下一页");

        // 返回按钮
        if (itemName.contains(backText.substring(backText.length() - 2))) {
            GuildRelationsGUI relationsGUI = new GuildRelationsGUI(plugin, guild, player);
            plugin.getGuiManager().openGUI(player, relationsGUI);
            return;
        }

        // 确认创建按钮
        if (itemName.contains(confirmText.substring(confirmText.length() - 4))) {
            if (selectedType != null && targetGuildName != null) {
                createRelation(player);
            } else {
                String message = languageManager.getMessage(player, "relations.select-both", "&c请先选择关系类型和目标工会！");
                player.sendMessage(ColorUtils.colorize(message));
            }
            return;
        }

        // 分页按钮
        if (itemName.contains(prevPageText.substring(prevPageText.length() - 2))) {
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
            return;
        }

        if (itemName.contains(nextPageText.substring(nextPageText.length() - 2))) {
            int maxPage = (availableGuilds.size() - 1) / itemsPerPage;
            if (currentPage < maxPage) {
                currentPage++;
                refreshInventory(player);
            }
            return;
        }

        // 关系类型选择 (slot 0-8)
        if (slot >= 0 && slot < 9) {
            handleRelationTypeClick(player, slot);
            return;
        }

        // 目标工会选择 (slot 9-44)
        if (slot >= 9 && slot < 45) {
            int guildIndex = (currentPage * itemsPerPage) + (slot - 9);
            if (guildIndex < availableGuilds.size()) {
                Guild targetGuild = availableGuilds.get(guildIndex);
                targetGuildName = targetGuild.getName();
                refreshInventory(player);

                String message = languageManager.getMessage(player, "relations.target-selected", "&a已选择目标工会: {guild}", "{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
            }
        }
    }
    
    /**
     * 加载可用工会列表
     */
    private CompletableFuture<List<Guild>> loadAvailableGuilds() {
        return plugin.getGuildService().getAllGuildsAsync().thenApply(guilds -> {
            List<Guild> available = new ArrayList<>();
            for (Guild g : guilds) {
                if (!g.getName().equals(guild.getName())) {
                    available.add(g);
                }
            }
            return available;
        });
    }
    
    /**
     * 显示关系类型选择
     */
    private void displayRelationTypes(Inventory inventory) {
        GuildRelation.RelationType[] types = GuildRelation.RelationType.values();
        String lang = languageManager.getPlayerLanguage(player);

        for (int i = 0; i < types.length && i < 9; i++) {
            GuildRelation.RelationType type = types[i];
            Material material = getRelationTypeMaterial(type);
            String color = type.getColor();
            String displayName = ColorUtils.colorize(color + type.getDisplayName(lang));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7关系类型: " + color + type.getDisplayName(lang)));

            // 添加关系类型描述
            String descKey = "relations.type." + type.name().toLowerCase() + ".description";
            lore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, descKey, "")));

            if (selectedType == type) {
                lore.add(ColorUtils.colorize("&a✓ " + languageManager.getMessage(player, "gui.selected", "已选择")));
            } else {
                lore.add(ColorUtils.colorize("&e" + languageManager.getMessage(player, "gui.click-to-select", "点击选择")));
            }

            ItemStack item = createItem(material, displayName, lore.toArray(new String[0]));
            inventory.setItem(i, item);
        }
    }
    
    /**
     * 显示目标工会选择
     */
    private void displayTargetGuilds(Inventory inventory) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, availableGuilds.size());

        for (int i = startIndex; i < endIndex; i++) {
            Guild targetGuild = availableGuilds.get(i);
            int slot = 9 + (i - startIndex);

            Material material = Material.SHIELD;
            String displayName = ColorUtils.colorize("&f" + targetGuild.getName());

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "gui.guild-name", "工会名称") + ": " + targetGuild.getName()));
            if (targetGuild.getTag() != null && !targetGuild.getTag().isEmpty()) {
                lore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "gui.guild-tag", "工会标签") + ": [" + targetGuild.getTag() + "]"));
            }
            lore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "gui.leader", "会长") + ": " + targetGuild.getLeaderName()));

            if (targetGuildName != null && targetGuildName.equals(targetGuild.getName())) {
                lore.add(ColorUtils.colorize("&a✓ " + languageManager.getMessage(player, "gui.selected", "已选择")));
            } else {
                lore.add(ColorUtils.colorize("&e" + languageManager.getMessage(player, "gui.click-to-select", "点击选择")));
            }

            ItemStack item = createItem(material, displayName, lore.toArray(new String[0]));
            inventory.setItem(slot, item);
        }
    }
    
    /**
     * 添加功能按钮
     */
    private void addFunctionButtons(Inventory inventory) {
        String lang = languageManager.getPlayerLanguage(player);

        // 确认创建按钮
        ItemStack confirmButton = createItem(
            Material.EMERALD,
            ColorUtils.colorize(languageManager.getGuiColoredMessage(player, "create-relation.confirm-button", "&a确认创建")),
            ColorUtils.colorize(languageManager.getMessage(player, "create-relation.confirm-lore-1", "&7创建工会关系")),
            ColorUtils.colorize(languageManager.getMessage(player, "create-relation.confirm-lore-2", "&7需要先选择关系类型和目标工会"))
        );
        inventory.setItem(45, confirmButton);

        // 当前选择显示
        List<String> selectionLore = new ArrayList<>();
        String typeText = selectedType != null ?
            selectedType.getColor() + selectedType.getDisplayName(lang) :
            "&c" + languageManager.getMessage(player, "gui.not-selected", "未选择");
        selectionLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "gui.relation-type", "关系类型") + ": " + typeText));
        selectionLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "gui.target-guild", "目标工会") + ": " +
            (targetGuildName != null ? "&a" + targetGuildName : "&c" + languageManager.getMessage(player, "gui.not-selected", "未选择"))));

        ItemStack selectionInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize(languageManager.getGuiColoredMessage(player, "create-relation.current-selection", "&e当前选择")),
            selectionLore.toArray(new String[0])
        );
        inventory.setItem(47, selectionInfo);
    }
    
    /**
     * 添加分页按钮
     */
    private void addPaginationButtons(Inventory inventory) {
        // 上一页按钮
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiColoredMessage(player, "gui.previous-page", "&c上一页")),
                ColorUtils.colorize(languageManager.getMessage(player, "gui.view-previous", "&7查看上一页"))
            );
            inventory.setItem(18, previousPage);
        }

        // 下一页按钮
        int maxPage = (availableGuilds.size() - 1) / itemsPerPage;
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiColoredMessage(player, "gui.next-page", "&a下一页")),
                ColorUtils.colorize(languageManager.getMessage(player, "gui.view-next", "&7查看下一页"))
            );
            inventory.setItem(26, nextPage);
        }

        // 返回按钮
        ItemStack backButton = createItem(
            Material.ARROW,
            ColorUtils.colorize(languageManager.getGuiColoredMessage(player, "gui.back", "&7返回")),
            ColorUtils.colorize(languageManager.getMessage(player, "create-relation.back-lore", "&7返回关系管理"))
        );
        inventory.setItem(49, backButton);

        // 页码显示
        ItemStack pageInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize(languageManager.getMessage(player, "gui.page-info", "&e第 {current} 页", "{current}", String.valueOf(currentPage + 1))),
            ColorUtils.colorize(languageManager.getMessage(player, "gui.total-pages", "&7共 {total} 页", "{total}", String.valueOf(maxPage + 1))),
            ColorUtils.colorize(languageManager.getMessage(player, "gui.total-guilds", "&7总计 {count} 个工会", "{count}", String.valueOf(availableGuilds.size())))
        );
        inventory.setItem(22, pageInfo);
    }
    
    /**
     * 处理关系类型点击
     */
    private void handleRelationTypeClick(Player player, int slot) {
        GuildRelation.RelationType[] types = GuildRelation.RelationType.values();
        if (slot < types.length) {
            selectedType = types[slot];
            refreshInventory(player);

            String lang = languageManager.getPlayerLanguage(player);
            String message = languageManager.getMessage(player, "relations.type-selected", "&a已选择关系类型: {type}", "{type}", selectedType.getDisplayName(lang));
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    /**
     * 创建关系
     */
    private void createRelation(Player player) {
        // 查找目标工会
        final Guild[] targetGuild = {null};
        for (Guild g : availableGuilds) {
            if (g.getName().equals(targetGuildName)) {
                targetGuild[0] = g;
                break;
            }
        }
        
        if (targetGuild[0] == null) {
            String message = languageManager.getMessage(player, "relations.target-not-found", "&c目标工会不存在！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查是否已有关系
        plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild[0].getId())
            .thenAccept(existingRelation -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (existingRelation != null) {
                        String message = languageManager.getMessage(player, "relations.already-exists", "&c与 {guild} 的关系已存在！", "{guild}", targetGuildName);
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }

                    // 创建新关系
                    plugin.getGuildService().createGuildRelationAsync(
                        guild.getId(), targetGuild[0].getId(),
                        guild.getName(), targetGuild[0].getName(),
                        selectedType, player.getUniqueId(), player.getName()
                    ).thenAccept(success -> {
                        CompatibleScheduler.runTask(plugin, () -> {
                            if (success) {
                                String message = languageManager.getMessage(player, "relations.create-success", "&a已向 {guild} 发送 {type} 关系请求！", "{guild}", targetGuildName, "{type}", selectedType.getDisplayName());
                                player.sendMessage(ColorUtils.colorize(message));

                                // 返回关系管理界面
                                GuildRelationsGUI relationsGUI = new GuildRelationsGUI(plugin, guild, player);
                                plugin.getGuiManager().openGUI(player, relationsGUI);
                            } else {
                                String message = languageManager.getMessage(player, "relations.create-failed", "&c创建关系失败！");
                                player.sendMessage(ColorUtils.colorize(message));
                            }
                        });
                    });
                });
            });
    }
    
    /**
     * 获取关系类型对应的材料
     */
    private Material getRelationTypeMaterial(GuildRelation.RelationType type) {
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
     * 刷新库存
     */
    private void refreshInventory(Player player) {
        CreateRelationGUI newGUI = new CreateRelationGUI(plugin, guild, player);
        newGUI.selectedType = this.selectedType;
        newGUI.targetGuildName = this.targetGuildName;
        newGUI.currentPage = this.currentPage;
        plugin.getGuiManager().openGUI(player, newGUI);
    }
    
    /**
     * 填充边框
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
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
