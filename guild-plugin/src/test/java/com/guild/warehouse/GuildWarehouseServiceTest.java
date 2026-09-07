package com.guild.warehouse;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.guild.GuildPlugin;
import com.guild.core.config.ConfigManager;
import com.guild.core.database.DatabaseManager;
import com.guild.core.language.LanguageManager;
import com.guild.core.permissions.GuildMembershipRules;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildMember.Role;
import com.guild.services.GuildService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuildWarehouseServiceTest {

    private static final UUID PLAYER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAYER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private HikariDataSource dataSource;
    private GuildPlugin plugin;
    private GuildMembershipRules membershipRules;
    private LanguageManager languageManager;
    private GuildService guildService;
    private YamlConfiguration cfg;
    private GuildWarehouseService service;

    @BeforeEach
    void setUp() throws Exception {
        MockBukkit.mock();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:gwh_svc_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(2);
        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE guild_warehouse_role_perms (
                        guild_id INT NOT NULL,
                        role VARCHAR(16) NOT NULL,
                        can_open TINYINT NOT NULL,
                        PRIMARY KEY (guild_id, role)
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE guild_warehouse_items (
                        guild_id INT NOT NULL,
                        slot INT NOT NULL,
                        nbt MEDIUMTEXT NOT NULL,
                        PRIMARY KEY (guild_id, slot)
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE guild_warehouse_access_log (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        guild_id INT NOT NULL,
                        player_uuid VARCHAR(36) NOT NULL,
                        player_name VARCHAR(16) NOT NULL,
                        action VARCHAR(16) NOT NULL,
                        page INT NOT NULL,
                        details VARCHAR(255),
                        created_at VARCHAR(32) NOT NULL
                    )
                    """);
        }

        plugin = mock(GuildPlugin.class);
        membershipRules = mock(GuildMembershipRules.class);
        languageManager = mock(LanguageManager.class);
        guildService = mock(GuildService.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);

        ConfigManager configManager = mock(ConfigManager.class);
        cfg = new YamlConfiguration();
        cfg.set("guild-warehouse.enabled", true);
        cfg.set("guild-warehouse.slots-by-level.1", 54);

        when(plugin.getLogger()).thenReturn(Logger.getLogger("GuildWarehouseServiceTest"));
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getDatabaseManager()).thenReturn(databaseManager);
        when(databaseManager.getConnection()).thenAnswer(inv -> dataSource.getConnection());
        when(databaseManager.getDatabaseType()).thenReturn(DatabaseManager.DatabaseType.MYSQL);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(cfg);
        when(plugin.getMembershipRules()).thenReturn(membershipRules);
        when(plugin.getLanguageManager()).thenReturn(languageManager);
        when(plugin.getGuildService()).thenReturn(guildService);

        service = new GuildWarehouseService(plugin);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
        MockBukkit.unmock();
    }

    @Test
    void tryAcquireSession_allowsFirstHolderAndSamePlayer() {
        assertTrue(service.tryAcquireSession(1, PLAYER_A));
        assertTrue(service.tryAcquireSession(1, PLAYER_A));
        assertFalse(service.tryAcquireSession(1, PLAYER_B));
        assertEquals(PLAYER_A, service.getSessionHolder(1));
    }

    @Test
    void releaseSession_onlyRemovesMatchingHolder() {
        service.tryAcquireSession(1, PLAYER_A);

        service.releaseSession(1, PLAYER_B);
        assertEquals(PLAYER_A, service.getSessionHolder(1));

        service.releaseSession(1, PLAYER_A);
        assertNull(service.getSessionHolder(1));
    }

    @Test
    void releaseSessionByPlayerIfIdle_keepsSessionWhileSavePending() {
        service.tryAcquireSession(1, PLAYER_A);

        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(new ItemStack[54]);
        CompletableFuture<Boolean> pending = service.savePage(1, inventory, 0, 54);

        service.releaseSessionByPlayerIfIdle(PLAYER_A);
        assertEquals(PLAYER_A, service.getSessionHolder(1));

        pending.join();
        service.releaseSessionByPlayerIfIdle(PLAYER_A);
        assertNull(service.getSessionHolder(1));
    }

    @Test
    void resolveSlotsAndPages_usePeakLevelFromGuild() {
        Guild guild = new Guild("G", "G", "", PLAYER_A, "Leader");
        guild.setId(1);
        guild.setPeakLevel(1);

        assertEquals(54, service.resolveSlots(guild));
        assertEquals(1, service.resolvePageCount(guild));
    }

    @Test
    void canOpenWarehouse_leaderAlwaysAllowed() {
        Guild guild = guild(1);
        GuildMember leader = member(1, Role.LEADER);

        assertTrue(service.canOpenWarehouse(guild, leader, null));
    }

    @Test
    void canOpenWarehouse_adminBypassesRoleChecks() {
        Player admin = mock(Player.class);
        when(admin.hasPermission("guild.admin")).thenReturn(true);

        assertTrue(service.canOpenWarehouse(null, null, admin));
    }

    @Test
    void canOpenWarehouse_rejectsMismatchedMember() {
        Guild guild = guild(1);
        GuildMember otherGuild = member(2, Role.OFFICER);

        assertFalse(service.canOpenWarehouse(guild, otherGuild, null));
        assertFalse(service.canOpenWarehouse(guild, null, null));
    }

    @Test
    void canOpenWarehouse_officerUsesDbOverride() throws Exception {
        Guild guild = guild(5);
        GuildMember officer = member(5, Role.OFFICER);

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO guild_warehouse_role_perms (guild_id, role, can_open) VALUES (5, 'OFFICER', 0)");
        }

        assertFalse(service.canOpenWarehouse(guild, officer, null));

        service.setRoleOpenPermission(5, Role.OFFICER, true).join();
        assertTrue(service.canOpenWarehouse(guild, officer, null));
    }

    @Test
    void canOpenWarehouse_memberFallsBackToMembershipRules() {
        Guild guild = guild(3);
        GuildMember member = member(3, Role.MEMBER);

        when(membershipRules.roleMatrixCanWarehouse(Role.MEMBER)).thenReturn(false);
        assertFalse(service.canOpenWarehouse(guild, member, null));

        when(membershipRules.roleMatrixCanWarehouse(Role.MEMBER)).thenReturn(true);
        assertTrue(service.canOpenWarehouse(guild, member, null));
    }

    @Test
    void setRoleOpenPermission_rejectsLeaderRole() {
        assertFalse(service.setRoleOpenPermission(1, Role.LEADER, true).join());
    }

    @Test
    void getRoleOpenOverrideSync_returnsNullForLeader() {
        assertNull(service.getRoleOpenOverrideSync(1, Role.LEADER));
    }

    @Test
    void summarizeDiff_unchangedWhenIdentical() {
        ItemStack[] snapshot = {new ItemStack(Material.DIRT, 4)};

        assertEquals("unchanged", GuildWarehouseService.summarizeDiff(snapshot, snapshot, 1));
    }

    @Test
    void summarizeDiff_reportsPutAndTake() {
        ItemStack[] before = {new ItemStack(Material.DIRT, 4), new ItemStack(Material.STONE, 2)};
        ItemStack[] after = {new ItemStack(Material.DIRT, 1), new ItemStack(Material.OAK_LOG, 3)};

        String summary = GuildWarehouseService.summarizeDiff(before, after, 2);

        assertEquals("put=OAK_LOG:3", summary.substring(0, summary.indexOf(';')));
        assertTrue(summary.contains("DIRT:3"));
        assertTrue(summary.contains("STONE:2"));
        assertTrue(summary.startsWith("put=OAK_LOG:3;take="));
    }

    @Test
    void giveOrDrop_addsItemsToPlayerInventory() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.addItem(any())).thenReturn(new HashMap<>());

        GuildWarehouseService.giveOrDrop(player, new ItemStack(Material.EMERALD, 2));

        verify(inventory).addItem(any(ItemStack.class));
    }

    @Test
    void handleClose_writesAccessLogAfterSuccessfulSave() throws Exception {
        enableWarehouseAvailable();
        cfg.set("guild-warehouse.access-log", true);
        service.reload();

        WarehouseChestHolder holder = new WarehouseChestHolder(1, 1, 54, 54);
        ItemStack[] contents = new ItemStack[54];
        holder.captureOpenSnapshot(contents);

        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(contents);
        when(inventory.getSize()).thenReturn(54);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER_A);
        when(player.getName()).thenReturn("Tester");
        when(player.isOnline()).thenReturn(true);
        org.bukkit.inventory.InventoryView view = mock(org.bukkit.inventory.InventoryView.class);
        when(view.getTopInventory()).thenReturn(mock(Inventory.class));
        when(player.getOpenInventory()).thenReturn(view);

        service.handleClose(player, holder, inventory);
        awaitAccessLogRows(1);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT action, details FROM guild_warehouse_access_log WHERE guild_id = ?")) {
            ps.setInt(1, 1);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("SAVE", rs.getString("action"));
                assertEquals("unchanged", rs.getString("details"));
            }
        }
    }

    @Test
    void handleClose_skipsAccessLogWhenDisabled() throws Exception {
        enableWarehouseAvailable();
        cfg.set("guild-warehouse.access-log", false);
        service.reload();

        WarehouseChestHolder holder = new WarehouseChestHolder(2, 1, 54, 54);
        ItemStack[] contents = new ItemStack[54];
        holder.captureOpenSnapshot(contents);

        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(contents);
        when(inventory.getSize()).thenReturn(54);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER_A);
        when(player.getName()).thenReturn("Tester");
        when(player.isOnline()).thenReturn(true);
        org.bukkit.inventory.InventoryView view = mock(org.bukkit.inventory.InventoryView.class);
        when(view.getTopInventory()).thenReturn(mock(Inventory.class));
        when(player.getOpenInventory()).thenReturn(view);

        service.handleClose(player, holder, inventory);
        Thread.sleep(200);
        assertEquals(0, countAccessLogRows());
    }

    @Test
    void openWarehouse_rejectsWhenNbtUnavailable() {
        Player player = player(PLAYER_A);
        stubMessage(player, "warehouse.nbtapi-missing", "NO_NBT", "NO_NBT");

        service.openWarehouse(player, guild(1));

        verify(player).sendMessage(ColorUtils.colorize("NO_NBT"));
    }

    @Test
    void openWarehouse_returnsSilentlyWhenGuildNull() throws Exception {
        Player player = mock(Player.class);
        enableWarehouseAvailable();

        service.openWarehouse(player, null);

        verify(player, never()).sendMessage(anyString());
    }

    @Test
    void openWarehouse_rejectsFrozenGuild() throws Exception {
        Player player = player(PLAYER_A);
        enableWarehouseAvailable();
        Guild guild = guild(1);
        guild.setFrozen(true);
        stubMessage(player, "warehouse.guild-frozen", "FROZEN", "FROZEN");

        service.openWarehouse(player, guild);

        verify(player).sendMessage(ColorUtils.colorize("FROZEN"));
    }

    @Test
    void openWarehouse_rejectsWithoutPermission() throws Exception {
        Player player = player(PLAYER_A);
        enableWarehouseAvailable();
        Guild guild = guild(1);
        when(guildService.getGuildMember(PLAYER_A)).thenReturn(member(1, Role.MEMBER));
        when(membershipRules.roleMatrixCanWarehouse(Role.MEMBER)).thenReturn(false);
        stubMessage(player, "warehouse.no-permission", "NO_PERM", "NO_PERM");

        service.openWarehouse(player, guild);

        verify(player).sendMessage(ColorUtils.colorize("NO_PERM"));
    }

    @Test
    void openWarehouse_rejectsInvalidPage() throws Exception {
        Player player = player(PLAYER_A);
        enableWarehouseAvailable();
        Guild guild = guild(1);
        when(guildService.getGuildMember(PLAYER_A)).thenReturn(member(1, Role.LEADER));
        stubMessage(player, "warehouse.invalid-page",
                "&cInvalid page. Available: &e1-{pages} &7({slots} slots)",
                "BAD_PAGE");

        service.openWarehouse(player, guild, 99);

        verify(player).sendMessage(ColorUtils.colorize("BAD_PAGE"));
    }

    @Test
    void openWarehouse_rejectsWhenSessionHeldByOther() throws Exception {
        Player player = player(PLAYER_A);
        enableWarehouseAvailable();
        Guild guild = guild(1);
        when(guildService.getGuildMember(PLAYER_A)).thenReturn(member(1, Role.LEADER));
        service.tryAcquireSession(1, PLAYER_B);
        stubMessage(player, "warehouse.in-use", "IN_USE", "IN_USE");

        service.openWarehouse(player, guild);

        verify(player).sendMessage(ColorUtils.colorize("IN_USE"));
    }

    private void enableWarehouseAvailable() throws Exception {
        Field field = GuildWarehouseService.class.getDeclaredField("nbtApiAvailable");
        field.setAccessible(true);
        field.setBoolean(service, true);
        assertTrue(service.isAvailable());
    }

    private Player player(UUID uuid) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("Tester");
        when(player.hasPermission("guild.admin")).thenReturn(false);
        return player;
    }

    private void stubMessage(Player player, String key, String defaultMsg, String response) {
        when(languageManager.getCoreMessage(eq(player), eq(key), anyString())).thenReturn(response);
    }

    private void awaitAccessLogRows(int expected) throws Exception {
        for (int i = 0; i < 50; i++) {
            if (countAccessLogRows() >= expected) {
                return;
            }
            Thread.sleep(20);
        }
        assertEquals(expected, countAccessLogRows());
    }

    private int countAccessLogRows() throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM guild_warehouse_access_log");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static Guild guild(int id) {
        Guild guild = new Guild("G" + id, "G" + id, "", PLAYER_A, "Leader");
        guild.setId(id);
        guild.setPeakLevel(1);
        return guild;
    }

    private static GuildMember member(int guildId, Role role) {
        return new GuildMember(guildId, PLAYER_A, "Tester", role);
    }
}
