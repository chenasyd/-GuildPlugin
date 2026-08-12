package com.guild.warehouse;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildMember.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Guild warehouse: load/save via NBTAPI, capacity from peak level, multi-page chests, one open session per guild.
 */
public class GuildWarehouseService {

    private final GuildPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final WarehouseSettings settings;
    private final ConcurrentHashMap<Integer, UUID> openSessions = new ConcurrentHashMap<>();
    private volatile boolean nbtApiAvailable;

    public GuildWarehouseService(GuildPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.logger = plugin.getLogger();
        this.settings = new WarehouseSettings(plugin);
        refreshAvailability();
    }

    public void reload() {
        settings.reload();
        refreshAvailability();
    }

    public void refreshAvailability() {
        Plugin nbt = Bukkit.getPluginManager().getPlugin("NBTAPI");
        nbtApiAvailable = nbt != null && nbt.isEnabled();
        if (!nbtApiAvailable) {
            logger.info("[Warehouse] NBTAPI not found — guild warehouse disabled until it is installed");
        } else if (!settings.isEnabled()) {
            logger.info("[Warehouse] Disabled in config (guild-warehouse.enabled=false)");
        } else {
            logger.info("[Warehouse] Ready (NBTAPI softdepend OK)");
        }
    }

    public boolean isAvailable() {
        return settings.isEnabled() && nbtApiAvailable;
    }

    public WarehouseSettings getSettings() {
        return settings;
    }

    public boolean tryAcquireSession(int guildId, UUID playerUuid) {
        UUID existing = openSessions.putIfAbsent(guildId, playerUuid);
        return existing == null || existing.equals(playerUuid);
    }

    public void releaseSession(int guildId, UUID playerUuid) {
        openSessions.computeIfPresent(guildId, (id, holder) ->
                holder.equals(playerUuid) ? null : holder);
    }

    public void releaseSessionByPlayer(UUID playerUuid) {
        openSessions.entrySet().removeIf(e -> e.getValue().equals(playerUuid));
    }

    public UUID getSessionHolder(int guildId) {
        return openSessions.get(guildId);
    }

    public int resolveSlots(Guild guild) {
        return settings.getSlotsForPeakLevel(guild.getPeakLevel());
    }

    public int resolvePageCount(Guild guild) {
        return WarehouseSettings.getPageCount(resolveSlots(guild));
    }

    /**
     * Leader always allowed. Officer/member: per-guild override, else config can-warehouse.
     */
    public boolean canOpenWarehouse(Guild guild, GuildMember member, Player player) {
        if (player != null && player.hasPermission("guild.admin")) {
            return true;
        }
        if (guild == null || member == null || member.getGuildId() != guild.getId()) {
            return false;
        }
        Role role = member.getRole();
        if (role == Role.LEADER) {
            return true;
        }
        if (role == Role.OFFICER || role == Role.MEMBER) {
            Boolean override = getRoleOpenOverrideSync(guild.getId(), role);
            if (override != null) {
                return override;
            }
            return plugin.getPermissionManager().getDefaultCanWarehouse(role);
        }
        return false;
    }

