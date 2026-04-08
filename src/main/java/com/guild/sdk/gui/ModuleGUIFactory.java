package com.guild.sdk.gui;

import com.guild.core.gui.GUI;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * 模块 GUI 工厂接口 - 模块可注册自定义 GUI 页面
 * <p>
 * 实现此接口来创建模块专属的完整 GUI 界面：
 * <pre>{@code
 * ctx.getApi().registerCustomGUI("my-gui-page",
 *     (player, data) -> new MyCustomGUI(player, data, context));
 * }</pre>
 */
public interface ModuleGUIFactory {

    /**
     * 创建 GUI 实例
     *
     * @param player 打开此 GUI 的玩家
     * @param data   传入的上下文数据（可为 null）
     * @return 构建好的 GUI 对象
     */
    GUI create(Player player, Map<String, Object> data);
}
