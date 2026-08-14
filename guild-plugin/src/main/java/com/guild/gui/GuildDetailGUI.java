package com.guild.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.geyser.BedrockFormSender;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.geysermc.cumulus.form.SimpleForm;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;

/**
 * 公会详情GUI
 */
public class GuildDetailGUI implements GUI {

    // ── 图像模式功能常量 ──
    public static final String FUNC_GUILD_NAME = "GUILD_NAME";
    public static final String FUNC_LEADER_HEAD = "LEADER_HEAD";
    public static final String FUNC_DESCRIPTION = "DESCRIPTION";
    public static final String FUNC_ECONOMY_INFO = "ECONOMY_INFO";
    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_FREEZE = "FREEZE";
    public static final String FUNC_DELETE = "DELETE";
    public static final String FUNC_TRANSFER = "TRANSFER";
    public static final String FUNC_REFRESH = "REFRESH";
    public static final String FUNC_BACK = "BACK";

    private static final int MEMBERS_PER_PAGE = 21; // 3行×7列

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player viewer;
    private final LanguageManager languageManager;
    private List<GuildMember> members = new ArrayList<>();
    /** 是否处于会长转移选择模式 */
    private boolean transferMode = false;
    /** 当前成员分页页码 */
    private int currentPage = 0;
    /** 总成员分页数 */
    private int totalPages = 0;

    public GuildDetailGUI(GuildPlugin plugin, Guild guild, Player viewer) {
        this.plugin = plugin;
        this.guild = guild;
        this.viewer = viewer;
        this.languageManager = plugin.getLanguageManager();
        loadMembers();
    }

    @Override
    public String getTitle() {
        String title = languageManager.getGuiMessage(viewer, "gui.guild-detail.title", "&6Guild Details - {name}");
        return ColorUtils.colorize(title.replace("{name}", guild.getName()));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 设置公会基本信息
        setupGuildInfo(inventory);
        
        // 设置公会成员列表
        setupMembersList(inventory);
        
        // 设置操作按钮
        setupActionButtons(inventory);

        plugin.getGuiManager().applyImageModeIfNeeded(viewer, inventory, getGuiType());
    }
    
