package com.guild.world.api;

import com.guild.world.GuildWorldService;
import com.guild.world.model.GuildWorld;
import com.guild.world.preset.PresetService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** {@link GuildWorldAPI} 默认实现，委托 {@link GuildWorldService}。 */
public final class GuildWorldAPIImpl implements GuildWorldAPI {

    private final GuildWorldService service;

    public GuildWorldAPIImpl(GuildWorldService service) {
        this.service = service;
    }

    @Override
    public boolean isAvailable() {
        return service.isEnabled();
    }

    @Override
    public String unavailableReason() {
        return service.isEnabled() ? "" : service.unsupportedMessage();
    }

    @Override
    public Collection<GuildWorld> getManagedWorlds() {
        return service.getWorlds();
    }

    @Override
    public GuildWorld getManagedWorld(String worldName) {
        return service.getWorld(service.buildWorldName(worldName));
    }

    @Override
    public Collection<PresetService.PresetMeta> listPresets() {
        return service.getPresets().list();
    }

    @Override
    public PresetService.PresetMeta getPreset(String name) {
        return service.getPresets().get(name);
    }

    @Override
    public CompletableFuture<PresetService.PresetMeta> savePresetFromSelection(Player editor, String presetName) {
        return service.savePresetFromSelection(editor, presetName);
    }

    @Override
    public CompletableFuture<Void> pastePreset(World world, Location pasteAt, String presetName) {
        return service.pastePreset(world, pasteAt, presetName);
    }

    @Override
    public CompletableFuture<GuildWorld> createWorldFromPreset(String worldName, String presetName) {
        return service.createWorldFromPreset(worldName, presetName);
    }

    @Override
    public CompletableFuture<Boolean> teleportToWorld(Player player, String worldName) {
        return service.teleportToWorld(player, worldName);
    }
}
