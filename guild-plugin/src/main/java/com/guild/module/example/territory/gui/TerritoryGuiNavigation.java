package com.guild.module.example.territory.gui;

import com.guild.models.Guild;
import com.guild.module.example.territory.TerritoryModule;
import org.bukkit.entity.Player;

/**
 * 领地 GUI 导航辅助：子页面（确认框）返回管理面板时使用 {@code navigateBack}，
 * 避免 {@code openGUI} 再次压栈导致「返回」落到错误界面。
 */
final class TerritoryGuiNavigation {

    private TerritoryGuiNavigation() {
    }

    /** 从确认对话框等子页面返回领地面板。 */
    static void backToManagement(TerritoryModule module, Player player) {
        if (!module.getContext().navigateBack(player)) {
            Guild guild = module.getContext().getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
            if (guild != null) {
                boolean manage = module.getContext().getPlugin().getMembershipRules().canManageGuild(player)
                        && module.getContext().getPlugin().getPermissionManager()
                        .hasPermission(player, "guild.territory.claim");
                module.openTerritoryGui(player, guild, manage);
            }
        }
    }

    /** 领地面板「返回」：有上一级则弹出，否则关闭 GUI（如命令直接进入）。 */
    static void backFromManagement(TerritoryModule module, Player player) {
        if (!module.getContext().navigateBack(player)) {
            module.getContext().getPlugin().getGuiManager().closeGUI(player);
        }
    }
}
