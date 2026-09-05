package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildMember.Role;
import org.bukkit.entity.Player;

public class GuildWarehouseHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                var warehouse = ctx.plugin().getGuildWarehouseService();
                if (warehouse == null) {
                    player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.unavailable",
                            "&cGuild warehouse is currently unavailable.")));
                    return;
                }

                Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "general.no-guild",
                            "&cYou have not joined any guild yet!")));
                    return;
                }

                if (args.length == 1) {
                    warehouse.openWarehouse(player, guild, 1);
                    return;
                }

                String sub = args[1].toLowerCase();
                switch (sub) {
                    case "info" -> handleWarehouseInfo(ctx, player, guild, warehouse);
                    case "perm" -> handleWarehousePerm(ctx, player, guild, warehouse, args);
                    default -> {
                        try {
                            int page = Integer.parseInt(sub);
                            warehouse.openWarehouse(player, guild, page);
                        } catch (NumberFormatException e) {
                            player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player,
                                    "warehouse.usage",
                                    "&eUsage: /guild warehouse [page] | /guild warehouse info | /guild warehouse perm <officer|member> <on|off>")));
                        }
                    }
                }

    }

    private void handleWarehouseInfo(GuildCommandContext ctx, Player player, Guild guild, com.guild.warehouse.GuildWarehouseService warehouse) {
                int peak = guild.getPeakLevel();
                int slots = warehouse.resolveSlots(guild);
                int pages = warehouse.resolvePageCount(guild);
                Boolean officerOverride = warehouse.getRoleOpenOverrideSync(guild.getId(), Role.OFFICER);
                Boolean memberOverride = warehouse.getRoleOpenOverrideSync(guild.getId(), Role.MEMBER);
                boolean officer = officerOverride != null
                        ? officerOverride
                        : ctx.plugin().getMembershipRules().roleMatrixCanWarehouse(Role.OFFICER);
                boolean member = memberOverride != null
                        ? memberOverride
                        : ctx.plugin().getMembershipRules().roleMatrixCanWarehouse(Role.MEMBER);

                String on = ctx.languageManager().getCoreMessage(player, "warehouse.state-on", "&aON");
                String off = ctx.languageManager().getCoreMessage(player, "warehouse.state-off", "&cOFF");
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.info-header",
                        "&a=== Guild Warehouse Info ===")));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.info-peak",
                                "&7Peak level: &e{peak}")
                        .replace("{peak}", String.valueOf(peak))));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.info-slots",
                                "&7Slots: &e{slots}")
                        .replace("{slots}", String.valueOf(slots))));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.info-pages",
                                "&7Pages: &e{pages} &7(/guild warehouse <page>)")
                        .replace("{pages}", String.valueOf(pages))));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.info-officer",
                                "&7Officer access: {state}")
                        .replace("{state}", officer ? on : off)));
                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.info-member",
                                "&7Member access: {state}")
                        .replace("{state}", member ? on : off)));
                if (!warehouse.isAvailable()) {
                    player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.nbtapi-missing",
                            "&cGuild warehouse requires the NBTAPI plugin.")));
                }

    }

    private void handleWarehousePerm(GuildCommandContext ctx, Player player, Guild guild,
                                     com.guild.warehouse.GuildWarehouseService warehouse, String[] args) {
                boolean isLeader = ctx.plugin().getMembershipRules().isLeaderOf(player, guild.getId());
                boolean isAdmin = ctx.plugin().getPermissionManager().hasPermission(player, "guild.admin");
                if (!isLeader && !isAdmin) {
                    player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.perm-leader-only",
                            "&cOnly the guild leader can change warehouse access.")));
                    return;
                }
                if (args.length < 4) {
                    player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.perm-usage",
                            "&eUsage: /guild warehouse perm <officer|member> <on|off>")));
                    return;
                }

                Role targetRole;
                if (args[2].equalsIgnoreCase("officer")) {
                    targetRole = Role.OFFICER;
                } else if (args[2].equalsIgnoreCase("member")) {
                    targetRole = Role.MEMBER;
                } else {
                    player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.perm-usage",
                            "&eUsage: /guild warehouse perm <officer|member> <on|off>")));
                    return;
                }

                boolean enable;
                if (args[3].equalsIgnoreCase("on") || args[3].equalsIgnoreCase("true") || args[3].equalsIgnoreCase("1")) {
                    enable = true;
                } else if (args[3].equalsIgnoreCase("off") || args[3].equalsIgnoreCase("false") || args[3].equalsIgnoreCase("0")) {
                    enable = false;
                } else {
                    player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.perm-usage",
                            "&eUsage: /guild warehouse perm <officer|member> <on|off>")));
                    return;
                }

                warehouse.setRoleOpenPermission(guild.getId(), targetRole, enable, player, guild.getName()).thenAccept(ok ->
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            if (!player.isOnline()) {
                                return;
                            }
                            if (ok) {
                                String roleName = targetRole == Role.OFFICER
                                        ? ctx.languageManager().getCoreMessage(player, "warehouse.role-officer", "officer")
                                        : ctx.languageManager().getCoreMessage(player, "warehouse.role-member", "member");
                                String state = enable
                                        ? ctx.languageManager().getCoreMessage(player, "warehouse.state-on", "&aON")
                                        : ctx.languageManager().getCoreMessage(player, "warehouse.state-off", "&cOFF");
                                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.perm-updated",
                                                "&aSet {role} warehouse access to {state}")
                                        .replace("{role}", roleName)
                                        .replace("{state}", state)));
                            } else {
                                player.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(player, "warehouse.perm-failed",
                                        "&cFailed to update warehouse permission.")));
                            }
                        }));

    }

}
