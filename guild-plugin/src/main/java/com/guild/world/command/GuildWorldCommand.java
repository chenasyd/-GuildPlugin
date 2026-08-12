package com.guild.world.command;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.world.GuildWorldService;
import com.guild.world.model.GuildWorld;
import com.guild.world.model.WorldStatus;
import com.guild.world.model.WorldType;
import com.guild.world.preset.PresetService;
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
    private static final String PREFIX = "&6[GuildWorld] &r";

    private final GuildPlugin plugin;
    private final GuildWorldService worldService;

    public GuildWorldCommand(GuildPlugin plugin, GuildWorldService worldService) {
        this.plugin = plugin;
        this.worldService = worldService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ColorUtils.colorize(PREFIX + "&c您没有权限执行此操作！"));
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
            sendMessage(sender, PREFIX + "&c" + worldService.unsupportedMessage());
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
            default -> sender.sendMessage(ColorUtils.colorize(
                    PREFIX + "&c未知子命令！使用 /guildworld help 查看帮助。"));
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
            sendMessage(sender, PREFIX + "&c用法: /guildworld create <名称> "
                    + "[--type battle|edit|template] [--preset <预设>] [--guild <工会ID>]");
            return;
        }
        String name = args[1];
        WorldType type = parseType(flagValue(args, 2, "--type"));
        String preset = flagValue(args, 2, "--preset");
        String guildId = flagValue(args, 2, "--guild");

        sendMessage(sender, PREFIX + "&e正在创建虚空世界 &f" + name + " &e...");
        CompletableFuture<?> create;
        if (preset != null && worldService.getPresets().hasSchematicFile(preset)) {
            create = worldService.createWorldFromPreset(name, preset);
        } else {
            create = worldService.createVoidWorld(name, type, preset, guildId, null);
        }
        create.thenAccept(gwObj -> {
            GuildWorld gw = (GuildWorld) gwObj;
            sendMessage(sender, PREFIX + "&a虚空世界 &f" + gw.getWorldName()
                    + " &a创建成功！类型: &f" + gw.getType()
                    + "&a，状态: &f" + gw.getStatus()
                    + (preset != null ? "&a，预设: &f" + preset : ""));
        }).exceptionally(ex -> {
            sendMessage(sender, PREFIX + "&c创建失败: " + rootMessage(ex));
            return null;
        });
    }

    private void handleList(CommandSender sender) {
        Collection<GuildWorld> worlds = worldService.getWorlds();
        sendMessage(sender, "&6========== 受管世界列表 (" + worlds.size() + ") ==========");
        if (worlds.isEmpty()) {
            sendMessage(sender, "&7暂无受管世界，使用 &e/guildworld create <名称> &7创建。");
            return;
        }
        for (GuildWorld gw : worlds) {
            boolean loaded = Bukkit.getWorld(gw.getWorldName()) != null;
            String loadedText = loaded ? "&a已加载" : "&7未加载";
            sendMessage(sender, String.format(
                    "&e%s &7| &f%s &7| &f%s &7| %s &7| 预设: &f%s",
                    gw.getWorldName(), gw.getType(), statusText(gw.getStatus()),
                    loadedText, gw.getPresetName().isEmpty() ? "&7-" : gw.getPresetName()));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, PREFIX + "&c用法: /guildworld info <名称>");
            return;
        }
        String name = worldService.buildWorldName(args[1]);
        GuildWorld gw = worldService.getWorld(name);
        if (gw == null) {
            sendMessage(sender, PREFIX + "&c世界 &f" + name + " &c不在受管列表中！");
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sendMessage(sender, "&6========== 世界信息: " + gw.getWorldName() + " ==========");
        sendMessage(sender, "&e类型: &f" + gw.getType());
        sendMessage(sender, "&e状态: &f" + statusText(gw.getStatus()));
        sendMessage(sender, "&e预设: &f" + (gw.getPresetName().isEmpty() ? "-" : gw.getPresetName()));
        sendMessage(sender, "&e出生点: &f" + (gw.getSpawn().isEmpty() ? "-" : gw.getSpawn()));
        sendMessage(sender, "&e关联工会: &f" + (gw.getOwnerGuildId().isEmpty() ? "-" : gw.getOwnerGuildId()));
        sendMessage(sender, "&e创建时间: &f" + fmt.format(new Date(gw.getCreatedAt())));
        sendMessage(sender, "&e最近活动: &f" + fmt.format(new Date(gw.getLastActiveAt())));
        sendMessage(sender, "&e加载状态: &f" + (Bukkit.getWorld(name) != null ? "&a已加载" : "&7未加载"));
        File worldDir = WorldFiles.resolveWorldDirectory(name);
        boolean exists = WorldFiles.worldDirectoryExists(name);
        sendMessage(sender, "&e文件夹: &f" + (exists ? "&a存在" : "&c缺失")
                + " &7(" + worldDir.getPath() + ")");
        if (WorldFiles.usesPaper26Layout()) {
            sendMessage(sender, "&7提示: Paper/Folia 26+ 世界位于 <level>/dimensions/<ns>/<name>/");
        }
    }

    private void handleLoad(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, PREFIX + "&c用法: /guildworld load <名称>");
            return;
        }
        String name = worldService.buildWorldName(args[1]);
        sendMessage(sender, PREFIX + "&e正在加载世界 &f" + name + " &e...");
        worldService.loadWorld(name).thenAccept(gw ->
                sendMessage(sender, PREFIX + "&a世界 &f" + gw.getWorldName() + " &a加载成功！")
        ).exceptionally(ex -> {
            sendMessage(sender, PREFIX + "&c加载失败: " + ex.getMessage());
            return null;
        });
    }

    private void handleUnload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, PREFIX + "&c用法: /guildworld unload <名称>");
            return;
        }
        String name = worldService.buildWorldName(args[1]);
        sendMessage(sender, PREFIX + "&e正在卸载世界 &f" + name + " &e...");
        worldService.unloadWorld(name).thenAccept(v ->
                sendMessage(sender, PREFIX + "&a世界 &f" + name + " &a已卸载。")
        ).exceptionally(ex -> {
            sendMessage(sender, PREFIX + "&c卸载失败: " + ex.getMessage());
            return null;
        });
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, PREFIX + "&c用法: /guildworld delete <名称> [--force]");
            return;
        }
        String name = worldService.buildWorldName(args[1]);
        boolean force = containsFlag(args, "--force");
        sendMessage(sender, PREFIX + "&e正在删除世界 &f" + name + " &e...");
        worldService.deleteWorld(name, force).thenAccept(v ->
                sendMessage(sender, PREFIX + "&a世界 &f" + name + " &a已删除（注册表记录已清除）。")
        ).exceptionally(ex -> {
            sendMessage(sender, PREFIX + "&c删除失败: " + ex.getMessage());
            return null;
        });
    }

    private void handleRestore(CommandSender sender, String[] args) {
        WorldRecoveryService recovery = worldService.getRecovery();

        if (containsFlag(args, "--run")) {
            sendMessage(sender, PREFIX + "&e立即执行恢复自检...");
            worldService.runRecovery();
            sendMessage(sender, PREFIX + "&a恢复自检完成。");
            return;
        }

        if (!recovery.hasRan()) {
            sendMessage(sender, PREFIX + "&e恢复自检尚未执行"
                    + "（服务器启动后由玩家加入或延迟任务自动执行，或使用 /guildworld restore --run 立即执行）。");
            return;
        }

        if (containsFlag(args, "--list")) {
            sendMessage(sender, "&6========== 恢复报告 ==========");
            sendMessage(sender, "&e崩溃检测: &f" + (recovery.isCrashDetected() ? "&c检测到异常" : "&a正常"));
            sendMessage(sender, "&e待处理残留世界: &f" + recovery.getStaleWorlds().size());
            if (recovery.getStaleWorlds().isEmpty()) {
                sendMessage(sender, "&7  (无)");
            } else {
                for (WorldRecoveryService.StaleWorld sw : recovery.getStaleWorlds()) {
                    sendMessage(sender, "  &e- &f" + sw.world().getWorldName()
                            + " &7(" + sw.world().getType() + ") &c" + sw.reason());
                }
            }
            sendMessage(sender, "&e孤儿记录已清除: &f" + recovery.getOrphanRecords().size());
            sendMessage(sender, "&e未注册前缀世界: &f" + recovery.getUnregisteredPrefixWorlds().size());
            sendMessage(sender, "&7处理方式: /guildworld restore --load <名称> | --delete <名称>");
            return;
        }

        String loadName = flagValue(args, 1, "--load");
        if (loadName != null) {
            String name = worldService.buildWorldName(loadName);
            worldService.loadWorld(name).thenAccept(gw ->
                    sendMessage(sender, PREFIX + "&a残留世界 &f" + gw.getWorldName() + " &a已恢复加载。")
            ).exceptionally(ex -> {
                sendMessage(sender, PREFIX + "&c恢复失败: " + ex.getMessage());
                return null;
            });
            return;
        }

        String deleteName = flagValue(args, 1, "--delete");
        if (deleteName != null) {
            String name = worldService.buildWorldName(deleteName);
            worldService.deleteWorld(name, true).thenAccept(v ->
                    sendMessage(sender, PREFIX + "&a残留世界 &f" + name + " &a已清理。")
            ).exceptionally(ex -> {
                sendMessage(sender, PREFIX + "&c清理失败: " + ex.getMessage());
                return null;
            });
            return;
        }

        sendMessage(sender, PREFIX + "&e用法: /guildworld restore [--run|--list|--load <名称>|--delete <名称>]");
    }

    /* ── tp / edit / preset ──────────────────────────────── */

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, PREFIX + "&c传送命令只能由玩家执行。");
            return;
        }
        if (args.length < 2) {
            sendMessage(sender, PREFIX + "&c用法: /guildworld tp <世界名>");
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
                    sendMessage(sender, PREFIX + "&c只能由玩家执行。");
                    return;
                }
                if (args.length < 3) {
                    sendMessage(sender, PREFIX + "&c用法: /guildworld edit tp <世界名>");
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
                    sendMessage(sender, PREFIX + "&c用法见 /guildworld edit help");
                    return;
                }
                teleportPlayer(player, args[1]);
            }
        }
    }

    private void handleEditWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, PREFIX + "&c只能由玩家执行。");
            return;
        }
        SelectionManager.Session session = worldService.getSelections().of(player);
        session.wandMode = true;
        Material wand = worldService.getWandMaterial();
        CompatibleScheduler.runTask(plugin, player, () -> {
            player.getInventory().addItem(new ItemStack(wand, 1));
            sendMessage(player, PREFIX + "&a已给予选区斧 (&f" + wand.name()
                    + "&a)。&e左键=Pos1 &a/ &e右键=Pos2");
        });
    }

    private void handleEditPos(CommandSender sender, boolean pos1) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, PREFIX + "&c只能由玩家执行。");
            return;
        }
        SelectionManager.Session session = worldService.getSelections().of(player);
        Location loc = player.getLocation().getBlock().getLocation();
        if (pos1) {
            session.pos1 = loc;
            sendMessage(player, PREFIX + "&aPos1 = &f" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        } else {
            session.pos2 = loc;
            sendMessage(player, PREFIX + "&aPos2 = &f" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }
    }

    private void handleEditCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, PREFIX + "&c用法: /guildworld edit create <名称> [--preset <预设名>]");
            return;
        }
        String name = args[2];
        String presetFlag = flagValue(args, 3, "--preset");
        final String preset = presetFlag != null ? presetFlag : name;
        sendMessage(sender, PREFIX + "&e正在创建编辑世界 &f" + name + " &e...");
        worldService.createVoidWorld(name, WorldType.EDIT, preset, null, null).thenAccept(gw -> {
            sendMessage(sender, PREFIX + "&a编辑世界 &f" + gw.getWorldName() + " &a已创建。");
            if (sender instanceof Player player) {
                sendMessage(sender, PREFIX + "&e正在传送...");
                worldService.teleportToWorld(player, gw.getWorldName()).thenAccept(ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        sendMessage(player, PREFIX + "&a已进入编辑世界。建造完成后可用 &e/guildworld edit save "
                                + preset + " &a保存预设元数据。");
                    } else {
                        sendMessage(player, PREFIX + "&c传送失败，请手动 /guildworld tp " + gw.getWorldName());
                    }
                }).exceptionally(ex -> {
                    sendMessage(player, PREFIX + "&c传送失败: " + rootMessage(ex));
                    return null;
                });
            }
        }).exceptionally(ex -> {
            sendMessage(sender, PREFIX + "&c创建失败: " + rootMessage(ex));
            return null;
        });
    }

    private void handleEditLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, PREFIX + "&c只能由玩家执行。");
            return;
        }
        sendMessage(sender, PREFIX + "&e正在返回安全世界...");
        worldService.teleportToFallbackWorld(player).thenAccept(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                sendMessage(player, PREFIX + "&a已离开受管世界。");
            } else {
                sendMessage(player, PREFIX + "&c返回失败（回退世界不可用）。");
            }
        }).exceptionally(ex -> {
            sendMessage(player, PREFIX + "&c返回失败: " + rootMessage(ex));
            return null;
        });
    }

    private void handleEditSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, PREFIX + "&c只能由玩家执行。");
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
                sendMessage(player, PREFIX + "&a已记录队伍 A 出生点（保存预设时写入）。");
            }
            case "b", "team-b", "spawn-b" -> {
                session.spawnB = loc.clone();
                sendMessage(player, PREFIX + "&a已记录队伍 B 出生点（保存预设时写入）。");
            }
            case "spec", "spectator", "观众" -> {
                session.spectator = loc.clone();
                sendMessage(player, PREFIX + "&a已记录观众点（保存预设时写入）。");
            }
            case "main", "world" -> {
                if (gw == null) {
                    sendMessage(player, PREFIX + "&c您当前不在受管世界中。");
                    return;
                }
                CompatibleScheduler.runTask(plugin, player, () -> {
                    gw.setSpawnLocation(loc);
                    gw.touch();
                    worldService.getRegistry().save();
                    sendMessage(player, PREFIX + "&a已将世界出生点设为当前坐标。");
                });
            }
            default -> sendMessage(player, PREFIX + "&c用法: /guildworld edit setspawn [a|b|spectator|main]");
        }
    }

    private void handleEditSave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, PREFIX + "&c只能由玩家执行。");
            return;
        }
        if (args.length < 3) {
            sendMessage(sender, PREFIX + "&c用法: /guildworld edit save <预设名>");
            return;
        }
        String presetName = args[2];
        if (!worldService.getSelections().hasCompleteSelection(player)) {
            sendMessage(sender, PREFIX + "&c请先用选区斧设置 Pos1/Pos2（/guildworld edit wand）。");
            return;
        }
        sendMessage(sender, PREFIX + "&e正在导出选区为预设 &f" + presetName + " &e...");
        worldService.savePresetFromSelection(player, presetName).thenAccept(meta ->
                sendMessage(sender, PREFIX + "&a预设 &f" + meta.name()
                        + " &a已保存！尺寸 &f" + meta.sizeX() + "x" + meta.sizeY() + "x" + meta.sizeZ()
                        + " &a，方块实体 &f" + meta.blockEntities()
                        + " &a，schematic=&f" + meta.hasSchematic())
        ).exceptionally(ex -> {
            sendMessage(sender, PREFIX + "&c保存失败: " + rootMessage(ex));
            return null;
        });
    }

    private void handleEditHelp(CommandSender sender) {
        sendMessage(sender, "&6========== GuildWorld 编辑模式 ==========");
        sendMessage(sender, "&e/guildworld edit wand &7- 获取选区斧（左键Pos1/右键Pos2）");
        sendMessage(sender, "&e/guildworld edit pos1|pos2 &7- 以站立点设置角点");
        sendMessage(sender, "&e/guildworld edit <世界> &7- 传送进入指定受管世界");
        sendMessage(sender, "&e/guildworld edit create <名称> &7- 创建 EDIT 世界并进入");
        sendMessage(sender, "&e/guildworld edit setspawn a|b|spectator|main &7- 记录锚点");
        sendMessage(sender, "&e/guildworld edit save <预设名> &7- 导出选区 schematic");
        sendMessage(sender, "&e/guildworld edit leave &7- 返回安全世界");
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
                    sendMessage(sender, PREFIX + "&c用法: /guildworld preset info <预设名>");
                    return;
                }
                handlePresetInfo(sender, args[2]);
            }
            case "delete" -> {
                if (args.length < 3) {
                    sendMessage(sender, PREFIX + "&c用法: /guildworld preset delete <预设名>");
                    return;
                }
                handlePresetDelete(sender, args[2]);
            }
            case "bind" -> {
                if (args.length < 4) {
                    sendMessage(sender, PREFIX + "&c用法: /guildworld preset bind <世界> <预设名>");
                    return;
                }
                handlePresetBind(sender, args[2], args[3]);
            }
            case "paste" -> {
                if (args.length < 3) {
                    sendMessage(sender, PREFIX + "&c用法: /guildworld preset paste <预设名> [世界]");
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
        sendMessage(sender, "&6========== 预设列表 (" + presets.size() + ") ==========");
        if (presets.isEmpty()) {
            sendMessage(sender, "&7暂无预设。在编辑世界中使用 &e/guildworld edit save <名称> &7创建。");
            return;
        }
        for (var meta : presets) {
            sendMessage(sender, String.format("&e%s &7| &f%dx%dx%d &7| schem:%s &7| 来源: &f%s",
                    meta.name(),
                    meta.sizeX(), meta.sizeY(), meta.sizeZ(),
                    meta.hasSchematic() ? "&a是" : "&c否",
                    meta.sourceWorld().isEmpty() ? "-" : meta.sourceWorld()));
        }
    }

    private void handlePresetInfo(CommandSender sender, String presetName) {
        var meta = worldService.getPresets().get(presetName);
        if (meta == null) {
            sendMessage(sender, PREFIX + "&c预设 &f" + presetName + " &c不存在。");
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sendMessage(sender, "&6========== 预设: " + meta.name() + " ==========");
        sendMessage(sender, "&eSchematic: &f" + (meta.hasSchematic() ? "&a有 (.gws)" : "&c无"));
        sendMessage(sender, "&e尺寸: &f" + meta.sizeX() + "x" + meta.sizeY() + "x" + meta.sizeZ());
        sendMessage(sender, "&e方块实体: &f" + meta.blockEntities());
        sendMessage(sender, "&eSpawnA: &f" + (meta.spawnA() == null ? "-" : meta.spawnA().serialize()));
        sendMessage(sender, "&eSpawnB: &f" + (meta.spawnB() == null ? "-" : meta.spawnB().serialize()));
        sendMessage(sender, "&eSpectator: &f" + (meta.spectator() == null ? "-" : meta.spectator().serialize()));
        sendMessage(sender, "&e来源世界: &f" + (meta.sourceWorld().isEmpty() ? "-" : meta.sourceWorld()));
        sendMessage(sender, "&e创建时间: &f" + fmt.format(new Date(meta.createdAt())));
        sendMessage(sender, "&e备注: &f" + (meta.note().isEmpty() ? "-" : meta.note()));
    }

    private void handlePresetPaste(CommandSender sender, String[] args) {
        String presetName = args[2];
        if (!worldService.getPresets().hasSchematicFile(presetName)) {
            sendMessage(sender, PREFIX + "&c预设 &f" + presetName + " &c没有 schematic 文件。");
            return;
        }
        World world;
        Location pasteAt;
        if (args.length >= 4) {
            String wname = worldService.buildWorldName(args[3]);
            world = Bukkit.getWorld(wname);
            if (world == null) {
                sendMessage(sender, PREFIX + "&c世界 &f" + wname + " &c未加载，请先 /guildworld load");
                return;
            }
            pasteAt = new Location(world, 0.5, 64, 0.5);
        } else if (sender instanceof Player player) {
            world = player.getWorld();
            pasteAt = player.getLocation();
        } else {
            sendMessage(sender, PREFIX + "&c控制台请指定世界: /guildworld preset paste <预设> <世界>");
            return;
        }
        sendMessage(sender, PREFIX + "&e正在粘贴预设 &f" + presetName + " &e到 &f" + world.getName() + " ...");
        worldService.pastePreset(world, pasteAt, presetName).thenAccept(v ->
                sendMessage(sender, PREFIX + "&a粘贴完成。")
        ).exceptionally(ex -> {
            sendMessage(sender, PREFIX + "&c粘贴失败: " + rootMessage(ex));
            return null;
        });
    }

    private void handlePresetDelete(CommandSender sender, String presetName) {
        if (!worldService.getPresets().exists(presetName)) {
            sendMessage(sender, PREFIX + "&c预设 &f" + presetName + " &c不存在。");
            return;
        }
        if (worldService.getPresets().delete(presetName)) {
            sendMessage(sender, PREFIX + "&a预设 &f" + presetName + " &a已删除（含 .gws）。");
        } else {
            sendMessage(sender, PREFIX + "&c删除失败。");
        }
    }

    private void handlePresetBind(CommandSender sender, String worldInput, String presetName) {
        String name = worldService.buildWorldName(worldInput);
        GuildWorld gw = worldService.getWorld(name);
        if (gw == null) {
            sendMessage(sender, PREFIX + "&c世界 &f" + name + " &c不在受管列表中。");
            return;
        }
        if (!worldService.getPresets().exists(presetName)) {
            sendMessage(sender, PREFIX + "&c预设 &f" + presetName + " &c不存在。");
            return;
        }
        gw.setPresetName(presetName.toLowerCase());
        gw.touch();
        worldService.getRegistry().save();
        sendMessage(sender, PREFIX + "&a已将世界 &f" + name + " &a绑定预设 &f" + presetName.toLowerCase());
    }

    private void handlePresetHelp(CommandSender sender) {
        sendMessage(sender, "&6========== GuildWorld 预设 ==========");
        sendMessage(sender, "&e/guildworld preset list &7- 列出预设");
        sendMessage(sender, "&e/guildworld preset info <名称> &7- 查看预设");
        sendMessage(sender, "&e/guildworld preset paste <名称> [世界] &7- 粘贴到世界（玩家默认脚下）");
        sendMessage(sender, "&e/guildworld preset delete <名称> &7- 删除预设");
        sendMessage(sender, "&e/guildworld preset bind <世界> <预设> &7- 绑定世界到预设");
        sendMessage(sender, "&e/guildworld create <名> --preset <预设> &7- 建世界并自动粘贴");
    }

    private void teleportPlayer(Player player, String worldInput) {
        String name = worldService.buildWorldName(worldInput);
        sendMessage(player, PREFIX + "&e正在传送到 &f" + name + " &e...");
        worldService.teleportToWorld(player, name).thenAccept(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                sendMessage(player, PREFIX + "&a已传送到 &f" + name);
            } else {
                sendMessage(player, PREFIX + "&c传送失败。");
            }
        }).exceptionally(ex -> {
            sendMessage(player, PREFIX + "&c传送失败: " + rootMessage(ex));
            return null;
        });
    }

    private void handleHelp(CommandSender sender) {
        sendMessage(sender, "&6========== GuildWorld 多世界管理 ==========");
        sendMessage(sender, "&e/guildworld create <名称> [--type battle|edit|template] [--preset <预设>] [--guild <ID>] &7- 创建虚空世界");
        sendMessage(sender, "&e/guildworld list &7- 列出受管世界");
        sendMessage(sender, "&e/guildworld info <名称> &7- 查看世界详情");
        sendMessage(sender, "&e/guildworld load <名称> &7- 加载世界");
        sendMessage(sender, "&e/guildworld unload <名称> &7- 卸载世界");
        sendMessage(sender, "&e/guildworld delete <名称> [--force] &7- 删除世界");
        sendMessage(sender, "&e/guildworld restore [--run|--list|--load <名称>|--delete <名称>] &7- 恢复管理");
        sendMessage(sender, "&e/guildworld tp <名称> &7- 传送进入受管世界（Folia 安全）");
        sendMessage(sender, "&e/guildworld edit ... &7- 编辑模式（create/tp/save/leave）");
        sendMessage(sender, "&e/guildworld preset ... &7- 预设管理（list/info/delete/bind）");
    }

    /* ── 工具 ────────────────────────────────────────────── */

    private static String rootMessage(Throwable ex) {
        Throwable cur = ex;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg == null || msg.isEmpty() ? cur.getClass().getSimpleName() : msg;
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
