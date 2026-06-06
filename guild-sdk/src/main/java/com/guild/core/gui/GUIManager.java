package com.guild.core.gui;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class GUIManager {
    public void openGUI(Player player, GUI gui) {
    }

    public void pushAndOpen(Player player, GUI gui) {
    }

    public boolean popAndOpen(Player player) {
        return false;
    }

    // ==================== 在线成员追踪 ====================

    public int getOnlineMemberCount(int guildId) {
        return 0;
    }

    public Set<UUID> getOnlineMembers(int guildId) {
        return Set.of();
    }

    public boolean isPlayerTrackedOnline(UUID playerUuid) {
        return false;
    }

    public void refreshOpenGUIs() {
    }

    public void dumpOnlineStatus() {
    }

    public void forceRefreshGUIs() {
    }
}
