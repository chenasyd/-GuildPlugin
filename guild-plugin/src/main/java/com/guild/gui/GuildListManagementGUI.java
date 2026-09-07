package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.geyser.PlayerConnectionService;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.base.AbstractPagedListGUI;
import com.guild.gui.base.GuiLayoutUtils;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.ArrayList;
import java.util.List;

/**
 * 公会列表管理 GUI（管理员）：3 列紧凑网格 + 分页。
 */
public class GuildListManagementGUI extends AbstractPagedListGUI<Guild> {

    public static final String FUNC_REFRESH = "REFRESH";

    private static final int TOOLBAR_BACK = 46;
    private static final int SLOT_PAGE_INFO = 49;
    private static final int TOOLBAR_REFRESH = 52;

    public GuildListManagementGUI(GuildPlugin plugin, Player player) {
        super(plugin, player, PaginationLayout.CENTER_BAR_PAGE_INFO, GuiLayoutUtils.ADMIN_GRID_ITEMS_PER_PAGE);
        loadGuilds();
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.guild-list-management.guild-list-management-title",
                "&4Guild List Management"));
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new AdminGuildGUI(plugin, player));
    }

    @Override
    protected int slotForEntryIndex(int pageIndex) {
        return GuiLayoutUtils.slotForGridPageIndex(pageIndex,
                GuiLayoutUtils.ADMIN_GRID_COLS, GuiLayoutUtils.ADMIN_GRID_START_COL);
    }

    @Override
    protected int listIndexFromSlot(int slot) {
        return GuiLayoutUtils.listIndexFromGridSlot(slot, currentPage, itemsPerPage(),
                GuiLayoutUtils.ADMIN_GRID_ROWS, GuiLayoutUtils.ADMIN_GRID_COLS,
                GuiLayoutUtils.ADMIN_GRID_START_COL);
    }

    @Override
    protected void setupToolbar(Inventory inventory) {
        inventory.setItem(TOOLBAR_BACK, createItem(Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.common.back", "Back"))));
        inventory.setItem(TOOLBAR_REFRESH, createItem(Material.EMERALD,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.guild-list-management.gui-refresh", "&aRefresh List"))));
    }

    @Override
    protected void setupNavigationButtons(Inventory inventory) {
        if (currentPage > 0) {
            inventory.setItem(slotForFunction(FUNC_PREV_PAGE, 48),
                    createItem(Material.ARROW,
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    "gui.common.previous-page", "&e&lPrevious Page")),
                            ColorUtils.colorize("&7" + languageManager.getGuiIndexedMessage(viewer,
                                    "gui.common.page-info", "Page {0} of {1}",
                                    String.valueOf(currentPage), String.valueOf(maxPageIndex() + 1)))));
        }

        int totalPages = entries.isEmpty() ? 1 : maxPageIndex() + 1;
        inventory.setItem(SLOT_PAGE_INFO, createItem(Material.PAPER,
                ColorUtils.colorize("&e" + languageManager.getGuiIndexedMessage(viewer,
                        "gui.common.page-info", "Page {0} of {1}",
                        String.valueOf(currentPage + 1), String.valueOf(totalPages)))));

        if (currentPage < maxPageIndex()) {
            inventory.setItem(slotForFunction(FUNC_NEXT_PAGE, 50),
                    createItem(Material.ARROW,
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    "gui.common.next-page", "&e&lNext Page")),
                            ColorUtils.colorize("&7" + languageManager.getGuiIndexedMessage(viewer,
                                    "gui.common.page-info", "Page {0} of {1}",
                                    String.valueOf(currentPage + 2), String.valueOf(totalPages)))));
        }
    }

    @Override
    protected boolean handleToolbarClick(Player player, int slot, ClickType clickType) {
        if (slot == TOOLBAR_BACK) {
            openBackGui(player);
            return true;
        }
        if (slot == TOOLBAR_REFRESH) {
            loadGuilds();
            return true;
        }
        return false;
    }

    @Override
    protected ItemStack createEntryItem(Guild guild) {
        Material material = guild.isFrozen() ? Material.RED_WOOL : Material.GREEN_WOOL;
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.common.leader", "Leader") + ": &e" + guild.getLeaderName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-list.level", "Level") + ": &e" + guild.getLevel()
                + "  &7" + languageManager.getGuiMessage(viewer, "gui.guild-list.balance", "Balance")
                + ": &a" + plugin.getEconomyManager().format(guild.getBalance())));
        lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer,
                "gui.guild-list.left-click-view", "Left click: View")
                + "  &c" + languageManager.getGuiMessage(viewer,
                "gui.guild-list.right-click-delete", "Right click: Delete")
                + "  &6" + languageManager.getGuiMessage(viewer,
                "gui.guild-list.shift-right-freeze", "Shift+Right click: Freeze/Unfreeze")));
        return createItem(material, ColorUtils.colorize("&6" + guild.getName()), lore.toArray(new String[0]));
    }

    @Override
    protected void onEntryClick(Player player, Guild guild, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            plugin.getGuiManager().openGUI(player, new GuildDetailGUI(plugin, guild, player));
        } else if (clickType == ClickType.RIGHT) {
            deleteGuild(player, guild);
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            toggleGuildFreeze(player, guild);
        }
    }

    private void loadGuilds() {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            setEntries(guilds == null ? List.of() : guilds);
            CompatibleScheduler.runTask(plugin, viewer, () -> {
                if (viewer.isOnline()) {
                    refresh(viewer);
                }
            });
        });
    }

    private void deleteGuild(Player player, Guild guild) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ColorUtils.colorize("&c您没有权限执行此操作！"));
            return;
        }
        plugin.getGuiManager().openGUI(player,
                new ConfirmDeleteGuildGUI(plugin, guild, player, "GuildListManagementGUI"));
    }

    private void toggleGuildFreeze(Player player, Guild guild) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.common.no-permission", "&cInsufficient permission")));
            return;
        }
        boolean newStatus = !guild.isFrozen();
        plugin.getGuildService().updateGuildFrozenStatusAsync(guild.getId(), newStatus, player.getUniqueId())
                .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        String message = newStatus
                                ? languageManager.getGuiMessage(player, "gui.guild-detail.guild-frozen",
                                "&aGuild {guild} has been frozen!", "{guild}", guild.getName())
                                : languageManager.getGuiMessage(player, "gui.guild-detail.guild-unfrozen",
                                "&aGuild {guild} has been unfrozen!", "{guild}", guild.getName());
                        player.sendMessage(ColorUtils.colorize(message));
                        loadGuilds();
                    } else {
                        player.sendMessage(ColorUtils.colorize("&c操作失败！"));
                    }
                }));
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }
        sendBedrockGuildList(player, 0);
        return true;
    }

    private void sendBedrockGuildList(Player player, int page) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (!player.isOnline()) {
                    return;
                }

                int itemsPerPage = GuiLayoutUtils.BEDROCK_ITEMS_PER_PAGE;
                int totalPages = Math.max(1, (int) Math.ceil((double) guilds.size() / itemsPerPage));
                final int safePage = Math.max(0, Math.min(page, totalPages - 1));
                int startIndex = safePage * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, guilds.size());

                SimpleForm.Builder builder = SimpleForm.builder()
                        .title(languageManager.getGuiColoredMessage(player,
                                "gui.guild-list-management.bedrock-title", "&4Guild List Management"))
                        .content(languageManager.getGuiColoredMessage(player,
                                "gui.guild-list-management.bedrock-page-info",
                                "&fPage {page}/{total} | Total {count} guilds",
                                "{page}", String.valueOf(safePage + 1),
                                "{total}", String.valueOf(totalPages),
                                "{count}", String.valueOf(guilds.size())));

                List<Guild> pageGuilds = new ArrayList<>();
                for (int i = startIndex; i < endIndex; i++) {
                    Guild g = guilds.get(i);
                    pageGuilds.add(g);
                    String prefix = g.isFrozen() ? "§c" : "§6";
                    builder.button(prefix + g.getName() + " §f[Lv." + g.getLevel() + "]");
                }

                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list-management.bedrock-refresh", "&aRefresh List"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list-management.bedrock-prev-page", "&ePrevious Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list-management.bedrock-next-page", "&eNext Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list-management.bedrock-back", "&cBack"));

                final int navOffset = pageGuilds.size();

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int id = response.clickedButtonId();
                    if (id < navOffset) {
                        sendBedrockGuildActions(player, pageGuilds.get(id), safePage);
                    } else if (id == navOffset) {
                        sendBedrockGuildList(player, safePage);
                    } else if (id == navOffset + 1) {
                        sendBedrockGuildList(player, safePage > 0 ? safePage - 1 : safePage);
                    } else if (id == navOffset + 2) {
                        sendBedrockGuildList(player, safePage < totalPages - 1 ? safePage + 1 : safePage);
                    } else {
                        openBackGui(player);
                    }
                }));

                builder.closedResultHandler(response -> {
                });

                BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
            });
        });
    }

    private void sendBedrockGuildActions(Player player, Guild guild, int page) {
        String status = guild.isFrozen()
                ? languageManager.getGuiColoredMessage(player,
                "gui.guild-list-management.bedrock-status-frozen", "&cFrozen")
                : languageManager.getGuiColoredMessage(player,
                "gui.guild-list-management.bedrock-status-normal", "&aNormal");
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list-management.bedrock-actions-title",
                        "&6Guild Actions - {guild_name}", "{guild_name}", guild.getName()))
                .content(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list-management.bedrock-leader", "&fLeader: &e{leader}",
                        "{leader}", guild.getLeaderName())
                        + "\n" + languageManager.getGuiColoredMessage(player,
                        "gui.guild-list-management.bedrock-level", "&fLevel: &e{level}",
                        "{level}", String.valueOf(guild.getLevel()))
                        + "\n" + languageManager.getGuiColoredMessage(player,
                        "gui.guild-list-management.bedrock-balance", "&fBalance: &a{balance}",
                        "{balance}", plugin.getEconomyManager().format(guild.getBalance()))
                        + "\n" + languageManager.getGuiColoredMessage(player,
                        "gui.guild-list-management.bedrock-status", "&fStatus: {status}",
                        "{status}", status));

        builder.button(languageManager.getGuiColoredMessage(player,
                "gui.guild-list-management.bedrock-view-detail", "&eView Details"));
        builder.button(languageManager.getGuiColoredMessage(player,
                "gui.guild-list-management.bedrock-delete", "&cDelete Guild"));
        builder.button(guild.isFrozen()
                ? languageManager.getGuiColoredMessage(player,
                "gui.guild-list-management.bedrock-unfreeze", "&aUnfreeze Guild")
                : languageManager.getGuiColoredMessage(player,
                "gui.guild-list-management.bedrock-freeze", "&cFreeze Guild"));
        builder.button(languageManager.getGuiColoredMessage(player,
                "gui.guild-list-management.bedrock-back-list", "&cBack to List"));

        builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
            switch (response.clickedButtonId()) {
                case 0 -> plugin.getGuiManager().openGUI(player, new GuildDetailGUI(plugin, guild, player));
                case 1 -> deleteGuild(player, guild);
                case 2 -> bedrockToggleFreeze(player, guild, page);
                case 3 -> sendBedrockGuildList(player, page);
            }
        }));

        builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                () -> sendBedrockGuildList(player, page)));

        BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
    }

    private void bedrockToggleFreeze(Player player, Guild guild, int page) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.common.no-permission", "&cInsufficient permission")));
            return;
        }
        boolean newStatus = !guild.isFrozen();
        plugin.getGuildService().updateGuildFrozenStatusAsync(guild.getId(), newStatus, player.getUniqueId())
                .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        String message = newStatus
                                ? languageManager.getGuiMessage(player, "gui.guild-detail.guild-frozen",
                                "&aGuild {guild} has been frozen!", "{guild}", guild.getName())
                                : languageManager.getGuiMessage(player, "gui.guild-detail.guild-unfrozen",
                                "&aGuild {guild} has been unfrozen!", "{guild}", guild.getName());
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.common.operation-failed", "&cOperation failed!")));
                    }
                    sendBedrockGuildList(player, page);
                }));
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
