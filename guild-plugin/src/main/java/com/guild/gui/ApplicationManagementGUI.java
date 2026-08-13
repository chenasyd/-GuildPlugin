package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.models.Guild;
import com.guild.models.GuildApplication;

import org.geysermc.cumulus.form.SimpleForm;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 申请管理GUI
 */
public class ApplicationManagementGUI implements GUI {

    // ── 图像模式功能常量 ──
    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_PENDING = "PENDING";
    public static final String FUNC_HISTORY = "HISTORY";
    public static final String FUNC_BACK = "BACK";

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;
    private final Guild guild;
    private int currentPage = 0;
    private int totalPages = 0;
    private static final int APPLICATIONS_PER_PAGE = 28; // 4行7列，除去边框
    private boolean showingHistory = false; // false=待处理申请, true=申请历史

    public ApplicationManagementGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.application-mgmt.application-management-title", "&6Application Management"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 添加功能按钮
        setupFunctionButtons(inventory);
        
        // 加载申请列表
        loadApplications(inventory);

        // 同步处理：立即移除边框并转换已放置的功能物品（49, 51）
        // 异步回调中会再次调用以处理异步放置的物品（47, 列表内容）
        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 检查是否是功能按钮
        if (isFunctionButton(slot)) {
            handleFunctionButton(player, slot);
            return;
        }
        
        // 检查是否是分页按钮
        if (isPaginationButton(slot)) {
            handlePaginationButton(player, slot);
            return;
        }
        
