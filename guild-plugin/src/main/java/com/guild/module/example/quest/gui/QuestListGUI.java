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
        
        // 注册GUI刷新监听器
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
        return ColorUtils.colorize("&6&l公会任务 - 可接取");
    }

    private static final int SLOT_TITLE = 4;

    @Override
    public void setupInventory(Inventory inv) {
        this.inventory = inv;
        fillBorder(inv);
        fillInteriorSlots(inv);
        slotDataMap.clear();

        inv.setItem(SLOT_TITLE, createItem(Material.BOOK,
            "&6&l公会任务面板",
            "",
            "&7每日: &f" + dailyQuests.size() + " 个  &e|&7  每周: &f" + weeklyQuests.size() + " 个  &e|&7  一次: &f" + oneTimeQuests.size() + " 个",
            "",
            "&8┃ quest 模块 (本地持久化)"));

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
                "&e&l上一页", "&e&l下一页");
        }
        inv.setItem(49, createBackButton(
            context.getMessage("module.quest.back", "&c返回"),
            context.getMessage("module.quest.back-hint", "&7返回公会信息")));
    }

    private List<String> buildQuestLore(QuestDefinition def, boolean canAccept,
                                          boolean alreadyAccepted,
                                          int acceptedDaily, int maxDaily) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7类型: " + colorPrefix(def));

        StringBuilder objectiveLine = new StringBuilder("&7目标: ");
        for (int j = 0; j < def.getObjectives().size(); j++) {
            QuestObjective obj = def.getObjectives().get(j);
            if (j > 0) objectiveLine.append(", ");
            objectiveLine.append(obj.getDescription());
        }
        lore.add(objectiveLine.toString());

        if (!def.getRewards().isEmpty()) {
            StringBuilder rewardLine = new StringBuilder("&7奖励: ");
            for (int j = 0; j < def.getRewards().size(); j++) {
                var r = def.getRewards().get(j);
                if (j > 0) rewardLine.append(", ");
                rewardLine.append(r.getType().getDisplayName()).append("+").append((int) r.getAmount());
            }
            lore.add(rewardLine.toString());
        }

        lore.add("&7最低公会等级: &f" + def.getMinGuildLevel());

        if (!canAccept) {
            switch (def.getType()) {
                case DAILY:
                    lore.add("&c已达每日上限 (" + acceptedDaily + "/" + maxDaily + ")");
                    lore.add("&7下次可接取: &f" + formatNextResetTime("daily"));
                    break;
                case WEEKLY:
                    lore.add("&c本周已接取此任务");
                    lore.add("&7下次可接取: &f" + formatNextResetTime("weekly"));
                    break;
                case ONE_TIME:
                    QuestProgress oneTimeProgress = module.getQuestManager()
                        .getPlayerQuestAny(guildId, playerUuid, def.getId());
                    if (oneTimeProgress != null && oneTimeProgress.isClaimed()) {
                        lore.add("&c已完成，不可重复接取");
                    } else if (oneTimeProgress != null) {
                        lore.add("&a已接取 (进行中)");
                        lore.add("&7点击查看详情与进度");
                    } else {
                        lore.add("&c无法接取");
                    }
                    break;
            }
        } else if (alreadyAccepted) {
            lore.add("&a已接取 (进行中)");
            lore.add("&7点击查看详情与进度");
        } else {
            lore.add("&a点击接取此任务");
        }
        lore.add("");
        lore.add("&8┃ 来自 quest 模块");
        return lore;
    }

    private String colorPrefix(QuestDefinition def) {
        return switch (def.getType()) {
            case DAILY -> "&e每日";
            case WEEKLY -> "&6每周";
            case ONE_TIME -> "&c一次性";
        };
    }

    private String formatNextResetTime(String type) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime resetTime = LocalTime.of(0, 0); // 改为00:00
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
            return "今天 " + String.format("%02d:%02d", resetHour.getHour(), resetHour.getMinute());
        } else if (resetDate.equals(now.toLocalDate().plusDays(1))) {
            return "明天 " + String.format("%02d:%02d", resetHour.getHour(), resetHour.getMinute());
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
                context.getLogger().severe("[Quest-List] 打开任务详情失败: " + e.getMessage());
                player.sendMessage(ColorUtils.colorize("&c&l[错误] 打开任务详情失败"));
                player.sendMessage(ColorUtils.colorize("&7任务: &f" + selected.getName()));
                player.sendMessage(ColorUtils.colorize("&7原因: &c" + e.getMessage()));
            }
        }
    }
}
