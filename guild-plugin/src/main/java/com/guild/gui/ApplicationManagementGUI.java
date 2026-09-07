package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.gui.base.AbstractPagedListGUI;
import com.guild.gui.base.GuiLayoutUtils;
import com.guild.models.Guild;
import com.guild.models.GuildApplication;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.ArrayList;
import java.util.List;

/**
 * 申请管理 GUI：分页申请列表 + 待处理/历史切换。
 */
public class ApplicationManagementGUI extends AbstractPagedListGUI<GuildApplication> {

    public static final String FUNC_PENDING = "PENDING";
    public static final String FUNC_HISTORY = "HISTORY";

    private static final int TOOLBAR_PENDING = 47;
    private static final int TOOLBAR_BACK = 49;
    private static final int TOOLBAR_HISTORY = 51;

    private final Guild guild;
    private boolean showingHistory = false;

    public ApplicationManagementGUI(GuildPlugin plugin, Guild guild, Player player) {
        super(plugin, player, PaginationLayout.SIDE, GuiLayoutUtils.ITEMS_PER_PAGE);
        this.guild = guild;
        loadApplications();
    }

    private void loadApplications() {
        var future = showingHistory
                ? plugin.getGuildService().getApplicationHistoryAsync(guild.getId())
                : plugin.getGuildService().getPendingApplicationsAsync(guild.getId());
        future.thenAccept(applications -> {
            setEntries(applications == null ? List.of() : applications);
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
                "gui.application-mgmt.application-management-title", "&6Application Management"));
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
    }

    @Override
    protected String prevPageTitleKey() {
        return "gui.common.previous-page-name";
    }

    @Override
    protected String prevPageTitleDefault() {
        return "&cPrevious Page";
    }

    @Override
    protected String prevPageLoreKey() {
        return "gui.common.previous-page-lore-1";
    }

    @Override
    protected String prevPageLoreDefault() {
        return "&7Page {page}";
    }

    @Override
    protected String nextPageTitleKey() {
        return "gui.common.next-page-name";
    }

    @Override
    protected String nextPageTitleDefault() {
        return "&aNext Page";
    }

    @Override
    protected String nextPageLoreKey() {
        return "gui.common.next-page-lore-1";
    }

    @Override
    protected String nextPageLoreDefault() {
        return "&7Page {page}";
    }

