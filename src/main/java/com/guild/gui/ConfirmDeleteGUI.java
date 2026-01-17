package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.utils.GuildDeletionManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * 删除确认 GUI
 */
public class ConfirmDeleteGUI implements GUI {
	private final GuildPlugin plugin;
	private final Player actor;
	private final Guild guild;
	private final boolean adminMode;

	public ConfirmDeleteGUI(GuildPlugin plugin, Player actor, Guild guild, boolean adminMode) {
		this.plugin = plugin;
		this.actor = actor;
		this.guild = guild;
		this.adminMode = adminMode;
	}

	@Override
	public String getTitle() {
		return ColorUtils.colorize("&c确认删除 - &e" + guild.getName());
	}

	@Override
	public int getSize() {
		return 9;
	}

	@Override
	public void setupInventory(Inventory inventory) {
		// 确认按钮（左）
		inventory.setItem(3, createItem(Material.LIME_WOOL, ColorUtils.colorize("&a确认删除"), ColorUtils.colorize("&7点击以永久删除工会")));
		// 取消按钮（右）
		inventory.setItem(5, createItem(Material.RED_WOOL, ColorUtils.colorize("&c取消"), ColorUtils.colorize("&7点击取消删除操作")));
	}

	@Override
	public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
		if (slot == 3) { // 确认
			boolean isLeader = (guild.getLeaderName() != null && guild.getLeaderName().equalsIgnoreCase(player.getName()));
			if (!isLeader && !adminMode && !plugin.getPermissionManager().hasPermission(player, "guild.admin")) {
				player.sendMessage(ColorUtils.colorize("&c只有会长或管理员可以删除工会！"));
				return;
			}

			player.sendMessage(ColorUtils.colorize("&e正在删除工会，请稍候..."));
			// 异步删除并在主线程通知
			GuildDeletionManager.forceDeleteGuild(plugin, guild, player).whenComplete((result, ex) -> {
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					if (ex != null) {
						plugin.getLogger().log(Level.SEVERE, "删除工会时发生异常: " + guild.getName(), ex);
						player.sendMessage(ColorUtils.colorize("&c删除工会时发生错误，查看控制台以获取详情。"));
						plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, player, guild, adminMode));
					} else if (result != null && result.isSuccess()) {
						player.sendMessage(ColorUtils.colorize("&a已删除工会: " + guild.getName()));
						plugin.getGuiManager().closeGUI(player);
					} else {
						String reason = result != null ? result.getReason() : "未知原因";
						player.sendMessage(ColorUtils.colorize("&c删除工会失败: &f" + reason));
						plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, player, guild, adminMode));
						if (result != null && result.getException() != null) {
							plugin.getLogger().log(Level.SEVERE, "删除工会失败: " + guild.getName(), result.getException());
						}
					}
				});
			});
		} else if (slot == 5) { // 取消
			plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, player, guild, adminMode));
		}
	}

	@Override
	public void onClose(Player player) { }

	@Override
	public void refresh(Player player) { }

	private ItemStack createItem(Material mat, String name, String lore) {
		ItemStack it = new ItemStack(mat);
		ItemMeta meta = it.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(name);
			List<String> ll = new ArrayList<>();
			ll.add(lore);
			meta.setLore(ll);
			it.setItemMeta(meta);
		}
		return it;
	}

	/**
	 * 统一的删除执行方法：命令与 GUI 都调用此方法。
	 * executor 可以是 Player 或 控制台（CommandSender）
	 */
	public static void confirmDelete(GuildPlugin plugin, CommandSender executor, Guild guild, boolean adminMode) {
		 // Player: 使用异步删除并在主线程反馈（与 GUI 点击一致）
		if (executor instanceof Player player) {
			player.sendMessage(ColorUtils.colorize("&e正在删除工会，请稍候..."));
			GuildDeletionManager.forceDeleteGuild(plugin, guild, player).whenComplete((result, ex) -> {
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					if (ex != null) {
						plugin.getLogger().log(Level.SEVERE, "删除工会时发生异常: " + guild.getName(), ex);
						player.sendMessage(ColorUtils.colorize("&c删除工会时发生错误，查看控制台以获取详情。"));
						plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, player, guild, adminMode));
					} else if (result != null && result.isSuccess()) {
						player.sendMessage(ColorUtils.colorize("&a已删除工会: " + guild.getName()));
						plugin.getGuiManager().closeGUI(player);
					} else {
						String reason = result != null ? result.getReason() : "未知原因";
						player.sendMessage(ColorUtils.colorize("&c删除工会失败: &f" + reason));
						plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, player, guild, adminMode));
						if (result != null && result.getException() != null) {
							plugin.getLogger().log(Level.SEVERE, "删除工会失败: " + guild.getName(), result.getException());
						}
					}
				});
			});
			return;
		}

		// 控制台或非玩家：同步删除并反馈
		try {
			plugin.getGuildManager().deleteGuild(guild);
			executor.sendMessage(ColorUtils.colorize("&a工会 &e" + guild.getName() + " &a已被删除（控制台）。"));
			plugin.getLogger().info("Guild deleted by " + (executor instanceof Player ? ((Player) executor).getName() : "Console") + " -> " + guild.getName());
		} catch (Exception ex) {
			plugin.getLogger().log(Level.SEVERE, "控制台删除工会时出现异常: " + guild.getName(), ex);
			executor.sendMessage(ColorUtils.colorize("&c删除工会时发生错误，查看控制台以获取详情。"));
		}
	}
}