        // 检查是否是申请按钮
        if (isApplicationSlot(slot)) {
            handleApplicationClick(player, slot, clickedItem, clickType);
        }
    }
    
    /**
     * 填充边框
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * 设置功能按钮
     */
    private void setupFunctionButtons(Inventory inventory) {
        // 异步获取待处理申请数量
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            int pendingCount = applications != null ? applications.size() : 0;
            
            // 待处理申请按钮
            ItemStack pendingApplications = createItem(
                Material.PAPER,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.application-mgmt.application-management-pending-applications-name", "&ePending Applications")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.application-mgmt.application-management-pending-applications-lore-1", "&7View pending applications")),
                ColorUtils.colorize("&f" + pendingCount + " " + languageManager.getGuiMessage(player, "gui.application-mgmt.application-management-applications-count", "applications"))
            );
            CompatibleScheduler.runTask(plugin, player, () -> {
                inventory.setItem(47, pendingApplications);
                plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
            });
        });
        
        // 申请历史按钮
        ItemStack applicationHistory = createItem(
            Material.BOOK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.application-mgmt.application-management-application-history-name", "&eApplication History")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.application-mgmt.application-management-application-history-lore-1", "&7View application history"))
        );
        inventory.setItem(51, applicationHistory);
        
        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.back", "Back")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.back-to-main-menu", "Back to main menu"))
        );
        inventory.setItem(49, back);
    }
    
    /**
     * 加载申请列表
     */
    private void loadApplications(Inventory inventory) {
        if (showingHistory) {
            loadApplicationHistory(inventory);
        } else {
            loadPendingApplications(inventory);
        }
    }
    
    /**
     * 加载待处理申请
     */
    private void loadPendingApplications(Inventory inventory) {
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (applications == null || applications.isEmpty()) {
                    // 显示无申请信息
                    ItemStack noApplications = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.application-mgmt.application-management-no-pending", "&aNo Pending Applications")),
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.application-mgmt.application-management-no-pending-desc", "&7There are no pending applications"))
                    );
                    inventory.setItem(22, noApplications);
                    return;
                }
                
                // 计算分页
                this.totalPages = (applications.size() - 1) / APPLICATIONS_PER_PAGE;
                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }
                
                // 设置分页按钮
                setupPaginationButtons(inventory, totalPages);
                inventory.setItem(22, null);
                
                // 显示当前页的申请
                displayApplications(inventory, applications);
                plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
            });
        });
    }
    
    /**
     * 加载申请历史
     */
    private void loadApplicationHistory(Inventory inventory) {
        plugin.getGuildService().getApplicationHistoryAsync(guild.getId()).thenAccept(applications -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (applications == null || applications.isEmpty()) {
                    // 显示无历史信息
                    ItemStack noHistory = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.application-mgmt.no-history", "&aNo Application History")),
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.application-mgmt.no-history.desc", "&7There is no application history"))
                    );
                    inventory.setItem(22, noHistory);
                    return;
                }
                
                // 计算分页
                this.totalPages = (applications.size() - 1) / APPLICATIONS_PER_PAGE;
                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }
                
                // 设置分页按钮
                setupPaginationButtons(inventory, totalPages);
                inventory.setItem(22, null);
                
                // 显示当前页的申请
                displayApplications(inventory, applications);
                plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
            });
        });
    }
    
    /**
     * 显示申请列表
     */
    private void displayApplications(Inventory inventory, List<GuildApplication> applications) {
        int startIndex = currentPage * APPLICATIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + APPLICATIONS_PER_PAGE, applications.size());
        
        int slotIndex = 10; // 从第2行第2列开始
        for (int i = startIndex; i < endIndex; i++) {
            GuildApplication application = applications.get(i);
            if (slotIndex >= 44) break; // 避免超出显示区域
            
            ItemStack applicationItem = createApplicationItem(application);
            inventory.setItem(slotIndex, applicationItem);
            
            slotIndex++;
            if (slotIndex % 9 == 8) { // 跳过边框
                slotIndex += 2;
            }
        }
    }
    
    /**
     * 设置分页按钮
     */
    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        // 上一页按钮
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.previous-page-name", "&cPrevious Page")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.previous-page-lore-1", "&7Page {page}"))
            );
            inventory.setItem(18, previousPage);
        }

        // 下一页按钮
        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.next-page-name", "&aNext Page")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.next-page-lore-1", "&7Page {page}"))
            );
            inventory.setItem(26, nextPage);
        }
    }
    
    /**
     * 创建申请物品
     */
    private ItemStack createApplicationItem(GuildApplication application) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();

        switch (application.getStatus()) {
            case PENDING:
                material = Material.YELLOW_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&e{applicant_name} " + languageManager.getGuiMessage(player, "gui.application-mgmt.application-suffix", "'s Application"), application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.application-mgmt.status", "gui.guild-relations.status") + ": &e" + languageManager.getGuiMessage(player, "gui.application-mgmt.status-pending", "Pending")));
                lore.add(PlaceholderUtils.replaceApplicationPlaceholders("&7" + languageManager.getGuiMessage(player, "gui.application-mgmt.apply-time", "Apply time") + ": {apply_time}", application.getPlayerName(), guild.getName(), application.getCreatedAt()));
                lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.application-mgmt.message", "gui.application-mgmt.message") + ": " + application.getMessage()));
                lore.add("");
                lore.add(ColorUtils.colorize("&a" + languageManager.getGuiMessage(player, "gui.application-mgmt.left-accept", "Left click: Accept")));
                lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(player, "gui.application-mgmt.right-reject", "Right click: Reject")));
                break;
            case APPROVED:
                material = Material.GREEN_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&a{applicant_name} " + languageManager.getGuiMessage(player, "gui.application-mgmt.application-suffix", "'s Application"), application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.application-mgmt.status", "gui.guild-relations.status") + ": &a" + languageManager.getGuiMessage(player, "gui.application-mgmt.status-approved", "Approved")));
                break;
            case REJECTED:
                material = Material.RED_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&c{applicant_name} " + languageManager.getGuiMessage(player, "gui.application-mgmt.application-suffix", "'s Application"), application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.application-mgmt.status", "gui.guild-relations.status") + ": &c" + languageManager.getGuiMessage(player, "gui.application-mgmt.status-rejected", "Rejected")));
                break;
            default:
                material = Material.GRAY_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&7{applicant_name} " + languageManager.getGuiMessage(player, "gui.application-mgmt.application-suffix", "'s Application"), application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.application-mgmt.status", "gui.guild-relations.status") + ": &7" + languageManager.getGuiMessage(player, "gui.application-mgmt.status-unknown", "gui.guild-relations.unknown")));
                break;
        }

        return createItem(material, name, lore.toArray(new String[0]));
    }
    
    /**
     * 检查是否是功能按钮
     */
    private boolean isFunctionButton(int slot) {
        return slot == 47 || slot == 51 || slot == 49;
    }
    
    /**
     * 检查是否是分页按钮
     */
    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }
    
    /**
     * 检查是否是申请槽位
     */
    private boolean isApplicationSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8 && slot != 22;
    }
    
    /**
     * 处理功能按钮点击
     */
    private void handleFunctionButton(Player player, int slot) {
        switch (slot) {
            case 47: // 待处理申请
                showingHistory = false;
                currentPage = 0;
                refreshInventory(player);
                break;
            case 51: // 申请历史
                showingHistory = true;
                currentPage = 0;
                refreshInventory(player);
                break;
            case 49: // 返回
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                break;
        }
    }
    
    /**
     * 处理分页按钮点击
     */
    private void handlePaginationButton(Player player, int slot) {
        if (slot == 18) { // 上一页
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
        } else if (slot == 26) { // 下一页
            if (currentPage < totalPages) {
                currentPage++;
                refreshInventory(player);
            }
        }
    }
    
    /**
     * 处理申请点击
     */
    private void handleApplicationClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (showingHistory) {
            // 历史记录只能查看，不能操作
            String message = languageManager.getGuiMessage(player, "gui.common.application-history-view-only", "&7This is read-only history");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 待处理申请可以接受或拒绝
        if (clickType == ClickType.LEFT) {
            // 接受申请
            handleAcceptApplication(player, slot);
        } else if (clickType == ClickType.RIGHT) {
            // 拒绝申请
            handleRejectApplication(player, slot);
        }
    }
    
    /**
     * 处理接受申请
     */
    private void handleAcceptApplication(Player player, int slot) {
        // 获取当前页的申请列表
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getGuiMessage(player, "gui.common.no-pending-applications", "&aNo pending applications");
                    player.sendMessage(ColorUtils.colorize(message));
                });
                return;
            }

            // 计算申请在列表中的索引
            int applicationIndex = currentPage * APPLICATIONS_PER_PAGE + (slot - 10);
            if (applicationIndex >= 0 && applicationIndex < applications.size()) {
                GuildApplication application = applications.get(applicationIndex);

                // 处理申请
                plugin.getGuildService().processApplicationAsync(application.getId(), GuildApplication.ApplicationStatus.APPROVED, player.getUniqueId()).thenAccept(success -> {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        if (success) {
                            String message = languageManager.getGuiMessage(player, "gui.common.application-accepted", "&aApplication accepted");
                            player.sendMessage(ColorUtils.colorize(message));

                            // 向申请者发送消息
                            Player applicant = Bukkit.getPlayer(application.getPlayerUuid());
                            if (applicant != null && applicant.isOnline()) {
                                // 去除工会名称中的颜色代码
                                String cleanGuildName = ColorUtils.stripColor(guild.getName());
                                String acceptedMessage = languageManager.getGuiMessage(applicant, "gui.application-mgmt.application.accepted", "&aYour application has been accepted by {guild}!", "{guild}", cleanGuildName);
                                applicant.sendMessage(ColorUtils.colorize(acceptedMessage));
                            }

                            // 刷新GUI
                            refreshInventory(player);
                        } else {
                            String message = languageManager.getGuiMessage(player, "gui.common.application-accept-failed", "&cFailed to accept application");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                    });
                });
            }
        });
    }
    
    /**
     * 处理拒绝申请
     */
    private void handleRejectApplication(Player player, int slot) {
        // 获取当前页的申请列表
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getGuiMessage(player, "gui.common.no-pending-applications", "&aNo pending applications");
                    player.sendMessage(ColorUtils.colorize(message));
                });
                return;
            }

            // 计算申请在列表中的索引
            int applicationIndex = currentPage * APPLICATIONS_PER_PAGE + (slot - 10);
            if (applicationIndex >= 0 && applicationIndex < applications.size()) {
                GuildApplication application = applications.get(applicationIndex);

                // 处理申请
                plugin.getGuildService().processApplicationAsync(application.getId(), GuildApplication.ApplicationStatus.REJECTED, player.getUniqueId()).thenAccept(success -> {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        if (success) {
                            String message = languageManager.getGuiMessage(player, "gui.common.application-rejected", "&cApplication rejected");
                            player.sendMessage(ColorUtils.colorize(message));

                            // 刷新GUI
                            refreshInventory(player);
                        } else {
                            String message = languageManager.getGuiMessage(player, "gui.common.application-reject-failed", "&cFailed to reject application");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                    });
                });
            }
        });
    }
    
    // ── 基岩版表单 ──

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;
        sendBedrockApplicationList(player, 0, false);
        return true;
    }

    private void sendBedrockApplicationList(Player player, int page, boolean history) {
        java.util.concurrent.CompletableFuture<List<GuildApplication>> future = history
            ? plugin.getGuildService().getApplicationHistoryAsync(guild.getId())
            : plugin.getGuildService().getPendingApplicationsAsync(guild.getId());
        future.thenAccept(applications -> CompatibleScheduler.runTask(plugin, player, () -> {
            if (applications == null || applications.isEmpty()) {
                String emptyMsg = history
                    ? languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-empty-history", "&fNo application history")
                    : languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-empty-pending", "&fNo pending applications");
                SimpleForm form = SimpleForm.builder()
                    .title(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-title", "&6Application Management"))
                    .content(emptyMsg)
                    .button(history
                        ? languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-pending-btn", "&ePending Applications")
                        : languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-history-btn", "&eApplication History"))
                    .button(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-back", "&cBack"))
                    .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                        if (response.clickedButtonId() == 0) {
                            sendBedrockApplicationList(player, 0, !history);
                        } else {
                            plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                        }
                    }))
                    .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                        plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player))))
                    .build();
                BedrockFormSender.sendForm(player.getUniqueId(), form);
                return;
            }

            final int itemsPerPage = 10;
            int totalPages = (applications.size() - 1) / itemsPerPage;
            final int safePage = Math.max(0, Math.min(page, totalPages));
            final int startIndex = safePage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, applications.size());
            final int appCount = endIndex - startIndex;
            final boolean isHistory = history;

            String modeText = history
                ? languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-mode-history", "Application History")
                : languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-mode-pending", "Pending Applications");
            SimpleForm.Builder builder = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-title-page", "&6Application Management - {mode} Page {page}", "{mode}", modeText, "{page}", String.valueOf(safePage + 1)))
                .content(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-total", "&fTotal {count} records", "{count}", String.valueOf(applications.size())));

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
                ? languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-pending-btn", "&ePending Applications")
                : languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-history-btn", "&eApplication History"));
            builder.button(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-prev-page", "&ePrevious Page"));
            builder.button(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-next-page", "&eNext Page"));
            builder.button(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-back", "&cBack"));

            builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                int clicked = response.clickedButtonId();
                if (clicked < appCount) {
                    GuildApplication app = applications.get(startIndex + clicked);
                    if (isHistory) {
                        player.sendMessage(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-applicant", "&fApplicant: {name}", "{name}", app.getPlayerName()));
                        player.sendMessage(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-status", "&fStatus: {status}", "{status}", app.getStatus().name()));
                        if (app.getMessage() != null && !app.getMessage().isEmpty()) {
                            player.sendMessage(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-message", "&fMessage: {msg}", "{msg}", app.getMessage()));
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
                    plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                }
            }));

            builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player))));

            BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
        }));
    }

    private void sendBedrockApplicationActions(Player player, GuildApplication application) {
        String msg = application.getMessage() != null && !application.getMessage().isEmpty()
            ? application.getMessage() : languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-no-message", "None");
        SimpleForm form = SimpleForm.builder()
            .title(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-actions-title", "&6Application Processing - {name}", "{name}", application.getPlayerName()))
            .content(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-actions-content", "&fApplicant: {name}\n&fMessage: {msg}", "{name}", application.getPlayerName(), "{msg}", msg))
            .button(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-accept", "&aAccept Application"))
            .button(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-reject", "&cReject Application"))
            .button(languageManager.getGuiColoredMessage(player, "gui.application-mgmt.bedrock-back-to-list", "&eBack to List"))
            .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                switch (response.clickedButtonId()) {
                    case 0 -> bedrockProcessApplication(player, application, GuildApplication.ApplicationStatus.APPROVED);
                    case 1 -> bedrockProcessApplication(player, application, GuildApplication.ApplicationStatus.REJECTED);
                    case 2 -> sendBedrockApplicationList(player, 0, false);
                }
            }))
            .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                sendBedrockApplicationList(player, 0, false)))
            .build();
        BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    private void bedrockProcessApplication(Player player, GuildApplication application, GuildApplication.ApplicationStatus status) {
        plugin.getGuildService().processApplicationAsync(application.getId(), status, player.getUniqueId()).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    if (status == GuildApplication.ApplicationStatus.APPROVED) {
                        String message = languageManager.getGuiMessage(player, "gui.common.application-accepted", "&aApplication accepted");
                        player.sendMessage(ColorUtils.colorize(message));
                        Player applicant = Bukkit.getPlayer(application.getPlayerUuid());
                        if (applicant != null && applicant.isOnline()) {
                            String cleanGuildName = ColorUtils.stripColor(guild.getName());
                            String acceptedMessage = languageManager.getGuiMessage(applicant, "gui.application-mgmt.application.accepted", "&aYour application has been accepted by {guild}!", "{guild}", cleanGuildName);
                            applicant.sendMessage(ColorUtils.colorize(acceptedMessage));
                        }
                    } else {
                        String message = languageManager.getGuiMessage(player, "gui.common.application-rejected", "&cApplication rejected");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                    sendBedrockApplicationList(player, 0, false);
                } else {
                    String message = languageManager.getGuiMessage(player, "gui.common.application-accept-failed", "&cFailed to accept application");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    /**
     * 刷新库存
     */
    private void refreshInventory(Player player) {
        plugin.getGuiManager().refreshGUI(player);
    }
    
    /**
     * 创建物品
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
