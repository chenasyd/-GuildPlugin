package com.guild.module.example.quest.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.core.module.ModuleContext;
import com.guild.module.example.quest.GuildQuestModule;
import com.guild.module.example.quest.QuestTexts;
import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestObjective;
import com.guild.module.example.quest.model.QuestProgress;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ActiveQuestsGUI extends AbstractModuleGUI {
    private final GuildQuestModule module;
    private final ModuleContext context;
    private final QuestTexts tx;
    private final List<QuestProgress> activeQuests;
    private final int guildId;
    private final Map<Integer, QuestProgress> slotDataMap = new HashMap<>();
    private int currentPage = 1;
    private static final int PER_PAGE = 14;
    private UUID playerUuid;

    public ActiveQuestsGUI(GuildQuestModule module, List<QuestProgress> activeQuests, int guildId, UUID playerUuid) {
        super();
        this.module = module;
        this.context = module.getContext();
        this.tx = module.texts();
        this.activeQuests = new ArrayList<>(activeQuests != null ? activeQuests : Collections.emptyList());
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        registerRefreshListener();
    }

    private void registerRefreshListener() {
        context.registerGUIRefreshListener("quest-active-list", (guiType, data) -> {
            Object guildIdObj = data.get("guildId");
            int notifiedGuildId = guildIdObj instanceof Number ? ((Number) guildIdObj).intValue() : 0;

            if (notifiedGuildId != 0 && notifiedGuildId == guildId &&
                data.containsKey("resetType")) {
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    List<QuestProgress> updatedActiveQuests = module.getQuestManager()
                        .getPlayerActiveQuests(guildId, playerUuid);
                    this.activeQuests.clear();
                    this.activeQuests.addAll(updatedActiveQuests);
                    refresh(player);
                }
                return;
            }

            UUID notifiedPlayerUuid = (UUID) data.get("playerUuid");
            if (notifiedGuildId != 0 && notifiedGuildId == guildId &&
                notifiedPlayerUuid != null && notifiedPlayerUuid.equals(playerUuid)) {
                List<QuestProgress> updatedActiveQuests = module.getQuestManager()
                    .getPlayerActiveQuests(guildId, playerUuid);
                this.activeQuests.clear();
                this.activeQuests.addAll(updatedActiveQuests);
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    refresh(player);
                }
            }
        });
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(tx.t("module.quest.gui.active-title", "&a&lActive Quests"));
    }

    @Override
    public void setupInventory(Inventory inv) {
        this.inventory = inv;
        fillBorder(inv);
        fillInteriorSlots(inv);
        slotDataMap.clear();

        inv.setItem(4, createItem(Material.COMPASS,
            tx.t("module.quest.gui.active-header", "&a&lActive Quests"),
            "",
            tx.tf("module.quest.gui.active-count", "&7Ongoing: &f{0} quest(s)", activeQuests.size()),
            tx.t("module.quest.gui.footer", "&8| from quest module")));

        if (activeQuests.isEmpty()) {
            inv.setItem(22, createItem(Material.BARRIER,
                tx.t("module.quest.gui.no-active", "&c&lNo Active Quests"),
                "",
                tx.t("module.quest.gui.no-active-hint", "&7Go to the quest list and accept some!"),
                tx.t("module.quest.gui.footer", "&8| Click to go back")));
            return;
        }

        int startIndex = (currentPage - 1) * PER_PAGE;
        for (int i = 0; i < Math.min(PER_PAGE, activeQuests.size() - startIndex); i++) {
            QuestProgress p = activeQuests.get(startIndex + i);
            QuestDefinition def = module.getQuestManager().getDefinition(p.getQuestId());
            if (def == null) continue;

            boolean isCompleted = p.isObjectivesCompleted(def) || p.isCompletedMarked();
            double pct = p.getCompletionPercent(def);
            Material icon = isCompleted ? Material.LIME_STAINED_GLASS_PANE :
                pct >= 50 ? Material.YELLOW_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;
            String colorPrefix = isCompleted ? "&a" : pct >= 50 ? "&e" : "&7";
            String typeIcon = switch (def.getType()) {
                case DAILY -> "D";
                case WEEKLY -> "W";
                case ONE_TIME -> "1";
            };

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(colorPrefix + typeIcon + " " + tx.questTypeShort(def.getType()));
            lore.add(tx.tf("module.quest.gui.progress", "&7Progress: {0}{1}%",
                colorPrefix, String.format("%.1f", pct)));
            if (!isCompleted && p.getObjectiveProgress().length > 0) {
                QuestObjective mainObj = def.getObjectives().get(0);
                lore.add("&7" + tx.objectiveType(mainObj.getType()) + ": &f"
                    + p.getObjectiveProgress()[0] + "/" + mainObj.getTarget());
            }
            if (isCompleted && !p.isClaimed()) lore.add("");
            if (isCompleted && !p.isClaimed()) {
                lore.add(tx.t("module.quest.gui.click-claim", "&eClick to claim reward"));
            } else if (isCompleted) {
                lore.add(tx.t("module.quest.gui.claimed", "&7Claimed"));
            } else {
                lore.add(tx.t("module.quest.gui.click-details", "&7Click to view details"));
            }
            lore.add("");
            lore.add(tx.t("module.quest.gui.footer", "&8| from quest module"));

            int slot = mapToSlot(i);
            if (slot != -1) {
                inv.setItem(slot, createItem(icon,
                    colorPrefix + tx.questName(def), lore.toArray(new String[0])));
                slotDataMap.put(slot, p);
            }
        }

        int totalPages = getTotalPages(activeQuests.size());
        if (totalPages > 1) {
            setupPagination(inv, currentPage, totalPages,
                tx.t("module.quest.gui.prev", "&e&lPrevious"),
                tx.t("module.quest.gui.next", "&e&lNext"));
        }
        inv.setItem(49, createBackButton(
            tx.t("module.quest.back", "&cBack"),
            tx.t("module.quest.back-hint", "&7Return to guild info")));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) {
            try {
                var guild = context.getPlugin().getGuildService().getGuildById(guildId);
                if (guild == null) {
                    player.closeInventory();
                    return;
                }
                context.getPlugin().getGuiManager().openGUI(player,
                    new com.guild.gui.GuildInfoGUI(context.getPlugin(), player, guild));
            } catch (Exception e) {
                player.closeInventory();
            }
            return;
        }
        int totalPages = getTotalPages(activeQuests.size());
        if (slot == 45 && currentPage > 1) { currentPage--; refresh(player); }
        else if (slot == 53 && currentPage < totalPages) { currentPage++; refresh(player); }

        QuestProgress selected = slotDataMap.get(slot);
        if (selected != null) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("definition", module.getQuestManager().getDefinition(selected.getQuestId()));
                data.put("guildId", guildId);
                data.put("playerUuid", player.getUniqueId());
                data.put("parent", QuestDetailGUI.ParentView.ACTIVE.name());
                GuildPluginAPI api = context.getApi();
                api.openCustomGUI("quest-detail", player, data);
            } catch (Exception e) {
                player.sendMessage(ColorUtils.colorize(tx.tf("module.quest.error.open-detail-short",
                    "&c[Quest] Failed to open details: {0}", e.getMessage())));
            }
        }
    }
}