    @Override
    protected void displayEmptyState(Inventory inventory) {
        if (showingHistory) {
            inventory.setItem(22, createItem(
                    Material.BARRIER,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.application-mgmt.no-history", "&aNo Application History")),
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.application-mgmt.no-history.desc",
                            "&7There is no application history"))));
        } else {
            inventory.setItem(22, createItem(
                    Material.BARRIER,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.application-mgmt.application-management-no-pending",
                            "&aNo Pending Applications")),
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.application-mgmt.application-management-no-pending-desc",
                            "&7There are no pending applications"))));
        }
    }

    @Override
    protected void setupToolbar(Inventory inventory) {
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            int pendingCount = applications != null ? applications.size() : 0;
            ItemStack pendingApplications = createItem(
                    Material.PAPER,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.application-mgmt.application-management-pending-applications-name",
                            "&ePending Applications")),
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.application-mgmt.application-management-pending-applications-lore-1",
                            "&7View pending applications")),
                    ColorUtils.colorize("&f" + pendingCount + " "
                            + languageManager.getGuiMessage(viewer,
                            "gui.application-mgmt.application-management-applications-count", "applications")));
            CompatibleScheduler.runTask(plugin, viewer, () -> {
                inventory.setItem(TOOLBAR_PENDING, pendingApplications);
                plugin.getGuiManager().applyImageModeIfNeeded(viewer, inventory, getGuiType());
            });
        });

        inventory.setItem(TOOLBAR_HISTORY, createItem(
                Material.BOOK,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.application-management-application-history-name",
                        "&eApplication History")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.application-management-application-history-lore-1",
                        "&7View application history"))));

        inventory.setItem(TOOLBAR_BACK, createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.common.back", "Back")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.common.back-to-main-menu", "Back to main menu"))));
    }

    @Override
    protected boolean handleToolbarClick(Player player, int slot, ClickType clickType) {
        switch (slot) {
            case TOOLBAR_PENDING -> {
                showingHistory = false;
                currentPage = 0;
                loadApplications();
                return true;
            }
            case TOOLBAR_HISTORY -> {
                showingHistory = true;
                currentPage = 0;
                loadApplications();
                return true;
            }
            case TOOLBAR_BACK -> {
                openBackGui(player);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected ItemStack createEntryItem(GuildApplication application) {
        String name;
        String colorPrefix;
        String statusMessage;
        List<String> lore = new ArrayList<>();

        switch (application.getStatus()) {
            case PENDING -> {
                colorPrefix = "&e";
                statusMessage = languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.status-pending", "Pending");
                lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.status", "Status") + ": &e" + statusMessage));
                lore.add(PlaceholderUtils.replaceApplicationPlaceholders("&7"
                        + languageManager.getGuiMessage(viewer, "gui.application-mgmt.apply-time", "Apply time")
                        + ": {apply_time}", application.getPlayerName(), guild.getName(), application.getCreatedAt()));
                lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.message", "Message") + ": " + application.getMessage()));
                lore.add("");
                lore.add(ColorUtils.colorize("&a" + languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.left-accept", "Left click: Accept")));
                lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.right-reject", "Right click: Reject")));
            }
            case APPROVED -> {
                colorPrefix = "&a";
                statusMessage = languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.status-approved", "Approved");
                lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.status", "Status") + ": &a" + statusMessage));
            }
            case REJECTED -> {
                colorPrefix = "&c";
                statusMessage = languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.status-rejected", "Rejected");
                lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.status", "Status") + ": &c" + statusMessage));
            }
            default -> {
                colorPrefix = "&7";
                statusMessage = languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.status-unknown", "Unknown");
                lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.application-mgmt.status", "Status") + ": &7" + statusMessage));
            }
        }

        name = PlaceholderUtils.replaceApplicationPlaceholders(colorPrefix + "{applicant_name} "
                        + languageManager.getGuiMessage(viewer, "gui.application-mgmt.application-suffix",
                        "'s Application"),
                application.getPlayerName(), guild.getName(), application.getCreatedAt());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer applicant = Bukkit.getOfflinePlayer(application.getPlayerName());
            meta.setOwningPlayer(applicant);
            meta.setDisplayName(ColorUtils.colorize(name));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    protected void onEntryClick(Player player, GuildApplication application, ClickType clickType) {
        if (showingHistory) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.common.application-history-view-only", "&7This is read-only history")));
            return;
        }
        if (clickType == ClickType.LEFT) {
            processApplication(player, application, GuildApplication.ApplicationStatus.APPROVED);
        } else if (clickType == ClickType.RIGHT) {
            processApplication(player, application, GuildApplication.ApplicationStatus.REJECTED);
        }
    }

    private void processApplication(Player player, GuildApplication application,
                                    GuildApplication.ApplicationStatus status) {
        plugin.getGuildService().processApplicationAsync(application.getId(), status, player.getUniqueId())
                .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (!success) {
                        String failKey = status == GuildApplication.ApplicationStatus.APPROVED
                                ? "gui.common.application-accept-failed"
                                : "gui.common.application-reject-failed";
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                failKey, "&cFailed to process application")));
                        return;
                    }
                    if (status == GuildApplication.ApplicationStatus.APPROVED) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.common.application-accepted", "&aApplication accepted")));
                        Player applicant = Bukkit.getPlayer(application.getPlayerUuid());
                        if (applicant != null && applicant.isOnline()) {
                            String cleanGuildName = ColorUtils.stripColor(guild.getName());
                            applicant.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(applicant,
                                    "gui.application-mgmt.application.accepted",
                                    "&aYour application has been accepted by {guild}!",
                                    "{guild}", cleanGuildName)));
                        }
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.common.application-rejected", "&cApplication rejected")));
                    }
                    loadApplications();
                }));
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }
        sendBedrockApplicationList(player, 0, false);
        return true;
    }

    private void sendBedrockApplicationList(Player player, int page, boolean history) {
        var future = history
                ? plugin.getGuildService().getApplicationHistoryAsync(guild.getId())
                : plugin.getGuildService().getPendingApplicationsAsync(guild.getId());
        future.thenAccept(applications -> CompatibleScheduler.runTask(plugin, player, () -> {
            if (applications == null || applications.isEmpty()) {
                String emptyMsg = history
                        ? languageManager.getGuiColoredMessage(player,
                        "gui.application-mgmt.bedrock-empty-history", "&fNo application history")
                        : languageManager.getGuiColoredMessage(player,
                        "gui.application-mgmt.bedrock-empty-pending", "&fNo pending applications");
                SimpleForm form = SimpleForm.builder()
                        .title(languageManager.getGuiColoredMessage(player,
                                "gui.application-mgmt.bedrock-title", "&6Application Management"))
                        .content(emptyMsg)
                        .button(history
                                ? languageManager.getGuiColoredMessage(player,
                                "gui.application-mgmt.bedrock-pending-btn", "&ePending Applications")
                                : languageManager.getGuiColoredMessage(player,
                                "gui.application-mgmt.bedrock-history-btn", "&eApplication History"))
                        .button(languageManager.getGuiColoredMessage(player,
                                "gui.application-mgmt.bedrock-back", "&cBack"))
                        .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                            if (response.clickedButtonId() == 0) {
                                sendBedrockApplicationList(player, 0, !history);
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

            final int itemsPerPage = GuiLayoutUtils.BEDROCK_ITEMS_PER_PAGE;
            int totalPages = GuiLayoutUtils.maxPageIndex(applications.size(), itemsPerPage);
            final int safePage = Math.max(0, Math.min(page, totalPages));
            final int startIndex = safePage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, applications.size());
            final int appCount = endIndex - startIndex;
            final boolean isHistory = history;

            String modeText = history
                    ? languageManager.getGuiColoredMessage(player,
                    "gui.application-mgmt.bedrock-mode-history", "Application History")
                    : languageManager.getGuiColoredMessage(player,
                    "gui.application-mgmt.bedrock-mode-pending", "Pending Applications");
            SimpleForm.Builder builder = SimpleForm.builder()
                    .title(languageManager.getGuiColoredMessage(player,
                            "gui.application-mgmt.bedrock-title-page",
                            "&6Application Management - {mode} Page {page}",
                            "{mode}", modeText, "{page}", String.valueOf(safePage + 1)))
                    .content(languageManager.getGuiColoredMessage(player,
                            "gui.application-mgmt.bedrock-total", "&fTotal {count} records",
                            "{count}", String.valueOf(applications.size())));

            for (int i = startIndex; i < endIndex; i++) {
                GuildApplication app = applications.get(i);
                String statusColor = switch (app.getStatus()) {
                    case PENDING -> "§e";
                    case APPROVED -> "§a";
                    case REJECTED -> "§c";
                    default -> "§f";
                };
                builder.button(statusColor + app.getPlayerName());
            }

            builder.button(history
                    ? languageManager.getGuiColoredMessage(player,
                    "gui.application-mgmt.bedrock-pending-btn", "&ePending Applications")
                    : languageManager.getGuiColoredMessage(player,
                    "gui.application-mgmt.bedrock-history-btn", "&eApplication History"));
            builder.button(languageManager.getGuiColoredMessage(player,
                    "gui.application-mgmt.bedrock-prev-page", "&ePrevious Page"));
            builder.button(languageManager.getGuiColoredMessage(player,
                    "gui.application-mgmt.bedrock-next-page", "&eNext Page"));
            builder.button(languageManager.getGuiColoredMessage(player,
                    "gui.application-mgmt.bedrock-back", "&cBack"));

            builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                int clicked = response.clickedButtonId();
                if (clicked < appCount) {
                    GuildApplication app = applications.get(startIndex + clicked);
                    if (isHistory) {
                        player.sendMessage(languageManager.getGuiColoredMessage(player,
                                "gui.application-mgmt.bedrock-applicant", "&fApplicant: {name}",
                                "{name}", app.getPlayerName()));
                        player.sendMessage(languageManager.getGuiColoredMessage(player,
                                "gui.application-mgmt.bedrock-status", "&fStatus: {status}",
                                "{status}", app.getStatus().name()));
                        if (app.getMessage() != null && !app.getMessage().isEmpty()) {
                            player.sendMessage(languageManager.getGuiColoredMessage(player,
                                    "gui.application-mgmt.bedrock-message", "&fMessage: {msg}",
                                    "{msg}", app.getMessage()));
                        }
                        sendBedrockApplicationList(player, safePage, true);
                    } else {
                        sendBedrockApplicationActions(player, app);
                    }
                } else if (clicked == appCount) {
                    sendBedrockApplicationList(player, 0, !isHistory);
                } else if (clicked == appCount + 1) {
                    sendBedrockApplicationList(player, safePage - 1, isHistory);
                } else if (clicked == appCount + 2) {
                    sendBedrockApplicationList(player, safePage + 1, isHistory);
                } else {
                    openBackGui(player);
                }
            }));

            builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                    () -> openBackGui(player)));

            BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
        }));
    }

    private void sendBedrockApplicationActions(Player player, GuildApplication application) {
        String msg = application.getMessage() != null && !application.getMessage().isEmpty()
                ? application.getMessage()
                : languageManager.getGuiColoredMessage(player,
                "gui.application-mgmt.bedrock-no-message", "None");
        SimpleForm form = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player,
                        "gui.application-mgmt.bedrock-actions-title",
                        "&6Application Processing - {name}", "{name}", application.getPlayerName()))
                .content(languageManager.getGuiColoredMessage(player,
                        "gui.application-mgmt.bedrock-actions-content",
                        "&fApplicant: {name}\n&fMessage: {msg}",
                        "{name}", application.getPlayerName(), "{msg}", msg))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.application-mgmt.bedrock-accept", "&aAccept Application"))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.application-mgmt.bedrock-reject", "&cReject Application"))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.application-mgmt.bedrock-back-to-list", "&eBack to List"))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> bedrockProcessApplication(player, application,
                                GuildApplication.ApplicationStatus.APPROVED);
                        case 1 -> bedrockProcessApplication(player, application,
                                GuildApplication.ApplicationStatus.REJECTED);
                        case 2 -> sendBedrockApplicationList(player, 0, false);
                    }
                }))
                .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                        () -> sendBedrockApplicationList(player, 0, false)))
                .build();
        BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    private void bedrockProcessApplication(Player player, GuildApplication application,
                                           GuildApplication.ApplicationStatus status) {
        plugin.getGuildService().processApplicationAsync(application.getId(), status, player.getUniqueId())
                .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        if (status == GuildApplication.ApplicationStatus.APPROVED) {
                            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                    "gui.common.application-accepted", "&aApplication accepted")));
                            Player applicant = Bukkit.getPlayer(application.getPlayerUuid());
                            if (applicant != null && applicant.isOnline()) {
                                String cleanGuildName = ColorUtils.stripColor(guild.getName());
                                applicant.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(applicant,
                                        "gui.application-mgmt.application.accepted",
                                        "&aYour application has been accepted by {guild}!",
                                        "{guild}", cleanGuildName)));
                            }
                        } else {
                            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                    "gui.common.application-rejected", "&cApplication rejected")));
                        }
                        sendBedrockApplicationList(player, 0, false);
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.common.application-accept-failed", "&cFailed to accept application")));
                    }
                }));
    }
}
