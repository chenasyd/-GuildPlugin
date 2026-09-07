package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.geyser.PlayerConnectionService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 关系管理 GUI（管理员）：全服关系分页列表 + 确认删除。
 */
public class RelationManagementGUI extends AbstractPagedListGUI<GuildRelation> {

    public static final String FUNC_REFRESH = "REFRESH";

    private static final int TOOLBAR_BACK = 46;
    private static final int SLOT_PAGE_INFO = 49;
    private static final int TOOLBAR_REFRESH = 52;

    private static final Map<UUID, GuildRelation> pendingDeletions = new HashMap<>();

    private boolean isLoading = false;

    public RelationManagementGUI(GuildPlugin plugin, Player player) {
        super(plugin, player, PaginationLayout.CENTER_BAR_PAGE_INFO, GuiLayoutUtils.ITEMS_PER_PAGE);
        if (player.hasPermission("guild.admin")) {
            loadRelations();
        }
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.relation-management.relation-management-title",
                "&4Relation Management - Admin"));
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new AdminGuildGUI(plugin, player));
    }

    @Override
    protected boolean validateAccess(Player player) {
        return player.hasPermission("guild.admin");
    }

    @Override
    protected void onUnauthorizedAccess(Player player) {
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.common.no-permission", "&cInsufficient permission")));
    }

    @Override
    public void setupInventory(Inventory inventory) {
        if (shouldFillBorder()) {
            GuiLayoutUtils.fillBorder54(inventory);
        }
        setupToolbar(inventory);
        if (isLoading) {
            inventory.setItem(22, createItem(Material.SAND,
                    ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer,
                            "gui.common.loading", "Loading...")),
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                            "gui.relation-management.loading-data", "Loading relation data..."))));
        } else if (entries.isEmpty()) {
            displayEmptyState(inventory);
        } else {
            displayEntries(inventory);
        }
        setupNavigationButtons(inventory);
        plugin.getGuiManager().applyImageModeIfNeeded(viewer, inventory, getGuiType());
    }

    @Override
    protected void displayEmptyState(Inventory inventory) {
        inventory.setItem(22, createItem(Material.BARRIER,
                ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                        "gui.relation-management.no-relations", "No relation data")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.relation-management.no-relations-desc", "No guild relations found"))));
    }

    @Override
    protected void setupToolbar(Inventory inventory) {
        inventory.setItem(TOOLBAR_BACK, createItem(Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.common.back", "Back")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.relation-management.relation-management-back-desc", "Back to admin menu"))));

        inventory.setItem(TOOLBAR_REFRESH, createItem(Material.EMERALD,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.guild-list-management.gui-refresh", "&aRefresh List")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.relation-management.relation-management-refresh-desc",
                        "Reload relation data"))));
    }

    @Override
    protected void setupNavigationButtons(Inventory inventory) {
        if (currentPage > 0) {
            inventory.setItem(slotForFunction(FUNC_PREV_PAGE, 48),
                    createItem(Material.ARROW,
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    "gui.relation-management.previous-page", "&aPrevious Page")),
                            ColorUtils.colorize(languageManager.getGuiIndexedMessage(viewer,
                                    "gui.relation-management.previous-page.desc", "&7Page {0}",
                                    String.valueOf(currentPage)))));
        }

        int totalPages = entries.isEmpty() ? 1 : maxPageIndex() + 1;
        inventory.setItem(SLOT_PAGE_INFO, createItem(Material.PAPER,
                ColorUtils.colorize(languageManager.getGuiIndexedMessage(viewer,
                        "gui.relation-management.page-info", "&ePage {0} of {1}",
                        String.valueOf(currentPage + 1), String.valueOf(totalPages))),
                ColorUtils.colorize(languageManager.getGuiIndexedMessage(viewer,
                        "gui.relation-management.total-relations", "&7Total {0} relations",
                        String.valueOf(entries.size())))));

        if (currentPage < maxPageIndex()) {
            inventory.setItem(slotForFunction(FUNC_NEXT_PAGE, 50),
                    createItem(Material.ARROW,
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    "gui.relation-management.next-page", "&aNext Page")),
                            ColorUtils.colorize(languageManager.getGuiIndexedMessage(viewer,
                                    "gui.relation-management.next-page.desc", "&7Page {0}",
                                    String.valueOf(currentPage + 2)))));
        }
    }

    @Override
    protected boolean handleToolbarClick(Player player, int slot, ClickType clickType) {
        if (slot == TOOLBAR_BACK) {
            openBackGui(player);
            return true;
        }
        if (slot == TOOLBAR_REFRESH) {
            if (!isLoading) {
                loadRelations();
                player.sendMessage(ColorUtils.colorize("&a" + languageManager.getGuiMessage(player,
                        "gui.relation-management.refreshing", "Refreshing relation list...")));
            }
            return true;
        }
        return false;
    }

    @Override
    protected ItemStack createEntryItem(GuildRelation relation) {
        Material material = getRelationMaterial(relation.getType());
        String status = getRelationStatus(relation.getStatus());
        boolean isPendingDeletion = pendingDeletions.containsKey(viewer.getUniqueId())
                && pendingDeletions.get(viewer.getUniqueId()).getId() == relation.getId();

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-relations.relation-type", "Relation type")
                + ": " + getRelationTypeName(relation.getType())));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-relations.status", "Status") + ": " + status));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.relation-management.guild1", "Guild 1") + ": " + relation.getGuild1Name()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.relation-management.guild2", "Guild 2") + ": " + relation.getGuild2Name()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-relations.initiator", "Initiator") + ": " + relation.getInitiatorName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-relations.created-time", "Created time")
                + ": " + formatDateTime(relation.getCreatedAt())));
        lore.add("");

        if (isPendingDeletion) {
            lore.add(ColorUtils.colorize("&4⚠ " + languageManager.getGuiMessage(viewer,
                    "gui.relation-management.pending-delete", "Pending confirmation for deletion")));
            lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                    "gui.relation-management.confirm-delete", "Left click: Confirm Delete")));
            lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer,
                    "gui.relation-management.cancel-delete", "Right click: Cancel")));
        } else {
            lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                    "gui.guild-relations.left-delete", "Left click: Delete relation")));
            lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer,
                    "gui.guild-relations.right-view-details", "Right click: View details")));
        }

        String displayName = isPendingDeletion
                ? ColorUtils.colorize("&4" + relation.getGuild1Name() + " ↔ " + relation.getGuild2Name())
                : ColorUtils.colorize("&6" + relation.getGuild1Name() + " ↔ " + relation.getGuild2Name());
        return createItem(material, displayName, lore.toArray(new String[0]));
    }

    @Override
    protected void onEntryClick(Player player, GuildRelation relation, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            if (pendingDeletions.containsKey(player.getUniqueId())
                    && pendingDeletions.get(player.getUniqueId()).getId() == relation.getId()) {
                confirmDeleteRelation(player, relation);
            } else {
                startDeleteRelation(player, relation);
            }
        } else if (clickType == ClickType.RIGHT) {
            if (pendingDeletions.containsKey(player.getUniqueId())
                    && pendingDeletions.get(player.getUniqueId()).getId() == relation.getId()) {
                cancelDeleteRelation(player);
            } else {
                showRelationDetails(player, relation);
            }
        }
    }

    private void loadRelations() {
        if (isLoading) {
            return;
        }
        isLoading = true;
        plugin.getGuiManager().refreshGUI(viewer);

        plugin.getGuildService().getAllGuildsAsync().thenCompose(guilds -> {
            List<CompletableFuture<List<GuildRelation>>> relationFutures = new ArrayList<>();
            if (guilds != null) {
                for (Guild guild : guilds) {
                    relationFutures.add(plugin.getGuildService().getGuildRelationsAsync(guild.getId()));
                }
            }
            if (relationFutures.isEmpty()) {
                return CompletableFuture.completedFuture(List.<GuildRelation>of());
            }
            return CompletableFuture.allOf(relationFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<GuildRelation> allRelationsList = new ArrayList<>();
                        for (CompletableFuture<List<GuildRelation>> future : relationFutures) {
                            try {
                                List<GuildRelation> part = future.join();
                                if (part != null) {
                                    allRelationsList.addAll(part);
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("Failed to load guild relations: " + e.getMessage());
                            }
                        }
                        return allRelationsList;
                    });
        }).thenAccept(relations -> CompatibleScheduler.runTask(plugin, viewer, () -> {
            setEntries(relations);
            isLoading = false;
            if (viewer.isOnline()) {
                refresh(viewer);
            }
        })).exceptionally(throwable -> {
            CompatibleScheduler.runTask(plugin, viewer, () -> {
                isLoading = false;
                if (viewer.isOnline()) {
                    viewer.sendMessage(ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                            "gui.relation-management.load-error",
                            "Error loading relation data: {error}", "{error}", throwable.getMessage())));
                    refresh(viewer);
                }
            });
            return null;
        });
    }

    private Material getRelationMaterial(GuildRelation.RelationType type) {
        return switch (type) {
            case ALLY -> Material.GREEN_WOOL;
            case ENEMY -> Material.RED_WOOL;
            case WAR -> Material.NETHERITE_SWORD;
            case TRUCE -> Material.YELLOW_WOOL;
            case NEUTRAL -> Material.GRAY_WOOL;
            default -> Material.STONE;
        };
    }

    private String getRelationTypeName(GuildRelation.RelationType type) {
        return type.getDisplayName(languageManager.getPlayerLanguage(viewer));
    }

    private String getRelationStatus(GuildRelation.RelationStatus status) {
        return status.getDisplayName(languageManager.getPlayerLanguage(viewer));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return languageManager.getGuiMessage(viewer, "gui.guild-relations.unknown", "Unknown");
        }
        return dateTime.format(TimeProvider.FULL_FORMATTER);
    }

    private void startDeleteRelation(Player player, GuildRelation relation) {
        pendingDeletions.put(player.getUniqueId(), relation);
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player,
                "gui.relation-management.confirm-delete-question",
                "&cAre you sure you want to delete relation: {0} ↔ {1}?",
                relation.getGuild1Name(), relation.getGuild2Name())));
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.relation-management.confirm-delete-instruction",
                "&cLeft Click: Confirm Delete | Right Click: Cancel")));
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.relation-management.auto-cancel", "&eAuto-cancel after 10 seconds")));
        plugin.getGuiManager().refreshGUI(player);

        CompatibleScheduler.runTaskLater(plugin, player, () -> {
            if (pendingDeletions.containsKey(player.getUniqueId())
                    && pendingDeletions.get(player.getUniqueId()).getId() == relation.getId()) {
                cancelDeleteRelation(player);
            }
        }, 200L);
    }

    private void confirmDeleteRelation(Player player, GuildRelation relation) {
        pendingDeletions.remove(player.getUniqueId());
        plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player,
                            "gui.relation-management.delete-success",
                            "&aDeleted relation: {0} ↔ {1}",
                            relation.getGuild1Name(), relation.getGuild2Name())));
                    loadRelations();
                } else {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.relation-management.delete-failed", "&cFailed to delete relation!")));
                }
            });
        }).exceptionally(throwable -> {
            CompatibleScheduler.runTask(plugin, player, () -> player.sendMessage(ColorUtils.colorize(
                    languageManager.getGuiIndexedMessage(player,
                            "gui.relation-management.delete-error",
                            "&cError deleting relation: {0}", throwable.getMessage()))));
            return null;
        });
    }

    private void cancelDeleteRelation(Player player) {
        GuildRelation relation = pendingDeletions.remove(player.getUniqueId());
        if (relation != null) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player,
                    "gui.relation-management.cancel-delete",
                    "&eCancelled deletion of relation: {0} ↔ {1}",
                    relation.getGuild1Name(), relation.getGuild2Name())));
            plugin.getGuiManager().refreshGUI(player);
        }
    }

    private void showRelationDetails(Player player, GuildRelation relation) {
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.relation-management.details.title", "&6=== Relation Details ===")));
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player,
                "gui.relation-management.details.type", "&eRelation Type: {0}",
                getRelationTypeName(relation.getType()))));
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player,
                "gui.relation-management.details.status", "&eStatus: {0}",
                getRelationStatus(relation.getStatus()))));
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player,
                "gui.relation-management.details.guild1", "&eGuild 1: {0} (ID: {1})",
                relation.getGuild1Name(), String.valueOf(relation.getGuild1Id()))));
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player,
                "gui.relation-management.details.guild2", "&eGuild 2: {0} (ID: {1})",
                relation.getGuild2Name(), String.valueOf(relation.getGuild2Id()))));
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player,
                "gui.relation-management.details.initiator", "&eInitiator: {0}",
                relation.getInitiatorName())));
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player,
                "gui.relation-management.details.created", "&eCreated: {0}",
                formatDateTime(relation.getCreatedAt()))));
        if (relation.getUpdatedAt() != null) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player,
                    "gui.relation-management.details.updated", "&eUpdated: {0}",
                    formatDateTime(relation.getUpdatedAt()))));
        }
        if (relation.getExpiresAt() != null) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiIndexedMessage(player,
                    "gui.relation-management.details.expires", "&eExpires: {0}",
                    formatDateTime(relation.getExpiresAt()))));
        }
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.relation-management.details.separator", "&6==================")));
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }
        if (!validateAccess(player)) {
            onUnauthorizedAccess(player);
            return false;
        }
        sendBedrockRelationMgmtList(player, 0);
        return true;
    }

    private void sendBedrockRelationMgmtList(Player player, int page) {
        plugin.getGuildService().getAllGuildsAsync().thenCompose(guilds -> {
            List<CompletableFuture<List<GuildRelation>>> futures = new ArrayList<>();
            if (guilds != null) {
                for (Guild g : guilds) {
                    futures.add(plugin.getGuildService().getGuildRelationsAsync(g.getId()));
                }
            }
            if (futures.isEmpty()) {
                return CompletableFuture.completedFuture(List.<GuildRelation>of());
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<GuildRelation> list = new ArrayList<>();
                        for (CompletableFuture<List<GuildRelation>> f : futures) {
                            try {
                                List<GuildRelation> part = f.join();
                                if (part != null) {
                                    list.addAll(part);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        return list;
                    });
        }).thenAccept(relations -> CompatibleScheduler.runTask(plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (relations.isEmpty()) {
                SimpleForm form = SimpleForm.builder()
                        .title(languageManager.getGuiColoredMessage(player,
                                "gui.relation-management.bedrock-title", "&4Relation Management"))
                        .content(languageManager.getGuiColoredMessage(player,
                                "gui.relation-management.bedrock-empty", "&cNo relation data"))
                        .button(languageManager.getGuiColoredMessage(player,
                                "gui.relation-management.bedrock-back", "&cBack"))
                        .validResultHandler(r -> CompatibleScheduler.runTask(plugin, player,
                                () -> openBackGui(player)))
                        .closedResultHandler(r -> {
                        })
                        .build();
                BedrockFormSender.sendForm(player.getUniqueId(), form);
                return;
            }

            int itemsPerPage = GuiLayoutUtils.BEDROCK_ITEMS_PER_PAGE;
            int totalPages = Math.max(1, (int) Math.ceil((double) relations.size() / itemsPerPage));
            final int safePage = Math.max(0, Math.min(page, totalPages - 1));
            int startIndex = safePage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, relations.size());

            SimpleForm.Builder builder = SimpleForm.builder()
                    .title(languageManager.getGuiColoredMessage(player,
                            "gui.relation-management.bedrock-title", "&4Relation Management"))
                    .content(languageManager.getGuiColoredMessage(player,
                            "gui.relation-management.bedrock-page-info",
                            "&fPage {current}/{total} | Total {count} relations",
                            "{current}", String.valueOf(safePage + 1),
                            "{total}", String.valueOf(totalPages),
                            "{count}", String.valueOf(relations.size())));

            List<GuildRelation> pageRelations = new ArrayList<>();
            String lang = languageManager.getPlayerLanguage(player);
            for (int i = startIndex; i < endIndex; i++) {
                GuildRelation rel = relations.get(i);
                pageRelations.add(rel);
                builder.button("§6" + rel.getGuild1Name() + " ↔ " + rel.getGuild2Name()
                        + " §f[" + rel.getType().getDisplayName(lang) + "]");
            }

            builder.button(languageManager.getGuiColoredMessage(player,
                    "gui.relation-management.bedrock-refresh", "&aRefresh List"));
            builder.button(languageManager.getGuiColoredMessage(player,
                    "gui.relation-management.bedrock-prev-page", "&ePrevious Page"));
            builder.button(languageManager.getGuiColoredMessage(player,
                    "gui.relation-management.bedrock-next-page", "&eNext Page"));
            builder.button(languageManager.getGuiColoredMessage(player,
                    "gui.relation-management.bedrock-back", "&cBack"));

            final int navOffset = pageRelations.size();

            builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                int id = response.clickedButtonId();
                if (id < navOffset) {
                    sendBedrockRelationMgmtActions(player, pageRelations.get(id), safePage);
                } else if (id == navOffset) {
                    sendBedrockRelationMgmtList(player, safePage);
                } else if (id == navOffset + 1) {
                    sendBedrockRelationMgmtList(player, safePage > 0 ? safePage - 1 : safePage);
                } else if (id == navOffset + 2) {
                    sendBedrockRelationMgmtList(player, safePage < totalPages - 1 ? safePage + 1 : safePage);
                } else {
                    openBackGui(player);
                }
            }));

            builder.closedResultHandler(response -> {
            });

            BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
        }));
    }

    private void sendBedrockRelationMgmtActions(Player player, GuildRelation relation, int page) {
        String lang = languageManager.getPlayerLanguage(player);
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player,
                        "gui.relation-management.bedrock-actions-title", "&6Relation Actions"))
                .content(languageManager.getGuiColoredMessage(player,
                        "gui.relation-management.bedrock-actions-content",
                        "&f{guild1} ↔ {guild2}\n&fType: {type}\n&fStatus: {status}\n&fInitiator: {initiator}",
                        "{guild1}", relation.getGuild1Name(), "{guild2}", relation.getGuild2Name(),
                        "{type}", ColorUtils.colorize(relation.getType().getDisplayName(lang)),
                        "{status}", ColorUtils.colorize(relation.getStatus().getDisplayName(lang)),
                        "{initiator}", relation.getInitiatorName()));

        builder.button(languageManager.getGuiColoredMessage(player,
                "gui.relation-management.bedrock-delete-relation", "&cDelete Relation"));
        builder.button(languageManager.getGuiColoredMessage(player,
                "gui.relation-management.bedrock-view-details", "&eView Details"));
        builder.button(languageManager.getGuiColoredMessage(player,
                "gui.relation-management.bedrock-back-to-list", "&cBack to List"));

        builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
            switch (response.clickedButtonId()) {
                case 0 -> sendBedrockConfirmDeleteRelation(player, relation, page);
                case 1 -> {
                    showRelationDetails(player, relation);
                    sendBedrockRelationMgmtActions(player, relation, page);
                }
                case 2 -> sendBedrockRelationMgmtList(player, page);
            }
        }));

        builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                () -> sendBedrockRelationMgmtList(player, page)));

        BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
    }

    private void sendBedrockConfirmDeleteRelation(Player player, GuildRelation relation, int page) {
        SimpleForm form = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player,
                        "gui.relation-management.bedrock-confirm-title", "&cConfirm Delete"))
                .content(languageManager.getGuiColoredMessage(player,
                        "gui.relation-management.bedrock-confirm-content",
                        "&fAre you sure you want to delete relation:\n&e{guild1} ↔ {guild2}&f?",
                        "{guild1}", relation.getGuild1Name(), "{guild2}", relation.getGuild2Name()))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.relation-management.bedrock-confirm-delete", "&cConfirm Delete"))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.relation-management.bedrock-cancel", "&fCancel"))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (response.clickedButtonId() == 0) {
                        plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).thenAccept(success -> {
                            CompatibleScheduler.runTask(plugin, player, () -> {
                                if (success) {
                                    player.sendMessage(languageManager.getGuiColoredMessage(player,
                                            "gui.relation-management.bedrock-delete-success",
                                            "&aDeleted relation: {guild1} ↔ {guild2}",
                                            "{guild1}", relation.getGuild1Name(),
                                            "{guild2}", relation.getGuild2Name()));
                                } else {
                                    player.sendMessage(languageManager.getGuiColoredMessage(player,
                                            "gui.relation-management.bedrock-delete-failed",
                                            "&cFailed to delete relation!"));
                                }
                                sendBedrockRelationMgmtList(player, page);
                            });
                        });
                    } else {
                        sendBedrockRelationMgmtActions(player, relation, page);
                    }
                }))
                .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                        () -> sendBedrockRelationMgmtActions(player, relation, page)))
                .build();

        BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    @Override
    public void onClose(Player player) {
        pendingDeletions.remove(player.getUniqueId());
    }

    @Override
    public void refresh(Player player) {
        if (player.isOnline()) {
            if (PlayerConnectionService.isBedrockPlayer(player)) {
                return;
            }
            plugin.getGuiManager().refreshGUI(player);
        }
    }
}
