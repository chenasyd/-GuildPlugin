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

import java.util.Arrays;
import java.util.List;

/**
 * 确认对话框基类（删除/离开/踢出/转让/升降级/资金变更等共用骨架）。
 * 子类保留独立类名，以便 imago-gui.yml / gui-image-layout.yml 按屏配置。
 */
public abstract class AbstractConfirmGUI implements GUI {

    public static final String FUNC_CONFIRM = "CONFIRM";
    public static final String FUNC_INFO = "INFO";
    public static final String FUNC_DETAILS = "DETAILS";
    public static final String FUNC_CANCEL = "CANCEL";

    protected final GuildPlugin plugin;
    protected final LanguageManager languageManager;
    protected final Guild guild;
    protected final Player viewer;
    protected final String sourceGuiType;

    protected AbstractConfirmGUI(GuildPlugin plugin, Guild guild, Player viewer, String sourceGuiType) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.viewer = viewer;
        this.sourceGuiType = sourceGuiType;
    }

    protected abstract String titleKey();
    protected abstract String titleDefault();

    protected abstract String bedrockTitleKey();
    protected abstract String bedrockTitleDefault();
    protected abstract String bedrockContentKey();
    protected abstract String bedrockContentDefault();
    protected abstract String bedrockConfirmKey();
    protected abstract String bedrockConfirmDefault();
    protected abstract String bedrockCancelKey();
    protected abstract String bedrockCancelDefault();

    protected abstract Material confirmMaterial();
    protected abstract String confirmButtonKey();
    protected abstract String confirmButtonDefault();
    protected abstract String confirmLoreKey();
    protected abstract String confirmLoreDefault();

    protected String[] confirmLoreMessages() {
        return new String[]{languageManager.getGuiMessage(viewer, confirmLoreKey(), confirmLoreDefault())};
    }

    protected abstract String cancelButtonKey();
    protected abstract String cancelButtonDefault();
    protected abstract String cancelLoreKey();
    protected abstract String cancelLoreDefault();

    /** 中间信息区在 gui-image-layout.yml 中的功能名（默认 INFO，资金确认为 DETAILS） */
    protected String infoFunctionName() {
        return FUNC_INFO;
    }

    protected abstract ItemStack createInfoItem();

    protected abstract String[] bedrockContentPlaceholders();

    protected abstract void onConfirm(Player player);

    protected abstract void onCancel(Player player);

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(viewer, titleKey(), titleDefault()));
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }

        String[] placeholders = bedrockContentPlaceholders();
        String content = languageManager.getGuiColoredMessage(
                viewer, bedrockContentKey(), bedrockContentDefault(), placeholders);

        SimpleForm form = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(viewer, bedrockTitleKey(), bedrockTitleDefault()))
                .content(content)
                .button(languageManager.getGuiColoredMessage(viewer, bedrockConfirmKey(), bedrockConfirmDefault()))
                .button(languageManager.getGuiColoredMessage(viewer, bedrockCancelKey(), bedrockCancelDefault()))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (response.clickedButtonId() == 0) {
                        onConfirm(player);
                    } else {
                        onCancel(player);
                    }
                }))
                .closedResultHandler(() -> CompatibleScheduler.runTask(plugin, player, () -> onCancel(player)))
                .build();

        return BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        if (!plugin.getGuiManager().isImageLayoutActive(viewer, getGuiType())) {
            GuiLayoutUtils.fillBorder27(inventory);
        }

        inventory.setItem(slotForFunction(infoFunctionName(), 13), createInfoItem());
        setupConfirmCancelButtons(inventory);
        plugin.getGuiManager().applyImageModeIfNeeded(viewer, inventory, getGuiType());
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (plugin.getGuiManager().isImageLayoutActive(player, getGuiType())) {
            GuiImageLayoutConfig layoutConfig = plugin.getGuiManager().getImageLayoutConfig();
            String func = layoutConfig.getFunctionAtSlot(getGuiType(), slot);
            if (func == null) {
                return;
            }
            dispatchFunction(player, func);
            return;
        }

        if (slot == slotForFunction(FUNC_CONFIRM, 11)) {
            onConfirm(player);
        } else if (slot == slotForFunction(FUNC_CANCEL, 15)) {
            onCancel(player);
        }
    }

    protected void dispatchFunction(Player player, String func) {
        if (FUNC_CONFIRM.equals(func)) {
            onConfirm(player);
        } else if (FUNC_CANCEL.equals(func)) {
            onCancel(player);
        }
    }

    protected void setupConfirmCancelButtons(Inventory inventory) {
        String[] confirmLore = confirmLoreMessages();
        ItemStack confirm = GuiLayoutUtils.createItem(confirmMaterial(),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, confirmButtonKey(), confirmButtonDefault())),
                Arrays.stream(confirmLore).map(ColorUtils::colorize).toArray(String[]::new));
        inventory.setItem(slotForFunction(FUNC_CONFIRM, 11), confirm);

        ItemStack cancel = GuiLayoutUtils.createItem(cancelMaterial(),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, cancelButtonKey(), cancelButtonDefault())),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, cancelLoreKey(), cancelLoreDefault())));
        inventory.setItem(slotForFunction(FUNC_CANCEL, 15), cancel);
    }

    protected int slotForFunction(String function, int defaultSlot) {
        GuiImageLayoutConfig layoutConfig = plugin.getGuiManager().getImageLayoutConfig();
        if (layoutConfig == null) {
            return defaultSlot;
        }
        List<Integer> slots = layoutConfig.getSlots(getGuiType(), function);
        return slots.isEmpty() ? defaultSlot : slots.get(0);
    }

    protected Material cancelMaterial() {
        return Material.EMERALD_BLOCK;
    }

    protected ItemStack createItem(Material material, String name, String... lore) {
        return GuiLayoutUtils.createItem(material, name, lore);
    }
}
