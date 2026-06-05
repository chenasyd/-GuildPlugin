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
 * Quest Detail GUI - fully rewritten version.
 * Design principles: constructor only takes preprocessed data; setupInventory uses defensive coding.
 */
public class QuestDetailGUI extends AbstractModuleGUI {
    
    // Module reference (for callbacks only)
    private final GuildQuestModule module;
    
    // Preloaded data (may be null, must check)
    private final String questId;
    private final String questName;
    private final String questDescription;
    private final QuestDefinition.QuestType questType;
    private final int minGuildLevel;
    private final boolean isRepeatable;
    private final List<QuestObjective> objectives;
    private final List<QuestReward> rewards;
    
    // Progress data (may be null)
    private final boolean hasProgress;
    private final int[] objectiveProgress;  // May be null or empty array
    private final boolean isCompleted;
    private final boolean isClaimed;
    
    // Metadata
    private final int guildId;
    private final UUID playerUuid;
    
    /**
     * Private constructor - only receives preprocessed data
     */
    private QuestDetailGUI(Builder builder) {
        super();
        this.module = builder.module;
        this.questId = builder.questId;
        this.questName = builder.questName != null ? builder.questName : "Unknown Quest";
        this.questDescription = builder.questDescription != null ? builder.questDescription : "No description";
        this.questType = builder.questType != null ? builder.questType : QuestDefinition.QuestType.DAILY;
        this.minGuildLevel = builder.minGuildLevel;
        this.isRepeatable = builder.isRepeatable;
        this.objectives = builder.objectives != null ? builder.objectives : new ArrayList<>();
        this.rewards = builder.rewards != null ? builder.rewards : new ArrayList<>();
        
        this.hasProgress = builder.hasProgress;
        this.objectiveProgress = builder.objectiveProgress;  // Allow null
        this.isCompleted = builder.isCompleted;
        this.isClaimed = builder.isClaimed;
        
        this.guildId = builder.guildId;
        this.playerUuid = builder.playerUuid;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6&lQuest Details - " + questName);
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
            module.getContext().getLogger().severe("[Quest-Detail] Critical render error: " + e.getMessage());
            
            // Show error UI
            inv.setItem(13, createItem(Material.BARRIER,
                "&c&l[ Render Error ]",
                "",
                "&7Quest ID: &f" + questId,
                "&7Error: &c" + (e.getMessage() != null ? e.getMessage() : "Unknown error"),
                "",
                "&7Please contact an administrator"));
        }
    }
    
    /**
     * Render header info (quest name, type, description, etc.)
     */
    private void renderHeader(Inventory inv) {
        String typeColor = getTypeColor();
        String typeName = getTypeName();
        Material icon = getIcon();
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Type: " + typeColor + typeName);
        lore.add("&7Min Guild Level: &f" + minGuildLevel);
        lore.add("&7Repeatable: " + (isRepeatable ? "&aYes" : "&cNo"));
        lore.add("");
        if (questDescription != null && !questDescription.isEmpty()) {
            lore.add(questDescription);
        }
        
        inv.setItem(4, createItem(icon,
            typeColor + "&l" + questName,
            lore.toArray(new String[0])));
    }
    
    /**
     * Render status info (progress, status text)
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
     * Render objectives list
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
                "&7Goal: &f" + getObjectiveDescription(obj),
                "&7Progress: " + (done ? "&a" : "&e") + current + "/" + target + 
                    " (" + String.format("%.0f", pct) + "%)",
                bar));
        }
    }
    
    /**
     * Render rewards list
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
        rewardLore.add("&8| from quest module config");
        
        inv.setItem(31, createItem(Material.DIAMOND,
            "&b&lQuest Rewards",
            rewardLore.toArray(new String[0])));
    }
    
    /**
     * Render action buttons (accept/cancel/claim)
     */
    private void renderActionButtons(Inventory inv) {
        if (canAccept()) {
            inv.setItem(40, createItem(Material.EMERALD,
                "&a&l[ Accept Quest ]",
                "",
                "&7Accept to start tracking progress",
                "&8| Click to accept"));
        } else if (isCompleted && !isClaimed) {
            inv.setItem(40, createItem(Material.GOLD_INGOT,
                "&e&l[ Claim Reward ]",
                "",
                "&7All objectives completed, claim your reward",
                "&8| Click to claim"));
        } else if (isClaimed) {
            inv.setItem(40, createItem(Material.GRAY_DYE,
                "&7&lReward Claimed",
                "",
                "&7This quest has been completed and reward claimed"));
        } else if (hasProgress) {
            inv.setItem(40, createItem(Material.REDSTONE,
                "&c&l[ Cancel Quest ]",
                "",
                "&7Abandon current progress (cannot undo)",
                "&8| Click to cancel"));
        } else {
            inv.setItem(40, createItem(Material.BARRIER,
                "&c&l[ Cannot Accept ]",
                "",
                getCannotAcceptReason(),
                "&7Check requirements and try again"));
        }
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 40 && clickType.isLeftClick()) {
            handleActionButtonClick(player);
        }
    }
    
    /**
     * Handle action button click
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
            player.sendMessage(ColorUtils.colorize("&c[Quest] Invalid guild ID, cannot accept quest! (guildId=" + guildId + ")"));
            module.getContext().getLogger().warning("[Quest-Detail] Accept denied: guildId=" + guildId +
                ", questId=" + questId + ", player=" + player.getName());
            return;
        }

        QuestDefinition definition = module.getQuestManager().getDefinition(questId);
        if (definition == null) {
            player.sendMessage(ColorUtils.colorize("&c[Quest] Quest definition does not exist!"));
            return;
        }
        
        QuestProgress newProgress = new QuestProgress(
            definition.getId(), playerUuid, player.getName(), guildId,
            definition.getObjectives().size());
        
        if (module.getQuestManager().acceptQuest(newProgress)) {
            module.getContext().sendMessage(player, "quest.accepted",
                "&a[Quest] Accepted quest: &f" + questName);
            
            notifyOtherGUIsRefresh();
            forceRefreshContent(player);
        }
    }
    
    private void handleClaimReward(Player player) {
        if (guildId <= 0) {
            player.sendMessage(ColorUtils.colorize("&c[Quest] Invalid guild ID, cannot claim reward!"));
            return;
        }

        QuestDefinition definition = module.getQuestManager().getDefinition(questId);
        QuestProgress progress = module.getQuestManager()
            .getPlayerQuest(guildId, playerUuid, questId);
        
        if (definition == null || progress == null) {
            player.sendMessage(ColorUtils.colorize("&c[Quest] Cannot retrieve quest data!"));
            return;
        }
        
        module.getRewardHandler().grantRewards(player, definition, progress);
        module.getQuestManager().saveGuildProgress(guildId);
        module.getContext().getEventBus().publish(
            new GuildQuestModule.QuestCompletedEvent(
                player.getName(), questName, guildId));
        module.getContext().sendMessage(player, "quest.claimed", "&a[Quest] Rewards granted!");
        
        notifyOtherGUIsRefresh();
        forceRefreshContent(player);
    }
    
    private void handleCancel(Player player) {
        player.sendMessage(ColorUtils.colorize("&c[Quest] Cancel feature not yet implemented"));
    }
    
    // ==================== Helper Methods ====================
    
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
        if (questType == null) return "Unknown";
        switch (questType) {
            case DAILY: return "Daily Quest";
            case WEEKLY: return "Weekly Quest";
            case ONE_TIME: return "One-time Quest";
            default: return "Unknown Type";
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
        if (isClaimed) return "&a&lClaimed";
        if (isCompleted) return "&e&lCompleted (Claim Pending)";
        if (hasProgress) return "&6&lIn Progress";
        if (canAccept()) return "&a&lAvailable";
        return "&c&lUnavailable";
    }
    
    private String getProgressText() {
        if (!hasProgress || objectiveProgress == null) {
            return "";
        }
        
        try {
            QuestDefinition def = module.getQuestManager().getDefinition(questId);
            if (def != null) {
                return "&7Progress: &f" + String.format("%.1f%%", def.getProgressPercent(objectiveProgress));
            }
        } catch (Exception ignored) {}
        
        return "";
    }
    
    private String getHintText() {
        if (isCompleted && !isClaimed) return "&eClick to claim reward";
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
        if (obj == null || obj.getType() == null) return "Unknown Objective";
        return obj.getType().getDisplayName();
    }
    
    private String getObjectiveDescription(QuestObjective obj) {
        if (obj == null) return "No description";
        return obj.getDescription() != null ? obj.getDescription() : "No description";
    }
    
    private String getRewardDisplayName(QuestReward r) {
        if (r == null || r.getType() == null) return "Unknown";
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
        if (hasProgress) return "&cAlready accepted this quest";
        
        try {
            var guild = module.getContext().getPlugin().getGuildService().getPlayerGuild(playerUuid);
            if (guild != null && guild.getLevel() < minGuildLevel) {
                return "&cRequires guild level: " + minGuildLevel + " (current: &f" + guild.getLevel() + "&c)";
            }
        } catch (Exception ignored) {}
        
        return "&cCannot accept this quest";
    }
    
    private void notifyOtherGUIsRefresh() {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("guildId", guildId);
        data.put("playerUuid", playerUuid);
        module.getContext().notifyGUIRefresh("quest-list", data);
        module.getContext().notifyGUIRefresh("quest-active-list", data);
    }
    
    private void forceRefreshContent(Player player) {
        // Key fix: since new architecture uses final fields (immutable data),
        // must reopen entire GUI instance to refresh data
        com.guild.core.utils.CompatibleScheduler.runTaskLater(module.getContext().getPlugin(), () -> {
            if (player.isOnline()) {
                try {
                    // Re-fetch the latest quest definition and progress
                    QuestDefinition def = module.getQuestManager().getDefinition(questId);
                    QuestProgress progress = null;
                    if (def != null && guildId > 0 && playerUuid != null) {
                        progress = module.getQuestManager().getPlayerQuest(guildId, playerUuid, questId);
                    }
                    
                    // Build new GUI instance (with latest data)
                    Map<String, Object> reopenData = new java.util.HashMap<>();
                    reopenData.put("definition", def);  // May be null, but Builder can handle
                    reopenData.put("guildId", guildId);
                    reopenData.put("playerUuid", playerUuid);
                    
                    // Reopen GUI (replaces current GUI instance)
                    module.getContext().getApi().openCustomGUI("quest-detail", player, reopenData);
                    
                } catch (Exception e) {
                    module.getContext().getLogger().warning("[Quest-Detail] Error refreshing GUI: " + e.getMessage());
                    
                    // If reopen fails, try simple inventory refresh as fallback
                    if (this.inventory != null) {
                        this.inventory.clear();
                        setupInventory(this.inventory);
                        player.updateInventory();
                    }
                }
            }
        }, 1L);  // 1 tick delay
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
    
    // ==================== Builder Pattern ====================
    
    /**
     * Builder class - safely constructs QuestDetailGUI instances.
     * All data is preprocessed and validated here.
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
