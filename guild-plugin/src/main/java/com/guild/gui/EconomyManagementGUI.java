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
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.ArrayList;
import java.util.List;

/**
 * 经济管理 GUI（管理员）：标准 7×4 分页 + 资金操作。
 */
public class EconomyManagementGUI extends AbstractPagedListGUI<Guild> {

    public static final String FUNC_REFRESH = "REFRESH";

    private static final int TOOLBAR_BACK = 46;
    private static final int SLOT_PAGE_INFO = 49;
    private static final int TOOLBAR_REFRESH = 52;

    public EconomyManagementGUI(GuildPlugin plugin, Player player) {
        super(plugin, player, PaginationLayout.CENTER_BAR_PAGE_INFO, GuiLayoutUtils.ITEMS_PER_PAGE);
        loadGuilds();
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.economy-management.economy-management-title", "&eEconomy Management"));
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
    protected void setupToolbar(Inventory inventory) {
        inventory.setItem(TOOLBAR_BACK, createItem(Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.economy-management.back", "&cBack"))));
        inventory.setItem(TOOLBAR_REFRESH, createItem(Material.EMERALD,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.economy-management.refresh", "&aRefresh List"))));
    }

    @Override
    protected void setupNavigationButtons(Inventory inventory) {
        if (currentPage > 0) {
            inventory.setItem(slotForFunction(FUNC_PREV_PAGE, 48),
                    createItem(Material.ARROW,
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    "gui.economy-management.previous-page", "&aPrevious Page")),
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    "gui.economy-management.previous-page.desc", "&7Page {page}",
                                    "{page}", String.valueOf(currentPage)))));
        }

        int totalPages = entries.isEmpty() ? 1 : maxPageIndex() + 1;
        inventory.setItem(SLOT_PAGE_INFO, createItem(Material.PAPER,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.economy-management.page-info", "&ePage {current} of {total}",
                        "{current}", String.valueOf(currentPage + 1),
                        "{total}", String.valueOf(totalPages)))));

