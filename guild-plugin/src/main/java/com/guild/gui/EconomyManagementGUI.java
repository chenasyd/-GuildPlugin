package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.geyser.PlayerConnectionService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.ArrayList;
import java.util.List;

/**
 * 经济管理GUI
 */
public class EconomyManagementGUI implements GUI {

    // ── 图像模式功能常量 ──
    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_PAGE_INFO = "PAGE_INFO";
    public static final String FUNC_REFRESH = "REFRESH";
    public static final String FUNC_BACK = "BACK";

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;
    private int currentPage = 0;
    private final int itemsPerPage = 28; // 7列 × 4行
    private static final int PREVIOUS_PAGE_SLOT = 48;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int BACK_SLOT = 46;
    private static final int REFRESH_SLOT = 52;
    private List<Guild> allGuilds = new ArrayList<>();

    public EconomyManagementGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        loadGuilds();
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.economy-management.economy-management-title", "&eEconomy Management"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 设置工会列表
        setupGuildList(inventory);
        
        // 设置分页按钮
        setupPaginationButtons(inventory);
        
        // 设置操作按钮
        setupActionButtons(inventory);

        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }
    
    private void setupGuildList(Inventory inventory) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allGuilds.size());
        
        for (int i = 0; i < itemsPerPage; i++) {
            if (startIndex + i < endIndex) {
                Guild guild = allGuilds.get(startIndex + i);
                
                // 计算在2-8列，2-5行的位置 (slots 10-43)
                int row = (i / 7) + 1; // 2-5行
                int col = (i % 7) + 1; // 2-8列
                int slot = row * 9 + col;
                
                inventory.setItem(slot, createGuildItem(guild));
            }
        }
    }
    
    private ItemStack createGuildItem(Guild guild) {
        Material material = Material.GOLD_INGOT;
        
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.guild-name", "Guild Name") + ": " + guild.getName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.leader", "Leader") + ": " + guild.getLeaderName()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.economy-management.level", "Level") + ": " + guild.getLevel()));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.economy-management.current-balance", "Current Balance") + ": " + plugin.getEconomyManager().format(guild.getBalance())));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.economy-management.max-members", "Max Members") + ": " + guild.getMaxMembers()));
        lore.add("");
        lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(player, "gui.economy-management.left-click-set", "Left click: Set balance")));
        lore.add(ColorUtils.colorize("&a" + languageManager.getGuiMessage(player, "gui.economy-management.right-click-add", "Right click: Add balance")));
        lore.add(ColorUtils.colorize("&c" + languageManager.getGuiMessage(player, "gui.economy-management.middle-click-remove", "Middle click: Remove balance")));
        
        return createItem(material, ColorUtils.colorize("&6" + guild.getName()), lore.toArray(new String[0]));
    }
    
    private void setupPaginationButtons(Inventory inventory) {
        int totalPages = Math.max(1, (int) Math.ceil((double) allGuilds.size() / itemsPerPage));
        if (currentPage > totalPages - 1) {
            currentPage = totalPages - 1;
        }
        
        // 上一页按钮
        if (currentPage > 0) {
            inventory.setItem(PREVIOUS_PAGE_SLOT, createItem(Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.economy-management.previous-page", "&aPrevious Page")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.economy-management.previous-page.desc", "&7Page {page}", "{page}", String.valueOf(currentPage)))));
        }

        // 页码信息
        inventory.setItem(PAGE_INFO_SLOT, createItem(Material.PAPER,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.economy-management.page-info", "&ePage {current} of {total}", "{current}", String.valueOf(currentPage + 1), "{total}", String.valueOf(totalPages)))));

        // 下一页按钮
        if (currentPage < totalPages - 1) {
            inventory.setItem(NEXT_PAGE_SLOT, createItem(Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.economy-management.next-page", "&aNext Page")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.economy-management.next-page.desc", "&7Page {page}", "{page}", String.valueOf(currentPage + 2)))));
        }
    }

    private void setupActionButtons(Inventory inventory) {
        // 返回按钮
        inventory.setItem(46, createItem(Material.ARROW,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.economy-management.back", "&cBack"))));

        // 刷新按钮
        inventory.setItem(52, createItem(Material.EMERALD,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.economy-management.refresh", "&aRefresh List"))));
    }
    
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        
        // 填充边框
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    private void loadGuilds() {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            this.allGuilds = guilds;
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (player.isOnline()) {
                    refresh(player);
                }
            });
        });
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 46) {
            // 返回
            plugin.getGuiManager().openGUI(player, new AdminGuildGUI(plugin, player));
        } else if (slot == REFRESH_SLOT) {
            // 刷新
            loadGuilds();
        } else if (slot == PREVIOUS_PAGE_SLOT && currentPage > 0) {
            // 上一页
            currentPage--;
            refresh(player);
        } else if (slot == NEXT_PAGE_SLOT && currentPage < (int) Math.ceil((double) allGuilds.size() / itemsPerPage) - 1) {
            // 下一页
            currentPage++;
            refresh(player);
        } else if (slot >= 10 && slot <= 43) {
            // 工会项目 - 检查是否在2-8列，2-5行范围内
            int row = slot / 9;
            int col = slot % 9;
            if (row >= 1 && row <= 4 && col >= 1 && col <= 7) {
                int relativeIndex = (row - 1) * 7 + (col - 1);
                int guildIndex = (currentPage * itemsPerPage) + relativeIndex;
                if (guildIndex < allGuilds.size()) {
                    Guild guild = allGuilds.get(guildIndex);
                    handleGuildClick(player, guild, clickType);
                }
            }
        }
    }
    
    private void handleGuildClick(Player player, Guild guild, ClickType clickType) {
        if (clickType == ClickType.MIDDLE) {
            // 中键：直接打开确认GUI（不需要输入金额）
            ConfirmChangeFundsGUI confirmGUI = new ConfirmChangeFundsGUI(
                    plugin, guild, player, "remove",
                    guild.getBalance());
            String msg = languageManager.getGuiMessage(player,
                    "gui.economy-management.middle-click-desc",
                    "&cAbout to clear funds of &e{guild}&c, please confirm", "{guild}", guild.getName());
            player.sendMessage(ColorUtils.colorize(msg));
            plugin.getGuiManager().openGUI(player, confirmGUI);
            return;
        }

        // 左键：设置资金 / 右键：增加资金 — 先关闭GUI并进入输入模式
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

        // 关闭当前GUI
        plugin.getGuiManager().closeGUI(player);

        // 发送提示
        String prompt = languageManager.getGuiMessage(player, promptKey,
                "&eEnter amount in chat:");
        player.sendMessage(ColorUtils.colorize(prompt));

        // 设置输入模式：捕获玩家输入的金额
        plugin.getGuiManager().setInputMode(player, input -> {
            try {
                double amount = Double.parseDouble(input.trim());
                if (amount <= 0) {
                    player.sendMessage(ColorUtils.colorize(
                            languageManager.getGuiMessage(player,
                                    "gui.economy-management.invalid-amount",
                                    "&cAmount must be greater than 0!")));
                    return false; // 继续等待有效输入
                }
                // 打开确认GUI
                ConfirmChangeFundsGUI confirmGUI = new ConfirmChangeFundsGUI(
                        plugin, guild, player, operationType, amount);
                plugin.getGuiManager().openGUI(player, confirmGUI);
                return true;
            } catch (NumberFormatException e) {
                if (input.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ColorUtils.colorize(
                            languageManager.getGuiMessage(player,
                                    "gui.economy-management.input-cancelled",
                                    "&7Operation cancelled")));
                    return true; // 退出输入模式
                }
                player.sendMessage(ColorUtils.colorize(
                        languageManager.getGuiMessage(player,
                                "gui.economy-management.invalid-number",
                                "&cInvalid number! Enter a valid amount or type cancel to abort")));
                return false;
            }
        });
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;
        sendBedrockEconomyList(player, 0);
        return true;
    }

    private void sendBedrockEconomyList(Player player, int page) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (!player.isOnline()) return;

                int itemsPerPage = 10;
                int totalPages = Math.max(1, (int) Math.ceil((double) guilds.size() / itemsPerPage));
                final int safePage = Math.max(0, Math.min(page, totalPages - 1));
                int startIndex = safePage * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, guilds.size());

                SimpleForm.Builder builder = SimpleForm.builder()
                    .title(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-title", "&eEconomy Management"))
                    .content(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-page-info", "&fPage {current}/{total} | Total {count} guilds", "{current}", String.valueOf(safePage + 1), "{total}", String.valueOf(totalPages), "{count}", String.valueOf(guilds.size())));

                List<Guild> pageGuilds = new ArrayList<>();
                for (int i = startIndex; i < endIndex; i++) {
                    Guild g = guilds.get(i);
                    pageGuilds.add(g);
                    builder.button("§6" + g.getName() + " §f- " + plugin.getEconomyManager().format(g.getBalance()));
                }

                builder.button(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-refresh", "&aRefresh List"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-prev-page", "&ePrevious Page"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-next-page", "&eNext Page"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-back", "&cBack"));

                final int navOffset = pageGuilds.size();

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int id = response.clickedButtonId();
                    if (id < navOffset) {
                        sendBedrockEconomyActions(player, pageGuilds.get(id), safePage);
                    } else if (id == navOffset) {
                        sendBedrockEconomyList(player, safePage);
                    } else if (id == navOffset + 1) {
                        if (safePage > 0) sendBedrockEconomyList(player, safePage - 1);
                        else sendBedrockEconomyList(player, safePage);
                    } else if (id == navOffset + 2) {
                        if (safePage < totalPages - 1) sendBedrockEconomyList(player, safePage + 1);
                        else sendBedrockEconomyList(player, safePage);
                    } else {
                        plugin.getGuiManager().openGUI(player, new AdminGuildGUI(plugin, player));
                    }
                }));

                builder.closedResultHandler(response -> {});

                BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
            });
        });
    }

    private void sendBedrockEconomyActions(Player player, Guild guild, int page) {
        SimpleForm.Builder builder = SimpleForm.builder()
            .title(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-actions-title", "&eEconomy Management - {guild}", "{guild}", guild.getName()))
            .content(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-actions-content", "&fCurrent Balance: &a{balance}\n&fLeader: &e{leader}\n&fLevel: &e{level}", "{balance}", plugin.getEconomyManager().format(guild.getBalance()), "{leader}", guild.getLeaderName(), "{level}", String.valueOf(guild.getLevel())));

        builder.button(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-set-funds", "&eSet Funds"));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-add-funds", "&aAdd Funds"));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-clear-funds", "&cClear Funds"));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-back-to-list", "&cBack to List"));

        builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
            switch (response.clickedButtonId()) {
                case 0:
                    sendBedrockAmountInput(player, guild, "set", page);
                    break;
                case 1:
                    sendBedrockAmountInput(player, guild, "add", page);
                    break;
                case 2:
                    plugin.getGuiManager().openGUI(player,
                        new ConfirmChangeFundsGUI(plugin, guild, player, "remove", guild.getBalance()));
                    break;
                case 3:
                    sendBedrockEconomyList(player, page);
                    break;
            }
        }));

        builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
            sendBedrockEconomyList(player, page)));

        BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
    }

    private void sendBedrockAmountInput(Player player, Guild guild, String operationType, int page) {
        String title = operationType.equals("set")
            ? languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-set-title", "&eSet Funds")
            : languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-add-title", "&aAdd Funds");
        CustomForm form = CustomForm.builder()
            .title(title + " - " + guild.getName())
            .input(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-input-label", "&fEnter amount"), "0", "")
            .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                String input = response.getInput(0);
                try {
                    double amount = Double.parseDouble(input.trim());
                    if (amount <= 0) {
                        player.sendMessage(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-invalid-amount", "&cAmount must be greater than 0!"));
                        sendBedrockEconomyActions(player, guild, page);
                        return;
                    }
                    plugin.getGuiManager().openGUI(player,
                        new ConfirmChangeFundsGUI(plugin, guild, player, operationType, amount));
                } catch (NumberFormatException e) {
                    player.sendMessage(languageManager.getGuiColoredMessage(player, "gui.economy-management.bedrock-invalid-number", "&cInvalid number!"));
                    sendBedrockEconomyActions(player, guild, page);
                }
            }))
            .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                sendBedrockEconomyActions(player, guild, page)))
            .build();

        BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    @Override
    public void onClose(Player player) {
        // 关闭时的处理
    }
    
    @Override
    public void refresh(Player player) {
        if (player.isOnline()) {
            // 基岩版玩家由 openBedrockForm 的异步方法自行刷新，
            // 跳过 GUIManager.refreshGUI 避免与 Cumulus 表单冲突
            if (PlayerConnectionService.isBedrockPlayer(player)) return;
            plugin.getGuiManager().refreshGUI(player);
        }
    }
}
