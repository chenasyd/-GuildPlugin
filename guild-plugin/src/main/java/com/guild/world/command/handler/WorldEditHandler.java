package com.guild.world.command.handler;

import com.guild.core.utils.CompatibleScheduler;
import com.guild.world.model.GuildWorld;
import com.guild.world.model.WorldType;
import com.guild.world.selection.SelectionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class WorldEditHandler implements GuildWorldSubCommandHandler {

    @Override
    public void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendEditHelp(ctx, sender);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "tp", "enter", "goto" -> {
                if (!(sender instanceof Player player)) {
                    ctx.sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
                    return;
                }
                if (args.length < 3) {
                    ctx.sendPrefixed(sender, "world.edit.tp.usage", "&c用法: /guildworld edit tp <世界名>");
                    return;
                }
                ctx.teleportPlayer(player, args[2]);
            }
            case "create" -> handleEditCreate(ctx, sender, args);
            case "leave", "exit" -> handleEditLeave(ctx, sender);
            case "wand" -> handleEditWand(ctx, sender);
            case "pos1" -> handleEditPos(ctx, sender, true);
            case "pos2" -> handleEditPos(ctx, sender, false);
            case "setspawn" -> handleEditSetSpawn(ctx, sender, args);
            case "save" -> handleEditSave(ctx, sender, args);
            case "help" -> sendEditHelp(ctx, sender);
            default -> {
                if (!(sender instanceof Player player)) {
                    ctx.sendPrefixed(sender, "world.edit.usage-see-help", "&c用法见 /guildworld edit help");
                    return;
                }
                ctx.teleportPlayer(player, args[1]);
            }
        }
    }

    private static void handleEditWand(GuildWorldCommandContext ctx, CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ctx.sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
            return;
        }
        SelectionManager.Session session = ctx.worldService().getSelections().of(player);
        session.wandMode = true;
        Material wand = ctx.worldService().getWandMaterial();
        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
            player.getInventory().addItem(new ItemStack(wand, 1));
            ctx.sendPrefixed(player, "world.edit.wand.given",
                    "&a已给予选区斧 (&f{material}&a)。&e左键=Pos1 &a/ &e右键=Pos2",
                    "{material}", wand.name());
        });
    }

    private static void handleEditPos(GuildWorldCommandContext ctx, CommandSender sender, boolean pos1) {
        if (!(sender instanceof Player player)) {
            ctx.sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
            return;
        }
        SelectionManager.Session session = ctx.worldService().getSelections().of(player);
        Location loc = player.getLocation().getBlock().getLocation();
        if (pos1) {
            session.pos1 = loc;
            ctx.sendPrefixed(player, "world.edit.pos1", "&aPos1 = &f{x},{y},{z}",
                    "{x}", String.valueOf(loc.getBlockX()),
                    "{y}", String.valueOf(loc.getBlockY()),
                    "{z}", String.valueOf(loc.getBlockZ()));
        } else {
            session.pos2 = loc;
            ctx.sendPrefixed(player, "world.edit.pos2", "&aPos2 = &f{x},{y},{z}",
                    "{x}", String.valueOf(loc.getBlockX()),
                    "{y}", String.valueOf(loc.getBlockY()),
                    "{z}", String.valueOf(loc.getBlockZ()));
        }
    }

    private static void handleEditCreate(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 3) {
            ctx.sendPrefixed(sender, "world.edit.create.usage",
                    "&c用法: /guildworld edit create <名称> [--preset <预设名>]");
            return;
        }
        String name = args[2];
        String presetFlag = WorldCommandArgs.flagValue(args, 3, "--preset");
        final String preset = presetFlag != null ? presetFlag : name;
        ctx.sendPrefixed(sender, "world.edit.create.working",
                "&e正在创建编辑世界 &f{name} &e...", "{name}", name);
        ctx.worldService().createVoidWorld(name, WorldType.EDIT, preset, null, null).thenAccept(gw -> {
            ctx.sendPrefixed(sender, "world.edit.create.success",
                    "&a编辑世界 &f{world} &a已创建。", "{world}", gw.getWorldName());
            if (sender instanceof Player player) {
                ctx.sendPrefixed(sender, "world.edit.teleporting", "&e正在传送...");
                ctx.worldService().teleportToWorld(player, gw.getWorldName()).thenAccept(ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        ctx.sendPrefixed(player, "world.edit.entered",
                                "&a已进入编辑世界。建造完成后可用 &e/guildworld edit save {preset} &a保存预设元数据。",
                                "{preset}", preset);
                    } else {
                        ctx.sendPrefixed(player, "world.tp.manual-hint",
                                "&c传送失败，请手动 /guildworld tp {world}",
                                "{world}", gw.getWorldName());
                    }
                }).exceptionally(ex -> {
                    ctx.sendPrefixed(player, "world.tp.failed-error", "&c传送失败: {error}",
                            "{error}", ctx.resolveError(player, ex));
                    return null;
                });
            }
        }).exceptionally(ex -> {
            ctx.sendPrefixed(sender, "world.create.failed", "&c创建失败: {error}",
                    "{error}", ctx.resolveError(sender, ex));
            return null;
        });
    }

    private static void handleEditLeave(GuildWorldCommandContext ctx, CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ctx.sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
            return;
        }
        ctx.sendPrefixed(sender, "world.edit.leave.working", "&e正在返回安全世界...");
        ctx.worldService().teleportToFallbackWorld(player).thenAccept(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                ctx.sendPrefixed(player, "world.edit.leave.success", "&a已离开受管世界。");
            } else {
                ctx.sendPrefixed(player, "world.edit.leave.failed", "&c返回失败（回退世界不可用）。");
            }
        }).exceptionally(ex -> {
            ctx.sendPrefixed(player, "world.edit.leave.failed-error", "&c返回失败: {error}",
                    "{error}", ctx.resolveError(player, ex));
            return null;
        });
    }

    private static void handleEditSetSpawn(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ctx.sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
            return;
        }
        String which = args.length >= 3 ? args[2].toLowerCase() : "main";
        Location loc = player.getLocation();
        SelectionManager.Session session = ctx.worldService().getSelections().of(player);
        String worldName = player.getWorld().getName();
        GuildWorld gw = ctx.worldService().getWorld(worldName);

        switch (which) {
            case "a", "team-a", "spawn-a" -> {
                session.spawnA = loc.clone();
                ctx.sendPrefixed(player, "world.edit.setspawn.a", "&a已记录队伍 A 出生点（保存预设时写入）。");
            }
            case "b", "team-b", "spawn-b" -> {
                session.spawnB = loc.clone();
                ctx.sendPrefixed(player, "world.edit.setspawn.b", "&a已记录队伍 B 出生点（保存预设时写入）。");
            }
            case "spec", "spectator", "观众" -> {
                session.spectator = loc.clone();
                ctx.sendPrefixed(player, "world.edit.setspawn.spectator", "&a已记录观众点（保存预设时写入）。");
            }
            case "main", "world" -> {
                if (gw == null) {
                    ctx.sendPrefixed(player, "world.edit.setspawn.not-managed", "&c您当前不在受管世界中。");
                    return;
                }
                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                    gw.setSpawnLocation(loc);
                    gw.touch();
                    ctx.worldService().getRegistry().save();
                    ctx.sendPrefixed(player, "world.edit.setspawn.main", "&a已将世界出生点设为当前坐标。");
                });
            }
            default -> ctx.sendPrefixed(player, "world.edit.setspawn.usage",
                    "&c用法: /guildworld edit setspawn [a|b|spectator|main]");
        }
    }

    private static void handleEditSave(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ctx.sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
            return;
        }
        if (args.length < 3) {
            ctx.sendPrefixed(sender, "world.edit.save.usage", "&c用法: /guildworld edit save <预设名>");
            return;
        }
        String presetName = args[2];
        if (!ctx.worldService().getSelections().hasCompleteSelection(player)) {
            ctx.sendPrefixed(sender, "world.edit.save.need-selection",
                    "&c请先用选区斧设置 Pos1/Pos2（/guildworld edit wand）。");
            return;
        }
        ctx.sendPrefixed(sender, "world.edit.save.working",
                "&e正在导出选区为预设 &f{preset} &e...", "{preset}", presetName);
        ctx.worldService().savePresetFromSelection(player, presetName).thenAccept(meta ->
                ctx.sendPrefixed(sender, "world.edit.save.success",
                        "&a预设 &f{preset} &a已保存！尺寸 &f{size} &a，方块实体 &f{tiles}",
                        "{preset}", meta.name(),
                        "{size}", meta.sizeX() + "x" + meta.sizeY() + "x" + meta.sizeZ(),
                        "{tiles}", String.valueOf(meta.blockEntities()))
        ).exceptionally(ex -> {
            ctx.sendPrefixed(sender, "world.edit.save.failed", "&c保存失败: {error}",
                    "{error}", ctx.resolveError(sender, ex));
            return null;
        });
    }

    private static void sendEditHelp(GuildWorldCommandContext ctx, CommandSender sender) {
        ctx.sendPlain(sender, "world.edit.help-title", "&6========== GuildWorld 编辑模式 ==========");
        ctx.sendPlain(sender, "world.edit.help.wand", "&e/guildworld edit wand &7- 获取选区斧（左键Pos1/右键Pos2）");
        ctx.sendPlain(sender, "world.edit.help.pos", "&e/guildworld edit pos1|pos2 &7- 以站立点设置角点");
        ctx.sendPlain(sender, "world.edit.help.tp", "&e/guildworld edit <世界> &7- 传送进入指定受管世界");
        ctx.sendPlain(sender, "world.edit.help.create", "&e/guildworld edit create <名称> &7- 创建 EDIT 世界并进入");
        ctx.sendPlain(sender, "world.edit.help.setspawn", "&e/guildworld edit setspawn a|b|spectator|main &7- 记录锚点");
        ctx.sendPlain(sender, "world.edit.help.save", "&e/guildworld edit save <预设名> &7- 导出选区 schematic");
        ctx.sendPlain(sender, "world.edit.help.leave", "&e/guildworld edit leave &7- 返回安全世界");
    }
}
