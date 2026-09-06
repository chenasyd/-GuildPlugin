package com.guild.module.example.territory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.guild.comm.api.BungeeClientAPI;
import com.guild.comm.api.CommAPI;
import com.guild.comm.bridge.ChannelRouter;
import com.guild.comm.bridge.MessagePacket;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 跨服领地元数据广播：DB 为权威源，Bungee 消息仅加速其它子服的内存缓存更新。
 * <p>
 * 出站 {@link TerritoryMessageTypes#PUSH}，入站 {@link TerritoryMessageTypes#BROADCAST}。
 */
public final class TerritoryCrossServerSync {

    static final String ACTION_CLAIM = "claim";
    static final String ACTION_UNCLAIM = "unclaim";
    static final String ACTION_GUILD_CLEAR = "guild-clear";

    private final TerritoryRepository repository;
    private final TerritorySettings settings;
    private final Logger logger;
    private final Gson gson;
    private final TerritoryEventSender eventSender;

    private volatile boolean suppressPublish;
    private final ChannelRouter.TopicHandler broadcastHandler = this::handleBroadcast;

    public TerritoryCrossServerSync(TerritoryRepository repository,
                                    TerritorySettings settings,
                                    Logger logger) {
        this(repository, settings, logger, BungeeClientAPI::pushTerritoryUpdate);
    }

    TerritoryCrossServerSync(TerritoryRepository repository,
                             TerritorySettings settings,
                             Logger logger,
                             TerritoryEventSender eventSender) {
        this.repository = repository;
        this.settings = settings;
        this.logger = logger;
        this.gson = new Gson();
        this.eventSender = eventSender;
    }

    public void register() {
        if (!isActive()) {
            return;
        }
        CommAPI.on(TerritoryMessageTypes.BROADCAST, broadcastHandler);
        logger.fine("[Territory] Cross-server broadcast listener registered.");
    }

    public void unregister() {
        CommAPI.off(TerritoryMessageTypes.BROADCAST, broadcastHandler);
    }

    public void runWithoutPublishing(Runnable action) {
        boolean previous = suppressPublish;
        suppressPublish = true;
        try {
            action.run();
        } finally {
            suppressPublish = previous;
        }
    }

    public void publishClaim(TerritoryRecord record) {
        if (!shouldPublish() || record == null) {
            return;
        }
        JsonObject payload = basePayload(ACTION_CLAIM, record.getUpdatedAtEpochMs(), record.getServerId());
        payload.add("record", gson.toJsonTree(record));
        sendPayload(payload);
    }

    public void publishUnclaim(int guildId, String serverId, String worldName, long revision) {
        if (!shouldPublish()) {
            return;
        }
        JsonObject payload = basePayload(ACTION_UNCLAIM, revision, serverId);
        payload.addProperty("guildId", guildId);
        payload.addProperty("serverId", serverId);
        payload.addProperty("worldName", worldName);
        sendPayload(payload);
    }

    public void publishGuildClear(int guildId, long revision) {
        if (!shouldPublish()) {
            return;
        }
        JsonObject payload = basePayload(ACTION_GUILD_CLEAR, revision, repository.getLocalServerId());
        payload.addProperty("guildId", guildId);
        sendPayload(payload);
    }

    /** 单测：直接解析并应用 payload，不经过 Bungee。 */
    boolean applyPayload(String payloadJson) {
        return applyPayloadInternal(payloadJson);
    }

    private void handleBroadcast(MessagePacket packet) {
        if (!isActive() || packet == null) {
            return;
        }
        String payload = packet.getPayload();
        if (payload == null || payload.isBlank()) {
            return;
        }
        try {
            if (applyPayloadInternal(payload)) {
                logger.fine("[Territory] Applied cross-server broadcast: "
                        + summarizePayload(payload));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[Territory] Failed to apply cross-server broadcast: " + e.getMessage(), e);
        }
    }

    private boolean applyPayloadInternal(String payloadJson) {
        JsonObject json = JsonParser.parseString(payloadJson).getAsJsonObject();
        String action = readString(json, "action");
        long revision = json.has("revision") ? json.get("revision").getAsLong() : 0L;
        String originServerId = readString(json, "originServerId");

        if (originServerId != null && originServerId.equals(repository.getLocalServerId())) {
            return false;
        }

        if (ACTION_CLAIM.equalsIgnoreCase(action)) {
            if (!json.has("record")) {
                return false;
            }
            TerritoryRecord record = gson.fromJson(json.get("record"), TerritoryRecord.class);
            if (record == null) {
                return false;
            }
            return repository.applyRemoteClaim(record, revision);
        }

        if (ACTION_UNCLAIM.equalsIgnoreCase(action)) {
            if (!json.has("guildId") || !json.has("worldName")) {
                return false;
            }
            int guildId = json.get("guildId").getAsInt();
            String serverId = readString(json, "serverId");
            String worldName = readString(json, "worldName");
            return repository.applyRemoteUnclaim(guildId, serverId, worldName, revision);
        }

        if (ACTION_GUILD_CLEAR.equalsIgnoreCase(action)) {
            if (!json.has("guildId")) {
                return false;
            }
            return repository.applyRemoteGuildClear(json.get("guildId").getAsInt(), revision);
        }

        return false;
    }

    private JsonObject basePayload(String action, long revision, String originServerId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", action);
        payload.addProperty("revision", revision);
        payload.addProperty("originServerId", originServerId != null ? originServerId : "");
        return payload;
    }

    private void sendPayload(JsonObject payload) {
        try {
            eventSender.send(gson.toJson(payload));
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[Territory] Failed to push territory update: " + e.getMessage(), e);
        }
    }

    private boolean shouldPublish() {
        return !suppressPublish && isActive();
    }

    private boolean isActive() {
        return settings.isCrossServerEnabled()
                && settings.isBroadcastEventsEnabled()
                && repository.isCrossServerStorage();
    }

    private static String readString(JsonObject json, String key) {
        if (json == null || key == null || !json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        return json.get(key).getAsString();
    }

    private static String summarizePayload(String payloadJson) {
        try {
            JsonObject json = JsonParser.parseString(payloadJson).getAsJsonObject();
            String action = readString(json, "action");
            if (ACTION_CLAIM.equalsIgnoreCase(action)) {
                return "claim";
            }
            if (ACTION_UNCLAIM.equalsIgnoreCase(action)) {
                return String.format(Locale.ROOT, "unclaim guild=%s world=%s",
                        json.has("guildId") ? json.get("guildId") : "?",
                        readString(json, "worldName"));
            }
            if (ACTION_GUILD_CLEAR.equalsIgnoreCase(action)) {
                return "guild-clear guild=" + (json.has("guildId") ? json.get("guildId") : "?");
            }
            return action != null ? action : "unknown";
        } catch (Exception e) {
            return "parse-error";
        }
    }

    @FunctionalInterface
    interface TerritoryEventSender {
        void send(String payloadJson);
    }
}