    public Boolean getRoleOpenOverrideSync(int guildId, Role role) {
        if (role != Role.OFFICER && role != Role.MEMBER) {
            return null;
        }
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT can_open FROM guild_warehouse_role_perms WHERE guild_id = ? AND role = ?")) {
            stmt.setInt(1, guildId);
            stmt.setString(2, role.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("can_open");
                }
            }
        } catch (SQLException e) {
            logger.warning("[Warehouse] Failed to read role perm: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<Boolean> setRoleOpenPermission(int guildId, Role role, boolean canOpen) {
        if (role != Role.OFFICER && role != Role.MEMBER) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            String sql = databaseManager.getDatabaseType() == DatabaseManager.DatabaseType.MYSQL
                    ? "INSERT INTO guild_warehouse_role_perms (guild_id, role, can_open) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE can_open = VALUES(can_open)"
                    : "INSERT INTO guild_warehouse_role_perms (guild_id, role, can_open) VALUES (?, ?, ?) "
                    + "ON CONFLICT(guild_id, role) DO UPDATE SET can_open = excluded.can_open";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                stmt.setString(2, role.name());
                stmt.setBoolean(3, canOpen);
                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                logger.severe("[Warehouse] Failed to set role perm: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Map<Integer, ItemStack>> loadItemsInRange(int guildId, int fromInclusive, int toExclusive) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Integer, ItemStack> items = new HashMap<>();
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT slot, nbt FROM guild_warehouse_items WHERE guild_id = ? AND slot >= ? AND slot < ?")) {
                stmt.setInt(1, guildId);
                stmt.setInt(2, fromInclusive);
                stmt.setInt(3, toExclusive);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int slot = rs.getInt("slot");
                        ItemStack stack = NbtItemSerializer.itemFromSnbt(rs.getString("nbt"));
                        if (stack != null && !stack.getType().isAir()) {
                            items.put(slot, stack);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("[Warehouse] Failed to load items: " + e.getMessage());
            } catch (Throwable t) {
                logger.severe("[Warehouse] NBT deserialize failed: " + t.getMessage());
            }
            return items;
        });
    }

    /**
     * Saves one warehouse page. Only replaces absolute slots in [slotOffset, slotOffset + pageCapacity).
     */
    public CompletableFuture<Boolean> savePage(int guildId, Inventory inventory, int slotOffset, int pageCapacity) {
        ItemStack[] contents = inventory.getContents();
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement del = conn.prepareStatement(
                            "DELETE FROM guild_warehouse_items WHERE guild_id = ? AND slot >= ? AND slot < ?")) {
                        del.setInt(1, guildId);
                        del.setInt(2, slotOffset);
                        del.setInt(3, slotOffset + pageCapacity);
                        del.executeUpdate();
                    }
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO guild_warehouse_items (guild_id, slot, nbt) VALUES (?, ?, ?)")) {
                        int limit = Math.min(pageCapacity, contents.length);
                        for (int local = 0; local < limit; local++) {
                            ItemStack stack = contents[local];
                            if (stack == null || stack.getType().isAir()) {
                                continue;
                            }
                            String snbt = NbtItemSerializer.itemToSnbt(stack);
                            if (snbt == null) {
                                continue;
                            }
                            ins.setInt(1, guildId);
                            ins.setInt(2, slotOffset + local);
                            ins.setString(3, snbt);
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                    conn.commit();
                    return true;
                } catch (Exception e) {
                    conn.rollback();
                    logger.severe("[Warehouse] Failed to save items: " + e.getMessage());
                    return false;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                logger.severe("[Warehouse] Save connection error: " + e.getMessage());
                return false;
            }
        });
    }

    public void openWarehouse(Player player, Guild guild) {
        openWarehouse(player, guild, 1);
    }

    /**
     * @param page 1-based page index
     */
    public void openWarehouse(Player player, Guild guild, int page) {
        if (!isAvailable()) {
            String msg = plugin.getLanguageManager().getCoreMessage(player, "warehouse.nbtapi-missing",
                    "&c公会仓库需要安装 NBTAPI 插件才能使用。");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        if (guild == null) {
            return;
        }
        if (guild.isFrozen()) {
            String msg = plugin.getLanguageManager().getCoreMessage(player, "warehouse.guild-frozen",
                    "&c工会已冻结，无法打开仓库。");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }

        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (!canOpenWarehouse(guild, member, player)) {
            String msg = plugin.getLanguageManager().getCoreMessage(player, "warehouse.no-permission",
                    "&c你没有权限打开公会仓库。");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }

        int totalSlots = resolveSlots(guild);
        int pageCount = WarehouseSettings.getPageCount(totalSlots);
        if (page < 1 || page > pageCount) {
            String msg = plugin.getLanguageManager().getCoreMessage(player, "warehouse.invalid-page",
                            "&c无效页码。可用页: &e1-{pages} &7（共 {slots} 槽）")
                    .replace("{pages}", String.valueOf(pageCount))
                    .replace("{slots}", String.valueOf(totalSlots));
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }

        if (!tryAcquireSession(guild.getId(), player.getUniqueId())) {
            String msg = plugin.getLanguageManager().getCoreMessage(player, "warehouse.in-use",
                    "&c仓库正被其他成员使用，请稍后再试。");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }

        int pageSlots = WarehouseSettings.getPageSlotCount(totalSlots, page);
        int offset = WarehouseSettings.getPageOffset(page);
        String titleTemplate = pageCount > 1
                ? plugin.getLanguageManager().getCoreMessage(player, "warehouse.title-page",
                "&8公会仓库 ({page}/{pages})")
                : plugin.getLanguageManager().getCoreMessage(player, "warehouse.title",
                "&8公会仓库");
        String title = ColorUtils.colorize(titleTemplate
                .replace("{page}", String.valueOf(page))
                .replace("{pages}", String.valueOf(pageCount)));

        loadItemsInRange(guild.getId(), offset, offset + pageSlots).thenAccept(items ->
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (!player.isOnline()) {
                        releaseSession(guild.getId(), player.getUniqueId());
                        return;
                    }
                    WarehouseChestHolder holder = new WarehouseChestHolder(guild.getId(), page, pageSlots, totalSlots);
                    Inventory inv = Bukkit.createInventory(holder, pageSlots, title);
                    holder.setInventory(inv);
                    for (Map.Entry<Integer, ItemStack> e : items.entrySet()) {
                        int absolute = e.getKey();
                        int local = absolute - offset;
                        if (local >= 0 && local < pageSlots) {
                            inv.setItem(local, e.getValue());
                        }
                    }
                    player.openInventory(inv);
                }));
    }

    public void handleClose(Player player, WarehouseChestHolder holder, Inventory inventory) {
        int guildId = holder.getGuildId();
        int capacity = holder.getPageSlotCount();
        int offset = holder.getSlotOffset();
        for (int i = capacity; i < inventory.getSize(); i++) {
            ItemStack extra = inventory.getItem(i);
            if (extra != null && !extra.getType().isAir()) {
                inventory.setItem(i, null);
                giveOrDrop(player, extra);
            }
        }
        savePage(guildId, inventory, offset, capacity).whenComplete((ok, err) ->
                CompatibleScheduler.runTask(plugin, player, () -> {
                    // Switching pages: close saves async; do not drop lock if another page is already open
                    if (!player.isOnline()) {
                        releaseSession(guildId, player.getUniqueId());
                        return;
                    }
                    Inventory top = player.getOpenInventory().getTopInventory();
                    if (!(top.getHolder() instanceof WarehouseChestHolder open)
                            || open.getGuildId() != guildId) {
                        releaseSession(guildId, player.getUniqueId());
                    }
                }));
    }

    static void giveOrDrop(Player player, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack left : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
    }
}
