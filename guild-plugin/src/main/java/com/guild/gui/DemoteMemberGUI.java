package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.geyser.BedrockFormSender;
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
import java.util.concurrent.CompletableFuture;

/**
 * 降级成员GUI
 */
public class DemoteMemberGUI implements GUI {

    // ── 图像模式功能常量 ──
    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_BACK = "BACK";

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final Guild guild;
    private final Player player;
    private int currentPage = 0;
    private List<GuildMember> members;

    public DemoteMemberGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.player = player;
        // 初始化时获取成员列表
        this.members = List.of();
        loadMembers();
    }

    private void loadMembers() {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(memberList -> {
            this.members = memberList.stream()
                .filter(member -> !member.getPlayerUuid().equals(guild.getLeaderUuid()))
                .filter(member -> member.getRole().equals(GuildMember.Role.OFFICER)) // 只显示官员
                .collect(java.util.stream.Collectors.toList());
        });
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.demote-member.title",
                "&6Demote Member - Page {page}" + (currentPage + 1) + "页", "{page}", String.valueOf(currentPage + 1)));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 显示成员列表
        displayMembers(inventory);
        
        // 添加导航按钮
        setupNavigationButtons(inventory);

        // 应用图像模式
        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 检查是否是成员槽位
        int memberIndex = getIndexFromSlot(slot);
        if (memberIndex >= 0) {
            if (memberIndex < members.size()) {
                GuildMember member = members.get(memberIndex);
                handleDemoteMember(player, member);
            }
        } else if (slot == 45) {
            // 上一页
            if (currentPage > 0) {
                currentPage--;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 53) {
            // 下一页
            int maxPage = (members.size() - 1) / 28;
            if (currentPage < maxPage) {
                currentPage++;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 49) {
            // 返回
            plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
        }
    }
    
    /**
     * 槽位计算方法
     */
    
    /**
     * 从页内索引获取inventory槽位
     * @param index 页内索引 (0-27)
     * @return inventory槽位 (10-16, 19-25, 28-34, 37-43)
     */
    private int getSlotForIndex(int index) {
        int row = index / 7;      // 行号 (0-3)
        int col = index % 7;      // 列号 (0-6)
        return (row + 1) * 9 + col + 1; // 转换为inventory槽位
    }
    
    /**
     * 从inventory槽位获取页内索引
     * @param slot inventory槽位
     * @return 页内索引 (0-27)，或 -1 表示无效槽位
     */
    private int getIndexFromSlot(int slot) {
        int row = slot / 9;      // 行号 (1-4)
        int col = slot % 9;      // 列号 (0-8)
        
        // 检查是否在有效范围内
        if (row < 1 || row > 4 || col < 1 || col > 7) {
            return -1;
        }
        
        // 计算页内索引
        int pageIndex = (row - 1) * 7 + (col - 1);
        return currentPage * 28 + pageIndex;
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
     * 显示成员列表
     */
    private void displayMembers(Inventory inventory) {
        int startIndex = currentPage * 28; // 每页最多28个成员（4行7列）
        int endIndex = Math.min(startIndex + 28, members.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            GuildMember member = members.get(i);
            int slot = getSlotForIndex(i - startIndex);
            
            ItemStack memberHead = createMemberHead(member);
            inventory.setItem(slot, memberHead);
        }
    }
    
    /**
     * 设置导航按钮
     */
    private void setupNavigationButtons(Inventory inventory) {
        // 上一页按钮
        if (currentPage > 0) {
            ItemStack prevPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.previous-page", "&e&lPrevious Page")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.view-previous", "View previous page"))
            );
            inventory.setItem(45, prevPage);
        }

        // 下一页按钮
        int maxPage = (members.size() - 1) / 28;
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.next-page", "&e&lNext Page")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.view-next", "View next page"))
            );
            inventory.setItem(53, nextPage);
        }

        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.back", "Back")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.member-operation.back-to-settings", "Return to guild settings"))
        );
        inventory.setItem(49, back);
    }

    /**
     * 创建成员头像
     */
    private ItemStack createMemberHead(GuildMember member) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(member.getOfflinePlayer());
            meta.setDisplayName(ColorUtils.colorize("&7" + member.getPlayerName()));
            meta.setLore(Arrays.asList(
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.member-operation.current-position", "Current position") + ": &e" + member.getRole().getDisplayName()),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.member-operation.join-time", "Join time") + ": &e" + member.getJoinedAt()),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.demote-member.click-demote", "Click to demote to member"))
            ));
            head.setItemMeta(meta);
        }

        return head;
    }
    
    /**
     * 处理降级成员
     */
    private void handleDemoteMember(Player demoter, GuildMember member) {
        // 检查权限
        if (!demoter.hasPermission("guild.demote")) {
            String message = languageManager.getGuiMessage(demoter, "gui.common.no-permission", "&cInsufficient permission");
            demoter.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 降级成员
        plugin.getGuildService().updateMemberRoleAsync(member.getPlayerUuid(), GuildMember.Role.MEMBER, demoter.getUniqueId()).thenAccept(success -> {
            if (success) {
                String demoterMessage = languageManager.getGuiMessage(demoter, "gui.demote-member.demote.success", "&aDemoted &e{player} &a to regular member!", "{player}", member.getPlayerName());
                demoter.sendMessage(ColorUtils.colorize(demoterMessage));

                // 通知被降级的玩家
                Player demotedPlayer = plugin.getServer().getPlayer(member.getPlayerUuid());
                if (demotedPlayer != null) {
                    String demotedMessage = languageManager.getGuiMessage(demotedPlayer, "gui.demote-member.demote.demoted", "&aYou have been demoted to regular member of guild &e{guild} &a!", "{guild}", guild.getName());
                    demotedPlayer.sendMessage(ColorUtils.colorize(demotedMessage));
                }

                // 刷新GUI
                plugin.getGuiManager().openGUI(demoter, new DemoteMemberGUI(plugin, guild, demoter));
            } else {
                String message = languageManager.getGuiMessage(demoter, "gui.demote-member.demote.failed", "&cFailed to demote member!");
                demoter.sendMessage(ColorUtils.colorize(message));
            }
        });
    }

    // ── 基岩版表单 ──

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;
        sendBedrockDemoteList(player, 0);
        return true;
    }

    private void sendBedrockDemoteList(Player player, int page) {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(memberList -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                List<GuildMember> officers = memberList.stream()
                    .filter(m -> !m.getPlayerUuid().equals(guild.getLeaderUuid()))
                    .filter(m -> m.getRole() == GuildMember.Role.OFFICER)
                    .collect(java.util.stream.Collectors.toList());

                if (officers.isEmpty()) {
                    SimpleForm form = SimpleForm.builder()
                        .title(languageManager.getGuiColoredMessage(player, "gui.demote-member.bedrock-title", "&6Demote Member"))
                        .content(languageManager.getGuiColoredMessage(player, "gui.demote-member.bedrock-no-members", "&fNo officers available to demote"))
                        .button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-back", "&cBack"))
                        .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                            plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player))))
                        .closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                            plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player))))
                        .build();
                    BedrockFormSender.sendForm(player.getUniqueId(), form);
                    return;
                }

                final int itemsPerPage = 10;
                int totalPages = (officers.size() - 1) / itemsPerPage;
                final int safePage = Math.max(0, Math.min(page, totalPages));
                final int startIndex = safePage * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, officers.size());
                final int memberCount = endIndex - startIndex;

                SimpleForm.Builder builder = SimpleForm.builder()
                    .title(languageManager.getGuiColoredMessage(player, "gui.demote-member.bedrock-title-page", "&6Demote Member - Page {page}", "{page}", String.valueOf(safePage + 1)))
                    .content(languageManager.getGuiColoredMessage(player, "gui.demote-member.bedrock-content", "&fSelect an officer to demote to member"));

                for (int i = startIndex; i < endIndex; i++) {
                    builder.button("§f" + officers.get(i).getPlayerName());
                }

                builder.button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-prev-page", "&ePrevious Page"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-next-page", "&eNext Page"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.common.bedrock-back", "&cBack"));

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int clicked = response.clickedButtonId();
                    if (clicked < memberCount) {
                        GuildMember m = officers.get(startIndex + clicked);
                        handleDemoteMember(player, m);
                    } else if (clicked == memberCount) {
                        sendBedrockDemoteList(player, safePage - 1);
                    } else if (clicked == memberCount + 1) {
                        sendBedrockDemoteList(player, safePage + 1);
                    } else {
                        plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player));
                    }
                }));

                builder.closedResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                    plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild, player))));

                BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
            });
        });
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
