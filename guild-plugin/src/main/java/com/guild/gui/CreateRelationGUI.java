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
    private static final int[] TARGET_GUILD_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private List<Guild> availableGuilds = new ArrayList<>();
    
    public CreateRelationGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.player = player;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-relation.title",
                "&6创建工会关系"));
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

        // 返回按钮
        if (slot == 49) {
            GuildRelationsGUI relationsGUI = new GuildRelationsGUI(plugin, guild, player);
            plugin.getGuiManager().openGUI(player, relationsGUI);
            return;
        }

        // 确认创建按钮
        if (slot == 45) {
            if (selectedType != null && targetGuildName != null) {
                createRelation(player);
            } else {
                String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.select-both", "&c请先选择关系类型和目标工会！");
                player.sendMessage(ColorUtils.colorize(message));
            }
            return;
        }

        // 分页按钮
        if (slot == 52) {
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
            return;
        }

        if (slot == 53) {
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

        // 目标工会选择
        if (isTargetGuildSlot(slot)) {
            int slotIndex = getTargetGuildSlotIndex(slot);
            if (slotIndex >= 0) {
                int guildIndex = currentPage * itemsPerPage + slotIndex;
                if (guildIndex < availableGuilds.size()) {
                    Guild targetGuild = availableGuilds.get(guildIndex);
                    targetGuildName = targetGuild.getName();
                    refreshInventory(player);

                    String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.target-selected", "&a已选择目标工会: {guild}", "{guild}", targetGuildName);
                    player.sendMessage(ColorUtils.colorize(message));
                }
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
            lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.relation-type", "关系类型") + ": " + color + type.getDisplayName(lang)));

            // 添加关系类型描述
            String descKey = "relations.type." + type.name().toLowerCase() + ".description";
            lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, descKey, "")));

            if (selectedType == type) {
                lore.add(ColorUtils.colorize("&a✓ " + languageManager.getGuiMessage(player, "gui.common.selected", "已选择")));
            } else {
                lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(player, "gui.common.click-to-select", "点击选择")));
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
            int slot = TARGET_GUILD_SLOTS[i - startIndex];

            Material material = Material.SHIELD;
            String displayName = ColorUtils.colorize("&f" + targetGuild.getName());

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.guild-name", "工会名称") + ": " + targetGuild.getName()));
            if (targetGuild.getTag() != null && !targetGuild.getTag().isEmpty()) {
                lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.guild-tag", "工会标签") + ": [" + targetGuild.getTag() + "]"));
            }
            lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.leader", "会长") + ": " + targetGuild.getLeaderName()));

            if (targetGuildName != null && targetGuildName.equals(targetGuild.getName())) {
                lore.add(ColorUtils.colorize("&a✓ " + languageManager.getGuiMessage(player, "gui.common.selected", "已选择")));
            } else {
                lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(player, "gui.common.click-to-select", "点击选择")));
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
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-relation.confirm-button", "&a确认创建")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-relation.confirm-lore-1", "&7创建工会关系")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-relation.confirm-lore-2", "&7需要先选择关系类型和目标工会"))
        );
        inventory.setItem(45, confirmButton);

        // 当前选择显示
        List<String> selectionLore = new ArrayList<>();
        String typeText = selectedType != null ?
            selectedType.getColor() + selectedType.getDisplayName(lang) :
            "&c" + languageManager.getGuiMessage(player, "gui.common.not-selected", "未选择");
        selectionLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.relation-type", "关系类型") + ": " + typeText));
        selectionLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.target-guild", "目标工会") + ": " +
            (targetGuildName != null ? "&a" + targetGuildName : "&c" + languageManager.getGuiMessage(player, "gui.common.not-selected", "未选择"))));

        ItemStack selectionInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-relation.current-selection", "&e当前选择")),
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
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.previous-page", "&c上一页")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.view-previous", "&7查看上一页"))
            );
            inventory.setItem(52, previousPage);
        }

        // 下一页按钮
        int maxPage = (availableGuilds.size() - 1) / itemsPerPage;
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.next-page", "&a下一页")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.view-next", "&7查看下一页"))
            );
            inventory.setItem(53, nextPage);
        }

        // 返回按钮
        ItemStack backButton = createItem(
            Material.ARROW,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.back", "&7返回")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-relation.back-lore", "&7返回关系管理"))
        );
        inventory.setItem(49, backButton);

        // 页码显示
        ItemStack pageInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize(languageManager.getGuiIndexedMessage(player, "gui.common.page-info", "&e第 {0} 页，共 {1} 页", String.valueOf(currentPage + 1), String.valueOf(maxPage + 1))),
            ColorUtils.colorize(languageManager.getGuiIndexedMessage(player, "gui.common.total-guilds", "&7总计 {0} 个工会", String.valueOf(availableGuilds.size())))
        );
        inventory.setItem(51, pageInfo);
    }
    
    private boolean isTargetGuildSlot(int slot) {
        for (int targetSlot : TARGET_GUILD_SLOTS) {
            if (targetSlot == slot) {
                return true;
            }
        }
        return false;
    }

    private int getTargetGuildSlotIndex(int slot) {
        for (int i = 0; i < TARGET_GUILD_SLOTS.length; i++) {
            if (TARGET_GUILD_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
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
            String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.type-selected", "&a已选择关系类型: {type}", "{type}", selectedType.getDisplayName(lang));
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
            String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.target-not-found", "&c目标工会不存在！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查是否已有关系
        plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild[0].getId())
            .thenAccept(existingRelation -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (existingRelation != null) {
                        String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.already-exists", "&c与 {guild} 的关系已存在！", "{guild}", targetGuildName);
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
                                String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.create-success", "&a已向 {guild} 发送 {type} 关系请求！", "{guild}", targetGuildName, "{type}", selectedType.getDisplayName());
                                player.sendMessage(ColorUtils.colorize(message));

                                // 返回关系管理界面
                                GuildRelationsGUI relationsGUI = new GuildRelationsGUI(plugin, guild, player);
                                plugin.getGuiManager().openGUI(player, relationsGUI);
                            } else {
                                String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.create-failed", "&c创建关系失败！");
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
