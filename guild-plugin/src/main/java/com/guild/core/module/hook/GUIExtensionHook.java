package com.guild.core.module.hook;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GUI 扩展点 - 允许模块在现有 GUI 界面中注入自定义按钮
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li><b>直接槽位模式</b>: 模块指定具体槽位号（如 GuildInfoGUI 的预留区域）</li>
 *   <li><b>自动分页模式</b>: 模块不指定槽位或使用特殊值，由宿主GUI按页分配槽位</li>
 * </ul>
 */
public class GUIExtensionHook implements HookPoint {

    /** 特殊槽位值：表示"自动分配槽位"（交由宿主GUI的布局引擎决定） */
    public static final int AUTO_SLOT = -1;

    /** GUI 类型 -> 该类型的所有注入槽位列表 */
    private final Map<String, List<GUIInjectionSlot>> injections = new ConcurrentHashMap<>();

    /**
     * 注册 GUI 按钮注入
     *
     * @param guiType  目标GUI类型标识符（如 "GuildSettingsGUI"、"GuildInfoGUI"）
     * @param slot     注入槽位(0-based)，传入 {@link #AUTO_SLOT} 表示由宿主GUI自动分配
     * @param item     显示物品图标
     * @param moduleId 模块ID(卸载时自动清理)
     * @param action   点击回调
     */
    public void registerButton(String guiType, int slot, ItemStack item,
                               String moduleId, GUIClickAction action) {
        injections.computeIfAbsent(guiType, k -> new CopyOnWriteArrayList<>())
                .add(new GUIInjectionSlot(moduleId, slot, item, action));
    }

    /**
     * 注册 GUI 按钮注入（自动分配槽位模式）
     * 使用此方法注册的按钮将由宿主GUI按页自动排列
     */
    public void registerButtonAuto(String guiType, ItemStack item,
                                   String moduleId, GUIClickAction action) {
        registerButton(guiType, AUTO_SLOT, item, moduleId, action);
    }

    /**
     * 获取指定 GUI 类型的所有注入项（只读视图）
     */
    public List<GUIInjectionSlot> getInjections(String guiType) {
        List<GUIInjectionSlot> list = injections.getOrDefault(guiType, Collections.emptyList());
        if (list.isEmpty()) return Collections.emptyList();
        // 按 moduleId A-Z 排序后返回只读视图
        List<GUIInjectionSlot> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(GUIInjectionSlot::getModuleId));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * 检查指定 GUI 类型是否存在任何注入
     */
    public boolean hasInjections(String guiType) {
        List<GUIInjectionSlot> list = injections.get(guiType);
        return list != null && !list.isEmpty();
    }

    /**
     * 获取指定 GUI 类型的总注入数量
     */
    public int getInjectionCount(String guiType) {
        List<GUIInjectionSlot> list = injections.get(guiType);
        return list != null ? list.size() : 0;
    }

    /**
     * 分页获取指定 GUI 类型的注入项（仅返回 {@link #AUTO_SLOT} 类型的条目）
     * 用于宿主GUI实现多页布局时按页渲染模块按钮。
     *
     * @param guiType      目标GUI类型
     * @param page         页码（从1开始）
     * @param perPage      每页最大条目数
     * @return 当前页的注入项列表（空列表表示该页无内容）
     */
    public List<GUIInjectionSlot> getPageInjections(String guiType, int page, int perPage) {
        // 仅筛选自动分配槽位的注入项（排除已指定固定槽位的）
        List<GUIInjectionSlot> autoSlots = new ArrayList<>();
        List<GUIInjectionSlot> all = injections.get(guiType);
        if (all == null) return Collections.emptyList();

        for (GUIInjectionSlot slot : all) {
            if (slot.slot == AUTO_SLOT) {
                autoSlots.add(slot);
            }
        }

        if (autoSlots.isEmpty()) return Collections.emptyList();

        // 按 moduleId A-Z 排序
        autoSlots.sort(Comparator.comparing(GUIInjectionSlot::getModuleId));

        int fromIndex = (page - 1) * perPage;
        int toIndex = Math.min(fromIndex + perPage, autoSlots.size());

        if (fromIndex >= autoSlots.size()) return Collections.emptyList();

        return Collections.unmodifiableList(autoSlots.subList(fromIndex, toIndex));
    }

    /**
     * 计算指定 GUI 类型需要多少页来容纳所有自动分配槽位的注入项
     *
     * @param guiType 目标GUI类型
     * @param perPage 每页最大条目数
     * @return 总页数（至少为1，即使没有注入项也返回1以便显示空页面）
     */
    public int getTotalPages(String guiType, int perPage) {
        int count = getAutoSlotCount(guiType);
        if (count == 0) return 1; // 至少返回1页
        return (int) Math.ceil((double) count / perPage);
    }

    /**
     * 获取指定 GUI 类型中自动分配槽位的注入项数量
     */
    public int getAutoSlotCount(String guiType) {
        List<GUIInjectionSlot> all = injections.get(guiType);
        if (all == null) return 0;
        int count = 0;
        for (GUIInjectionSlot slot : all) {
            if (slot.slot == AUTO_SLOT) count++;
        }
        return count;
    }

    /**
     * 获取指定 GUI 类型中指定了固定槽位的注入项
     * 用于 GuildInfoGUI 等在预留区域直接放置的模块按钮
     */
    public List<GUIInjectionSlot> getFixedSlotInjections(String guiType) {
        List<GUIInjectionSlot> fixed = new ArrayList<>();
        List<GUIInjectionSlot> all = injections.get(guiType);
        if (all == null) return Collections.emptyList();

        for (GUIInjectionSlot slot : all) {
            if (slot.slot != AUTO_SLOT) {
                fixed.add(slot);
            }
        }

        // 按 moduleId A-Z 排序
        fixed.sort(Comparator.comparing(GUIInjectionSlot::getModuleId));

        return Collections.unmodifiableList(fixed);
    }

    /**
     * 检查是否有任何自动分配槽位的注入项存在
     */
    public boolean hasAutoSlotInjections(String guiType) {
        return getAutoSlotCount(guiType) > 0;
    }

    @Override
    public void unregisterByModule(String moduleId) {
        for (List<GUIInjectionSlot> list : injections.values()) {
            list.removeIf(slot -> slot.moduleId.equals(moduleId));
        }
        // 清理空的 entry
        injections.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @Override
    public void unregisterAll() {
        injections.clear();
    }

    // ==================== 数据结构 ====================

    /**
     * 注入槽位数据模型
     */
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

        /** 是否为自动分配槽位 */
        public boolean isAutoSlot() { return slot == AUTO_SLOT; }
    }

    /**
     * 点击动作函数式接口
     */
    @FunctionalInterface
    public interface GUIClickAction {
        void onClick(Player player, Object... context);
    }
}
