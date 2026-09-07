package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.time.TimeProvider;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.base.AbstractPagedListGUI;
import com.guild.gui.base.GuiLayoutUtils;
import com.guild.models.Guild;
import com.guild.models.GuildRelation;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 公会关系 GUI：分页关系列表 + 创建关系。
 */
public class GuildRelationsGUI extends AbstractPagedListGUI<GuildRelation> {

    public static final String FUNC_CREATE_RELATION = "CREATE_RELATION";
    public static final String FUNC_PAGE_INFO = "PAGE_INFO";

    private static final int TOOLBAR_CREATE = 45;
    private static final int SLOT_PAGE_INFO = 46;

    private final Guild guild;

    public GuildRelationsGUI(GuildPlugin plugin, Guild guild, Player player) {
        super(plugin, player, PaginationLayout.CENTER_BAR, GuiLayoutUtils.ITEMS_PER_PAGE);
        this.guild = guild;
        loadRelations();
    }

    private void loadRelations() {
        plugin.getGuildService().getGuildRelationsAsync(guild.getId()).thenAccept(relationsList -> {
            setEntries(relationsList == null ? List.of() : relationsList);
            CompatibleScheduler.runTask(plugin, viewer, () -> {
                if (viewer.isOnline()) {
                    plugin.getGuiManager().refreshGUI(viewer);
                }
            });
        });
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.guild-relations.title", "&6Guild Relations"));
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
    }

    @Override
    protected String backLoreKey() {
        return "gui.guild-relations.back-to-menu";
    }

    @Override
    protected String backLoreDefault() {
        return "Return to main menu";
    }

