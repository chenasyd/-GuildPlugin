package com.guild.module.example.territory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.guild.core.database.DatabaseManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 公会领地元数据存储。
 * <p>
 * 跨服模式（{@code cross-server.enabled}）：共享 {@code guild_territories} 表为权威源。
 * 关闭时回退到 {@code territories.json}（单服）。
 */
public final class TerritoryRepository {

    private static final String STORE_FILE = "territories.json";
    private static final String JSON_MIGRATED_FLAG = ".json-migrated";
    private static final Type STORE_TYPE = new TypeToken<Map<String, TerritoryRecord>>() {}.getType();

    private final File dataDir;
    private final File storeFile;
    private final Gson gson;
    private final Logger logger;
    private final TerritoryServerIdentity serverIdentity;
    private final TerritoryDatabaseStore databaseStore;
    private final boolean useDatabase;

    private final Map<String, TerritoryRecord> index = new ConcurrentHashMap<>();

    public TerritoryRepository(File dataDir, Logger logger, DatabaseManager databaseManager,
                               TerritorySettings settings, TerritoryServerIdentity serverIdentity) {
        this.dataDir = dataDir;
        this.storeFile = new File(dataDir, STORE_FILE);
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.serverIdentity = serverIdentity;
        this.useDatabase = settings.isCrossServerEnabled() && databaseManager != null;
        this.databaseStore = useDatabase ? new TerritoryDatabaseStore(databaseManager, logger) : null;
    }

    /** 单测 / 旧构造路径：仅 JSON、无 DB。 */
    TerritoryRepository(File dataDir, Logger logger) {
        this.dataDir = dataDir;
        this.storeFile = new File(dataDir, STORE_FILE);
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.serverIdentity = TerritoryServerIdentity.forTests("local-test");
        this.useDatabase = false;
        this.databaseStore = null;
    }

    public void load() {
        index.clear();
        if (useDatabase) {
            index.putAll(databaseStore.loadAll());
            migrateJsonIfNeeded();
            return;
        }
        loadJsonIntoIndex();
    }

    public void save() {
        if (useDatabase) {
            return;
        }
        saveJsonFromIndex();
    }

    public String getLocalServerId() {
        return serverIdentity.getServerId();
    }

    public boolean isCrossServerStorage() {
        return useDatabase;
    }

    public boolean isLocalRecord(TerritoryRecord record) {
        if (record == null) {
            return false;
        }
        String sid = record.getServerId();
        if (sid == null || sid.isBlank()) {
            return true;
        }
        return serverIdentity.getServerId().equals(sid);
    }

    public Map<String, TerritoryRecord> viewAll() {
        return Collections.unmodifiableMap(index);
    }

    public Map<String, TerritoryRecord> viewLocal() {
        return index.values().stream()
                .filter(this::isLocalRecord)
                .collect(Collectors.toUnmodifiableMap(TerritoryRepository::storageKey, r -> r));
    }

    /** 本机 server-id + 世界。 */
    public Optional<TerritoryRecord> getLocal(int guildId, String worldName) {
        return get(guildId, serverIdentity.getServerId(), worldName);
    }

    /** 兼容旧调用：等同 {@link #getLocal(int, String)}。 */
    public Optional<TerritoryRecord> get(int guildId, String worldName) {
        return getLocal(guildId, worldName);
    }

    public Optional<TerritoryRecord> get(int guildId, String serverId, String worldName) {
        String resolvedServer = serverId != null && !serverId.isBlank()
                ? serverId
                : serverIdentity.getServerId();
        return Optional.ofNullable(index.get(storageKey(guildId, resolvedServer, worldName)));
    }

    public void put(TerritoryRecord record) {
        if (record == null) {
            return;
        }
        TerritoryRecord normalized = normalizeServerId(record);
        index.put(storageKey(normalized), normalized);
        if (useDatabase) {
            databaseStore.upsert(normalized);
        }
    }

    public void removeLocal(int guildId, String worldName) {
        remove(guildId, serverIdentity.getServerId(), worldName);
    }

    /** 兼容旧调用。 */
    public void remove(int guildId, String worldName) {
        removeLocal(guildId, worldName);
    }

