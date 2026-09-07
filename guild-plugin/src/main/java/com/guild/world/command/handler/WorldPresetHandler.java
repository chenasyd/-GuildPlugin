package com.guild.world.command.handler;

import com.guild.world.model.GuildWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class WorldPresetHandler implements GuildWorldSubCommandHandler {

    @Override
    public void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendPresetHelp(ctx, sender);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "list" -> handlePresetList(ctx, sender);
            case "info" -> {
                if (args.length < 3) {
                    ctx.sendPrefixed(sender, "world.preset.info.usage",
                            "&c用法: /guildworld preset info <预设名>");
                    return;
                }
                handlePresetInfo(ctx, sender, args[2]);
            }
            case "delete" -> {
                if (args.length < 3) {
                    ctx.sendPrefixed(sender, "world.preset.delete.usage",
                            "&c用法: /guildworld preset delete <预设名>");
                    return;
                }
                handlePresetDelete(ctx, sender, args[2]);
            }
            case "bind" -> {
                if (args.length < 4) {
                    ctx.sendPrefixed(sender, "world.preset.bind.usage",
                            "&c用法: /guildworld preset bind <世界> <预设名>");
                    return;
                }
                handlePresetBind(ctx, sender, args[2], args[3]);
            }
            case "paste" -> {
                if (args.length < 3) {
                    ctx.sendPrefixed(sender, "world.preset.paste.usage",
                            "&c用法: /guildworld preset paste <预设名> [世界]");
                    return;
                }
                handlePresetPaste(ctx, sender, args);
            }
            case "help" -> sendPresetHelp(ctx, sender);
            default -> sendPresetHelp(ctx, sender);
        }
    }

    private static void handlePresetList(GuildWorldCommandContext ctx, CommandSender sender) {
        var presets = ctx.worldService().getPresets().list();
        ctx.sendPlain(sender, "world.preset.list.title",
                "&6========== 预设列表 ({count}) ==========",
                "{count}", String.valueOf(presets.size()));
        if (presets.isEmpty()) {
            ctx.sendPlain(sender, "world.preset.list.empty",
                    "&7暂无预设。在编辑世界中使用 &e/guildworld edit save <名称> &7创建。");
            return;
        }
        for (var meta : presets) {
            String schem = meta.hasSchematic()
                    ? ctx.t(sender, "world.preset.yes", "&a是")
                    : ctx.t(sender, "world.preset.no", "&c否");
            ctx.sendPlain(sender, "world.preset.list.entry",
                    "&e{name} &7| &f{sx}x{sy}x{sz} &7| schem:{schem} &7| 来源: &f{source}",
                    "{name}", meta.name(),
                    "{sx}", String.valueOf(meta.sizeX()),
                    "{sy}", String.valueOf(meta.sizeY()),
                    "{sz}", String.valueOf(meta.sizeZ()),
                    "{schem}", schem,
                    "{source}", meta.sourceWorld().isEmpty() ? "-" : meta.sourceWorld());
        }
    }

    private static void handlePresetInfo(GuildWorldCommandContext ctx, CommandSender sender, String presetName) {
        var meta = ctx.worldService().getPresets().get(presetName);
        if (meta == null) {
            ctx.sendPrefixed(sender, "world.preset.missing",
                    "&c预设 &f{preset} &c不存在。", "{preset}", presetName);
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ctx.sendPlain(sender, "world.preset.info.title",
                "&6========== 预设: {preset} ==========", "{preset}", meta.name());
        String schemValue = meta.hasSchematic()
                ? ctx.t(sender, "world.preset.has-gws", "&a有 (.gws)")
                : ctx.t(sender, "world.preset.no-gws", "&c无");
        ctx.sendPlain(sender, "world.preset.info.schematic", "&eSchematic: &f{value}", "{value}", schemValue);
        ctx.sendPlain(sender, "world.preset.info.size", "&e尺寸: &f{size}",
                "{size}", meta.sizeX() + "x" + meta.sizeY() + "x" + meta.sizeZ());
        ctx.sendPlain(sender, "world.preset.info.tiles", "&e方块实体: &f{tiles}",
                "{tiles}", String.valueOf(meta.blockEntities()));
        ctx.sendPlain(sender, "world.preset.info.spawn-a", "&eSpawnA: &f{spawn}",
                "{spawn}", meta.spawnA() == null ? "-" : meta.spawnA().serialize());
        ctx.sendPlain(sender, "world.preset.info.spawn-b", "&eSpawnB: &f{spawn}",
                "{spawn}", meta.spawnB() == null ? "-" : meta.spawnB().serialize());
        ctx.sendPlain(sender, "world.preset.info.spectator", "&eSpectator: &f{spawn}",
                "{spawn}", meta.spectator() == null ? "-" : meta.spectator().serialize());
        ctx.sendPlain(sender, "world.preset.info.source", "&e来源世界: &f{world}",
                "{world}", meta.sourceWorld().isEmpty() ? "-" : meta.sourceWorld());
        ctx.sendPlain(sender, "world.preset.info.created", "&e创建时间: &f{time}",
                "{time}", fmt.format(new Date(meta.createdAt())));
        ctx.sendPlain(sender, "world.preset.info.note", "&e备注: &f{note}",
                "{note}", meta.note().isEmpty() ? "-" : meta.note());
    }

    private static void handlePresetPaste(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        String presetName = args[2];
        if (!ctx.worldService().getPresets().hasSchematicFile(presetName)) {
            ctx.sendPrefixed(sender, "world.preset.no-schematic",
                    "&c预设 &f{preset} &c没有 schematic 文件。", "{preset}", presetName);
            return;
        }
        World world;
        Location pasteAt;
        if (args.length >= 4) {
            String wname = ctx.worldService().buildWorldName(args[3]);
            world = Bukkit.getWorld(wname);
            if (world == null) {
                ctx.sendPrefixed(sender, "world.preset.world-unloaded",
                        "&c世界 &f{world} &c未加载，请先 /guildworld load", "{world}", wname);
                return;
            }
            pasteAt = new Location(world, 0.5, 64, 0.5);
        } else if (sender instanceof Player player) {
            world = player.getWorld();
            pasteAt = player.getLocation();
        } else {
            ctx.sendPrefixed(sender, "world.preset.paste.console-need-world",
                    "&c控制台请指定世界: /guildworld preset paste <预设> <世界>");
            return;
        }
        ctx.sendPrefixed(sender, "world.preset.paste.working",
                "&e正在粘贴预设 &f{preset} &e到 &f{world} ...",
                "{preset}", presetName, "{world}", world.getName());
        ctx.worldService().pastePreset(world, pasteAt, presetName).thenAccept(v ->
                ctx.sendPrefixed(sender, "world.preset.paste.success", "&a粘贴完成。")
        ).exceptionally(ex -> {
            ctx.sendPrefixed(sender, "world.preset.paste.failed", "&c粘贴失败: {error}",
                    "{error}", ctx.resolveError(sender, ex));
            return null;
        });
    }

    private static void handlePresetDelete(GuildWorldCommandContext ctx, CommandSender sender, String presetName) {
        if (!ctx.worldService().getPresets().exists(presetName)) {
            ctx.sendPrefixed(sender, "world.preset.missing",
                    "&c预设 &f{preset} &c不存在。", "{preset}", presetName);
            return;
        }
        if (ctx.worldService().getPresets().delete(presetName)) {
            ctx.sendPrefixed(sender, "world.preset.delete.success",
                    "&a预设 &f{preset} &a已删除（含 .gws）。", "{preset}", presetName);
        } else {
            ctx.sendPrefixed(sender, "world.preset.delete.failed", "&c删除失败。");
        }
    }

    private static void handlePresetBind(GuildWorldCommandContext ctx, CommandSender sender,
                                         String worldInput, String presetName) {
        String name = ctx.worldService().buildWorldName(worldInput);
        GuildWorld gw = ctx.worldService().getWorld(name);
        if (gw == null) {
            ctx.sendPrefixed(sender, "world.info.not-managed",
                    "&c世界 &f{name} &c不在受管列表中！", "{name}", name);
            return;
        }
        if (!ctx.worldService().getPresets().exists(presetName)) {
            ctx.sendPrefixed(sender, "world.preset.missing",
                    "&c预设 &f{preset} &c不存在。", "{preset}", presetName);
            return;
        }
        gw.setPresetName(presetName.toLowerCase());
        gw.touch();
        ctx.worldService().getRegistry().save();
        ctx.sendPrefixed(sender, "world.preset.bind.success",
                "&a已将世界 &f{world} &a绑定预设 &f{preset}",
                "{world}", name, "{preset}", presetName.toLowerCase());
    }

    private static void sendPresetHelp(GuildWorldCommandContext ctx, CommandSender sender) {
        ctx.sendPlain(sender, "world.preset.help-title", "&6========== GuildWorld 预设 ==========");
        ctx.sendPlain(sender, "world.preset.help.list", "&e/guildworld preset list &7- 列出预设");
        ctx.sendPlain(sender, "world.preset.help.info", "&e/guildworld preset info <名称> &7- 查看预设");
        ctx.sendPlain(sender, "world.preset.help.paste",
                "&e/guildworld preset paste <名称> [世界] &7- 粘贴到世界（玩家默认脚下）");
        ctx.sendPlain(sender, "world.preset.help.delete", "&e/guildworld preset delete <名称> &7- 删除预设");
        ctx.sendPlain(sender, "world.preset.help.bind", "&e/guildworld preset bind <世界> <预设> &7- 绑定世界到预设");
        ctx.sendPlain(sender, "world.preset.help.create-with",
                "&e/guildworld create <名> --preset <预设> &7- 建世界并自动粘贴");
    }
}
