package com.guild.gui.base;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.gui.GUI;
import com.guild.core.gui.layout.GuiImageLayoutConfig;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.List;

/**
 * 分页在线玩家选择 GUI 基类（邀请成员等）。
 * 子类保留独立类名，以便 imago-gui.yml / gui-image-layout.yml 按屏配置。
 */
public abstract class AbstractPagedPlayerGUI implements GUI {

    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_BACK = "BACK";

    protected final GuildPlugin plugin;
    protected final LanguageManager languageManager;
    protected final Guild guild;
    protected final Player viewer;
    protected int currentPage = 0;
    protected List<Player> entries;

    protected AbstractPagedPlayerGUI(GuildPlugin plugin, Guild guild, Player viewer) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.viewer = viewer;
        this.entries = loadEntries();
    }

    protected abstract List<Player> loadEntries();

    protected abstract String titleKey();

    protected abstract String titleDefault();

    protected abstract String bedrockTitleKey();

    protected abstract String bedrockTitleDefault();

    protected abstract String bedrockTitlePageKey();

    protected abstract String bedrockTitlePageDefault();

    protected abstract String bedrockContentKey();

    protected abstract String bedrockContentDefault();

    protected abstract String bedrockEmptyKey();

    protected abstract String bedrockEmptyDefault();

    protected abstract String bedrockMemberButtonPrefix();

    protected abstract ItemStack createEntryItem(Player target);

    protected abstract void onEntrySelected(Player viewer, Player target);

    protected abstract void openBackGui(Player viewer);

    protected String backLoreKey() {
        return "gui.common.member-operation.back-to-settings";
    }

    protected String backLoreDefault() {
        return "Return to guild settings";
    }

    protected String[] bedrockContentPlaceholders() {
        return new String[0];
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(viewer, titleKey(), titleDefault(),
                "{page}", String.valueOf(currentPage + 1), "{guild}", guild.getName()));
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        if (!plugin.getGuiManager().isImageLayoutActive(viewer, getGuiType())) {
            GuiLayoutUtils.fillBorder54(inventory);
        }
        displayEntries(inventory);
        setupNavigationButtons(inventory);
        plugin.getGuiManager().applyImageModeIfNeeded(viewer, inventory, getGuiType());
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (plugin.getGuiManager().isImageLayoutActive(player, getGuiType())) {
            GuiImageLayoutConfig layoutConfig = plugin.getGuiManager().getImageLayoutConfig();
            String func = layoutConfig.getFunctionAtSlot(getGuiType(), slot);
            if (func != null) {
                dispatchNavFunction(player, func);
                return;
            }
        }

        int entryIndex = GuiLayoutUtils.listIndexFromSlot(slot, currentPage, GuiLayoutUtils.ITEMS_PER_PAGE);
        if (entryIndex >= 0 && entryIndex < entries.size()) {
            onEntrySelected(player, entries.get(entryIndex));
            return;
        }

        if (slot == slotForFunction(FUNC_PREV_PAGE, 45)) {
            goPrevPage(player);
        } else if (slot == slotForFunction(FUNC_NEXT_PAGE, 53)) {
            goNextPage(player);
        } else if (slot == slotForFunction(FUNC_BACK, 49)) {
            openBackGui(player);
        }
    }

    protected void dispatchNavFunction(Player player, String func) {
        if (FUNC_PREV_PAGE.equals(func)) {
            goPrevPage(player);
        } else if (FUNC_NEXT_PAGE.equals(func)) {
            goNextPage(player);
        } else if (FUNC_BACK.equals(func)) {
            openBackGui(player);
        }
    }

    protected void goPrevPage(Player player) {
        if (currentPage > 0) {
            currentPage--;
            plugin.getGuiManager().refreshGUI(player);
        }
    }

    protected void goNextPage(Player player) {
        if (currentPage < maxPageIndex()) {
            currentPage++;
            plugin.getGuiManager().refreshGUI(player);
        }
    }

    protected int maxPageIndex() {
        return GuiLayoutUtils.maxPageIndex(entries.size(), GuiLayoutUtils.ITEMS_PER_PAGE);
    }

    protected void displayEntries(Inventory inventory) {
        int startIndex = currentPage * GuiLayoutUtils.ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + GuiLayoutUtils.ITEMS_PER_PAGE, entries.size());
        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(GuiLayoutUtils.slotForPageIndex(i - startIndex), createEntryItem(entries.get(i)));
        }
    }

    protected void setupNavigationButtons(Inventory inventory) {
        if (currentPage > 0) {
            inventory.setItem(slotForFunction(FUNC_PREV_PAGE, 45), GuiLayoutUtils.createItem(
                    Material.ARROW,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.common.previous-page", "&e&lPrevious Page")),
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.common.view-previous", "View previous page"))));
        }

        if (currentPage < maxPageIndex()) {
            inventory.setItem(slotForFunction(FUNC_NEXT_PAGE, 53), GuiLayoutUtils.createItem(
                    Material.ARROW,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.common.next-page", "&e&lNext Page")),
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.common.view-next", "View next page"))));
        }

        inventory.setItem(slotForFunction(FUNC_BACK, 49), GuiLayoutUtils.createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.common.back", "Back")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, backLoreKey(), backLoreDefault()))));
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }
        sendBedrockList(player, 0);
        return true;
    }

    protected void sendBedrockList(Player player, int page) {
        List<Player> currentEntries = loadEntries();
        this.entries = currentEntries;

        if (currentEntries.isEmpty()) {
            SimpleForm form = SimpleForm.builder()
                    .title(languageManager.getGuiColoredMessage(player, bedrockTitleKey(), bedrockTitleDefault()))
                    .content(languageManager.getGuiColoredMessage(player, bedrockEmptyKey(), bedrockEmptyDefault()))
                    .button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-back", "&cBack"))
                    .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> openBackGui(player)))
                    .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> openBackGui(player)))
                    .build();
            BedrockFormSender.sendForm(player.getUniqueId(), form);
            return;
        }

        int totalPages = GuiLayoutUtils.maxPageIndex(currentEntries.size(), GuiLayoutUtils.BEDROCK_ITEMS_PER_PAGE);
        final int safePage = Math.max(0, Math.min(page, totalPages));
        final int startIndex = safePage * GuiLayoutUtils.BEDROCK_ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + GuiLayoutUtils.BEDROCK_ITEMS_PER_PAGE, currentEntries.size());
        final int entryCount = endIndex - startIndex;

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player,
                        bedrockTitlePageKey(), bedrockTitlePageDefault(),
                        "{page}", String.valueOf(safePage + 1)))
                .content(languageManager.getGuiColoredMessage(player,
                        bedrockContentKey(), bedrockContentDefault(), bedrockContentPlaceholders()));

        for (int i = startIndex; i < endIndex; i++) {
            builder.button(bedrockMemberButtonPrefix() + currentEntries.get(i).getName());
        }

        builder.button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-prev-page", "&ePrevious Page"));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-next-page", "&eNext Page"));
        builder.button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-back", "&cBack"));

        builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
            int clicked = response.clickedButtonId();
            if (clicked < entryCount) {
                onEntrySelected(player, currentEntries.get(startIndex + clicked));
            } else if (clicked == entryCount) {
                sendBedrockList(player, safePage - 1);
            } else if (clicked == entryCount + 1) {
                sendBedrockList(player, safePage + 1);
            } else {
                openBackGui(player);
            }
        }));

        builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> openBackGui(player)));

        BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
    }

    protected int slotForFunction(String function, int defaultSlot) {
        GuiImageLayoutConfig layoutConfig = plugin.getGuiManager().getImageLayoutConfig();
        if (layoutConfig == null) {
            return defaultSlot;
        }
        List<Integer> slots = layoutConfig.getSlots(getGuiType(), function);
        return slots.isEmpty() ? defaultSlot : slots.get(0);
    }
}
