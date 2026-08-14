package com.guild.world.command;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import com.guild.core.language.LocalizedException;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ServerUtils;
import com.guild.world.GuildWorldService;
import com.guild.world.model.GuildWorld;
import com.guild.world.model.WorldStatus;
import com.guild.world.model.WorldType;
import com.guild.world.recovery.WorldRecoveryService;
import com.guild.world.selection.SelectionManager;
import com.guild.world.util.WorldFiles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 多世界管理命令：/guildworld
 *
 * <p>权限：{@code guild.admin.world}
 *
 * <pre>
 * /guildworld create &lt;name&gt; [--type battle|edit|template] [--preset &lt;name&gt;] [--guild &lt;id&gt;]
 * /guildworld list
 * /guildworld info &lt;name&gt;
 * /guildworld load &lt;name&gt;
 * /guildworld unload &lt;name&gt;
 * /guildworld delete &lt;name&gt; [--force]
 * /guildworld restore [--list|--load &lt;name&gt;|--delete &lt;name&gt;]
 * /guildworld tp &lt;name&gt;
 * /guildworld edit tp|create|leave|setspawn|save|help ...
 * /guildworld preset list|info|delete|bind|help ...
 * </pre>
 */
public class GuildWorldCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "guild.admin.world";

    private final GuildPlugin plugin;
    private final GuildWorldService worldService;

    public GuildWorldCommand(GuildPlugin plugin, GuildWorldService worldService) {
        this.plugin = plugin;
        this.worldService = worldService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sendPrefixed(sender, "world.no-permission", "&c您没有权限执行此操作！");
            return true;
        }

        if (plugin.getFileLogger() != null) {
            String sourceName = (sender instanceof Player) ? ((Player) sender).getName() : "Console";
            plugin.getFileLogger().logAdmin(sourceName, "/" + command.getName() + " " + String.join(" ", args));
        }

        if (args.length == 0) {
            handleHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        // 变更类操作在 Folia 不支持版本上直接拦截；只读/帮助仍可用
        if (!worldService.isEnabled() && isMutatingSubcommand(sub)) {
            sendPrefixed(sender, "world.disabled.folia-unsupported",
                    "&c当前 Folia 版本 ({version}) 不支持 gworld",
                    "{version}", ServerUtils.getMinecraftVersion());
            return true;
        }

        switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "load" -> handleLoad(sender, args);
            case "unload" -> handleUnload(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "restore" -> handleRestore(sender, args);
            case "tp", "goto", "enter" -> handleTp(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "preset" -> handlePreset(sender, args);
            case "help" -> handleHelp(sender);
            default -> sendPrefixed(sender, "world.unknown-subcommand",
                    "&c未知子命令！使用 /guildworld help 查看帮助。");
        }
        return true;
    }

    private static boolean isMutatingSubcommand(String sub) {
        return switch (sub) {
            case "create", "load", "unload", "delete", "restore", "tp", "goto", "enter", "edit", "preset" -> true;
            default -> false;
        };
    }

    /* ── 子命令实现 ──────────────────────────────────────── */

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendPrefixed(sender, "world.create.usage",
                    "&c用法: /guildworld create <名称> [--type battle|edit|template] [--preset <预设>] [--guild <公会ID>]");
            return;
        }
        String name = args[1];
        WorldType type = parseType(flagValue(args, 2, "--type"));
        String preset = flagValue(args, 2, "--preset");
        String guildId = flagValue(args, 2, "--guild");

        sendPrefixed(sender, "world.create.working", "&e正在创建虚空世界 &f{name} &e...", "{name}", name);
        CompletableFuture<?> create;
        if (preset != null && worldService.getPresets().hasSchematicFile(preset)) {
            create = worldService.createWorldFromPreset(name, preset);
        } else {
            create = worldService.createVoidWorld(name, type, preset, guildId, null);
        }
        create.thenAccept(gwObj -> {
            GuildWorld gw = (GuildWorld) gwObj;
            sendPrefixed(sender, "world.create.success",
                    "&a虚空世界 &f{world} &a创建成功！类型: &f{type}&a，状态: &f{status}&a，预设: &f{preset}",
                    "{world}", gw.getWorldName(),
                    "{type}", String.valueOf(gw.getType()),
                    "{status}", String.valueOf(gw.getStatus()),
                    "{preset}", preset != null ? preset : "-");
        }).exceptionally(ex -> {
            sendPrefixed(sender, "world.create.failed", "&c创建失败: {error}",
                    "{error}", LocalizedException.resolveThrowable(plugin, sender, ex));
            return null;
        });
    }

    private void handleList(CommandSender sender) {
        Collection<GuildWorld> worlds = worldService.getWorlds();
        sendPlain(sender, "world.list.title",
                "&6========== 受管世界列表 ({count}) ==========",
                "{count}", String.valueOf(worlds.size()));
        if (worlds.isEmpty()) {
            sendPlain(sender, "world.list.empty",
                    "&7暂无受管世界，使用 &e/guildworld create <名称> &7创建。");
            return;
        }
        for (GuildWorld gw : worlds) {
            boolean loaded = Bukkit.getWorld(gw.getWorldName()) != null;
            String loadedText = loaded
                    ? t(sender, "world.loaded.yes", "&a已加载")
                    : t(sender, "world.loaded.no", "&7未加载");
            sendPlain(sender, "world.list.entry",
                    "&e{world} &7| &f{type} &7| &f{status} &7| {loaded} &7| 预设: &f{preset}",
                    "{world}", gw.getWorldName(),
                    "{type}", String.valueOf(gw.getType()),
                    "{status}", statusText(gw.getStatus()),
                    "{loaded}", loadedText,
                    "{preset}", gw.getPresetName().isEmpty() ? "&7-" : gw.getPresetName());
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendPrefixed(sender, "world.info.usage", "&c用法: /guildworld info <名称>");
            return;
        }
        String name = worldService.buildWorldName(args[1]);
        GuildWorld gw = worldService.getWorld(name);
        if (gw == null) {
            sendPrefixed(sender, "world.info.not-managed",
                    "&c世界 &f{name} &c不在受管列表中！", "{name}", name);
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sendPlain(sender, "world.info.title",
                "&6========== 世界信息: {world} ==========", "{world}", gw.getWorldName());
        sendPlain(sender, "world.info.type", "&e类型: &f{type}", "{type}", String.valueOf(gw.getType()));
        sendPlain(sender, "world.info.status", "&e状态: &f{status}", "{status}", statusText(gw.getStatus()));
        sendPlain(sender, "world.info.preset", "&e预设: &f{preset}",
                "{preset}", gw.getPresetName().isEmpty() ? "-" : gw.getPresetName());
        sendPlain(sender, "world.info.spawn", "&e出生点: &f{spawn}",
                "{spawn}", gw.getSpawn().isEmpty() ? "-" : gw.getSpawn());
        sendPlain(sender, "world.info.guild", "&e关联公会: &f{guild}",
                "{guild}", gw.getOwnerGuildId().isEmpty() ? "-" : gw.getOwnerGuildId());
        sendPlain(sender, "world.info.created", "&e创建时间: &f{time}",
                "{time}", fmt.format(new Date(gw.getCreatedAt())));
        sendPlain(sender, "world.info.last-active", "&e最近活动: &f{time}",
                "{time}", fmt.format(new Date(gw.getLastActiveAt())));
        String loadedState = Bukkit.getWorld(name) != null
                ? t(sender, "world.loaded.yes", "&a已加载")
                : t(sender, "world.loaded.no", "&7未加载");
        sendPlain(sender, "world.info.load-state", "&e加载状态: &f{loaded}", "{loaded}", loadedState);
        File worldDir = WorldFiles.resolveWorldDirectory(name);
        boolean exists = WorldFiles.worldDirectoryExists(name);
        String existsText = exists
                ? t(sender, "world.info.folder.exists", "&a存在")
                : t(sender, "world.info.folder.missing", "&c缺失");
        sendPlain(sender, "world.info.folder", "&e文件夹: &f{exists} &7({path})",
                "{exists}", existsText, "{path}", worldDir.getPath());
        if (WorldFiles.usesPaper26Layout()) {
            sendPlain(sender, "world.info.paper26-hint",
                    "&7提示: Paper/Folia 26+ 世界位于 <level>/dimensions/<ns>/<name>/");
        }
    }

    private void handleLoad(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendPrefixed(sender, "world.load.usage", "&c用法: /guildworld load <名称>");
            return;
        }
        String name = worldService.buildWorldName(args[1]);
        sendPrefixed(sender, "world.load.working", "&e正在加载世界 &f{name} &e...", "{name}", name);
        worldService.loadWorld(name).thenAccept(gw ->
                sendPrefixed(sender, "world.load.success", "&a世界 &f{world} &a加载成功！",
                        "{world}", gw.getWorldName())
        ).exceptionally(ex -> {
            sendPrefixed(sender, "world.load.failed", "&c加载失败: {error}",
                    "{error}", LocalizedException.resolveThrowable(plugin, sender, ex));
            return null;
        });
    }

    private void handleUnload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendPrefixed(sender, "world.unload.usage", "&c用法: /guildworld unload <名称>");
            return;
        }
        String name = worldService.buildWorldName(args[1]);
        sendPrefixed(sender, "world.unload.working", "&e正在卸载世界 &f{name} &e...", "{name}", name);
        worldService.unloadWorld(name).thenAccept(v ->
                sendPrefixed(sender, "world.unload.success", "&a世界 &f{name} &a已卸载。", "{name}", name)
        ).exceptionally(ex -> {
            sendPrefixed(sender, "world.unload.failed", "&c卸载失败: {error}",
                    "{error}", LocalizedException.resolveThrowable(plugin, sender, ex));
            return null;
        });
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendPrefixed(sender, "world.delete.usage", "&c用法: /guildworld delete <名称> [--force]");
            return;
        }
        String name = worldService.buildWorldName(args[1]);
        boolean force = containsFlag(args, "--force");
        sendPrefixed(sender, "world.delete.working", "&e正在删除世界 &f{name} &e...", "{name}", name);
        worldService.deleteWorld(name, force).thenAccept(v ->
                sendPrefixed(sender, "world.delete.success",
                        "&a世界 &f{name} &a已删除（注册表记录已清除）。", "{name}", name)
        ).exceptionally(ex -> {
            sendPrefixed(sender, "world.delete.failed", "&c删除失败: {error}",
                    "{error}", LocalizedException.resolveThrowable(plugin, sender, ex));
            return null;
        });
    }

    private void handleRestore(CommandSender sender, String[] args) {
        WorldRecoveryService recovery = worldService.getRecovery();

        if (containsFlag(args, "--run")) {
            sendPrefixed(sender, "world.restore.running", "&e立即执行恢复自检...");
            worldService.runRecovery();
            sendPrefixed(sender, "world.restore.done", "&a恢复自检完成。");
            return;
        }

        if (!recovery.hasRan()) {
            sendPrefixed(sender, "world.restore.not-run",
                    "&e恢复自检尚未执行（服务器启动后由玩家加入或延迟任务自动执行，或使用 /guildworld restore --run 立即执行）。");
            return;
        }

        if (containsFlag(args, "--list")) {
            sendPlain(sender, "world.restore.report-title", "&6========== 恢复报告 ==========");
            String crash = recovery.isCrashDetected()
                    ? t(sender, "world.restore.crash.abnormal", "&c检测到异常")
                    : t(sender, "world.restore.crash.ok", "&a正常");
            sendPlain(sender, "world.restore.crash", "&e崩溃检测: &f{result}", "{result}", crash);
            sendPlain(sender, "world.restore.stale-count", "&e待处理残留世界: &f{count}",
                    "{count}", String.valueOf(recovery.getStaleWorlds().size()));
            if (recovery.getStaleWorlds().isEmpty()) {
                sendPlain(sender, "world.restore.none", "&7  (无)");
            } else {
                for (WorldRecoveryService.StaleWorld sw : recovery.getStaleWorlds()) {
                    sendPlain(sender, "world.restore.stale-entry",
                            "  &e- &f{world} &7({type}) &c{reason}",
                            "{world}", sw.world().getWorldName(),
                            "{type}", String.valueOf(sw.world().getType()),
                            "{reason}", sw.reason());
                }
            }
            sendPlain(sender, "world.restore.orphans", "&e孤儿记录已清除: &f{count}",
                    "{count}", String.valueOf(recovery.getOrphanRecords().size()));
            sendPlain(sender, "world.restore.unregistered", "&e未注册前缀世界: &f{count}",
                    "{count}", String.valueOf(recovery.getUnregisteredPrefixWorlds().size()));
            sendPlain(sender, "world.restore.howto",
                    "&7处理方式: /guildworld restore --load <名称> | --delete <名称>");
            return;
        }

        String loadName = flagValue(args, 1, "--load");
        if (loadName != null) {
            String name = worldService.buildWorldName(loadName);
            worldService.loadWorld(name).thenAccept(gw ->
                    sendPrefixed(sender, "world.restore.load-ok",
                            "&a残留世界 &f{world} &a已恢复加载。", "{world}", gw.getWorldName())
            ).exceptionally(ex -> {
                sendPrefixed(sender, "world.restore.load-fail", "&c恢复失败: {error}",
                        "{error}", LocalizedException.resolveThrowable(plugin, sender, ex));
                return null;
            });
            return;
        }

        String deleteName = flagValue(args, 1, "--delete");
        if (deleteName != null) {
            String name = worldService.buildWorldName(deleteName);
            worldService.deleteWorld(name, true).thenAccept(v ->
                    sendPrefixed(sender, "world.restore.delete-ok",
                            "&a残留世界 &f{name} &a已清理。", "{name}", name)
            ).exceptionally(ex -> {
                sendPrefixed(sender, "world.restore.delete-fail", "&c清理失败: {error}",
                        "{error}", LocalizedException.resolveThrowable(plugin, sender, ex));
                return null;
            });
            return;
        }

        sendPrefixed(sender, "world.restore.usage",
                "&e用法: /guildworld restore [--run|--list|--load <名称>|--delete <名称>]");
    }

    /* ── tp / edit / preset ──────────────────────────────── */

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, "world.tp.player-only", "&c传送命令只能由玩家执行。");
            return;
        }
        if (args.length < 2) {
            sendPrefixed(sender, "world.tp.usage", "&c用法: /guildworld tp <世界名>");
            return;
        }
        teleportPlayer(player, args[1]);
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            handleEditHelp(sender);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "tp", "enter", "goto" -> {
                if (!(sender instanceof Player player)) {
                    sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
                    return;
                }
                if (args.length < 3) {
                    sendPrefixed(sender, "world.edit.tp.usage", "&c用法: /guildworld edit tp <世界名>");
                    return;
                }
                teleportPlayer(player, args[2]);
            }
            case "create" -> handleEditCreate(sender, args);
            case "leave", "exit" -> handleEditLeave(sender);
            case "wand" -> handleEditWand(sender);
            case "pos1" -> handleEditPos(sender, true);
            case "pos2" -> handleEditPos(sender, false);
            case "setspawn" -> handleEditSetSpawn(sender, args);
            case "save" -> handleEditSave(sender, args);
            case "help" -> handleEditHelp(sender);
            default -> {
                // 兼容：/guildworld edit <世界名> 直接传送
                if (!(sender instanceof Player player)) {
                    sendPrefixed(sender, "world.edit.usage-see-help", "&c用法见 /guildworld edit help");
                    return;
                }
                teleportPlayer(player, args[1]);
            }
        }
    }

    private void handleEditWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
            return;
        }
        SelectionManager.Session session = worldService.getSelections().of(player);
        session.wandMode = true;
        Material wand = worldService.getWandMaterial();
        CompatibleScheduler.runTask(plugin, player, () -> {
            player.getInventory().addItem(new ItemStack(wand, 1));
            sendPrefixed(player, "world.edit.wand.given",
                    "&a已给予选区斧 (&f{material}&a)。&e左键=Pos1 &a/ &e右键=Pos2",
                    "{material}", wand.name());
        });
    }

    private void handleEditPos(CommandSender sender, boolean pos1) {
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
            return;
        }
        SelectionManager.Session session = worldService.getSelections().of(player);
        Location loc = player.getLocation().getBlock().getLocation();
        if (pos1) {
            session.pos1 = loc;
            sendPrefixed(player, "world.edit.pos1", "&aPos1 = &f{x},{y},{z}",
                    "{x}", String.valueOf(loc.getBlockX()),
                    "{y}", String.valueOf(loc.getBlockY()),
                    "{z}", String.valueOf(loc.getBlockZ()));
        } else {
            session.pos2 = loc;
            sendPrefixed(player, "world.edit.pos2", "&aPos2 = &f{x},{y},{z}",
                    "{x}", String.valueOf(loc.getBlockX()),
                    "{y}", String.valueOf(loc.getBlockY()),
                    "{z}", String.valueOf(loc.getBlockZ()));
        }
    }

    private void handleEditCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendPrefixed(sender, "world.edit.create.usage",
                    "&c用法: /guildworld edit create <名称> [--preset <预设名>]");
            return;
        }
        String name = args[2];
        String presetFlag = flagValue(args, 3, "--preset");
        final String preset = presetFlag != null ? presetFlag : name;
        sendPrefixed(sender, "world.edit.create.working",
                "&e正在创建编辑世界 &f{name} &e...", "{name}", name);
        worldService.createVoidWorld(name, WorldType.EDIT, preset, null, null).thenAccept(gw -> {
            sendPrefixed(sender, "world.edit.create.success",
                    "&a编辑世界 &f{world} &a已创建。", "{world}", gw.getWorldName());
            if (sender instanceof Player player) {
                sendPrefixed(sender, "world.edit.teleporting", "&e正在传送...");
                worldService.teleportToWorld(player, gw.getWorldName()).thenAccept(ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        sendPrefixed(player, "world.edit.entered",
                                "&a已进入编辑世界。建造完成后可用 &e/guildworld edit save {preset} &a保存预设元数据。",
                                "{preset}", preset);
                    } else {
                        sendPrefixed(player, "world.tp.manual-hint",
                                "&c传送失败，请手动 /guildworld tp {world}",
                                "{world}", gw.getWorldName());
                    }
                }).exceptionally(ex -> {
                    sendPrefixed(player, "world.tp.failed-error", "&c传送失败: {error}",
                            "{error}", LocalizedException.resolveThrowable(plugin, player, ex));
                    return null;
                });
            }
        }).exceptionally(ex -> {
            sendPrefixed(sender, "world.create.failed", "&c创建失败: {error}",
                    "{error}", LocalizedException.resolveThrowable(plugin, sender, ex));
            return null;
        });
    }

    private void handleEditLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
            return;
        }
        sendPrefixed(sender, "world.edit.leave.working", "&e正在返回安全世界...");
        worldService.teleportToFallbackWorld(player).thenAccept(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                sendPrefixed(player, "world.edit.leave.success", "&a已离开受管世界。");
            } else {
                sendPrefixed(player, "world.edit.leave.failed", "&c返回失败（回退世界不可用）。");
            }
        }).exceptionally(ex -> {
            sendPrefixed(player, "world.edit.leave.failed-error", "&c返回失败: {error}",
                    "{error}", LocalizedException.resolveThrowable(plugin, player, ex));
            return null;
        });
    }

    private void handleEditSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
            return;
        }
        String which = args.length >= 3 ? args[2].toLowerCase() : "main";
        Location loc = player.getLocation();
        SelectionManager.Session session = worldService.getSelections().of(player);
        String worldName = player.getWorld().getName();
        GuildWorld gw = worldService.getWorld(worldName);

        switch (which) {
            case "a", "team-a", "spawn-a" -> {
                session.spawnA = loc.clone();
                sendPrefixed(player, "world.edit.setspawn.a", "&a已记录队伍 A 出生点（保存预设时写入）。");
            }
            case "b", "team-b", "spawn-b" -> {
                session.spawnB = loc.clone();
                sendPrefixed(player, "world.edit.setspawn.b", "&a已记录队伍 B 出生点（保存预设时写入）。");
            }
            case "spec", "spectator", "观众" -> {
                session.spectator = loc.clone();
                sendPrefixed(player, "world.edit.setspawn.spectator", "&a已记录观众点（保存预设时写入）。");
            }
            case "main", "world" -> {
                if (gw == null) {
                    sendPrefixed(player, "world.edit.setspawn.not-managed", "&c您当前不在受管世界中。");
                    return;
                }
                CompatibleScheduler.runTask(plugin, player, () -> {
                    gw.setSpawnLocation(loc);
                    gw.touch();
                    worldService.getRegistry().save();
                    sendPrefixed(player, "world.edit.setspawn.main", "&a已将世界出生点设为当前坐标。");
                });
            }
            default -> sendPrefixed(player, "world.edit.setspawn.usage",
                    "&c用法: /guildworld edit setspawn [a|b|spectator|main]");
        }
    }

    private void handleEditSave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, "world.player-only", "&c只能由玩家执行。");
            return;
        }
        if (args.length < 3) {
            sendPrefixed(sender, "world.edit.save.usage", "&c用法: /guildworld edit save <预设名>");
            return;
        }
        String presetName = args[2];
        if (!worldService.getSelections().hasCompleteSelection(player)) {
            sendPrefixed(sender, "world.edit.save.need-selection",
                    "&c请先用选区斧设置 Pos1/Pos2（/guildworld edit wand）。");
            return;
        }
        sendPrefixed(sender, "world.edit.save.working",
                "&e正在导出选区为预设 &f{preset} &e...", "{preset}", presetName);
        worldService.savePresetFromSelection(player, presetName).thenAccept(meta ->
                sendPrefixed(sender, "world.edit.save.success",
                        "&a预设 &f{preset} &a已保存！尺寸 &f{size} &a，方块实体 &f{tiles}",
                        "{preset}", meta.name(),
                        "{size}", meta.sizeX() + "x" + meta.sizeY() + "x" + meta.sizeZ(),
                        "{tiles}", String.valueOf(meta.blockEntities()))
        ).exceptionally(ex -> {
            sendPrefixed(sender, "world.edit.save.failed", "&c保存失败: {error}",
                    "{error}", LocalizedException.resolveThrowable(plugin, sender, ex));
            return null;
        });
    }

    private void handleEditHelp(CommandSender sender) {
        sendPlain(sender, "world.edit.help-title", "&6========== GuildWorld 编辑模式 ==========");
        sendPlain(sender, "world.edit.help.wand", "&e/guildworld edit wand &7- 获取选区斧（左键Pos1/右键Pos2）");
        sendPlain(sender, "world.edit.help.pos", "&e/guildworld edit pos1|pos2 &7- 以站立点设置角点");
        sendPlain(sender, "world.edit.help.tp", "&e/guildworld edit <世界> &7- 传送进入指定受管世界");
        sendPlain(sender, "world.edit.help.create", "&e/guildworld edit create <名称> &7- 创建 EDIT 世界并进入");
        sendPlain(sender, "world.edit.help.setspawn", "&e/guildworld edit setspawn a|b|spectator|main &7- 记录锚点");
        sendPlain(sender, "world.edit.help.save", "&e/guildworld edit save <预设名> &7- 导出选区 schematic");
        sendPlain(sender, "world.edit.help.leave", "&e/guildworld edit leave &7- 返回安全世界");
    }

    private void handlePreset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            handlePresetHelp(sender);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "list" -> handlePresetList(sender);
            case "info" -> {
                if (args.length < 3) {
                    sendPrefixed(sender, "world.preset.info.usage",
                            "&c用法: /guildworld preset info <预设名>");
                    return;
                }
                handlePresetInfo(sender, args[2]);
            }
            case "delete" -> {
                if (args.length < 3) {
                    sendPrefixed(sender, "world.preset.delete.usage",
                            "&c用法: /guildworld preset delete <预设名>");
                    return;
                }
                handlePresetDelete(sender, args[2]);
            }
            case "bind" -> {
                if (args.length < 4) {
                    sendPrefixed(sender, "world.preset.bind.usage",
                            "&c用法: /guildworld preset bind <世界> <预设名>");
                    return;
                }
                handlePresetBind(sender, args[2], args[3]);
            }
            case "paste" -> {
                if (args.length < 3) {
                    sendPrefixed(sender, "world.preset.paste.usage",
                            "&c用法: /guildworld preset paste <预设名> [世界]");
                    return;
                }
                handlePresetPaste(sender, args);
            }
            case "help" -> handlePresetHelp(sender);
            default -> handlePresetHelp(sender);
        }
    }

    private void handlePresetList(CommandSender sender) {
        var presets = worldService.getPresets().list();
        sendPlain(sender, "world.preset.list.title",
                "&6========== 预设列表 ({count}) ==========",
                "{count}", String.valueOf(presets.size()));
        if (presets.isEmpty()) {
            sendPlain(sender, "world.preset.list.empty",
                    "&7暂无预设。在编辑世界中使用 &e/guildworld edit save <名称> &7创建。");
            return;
        }
        for (var meta : presets) {
            String schem = meta.hasSchematic()
                    ? t(sender, "world.preset.yes", "&a是")
                    : t(sender, "world.preset.no", "&c否");
            sendPlain(sender, "world.preset.list.entry",
                    "&e{name} &7| &f{sx}x{sy}x{sz} &7| schem:{schem} &7| 来源: &f{source}",
                    "{name}", meta.name(),
                    "{sx}", String.valueOf(meta.sizeX()),
                    "{sy}", String.valueOf(meta.sizeY()),
                    "{sz}", String.valueOf(meta.sizeZ()),
                    "{schem}", schem,
                    "{source}", meta.sourceWorld().isEmpty() ? "-" : meta.sourceWorld());
        }
    }

    private void handlePresetInfo(CommandSender sender, String presetName) {
        var meta = worldService.getPresets().get(presetName);
        if (meta == null) {
            sendPrefixed(sender, "world.preset.missing",
                    "&c预设 &f{preset} &c不存在。", "{preset}", presetName);
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sendPlain(sender, "world.preset.info.title",
                "&6========== 预设: {preset} ==========", "{preset}", meta.name());
        String schemValue = meta.hasSchematic()
                ? t(sender, "world.preset.has-gws", "&a有 (.gws)")
                : t(sender, "world.preset.no-gws", "&c无");
        sendPlain(sender, "world.preset.info.schematic", "&eSchematic: &f{value}", "{value}", schemValue);
        sendPlain(sender, "world.preset.info.size", "&e尺寸: &f{size}",
                "{size}", meta.sizeX() + "x" + meta.sizeY() + "x" + meta.sizeZ());
        sendPlain(sender, "world.preset.info.tiles", "&e方块实体: &f{tiles}",
                "{tiles}", String.valueOf(meta.blockEntities()));
        sendPlain(sender, "world.preset.info.spawn-a", "&eSpawnA: &f{spawn}",
                "{spawn}", meta.spawnA() == null ? "-" : meta.spawnA().serialize());
        sendPlain(sender, "world.preset.info.spawn-b", "&eSpawnB: &f{spawn}",
                "{spawn}", meta.spawnB() == null ? "-" : meta.spawnB().serialize());
        sendPlain(sender, "world.preset.info.spectator", "&eSpectator: &f{spawn}",
                "{spawn}", meta.spectator() == null ? "-" : meta.spectator().serialize());
        sendPlain(sender, "world.preset.info.source", "&e来源世界: &f{world}",
                "{world}", meta.sourceWorld().isEmpty() ? "-" : meta.sourceWorld());
        sendPlain(sender, "world.preset.info.created", "&e创建时间: &f{time}",
                "{time}", fmt.format(new Date(meta.createdAt())));
        sendPlain(sender, "world.preset.info.note", "&e备注: &f{note}",
                "{note}", meta.note().isEmpty() ? "-" : meta.note());
    }

    private void handlePresetPaste(CommandSender sender, String[] args) {
        String presetName = args[2];
        if (!worldService.getPresets().hasSchematicFile(presetName)) {
            sendPrefixed(sender, "world.preset.no-schematic",
                    "&c预设 &f{preset} &c没有 schematic 文件。", "{preset}", presetName);
            return;
        }
        World world;
        Location pasteAt;
        if (args.length >= 4) {
            String wname = worldService.buildWorldName(args[3]);
            world = Bukkit.getWorld(wname);
            if (world == null) {
                sendPrefixed(sender, "world.preset.world-unloaded",
                        "&c世界 &f{world} &c未加载，请先 /guildworld load", "{world}", wname);
                return;
            }
            pasteAt = new Location(world, 0.5, 64, 0.5);
        } else if (sender instanceof Player player) {
            world = player.getWorld();
            pasteAt = player.getLocation();
        } else {
            sendPrefixed(sender, "world.preset.paste.console-need-world",
                    "&c控制台请指定世界: /guildworld preset paste <预设> <世界>");
            return;
        }
        sendPrefixed(sender, "world.preset.paste.working",
                "&e正在粘贴预设 &f{preset} &e到 &f{world} ...",
                "{preset}", presetName, "{world}", world.getName());
        worldService.pastePreset(world, pasteAt, presetName).thenAccept(v ->
                sendPrefixed(sender, "world.preset.paste.success", "&a粘贴完成。")
        ).exceptionally(ex -> {
            sendPrefixed(sender, "world.preset.paste.failed", "&c粘贴失败: {error}",
                    "{error}", LocalizedException.resolveThrowable(plugin, sender, ex));
            return null;
        });
    }

    private void handlePresetDelete(CommandSender sender, String presetName) {
        if (!worldService.getPresets().exists(presetName)) {
            sendPrefixed(sender, "world.preset.missing",
                    "&c预设 &f{preset} &c不存在。", "{preset}", presetName);
            return;
        }
        if (worldService.getPresets().delete(presetName)) {
            sendPrefixed(sender, "world.preset.delete.success",
                    "&a预设 &f{preset} &a已删除（含 .gws）。", "{preset}", presetName);
        } else {
            sendPrefixed(sender, "world.preset.delete.failed", "&c删除失败。");
        }
    }

    private void handlePresetBind(CommandSender sender, String worldInput, String presetName) {
        String name = worldService.buildWorldName(worldInput);
        GuildWorld gw = worldService.getWorld(name);
        if (gw == null) {
            sendPrefixed(sender, "world.info.not-managed",
                    "&c世界 &f{name} &c不在受管列表中！", "{name}", name);
            return;
        }
        if (!worldService.getPresets().exists(presetName)) {
            sendPrefixed(sender, "world.preset.missing",
                    "&c预设 &f{preset} &c不存在。", "{preset}", presetName);
            return;
        }
        gw.setPresetName(presetName.toLowerCase());
        gw.touch();
        worldService.getRegistry().save();
        sendPrefixed(sender, "world.preset.bind.success",
                "&a已将世界 &f{world} &a绑定预设 &f{preset}",
                "{world}", name, "{preset}", presetName.toLowerCase());
    }

    private void handlePresetHelp(CommandSender sender) {
        sendPlain(sender, "world.preset.help-title", "&6========== GuildWorld 预设 ==========");
        sendPlain(sender, "world.preset.help.list", "&e/guildworld preset list &7- 列出预设");
        sendPlain(sender, "world.preset.help.info", "&e/guildworld preset info <名称> &7- 查看预设");
        sendPlain(sender, "world.preset.help.paste",
                "&e/guildworld preset paste <名称> [世界] &7- 粘贴到世界（玩家默认脚下）");
        sendPlain(sender, "world.preset.help.delete", "&e/guildworld preset delete <名称> &7- 删除预设");
        sendPlain(sender, "world.preset.help.bind", "&e/guildworld preset bind <世界> <预设> &7- 绑定世界到预设");
        sendPlain(sender, "world.preset.help.create-with",
                "&e/guildworld create <名> --preset <预设> &7- 建世界并自动粘贴");
    }

    private void teleportPlayer(Player player, String worldInput) {
        String name = worldService.buildWorldName(worldInput);
        sendPrefixed(player, "world.tp.working", "&e正在传送到 &f{world} &e...", "{world}", name);
        worldService.teleportToWorld(player, name).thenAccept(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                sendPrefixed(player, "world.tp.success", "&a已传送到 &f{world}", "{world}", name);
            } else {
                sendPrefixed(player, "world.tp.failed", "&c传送失败。");
            }
        }).exceptionally(ex -> {
            sendPrefixed(player, "world.tp.failed-error", "&c传送失败: {error}",
                    "{error}", LocalizedException.resolveThrowable(plugin, player, ex));
            return null;
        });
    }

    private void handleHelp(CommandSender sender) {
        sendPlain(sender, "world.help.title", "&6========== GuildWorld 多世界管理 ==========");
        sendPlain(sender, "world.help.create",
                "&e/guildworld create <名称> [--type battle|edit|template] [--preset <预设>] [--guild <ID>] &7- 创建虚空世界");
        sendPlain(sender, "world.help.list", "&e/guildworld list &7- 列出受管世界");
        sendPlain(sender, "world.help.info", "&e/guildworld info <名称> &7- 查看世界详情");
        sendPlain(sender, "world.help.load", "&e/guildworld load <名称> &7- 加载世界");
        sendPlain(sender, "world.help.unload", "&e/guildworld unload <名称> &7- 卸载世界");
        sendPlain(sender, "world.help.delete", "&e/guildworld delete <名称> [--force] &7- 删除世界");
        sendPlain(sender, "world.help.restore",
                "&e/guildworld restore [--run|--list|--load <名称>|--delete <名称>] &7- 恢复管理");
        sendPlain(sender, "world.help.tp", "&e/guildworld tp <名称> &7- 传送进入受管世界（Folia 安全）");
        sendPlain(sender, "world.help.edit", "&e/guildworld edit ... &7- 编辑模式（create/tp/save/leave）");
        sendPlain(sender, "world.help.preset", "&e/guildworld preset ... &7- 预设管理（list/info/delete/bind）");
    }

    /* ── 工具 ────────────────────────────────────────────── */

    private String prefix(CommandSender sender) {
        return CoreMsg.raw(plugin, sender, "world.prefix", "&6[GuildWorld] &r");
    }

    private String t(CommandSender sender, String path, String def, String... ph) {
        return CoreMsg.raw(plugin, sender, path, def, ph);
    }

    private void sendPrefixed(CommandSender sender, String path, String def, String... ph) {
        sendMessage(sender, prefix(sender) + t(sender, path, def, ph));
    }

    private void sendPlain(CommandSender sender, String path, String def, String... ph) {
        sendMessage(sender, t(sender, path, def, ph));
    }

    private static String flagValue(String[] args, int start, String flag) {
        for (int i = start; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase(flag)) {
                return args[i + 1];
            }
        }
        return null;
    }

    private static boolean containsFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(flag)) {
                return true;
            }
        }
        return false;
    }

    private static WorldType parseType(String value) {
        if (value == null) {
            return WorldType.BATTLE;
        }
        try {
            return WorldType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WorldType.BATTLE;
        }
    }

    private static String statusText(WorldStatus status) {
        return switch (status) {
            case REGISTERED -> "&7REGISTERED";
            case LOADING -> "&eLOADING";
            case READY -> "&aREADY";
            case BUSY -> "&cBUSY";
            case UNLOADING -> "&eUNLOADING";
            case UNLOADED -> "&7UNLOADED";
            case ERROR -> "&4ERROR";
            case STALE -> "&4STALE";
        };
    }

    /** Folia 安全的消息发送（玩家在其区域线程接收）。 */
    private void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            CompatibleScheduler.runTask(plugin, player, () -> player.sendMessage(ColorUtils.colorize(message)));
        } else {
            sender.sendMessage(ColorUtils.colorize(message));
        }
    }

    /* ── Tab 补全 ────────────────────────────────────────── */

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission(PERMISSION)) {
            return completions;
        }
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "list", "info", "load", "unload",
                    "delete", "restore", "tp", "edit", "preset", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "info", "load", "unload", "delete", "tp", "goto", "enter" -> addWorldNames(completions);
                case "restore" -> completions.addAll(Arrays.asList("--run", "--list", "--load", "--delete"));
                case "edit" -> completions.addAll(Arrays.asList(
                        "wand", "pos1", "pos2", "tp", "create", "leave", "setspawn", "save", "help"));
                case "preset" -> completions.addAll(Arrays.asList(
                        "list", "info", "delete", "bind", "paste", "help"));
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "create" -> completions.addAll(Arrays.asList("--type", "--preset", "--guild"));
                case "delete" -> completions.add("--force");
                case "restore" -> {
                    if ("--load".equalsIgnoreCase(args[1]) || "--delete".equalsIgnoreCase(args[1])) {
                        addWorldNames(completions);
                    } else {
                        completions.addAll(Arrays.asList("--force", "--load", "--delete"));
                    }
                }
                case "edit" -> {
                    switch (args[1].toLowerCase()) {
                        case "tp", "enter", "goto" -> addWorldNames(completions);
                        case "save" -> addPresetNames(completions);
                        case "setspawn" -> completions.addAll(Arrays.asList("a", "b", "spectator", "main"));
                        case "create" -> { /* free text name */ }
                    }
                }
                case "preset" -> {
                    switch (args[1].toLowerCase()) {
                        case "info", "delete", "paste" -> addPresetNames(completions);
                        case "bind" -> addWorldNames(completions);
                    }
                }
            }
        } else if (args.length == 4) {
            if ("create".equalsIgnoreCase(args[0]) && "--type".equalsIgnoreCase(args[2])) {
                completions.addAll(Arrays.asList("battle", "edit", "template"));
            } else if ("edit".equalsIgnoreCase(args[0]) && "create".equalsIgnoreCase(args[1])) {
                completions.add("--preset");
            } else if ("preset".equalsIgnoreCase(args[0]) && "bind".equalsIgnoreCase(args[1])) {
                addPresetNames(completions);
            } else if ("preset".equalsIgnoreCase(args[0]) && "paste".equalsIgnoreCase(args[1])) {
                addWorldNames(completions);
            }
        }
        String last = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(last));
        return completions;
    }

    private void addWorldNames(List<String> completions) {
        for (GuildWorld gw : worldService.getWorlds()) {
            completions.add(gw.getWorldName());
        }
    }

    private void addPresetNames(List<String> completions) {
        for (var meta : worldService.getPresets().list()) {
            completions.add(meta.name());
        }
    }
}
