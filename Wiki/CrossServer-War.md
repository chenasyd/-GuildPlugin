# Cross-Server Guild War (P3 Skeleton)

> Protocol draft + Bungee orchestrator skeleton. Full lobby↔battle wiring is not production-complete yet.

## Goals

- Matching / challenge state on the **proxy**
- Arena instances created only on a designated **battle** sub-server (`GuildWorldService` + local `GuildWarService`)
- No realtime entity sync across servers

## Message types (`guild:main`)

| Type | Direction | Purpose |
|------|-----------|---------|
| `war.challenge` | Lobby → Proxy | Start cross-server challenge |
| `war.challenge.notify` | Proxy → Lobby B | Notify defenders |
| `war.accept` / `war.deny` | Lobby → Proxy | Response |
| `war.arena.create` | Proxy → Battle | Create void world + paste preset |
| `war.arena.ready` | Battle → Proxy | Arena ready, include world name / spawns |
| `war.transfer` | Proxy → Lobbies | Instruct player transfer to battle |
| `war.end.snapshot` | Battle → Proxy | End snapshot JSON |
| `war.report.fanout` | Proxy → others | Fan-out report / season updates |

Constants: `com.guild.bungee.war.WarMessageTypes`  
Orchestrator: `com.guild.bungee.war.WarOrchestrator` (default battle server name: `battle`)

## Payload (JSON, common fields)

```json
{
  "crossMatchId": 1,
  "guildAId": 10,
  "guildBId": 20,
  "preset": "arena1",
  "mode": "FIRST_TO_SCORE",
  "targetServer": "lobby-b",
  "battleServer": "battle"
}
```

## Sequence

```text
LobbyA --war.challenge--> Proxy --war.challenge.notify--> LobbyB
LobbyB --war.accept--> Proxy --war.arena.create--> Battle
Battle --war.arena.ready--> Proxy --war.transfer--> LobbyA/B
(players connect to Battle; local GuildWarService runs)
Battle --war.end.snapshot--> Proxy --war.report.fanout--> others
```

## Prerequisites

- Shared MySQL for guild data across lobbies + battle
- Folia battle server must be in supported Folia versions if using Folia
- Configure battle server name via `WarOrchestrator#setBattleServerName` (future: bungee `config.yml`)

## Status

Skeleton only: proxy routes `war.*` messages; sub-servers do not yet emit challenge/accept over the bridge automatically. Local `/guildwar` remains the supported single-server path.
