package com.guild.module.example.quest.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.core.module.ModuleContext;
import com.guild.module.example.quest.GuildQuestModule;
import com.guild.module.example.quest.QuestTexts;
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
    private final QuestTexts tx;
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
        this.tx = module.texts();
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.dailyQuests = new ArrayList<>(module.getQuestManager()
            .getDefinitionsByType(QuestDefinition.QuestType.DAILY));
        this.weeklyQuests = new ArrayList<>(module.getQuestManager()
            .getDefinitionsByType(QuestDefinition.QuestType.WEEKLY));
        this.oneTimeQuests = new ArrayList<>(module.getQuestManager()
            .getDefinitionsByType(QuestDefinition.QuestType.ONE_TIME));
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
        return ColorUtils.colorize(tx.t("module.quest.gui.list-title", "&6&lGuild Quests - Available"));
    }

    private static final int SLOT_TITLE = 4;

    @Override
    public void setupInventory(Inventory inv) {
        this.inventory = inv;
        fillBorder(inv);
        fillInteriorSlots(inv);
        slotDataMap.clear();

        inv.setItem(SLOT_TITLE, createItem(Material.BOOK,
            tx.t("module.quest.gui.list-header", "&6&lGuild Quest Panel"),
            "",
            tx.tf("module.quest.gui.list-counts",
                "&7Daily: &f{0}  &e|&7  Weekly: &f{1}  &e|&7  One-time: &f{2}",
                dailyQuests.size(), weeklyQuests.size(), oneTimeQuests.size()),
            "",
            tx.t("module.quest.gui.footer", "&8| from quest module")));

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
            String colorPrefix = !canAccept ? "&7" : tx.questTypeColor(def.getType());
            List<String> lore = buildQuestLore(def, canAccept, alreadyAccepted, acceptedDaily, maxDaily);

            int slot = mapToSlot(i);
            if (slot != -1) {
                inv.setItem(slot, createItem(icon,
                    colorPrefix + "&l" + tx.questName(def),
                    lore.toArray(new String[0])));
                slotDataMap.put(slot, def);
            }
        }

        int totalPages = getTotalPages(allToShow.size());
        if (totalPages > 1) {
            setupPagination(inv, currentPage, totalPages,
                tx.t("module.quest.gui.prev", "&e&lPrevious"),
                tx.t("module.quest.gui.next", "&e&lNext"));
        }
        inv.setItem(49, createBackButton(
            tx.t("module.quest.back", "&cBack"),
            tx.t("module.quest.back-hint", "&7Return to guild info")));
        inv.setItem(48, createItem(Material.OAK_SAPLING,
            tx.t("module.quest.tree.button", "&a&lGuild Tree"),
            "",
            tx.t("module.quest.tree.button-lore", "&7View shared virtual EXP & withdraw"),
            tx.t("module.quest.gui.click-hint", "&8| Click to open")));
    }

    private List<String> buildQuestLore(QuestDefinition def, boolean canAccept,
                                          boolean alreadyAccepted,
                                          int acceptedDaily, int maxDaily) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(tx.tf("module.quest.gui.type-label", "&7Type: {0}",
            tx.questTypeColor(def.getType()) + tx.questTypeShort(def.getType())));

        StringBuilder objectiveLine = new StringBuilder();
        for (int j = 0; j < def.getObjectives().size(); j++) {
            QuestObjective obj = def.getObjectives().get(j);
            if (j > 0) objectiveLine.append(", ");
            objectiveLine.append(tx.objectiveDescription(obj));
        }
        lore.add(tx.tf("module.quest.gui.goal-label", "&7Goal: {0}", objectiveLine.toString()));

        if (!def.getRewards().isEmpty()) {
            StringBuilder rewardLine = new StringBuilder();
            for (int j = 0; j < def.getRewards().size(); j++) {
                var r = def.getRewards().get(j);
                if (j > 0) rewardLine.append(", ");
                rewardLine.append(tx.rewardType(r.getType())).append("+").append((int) r.getAmount());
            }
            lore.add(tx.tf("module.quest.gui.rewards-label", "&7Rewards: {0}", rewardLine.toString()));
        }

        lore.add(tx.tf("module.quest.gui.min-level", "&7Min Guild Level: &f{0}", def.getMinGuildLevel()));

        if (!canAccept) {
            if (!module.getQuestManager().meetsMinGuildLevel(guildId, def)) {
                lore.add(tx.tf("module.quest.gui.level-required",
                    "&cRequires guild level: {0} (current: &f{1}&c)",
                    def.getMinGuildLevel(),
                    module.getQuestManager().getGuildLevel(guildId)));
            } else {
            switch (def.getType()) {
                case DAILY: {
                    QuestProgress dailyAny = module.getQuestManager()
                        .getPlayerQuestAny(guildId, playerUuid, def.getId());
                    if (dailyAny != null && dailyAny.isClaimed()
                        && module.getQuestManager().isDailyCompletedToday(guildId, playerUuid, def.getId())) {
                        lore.add(tx.t("module.quest.gui.daily-completed",
                            "&cCompleted today, cannot accept again"));
                    } else if (dailyAny != null && !dailyAny.isClaimed()) {
                        lore.add(tx.t("module.quest.gui.accepted-progress", "&aAccepted (In Progress)"));
                        lore.add(tx.t("module.quest.gui.click-details", "&7Click to view details & progress"));
                    } else {
                        lore.add(tx.tf("module.quest.gui.daily-limit",
                            "&cDaily limit reached ({0}/{1})", acceptedDaily, maxDaily));
                    }
                    lore.add(tx.tf("module.quest.gui.next-available",
                        "&7Next available: &f{0}", formatNextResetTime("daily")));
                    break;
                }
                case WEEKLY: {
                    QuestProgress weeklyAny = module.getQuestManager()
                        .getPlayerQuestAny(guildId, playerUuid, def.getId());
                    if (weeklyAny != null && weeklyAny.isClaimed()
                        && module.getQuestManager().isWeeklyCompletedThisWeek(guildId, playerUuid, def.getId())) {
                        lore.add(tx.t("module.quest.gui.weekly-completed",
                            "&cCompleted this week, cannot accept again"));
                    } else if (weeklyAny != null && !weeklyAny.isClaimed()) {
                        lore.add(tx.t("module.quest.gui.accepted-progress", "&aAccepted (In Progress)"));
                        lore.add(tx.t("module.quest.gui.click-details", "&7Click to view details & progress"));
                    } else {
                        lore.add(tx.t("module.quest.gui.weekly-accepted", "&cAlready accepted this week"));
                    }
                    lore.add(tx.tf("module.quest.gui.next-available",
                        "&7Next available: &f{0}", formatNextResetTime("weekly")));
                    break;
                }
                case ONE_TIME:
                    QuestProgress oneTimeProgress = module.getQuestManager()
                        .getPlayerQuestAny(guildId, playerUuid, def.getId());
                    if (oneTimeProgress != null && oneTimeProgress.isClaimed()) {
                        lore.add(tx.t("module.quest.gui.completed-once", "&cCompleted, cannot re-accept"));
                    } else if (oneTimeProgress != null) {
                        lore.add(tx.t("module.quest.gui.accepted-progress", "&aAccepted (In Progress)"));
                        lore.add(tx.t("module.quest.gui.click-details", "&7Click to view details & progress"));
                    } else {
                        lore.add(tx.t("module.quest.cannot-accept", "&cCannot accept this quest"));
                    }
                    break;
            }
            }
        } else if (alreadyAccepted) {
            lore.add(tx.t("module.quest.gui.accepted-progress", "&aAccepted (In Progress)"));
            lore.add(tx.t("module.quest.gui.click-details", "&7Click to view details & progress"));
        } else {
            lore.add(tx.t("module.quest.gui.click-accept", "&aClick to accept this quest"));
        }
        lore.add("");
        lore.add(tx.t("module.quest.gui.footer", "&8| from quest module"));
        return lore;
    }

    private String formatNextResetTime(String type) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime resetTime = LocalTime.of(0, 0);
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
        String hhmm = String.format("%02d:%02d", resetHour.getHour(), resetHour.getMinute());
        if (resetDate.equals(now.toLocalDate())) {
            return tx.tf("module.quest.next-reset.today", "Today {0}", hhmm);
        } else if (resetDate.equals(now.toLocalDate().plusDays(1))) {
            return tx.tf("module.quest.next-reset.tomorrow", "Tomorrow {0}", hhmm);
        }
        return nextReset.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        int totalPages = getTotalPages(dailyQuests.size() + weeklyQuests.size() + oneTimeQuests.size());

        if (slot == 49) {
            openGuildInfo(player);
            return;
        }
        if (slot == 48) {
            int gLevel = module.getQuestManager().getGuildLevel(guildId);
            context.openGUI(player, new GuildTreeGUI(module, guildId, gLevel, player.getUniqueId()));
            return;
        }

        if (slot == 45 && currentPage > 1) { currentPage--; refresh(player); return; }
        if (slot == 53 && currentPage < totalPages) { currentPage++; refresh(player); return; }

        QuestDefinition selected = slotDataMap.get(slot);
        if (selected != null) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("definition", selected);
                data.put("guildId", guildId);
                data.put("playerUuid", playerUuid);
                data.put("parent", QuestDetailGUI.ParentView.LIST.name());

                context.getApi().openCustomGUI("quest-detail", player, data);

            } catch (Exception e) {
                context.getLogger().severe("[Quest-List] Failed to open quest details: " + e.getMessage());
                player.sendMessage(ColorUtils.colorize(tx.t("module.quest.error.open-detail",
                    "&c&l[Error] Failed to open quest details")));
                player.sendMessage(ColorUtils.colorize(tx.tf("module.quest.error.quest-line",
                    "&7Quest: &f{0}", tx.questName(selected))));
                player.sendMessage(ColorUtils.colorize(tx.tf("module.quest.error.reason-line",
                    "&7Reason: &c{0}", e.getMessage())));
            }
        }
    }

    private void openGuildInfo(Player player) {
        try {
            var guild = context.getPlugin().getGuildService().getGuildById(guildId);
            if (guild == null) {
                player.closeInventory();
                return;
            }
            context.getPlugin().getGuiManager().openGUI(player,
                new com.guild.gui.GuildInfoGUI(context.getPlugin(), player, guild));
        } catch (Exception e) {
            context.getLogger().warning("[Quest-List] Failed to open GuildInfoGUI: " + e.getMessage());
            player.closeInventory();
        }
    }
}
