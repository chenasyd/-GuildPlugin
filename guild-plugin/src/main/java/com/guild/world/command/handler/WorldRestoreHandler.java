package com.guild.world.command.handler;

import com.guild.world.recovery.WorldRecoveryService;
import org.bukkit.command.CommandSender;

public final class WorldRestoreHandler implements GuildWorldSubCommandHandler {

    @Override
    public void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        WorldRecoveryService recovery = ctx.worldService().getRecovery();

        if (WorldCommandArgs.containsFlag(args, "--run")) {
            ctx.sendPrefixed(sender, "world.restore.running", "&e立即执行恢复自检...");
            ctx.worldService().runRecovery();
            ctx.sendPrefixed(sender, "world.restore.done", "&a恢复自检完成。");
            return;
        }

        if (!recovery.hasRan()) {
            ctx.sendPrefixed(sender, "world.restore.not-run",
                    "&e恢复自检尚未执行（服务器启动后由玩家加入或延迟任务自动执行，或使用 /guildworld restore --run 立即执行）。");
            return;
        }

        if (WorldCommandArgs.containsFlag(args, "--list")) {
            ctx.sendPlain(sender, "world.restore.report-title", "&6========== 恢复报告 ==========");
            String crash = recovery.isCrashDetected()
                    ? ctx.t(sender, "world.restore.crash.abnormal", "&c检测到异常")
                    : ctx.t(sender, "world.restore.crash.ok", "&a正常");
            ctx.sendPlain(sender, "world.restore.crash", "&e崩溃检测: &f{result}", "{result}", crash);
            ctx.sendPlain(sender, "world.restore.stale-count", "&e待处理残留世界: &f{count}",
                    "{count}", String.valueOf(recovery.getStaleWorlds().size()));
            if (recovery.getStaleWorlds().isEmpty()) {
                ctx.sendPlain(sender, "world.restore.none", "&7  (无)");
            } else {
                for (WorldRecoveryService.StaleWorld sw : recovery.getStaleWorlds()) {
                    ctx.sendPlain(sender, "world.restore.stale-entry",
                            "  &e- &f{world} &7({type}) &c{reason}",
                            "{world}", sw.world().getWorldName(),
                            "{type}", String.valueOf(sw.world().getType()),
                            "{reason}", sw.reason());
                }
            }
            ctx.sendPlain(sender, "world.restore.orphans", "&e孤儿记录已清除: &f{count}",
                    "{count}", String.valueOf(recovery.getOrphanRecords().size()));
            ctx.sendPlain(sender, "world.restore.unregistered", "&e未注册前缀世界: &f{count}",
                    "{count}", String.valueOf(recovery.getUnregisteredPrefixWorlds().size()));
            ctx.sendPlain(sender, "world.restore.howto",
                    "&7处理方式: /guildworld restore --load <名称> | --delete <名称>");
            return;
        }

        String loadName = WorldCommandArgs.flagValue(args, 1, "--load");
        if (loadName != null) {
            String name = ctx.worldService().buildWorldName(loadName);
            ctx.worldService().loadWorld(name).thenAccept(gw ->
                    ctx.sendPrefixed(sender, "world.restore.load-ok",
                            "&a残留世界 &f{world} &a已恢复加载。", "{world}", gw.getWorldName())
            ).exceptionally(ex -> {
                ctx.sendPrefixed(sender, "world.restore.load-fail", "&c恢复失败: {error}",
                        "{error}", ctx.resolveError(sender, ex));
                return null;
            });
            return;
        }

        String deleteName = WorldCommandArgs.flagValue(args, 1, "--delete");
        if (deleteName != null) {
            String name = ctx.worldService().buildWorldName(deleteName);
            ctx.worldService().deleteWorld(name, true).thenAccept(v ->
                    ctx.sendPrefixed(sender, "world.restore.delete-ok",
                            "&a残留世界 &f{name} &a已清理。", "{name}", name)
            ).exceptionally(ex -> {
                ctx.sendPrefixed(sender, "world.restore.delete-fail", "&c清理失败: {error}",
                        "{error}", ctx.resolveError(sender, ex));
                return null;
            });
            return;
        }

        ctx.sendPrefixed(sender, "world.restore.usage",
                "&e用法: /guildworld restore [--run|--list|--load <名称>|--delete <名称>]");
    }
}
