package com.guild.bungee.war;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.guild.bungee.bridge.CrossServerBridge;
import com.guild.bungee.data.BungeeMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Cross-server guild-war orchestrator (skeleton).
 *
 * <p>Coordinates challenge/accept and routes arena creation to a configured battle server.
 * Full match simulation still runs on the battle sub-server via local {@code GuildWarService}.
 */
public final class WarOrchestrator {

    public record CrossMatch(int id, String lobbyA, String lobbyB, String battleServer,
                             String state, String payloadJson) {
    }

    private final Logger logger;
    private final CrossServerBridge bridge;
    private final AtomicInteger idSeq = new AtomicInteger(1);
    private final Map<Integer, CrossMatch> matches = new ConcurrentHashMap<>();
    private volatile String battleServerName = "battle";

    public WarOrchestrator(Logger logger, CrossServerBridge bridge) {
        this.logger = logger;
        this.bridge = bridge;
    }

    public void setBattleServerName(String battleServerName) {
        if (battleServerName != null && !battleServerName.isBlank()) {
            this.battleServerName = battleServerName;
        }
    }

    public String getBattleServerName() {
        return battleServerName;
    }

    public void handle(BungeeMessage message, ServerInfo source) {
        String type = message.getType();
        if (type == null) {
            return;
        }
        logger.info("[WarOrchestrator] " + type + " from " + source.getName()
                + " (skeleton — routing only)");
        switch (type) {
            case WarMessageTypes.CHALLENGE -> onChallenge(message, source);
            case WarMessageTypes.ACCEPT -> onAccept(message, source);
            case WarMessageTypes.ARENA_READY -> onArenaReady(message, source);
            case WarMessageTypes.END_SNAPSHOT -> onEnd(message, source);
            default -> {
                if (type.startsWith("war.")) {
                    forwardToBattle(message);
                }
            }
        }
    }

    private void onChallenge(BungeeMessage message, ServerInfo source) {
        int id = idSeq.getAndIncrement();
        JsonObject payload = parsePayload(message);
        String targetServer = payload.has("targetServer")
                ? payload.get("targetServer").getAsString() : null;
        payload.addProperty("crossMatchId", id);
        String json = payload.toString();
        CrossMatch match = new CrossMatch(id, source.getName(), targetServer, battleServerName,
                "PENDING", json);
        matches.put(id, match);

        BungeeMessage notify = BungeeMessage.create(WarMessageTypes.CHALLENGE_NOTIFY, "guild-bungee")
                .payload(json)
                .build();
        if (targetServer != null) {
            ServerInfo target = ProxyServer.getInstance().getServerInfo(targetServer);
            if (target != null) {
                bridge.forwardToServerPublic(target, notify);
                return;
            }
        }
        bridge.broadcastToAllExceptPublic(source, notify);
    }

    private void onAccept(BungeeMessage message, ServerInfo source) {
        JsonObject payload = parsePayload(message);
        int crossId = payload.has("crossMatchId") ? payload.get("crossMatchId").getAsInt() : -1;
        CrossMatch match = matches.get(crossId);
        if (match == null) {
            logger.warning("[WarOrchestrator] accept for unknown crossMatchId=" + crossId);
            return;
        }
        payload.addProperty("battleServer", battleServerName);
        String json = payload.toString();
        matches.put(crossId, new CrossMatch(match.id(), match.lobbyA(), source.getName(),
                battleServerName, "CREATING", json));
        BungeeMessage create = BungeeMessage.create(WarMessageTypes.ARENA_CREATE, "guild-bungee")
                .payload(json)
                .build();
        forwardToBattle(create);
    }

    private void onArenaReady(BungeeMessage message, ServerInfo source) {
        JsonObject payload = parsePayload(message);
        int crossId = payload.has("crossMatchId") ? payload.get("crossMatchId").getAsInt() : -1;
        CrossMatch match = matches.get(crossId);
        if (match == null) {
            return;
        }
        String json = payload.toString();
        matches.put(crossId, new CrossMatch(match.id(), match.lobbyA(), match.lobbyB(),
                match.battleServer(), "READY", json));
        BungeeMessage transfer = BungeeMessage.create(WarMessageTypes.TRANSFER, "guild-bungee")
                .payload(json)
                .build();
        ServerInfo a = ProxyServer.getInstance().getServerInfo(match.lobbyA());
        ServerInfo b = match.lobbyB() != null
                ? ProxyServer.getInstance().getServerInfo(match.lobbyB()) : null;
        if (a != null) {
            bridge.forwardToServerPublic(a, transfer);
        }
        if (b != null) {
            bridge.forwardToServerPublic(b, transfer);
        }
    }

    private void onEnd(BungeeMessage message, ServerInfo source) {
        JsonObject payload = parsePayload(message);
        int crossId = payload.has("crossMatchId") ? payload.get("crossMatchId").getAsInt() : -1;
        matches.remove(crossId);
        BungeeMessage fanout = BungeeMessage.create(WarMessageTypes.REPORT_FANOUT, "guild-bungee")
                .payload(payload.toString())
                .build();
        bridge.broadcastToAllExceptPublic(source, fanout);
    }

    private void forwardToBattle(BungeeMessage message) {
        ServerInfo battle = ProxyServer.getInstance().getServerInfo(battleServerName);
        if (battle == null) {
            logger.warning("[WarOrchestrator] Battle server '" + battleServerName + "' not found");
            return;
        }
        bridge.forwardToServerPublic(battle, message);
    }

    private static JsonObject parsePayload(BungeeMessage message) {
        try {
            String raw = message.getPayload();
            if (raw == null || raw.isEmpty()) {
                return new JsonObject();
            }
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public Map<Integer, CrossMatch> getMatches() {
        return Map.copyOf(matches);
    }
}
