package com.guild.core.module.hook;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class GUIExtensionHook implements HookPoint {
    public static final int AUTO_SLOT = -1;

    @Override
    public void unregisterByModule(String moduleId) {
    }

    @Override
    public void unregisterAll() {
    }

    public List<GUIInjectionSlot> getInjections(String guiType) {
        return Collections.emptyList();
    }

    public static class GUIInjectionSlot {
        private final String moduleId;
        private final int slot;
        private final ItemStack item;
        private final GUIClickAction action;

        public GUIInjectionSlot(String moduleId, int slot, ItemStack item, GUIClickAction action) {
            this.moduleId = moduleId;
            this.slot = slot;
            this.item = item;
            this.action = action;
        }

        public String getModuleId() { return moduleId; }

        public int getSlot() { return slot; }

        public ItemStack getItem() { return item; }

        public GUIClickAction getAction() { return action; }

        public boolean isAutoSlot() { return slot == AUTO_SLOT; }
    }

    @FunctionalInterface
    public interface GUIClickAction {
        void onClick(Player player, Object... context);
    }
}
