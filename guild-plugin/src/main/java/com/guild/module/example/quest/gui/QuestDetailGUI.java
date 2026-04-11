package com.guild.module.example.quest.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.module.example.quest.GuildQuestModule;
import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestObjective;
import com.guild.module.example.quest.model.QuestProgress;
import com.guild.module.example.quest.model.QuestReward;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 任务详情GUI - 完全重写版本
 * 
 * 设计原则：
 * 1. 构造函数只接收预处理好的数据，不做任何业务逻辑
 * 2. setupInventory()采用纯防御性编程，确保100%不会崩溃
 * 3. 所有数据访问都有null保护和默认值
 */
public class QuestDetailGUI extends AbstractModuleGUI {
    
    // 必要的模块引用（仅用于回调）
    private final GuildQuestModule module;
    
    // 预加载的数据（全部可能为null，必须检查）
    private final String questId;
    private final String questName;
    private final String questDescription;
    private final QuestDefinition.QuestType questType;
    private final int minGuildLevel;
    private final boolean isRepeatable;
    private final List<QuestObjective> objectives;
    private final List<QuestReward> rewards;
    
    // 进度数据（可能为null）
    private final boolean hasProgress;
    private final int[] objectiveProgress;  // 可能为null或空数组
    private final boolean isCompleted;
    private final boolean isClaimed;
    
    // 元数据
    private final int guildId;
    private final UUID playerUuid;
    
    /**
     * 私有构造函数 - 只接收预处理好的数据
     */
    private QuestDetailGUI(Builder builder) {
        super();
        this.module = builder.module;
        this.questId = builder.questId;
        this.questName = builder.questName != null ? builder.questName : "未知任务";
        this.questDescription = builder.questDescription != null ? builder.questDescription : "暂无描述";
        this.questType = builder.questType != null ? builder.questType : QuestDefinition.QuestType.DAILY;
        this.minGuildLevel = builder.minGuildLevel;
        this.isRepeatable = builder.isRepeatable;
        this.objectives = builder.objectives != null ? builder.objectives : new ArrayList<>();
        this.rewards = builder.rewards != null ? builder.rewards : new ArrayList<>();
        
        this.hasProgress = builder.hasProgress;
        this.objectiveProgress = builder.objectiveProgress;  // 允许为null
        this.isCompleted = builder.isCompleted;
        this.isClaimed = builder.isClaimed;
        
        this.guildId = builder.guildId;
        this.playerUuid = builder.playerUuid;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6&l任务详情 - " + questName);
    }
    
    @Override
    public void setupInventory(Inventory inv) {
        fillBorder(inv);
        fillInteriorSlots(inv);
        
        try {
            renderHeader(inv);
            renderStatus(inv);
            renderObjectives(inv);
            renderRewards(inv);
            renderActionButtons(inv);
        } catch (Exception e) {
            module.getContext().getLogger().severe("[Quest-Detail] 渲染GUI时发生严重错误: " + e.getMessage());
            
            // 显示错误提示界面
            inv.setItem(13, createItem(Material.BARRIER,
                "&c&l[ 渲染错误 ]",
                "",
                "&7任务ID: &f" + questId,
                "&7错误: &c" + (e.getMessage() != null ? e.getMessage() : "未知错误"),
                "",
                "&7请联系管理员"));
        }
    }
    
    /**
     * 渲染头部信息（任务名称、类型、描述等）
     */
    private void renderHeader(Inventory inv) {
        String typeColor = getTypeColor();
        String typeName = getTypeName();
        Material icon = getIcon();
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7类型: " + typeColor + typeName);
        lore.add("&7最低公会等级: &f" + minGuildLevel);
        lore.add("&7可重复完成: " + (isRepeatable ? "&a是" : "&c否"));
        lore.add("");
        if (questDescription != null && !questDescription.isEmpty()) {
            lore.add(questDescription);
        }
        
        inv.setItem(4, createItem(icon,
            typeColor + "&l" + questName,
            lore.toArray(new String[0])));
    }
    
    /**
     * 渲染状态信息（进度、状态文字）
     */
    private void renderStatus(Inventory inv) {
        String statusText = getStatusText();
        String progressText = getProgressText();
        String hintText = getHintText();
        
        inv.setItem(13, createItem(Material.PAPER,
            statusText,
            "",
            progressText,
            hintText));
    }
    
