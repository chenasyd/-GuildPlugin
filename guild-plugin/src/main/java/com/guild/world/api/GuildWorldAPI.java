package com.guild.world.api;

import com.guild.world.model.GuildWorld;
import com.guild.world.preset.PresetService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * 工会插件世界管理对外 API（供外部插件 / 后续工会战模块调用）。
 *
 * <p>通过 {@code GuildPlugin.getGuildWorldAPI()} 或 ServiceContainer 获取。
 */
public interface GuildWorldAPI {

    boolean isAvailable();

    String unavailableReason();

    Collection<GuildWorld> getManagedWorlds();

    GuildWorld getManagedWorld(String worldName);

    Collection<PresetService.PresetMeta> listPresets();

    PresetService.PresetMeta getPreset(String name);

    /**
     * 从玩家当前选区导出并保存预设（含 schematic）。
     */
    CompletableFuture<PresetService.PresetMeta> savePresetFromSelection(Player editor, String presetName);

    /**
     * 将预设粘贴到已加载世界；{@code pasteAt} 对齐 schematic.origin。
     */
    CompletableFuture<Void> pastePreset(World world, Location pasteAt, String presetName);

    /**
     * 创建虚空世界并粘贴预设（BATTLE 类型）；返回受管世界记录。
     */
    CompletableFuture<GuildWorld> createWorldFromPreset(String worldName, String presetName);

    CompletableFuture<Boolean> teleportToWorld(Player player, String worldName);
}