    private void setupGuildInfo(Inventory inventory) {
        String guildTag = guild.getTag() != null ? guild.getTag() :
            languageManager.getGuiMessage(viewer, "gui.common.no-tag", "No tag");

        // 公会名称和标签 - 放在顶部中央
        List<String> guildLore = new ArrayList<>();
        guildLore.add(ColorUtils.colorize("&7ID: " + guild.getId()));
        guildLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.common.guild-tag", "Guild Tag") + ": [" + guildTag + "]"));
        guildLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.created-time", "Created time") + ": " + formatTime(guild.getCreatedAt())));
        guildLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.status", "Status") + ": " +
            (guild.isFrozen() ? "&c" + languageManager.getGuiMessage(viewer, "gui.guild-detail.frozen", "Frozen") : "&a" + languageManager.getGuiMessage(viewer, "gui.guild-detail.normal", "Normal"))));

        inventory.setItem(4, createItem(Material.SHIELD, ColorUtils.colorize("&6" + guild.getName()), guildLore.toArray(new String[0])));

        // 公会等级和资金 - 放在 slot 16
        List<String> economyLore = new ArrayList<>();
        economyLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.current-level", "Current level") + ": &e" + guild.getLevel()));
        economyLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.current-balance", "Current balance") + ": &a" + plugin.getEconomyManager().format(guild.getBalance())));
        economyLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.max-members", "Max members") + ": &e" + guild.getMaxMembers()));
        economyLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.current-members", "Current members") + ": &e" + members.size()));

        inventory.setItem(16, createItem(Material.GOLD_INGOT,
            ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.guild-detail.economy-info", "Economy info")),
            economyLore.toArray(new String[0])));

        // 公会描述 - 放在 slot 14
        List<String> descLore = new ArrayList<>();
        String description = guild.getDescription();
        if (description != null && !description.isEmpty()) {
            descLore.add(ColorUtils.colorize("&7" + description));
        } else {
            descLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.common.no-description", "No description")));
        }

        inventory.setItem(14, createItem(Material.BOOK,
            ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.guild-detail.guild-description", "Guild Description")),
            descLore.toArray(new String[0])));

        // 会长头像 - 放在 slot 12
        GuildMember leader = getLeaderMember();
        if (leader != null) {
            List<String> leaderLore = new ArrayList<>();
            leaderLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.common.leader", "Leader") + ": &c" + leader.getPlayerName()));
            leaderLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.joined", "Joined") + ": " + formatTime(leader.getJoinedAt())));
            leaderLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.online", "Online") + ": " +
                (isPlayerOnline(leader.getPlayerUuid()) ? "&a" + languageManager.getGuiMessage(viewer, "gui.guild-detail.online-yes", "Online") : "&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.online-no", "Offline"))));
            inventory.setItem(12, createPlayerHead(leader.getPlayerName(), leader.getPlayerUuid(), leaderLore.toArray(new String[0])));
        }
    }
    
    /** 成员显示区域：3行×7列 = 21 个槽位，排除会长 */
    private static final int[] MEMBER_SLOTS = {
        19, 20, 21, 22, 23, 24, 25, // 第3行
        28, 29, 30, 31, 32, 33, 34, // 第4行
        37, 38, 39, 40, 41, 42, 43  // 第5行
    };

    private void setupMembersList(Inventory inventory) {
        // 成员列表标题
        inventory.setItem(10, createItem(Material.PAPER,
            ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.guild-detail.guild-members", "Guild Members")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.total-members", "Total {count} members", "{count}", String.valueOf(members.size())))));

        // 清除旧的成员槽位
        for (int slot : MEMBER_SLOTS) {
            inventory.setItem(slot, null);
        }

        // 排除会长后的成员列表
        List<GuildMember> nonLeaderMembers = new ArrayList<>();
        for (GuildMember m : members) {
            if (!m.getPlayerUuid().equals(guild.getLeaderUuid())) {
                nonLeaderMembers.add(m);
            }
        }

        // 计算分页
        totalPages = (nonLeaderMembers.size() - 1) / MEMBERS_PER_PAGE;
        if (totalPages < 0) totalPages = 0;
        if (currentPage > totalPages) currentPage = totalPages;

        // 显示当前页成员
        int startIndex = currentPage * MEMBERS_PER_PAGE;
        int endIndex = Math.min(startIndex + MEMBERS_PER_PAGE, nonLeaderMembers.size());

        for (int i = startIndex; i < endIndex; i++) {
            GuildMember member = nonLeaderMembers.get(i);
            int slot = MEMBER_SLOTS[i - startIndex];

            List<String> memberLore = new ArrayList<>();
            memberLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.position", "Position") + ": " + getRoleDisplayName(member.getRole())));
            memberLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.joined", "Joined") + ": " + formatTime(member.getJoinedAt())));
            memberLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.online", "Online") + ": " +
                (isPlayerOnline(member.getPlayerUuid()) ? "&a" + languageManager.getGuiMessage(viewer, "gui.guild-detail.online-yes", "Online") : "&7" + languageManager.getGuiMessage(viewer, "gui.guild-detail.online-no", "Offline"))));

            inventory.setItem(slot, createPlayerHead(member.getPlayerName(), member.getPlayerUuid(), memberLore.toArray(new String[0])));
        }

        // 分页按钮 — 无分页时显示边框
        ItemStack borderPane = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        if (currentPage > 0) {
            inventory.setItem(18, createItem(Material.ARROW,
                ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer, "gui.common.previous-page", "&e&lPrevious Page")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.common.view-previous", "View previous page"))));
        } else {
            inventory.setItem(18, borderPane);
        }
        if (currentPage < totalPages) {
            inventory.setItem(26, createItem(Material.ARROW,
                ColorUtils.colorize("&a" + languageManager.getGuiMessage(viewer, "gui.common.next-page", "&e&lNext Page")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, "gui.common.view-next", "View next page"))));
        } else {
            inventory.setItem(26, borderPane);
        }
    }

    /** 获取会长成员对象 */
    private GuildMember getLeaderMember() {
        for (GuildMember m : members) {
            if (m.getPlayerUuid().equals(guild.getLeaderUuid())) return m;
        }
        return null;
    }
    
    private void setupActionButtons(Inventory inventory) {
        // 返回
        inventory.setItem(45, createItem(Material.ARROW,
            ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.common.back", "Back"))));

        // 管理操作（仅保留常用：冻结/删除）
        if (viewer.hasPermission("guild.admin")) {
            String freezeText = guild.isFrozen() ?
                languageManager.getGuiMessage(viewer, "gui.guild-detail.unfreeze-guild", "Unfreeze Guild") :
                languageManager.getGuiMessage(viewer, "gui.guild-detail.freeze-guild", "Freeze Guild");
            inventory.setItem(47, createItem(Material.ICE,
                ColorUtils.colorize(freezeText),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.guild-detail.toggle-freeze", "Click to toggle freeze status"))));
            inventory.setItem(49, createItem(Material.TNT,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.guild-detail.delete-guild", "Delete Guild")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.guild-detail.click-delete", "Click to delete guild"))));
        }

        // 刷新
        inventory.setItem(53, createItem(Material.EMERALD,
            ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.guild-detail.refresh", "&aRefresh Info"))));

        // 会长转移（仅管理员）
        if (viewer.hasPermission("guild.admin")) {
            String transferKey = transferMode ? "gui.guild-detail.transfer-leader-active" : "gui.guild-detail.transfer-leader";
            String transferDescKey = transferMode ? "gui.guild-detail.transfer-leader-active-desc" : "gui.guild-detail.transfer-leader-desc";
            inventory.setItem(51, createItem(Material.GOLD_INGOT,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, transferKey,
                        transferMode ? "&eClick the member to transfer to..." : "&cTransfer Leader")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer, transferDescKey,
                        "&7Click to choose the new leader"))));
        }
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
    
    private void loadMembers() {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(membersList -> {
            this.members = membersList != null ? membersList : new ArrayList<>();
            CompatibleScheduler.runTask(plugin, viewer, () -> {
                if (viewer.isOnline()) {
                    refresh(viewer);
                }
            });
        });
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 45) {
            // 返回
            plugin.getGuiManager().openGUI(player, new GuildListManagementGUI(plugin, player));
            return;
        } else if (slot == 53) {
            // 刷新
            loadMembers();
            return;
        } else if (slot == 47 && player.hasPermission("guild.admin")) {
            // 冻结/解冻公会
            toggleGuildFreeze(player);
            return;
        } else if (slot == 49 && player.hasPermission("guild.admin")) {
            // 删除公会
            deleteGuild(player);
            return;
        } else if (slot == 51 && player.hasPermission("guild.admin")) {
            // 切换会长转移模式
            transferMode = !transferMode;
            refresh(player);
            String msg = transferMode
                ? "&eClick a member head to transfer leadership"
                : "&7Transfer cancelled";
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.guild-detail.transfer-mode-" + (transferMode ? "active" : "cancelled"), msg)));
            return;
        }

        // 分页
        if (slot == 18 && currentPage > 0) {
            currentPage--;
            refresh(player);
            return;
        }
        if (slot == 26 && currentPage < totalPages) {
            currentPage++;
            refresh(player);
            return;
        }

        // 点击成员头像：会长转移模式 or 查看详情
        GuildMember target = null;

        // 会长槽位 (slot 12)
        if (slot == 12) {
            for (GuildMember m : members) {
                if (m.getPlayerUuid().equals(guild.getLeaderUuid())) {
                    target = m;
                    break;
                }
            }
        } else {
            // 成员显示区域 (19-25, 28-34, 37-43)
            int memberSlotIndex = -1;
            for (int i = 0; i < MEMBER_SLOTS.length; i++) {
                if (MEMBER_SLOTS[i] == slot) {
                    memberSlotIndex = i;
                    break;
                }
            }
            if (memberSlotIndex >= 0) {
                // 从排除会长的列表中计算实际成员索引
                List<GuildMember> nonLeaderMembers = new ArrayList<>();
                for (GuildMember m : members) {
                    if (!m.getPlayerUuid().equals(guild.getLeaderUuid())) {
                        nonLeaderMembers.add(m);
                    }
                }
                int memberIdx = (currentPage * MEMBERS_PER_PAGE) + memberSlotIndex;
                if (memberIdx < nonLeaderMembers.size()) {
                    target = nonLeaderMembers.get(memberIdx);
                }
            }
        }

        if (target != null) {
            if (transferMode && player.hasPermission("guild.admin")) {
                handleTransferLeader(player, target);
            }
            // 非转移模式：可扩展其他点击行为
        }
    }
    
    private void toggleGuildFreeze(Player player) {
        boolean newStatus = !guild.isFrozen();
        plugin.getGuildService().updateGuildFrozenStatusAsync(guild.getId(), newStatus).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    String message = newStatus ?
                        languageManager.getGuiMessage(player, "gui.guild-detail.guild-frozen", "&aGuild {guild} has been frozen!", "{guild}", guild.getName()) :
                        languageManager.getGuiMessage(player, "gui.guild-detail.guild-unfrozen", "&aGuild {guild} has been unfrozen!", "{guild}", guild.getName());
                    player.sendMessage(ColorUtils.colorize(message));
                    // 更新本地guild对象
                    guild.setFrozen(newStatus);
                    refresh(player);
                } else {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.operation-failed", "&cOperation failed!")));
                }
            });
        });
    }
    
    private void deleteGuild(Player player) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.general.no-permission", "&cYou do not have permission to perform this action!")));
            return;
        }
        // 打开统一的确认删除GUI，标记来源为 GuildDetailGUI 以便取消时返回
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild, player, "GuildDetailGUI"));
    }
    
    private String formatTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return languageManager.getGuiMessage(viewer, "gui.common.unknown", "Unknown");
        return dateTime.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }
    
    private String getRoleDisplayName(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return languageManager.getGuiMessage(viewer, "gui.common.guild-role.leader", "&6Leader");
            case OFFICER:
                return languageManager.getGuiMessage(viewer, "gui.common.guild-role.officer", "&eOfficer");
            case MEMBER:
                return languageManager.getGuiMessage(viewer, "gui.common.guild-role.member", "&7Member");
            default:
                return languageManager.getGuiMessage(viewer, "gui.common.guild-role.unknown", "&7Unknown");
        }
    }
    
    private void handleTransferLeader(Player player, GuildMember target) {
        transferMode = false;

        // 不能转移给自己
        if (target.getPlayerUuid().equals(guild.getLeaderUuid())) {
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.guild-detail.transfer-self", "&cCannot transfer leadership to yourself")));
            refresh(player);
            return;
        }

        // 执行转移
        plugin.getGuildService().transferGuildLeadershipAsync(guild.getId(), target.getPlayerUuid(), target.getPlayerName())
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.guild-detail.transfer-success", "&aSuccessfully transferred to &e{name}")
                                .replace("{name}", target.getPlayerName())));
                        guild.setLeaderUuid(target.getPlayerUuid());
                        guild.setLeaderName(target.getPlayerName());
                        loadMembers();
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                "gui.guild-detail.transfer-failed", "&cLeadership transfer failed!")));
                        refresh(player);
                    }
                });
            });
    }

    private boolean isPlayerOnline(java.util.UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
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
    
    private ItemStack createPlayerHead(String playerName, UUID playerUuid, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&e" + playerName));
            
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            
            // 使用 UUID 设置玩家皮肤头颅
            try {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUuid));
            } catch (Exception e) {
                // 如果设置失败，使用默认头颅
            }
            
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;
        sendBedrockGuildDetail(player, 0);
        return true;
    }

    private void sendBedrockGuildDetail(Player player, int page) {
        plugin.getGuildService().getGuildMembersAsync(guild.getId()).thenAccept(membersList -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (!player.isOnline()) return;

                List<GuildMember> allMembers = membersList != null ? membersList : new ArrayList<>();

                // 排除会长的成员列表
                List<GuildMember> nonLeaderMembers = new ArrayList<>();
                GuildMember leader = null;
                for (GuildMember m : allMembers) {
                    if (m.getPlayerUuid().equals(guild.getLeaderUuid())) {
                        leader = m;
                    } else {
                        nonLeaderMembers.add(m);
                    }
                }

                String guildTag = guild.getTag() != null ? guild.getTag() : languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-no-tag", "None");
                String statusText = guild.isFrozen()
                        ? languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-status-frozen", "&cFrozen")
                        : languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-status-normal", "&aNormal");
                String leaderName = leader != null ? leader.getPlayerName() : guild.getLeaderName();

                StringBuilder content = new StringBuilder();
                content.append("§6").append(guild.getName()).append(" §f[").append(guildTag).append("]\n");
                content.append(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-level", "&fLevel: &e{level}",
                        "{level}", String.valueOf(guild.getLevel())));
                content.append(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-balance", " &fBalance: &a{balance}",
                        "{balance}", plugin.getEconomyManager().format(guild.getBalance()))).append("\n");
                content.append(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-leader", "&fLeader: &c{leader}",
                        "{leader}", leaderName));
                content.append(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-members", " &fMembers: &e{current}/{max}",
                        "{current}", String.valueOf(allMembers.size()), "{max}", String.valueOf(guild.getMaxMembers()))).append("\n");
                content.append(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-status", "&fStatus: {status}",
                        "{status}", statusText));
                if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
                    content.append("\n").append(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-description", "&fDescription: &7{description}",
                            "{description}", guild.getDescription()));
                }
                if (transferMode) {
                    content.append("\n\n").append(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-transfer-hint", "&e>>> Transfer Mode: Click member to transfer <<<"));
                }

                int itemsPerPage = 10;
                int totalPages = Math.max(1, (int) Math.ceil((double) nonLeaderMembers.size() / itemsPerPage));
                final int safePage = Math.max(0, Math.min(page, totalPages - 1));
                int startIndex = safePage * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, nonLeaderMembers.size());

                SimpleForm.Builder builder = SimpleForm.builder()
                    .title(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-title", "&6Guild Details - {guild_name}",
                            "{guild_name}", guild.getName()))
                    .content(content.toString());

                List<GuildMember> pageMembers = new ArrayList<>();
                for (int i = startIndex; i < endIndex; i++) {
                    GuildMember m = nonLeaderMembers.get(i);
                    pageMembers.add(m);
                    String roleColor = m.getRole() == GuildMember.Role.OFFICER ? "§e" : "§f";
                    String online = isPlayerOnline(m.getPlayerUuid()) ? "§a●" : "§7●";
                    builder.button(roleColor + m.getPlayerName() + " " + online);
                }

                // 操作按钮
                builder.button(guild.isFrozen()
                        ? languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-unfreeze", "&aUnfreeze Guild")
                        : languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-freeze", "&cFreeze Guild"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-delete", "&4Delete Guild"));
                builder.button(transferMode
                        ? languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-cancel-transfer", "&eCancel Transfer Mode")
                        : languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-transfer", "&cTransfer Leader"));
                if (safePage > 0) builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-prev-page", "&ePrevious Page"));
                if (safePage < totalPages - 1) builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-next-page", "&eNext Page"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-back", "&cBack"));

                final int memberCount = pageMembers.size();
                final int freezeIdx = memberCount;
                final int deleteIdx = memberCount + 1;
                final int transferIdx = memberCount + 2;
                int nextIdx = memberCount + 3;
                final int prevIdx = safePage > 0 ? nextIdx++ : -1;
                final int nextIdxFinal = safePage < totalPages - 1 ? nextIdx++ : -1;
                final int backIdx = nextIdx;

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int id = response.clickedButtonId();
                    if (id < memberCount) {
                        GuildMember target = pageMembers.get(id);
                        if (transferMode) {
                            bedrockTransferLeader(player, target);
                        }
                        // 非转移模式下点击成员暂无额外操作
                        return;
                    }
                    if (id == freezeIdx) {
                        bedrockToggleFreeze(player);
                        return;
                    }
                    if (id == deleteIdx) {
                        deleteGuild(player);
                        return;
                    }
                    if (id == transferIdx) {
                        transferMode = !transferMode;
                        String msg = transferMode
                                ? languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-transfer-mode-active", "&eClick a member to transfer leadership")
                                : languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-transfer-mode-cancelled", "&7Transfer cancelled");
                        player.sendMessage(msg);
                        sendBedrockGuildDetail(player, safePage);
                        return;
                    }
                    if (id == prevIdx && prevIdx >= 0) {
                        sendBedrockGuildDetail(player, safePage - 1);
                        return;
                    }
                    if (id == nextIdxFinal && nextIdxFinal >= 0) {
                        sendBedrockGuildDetail(player, safePage + 1);
                        return;
                    }
                    if (id == backIdx) {
                        plugin.getGuiManager().openGUI(player, new GuildListManagementGUI(plugin, player));
                    }
                }));

                builder.closedResultHandler(response -> {});

                BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
            });
        });
    }

    private void bedrockToggleFreeze(Player player) {
        boolean newStatus = !guild.isFrozen();
        plugin.getGuildService().updateGuildFrozenStatusAsync(guild.getId(), newStatus).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    String message = newStatus ?
                        languageManager.getGuiMessage(player, "gui.guild-detail.guild-frozen", "&aGuild {guild} has been frozen!", "{guild}", guild.getName()) :
                        languageManager.getGuiMessage(player, "gui.guild-detail.guild-unfrozen", "&aGuild {guild} has been unfrozen!", "{guild}", guild.getName());
                    player.sendMessage(ColorUtils.colorize(message));
                    guild.setFrozen(newStatus);
                } else {
                    player.sendMessage(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-operation-failed", "&cOperation failed!"));
                }
                sendBedrockGuildDetail(player, 0);
            });
        });
    }

    private void bedrockTransferLeader(Player player, GuildMember target) {
        if (target.getPlayerUuid().equals(guild.getLeaderUuid())) {
            player.sendMessage(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-transfer-self", "&cCannot transfer leadership to yourself"));
            sendBedrockGuildDetail(player, 0);
            return;
        }

        plugin.getGuildService().transferGuildLeadershipAsync(guild.getId(), target.getPlayerUuid(), target.getPlayerName())
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        player.sendMessage(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-transfer-success", "&aSuccessfully transferred to &e{name}",
                                "{name}", target.getPlayerName()));
                        guild.setLeaderUuid(target.getPlayerUuid());
                        guild.setLeaderName(target.getPlayerName());
                        transferMode = false;
                    } else {
                        player.sendMessage(languageManager.getGuiColoredMessage(player, "gui.guild-detail.bedrock-transfer-failed", "&cLeadership transfer failed!"));
                    }
                    sendBedrockGuildDetail(player, 0);
                });
            });
    }

    @Override
    public void onClose(Player player) {
        // 关闭时的处理
    }
    
    @Override
    public void refresh(Player player) {
        if (player.isOnline()) {
            plugin.getGuiManager().refreshGUI(player);
        }
    }
}