    /**
     * 渲染目标列表
     */
    private void renderObjectives(Inventory inv) {
        if (objectives == null || objectives.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < Math.min(objectives.size(), 14); i++) {
            QuestObjective obj = objectives.get(i);
            if (obj == null) continue;
            
            int current = getCurrentProgress(i);
            int target = obj.getTarget();
            double pct = calculatePercent(current, target);
            boolean done = current >= target && target > 0;
            
            int slot = mapObjectiveToSlot(i);
            if (slot == -1) continue;
            
            String bar = buildProgressBar(pct);
            
            inv.setItem(slot, createItem(
                done ? Material.LIME_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE,
                (done ? "&a" : "&e") + getObjectiveDisplayName(obj),
                "",
                "&7目标: &f" + getObjectiveDescription(obj),
                "&7进度: " + (done ? "&a" : "&e") + current + "/" + target + 
                    " (" + String.format("%.0f", pct) + "%)",
                bar));
        }
    }
    
    /**
     * 渲染奖励列表
     */
    private void renderRewards(Inventory inv) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }
        
        List<String> rewardLore = new ArrayList<>();
        rewardLore.add("");
        for (QuestReward r : rewards) {
            if (r == null) continue;
            rewardLore.add("&7" + getRewardDisplayName(r) + ": &f+" + String.format("%.0f", r.getAmount()));
        }
        rewardLore.add("");
        rewardLore.add("&8┃ 来自 quest 模块配置");
        
        inv.setItem(31, createItem(Material.DIAMOND,
            "&b&l任务奖励",
            rewardLore.toArray(new String[0])));
    }
    
    /**
     * 渲染操作按钮（接取/取消/领取）
     */
    private void renderActionButtons(Inventory inv) {
        if (canAccept()) {
            inv.setItem(40, createItem(Material.EMERALD,
                "&a&l[ 接取此任务 ]",
                "",
                "&7确认后开始追踪进度",
                "&8┃ 点击接取"));
        } else if (isCompleted && !isClaimed) {
            inv.setItem(40, createItem(Material.GOLD_INGOT,
                "&e&l[ 领取奖励 ]",
                "",
                "&7所有目标已完成，领取你的奖励",
                "&8┃ 点击领取"));
        } else if (isClaimed) {
            inv.setItem(40, createItem(Material.GRAY_DYE,
                "&7&l已领取奖励",
                "",
                "&7该任务已完成并领取了奖励"));
        } else if (hasProgress) {
            inv.setItem(40, createItem(Material.REDSTONE,
                "&c&l[ 取消任务 ]",
                "",
                "&7放弃当前进度（不可恢复）",
                "&8┃ 点击取消"));
        } else {
            inv.setItem(40, createItem(Material.BARRIER,
                "&c&l[ 无法接取 ]",
                "",
                getCannotAcceptReason(),
                "&7请检查条件后重试"));
        }
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 40 && clickType.isLeftClick()) {
            handleActionButtonClick(player);
        }
    }
    
    /**
     * 处理操作按钮点击
     */
    private void handleActionButtonClick(Player player) {
        if (canAccept()) {
            handleAccept(player);
        } else if (isCompleted && !isClaimed) {
            handleClaimReward(player);
        } else if (hasProgress && !isCompleted) {
            handleCancel(player);
        }
    }
    
    private void handleAccept(Player player) {
        if (guildId <= 0) {
            player.sendMessage(ColorUtils.colorize("&c[Quest] 公会ID无效，无法接取任务! (guildId=" + guildId + ")"));
            module.getContext().getLogger().warning("[Quest-Detail] ⚠️ 拒绝接取: guildId=" + guildId +
                ", questId=" + questId + ", player=" + player.getName());
            return;
        }

        QuestDefinition definition = module.getQuestManager().getDefinition(questId);
        if (definition == null) {
            player.sendMessage(ColorUtils.colorize("&c[Quest] 任务定义不存在!"));
            return;
        }
        
        QuestProgress newProgress = new QuestProgress(
            definition.getId(), playerUuid, player.getName(), guildId,
            definition.getObjectives().size());
        
        if (module.getQuestManager().acceptQuest(newProgress)) {
            module.getContext().sendMessage(player, "quest.accepted",
                "&a[Quest] 已接取任务: &f" + questName);
            
            notifyOtherGUIsRefresh();
            forceRefreshContent(player);
        }
    }
    
    private void handleClaimReward(Player player) {
        if (guildId <= 0) {
            player.sendMessage(ColorUtils.colorize("&c[Quest] 公会ID无效，无法领取奖励!"));
            return;
        }

        QuestDefinition definition = module.getQuestManager().getDefinition(questId);
        QuestProgress progress = module.getQuestManager()
            .getPlayerQuest(guildId, playerUuid, questId);
        
        if (definition == null || progress == null) {
            player.sendMessage(ColorUtils.colorize("&c[Quest] 无法获取任务数据!"));
            return;
        }
        
        module.getRewardHandler().grantRewards(player, definition, progress);
        module.getQuestManager().saveGuildProgress(guildId);
        module.getContext().getEventBus().publish(
            new GuildQuestModule.QuestCompletedEvent(
                player.getName(), questName, guildId));
        module.getContext().sendMessage(player, "quest.claimed", "&a[Quest] 奖励已发放!");
        
        notifyOtherGUIsRefresh();
        forceRefreshContent(player);
    }
    
    private void handleCancel(Player player) {
        player.sendMessage(ColorUtils.colorize("&c[Quest] 取消功能待实现"));
    }
    
    // ==================== 辅助方法 ====================
    
    private String getTypeColor() {
        if (questType == null) return "&7";
        switch (questType) {
            case DAILY: return "&e";
            case WEEKLY: return "&6";
            case ONE_TIME: return "&c";
            default: return "&7";
        }
    }
    
    private String getTypeName() {
        if (questType == null) return "未知";
        switch (questType) {
            case DAILY: return "每日任务";
            case WEEKLY: return "每周任务";
            case ONE_TIME: return "一次性任务";
            default: return "未知类型";
        }
    }
    
    private Material getIcon() {
        if (questType == null) return Material.PAPER;
        switch (questType) {
            case DAILY: return Material.CLOCK;
            case WEEKLY: return Material.SUNFLOWER;
            case ONE_TIME: return Material.TOTEM_OF_UNDYING;
            default: return Material.PAPER;
        }
    }
    
    private String getStatusText() {
        if (isClaimed) return "&a&l已领取";
        if (isCompleted) return "&e&l已完成 (待领取)";
        if (hasProgress) return "&6&l进行中";
        if (canAccept()) return "&a&l可接取";
        return "&c&l不可接取";
    }
    
    private String getProgressText() {
        if (!hasProgress || objectiveProgress == null) {
            return "";
        }
        
        try {
            QuestDefinition def = module.getQuestManager().getDefinition(questId);
            if (def != null) {
                return "&7进度: &f" + String.format("%.1f%%", def.getProgressPercent(objectiveProgress));
            }
        } catch (Exception ignored) {}
        
        return "";
    }
    
    private String getHintText() {
        if (isCompleted && !isClaimed) return "&e点击领取奖励";
        return "";
    }
    
    private int getCurrentProgress(int index) {
        if (!hasProgress || objectiveProgress == null || index < 0 || index >= objectiveProgress.length) {
            return 0;
        }
        return Math.max(0, objectiveProgress[index]);
    }
    
    private double calculatePercent(int current, int target) {
        if (target <= 0) return 0;
        return Math.min(100.0, (double) current / target * 100.0);
    }
    
    private String getObjectiveDisplayName(QuestObjective obj) {
        if (obj == null || obj.getType() == null) return "未知目标";
        return obj.getType().getDisplayName();
    }
    
    private String getObjectiveDescription(QuestObjective obj) {
        if (obj == null) return "无描述";
        return obj.getDescription() != null ? obj.getDescription() : "无描述";
    }
    
    private String getRewardDisplayName(QuestReward r) {
        if (r == null || r.getType() == null) return "未知";
        return r.getType().getDisplayName();
    }
    
    private boolean canAccept() {
        if (hasProgress || isCompleted || isClaimed) return false;
        
        try {
            QuestDefinition def = module.getQuestManager().getDefinition(questId);
            if (def == null) return false;
            
            return module.getQuestManager().canAccept(guildId, playerUuid, def);
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getCannotAcceptReason() {
        if (hasProgress) return "&c已接取此任务";
        
        try {
            var guild = module.getContext().getPlugin().getGuildService().getPlayerGuild(playerUuid);
            if (guild != null && guild.getLevel() < minGuildLevel) {
                return "&c需要公会等级: " + minGuildLevel + " (当前: &f" + guild.getLevel() + "&c)";
            }
        } catch (Exception ignored) {}
        
        return "&c无法接取此任务";
    }
    
    private void notifyOtherGUIsRefresh() {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("guildId", guildId);
        data.put("playerUuid", playerUuid);
        module.getContext().notifyGUIRefresh("quest-list", data);
        module.getContext().notifyGUIRefresh("quest-active-list", data);
    }
    
    private void forceRefreshContent(Player player) {
        // 关键修复：由于新架构使用final字段（不可变数据），
        // 必须重新打开整个GUI实例来刷新数据
        org.bukkit.Bukkit.getScheduler().runTaskLater(module.getContext().getPlugin(), () -> {
            if (player.isOnline()) {
                try {
                    // 重新获取最新的任务定义和进度
                    QuestDefinition def = module.getQuestManager().getDefinition(questId);
                    QuestProgress progress = null;
                    if (def != null && guildId > 0 && playerUuid != null) {
                        progress = module.getQuestManager().getPlayerQuest(guildId, playerUuid, questId);
                    }
                    
                    // 构建新的GUI实例（包含最新数据）
                    Map<String, Object> reopenData = new java.util.HashMap<>();
                    reopenData.put("definition", def);  // 可能是null，但Builder能处理
                    reopenData.put("guildId", guildId);
                    reopenData.put("playerUuid", playerUuid);
                    
                    // 重新打开GUI（这会替换当前GUI实例）
                    module.getContext().getApi().openCustomGUI("quest-detail", player, reopenData);
                    
                } catch (Exception e) {
                    module.getContext().getLogger().warning("[Quest-Detail] 刷新GUI时出错: " + e.getMessage());
                    
                    // 如果重新打开失败，尝试简单的inventory刷新作为后备方案
                    if (this.inventory != null) {
                        this.inventory.clear();
                        setupInventory(this.inventory);
                        player.updateInventory();
                    }
                }
            }
        }, 1L);  // 延迟1tick
    }
    
    private static int mapObjectiveToSlot(int index) {
        if (index >= 14) return -1;
        int[] slots = {19,20,21,28,29,30,37,38,39,46,47,48,51,52};
        return slots[index];
    }
    
    private static String buildProgressBar(double percent) {
        int filled = (int)(percent / 10);
        StringBuilder bar = new StringBuilder("&8");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "&a█" : "&7█");
        }
        return bar.toString();
    }
    
    // ==================== Builder模式 ====================
    
    /**
     * Builder类 - 用于安全地构建QuestDetailGUI实例
     * 所有数据都在这里预处理和验证
     */
    public static class Builder {
        private GuildQuestModule module;
        private String questId;
        private String questName;
        private String questDescription;
        private QuestDefinition.QuestType questType;
        private int minGuildLevel = 1;
        private boolean isRepeatable = false;
        private List<QuestObjective> objectives = new ArrayList<>();
        private List<QuestReward> rewards = new ArrayList<>();
        private boolean hasProgress = false;
        private int[] objectiveProgress;
        private boolean isCompleted = false;
        private boolean isClaimed = false;
        private int guildId;
        private UUID playerUuid;
        
        public Builder(GuildQuestModule module) {
            this.module = module;
        }
        
        public Builder fromDefinition(QuestDefinition def) {
            if (def == null) return this;
            
            this.questId = def.getId();
            this.questName = def.getName();
            this.questDescription = def.getDescription();
            this.questType = def.getType();
            this.minGuildLevel = def.getMinGuildLevel();
            this.isRepeatable = def.isRepeatable();
            this.objectives = def.getObjectives() != null ? def.getObjectives() : new ArrayList<>();
            this.rewards = def.getRewards() != null ? def.getRewards() : new ArrayList<>();
            return this;
        }
        
        public Builder fromProgress(QuestProgress progress) {
            if (progress == null) {
                this.hasProgress = false;
                return this;
            }
            
            this.hasProgress = true;
            this.objectiveProgress = progress.getObjectiveProgress();
            this.isCompleted = progress.isCompletedMarked();
            this.isClaimed = progress.isClaimed();
            return this;
        }
        
        public Builder withGuildInfo(int guildId, UUID playerUuid) {
            this.guildId = guildId;
            this.playerUuid = playerUuid;
            return this;
        }
        
        public QuestDetailGUI build() {
            if (questId == null) questId = "unknown_" + System.currentTimeMillis();
            if (playerUuid == null) playerUuid = new UUID(0, 0);
            return new QuestDetailGUI(this);
        }
    }
}
