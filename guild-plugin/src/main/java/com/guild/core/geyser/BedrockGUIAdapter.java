package com.guild.core.geyser;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/**
 * 基岩版 GUI 点击适配器。
 * <p>
 * 由于基岩版客户端无法可靠触发右键点击（{@link ClickType#RIGHT} / {@link ClickType#SHIFT_RIGHT}），
 * 本适配器对基岩版玩家的点击类型进行默认重映射：
 * <ul>
 *   <li>所有包含 RIGHT 的点击 → LEFT_CLICK</li>
 *   <li>其余点击类型保持不变</li>
 * </ul>
 * <p>
 * 长远规划：对于需要区分左/右键的多组合 GUI，应为其提供独立的基岩版 GUI 实现。
 */
public final class BedrockGUIAdapter {

    private BedrockGUIAdapter() {}

    /**
     * 对基岩版玩家的点击类型进行适配。
     *
     * @param player        目标玩家
     * @param originalClick 原始点击类型
     * @return 适配后的点击类型（Java 版玩家原样返回）
     */
    public static ClickType adapt(Player player, ClickType originalClick) {
        if (!GeyserAPI.isBedrockPlayer(player)) {
            return originalClick;
        }
        // 基岩版右键不可靠 → 全部映射为左键
        return switch (originalClick) {
            case RIGHT, SHIFT_RIGHT, CONTROL_DROP, DROP -> ClickType.LEFT;
            default -> originalClick;
        };
    }
}