        if (currentPage < maxPageIndex()) {
            inventory.setItem(slotForFunction(FUNC_NEXT_PAGE, 50),
                    createItem(Material.ARROW,
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    "gui.economy-management.next-page", "&aNext Page")),
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    "gui.economy-management.next-page.desc", "&7Page {page}",
                                    "{page}", String.valueOf(currentPage + 2)))));
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
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.common.guild-name", "Guild Name") + ": " + guild.getName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.common.leader", "Leader") + ": " + guild.getLeaderName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.economy-management.level", "Level") + ": " + guild.getLevel()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.economy-management.current-balance", "Current Balance")
                + ": " + plugin.getEconomyManager().format(guild.getBalance())));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.economy-management.max-members", "Max Members") + ": " + guild.getMaxMembers()));
        lore.add("");
        lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer,
                "gui.economy-management.left-click-set", "Left click: Set balance")));
        lore.add(ColorUtils.colorize("&a" + languageManager.getGuiMessage(viewer,
                "gui.economy-management.right-click-add", "Right click: Add balance")));
        lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                "gui.economy-management.middle-click-remove", "Middle click: Remove balance")));
        return createItem(Material.GOLD_INGOT, ColorUtils.colorize("&6" + guild.getName()),
                lore.toArray(new String[0]));
    }

    @Override
    protected void onEntryClick(Player player, Guild guild, ClickType clickType) {
        if (clickType == ClickType.MIDDLE) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.economy-management.middle-click-desc",
                    "&cAbout to clear funds of &e{guild}&c, please confirm", "{guild}", guild.getName())));
            plugin.getGuiManager().openGUI(player,
                    new ConfirmChangeFundsGUI(plugin, guild, player, "remove", guild.getBalance()));
            return;
        }

        String operationType;
        String promptKey;
        if (clickType == ClickType.LEFT) {
            operationType = "set";
            promptKey = "gui.economy-management.set-prompt";
        } else if (clickType == ClickType.RIGHT) {
            operationType = "add";
            promptKey = "gui.economy-management.add-prompt";
        } else {
            return;
        }

        plugin.getGuiManager().closeGUI(player);
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player, promptKey,
                "&eEnter amount in chat:")));

        plugin.getGuiManager().setInputMode(player, input -> {
            try {
                double amount = Double.parseDouble(input.trim());
                if (amount <= 0) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.economy-management.invalid-amount",
                            "&cAmount must be greater than 0!")));
                    return false;
                }
                plugin.getGuiManager().openGUI(player,
                        new ConfirmChangeFundsGUI(plugin, guild, player, operationType, amount));
                return true;
            } catch (NumberFormatException e) {
                if (input.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.economy-management.input-cancelled", "&7Operation cancelled")));
                    return true;
                }
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.economy-management.invalid-number",
                        "&cInvalid number! Enter a valid amount or type cancel to abort")));
                return false;
            }
        });
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

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }
        sendBedrockEconomyList(player, 0);
        return true;
    }

    private void sendBedrockEconomyList(Player player, int page) {
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
                                "gui.economy-management.bedrock-title", "&eEconomy Management"))
                        .content(languageManager.getGuiColoredMessage(player,
                                "gui.economy-management.bedrock-page-info",
                                "&fPage {current}/{total} | Total {count} guilds",
                                "{current}", String.valueOf(safePage + 1),
                                "{total}", String.valueOf(totalPages),
                                "{count}", String.valueOf(guilds.size())));

                List<Guild> pageGuilds = new ArrayList<>();
                for (int i = startIndex; i < endIndex; i++) {
                    Guild g = guilds.get(i);
                    pageGuilds.add(g);
                    builder.button("§6" + g.getName() + " §f- " + plugin.getEconomyManager().format(g.getBalance()));
                }

                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.economy-management.bedrock-refresh", "&aRefresh List"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.economy-management.bedrock-prev-page", "&ePrevious Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.economy-management.bedrock-next-page", "&eNext Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.economy-management.bedrock-back", "&cBack"));

                final int navOffset = pageGuilds.size();

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int id = response.clickedButtonId();
                    if (id < navOffset) {
                        sendBedrockEconomyActions(player, pageGuilds.get(id), safePage);
                    } else if (id == navOffset) {
                        sendBedrockEconomyList(player, safePage);
                    } else if (id == navOffset + 1) {
                        sendBedrockEconomyList(player, safePage > 0 ? safePage - 1 : safePage);
                    } else if (id == navOffset + 2) {
                        sendBedrockEconomyList(player, safePage < totalPages - 1 ? safePage + 1 : safePage);
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

    private void sendBedrockEconomyActions(Player player, Guild guild, int page) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player,
                        "gui.economy-management.bedrock-actions-title",
                        "&eEconomy Management - {guild}", "{guild}", guild.getName()))
                .content(languageManager.getGuiColoredMessage(player,
                        "gui.economy-management.bedrock-actions-content",
                        "&fCurrent Balance: &a{balance}\n&fLeader: &e{leader}\n&fLevel: &e{level}",
                        "{balance}", plugin.getEconomyManager().format(guild.getBalance()),
                        "{leader}", guild.getLeaderName(),
                        "{level}", String.valueOf(guild.getLevel())));

        builder.button(languageManager.getGuiColoredMessage(player,
                "gui.economy-management.bedrock-set-funds", "&eSet Funds"));
        builder.button(languageManager.getGuiColoredMessage(player,
                "gui.economy-management.bedrock-add-funds", "&aAdd Funds"));
        builder.button(languageManager.getGuiColoredMessage(player,
                "gui.economy-management.bedrock-clear-funds", "&cClear Funds"));
        builder.button(languageManager.getGuiColoredMessage(player,
                "gui.economy-management.bedrock-back-to-list", "&cBack to List"));

        builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
            switch (response.clickedButtonId()) {
                case 0 -> sendBedrockAmountInput(player, guild, "set", page);
                case 1 -> sendBedrockAmountInput(player, guild, "add", page);
                case 2 -> plugin.getGuiManager().openGUI(player,
                        new ConfirmChangeFundsGUI(plugin, guild, player, "remove", guild.getBalance()));
                case 3 -> sendBedrockEconomyList(player, page);
            }
        }));

        builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                () -> sendBedrockEconomyList(player, page)));

        BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
    }

    private void sendBedrockAmountInput(Player player, Guild guild, String operationType, int page) {
        String title = operationType.equals("set")
                ? languageManager.getGuiColoredMessage(player,
                "gui.economy-management.bedrock-set-title", "&eSet Funds")
                : languageManager.getGuiColoredMessage(player,
                "gui.economy-management.bedrock-add-title", "&aAdd Funds");
        CustomForm form = CustomForm.builder()
                .title(title + " - " + guild.getName())
                .input(languageManager.getGuiColoredMessage(player,
                        "gui.economy-management.bedrock-input-label", "&fEnter amount"), "0", "")
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    String input = response.getInput(0);
                    try {
                        double amount = Double.parseDouble(input.trim());
                        if (amount <= 0) {
                            player.sendMessage(languageManager.getGuiColoredMessage(player,
                                    "gui.economy-management.bedrock-invalid-amount",
                                    "&cAmount must be greater than 0!"));
                            sendBedrockEconomyActions(player, guild, page);
                            return;
                        }
                        plugin.getGuiManager().openGUI(player,
                                new ConfirmChangeFundsGUI(plugin, guild, player, operationType, amount));
                    } catch (NumberFormatException e) {
                        player.sendMessage(languageManager.getGuiColoredMessage(player,
                                "gui.economy-management.bedrock-invalid-number", "&cInvalid number!"));
                        sendBedrockEconomyActions(player, guild, page);
                    }
                }))
                .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                        () -> sendBedrockEconomyActions(player, guild, page)))
                .build();

        BedrockFormSender.sendForm(player.getUniqueId(), form);
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