    public void remove(int guildId, String serverId, String worldName) {
        String resolvedServer = serverId != null && !serverId.isBlank()
                ? serverId
                : serverIdentity.getServerId();
        index.remove(storageKey(guildId, resolvedServer, worldName));
        if (useDatabase) {
            databaseStore.delete(guildId, resolvedServer, worldName);
        }
    }

    public List<TerritoryRecord> findByGuildId(int guildId) {
        return index.values().stream()
                .filter(record -> record.getGuildId() == guildId)
                .sorted((a, b) -> {
                    int byServer = nullSafe(a.getServerId()).compareTo(nullSafe(b.getServerId()));
                    if (byServer != 0) {
                        return byServer;
                    }
                    return nullSafe(a.getWorldName()).compareTo(nullSafe(b.getWorldName()));
                })
                .collect(Collectors.toList());
    }

    public List<TerritoryRecord> findByGuildIdLocal(int guildId) {
        return findByGuildId(guildId).stream()
                .filter(this::isLocalRecord)
                .collect(Collectors.toList());
    }

    public void removeAllForGuild(int guildId) {
        index.entrySet().removeIf(entry -> entry.getValue().getGuildId() == guildId);
        if (useDatabase) {
            databaseStore.deleteAllForGuild(guildId);
        }
    }

    static String storageKey(TerritoryRecord record) {
        return storageKey(record.getGuildId(), record.getServerId(), record.getWorldName());
    }

    static String storageKey(int guildId, String serverId, String worldName) {
        return guildId + "@" + (serverId != null ? serverId : "") + "@" + worldName;
    }

    /** @deprecated 旧 JSON 键；仅迁移时使用。 */
    @Deprecated
    static String legacyKey(int guildId, String worldName) {
        return guildId + "@" + worldName;
    }

    private TerritoryRecord normalizeServerId(TerritoryRecord record) {
        if (record.getServerId() == null || record.getServerId().isBlank()) {
            return record.withServerId(serverIdentity.getServerId());
        }
        return record;
    }

    private void migrateJsonIfNeeded() {
        File flag = new File(dataDir, JSON_MIGRATED_FLAG);
        if (flag.exists()) {
            return;
        }
        if (!storeFile.exists()) {
            touchFlag(flag);
            return;
        }

        Map<String, TerritoryRecord> legacy = readJsonFile();
        if (legacy.isEmpty()) {
            touchFlag(flag);
            return;
        }

        int migrated = 0;
        for (TerritoryRecord record : legacy.values()) {
            TerritoryRecord withServer = record.withServerId(serverIdentity.getServerId())
                    .withSyncState(TerritorySyncState.MATERIALIZED, System.currentTimeMillis());
            index.put(storageKey(withServer), withServer);
            databaseStore.upsert(withServer);
            migrated++;
        }
        touchFlag(flag);
        logger.info("[Territory] Migrated " + migrated + " record(s) from territories.json to guild_territories.");
    }

    private void loadJsonIntoIndex() {
        Map<String, TerritoryRecord> loaded = readJsonFile();
        if (loaded == null || loaded.isEmpty()) {
            return;
        }
        for (Map.Entry<String, TerritoryRecord> entry : loaded.entrySet()) {
            TerritoryRecord record = normalizeServerId(entry.getValue());
            index.put(storageKey(record), record);
        }
    }

    private Map<String, TerritoryRecord> readJsonFile() {
        if (!storeFile.exists()) {
            return Map.of();
        }
        try (FileReader reader = new FileReader(storeFile)) {
            Map<String, TerritoryRecord> loaded = gson.fromJson(reader, STORE_TYPE);
            return loaded != null ? loaded : Map.of();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load territory store: " + storeFile, e);
            return Map.of();
        }
    }

    private void saveJsonFromIndex() {
        File parent = storeFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            logger.warning("Could not create territory data directory: " + parent);
            return;
        }
        Map<String, TerritoryRecord> snapshot = index.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        e -> legacyKey(e.getValue().getGuildId(), e.getValue().getWorldName()),
                        Map.Entry::getValue));
        try (FileWriter writer = new FileWriter(storeFile)) {
            gson.toJson(snapshot, writer);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save territory store: " + storeFile, e);
        }
    }

    private void touchFlag(File flag) {
        try {
            if (!flag.exists()) {
                File parent = flag.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                flag.createNewFile();
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "Could not write migration flag: " + flag, e);
        }
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}
