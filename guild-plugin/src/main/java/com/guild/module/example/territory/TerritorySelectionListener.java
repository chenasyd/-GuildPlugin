package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
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

/** 领地选区斧：左键 Pos1 / 右键 Pos2。 */
public final class TerritorySelectionListener implements Listener {

    private final ModuleContext context;
    private final TerritorySelectionManager selections;
    private final Material wandMaterial;

    public TerritorySelectionListener(ModuleContext context, TerritorySelectionManager selections, Material wandMaterial) {
        this.context = context;
        this.selections = selections;
        this.wandMaterial = wandMaterial == null ? Material.WOODEN_AXE : wandMaterial;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("guild.territory.claim")) {
            return;
        }
        TerritorySelectionManager.Session session = selections.of(player);
        if (!session.wandMode) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != wandMaterial) {
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
        CompatibleScheduler.runTask(context.getPlugin(), player, () -> {
            if (action == Action.LEFT_CLICK_BLOCK) {
                session.pos1 = block.getLocation();
                context.sendMessage(player, "module.territory.pos1",
                        "&a[领地] Pos1: &f{0},{1},{2}",
                        session.pos1.getBlockX(), session.pos1.getBlockY(), session.pos1.getBlockZ());
            } else {
                session.pos2 = block.getLocation();
                context.sendMessage(player, "module.territory.pos2",
                        "&a[领地] Pos2: &f{0},{1},{2}",
                        session.pos2.getBlockX(), session.pos2.getBlockY(), session.pos2.getBlockZ());
            }
            if (selections.hasCompleteSelection(player)) {
                long volume = selections.selectionVolume(player);
                context.sendMessage(player, "module.territory.selection-volume",
                        "&7选区体积: &f{0} &7方块", volume);
            }
        });
    }
}
