package com.guild.gui.base;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.List;

/**
 * 分页在线玩家选择 GUI 基类（邀请成员等）。
 * 子类保留独立类名，以便 imago-gui.yml / gui-image-layout.yml 按屏配置。
 */
public abstract class AbstractPagedPlayerGUI extends AbstractPagedListGUI<Player> {

    protected final Guild guild;

    protected AbstractPagedPlayerGUI(GuildPlugin plugin, Guild guild, Player viewer) {
        super(plugin, viewer);
        this.guild = guild;
        setEntries(loadEntries());
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

    @Override
    protected abstract void onEntrySelected(Player viewer, Player target);

    protected String[] bedrockContentPlaceholders() {
        return new String[0];
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(viewer, titleKey(), titleDefault(),
                "{page}", String.valueOf(currentPage + 1), "{guild}", guild.getName()));
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
        setEntries(currentEntries);

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
}
