package com.guild.gui.base;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.gui.GUI;
import com.guild.core.gui.layout.GuiImageLayoutConfig;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分页成员选择 GUI 基类（踢出/升降级/转让会长等共用骨架）。
 * 子类保留独立类名，以便 imago-gui.yml / gui-image-layout.yml 按屏配置。
 */
public abstract class AbstractPagedMemberGUI implements GUI {

    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_BACK = "BACK";

    protected static final int MEMBERS_PER_PAGE = 28;
    protected static final int BEDROCK_ITEMS_PER_PAGE = 10;

    protected final GuildPlugin plugin;
    protected final LanguageManager languageManager;
    protected final Guild guild;
    protected final Player viewer;
    protected int currentPage = 0;
    protected List<GuildMember> members = List.of();

    protected AbstractPagedMemberGUI(GuildPlugin plugin, Guild guild, Player viewer) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.viewer = viewer;
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

    protected abstract void openBackGui(Player player);

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

    /** 点击/打开前鉴权，默认通过 */
    protected boolean validateAccess(Player player) {
        return true;
    }

    protected void onUnauthorizedAccess(Player player) {
        // 子类按需覆盖
    }

    protected void loadMembers() {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(memberList -> {
            this.members = filterMembers(memberList);
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
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        if (!plugin.getGuiManager().isImageLayoutActive(viewer, getGuiType())) {
            fillBorder(inventory);
        }
        displayMembers(inventory);
        setupNavigationButtons(inventory);
        plugin.getGuiManager().applyImageModeIfNeeded(viewer, inventory, getGuiType());
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (!validateAccess(player)) {
            onUnauthorizedAccess(player);
            return;
        }

        if (plugin.getGuiManager().isImageLayoutActive(player, getGuiType())) {
            GuiImageLayoutConfig layoutConfig = plugin.getGuiManager().getImageLayoutConfig();
            String func = layoutConfig.getFunctionAtSlot(getGuiType(), slot);
            if (func != null) {
                dispatchNavFunction(player, func);
                return;
            }
        }

        int memberIndex = getMemberIndexFromSlot(slot);
        if (memberIndex >= 0 && memberIndex < members.size()) {
            onMemberSelected(player, members.get(memberIndex));
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
        int maxPage = maxPageIndex();
        if (currentPage < maxPage) {
            currentPage++;
            plugin.getGuiManager().refreshGUI(player);
        }
    }

    protected int maxPageIndex() {
        if (members.isEmpty()) {
            return 0;
        }
        return (members.size() - 1) / MEMBERS_PER_PAGE;
    }

    protected int getSlotForPageIndex(int pageIndex) {
        int row = pageIndex / 7;
        int col = pageIndex % 7;
        return (row + 1) * 9 + col + 1;
    }

    protected int getMemberIndexFromSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > 4 || col < 1 || col > 7) {
            return -1;
        }
        return currentPage * MEMBERS_PER_PAGE + (row - 1) * 7 + (col - 1);
    }

    protected void fillBorder(Inventory inventory) {
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

    protected void displayMembers(Inventory inventory) {
        int startIndex = currentPage * MEMBERS_PER_PAGE;
        int endIndex = Math.min(startIndex + MEMBERS_PER_PAGE, members.size());
        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(getSlotForPageIndex(i - startIndex), createMemberHead(members.get(i)));
        }
    }

    protected void setupNavigationButtons(Inventory inventory) {
        if (currentPage > 0) {
            inventory.setItem(slotForFunction(FUNC_PREV_PAGE, 45), createItem(
                    Material.ARROW,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.common.previous-page", "&e&lPrevious Page")),
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.common.view-previous", "View previous page"))));
        }

        if (currentPage < maxPageIndex()) {
            inventory.setItem(slotForFunction(FUNC_NEXT_PAGE, 53), createItem(
                    Material.ARROW,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.common.next-page", "&e&lNext Page")),
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.common.view-next", "View next page"))));
        }

        inventory.setItem(slotForFunction(FUNC_BACK, 49), createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.common.back", "Back")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.common.member-operation.back-to-settings", "Return to guild settings"))));
    }

    protected ItemStack createMemberHead(GuildMember member) {
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

                int totalPages = (filtered.size() - 1) / BEDROCK_ITEMS_PER_PAGE;
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

    protected int slotForFunction(String function, int defaultSlot) {
        GuiImageLayoutConfig layoutConfig = plugin.getGuiManager().getImageLayoutConfig();
        if (layoutConfig == null) {
            return defaultSlot;
        }
        List<Integer> slots = layoutConfig.getSlots(getGuiType(), function);
        return slots.isEmpty() ? defaultSlot : slots.get(0);
    }

    protected ItemStack createItem(Material material, String name, String... lore) {
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
