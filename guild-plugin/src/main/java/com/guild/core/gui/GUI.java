package com.guild.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * GUI接口 - 定义GUI的基本方法
 */
public interface GUI {
    
    /**
     * 获取GUI标题
     */
    String getTitle();
    
    /**
     * 获取GUI大小（必须是9的倍数）
     */
    int getSize();
    
    /**
     * 设置GUI内容
     */
    void setupInventory(Inventory inventory);
    
    /**
     * 处理GUI点击事件
     */
    void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType);
    
    /**
     * 处理GUI关闭事件
     */
    default void onClose(Player player) {
        // 默认实现为空
    }
    
    /**
     * 刷新GUI
     */
    default void refresh(Player player) {
        // 默认实现为空
    }
    
    /**
     * 检查GUI是否有效
     */
    default boolean isValid() {
        return true;
    }
    
    /**
     * 获取GUI类型标识
     */
    default String getGuiType() {
        return this.getClass().getSimpleName();
    }

    /**
     * 为基岩版玩家打开原生 Cumulus 表单。
     * 返回 true 表示已处理（表单已发送），GUIManager 将跳过 Java Inventory 路径。
     * 返回 false 表示未实现，回退到标准 Inventory（Geyser 自动翻译）。
     */
    default boolean openBedrockForm(Player player) {
        return false;
    }
}
