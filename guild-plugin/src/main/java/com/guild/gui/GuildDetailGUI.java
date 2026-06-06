package com.guild.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;

/**
 * 工会详情GUI
 */
public class GuildDetailGUI implements GUI {

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
        String title = languageManager.getMessage(viewer, "guild-detail.title", "&6工会详情 - {name}");
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
        
        // 设置工会基本信息
        setupGuildInfo(inventory);
        
        // 设置工会成员列表
        setupMembersList(inventory);
        
        // 设置操作按钮
        setupActionButtons(inventory);
    }
    
    private void setupGuildInfo(Inventory inventory) {
        String guildTag = guild.getTag() != null ? guild.getTag() :
            languageManager.getMessage(viewer, "gui.no-tag", "无");

        // 工会名称和标签 - 放在顶部中央
        List<String> guildLore = new ArrayList<>();
        guildLore.add(ColorUtils.colorize("&7ID: " + guild.getId()));
        guildLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "gui.guild-tag", "标签") + ": [" + guildTag + "]"));
        guildLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.created-time", "创建时间") + ": " + formatTime(guild.getCreatedAt())));
        guildLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.status", "状态") + ": " +
            (guild.isFrozen() ? "&c" + languageManager.getMessage(viewer, "guild-detail.frozen", "已冻结") : "&a" + languageManager.getMessage(viewer, "guild-detail.normal", "正常"))));

        inventory.setItem(4, createItem(Material.SHIELD, ColorUtils.colorize("&6" + guild.getName()), guildLore.toArray(new String[0])));

        // 工会等级和资金 - 放在 slot 16
        List<String> economyLore = new ArrayList<>();
        economyLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.current-level", "当前等级") + ": &e" + guild.getLevel()));
        economyLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.current-balance", "当前资金") + ": &a" + plugin.getEconomyManager().format(guild.getBalance())));
        economyLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.max-members", "最大成员数") + ": &e" + guild.getMaxMembers()));
        economyLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.current-members", "当前成员数") + ": &e" + members.size()));

        inventory.setItem(16, createItem(Material.GOLD_INGOT,
            ColorUtils.colorize(languageManager.getMessage(viewer, "guild-detail.economy-info", "&e经济信息")),
            economyLore.toArray(new String[0])));

        // 工会描述 - 放在 slot 14
        List<String> descLore = new ArrayList<>();
        String description = guild.getDescription();
        if (description != null && !description.isEmpty()) {
            descLore.add(ColorUtils.colorize("&7" + description));
        } else {
            descLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "gui.no-description", "暂无描述")));
        }

        inventory.setItem(14, createItem(Material.BOOK,
            ColorUtils.colorize(languageManager.getMessage(viewer, "guild-detail.guild-description", "&e工会描述")),
            descLore.toArray(new String[0])));

        // 会长头像 - 放在 slot 12
        GuildMember leader = getLeaderMember();
        if (leader != null) {
            List<String> leaderLore = new ArrayList<>();
            leaderLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "gui.leader", "会长") + ": &c" + leader.getPlayerName()));
            leaderLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.joined", "加入") + ": " + formatTime(leader.getJoinedAt())));
            leaderLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.online", "在线") + ": " +
                (isPlayerOnline(leader.getPlayerUuid()) ? "&a" + languageManager.getMessage(viewer, "guild-detail.online-yes", "在线") : "&7" + languageManager.getMessage(viewer, "guild-detail.online-no", "离线"))));
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
            ColorUtils.colorize(languageManager.getMessage(viewer, "guild-detail.guild-members", "&a工会成员")),
            ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.total-members", "共 {count} 名成员", "{count}", String.valueOf(members.size())))));

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
            memberLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.position", "职位") + ": " + getRoleDisplayName(member.getRole())));
            memberLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.joined", "加入") + ": " + formatTime(member.getJoinedAt())));
            memberLore.add(ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "guild-detail.online", "在线") + ": " +
                (isPlayerOnline(member.getPlayerUuid()) ? "&a" + languageManager.getMessage(viewer, "guild-detail.online-yes", "在线") : "&7" + languageManager.getMessage(viewer, "guild-detail.online-no", "离线"))));

            inventory.setItem(slot, createPlayerHead(member.getPlayerName(), member.getPlayerUuid(), memberLore.toArray(new String[0])));
        }

        // 分页按钮
        inventory.setItem(18, currentPage > 0
            ? createItem(Material.ARROW,
                ColorUtils.colorize("&e" + languageManager.getMessage(viewer, "gui.previous-page", "上一页")),
                ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "gui.view-previous", "查看上一页")))
            : null);
        inventory.setItem(26, currentPage < totalPages
            ? createItem(Material.ARROW,
                ColorUtils.colorize("&a" + languageManager.getMessage(viewer, "gui.next-page", "下一页")),
                ColorUtils.colorize("&7" + languageManager.getMessage(viewer, "gui.view-next", "查看下一页")))
            : null);
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
            ColorUtils.colorize(languageManager.getMessage(viewer, "gui.back", "&c返回"))));

        // 管理操作（仅保留常用：冻结/删除）
        if (viewer.hasPermission("guild.admin")) {
            String freezeText = guild.isFrozen() ?
                languageManager.getMessage(viewer, "guild-detail.unfreeze-guild", "&a解冻工会") :
                languageManager.getMessage(viewer, "guild-detail.freeze-guild", "&c冻结工会");
            inventory.setItem(47, createItem(Material.ICE,
                ColorUtils.colorize(freezeText),
                ColorUtils.colorize(languageManager.getMessage(viewer, "guild-detail.toggle-freeze", "&7点击切换冻结状态"))));
            inventory.setItem(49, createItem(Material.TNT,
                ColorUtils.colorize(languageManager.getMessage(viewer, "guild-detail.delete-guild", "&4删除工会")),
                ColorUtils.colorize(languageManager.getMessage(viewer, "guild-detail.click-delete", "&7点击删除工会"))));
        }

        // 刷新
        inventory.setItem(53, createItem(Material.EMERALD,
            ColorUtils.colorize(languageManager.getMessage(viewer, "guild-detail.refresh", "&a刷新信息"))));

        // 会长转移（仅管理员）
        if (viewer.hasPermission("guild.admin")) {
            String transferKey = transferMode ? "guild-detail.transfer-leader-active" : "guild-detail.transfer-leader";
            String transferDescKey = transferMode ? "guild-detail.transfer-leader-active-desc" : "guild-detail.transfer-leader-desc";
            inventory.setItem(51, createItem(Material.GOLD_INGOT,
                ColorUtils.colorize(languageManager.getMessage(viewer, transferKey,
                        transferMode ? "&e请点击要转移给的成员..." : "&c转移会长")),
                ColorUtils.colorize("&7" + languageManager.getMessage(viewer, transferDescKey,
                        "&7点击后选择新会长"))));
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
            CompatibleScheduler.runTask(plugin, () -> {
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
            // 冻结/解冻工会
            toggleGuildFreeze(player);
            return;
        } else if (slot == 49 && player.hasPermission("guild.admin")) {
            // 删除工会
            deleteGuild(player);
            return;
        } else if (slot == 51 && player.hasPermission("guild.admin")) {
            // 切换会长转移模式
            transferMode = !transferMode;
            refresh(player);
            String msg = transferMode
                ? "&e请点击一个成员头像来转移会长职位"
                : "&7已取消会长转移";
            player.sendMessage(ColorUtils.colorize(languageManager.getMessage(player,
                    "guild-detail.transfer-mode-" + (transferMode ? "active" : "cancelled"), msg)));
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
            if (success) {
                String message = newStatus ?
                    languageManager.getMessage(player, "guild-detail.guild-frozen", "&a工会 {guild} 已被冻结！", "{guild}", guild.getName()) :
                    languageManager.getMessage(player, "guild-detail.guild-unfrozen", "&a工会 {guild} 已被解冻！", "{guild}", guild.getName());
                player.sendMessage(ColorUtils.colorize(message));
                // 更新本地guild对象
                guild.setFrozen(newStatus);
                refresh(player);
            } else {
                player.sendMessage(ColorUtils.colorize(languageManager.getMessage(player, "gui.operation-failed", "&c操作失败！")));
            }
        });
    }
    
    private void deleteGuild(Player player) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ColorUtils.colorize("&c您没有权限执行此操作！"));
            return;
        }
        // 打开统一的确认删除GUI
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild, player));
        player.closeInventory();
    }
    
    private String formatTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return languageManager.getMessage(viewer, "gui.unknown", "未知");
        return dateTime.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }
    
    private String getRoleDisplayName(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return languageManager.getMessage(viewer, "guild-role.leader", "&6会长");
            case OFFICER:
                return languageManager.getMessage(viewer, "guild-role.officer", "&e官员");
            case MEMBER:
                return languageManager.getMessage(viewer, "guild-role.member", "&7成员");
            default:
                return languageManager.getMessage(viewer, "guild-role.unknown", "&7未知");
        }
    }
    
    private void handleTransferLeader(Player player, GuildMember target) {
        transferMode = false;

        // 不能转移给自己
        if (target.getPlayerUuid().equals(guild.getLeaderUuid())) {
            player.sendMessage(ColorUtils.colorize(languageManager.getMessage(player,
                    "guild-detail.transfer-self", "&c不能将会长转移给自己")));
            refresh(player);
            return;
        }

        // 执行转移
        plugin.getGuildService().transferGuildLeadershipAsync(guild.getId(), target.getPlayerUuid(), target.getPlayerName())
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage(ColorUtils.colorize(languageManager.getMessage(player,
                                "guild-detail.transfer-success", "&a成功将会长转移给 &e{name}")
                                .replace("{name}", target.getPlayerName())));
                        guild.setLeaderUuid(target.getPlayerUuid());
                        guild.setLeaderName(target.getPlayerName());
                        loadMembers();
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getMessage(player,
                                "guild-detail.transfer-failed", "&c会长转移失败！")));
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
