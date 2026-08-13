package com.guild.warehouse;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.core.time.TimeProvider;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildLog;
import com.guild.models.GuildMember;
import com.guild.models.GuildMember.Role;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import java.util.concurrent.CompletionException;
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
    /** In-flight page saves keyed by guild id; quit must not drop the session until these finish. */
    private final ConcurrentHashMap<Integer, CompletableFuture<Boolean>> pendingSaves = new ConcurrentHashMap<>();
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

    /**
     * Safety net for quit when InventoryClose never started a save.
     * If a save is still in flight, leave the session until {@link #handleClose} completes.
     */
    public void releaseSessionByPlayerIfIdle(UUID playerUuid) {
        openSessions.entrySet().removeIf(e -> {
            if (!e.getValue().equals(playerUuid)) {
                return false;
            }
            CompletableFuture<Boolean> pending = pendingSaves.get(e.getKey());
            return pending == null || pending.isDone();
        });
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

    /**
     * Persist role open permission and write a guild_logs audit entry for the actor.
     */
    public CompletableFuture<Boolean> setRoleOpenPermission(int guildId, Role role, boolean canOpen,
                                                            Player actor, String guildName) {
        return setRoleOpenPermission(guildId, role, canOpen).thenCompose(ok -> {
            if (!Boolean.TRUE.equals(ok) || actor == null) {
                return CompletableFuture.completedFuture(ok);
            }
            String name = guildName != null ? guildName : ("#" + guildId);
            return plugin.getGuildService().logGuildActionAsync(
                            guildId, name,
                            actor.getUniqueId().toString(), actor.getName(),
                            GuildLog.LogType.WAREHOUSE_PERM_CHANGED,
                            "Warehouse permission updated",
                            role.name() + "=" + (canOpen ? "on" : "off"))
                    .thenApply(v -> true);
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
                        try {
                            ItemStack stack = NbtItemSerializer.itemFromSnbt(rs.getString("nbt"));
                            if (stack != null && !stack.getType().isAir()) {
                                items.put(slot, stack);
                            }
                        } catch (Throwable t) {
                            logger.warning("[Warehouse] Skipping corrupt NBT at guild=" + guildId
                                    + " slot=" + slot + ": " + t.getMessage());
                        }
                    }
                }
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
            return items;
        });
    }

    /**
     * Saves one warehouse page. Only replaces absolute slots in [slotOffset, slotOffset + pageCapacity).
     * Callers must invoke from the region/main thread so inventory contents are snapshotted safely.
     */
    public CompletableFuture<Boolean> savePage(int guildId, Inventory inventory, int slotOffset, int pageCapacity) {
        ItemStack[] contents = cloneContents(inventory.getContents());
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
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
        pendingSaves.put(guildId, future);
        future.whenComplete((ok, err) -> pendingSaves.remove(guildId, future));
        return future;
    }

    private static ItemStack[] cloneContents(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            ItemStack stack = source[i];
            copy[i] = stack == null ? null : stack.clone();
        }
        return copy;
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
                    "&cGuild warehouse requires the NBTAPI plugin.");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        if (guild == null) {
            return;
        }
        if (guild.isFrozen()) {
            String msg = plugin.getLanguageManager().getCoreMessage(player, "warehouse.guild-frozen",
                    "&cThe guild is frozen; warehouse cannot be opened.");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }

        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (!canOpenWarehouse(guild, member, player)) {
            String msg = plugin.getLanguageManager().getCoreMessage(player, "warehouse.no-permission",
                    "&cYou do not have permission to open the guild warehouse.");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }

        int totalSlots = resolveSlots(guild);
        int pageCount = WarehouseSettings.getPageCount(totalSlots);
        if (page < 1 || page > pageCount) {
            String msg = plugin.getLanguageManager().getCoreMessage(player, "warehouse.invalid-page",
                            "&cInvalid page. Available: &e1-{pages} &7({slots} slots)")
                    .replace("{pages}", String.valueOf(pageCount))
                    .replace("{slots}", String.valueOf(totalSlots));
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }

        if (!tryAcquireSession(guild.getId(), player.getUniqueId())) {
            String msg = plugin.getLanguageManager().getCoreMessage(player, "warehouse.in-use",
                    "&cThe warehouse is in use by another member. Try again later.");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }

        int pageSlots = WarehouseSettings.getPageSlotCount(totalSlots, page);
        int offset = WarehouseSettings.getPageOffset(page);
        String titleTemplate = pageCount > 1
                ? plugin.getLanguageManager().getCoreMessage(player, "warehouse.title-page",
                "&8Guild Warehouse ({page}/{pages})")
                : plugin.getLanguageManager().getCoreMessage(player, "warehouse.title",
                "&8Guild Warehouse");
        String title = ColorUtils.colorize(titleTemplate
                .replace("{page}", String.valueOf(page))
                .replace("{pages}", String.valueOf(pageCount)));

        final int guildId = guild.getId();
        loadItemsInRange(guildId, offset, offset + pageSlots).whenComplete((items, err) -> {
            if (err != null || items == null) {
                Throwable cause = err instanceof CompletionException && err.getCause() != null
                        ? err.getCause() : err;
                logger.severe("[Warehouse] Failed to load items for guild " + guildId + ": "
                        + (cause != null ? cause.getMessage() : "unknown"));
                CompatibleScheduler.runTask(plugin, player, () -> {
                    releaseSession(guildId, player.getUniqueId());
                    if (player.isOnline()) {
                        String msg = plugin.getLanguageManager().getCoreMessage(player, "warehouse.load-failed",
                                "&cFailed to load guild warehouse. Please try again.");
                        player.sendMessage(ColorUtils.colorize(msg));
                    }
                });
                return;
            }
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (!player.isOnline()) {
                    releaseSession(guildId, player.getUniqueId());
                    return;
                }
                WarehouseChestHolder holder = new WarehouseChestHolder(guildId, page, pageSlots, totalSlots);
                Inventory inv = Bukkit.createInventory(holder, pageSlots, title);
                holder.setInventory(inv);
                for (Map.Entry<Integer, ItemStack> e : items.entrySet()) {
                    int absolute = e.getKey();
                    int local = absolute - offset;
                    if (local >= 0 && local < pageSlots) {
                        inv.setItem(local, e.getValue());
                    }
                }
                holder.captureOpenSnapshot(inv.getContents());
                player.openInventory(inv);
                logAccessAsync(guildId, player.getUniqueId(), player.getName(), "OPEN", page,
                        "slots=" + pageSlots);
            });
        });
    }

    public void handleClose(Player player, WarehouseChestHolder holder, Inventory inventory) {
        int guildId = holder.getGuildId();
        int capacity = holder.getPageSlotCount();
        int offset = holder.getSlotOffset();
        int page = holder.getPage();
        for (int i = capacity; i < inventory.getSize(); i++) {
            ItemStack extra = inventory.getItem(i);
            if (extra != null && !extra.getType().isAir()) {
                inventory.setItem(i, null);
                giveOrDrop(player, extra);
            }
        }
        String diffSummary = summarizeDiff(holder.getOpenSnapshot(), inventory.getContents(), capacity);
        UUID playerUuid = player.getUniqueId();
        String playerName = player.getName();
        savePage(guildId, inventory, offset, capacity).whenComplete((ok, err) -> {
            if (!Boolean.TRUE.equals(ok) || err != null) {
                Throwable cause = err instanceof CompletionException && err.getCause() != null
                        ? err.getCause() : err;
                logger.severe("[Warehouse] Save incomplete for guild " + guildId
                        + (cause != null ? ": " + cause.getMessage() : ""));
            } else {
                logAccessAsync(guildId, playerUuid, playerName, "SAVE", page, diffSummary);
            }
            CompatibleScheduler.runTask(plugin, player, () -> {
                // Switching pages: close saves async; do not drop lock if another page is already open
                if (!player.isOnline()) {
                    releaseSession(guildId, playerUuid);
                    return;
                }
                Inventory top = player.getOpenInventory().getTopInventory();
                if (!(top.getHolder() instanceof WarehouseChestHolder open)
                        || open.getGuildId() != guildId) {
                    releaseSession(guildId, playerUuid);
                }
            });
        });
    }

    private void logAccessAsync(int guildId, UUID playerUuid, String playerName,
                                String action, int page, String details) {
        if (!settings.isAccessLogEnabled()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO guild_warehouse_access_log "
                                 + "(guild_id, player_uuid, player_name, action, page, details, created_at) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, guildId);
                stmt.setString(2, playerUuid.toString());
                stmt.setString(3, playerName != null ? playerName : "?");
                stmt.setString(4, action);
                stmt.setInt(5, page);
                stmt.setString(6, details);
                stmt.setString(7, TimeProvider.nowString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.warning("[Warehouse] Access log write failed: " + e.getMessage());
            }
        });
    }

    /**
     * Build a compact put/take summary for access-log (no NBT, material:amount only).
     */
    static String summarizeDiff(ItemStack[] before, ItemStack[] after, int capacity) {
        Map<Material, Integer> delta = new HashMap<>();
        int limit = Math.min(capacity, Math.max(
                before != null ? before.length : 0,
                after != null ? after.length : 0));
        for (int i = 0; i < limit; i++) {
            ItemStack b = before != null && i < before.length ? before[i] : null;
            ItemStack a = after != null && i < after.length ? after[i] : null;
            applyStackDelta(delta, b, -1);
            applyStackDelta(delta, a, +1);
        }
        if (delta.isEmpty()) {
            return "unchanged";
        }
        StringBuilder put = new StringBuilder();
        StringBuilder take = new StringBuilder();
        for (Map.Entry<Material, Integer> e : delta.entrySet()) {
            int d = e.getValue();
            if (d == 0) {
                continue;
            }
            StringBuilder target = d > 0 ? put : take;
            if (target.length() > 0) {
                target.append(',');
            }
            target.append(e.getKey().name()).append(':').append(Math.abs(d));
        }
        StringBuilder out = new StringBuilder();
        if (put.length() > 0) {
            out.append("put=").append(put);
        }
        if (take.length() > 0) {
            if (out.length() > 0) {
                out.append(';');
            }
            out.append("take=").append(take);
        }
        return out.length() == 0 ? "unchanged" : out.toString();
    }

    private static void applyStackDelta(Map<Material, Integer> delta, ItemStack stack, int sign) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        delta.merge(stack.getType(), sign * stack.getAmount(), Integer::sum);
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
