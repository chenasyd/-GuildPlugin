package com.guild.module.example.territory;

import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * 公会领地 WG 区域默认 Flag：非会员受限，会员通过 WG 成员域（owners/members）获得 BUILD 等权限。
 */
public final class TerritoryFlagDefaults {

    private TerritoryFlagDefaults() {
    }

    public static void apply(ProtectedRegion region, TerritorySettings settings) {
        applyStateFlag(region, Flags.BUILD, settings.getFlag("build", "deny"));
        applyStateFlag(region, Flags.PVP, settings.getFlag("pvp", "deny"));
        applyStateFlag(region, Flags.MOB_DAMAGE, settings.getFlag("mob-damage", "deny"));
        applyStateFlag(region, Flags.TNT, settings.getFlag("tnt", "deny"));
        applyStateFlag(region, Flags.CHEST_ACCESS, settings.getFlag("chest-access", "deny"));
        applyStateFlag(region, Flags.USE, settings.getFlag("use", "deny"));
        applyStateFlag(region, Flags.INTERACT, settings.getFlag("interact", "deny"));
        applyStateFlag(region, Flags.ENTRY, settings.getFlag("entry", "allow"));
        applyStateFlag(region, Flags.EXIT, settings.getFlag("exit", "allow"));
        region.setPriority(settings.getRegionPriority());
    }

    static void applyStateFlag(ProtectedRegion region, StateFlag flag, String configValue) {
        if (flag == null || configValue == null) {
            return;
        }
        String normalized = configValue.trim().toLowerCase();
        if ("none".equals(normalized) || "unset".equals(normalized)) {
            return;
        }
        StateFlag.State state = switch (normalized) {
            case "allow", "true", "yes" -> StateFlag.State.ALLOW;
            case "deny", "false", "no" -> StateFlag.State.DENY;
            default -> null;
        };
        if (state != null) {
            region.setFlag(flag, state);
        }
    }
}
