package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;

import org.geysermc.cumulus.form.SimpleForm;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 转移会长 - 选择目标成员 GUI
 */
public class TransferLeaderGUI implements GUI {

    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_BACK = "BACK";

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final Guild guild;
    private final Player player;
    private int currentPage = 0;
    private List<GuildMember> members;

    public TransferLeaderGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.player = player;
        this.members = List.of();
        loadMembers();
    }

    private void loadMembers() {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(memberList -> {
            this.members = memberList.stream()
                    .filter(m -> !m.getPlayerUuid().equals(guild.getLeaderUuid()))
                    .collect(Collectors.toList());
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (player.isOnline()) {
                    plugin.getGuiManager().refreshGUI(player);
                }
            });
        });
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.transfer-leader.title",
                "&6Transfer Leadership - Page {page}", "{page}", String.valueOf(currentPage + 1)));
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        fillBorder(inventory);
        displayMembers(inventory);
        setupNavigationButtons(inventory);
        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 入口鉴权：仅会长
        if (!canTransfer(player)) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
            return;
        }

        int memberIndex = getIndexFromSlot(slot);
        if (memberIndex >= 0) {
            if (memberIndex < members.size()) {
                GuildMember member = members.get(memberIndex);
                plugin.getGuiManager().openGUI(player,
                        new ConfirmTransferLeaderGUI(plugin, guild, member, player, "TransferLeaderGUI", false));
            }
        } else if (slot == 45) {
            if (currentPage > 0) {
                currentPage--;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 53) {
            int maxPage = members.isEmpty() ? 0 : (members.size() - 1) / 28;
            if (currentPage < maxPage) {
                currentPage++;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 49) {
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
        }
    }

    private boolean canTransfer(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        return member != null
                && member.getGuildId() == guild.getId()
                && member.getRole() == GuildMember.Role.LEADER
                && player.getUniqueId().equals(guild.getLeaderUuid());
    }

    private int getSlotForIndex(int index) {
        int row = index / 7;
        int col = index % 7;
        return (row + 1) * 9 + col + 1;
    }

    private int getIndexFromSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > 4 || col < 1 || col > 7) {
            return -1;
        }
        return currentPage * 28 + (row - 1) * 7 + (col - 1);
    }

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

    private void displayMembers(Inventory inventory) {
        int startIndex = currentPage * 28;
        int endIndex = Math.min(startIndex + 28, members.size());
        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(getSlotForIndex(i - startIndex), createMemberHead(members.get(i)));
        }
    }

    private void setupNavigationButtons(Inventory inventory) {
        if (currentPage > 0) {
            inventory.setItem(45, createItem(Material.ARROW,
                    ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.previous-page", "&e&lPrevious Page")),
                    ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.view-previous", "View previous page"))));
        }

        int maxPage = members.isEmpty() ? 0 : (members.size() - 1) / 28;
        if (currentPage < maxPage) {
            inventory.setItem(53, createItem(Material.ARROW,
                    ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.next-page", "&e&lNext Page")),
                    ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.view-next", "View next page"))));
        }

        inventory.setItem(49, createItem(Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.back", "Back")),
                ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.common.member-operation.back-to-settings", "Return to guild settings"))));
    }

    private ItemStack createMemberHead(GuildMember member) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(member.getOfflinePlayer());
            meta.setDisplayName(ColorUtils.colorize("&e" + member.getPlayerName()));
            meta.setLore(Arrays.asList(
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                            "gui.common.member-operation.position", "Position") + ": &e" + member.getRole().getDisplayName()),
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                            "gui.common.member-operation.join-time", "Join time") + ": &e" + member.getJoinedAt()),
                    ColorUtils.colorize("&e" + languageManager.getGuiMessage(player,
                            "gui.transfer-leader.click-transfer", "Click to transfer leadership"))
            ));
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;
        if (!canTransfer(player)) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.common.leader-only", "&cOnly the guild leader can perform this operation")));
            return true;
        }
        sendBedrockList(player, 0);
        return true;
    }

    private void sendBedrockList(Player player, int page) {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(memberList -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                List<GuildMember> candidates = memberList.stream()
                        .filter(m -> !m.getPlayerUuid().equals(guild.getLeaderUuid()))
                        .collect(Collectors.toList());

                if (candidates.isEmpty()) {
                    SimpleForm form = SimpleForm.builder()
                            .title(languageManager.getGuiColoredMessage(player, "gui.transfer-leader.bedrock-title",
                                    "&6Transfer Leadership"))
                            .content(languageManager.getGuiColoredMessage(player, "gui.transfer-leader.bedrock-no-members",
                                    "&fNo members available to transfer leadership to"))
                            .button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-back", "&cBack"))
                            .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player))))
                            .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player))))
                            .build();
                    BedrockFormSender.sendForm(player.getUniqueId(), form);
                    return;
                }

                final int itemsPerPage = 10;
                int totalPages = (candidates.size() - 1) / itemsPerPage;
                final int safePage = Math.max(0, Math.min(page, totalPages));
                final int startIndex = safePage * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, candidates.size());
                final int memberCount = endIndex - startIndex;

                SimpleForm.Builder builder = SimpleForm.builder()
                        .title(languageManager.getGuiColoredMessage(player, "gui.transfer-leader.bedrock-title-page",
                                "&6Transfer Leadership - Page {page}", "{page}", String.valueOf(safePage + 1)))
                        .content(languageManager.getGuiColoredMessage(player, "gui.transfer-leader.bedrock-content",
                                "&fSelect a member to transfer leadership to"));

                for (int i = startIndex; i < endIndex; i++) {
                    builder.button("§e" + candidates.get(i).getPlayerName());
                }
                builder.button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-prev-page", "&ePrevious Page"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-next-page", "&eNext Page"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-back", "&cBack"));

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int clicked = response.clickedButtonId();
                    if (clicked < memberCount) {
                        GuildMember m = candidates.get(startIndex + clicked);
                        plugin.getGuiManager().openGUI(player,
                                new ConfirmTransferLeaderGUI(plugin, guild, m, player, "TransferLeaderGUI", false));
                    } else if (clicked == memberCount) {
                        sendBedrockList(player, safePage - 1);
                    } else if (clicked == memberCount + 1) {
                        sendBedrockList(player, safePage + 1);
                    } else {
                        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
                    }
                }));

                builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player))));

                BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
            });
        });
    }

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
