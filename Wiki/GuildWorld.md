# GuildWorld — 多世界 / 预设管理

> 语言: **中文** | [English](./GuildWorld_EN.md)

管理员用虚空世界制作工会战地图，导出为预设（`.gws` + `.yml`），开战时实例化战场。

## 前置

- 权限：`guild.admin.world`
- 命令：`/guildworld`（别名 `gworld` / `工会世界`）
- Folia：仅支持列表内版本（见 `ServerUtils.FOLIA_SUPPORTED_VERSIONS`）；不支持时 create/load/unload/delete 禁用
- Paper/Spigot：始终可用

## 配置（`config.yml` → `world`）

| 项 | 说明 |
|----|------|
| `name-prefix` | 受管世界名前缀，默认 `gw_` |
| `safety.fallback-world` | 卸载/战后回退世界 |
| `edit.wand-material` | 选区斧，默认 `WOODEN_AXE` |
| `schematic.max-volume` | 选区最大体积 |
| `schematic.ignore-air` | 粘贴时跳过空气 |
| `schematic.include-block-entities` | 导出方块实体（失败降级为仅方块） |
| `arena.post-match` | 工会战后策略：`destroy`（默认）\| `reset`（本期等同 destroy） |

## 制作战图流程

```text
/guildworld edit create <名>
/guildworld edit wand          # 左键 Pos1 / 右键 Pos2
# 建造地图…
/guildworld edit setspawn a|b|spectator
/guildworld edit save <preset>
```

也可用 `/guildworld create <arena> --preset <preset>` 直接建 BATTLE 世界并粘贴。

## 子命令摘要

| 命令 | 作用 |
|------|------|
| `create / list / info / load / unload / delete` | 世界生命周期 |
| `tp <世界>` | Folia 安全传送 |
| `restore …` | 崩溃残留恢复 |
| `edit …` | 编辑模式与选区导出 |
| `preset list\|info\|paste\|delete\|bind` | 预设管理 |

## 预设格式

- 目录：`plugins/GuildPlugin/worlds/presets/`
- `<name>.gws`：GZIP+JSON schematic（自研，无 WE/FAWE 依赖）
- `<name>.yml`：元数据、尺寸、锚点（相对 paste 原点的 A/B/观众偏移）

## 多语言

文案键：`lang/core/{zh,en}.yml` 中 `world.*`。  
实现清单见 [World-War-I18N.md](./World-War-I18N.md)。

## API

见 [API-World-War.md](./API-World-War.md)（中文）/ [API-World-War_EN.md](./API-World-War_EN.md)（English）中的 `GuildWorldAPI`。
