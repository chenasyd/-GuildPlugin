package com.guild.war.command.handler;

import com.guild.war.GuildWarService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;

/**
 * accept / deny / join / leave / ready / cancel — 玩家触发、异步回调的统一处理。
 */
public final class WarPlayerActionHandler implements GuildWarSubCommandHandler {

    public enum Action {
        ACCEPT("war.accept.ok", "&a已接受挑战，开始报名", GuildWarService::accept),
        DENY("war.deny.ok", "&e已拒绝挑战", GuildWarService::deny),
        JOIN("war.join.ok", "&a报名成功", GuildWarService::join),
        LEAVE("war.leave.ok", "&e已退出报名", GuildWarService::leave),
        READY("war.ready.ok", "&a已标记准备就绪", GuildWarService::ready),
        CANCEL("war.cancel.ok", "&e已取消", GuildWarService::cancel);

        private final String okPath;
        private final String okDef;
        private final BiFunction<GuildWarService, Player, java.util.concurrent.CompletableFuture<?>> invoke;

        Action(String okPath, String okDef,
               BiFunction<GuildWarService, Player, java.util.concurrent.CompletableFuture<?>> invoke) {
            this.okPath = okPath;
            this.okDef = okDef;
            this.invoke = invoke;
        }
    }

    private final Action action;

    public WarPlayerActionHandler(Action action) {
        this.action = action;
    }

    @Override
    public void handle(GuildWarCommandContext ctx, CommandSender sender, String[] args) {
        ctx.requirePlayer(sender, player ->
                action.invoke.apply(ctx.war(), player)
                        .whenComplete((v, err) -> ctx.reply(player, err, action.okPath, action.okDef)));
    }
}
