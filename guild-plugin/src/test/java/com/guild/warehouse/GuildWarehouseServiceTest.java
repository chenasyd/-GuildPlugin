package com.guild.warehouse;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.guild.GuildPlugin;
import com.guild.core.config.ConfigManager;
import com.guild.core.database.DatabaseManager;
import com.guild.core.permissions.GuildMembershipRules;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildMember.Role;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuildWarehouseServiceTest {

    private static final UUID PLAYER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAYER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private HikariDataSource dataSource;
    private GuildPlugin plugin;
    private GuildMembershipRules membershipRules;
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
        }

        plugin = mock(GuildPlugin.class);
        membershipRules = mock(GuildMembershipRules.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);

        ConfigManager configManager = mock(ConfigManager.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("guild-warehouse.enabled", true);
        cfg.set("guild-warehouse.slots-by-level.1", 54);

        when(plugin.getLogger()).thenReturn(Logger.getLogger("GuildWarehouseServiceTest"));
        when(plugin.getDatabaseManager()).thenReturn(databaseManager);
        when(databaseManager.getConnection()).thenAnswer(inv -> dataSource.getConnection());
        when(databaseManager.getDatabaseType()).thenReturn(DatabaseManager.DatabaseType.MYSQL);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(cfg);
        when(plugin.getMembershipRules()).thenReturn(membershipRules);

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
