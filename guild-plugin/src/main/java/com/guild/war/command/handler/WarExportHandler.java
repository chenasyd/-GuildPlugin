package com.guild.war.command.handler;

import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.command.CommandSender;

public final class WarExportHandler implements GuildWarSubCommandHandler {

    @Override
    public void handle(GuildWarCommandContext ctx, CommandSender sender, String[] args) {
        if (!sender.hasPermission(GuildWarCommandContext.PERM_ADMIN)) {
            ctx.send(sender, "war.admin-permission", "&c需要 guild.war.admin");
            return;
        }
        if (args.length < 2) {
            ctx.send(sender, "war.export.usage",
                    "&c用法: /guildwar export <reportId> [json|csv]");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            ctx.send(sender, "war.report.bad-id", "&c战报 ID 无效");
            return;
        }
        String format = args.length >= 3 ? args[2] : "json";
        if (!format.equalsIgnoreCase("json") && !format.equalsIgnoreCase("csv")) {
            ctx.send(sender, "war.export.bad-format", "&c格式须为 json 或 csv");
            return;
        }
        var api = ctx.plugin().getGuildWarAPI();
        if (api == null) {
            ctx.send(sender, "war.unavailable", "&c公会战不可用: {reason}", "{reason}", "API");
            return;
        }
        api.exportReport(id, format).whenComplete((path, err) ->
                CompatibleScheduler.runTask(ctx.plugin(), () -> {
                    if (err != null) {
                        Throwable cause = err.getCause() != null ? err.getCause() : err;
                        if (cause instanceof IllegalArgumentException) {
                            ctx.send(sender, "war.report.none", "&7没有找到战报");
                        } else {
                            ctx.send(sender, "war.export.failed",
                                    "&c导出失败: {msg}",
                                    "{msg}", cause.getMessage() != null
                                            ? cause.getMessage() : cause.getClass().getSimpleName());
                        }
                        return;
                    }
                    ctx.send(sender, "war.export.ok",
                            "&a已导出战报 #{id} → &f{path}",
                            "{id}", String.valueOf(id),
                            "{path}", path.toAbsolutePath().toString());
                }));
    }
}
