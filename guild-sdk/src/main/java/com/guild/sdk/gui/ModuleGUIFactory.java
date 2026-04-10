package com.guild.sdk.gui;

import com.guild.core.gui.GUI;
import org.bukkit.entity.Player;

import java.util.Map;

public interface ModuleGUIFactory {
    GUI create(Player player, Map<String, Object> data);
}
