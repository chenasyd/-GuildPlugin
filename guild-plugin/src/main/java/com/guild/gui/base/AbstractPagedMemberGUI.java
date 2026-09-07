package com.guild.gui.base;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分页成员选择 GUI 基类（踢出/升降级/转让会长等共用骨架）。
 * 子类保留独立类名，以便 imago-gui.yml / gui-image-layout.yml 按屏配置。
 */
public abstract class AbstractPagedMemberGUI extends AbstractPagedListGUI<GuildMember> {

    protected static final int BEDROCK_ITEMS_PER_PAGE = GuiLayoutUtils.BEDROCK_ITEMS_PER_PAGE;

    protected final Guild guild;

    protected AbstractPagedMemberGUI(GuildPlugin plugin, Guild guild, Player viewer) {
        super(plugin, viewer);
        this.guild = guild;
        loadMembers();
    }

    protected abstract String titleKey();

    protected abstract String titleDefault();

    protected abstract String bedrockTitleKey();

    protected abstract String bedrockTitleDefault();

    protected abstract String bedrockTitlePageKey();

    protected abstract String bedrockTitlePageDefault();

    protected abstract String bedrockContentKey();

    protected abstract String bedrockContentDefault();

    protected abstract String bedrockNoMembersKey();

    protected abstract String bedrockNoMembersDefault();

    /** 成员头颅 displayName 颜色前缀，如 {@code "&c"} */
    protected abstract String memberDisplayNamePrefix();

    /** Bedrock 列表按钮颜色前缀，如 {@code "§c"} */
    protected abstract String bedrockMemberButtonPrefix();

    protected abstract String memberPositionLoreKey();

    protected abstract String memberPositionLoreDefault();

    protected abstract String memberClickLoreColorPrefix();

    protected abstract String memberClickLoreKey();

    protected abstract String memberClickLoreDefault();

    protected abstract void onMemberSelected(Player player, GuildMember member);

    /** 从全量成员中筛选本 GUI 可操作的成员 */
    protected List<GuildMember> filterMembers(List<GuildMember> allMembers) {
        return allMembers.stream()
                .filter(m -> !m.getPlayerUuid().equals(guild.getLeaderUuid()))
                .collect(Collectors.toList());
    }

    /** 成员列表加载完成后是否刷新 GUI（Kick 为 false，其余为 true） */
    protected boolean refreshGuiAfterLoad() {
        return true;
    }

    protected void loadMembers() {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(memberList -> {
            setEntries(filterMembers(memberList));
            if (refreshGuiAfterLoad()) {
                CompatibleScheduler.runTask(plugin, viewer, () -> {
                    if (viewer.isOnline()) {
                        plugin.getGuiManager().refreshGUI(viewer);
                    }
                });
            }
        });
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(viewer, titleKey(), titleDefault(),
                "{page}", String.valueOf(currentPage + 1)));
    }

    @Override
    protected ItemStack createEntryItem(GuildMember member) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(member.getOfflinePlayer());
            meta.setDisplayName(ColorUtils.colorize(memberDisplayNamePrefix() + member.getPlayerName()));
            meta.setLore(Arrays.asList(
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                            memberPositionLoreKey(), memberPositionLoreDefault())
                            + ": &e" + member.getRole().getDisplayName()),
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                            "gui.common.member-operation.join-time", "Join time")
                            + ": &e" + member.getJoinedAt()),
                    ColorUtils.colorize(memberClickLoreColorPrefix()
                            + languageManager.getGuiMessage(viewer, memberClickLoreKey(), memberClickLoreDefault()))
            ));
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    protected void onEntrySelected(Player player, GuildMember member) {
        onMemberSelected(player, member);
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }
        if (!validateAccess(player)) {
            onUnauthorizedAccess(player);
            return true;
        }
        sendBedrockList(player, 0);
        return true;
    }

    protected void sendBedrockList(Player player, int page) {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(memberList -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                List<GuildMember> filtered = filterMembers(memberList);

                if (filtered.isEmpty()) {
                    SimpleForm form = SimpleForm.builder()
                            .title(languageManager.getGuiColoredMessage(player,
                                    bedrockTitleKey(), bedrockTitleDefault()))
                            .content(languageManager.getGuiColoredMessage(player,
                                    bedrockNoMembersKey(), bedrockNoMembersDefault()))
                            .button(languageManager.getGuiColoredMessage(player,
                                    "gui.common.bedrock-back", "&cBack"))
                            .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                                    () -> openBackGui(player)))
                            .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                                    () -> openBackGui(player)))
                            .build();
                    BedrockFormSender.sendForm(player.getUniqueId(), form);
                    return;
                }

                int totalPages = GuiLayoutUtils.maxPageIndex(filtered.size(), BEDROCK_ITEMS_PER_PAGE);
                final int safePage = Math.max(0, Math.min(page, totalPages));
                final int startIndex = safePage * BEDROCK_ITEMS_PER_PAGE;
                int endIndex = Math.min(startIndex + BEDROCK_ITEMS_PER_PAGE, filtered.size());
                final int memberCount = endIndex - startIndex;

                SimpleForm.Builder builder = SimpleForm.builder()
                        .title(languageManager.getGuiColoredMessage(player,
                                bedrockTitlePageKey(), bedrockTitlePageDefault(),
                                "{page}", String.valueOf(safePage + 1)))
                        .content(languageManager.getGuiColoredMessage(player,
                                bedrockContentKey(), bedrockContentDefault()));

                for (int i = startIndex; i < endIndex; i++) {
                    builder.button(bedrockMemberButtonPrefix() + filtered.get(i).getPlayerName());
                }

                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.common.bedrock-prev-page", "&ePrevious Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.common.bedrock-next-page", "&eNext Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.common.bedrock-back", "&cBack"));

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int clicked = response.clickedButtonId();
                    if (clicked < memberCount) {
                        onMemberSelected(player, filtered.get(startIndex + clicked));
                    } else if (clicked == memberCount) {
                        sendBedrockList(player, safePage - 1);
                    } else if (clicked == memberCount + 1) {
                        sendBedrockList(player, safePage + 1);
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
}
