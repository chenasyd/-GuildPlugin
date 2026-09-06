package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** {@code /guild territory admin ...} 管理子命令。 */
public final class TerritoryAdminHandler {

    private final TerritoryModule module;
    private final ModuleContext context;
    private final TerritoryTexts texts;

    public TerritoryAdminHandler(TerritoryModule module, ModuleContext context, TerritoryTexts texts) {
        this.module = module;
        this.context = context;
        this.texts = texts;
    }

    public void handle(CommandSender sender, String[] args) {
        if (!checkAdmin(sender)) {
            return;
        }

        if (args.length == 0) {
            sendAdminHelp(sender);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> handleList(sender);
            case "force-unclaim" -> handleForceUnclaim(sender, args);
            case "repair-sync" -> handleRepairSync(sender, args);
            case "materialize" -> handleMaterialize(sender, args);
            case "help" -> sendAdminHelp(sender);
            default -> sendAdminHelp(sender);
        }
    }

    private void handleList(CommandSender sender) {
        Map<String, TerritoryRecord> all = module.getRepository().viewAll();
        if (all.isEmpty()) {
            texts.send(sender, "module.territory.admin-list-empty", "&7暂无公会领地记录。");
            return;
        }

        texts.send(sender, "module.territory.admin-list-header",
                "&6—— 公会领地列表（{0}）——", all.size());

        List<TerritoryRecord> sorted = new ArrayList<>(all.values());
        sorted.sort(Comparator
                .comparingInt(TerritoryRecord::getGuildId)
                .thenComparing(TerritoryRecord::getWorldName));

        for (TerritoryRecord record : sorted) {
            texts.send(sender, "module.territory.admin-list-line",
                    "&7#{0} &f{1} &7@ &f{2}&8/&f{3} &8({4})",
                    record.getGuildId(),
                    record.getGuildName(),
                    record.getServerId(),
                    record.getWorldName(),
                    record.getRegionId());
        }
    }

    private void handleForceUnclaim(CommandSender sender, String[] args) {
        if (args.length < 2) {
            texts.send(sender, "module.territory.admin-force-unclaim-usage",
                    "&c用法: /guild territory admin force-unclaim <公会名|ID> [世界]");
            return;
        }

        Guild guild = resolveGuild(args[1]);
        if (guild == null) {
            texts.send(sender, "module.territory.admin-guild-not-found",
                    "&c找不到公会: &f{0}", args[1]);
            return;
        }

        int guildId = guild.getId();
        List<TerritoryRecord> targets;
        if (args.length >= 3) {
            String worldName = args[2];
            targets = module.getRepository().get(guildId, worldName)
                    .map(List::of)
                    .orElseGet(ArrayList::new);
            if (targets.isEmpty() && module.getBridge().isOperational()) {
                module.getBridge().findTerritory(guildId, worldName).ifPresent(targets::add);
            }
        } else {
            targets = new ArrayList<>(module.getRepository().findByGuildId(guildId));
        }

        if (targets.isEmpty()) {
            texts.send(sender, "module.territory.admin-no-territory",
                    "&7公会 &f{0} &7在指定范围暂无领地。", guild.getName());
            return;
        }

        Runnable task = () -> {
            int removed = 0;
            for (TerritoryRecord record : targets) {
                if (module.getRepository().isLocalRecord(record)) {
                    if (module.getBridge().unclaimTerritory(guildId, record.getWorldName())) {
                        removed++;
                    }
                } else {
                    long revision = System.currentTimeMillis();
                    module.getRepository().remove(record.getGuildId(), record.getServerId(), record.getWorldName());
                    if (module.getCrossServerSync() != null) {
                        module.getCrossServerSync().publishUnclaim(
                                record.getGuildId(), record.getServerId(), record.getWorldName(), revision);
                    }
                    removed++;
                }
            }
            texts.send(sender, "module.territory.admin-force-unclaim-done",
                    "&a已强制放弃 &f{0} &a个领地（公会 &f{1}&a）。", removed, guild.getName());
        };

        if (sender instanceof Player player) {
            CompatibleScheduler.runTask(context.getPlugin(), player, task);
        } else {
            CompatibleScheduler.runTask(context.getPlugin(), task);
        }
    }

    private void handleRepairSync(CommandSender sender, String[] args) {
        if (!module.getSettings().isMemberSyncEnabled()) {
            texts.send(sender, "module.territory.admin-sync-disabled",
                    "&c成员同步已在配置中关闭（member-sync.enabled=false）。");
            return;
        }
        if (!module.getBridge().isOperational()) {
            texts.send(sender, "module.territory.wg-missing",
                    "&c未检测到 WorldGuard/WorldEdit。");
            return;
        }

        if (args.length >= 2) {
            Guild guild = resolveGuild(args[1]);
            if (guild == null) {
                texts.send(sender, "module.territory.admin-guild-not-found",
                        "&c找不到公会: &f{0}", args[1]);
                return;
            }
            module.getMemberSync().repairSync(guild.getId());
            texts.send(sender, "module.territory.admin-repair-started",
                    "&a已开始修复公会 &f{0} &a的 WG 成员同步。", guild.getName());
            return;
        }

        int guildCount = module.getMemberSync().repairSyncAll();
        texts.send(sender, "module.territory.admin-repair-all-started",
                "&a已开始修复 &f{0} &a个公会的 WG 成员同步。", guildCount);
    }

