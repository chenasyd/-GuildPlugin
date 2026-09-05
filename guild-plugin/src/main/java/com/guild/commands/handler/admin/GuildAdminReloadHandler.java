package com.guild.commands.handler.admin;

import com.guild.core.module.ModuleManager;
import com.guild.core.utils.ColorUtils;
import org.bukkit.command.CommandSender;

public class GuildAdminReloadHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        try {
            ctx.plugin().reloadRuntimeConfiguration();

            ctx.plugin().getLanguageManager().reloadLanguagesAsync(() -> {
                try {
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (ctx.plugin().getGuiManager().hasOpenGUI(p)) {
                            ctx.plugin().getGuiManager().refreshGUI(p);
                        }
                    }
                } catch (Exception ignored) {
                }

                String success = ctx.languageManager().getCoreMessage(
                        "admin.reload.success", "&aConfiguration has been reloaded!");
                ctx.sendMessage(sender, ColorUtils.colorize(success));
            });

            ctx.plugin().getLanguageManager().reloadModuleLanguagesAsync(() -> {
                try {
                    var lm = ctx.plugin().getLanguageManager();
                    for (String dir : lm.getKnownModuleLangDirs()) {
                        try {
                            lm.loadModuleLanguageResourcesForModule(dir);
                        } catch (Exception ignored) {
                        }
                    }
                    ModuleManager mm = ctx.plugin().getModuleManager();
                    var api = mm.getSharedApi();
                    for (String moduleId : mm.getRegistry().getModuleIds()) {
                        try {
                            api.loadModuleLanguageResource(moduleId, null);
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception ignored) {
                }
                try {
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (ctx.plugin().getGuiManager().hasOpenGUI(p)) {
                            ctx.plugin().getGuiManager().refreshGUI(p);
                        }
                    }
                } catch (Exception ignored) {
                }
            });
        } catch (Exception e) {
            String failed = ctx.languageManager().getCoreMessage("admin.reload.failed",
                            "&cFailed to reload configuration: {error}")
                    .replace("{error}", e.getMessage());
            sender.sendMessage(ColorUtils.colorize(failed));
        }
    }
}
