package com.guild.module.example.quest.gui;

import com.guild.core.module.ModuleContext;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.base.AbstractPagedListGUI;
import com.guild.gui.base.GuiLayoutUtils;
import com.guild.module.example.quest.GuildQuestModule;
import com.guild.module.example.quest.QuestTexts;
import com.guild.module.example.quest.model.QuestDefinition;
import com.guild.module.example.quest.model.QuestObjective;
import com.guild.module.example.quest.model.QuestProgress;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ActiveQuestsGUI extends AbstractPagedListGUI<QuestProgress> {

    private static final int SLOT_HEADER = 4;

    private final GuildQuestModule module;
    private final ModuleContext context;
    private final QuestTexts tx;
    private final int guildId;
    private final UUID playerUuid;

    public ActiveQuestsGUI(GuildQuestModule module, List<QuestProgress> activeQuests,
                           int guildId, Player player) {
        super(module.getContext().getPlugin(), player, PaginationLayout.BOTTOM, GuiLayoutUtils.ITEMS_PER_PAGE);
        this.module = module;
        this.context = module.getContext();
        this.tx = module.texts();
        this.guildId = guildId;
        this.playerUuid = player.getUniqueId();
        setEntries(activeQuests != null ? activeQuests : List.of());
        registerRefreshListener();
    }

    private void registerRefreshListener() {
        context.registerGUIRefreshListener("quest-active-list", (guiType, data) -> {
            Object guildIdObj = data.get("guildId");
            int notifiedGuildId = guildIdObj instanceof Number ? ((Number) guildIdObj).intValue() : 0;

            if (notifiedGuildId != 0 && notifiedGuildId == guildId && data.containsKey("resetType")) {
                reloadFromManager();
                return;
            }

            UUID notifiedPlayerUuid = (UUID) data.get("playerUuid");
            if (notifiedGuildId != 0 && notifiedGuildId == guildId
                    && notifiedPlayerUuid != null && notifiedPlayerUuid.equals(playerUuid)) {
                reloadFromManager();
            }
        });
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(tx.t("module.quest.gui.active-title", "&a&lActive Quests"));
    }

    @Override
    protected void setupToolbar(Inventory inventory) {
        inventory.setItem(SLOT_HEADER, createItem(Material.COMPASS,
                tx.t("module.quest.gui.active-header", "&a&lActive Quests"),
                "",
                tx.tf("module.quest.gui.active-count", "&7Ongoing: &f{0} quest(s)", entries.size()),
                tx.t("module.quest.gui.footer", "&8| from quest module")));
    }

    @Override
    protected void displayEmptyState(Inventory inventory) {
        inventory.setItem(22, createItem(Material.BARRIER,
                tx.t("module.quest.gui.no-active", "&c&lNo Active Quests"),
                "",
                tx.t("module.quest.gui.no-active-hint", "&7Go to the quest list and accept some!"),
                tx.t("module.quest.gui.footer", "&8| Click to go back")));
    }

    @Override
    protected ItemStack createEntryItem(QuestProgress progress) {
        QuestDefinition def = module.getQuestManager().getDefinition(progress.getQuestId());
        if (def == null) {
            return createItem(Material.BARRIER, "&c?", "&7Unknown quest");
        }

        boolean isCompleted = progress.isObjectivesCompleted(def) || progress.isCompletedMarked();
        double pct = progress.getCompletionPercent(def);
        Material icon = isCompleted ? Material.LIME_STAINED_GLASS_PANE
                : pct >= 50 ? Material.YELLOW_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;
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
        if (!isCompleted && progress.getObjectiveProgress().length > 0) {
            QuestObjective mainObj = def.getObjectives().get(0);
            lore.add("&7" + tx.objectiveType(mainObj.getType()) + ": &f"
                    + progress.getObjectiveProgress()[0] + "/" + mainObj.getTarget());
        }
        if (isCompleted && !progress.isClaimed()) {
            lore.add("");
            lore.add(tx.t("module.quest.gui.click-claim", "&eClick to claim reward"));
        } else if (isCompleted) {
            lore.add(tx.t("module.quest.gui.claimed", "&7Claimed"));
        } else {
            lore.add(tx.t("module.quest.gui.click-details", "&7Click to view details"));
        }
        lore.add("");
        lore.add(tx.t("module.quest.gui.footer", "&8| from quest module"));

        return createItem(icon, colorPrefix + tx.questName(def), lore.toArray(new String[0]));
    }

    @Override
    protected void onEntrySelected(Player player, QuestProgress selected) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("definition", module.getQuestManager().getDefinition(selected.getQuestId()));
            data.put("guildId", guildId);
            data.put("playerUuid", player.getUniqueId());
            data.put("parent", QuestDetailGUI.ParentView.ACTIVE.name());
            context.getApi().openCustomGUI("quest-detail", player, data);
        } catch (Exception e) {
            player.sendMessage(ColorUtils.colorize(tx.tf("module.quest.error.open-detail-short",
                    "&c[Quest] Failed to open details: {0}", e.getMessage())));
        }
    }

    @Override
    protected void openBackGui(Player player) {
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
    }

    @Override
    protected String backTitleKey() {
        return "module.quest.back";
    }

    @Override
    protected String backTitleDefault() {
        return "&cBack";
    }

    @Override
    protected String backLoreKey() {
        return "module.quest.back-hint";
    }

    @Override
    protected String backLoreDefault() {
        return "&7Return to guild info";
    }

    @Override
    public void refresh(Player player) {
        reloadFromManager();
        plugin.getGuiManager().refreshGUI(player);
    }

    private void reloadFromManager() {
        List<QuestProgress> updated = module.getQuestManager().getPlayerActiveQuests(guildId, playerUuid);
        setEntries(updated != null ? updated : List.of());
        if (viewer != null && viewer.isOnline()) {
            plugin.getGuiManager().refreshGUI(viewer);
        }
    }
}
