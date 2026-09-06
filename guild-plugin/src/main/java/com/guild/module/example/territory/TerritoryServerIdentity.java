package com.guild.module.example.territory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 本机领地实例的唯一标识。
 * <p>
 * 首次启动时随机生成并写入 {@code server-id.txt}，重启后复用，避免与子服显示名冲突。
 * 配置 {@code cross-server.server-id} 可强制覆盖。
 */
public final class TerritoryServerIdentity {

    static final String ID_FILE = "server-id.txt";
    private static final String ID_PREFIX = "terr-";
    private static final int RANDOM_LEN = 12;

    private final String serverId;

    private TerritoryServerIdentity(String serverId) {
        this.serverId = serverId;
    }

    /** 单测用固定 server-id。 */
    static TerritoryServerIdentity forTests(String serverId) {
        return new TerritoryServerIdentity(serverId);
    }

    public String getServerId() {
        return serverId;
    }

    public static TerritoryServerIdentity resolve(File dataDir, TerritorySettings settings, Logger logger) {
        if (settings != null) {
            String configured = settings.getConfiguredServerId();
            if (configured != null && !configured.isBlank()) {
                String id = configured.trim();
                logger.info("[Territory] Using configured server-id: " + id);
                return new TerritoryServerIdentity(id);
            }
        }

        File idFile = new File(dataDir, ID_FILE);
        if (idFile.isFile()) {
            try {
                String stored = Files.readString(idFile.toPath(), StandardCharsets.UTF_8).trim();
                if (!stored.isEmpty()) {
                    logger.info("[Territory] Loaded server-id: " + stored);
                    return new TerritoryServerIdentity(stored);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "[Territory] Could not read " + idFile + ", generating new id", e);
            }
        }

        String generated = ID_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, RANDOM_LEN);
        writeIdFile(dataDir, idFile, generated, logger);
        logger.info("[Territory] Generated new server-id: " + generated + " (persisted to server-id.txt)");
        return new TerritoryServerIdentity(generated);
    }

    private static void writeIdFile(File dataDir, File idFile, String serverId, Logger logger) {
        if (dataDir != null && !dataDir.exists() && !dataDir.mkdirs()) {
            logger.warning("[Territory] Could not create data directory: " + dataDir);
        }
        try {
            Files.writeString(idFile.toPath(), serverId + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.log(Level.WARNING, "[Territory] Could not persist server-id to " + idFile, e);
        }
    }
}
