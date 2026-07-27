package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.language.LanguageManager;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildRelation;

import org.geysermc.cumulus.form.SimpleForm;
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

    // ── 图像模式功能常量 ──
    public static final String FUNC_CREATE_RELATION = "CREATE_RELATION";
    public static final String FUNC_PAGE_INFO = "PAGE_INFO";
    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_BACK = "BACK";

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;
    private int currentPage = 0;
    private final int itemsPerPage = 28; // 每页显示28个关系 (7列 × 4行)
    private List<GuildRelation> relations = new ArrayList<>();

    public GuildRelationsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.guild-relations.title", "&6工会关系"));
    }

    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 加载关系数据
        loadRelations().thenAccept(relationsList -> {
            this.relations = relationsList;
            
            // 确保在玩家区域线程中执行GUI操作（Folia 兼容）
            CompatibleScheduler.runTask(plugin, player, () -> {
                // 显示关系列表
                displayRelations(inventory);
                
                // 添加功能按钮
                addFunctionButtons(inventory);
                
                // 添加分页按钮
                addPaginationButtons(inventory);

                plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
            });
        });
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        // 功能按钮槽位检测
        if (slot == 49) { // 返回按钮
            MainGuildGUI mainGUI = new MainGuildGUI(plugin, player);
            plugin.getGuiManager().openGUI(player, mainGUI);
            return;
        }

        if (slot == 45) { // 创建关系按钮
            openCreateRelationGUI(player);
            return;
        }

        // 分页按钮
        if (slot == 48) { // 上一页
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
            return;
        }

        if (slot == 50) { // 下一页
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
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-relations.relation-type", "关系类型") + ": " + color + type.getDisplayName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-relations.status", "状态") + ": " + getStatusColor(status) + status.getDisplayName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-relations.initiator", "发起人") + ": " + relation.getInitiatorName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-relations.created-time", "创建时间") + ": " + formatDateTime(relation.getCreatedAt())));

        if (relation.getExpiresAt() != null) {
            lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-relations.expires-time", "过期时间") + ": " + formatDateTime(relation.getExpiresAt())));
        }

        lore.add("");

        // 根据关系类型和状态添加操作提示
        if (status == GuildRelation.RelationStatus.PENDING) {
            if (relation.getInitiatorUuid().equals(player.getUniqueId())) {
                lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(player, "gui.guild-relations.right-cancel", "右键: 取消关系")));
            } else {
                lore.add(ColorUtils.colorize("&a" + languageManager.getGuiMessage(player, "gui.guild-relations.left-accept", "左键: 接受关系")));
                lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(player, "gui.guild-relations.right-reject", "右键: 拒绝关系")));
            }
        } else if (status == GuildRelation.RelationStatus.ACTIVE) {
            if (type == GuildRelation.RelationType.TRUCE) {
                lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(player, "gui.guild-relations.left-end-truce", "左键: 结束停战")));
            } else if (type == GuildRelation.RelationType.WAR) {
                lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(player, "gui.guild-relations.left-propose-truce", "左键: 提议停战")));
            } else {
                lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(player, "gui.guild-relations.right-delete", "右键: 删除关系")));
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
        if (dateTime == null) return languageManager.getGuiMessage(player, "gui.guild-relations.unknown", "未知");
        return dateTime.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }
    
    /**
     * 添加功能按钮
     */
    private void addFunctionButtons(Inventory inventory) {
        // 创建关系按钮
        ItemStack createRelation = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&a" + languageManager.getGuiMessage(player, "gui.guild-relations.create-relation", "创建关系")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-relations.create-relation-desc", "创建新的工会关系")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-relations.types", "盟友、敌对、开战等"))
        );
        inventory.setItem(45, createRelation);
    }
    
    /**
     * 添加分页按钮
     */
    private void addPaginationButtons(Inventory inventory) {
        int maxPage = (relations.size() - 1) / itemsPerPage;

        // 上一页按钮 (槽位48)
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&c" + languageManager.getGuiMessage(player, "gui.common.previous-page", "上一页")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.view-previous", "查看上一页"))
            );
            inventory.setItem(48, previousPage);
        }

        // 下一页按钮 (槽位50)
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&a" + languageManager.getGuiMessage(player, "gui.common.next-page", "下一页")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.view-next", "查看下一页"))
            );
            inventory.setItem(50, nextPage);
        }

        // 返回按钮 (槽位49)
        ItemStack backButton = createItem(
            Material.ARROW,
            ColorUtils.colorize("&c" + languageManager.getGuiMessage(player, "gui.common.back", "返回")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-relations.back-to-menu", "返回主菜单"))
        );
        inventory.setItem(49, backButton);

        // 页码显示 (槽位46)
        ItemStack pageInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize("&e" + languageManager.getGuiMessage(player, "gui.guild-relations.page-info", "第 {current} 页", "{current}", String.valueOf(currentPage + 1))),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-relations.total-pages", "共 {total} 页", "{total}", String.valueOf(maxPage + 1))),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-relations.total-relations", "总计 {count} 个关系", "{count}", String.valueOf(relations.size())))
        );
        inventory.setItem(46, pageInfo);
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
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.accept-success", "&a已接受与 {guild} 的关系！", "{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.accept-failed", "&c接受关系失败！");
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
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.reject-success", "&c已拒绝与 {guild} 的关系！", "{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.reject-failed", "&c拒绝关系失败！");
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
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.cancel-success", "&c已取消与 {guild} 的关系！", "{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.cancel-failed", "&c取消关系失败！");
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
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    // 删除旧的停战关系
                    plugin.getGuildService().deleteGuildRelationAsync(relation.getId());

                    String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.truce-end", "&a与 {guild} 的停战已结束，关系转为中立！", "{guild}", relation.getOtherGuildName(guild.getId()));
                    player.sendMessage(ColorUtils.colorize(message));
                    refreshInventory(player);
                } else {
                    String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.truce-end-failed", "&c结束停战失败！");
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
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.truce-proposed", "&e已向 {guild} 提议停战！", "{guild}", relation.getOtherGuildName(guild.getId()));
                    player.sendMessage(ColorUtils.colorize(message));
                    refreshInventory(player);
                } else {
                    String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.truce-propose-failed", "&c提议停战失败！");
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
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.delete-success", "&a已删除与 {guild} 的关系！", "{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = languageManager.getGuiMessage(player, "gui.guild-relations.relations.delete-failed", "&c删除关系失败！");
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
    
    // ── 基岩版表单 ──

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;
        sendBedrockRelationList(player, 0);
        return true;
    }

    private void sendBedrockRelationList(Player player, int page) {
        loadRelations().thenAccept(relationsList -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (relationsList == null || relationsList.isEmpty()) {
                    SimpleForm form = SimpleForm.builder()
                        .title("§6工会关系")
                        .content("§f当前没有任何工会关系")
                        .button("§a创建关系")
                        .button("§c返回")
                        .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                            if (response.clickedButtonId() == 0) {
                                plugin.getGuiManager().openGUI(player, new CreateRelationGUI(plugin, guild, player));
                            } else {
                                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                            }
                        }))
                        .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                            plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player))))
                        .build();
                    BedrockFormSender.sendForm(player.getUniqueId(), form);
                    return;
                }

                final int bedrockPerPage = 10;
                int totalPages = (relationsList.size() - 1) / bedrockPerPage;
                final int safePage = Math.max(0, Math.min(page, totalPages));
                final int startIndex = safePage * bedrockPerPage;
                int endIndex = Math.min(startIndex + bedrockPerPage, relationsList.size());
                final int relCount = endIndex - startIndex;

                SimpleForm.Builder builder = SimpleForm.builder()
                    .title("§6工会关系 - 第" + (safePage + 1) + "页")
                    .content("§f共" + relationsList.size() + "个关系");

                for (int i = startIndex; i < endIndex; i++) {
                    GuildRelation rel = relationsList.get(i);
                    String otherName = rel.getOtherGuildName(guild.getId());
                    String color = rel.getType().getColor();
                    builder.button(ColorUtils.colorize(color + otherName + " - " + rel.getType().getDisplayName()));
                }

                builder.button("§a创建关系");
                builder.button("§e上一页");
                builder.button("§e下一页");
                builder.button("§c返回");

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int clicked = response.clickedButtonId();
                    if (clicked < relCount) {
                        GuildRelation rel = relationsList.get(startIndex + clicked);
                        sendBedrockRelationActions(player, rel);
                    } else if (clicked == relCount) {
                        plugin.getGuiManager().openGUI(player, new CreateRelationGUI(plugin, guild, player));
                    } else if (clicked == relCount + 1) {
                        sendBedrockRelationList(player, safePage - 1);
                    } else if (clicked == relCount + 2) {
                        sendBedrockRelationList(player, safePage + 1);
                    } else {
                        plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                    }
                }));

                builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                    plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player))));

                BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
            });
        });
    }

    private void sendBedrockRelationActions(Player player, GuildRelation relation) {
        String otherName = relation.getOtherGuildName(guild.getId());
        GuildRelation.RelationStatus status = relation.getStatus();
        GuildRelation.RelationType type = relation.getType();

        SimpleForm.Builder builder = SimpleForm.builder()
            .title("§6关系操作 - " + otherName)
            .content(ColorUtils.colorize("§f类型: " + type.getDisplayName() + "\n§f状态: " + status.getDisplayName()));

        List<String> actionTypes = new ArrayList<>();

        if (status == GuildRelation.RelationStatus.PENDING) {
            if (relation.getInitiatorUuid().equals(player.getUniqueId())) {
                builder.button("§c取消关系");
                actionTypes.add("cancel");
            } else {
                builder.button("§a接受关系");
                actionTypes.add("accept");
                builder.button("§c拒绝关系");
                actionTypes.add("reject");
            }
        } else if (status == GuildRelation.RelationStatus.ACTIVE) {
            if (type == GuildRelation.RelationType.TRUCE) {
                builder.button("§e结束停战");
                actionTypes.add("end_truce");
            } else if (type == GuildRelation.RelationType.WAR) {
                builder.button("§e提议停战");
                actionTypes.add("propose_truce");
            } else {
                builder.button("§c删除关系");
                actionTypes.add("delete");
            }
        }

        builder.button("§e返回列表");

        final int actionCount = actionTypes.size();
        builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
            int clicked = response.clickedButtonId();
            if (clicked < actionCount) {
                switch (actionTypes.get(clicked)) {
                    case "accept" -> bedrockUpdateRelation(player, relation, GuildRelation.RelationStatus.ACTIVE, "&a已接受与 " + otherName + " 的关系！");
                    case "reject" -> bedrockUpdateRelation(player, relation, GuildRelation.RelationStatus.CANCELLED, "&c已拒绝与 " + otherName + " 的关系！");
                    case "cancel" -> bedrockUpdateRelation(player, relation, GuildRelation.RelationStatus.CANCELLED, "&c已取消与 " + otherName + " 的关系！");
                    case "end_truce" -> bedrockEndTruce(player, relation);
                    case "propose_truce" -> bedrockProposeTruce(player, relation);
                    case "delete" -> bedrockDeleteRelation(player, relation);
                }
            } else {
                sendBedrockRelationList(player, 0);
            }
        }));

        builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
            sendBedrockRelationList(player, 0)));

        BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
    }

    private void bedrockUpdateRelation(Player player, GuildRelation relation, GuildRelation.RelationStatus newStatus, String successMsg) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), newStatus).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    player.sendMessage(ColorUtils.colorize(successMsg));
                    sendBedrockRelationList(player, 0);
                } else {
                    player.sendMessage(ColorUtils.colorize("&c操作失败！"));
                }
            });
        });
    }

    private void bedrockEndTruce(Player player, GuildRelation relation) {
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
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    plugin.getGuildService().deleteGuildRelationAsync(relation.getId());
                    player.sendMessage(ColorUtils.colorize("&a与 " + relation.getOtherGuildName(guild.getId()) + " 的停战已结束！"));
                    sendBedrockRelationList(player, 0);
                } else {
                    player.sendMessage(ColorUtils.colorize("&c结束停战失败！"));
                }
            });
        });
    }

    private void bedrockProposeTruce(Player player, GuildRelation relation) {
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
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    player.sendMessage(ColorUtils.colorize("&e已向 " + relation.getOtherGuildName(guild.getId()) + " 提议停战！"));
                    sendBedrockRelationList(player, 0);
                } else {
                    player.sendMessage(ColorUtils.colorize("&c提议停战失败！"));
                }
            });
        });
    }

    private void bedrockDeleteRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    player.sendMessage(ColorUtils.colorize("&a已删除与 " + relation.getOtherGuildName(guild.getId()) + " 的关系！"));
                    sendBedrockRelationList(player, 0);
                } else {
                    player.sendMessage(ColorUtils.colorize("&c删除关系失败！"));
                }
            });
        });
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
            meta.setDisplayName(ColorUtils.colorize(name));
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
