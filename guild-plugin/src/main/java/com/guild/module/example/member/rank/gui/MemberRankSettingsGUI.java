package com.guild.module.example.member.rank.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.module.example.member.rank.MemberRankModule;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * A 币增长规则配置 GUI
 * <p>
 * 允许会长配置 A 币的自动增长规则，包括在线时间、奖励间隔等参数。
 */
public class MemberRankSettingsGUI extends AbstractModuleGUI {

    private final MemberRankModule module;
    private final Guild guild;
    private final Player viewer;

    public MemberRankSettingsGUI(MemberRankModule module, Guild guild, Player viewer) {
        this.module = module;
        this.guild = guild;
        this.viewer = viewer;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(module.getContext().getMessage(
                "module.member-rank.settings.title", "&6&lA币增长规则配置"));
    }

    @Override
    public void setupInventory(Inventory inventory) {
        this.inventory = inventory;
        inventory.clear();
        fillBorder(inventory);

        // 标题
        inventory.setItem(4, createItem(Material.BOOK,
                "&6&lA币增长规则配置",
                "",
                "&7配置 A 币的自动增长规则",
                "&7包括在线时间、奖励间隔等参数",
                "",
                "&7数据来源: &e成员排名模块配置",
                "&7更新方式: &a实时保存"));

        // 在线检查间隔
        int checkInterval = module.getContext().getConfig().getInt("activity.online.check-interval-minutes", 1);
        inventory.setItem(11, createItem(Material.CLOCK,
                "&e&l在线检查间隔",
                "&7当前值: &f" + checkInterval + " 分钟",
                "&7设置系统检查玩家在线状态的频率",
                "&7建议值: 1-5 分钟",
                "",
                "&e左键增加 | 右键减少"));

        // 奖励发放间隔
        int awardInterval = module.getContext().getConfig().getInt("activity.online.award-every-minutes", 5);
        inventory.setItem(12, createItem(Material.GOLD_INGOT,
                "&e&l奖励发放间隔",
                "&7当前值: &f" + awardInterval + " 分钟",
                "&7设置发放 A 币的时间间隔",
                "&7建议值: 5-15 分钟",
                "",
                "&e左键增加 | 右键减少"));

        // 活跃窗口时间
        long activeWindow = module.getContext().getConfig().getLong("activity.online.active-window-seconds", 120);
        inventory.setItem(13, createItem(Material.REDSTONE,
                "&e&l活跃窗口时间",
                "&7当前值: &f" + activeWindow + " 秒",
                "&7设置玩家保持活跃的时间窗口",
                "&7建议值: 60-300 秒",
                "",
                "&e左键增加 | 右键减少"));

        // 每次奖励的 A 币数量
        int awardPoints = module.getContext().getConfig().getInt("activity.online.award-points", 2);
        inventory.setItem(14, createItem(Material.DIAMOND,
                "&e&l每次奖励 A 币",
                "&7当前值: &f" + awardPoints + " 个",
                "&7设置每次发放的 A 币数量",
                "&7建议值: 1-5 个",
                "",
                "&e左键增加 | 右键减少"));

        // 每日 A 币上限
        int dailyCap = module.getContext().getConfig().getInt("activity.online.daily-cap", 60);
        inventory.setItem(15, createItem(Material.EMERALD,
                "&e&l每日 A 币上限",
                "&7当前值: &f" + dailyCap + " 个",
                "&7设置玩家每日获得的 A 币上限",
                "&7建议值: 30-120 个",
                "",
                "&e左键增加 | 右键减少"));

        // 加入工会时的初始 A 币
        int defaultContribution = module.getContext().getConfig().getInt("default-contribution-on-join", 0);
        inventory.setItem(20, createItem(Material.GOLD_NUGGET,
                "&e&l初始 A 币",
                "&7当前值: &f" + defaultContribution + " 个",
                "&7设置新成员加入时的初始 A 币",
                "&7建议值: 0-50 个",
                "",
                "&e左键增加 | 右键减少"));

        // 自动发放开关
        boolean autoDepositEnabled = module.getContext().getConfig().getBoolean("auto-deposit.enabled", true);
        inventory.setItem(21, createItem(autoDepositEnabled ? Material.LIME_WOOL : Material.RED_WOOL,
                "&e&l自动发放",
                "&7当前状态: &f" + (autoDepositEnabled ? "开启" : "关闭"),
                "&7是否自动发放 A 币",
                "&7开启后会根据在线时间自动发放",
                "",
                "&e点击切换状态"));

        // 自动发放金额
        double autoDepositAmount = module.getContext().getConfig().getDouble("auto-deposit.amount", 10);
        inventory.setItem(22, createItem(Material.GOLD_BLOCK,
                "&e&l自动发放金额",
                "&7当前值: &f" + autoDepositAmount + " 金币",
                "&7设置自动发放的金币数量",
                "&7建议值: 5-50 金币",
                "",
                "&e左键增加 | 右键减少"));

        // 返回按钮
        inventory.setItem(49, createBackButton(
                module.getContext().getMessage("module.member-rank.gui.back", "&c返回"),
                module.getContext().getMessage("module.member-rank.gui.back-hint", "&7点击返回上一页")));

        fillInteriorSlots(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // 返回按钮
        if (slot == 49) {
            module.getContext().navigateBack(player);
            return;
        }

        // 配置项点击提示
        if (slot >= 11 && slot <= 22) {
            player.sendMessage(ColorUtils.colorize("&e[MemberRank] 配置修改功能暂未开放"));
            return;
        }
    }
}
