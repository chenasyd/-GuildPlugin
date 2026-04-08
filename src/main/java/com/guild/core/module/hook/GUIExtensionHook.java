package com.guild.core.module.hook;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GUI 扩展点 - 允许模块在现有 GUI 界面中注入自定义按钮
 */
public class GUIExtensionHook implements HookPoint {

    /** GUI 类型 -> 该类型的所有注入槽位列表 */
    private final Map<String, List<GUIInjectionSlot>> injections = new ConcurrentHashMap<>();

    /**
     * 注册 GUI 按钮注入
     */
    public void registerButton(String guiType, int slot, ItemStack item,
                               String moduleId, GUIClickAction action) {
        injections.computeIfAbsent(guiType, k -> new CopyOnWriteArrayList<>())
                .add(new GUIInjectionSlot(moduleId, slot, item, action));
    }

    /**
     * 获取指定 GUI 类型的所有注入项
     */
    public List<GUIInjectionSlot> getInjections(String guiType) {
        return Collections.unmodifiableList(
                injections.getOrDefault(guiType, Collections.emptyList())
        );
    }

    /** 检查指定 GUI 类型是否存在任何注入 */
    public boolean hasInjections(String guiType) {
        List<GUIInjectionSlot> list = injections.get(guiType);
        return list != null && !list.isEmpty();
    }

    @Override
    public void unregisterByModule(String moduleId) {
        for (List<GUIInjectionSlot> list : injections.values()) {
            list.removeIf(slot -> slot.moduleId.equals(moduleId));
        }
        injections.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @Override
    public void unregisterAll() {
        injections.clear();
    }

    // ==================== 数据结构 ====================

    public static class GUIInjectionSlot {

        private final String moduleId;
        private final int slot;
        private final ItemStack item;
        private final GUIClickAction action;

        public GUIInjectionSlot(String moduleId, int slot, ItemStack item,
                                GUIClickAction action) {
            this.moduleId = moduleId;
            this.slot = slot;
            this.item = item;
            this.action = action;
        }

        public String getModuleId() { return moduleId; }
        public int getSlot() { return slot; }
        public ItemStack getItem() { return item; }
        public GUIClickAction getAction() { return action; }
    }

    @FunctionalInterface
    public interface GUIClickAction {
        void onClick(Player player, Object... context);
    }
}
