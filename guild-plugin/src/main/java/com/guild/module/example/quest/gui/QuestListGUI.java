package com.guild.module.example.quest.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.core.module.ModuleContext;
import com.guild.module.example.quest.GuildQuestModule;
import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestObjective;
import com.guild.module.example.quest.model.QuestProgress;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class QuestListGUI extends AbstractModuleGUI {
    private final GuildQuestModule module;
    private final ModuleContext context;
    private final int guildId;
    private final UUID playerUuid;
    private Player viewer;
    private final List<QuestDefinition> dailyQuests;
    private final List<QuestDefinition> weeklyQuests;
    private final List<QuestDefinition> oneTimeQuests;
    private static final int PER_PAGE = 14;
    private int currentPage = 1;
    private final Map<Integer, QuestDefinition> slotDataMap = new HashMap<>();

    public QuestListGUI(GuildQuestModule module, int guildId, UUID playerUuid) {
        super();
        this.module = module;
        this.context = module.getContext();
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.dailyQuests = new ArrayList<>(module.getQuestManager()
            .getDefinitionsByType(QuestDefinition.QuestType.DAILY));
        this.weeklyQuests = new ArrayList<>(module.getQuestManager()
            .getDefinitionsByType(QuestDefinition.QuestType.WEEKLY));
        this.oneTimeQuests = new ArrayList<>(module.getQuestManager()
            .getDefinitionsByType(QuestDefinition.QuestType.ONE_TIME));
        
        // Register GUI refresh listener
        registerRefreshListener();
    }
    
    private void registerRefreshListener() {
        context.registerGUIRefreshListener("quest-list", (guiType, data) -> {
            Object guildIdObj = data.get("guildId");
            int notifiedGuildId = guildIdObj instanceof Number ? ((Number) guildIdObj).intValue() : 0;
            
            if (notifiedGuildId != 0 && notifiedGuildId == guildId && 
                data.containsKey("resetType") && viewer != null && viewer.isOnline()) {
                refresh(viewer);
                return;
            }
            
            UUID notifiedPlayerUuid = (UUID) data.get("playerUuid");
            if (notifiedGuildId != 0 && notifiedGuildId == guildId && 
                notifiedPlayerUuid != null && notifiedPlayerUuid.equals(playerUuid) && 
                viewer != null && viewer.isOnline()) {
                refresh(viewer);
            }
        });
    }

    public void setViewer(Player player) { this.viewer = player; }
    public Player getViewer() { return viewer; }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6&lGuild Quests - Available");
    }

    private static final int SLOT_TITLE = 4;

    @Override
    public void setupInventory(Inventory inv) {
        this.inventory = inv;
        fillBorder(inv);
        fillInteriorSlots(inv);
        slotDataMap.clear();

        inv.setItem(SLOT_TITLE, createItem(Material.BOOK,
            "&6&lGuild Quest Panel",
            "",
            "&7Daily: &f" + dailyQuests.size() + "  &e|&7  Weekly: &f" + weeklyQuests.size() + "  &e|&7  One-time: &f" + oneTimeQuests.size(),
            "",
            "&8| quest module (local persistence)"));

        List<QuestDefinition> allToShow = new ArrayList<>();
        allToShow.addAll(dailyQuests);
        allToShow.addAll(weeklyQuests);
        allToShow.addAll(oneTimeQuests);

        int maxDaily = context.getConfig().getInt("settings.max-daily-quests", 3);
        int acceptedDaily = module.getQuestManager()
            .getAcceptedCount(guildId, playerUuid, QuestDefinition.QuestType.DAILY);

        int startIndex = (currentPage - 1) * PER_PAGE;
        for (int i = 0; i < Math.min(PER_PAGE, allToShow.size() - startIndex); i++) {
            QuestDefinition def = allToShow.get(startIndex + i);
            boolean canAccept = module.getQuestManager().canAccept(guildId, playerUuid, def);
            boolean alreadyAccepted = module.getQuestManager()
                .getPlayerQuest(guildId, playerUuid, def.getId()) != null;
            Material icon = switch (def.getType()) {
                case DAILY -> Material.CLOCK;
                case WEEKLY -> Material.SUNFLOWER;
                case ONE_TIME -> Material.TOTEM_OF_UNDYING;
            };
            String colorPrefix = !canAccept ? "&7" :
                def.getType() == QuestDefinition.QuestType.DAILY ? "&e" :
                def.getType() == QuestDefinition.QuestType.WEEKLY ? "&6" : "&c";
            List<String> lore = buildQuestLore(def, canAccept, alreadyAccepted, acceptedDaily, maxDaily);

            int slot = mapToSlot(i);
            if (slot != -1) {
                inv.setItem(slot, createItem(icon,
                    colorPrefix + "&l" + def.getName(),
                    lore.toArray(new String[0])));
                slotDataMap.put(slot, def);
            }
        }

        int totalPages = getTotalPages(allToShow.size());
        if (totalPages > 1) {
            setupPagination(inv, currentPage, totalPages,
                "&e&lPrevious", "&e&lNext");
        }
        inv.setItem(49, createBackButton(
            context.getMessage("module.quest.back", "&cBack"),
            context.getMessage("module.quest.back-hint", "&7Return to guild info")));
    }

    private List<String> buildQuestLore(QuestDefinition def, boolean canAccept,
                                          boolean alreadyAccepted,
                                          int acceptedDaily, int maxDaily) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Type: " + colorPrefix(def));

        StringBuilder objectiveLine = new StringBuilder("&7Goal: ");
        for (int j = 0; j < def.getObjectives().size(); j++) {
            QuestObjective obj = def.getObjectives().get(j);
            if (j > 0) objectiveLine.append(", ");
            objectiveLine.append(obj.getDescription());
        }
        lore.add(objectiveLine.toString());

        if (!def.getRewards().isEmpty()) {
            StringBuilder rewardLine = new StringBuilder("&7Rewards: ");
            for (int j = 0; j < def.getRewards().size(); j++) {
                var r = def.getRewards().get(j);
                if (j > 0) rewardLine.append(", ");
                rewardLine.append(r.getType().getDisplayName()).append("+").append((int) r.getAmount());
            }
            lore.add(rewardLine.toString());
        }

        lore.add("&7Min Guild Level: &f" + def.getMinGuildLevel());

        if (!canAccept) {
            switch (def.getType()) {
                case DAILY:
                    lore.add("&cDaily limit reached (" + acceptedDaily + "/" + maxDaily + ")");
                    lore.add("&7Next available: &f" + formatNextResetTime("daily"));
                    break;
                case WEEKLY:
                    lore.add("&cAlready accepted this week");
                    lore.add("&7Next available: &f" + formatNextResetTime("weekly"));
                    break;
                case ONE_TIME:
                    QuestProgress oneTimeProgress = module.getQuestManager()
                        .getPlayerQuestAny(guildId, playerUuid, def.getId());
                    if (oneTimeProgress != null && oneTimeProgress.isClaimed()) {
                        lore.add("&cCompleted, cannot re-accept");
                    } else if (oneTimeProgress != null) {
                        lore.add("&aAccepted (In Progress)");
                        lore.add("&7Click to view details & progress");
                    } else {
                        lore.add("&cCannot accept");
                    }
                    break;
            }
        } else if (alreadyAccepted) {
            lore.add("&aAccepted (In Progress)");
            lore.add("&7Click to view details & progress");
        } else {
            lore.add("&aClick to accept this quest");
        }
        lore.add("");
        lore.add("&8| from quest module");
        return lore;
    }

    private String colorPrefix(QuestDefinition def) {
        return switch (def.getType()) {
            case DAILY -> "&eDaily";
            case WEEKLY -> "&6Weekly";
            case ONE_TIME -> "&cOne-time";
        };
    }

    private String formatNextResetTime(String type) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime resetTime = LocalTime.of(0, 0); // Reset at midnight
        LocalDateTime nextReset;
        if ("daily".equals(type)) {
            nextReset = now.toLocalDate().plusDays(1).atTime(resetTime);
            if (now.toLocalTime().isAfter(resetTime)) {
                nextReset = now.toLocalDate().atTime(resetTime);
            }
        } else {
            LocalDate monday = now.toLocalDate()
                .plusDays(8 - now.toLocalDate().getDayOfWeek().getValue());
            nextReset = monday.atTime(resetTime);
            if (now.toLocalDate().getDayOfWeek().getValue() == 1 && now.toLocalDate().isBefore(monday)) {
                if (now.toLocalTime().isBefore(resetTime)) {
                    nextReset = now.toLocalDate().atTime(resetTime);
                }
            }
        }
        if (nextReset.isBefore(now)) nextReset = now.plusSeconds(1);
        LocalDate resetDate = nextReset.toLocalDate();
        LocalTime resetHour = nextReset.toLocalTime();
        if (resetDate.equals(now.toLocalDate())) {
            return "Today " + String.format("%02d:%02d", resetHour.getHour(), resetHour.getMinute());
        } else if (resetDate.equals(now.toLocalDate().plusDays(1))) {
            return "Tomorrow " + String.format("%02d:%02d", resetHour.getHour(), resetHour.getMinute());
        }
        return nextReset.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        int totalPages = getTotalPages(dailyQuests.size() + weeklyQuests.size() + oneTimeQuests.size());

        if (slot == 49) { context.navigateBack(player); return; }

        if (slot == 45 && currentPage > 1) { currentPage--; refresh(player); return; }
        if (slot == 53 && currentPage < totalPages) { currentPage++; refresh(player); return; }

        QuestDefinition selected = slotDataMap.get(slot);
        if (selected != null) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("definition", selected);
                data.put("guildId", guildId);
                data.put("playerUuid", playerUuid);

                context.getApi().openCustomGUI("quest-detail", player, data);

            } catch (Exception e) {
                context.getLogger().severe("[Quest-List] Failed to open quest details: " + e.getMessage());
                player.sendMessage(ColorUtils.colorize("&c&l[Error] Failed to open quest details"));
                player.sendMessage(ColorUtils.colorize("&7Quest: &f" + selected.getName()));
                player.sendMessage(ColorUtils.colorize("&7Reason: &c" + e.getMessage()));
            }
        }
    }
}
