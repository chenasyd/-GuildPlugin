package com.guild.commands.handler.admin;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.update.UpdateManager;
import com.guild.update.UpdateManager.VersionInfo;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GuildAdminUpdateHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        UpdateManager updateManager = ctx.plugin().getUpdateManager();

        if (args.length >= 2 && "download".equalsIgnoreCase(args[1])) {
            if (!sender.hasPermission("guild.admin.update")) {
                sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                        "general.no-permission", "&cYou do not have permission!")));
                return;
            }

            sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                    "admin.update.cleanup-notice", "&6[GuildPlugin] &eImportant: After downloading, please ensure ALL old "
                            + "GuildPlugin JARs are deleted from the plugins folder before restarting!")));
            sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                    "admin.update.checking", "&6[GuildPlugin] &eChecking for latest version...")));
            CompatibleScheduler.runTaskAsync(ctx.plugin(), () -> {
                VersionInfo info = updateManager.checkLatestVersion();
                if (info == null) {
                    ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                            "admin.update.fetch-failed", "&c[GuildPlugin] Failed to fetch version info.")));
                    return;
                }

                String localVersion = ctx.plugin().getDescription().getVersion();
                int cmp = UpdateManager.compareVersions(localVersion, info.version);
                if (cmp >= 0) {
                    ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                                    "admin.update.already-latest", "&a[GuildPlugin] You are already running the latest version (v{version}).")
                            .replace("{version}", localVersion)));
                    return;
                }

                updateManager.downloadUpdate(info, sender);

                ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                        "admin.update.manual-cleanup-reminder",
                        "&6[GuildPlugin] &eREMINDER: Check the plugins folder and delete ALL old "
                                + "GuildPlugin JARs (including renamed ones) before restarting the server!")));

                String broadcastMsg = ctx.languageManager().getCoreMessage(
                                "admin.update.download-broadcast", "&6[GuildPlugin] &e{player} downloaded v{version}. "
                                        + "Remove all old GuildPlugin JARs and restart to apply.")
                        .replace("{player}", sender.getName())
                        .replace("{version}", info.version);
                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("guild.admin") && !p.equals(sender)) {
                        CompatibleScheduler.runTask(ctx.plugin(), p, () -> p.sendMessage(ColorUtils.colorize(broadcastMsg)));
                    }
                }
            });
            return;
        }

        sender.sendMessage(ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                "admin.update.checking-dual", "&6[GuildPlugin] &eChecking for updates from GitHub & Modrinth...")));
        CompatibleScheduler.runTaskAsync(ctx.plugin(), () -> {
            VersionInfo info = updateManager.checkLatestVersion();
            if (info == null) {
                ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                        "admin.update.unreachable", "&c[GuildPlugin] Unable to check for updates. Both GitHub and Modrinth are unreachable.")));
                return;
            }

            String localVersion = ctx.plugin().getDescription().getVersion();
            int cmp = UpdateManager.compareVersions(localVersion, info.version);

            ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                    "admin.update.header", "&6======== GuildPlugin Update ========")));
            ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                    "admin.update.source", "&eSource: &f{source}").replace("{source}", info.source)));
            ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                    "admin.update.current", "&eCurrent: &fv{version}").replace("{version}", localVersion)));

            UpdateManager.PluginVersion localParsed = UpdateManager.parseVersion(localVersion);
            if (localParsed == null || !localParsed.official) {
                ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                                "admin.update.third-party-warning",
                                "&c[GuildPlugin] Current version \"{version}\" differs from official naming convention. "
                                        + "This may be a third-party fork not officially maintained.")
                        .replace("{version}", localVersion)));
            }

            ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                            cmp < 0 ? "admin.update.latest" : "admin.update.latest-up-to-date",
                            "&eLatest: &fv{version}").replace("{version}", info.version)));

            if (cmp < 0) {
                if (!info.changelog.isEmpty()) {
                    ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                            "admin.update.changelog-title", "&eChangelog:")));
                    for (String line : info.changelog.split("\n")) {
                        ctx.sendMessage(sender, ColorUtils.colorize("&7  " + line));
                    }
                }
                ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                        "admin.update.usage-download", "&eUsage: &f/guildadmin update download &7to download the update")));
            } else {
                ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                        "admin.update.up-to-date", "&aYou are running the latest version.")));
            }
            ctx.sendMessage(sender, ColorUtils.colorize(ctx.languageManager().getCoreMessage(
                    "admin.update.footer", "&6====================================")));
        });
    }
}
