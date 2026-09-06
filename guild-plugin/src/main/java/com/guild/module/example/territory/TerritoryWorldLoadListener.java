package com.guild.module.example.territory;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/** 世界延迟加载时补建本机 WG 领地。 */
public final class TerritoryWorldLoadListener implements Listener {

    private final TerritoryMaterializer materializer;

    public TerritoryWorldLoadListener(TerritoryMaterializer materializer) {
        this.materializer = materializer;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        materializer.scheduleMaterializeForWorld(event.getWorld().getName());
    }
}
