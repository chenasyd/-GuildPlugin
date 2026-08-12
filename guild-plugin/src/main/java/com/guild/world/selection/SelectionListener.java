package com.guild.world.selection;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * 木斧（可配置）左键 pos1 / 右键 pos2，类 WorldEdit。
 */
public class SelectionListener implements Listener {

    private final GuildPlugin plugin;
    private final SelectionManager selections;
    private final Material wandMaterial;

    public SelectionListener(GuildPlugin plugin, SelectionManager selections, Material wandMaterial) {
        this.plugin = plugin;
        this.selections = selections;
        this.wandMaterial = wandMaterial == null ? Material.WOODEN_AXE : wandMaterial;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("guild.admin.world")) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != wandMaterial) {
            return;
        }
        SelectionManager.Session session = selections.of(player);
        if (!session.wandMode) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        event.setCancelled(true);
        CompatibleScheduler.runTask(plugin, player, () -> {
            String prefix = CoreMsg.raw(plugin, player, "world.prefix", "&6[GuildWorld] &r");
            if (action == Action.LEFT_CLICK_BLOCK) {
                session.pos1 = block.getLocation();
                player.sendMessage(ColorUtils.colorize(prefix + CoreMsg.raw(plugin, player,
                        "world.selection.pos1", "&aPos1 设为 &f{x},{y},{z}",
                        "{x}", String.valueOf(session.pos1.getBlockX()),
                        "{y}", String.valueOf(session.pos1.getBlockY()),
                        "{z}", String.valueOf(session.pos1.getBlockZ()))));
            } else {
                session.pos2 = block.getLocation();
                player.sendMessage(ColorUtils.colorize(prefix + CoreMsg.raw(plugin, player,
                        "world.selection.pos2", "&aPos2 设为 &f{x},{y},{z}",
                        "{x}", String.valueOf(session.pos2.getBlockX()),
                        "{y}", String.valueOf(session.pos2.getBlockY()),
                        "{z}", String.valueOf(session.pos2.getBlockZ()))));
            }
            if (session.pos1 != null && session.pos2 != null
                    && session.pos1.getWorld() != null
                    && session.pos1.getWorld().equals(session.pos2.getWorld())) {
                int dx = Math.abs(session.pos1.getBlockX() - session.pos2.getBlockX()) + 1;
                int dy = Math.abs(session.pos1.getBlockY() - session.pos2.getBlockY()) + 1;
                int dz = Math.abs(session.pos1.getBlockZ() - session.pos2.getBlockZ()) + 1;
                long volume = (long) dx * dy * dz;
                player.sendMessage(ColorUtils.colorize(prefix + CoreMsg.raw(plugin, player,
                        "world.selection.volume", "&7选区 &f{dx}x{dy}x{dz} &7= &f{volume} &7块",
                        "{dx}", String.valueOf(dx),
                        "{dy}", String.valueOf(dy),
                        "{dz}", String.valueOf(dz),
                        "{volume}", String.valueOf(volume))));
            }
        });
    }
}