    @Override
    protected void setupToolbar(Inventory inventory) {
        inventory.setItem(TOOLBAR_CREATE, createItem(
                Material.EMERALD,
                ColorUtils.colorize("&a" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.create-relation", "Create relation")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.create-relation-desc", "Create new guild relation")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.types", "Ally, Enemy, War, etc."))));
    }

    @Override
    protected void setupNavigationButtons(Inventory inventory) {
        super.setupNavigationButtons(inventory);
        int maxPage = maxPageIndex();
        inventory.setItem(SLOT_PAGE_INFO, createItem(
                Material.PAPER,
                ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.page-info", "Page {current}",
                        "{current}", String.valueOf(currentPage + 1))),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.total-pages", "Total {total} pages",
                        "{total}", String.valueOf(maxPage + 1))),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.total-relations", "Total {count} relations",
                        "{count}", String.valueOf(entries.size())))));
    }

    @Override
    protected boolean handleToolbarClick(Player player, int slot, ClickType clickType) {
        if (slot == TOOLBAR_CREATE) {
            plugin.getGuiManager().openGUI(player, new CreateRelationGUI(plugin, guild, player));
            return true;
        }
        return false;
    }

    @Override
    protected ItemStack createEntryItem(GuildRelation relation) {
        String otherGuildName = relation.getOtherGuildName(guild.getId());
        GuildRelation.RelationType type = relation.getType();
        GuildRelation.RelationStatus status = relation.getStatus();

        Material material = getRelationMaterial(type);
        String color = type.getColor();
        String displayName = color + otherGuildName + " - " + type.getDisplayName();

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-relations.relation-type", "Relation type")
                + ": " + color + type.getDisplayName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-relations.status", "Status")
                + ": " + getStatusColor(status) + status.getDisplayName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-relations.initiator", "Initiator") + ": " + relation.getInitiatorName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-relations.created-time", "Created time")
                + ": " + formatDateTime(relation.getCreatedAt())));

        if (relation.getExpiresAt() != null) {
            lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                    "gui.guild-relations.expires-time", "Expires time")
                    + ": " + formatDateTime(relation.getExpiresAt())));
        }

        lore.add("");

        if (status == GuildRelation.RelationStatus.PENDING) {
            if (relation.getInitiatorUuid().equals(viewer.getUniqueId())) {
                lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.right-cancel", "Right click: Cancel relation")));
            } else {
                lore.add(ColorUtils.colorize("&a" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.left-accept", "Left click: Accept relation")));
                lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.right-reject", "Right click: Reject relation")));
            }
        } else if (status == GuildRelation.RelationStatus.ACTIVE) {
            if (type == GuildRelation.RelationType.TRUCE) {
                lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.left-end-truce", "Left click: End truce")));
            } else if (type == GuildRelation.RelationType.WAR) {
                lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.left-propose-truce", "Left click: Propose truce")));
            } else {
                lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                        "gui.guild-relations.right-delete", "Right click: Delete relation")));
            }
        }

        return createItem(material, ColorUtils.colorize(displayName), lore.toArray(new String[0]));
    }

    @Override
    protected void onEntryClick(Player player, GuildRelation relation, ClickType clickType) {
        GuildRelation.RelationStatus status = relation.getStatus();
        GuildRelation.RelationType type = relation.getType();

        if (status == GuildRelation.RelationStatus.PENDING) {
            if (relation.getInitiatorUuid().equals(player.getUniqueId())) {
                if (clickType == ClickType.RIGHT) {
                    cancelRelation(player, relation);
                }
            } else {
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
            } else if (clickType == ClickType.RIGHT) {
                deleteRelation(player, relation);
            }
        }
    }

    private Material getRelationMaterial(GuildRelation.RelationType type) {
        return switch (type) {
            case ALLY -> Material.GREEN_WOOL;
            case ENEMY -> Material.RED_WOOL;
            case WAR -> Material.NETHERITE_SWORD;
            case TRUCE -> Material.YELLOW_WOOL;
            case NEUTRAL -> Material.GRAY_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }

    private String getStatusColor(GuildRelation.RelationStatus status) {
        return switch (status) {
            case PENDING -> "&e";
            case ACTIVE -> "&a";
            case EXPIRED -> "&7";
            case CANCELLED -> "&c";
            default -> "&f";
        };
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return languageManager.getGuiMessage(viewer, "gui.guild-relations.unknown", "Unknown");
        }
        return dateTime.format(TimeProvider.FULL_FORMATTER);
    }

    private void acceptRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.ACTIVE)
                .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.guild-relations.relations.accept-success",
                                "&aAccepted relation with {guild}!",
                                "{guild}", relation.getOtherGuildName(guild.getId()))));
                        loadRelations();
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.guild-relations.relations.accept-failed",
                                "&cFailed to accept relation!")));
                    }
                }));
    }

    private void rejectRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.CANCELLED)
                .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.guild-relations.relations.reject-success",
                                "&cRejected relation with {guild}!",
                                "{guild}", relation.getOtherGuildName(guild.getId()))));
                        loadRelations();
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.guild-relations.relations.reject-failed",
                                "&cFailed to reject relation!")));
                    }
                }));
    }

    private void cancelRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.CANCELLED)
                .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.guild-relations.relations.cancel-success",
                                "&cCancelled relation with {guild}!",
                                "{guild}", relation.getOtherGuildName(guild.getId()))));
                        loadRelations();
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.guild-relations.relations.cancel-failed",
                                "&cFailed to cancel relation!")));
                    }
                }));
    }

    private void endTruce(Player player, GuildRelation relation) {
        GuildRelation newRelation = new GuildRelation(
                relation.getGuild1Id(), relation.getGuild2Id(),
                relation.getGuild1Name(), relation.getGuild2Name(),
                GuildRelation.RelationType.NEUTRAL, player.getUniqueId(), player.getName());

        plugin.getGuildService().createGuildRelationAsync(
                newRelation.getGuild1Id(), newRelation.getGuild2Id(),
                newRelation.getGuild1Name(), newRelation.getGuild2Name(),
                newRelation.getType(), newRelation.getInitiatorUuid(), newRelation.getInitiatorName()
        ).thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
            if (success) {
                plugin.getGuildService().deleteGuildRelationAsync(relation.getId());
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.guild-relations.relations.truce-end",
                        "&aTruce with {guild} has ended, relation reset to neutral!",
                        "{guild}", relation.getOtherGuildName(guild.getId()))));
                loadRelations();
            } else {
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.guild-relations.relations.truce-end-failed",
                        "&cFailed to end truce!")));
            }
        }));
    }

    private void proposeTruce(Player player, GuildRelation relation) {
        GuildRelation truceRelation = new GuildRelation(
                relation.getGuild1Id(), relation.getGuild2Id(),
                relation.getGuild1Name(), relation.getGuild2Name(),
                GuildRelation.RelationType.TRUCE, player.getUniqueId(), player.getName());

        plugin.getGuildService().createGuildRelationAsync(
                truceRelation.getGuild1Id(), truceRelation.getGuild2Id(),
                truceRelation.getGuild1Name(), truceRelation.getGuild2Name(),
                truceRelation.getType(), truceRelation.getInitiatorUuid(), truceRelation.getInitiatorName()
        ).thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
            if (success) {
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.guild-relations.relations.truce-proposed",
                        "&eTruce proposed to {guild}!",
                        "{guild}", relation.getOtherGuildName(guild.getId()))));
                loadRelations();
            } else {
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.guild-relations.relations.truce-propose-failed",
                        "&cFailed to propose truce!")));
            }
        }));
    }

    private void deleteRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().deleteGuildRelationAsync(relation.getId())
                .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.guild-relations.relations.delete-success",
                                "&aDeleted relation with {guild}!",
                                "{guild}", relation.getOtherGuildName(guild.getId()))));
                        loadRelations();
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.guild-relations.relations.delete-failed",
                                "&cFailed to delete relation!")));
                    }
                }));
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }
        sendBedrockRelationList(player, 0);
        return true;
    }

    private void sendBedrockRelationList(Player player, int page) {
        plugin.getGuildService().getGuildRelationsAsync(guild.getId()).thenAccept(relationsList -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (relationsList == null || relationsList.isEmpty()) {
                    SimpleForm form = SimpleForm.builder()
                            .title(languageManager.getGuiColoredMessage(player,
                                    "gui.guild-relations.bedrock-title", "&6Guild Relations"))
                            .content(languageManager.getGuiColoredMessage(player,
                                    "gui.guild-relations.bedrock-empty", "&fNo guild relations currently"))
                            .button(languageManager.getGuiColoredMessage(player,
                                    "gui.guild-relations.bedrock-create-relation", "&aCreate Relation"))
                            .button(languageManager.getGuiColoredMessage(player,
                                    "gui.guild-relations.bedrock-back", "&cBack"))
                            .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                                if (response.clickedButtonId() == 0) {
                                    plugin.getGuiManager().openGUI(player,
                                            new CreateRelationGUI(plugin, guild, player));
                                } else {
                                    openBackGui(player);
                                }
                            }))
                            .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                                    () -> openBackGui(player)))
                            .build();
                    BedrockFormSender.sendForm(player.getUniqueId(), form);
                    return;
                }

                final int bedrockPerPage = GuiLayoutUtils.BEDROCK_ITEMS_PER_PAGE;
                int totalPages = GuiLayoutUtils.maxPageIndex(relationsList.size(), bedrockPerPage);
                final int safePage = Math.max(0, Math.min(page, totalPages));
                final int startIndex = safePage * bedrockPerPage;
                int endIndex = Math.min(startIndex + bedrockPerPage, relationsList.size());
                final int relCount = endIndex - startIndex;

                SimpleForm.Builder builder = SimpleForm.builder()
                        .title(languageManager.getGuiColoredMessage(player,
                                "gui.guild-relations.bedrock-title-page",
                                "&6Guild Relations - Page {page}", "{page}", String.valueOf(safePage + 1)))
                        .content(languageManager.getGuiColoredMessage(player,
                                "gui.guild-relations.bedrock-total", "&fTotal {count} relations",
                                "{count}", String.valueOf(relationsList.size())));

                for (int i = startIndex; i < endIndex; i++) {
                    GuildRelation rel = relationsList.get(i);
                    String otherName = rel.getOtherGuildName(guild.getId());
                    String color = rel.getType().getColor();
                    builder.button(ColorUtils.colorize(color + otherName + " - " + rel.getType().getDisplayName())
                            .replace("§7", "§f"));
                }

                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-create-relation", "&aCreate Relation"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-prev-page", "&ePrevious Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-next-page", "&eNext Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-back", "&cBack"));

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int clicked = response.clickedButtonId();
                    if (clicked < relCount) {
                        sendBedrockRelationActions(player, relationsList.get(startIndex + clicked));
                    } else if (clicked == relCount) {
                        plugin.getGuiManager().openGUI(player, new CreateRelationGUI(plugin, guild, player));
                    } else if (clicked == relCount + 1) {
                        sendBedrockRelationList(player, safePage - 1);
                    } else if (clicked == relCount + 2) {
                        sendBedrockRelationList(player, safePage + 1);
                    } else {
                        openBackGui(player);
                    }
                }));

                builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                        () -> openBackGui(player)));

                BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
            });
        });
    }

    private void sendBedrockRelationActions(Player player, GuildRelation relation) {
        String otherName = relation.getOtherGuildName(guild.getId());
        GuildRelation.RelationStatus status = relation.getStatus();
        GuildRelation.RelationType type = relation.getType();

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-actions-title",
                        "&6Relation Actions - {guild}", "{guild}", otherName))
                .content(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-actions-content",
                        "&fType: {type}\n&fStatus: {status}",
                        "{type}", type.getDisplayName(), "{status}", status.getDisplayName()));

        List<String> actionTypes = new ArrayList<>();

        if (status == GuildRelation.RelationStatus.PENDING) {
            if (relation.getInitiatorUuid().equals(player.getUniqueId())) {
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-cancel-relation", "&cCancel Relation"));
                actionTypes.add("cancel");
            } else {
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-accept-relation", "&aAccept Relation"));
                actionTypes.add("accept");
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-reject-relation", "&cReject Relation"));
                actionTypes.add("reject");
            }
        } else if (status == GuildRelation.RelationStatus.ACTIVE) {
            if (type == GuildRelation.RelationType.TRUCE) {
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-end-truce", "&eEnd Truce"));
                actionTypes.add("end_truce");
            } else if (type == GuildRelation.RelationType.WAR) {
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-propose-truce", "&ePropose Truce"));
                actionTypes.add("propose_truce");
            } else {
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-delete-relation", "&cDelete Relation"));
                actionTypes.add("delete");
            }
        }

        builder.button(languageManager.getGuiColoredMessage(player,
                "gui.guild-relations.bedrock-back-to-list", "&eBack to List"));

        final int actionCount = actionTypes.size();
        builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
            int clicked = response.clickedButtonId();
            if (clicked < actionCount) {
                switch (actionTypes.get(clicked)) {
                    case "accept" -> bedrockUpdateRelation(player, relation,
                            GuildRelation.RelationStatus.ACTIVE,
                            languageManager.getGuiColoredMessage(player,
                                    "gui.guild-relations.relations.accept-success",
                                    "&aAccepted relation with {guild}!", "{guild}", otherName));
                    case "reject" -> bedrockUpdateRelation(player, relation,
                            GuildRelation.RelationStatus.CANCELLED,
                            languageManager.getGuiColoredMessage(player,
                                    "gui.guild-relations.relations.reject-success",
                                    "&cRejected relation with {guild}!", "{guild}", otherName));
                    case "cancel" -> bedrockUpdateRelation(player, relation,
                            GuildRelation.RelationStatus.CANCELLED,
                            languageManager.getGuiColoredMessage(player,
                                    "gui.guild-relations.relations.cancel-success",
                                    "&cCancelled relation with {guild}!", "{guild}", otherName));
                    case "end_truce" -> bedrockEndTruce(player, relation);
                    case "propose_truce" -> bedrockProposeTruce(player, relation);
                    case "delete" -> bedrockDeleteRelation(player, relation);
                }
            } else {
                sendBedrockRelationList(player, 0);
            }
        }));

        builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                () -> sendBedrockRelationList(player, 0)));

        BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
    }

    private void bedrockUpdateRelation(Player player, GuildRelation relation,
                                       GuildRelation.RelationStatus newStatus, String successMsg) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), newStatus).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    player.sendMessage(successMsg);
                    sendBedrockRelationList(player, 0);
                } else {
                    player.sendMessage(languageManager.getGuiColoredMessage(player,
                            "gui.common.operation-failed", "&cOperation failed!"));
                }
            });
        });
    }

    private void bedrockEndTruce(Player player, GuildRelation relation) {
        GuildRelation newRelation = new GuildRelation(
                relation.getGuild1Id(), relation.getGuild2Id(),
                relation.getGuild1Name(), relation.getGuild2Name(),
                GuildRelation.RelationType.NEUTRAL, player.getUniqueId(), player.getName());
        plugin.getGuildService().createGuildRelationAsync(
                newRelation.getGuild1Id(), newRelation.getGuild2Id(),
                newRelation.getGuild1Name(), newRelation.getGuild2Name(),
                newRelation.getType(), newRelation.getInitiatorUuid(), newRelation.getInitiatorName()
        ).thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
            if (success) {
                plugin.getGuildService().deleteGuildRelationAsync(relation.getId());
                player.sendMessage(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.bedrock-truce-ended",
                        "&aTruce with {guild} has ended!", "{guild}", relation.getOtherGuildName(guild.getId())));
                sendBedrockRelationList(player, 0);
            } else {
                player.sendMessage(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.relations.truce-end-failed", "&cFailed to end truce!"));
            }
        }));
    }

    private void bedrockProposeTruce(Player player, GuildRelation relation) {
        GuildRelation truceRelation = new GuildRelation(
                relation.getGuild1Id(), relation.getGuild2Id(),
                relation.getGuild1Name(), relation.getGuild2Name(),
                GuildRelation.RelationType.TRUCE, player.getUniqueId(), player.getName());
        plugin.getGuildService().createGuildRelationAsync(
                truceRelation.getGuild1Id(), truceRelation.getGuild2Id(),
                truceRelation.getGuild1Name(), truceRelation.getGuild2Name(),
                truceRelation.getType(), truceRelation.getInitiatorUuid(), truceRelation.getInitiatorName()
        ).thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
            if (success) {
                player.sendMessage(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.relations.truce-proposed",
                        "&eTruce proposed to {guild}!", "{guild}", relation.getOtherGuildName(guild.getId())));
                sendBedrockRelationList(player, 0);
            } else {
                player.sendMessage(languageManager.getGuiColoredMessage(player,
                        "gui.guild-relations.relations.truce-propose-failed", "&cFailed to propose truce!"));
            }
        }));
    }

    private void bedrockDeleteRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    player.sendMessage(languageManager.getGuiColoredMessage(player,
                            "gui.guild-relations.relations.delete-success",
                            "&aDeleted relation with {guild}!", "{guild}", relation.getOtherGuildName(guild.getId())));
                    sendBedrockRelationList(player, 0);
                } else {
                    player.sendMessage(languageManager.getGuiColoredMessage(player,
                            "gui.guild-relations.relations.delete-failed", "&cFailed to delete relation!"));
                }
            });
        });
    }
}