    private void handleMaterialize(CommandSender sender, String[] args) {
        if (!module.getBridge().isOperational()) {
            texts.send(sender, "module.territory.wg-missing",
                    "&c未检测到 WorldGuard/WorldEdit。");
            return;
        }
        TerritoryMaterializer materializer = module.getMaterializer();
        if (materializer == null) {
            texts.send(sender, "module.territory.admin-materialize-unavailable",
                    "&cMaterializer 未初始化。");
            return;
        }

        boolean forceFailed = false;
        List<String> tokens = new ArrayList<>();
        for (String arg : args) {
            if ("--force".equalsIgnoreCase(arg)) {
                forceFailed = true;
            } else {
                tokens.add(arg);
            }
        }
        if (tokens.size() > 2) {
            texts.send(sender, "module.territory.admin-materialize-usage",
                    "&c用法: /guild territory admin materialize [公会名|ID] [世界] [--force]");
            return;
        }

        Integer guildId = null;
        String worldName = null;
        if (!tokens.isEmpty()) {
            Guild guild = resolveGuild(tokens.get(0));
            if (guild == null) {
                texts.send(sender, "module.territory.admin-guild-not-found",
                        "&c找不到公会: &f{0}", tokens.get(0));
                return;
            }
            guildId = guild.getId();
        }
        if (tokens.size() >= 2) {
            worldName = tokens.get(1);
        }

        boolean finalForceFailed = forceFailed;
        Integer finalGuildId = guildId;
        String finalWorldName = worldName;

        Runnable start = () -> {
            int targetCount = materializer.countAdminTargets(finalGuildId, finalWorldName, finalForceFailed);
            if (targetCount == 0) {
                texts.send(sender, "module.territory.admin-materialize-none",
                        "&7没有需要 materialize 的本机领地记录。");
                return;
            }

            texts.send(sender, "module.territory.admin-materialize-started",
                    "&a已开始 materialize &f{0} &a条本机记录…", targetCount);

            materializer.materializeAdmin(finalGuildId, finalWorldName, finalForceFailed, summary ->
                    texts.send(sender, "module.territory.admin-materialize-done",
                            "&aMaterialize 完成：创建 &f{0}&a，已存在 &f{1}&a，跳过 &f{2}&a，"
                                    + "世界未加载 &f{3}&a，冲突 &f{4}&a，失败 &f{5}&a",
                            summary.getCreated(),
                            summary.getAlreadyPresent(),
                            summary.getSkipped(),
                            summary.getWorldNotLoaded(),
                            summary.getConflict(),
                            summary.getFailed()));
        };

        if (sender instanceof Player player) {
            CompatibleScheduler.runTask(context.getPlugin(), player, start);
        } else {
            CompatibleScheduler.runTask(context.getPlugin(), start);
        }
    }

    private Guild resolveGuild(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            int id = Integer.parseInt(token.trim());
            Guild byId = context.getPlugin().getGuildService().getGuildById(id);
            if (byId != null) {
                return byId;
            }
        } catch (NumberFormatException ignored) {
            // fall through to name lookup
        }
        return context.getPlugin().getGuildService().getGuildByName(token.trim());
    }

    private void sendAdminHelp(CommandSender sender) {
        texts.send(sender, "module.territory.admin-help-header", "&6/guild territory admin");
        texts.send(sender, "module.territory.admin-help-list", "&elist &7- 列出全部领地");
        texts.send(sender, "module.territory.admin-help-force-unclaim",
                "&eforce-unclaim <公会> [世界] &7- 强制放弃领地");
        texts.send(sender, "module.territory.admin-help-repair-sync",
                "&erepair-sync [公会] &7- 修复 WG 成员同步");
        texts.send(sender, "module.territory.admin-help-materialize",
                "&ematerialize [公会] [世界] [--force] &7- 手动补建 WG 区域");
    }

    private boolean checkAdmin(CommandSender sender) {
        if (sender instanceof Player player) {
            if (context.getPlugin().getPermissionManager().hasPermission(player, "guild.territory.admin")) {
                return true;
            }
        } else if (sender.hasPermission("guild.territory.admin")) {
            return true;
        }
        texts.send(sender, "module.territory.no-permission", "&c你没有权限执行此操作。");
        return false;
    }
}
