package com.guild.module.example.territory;

import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * 公会领地 WG 区域默认 Flag：非会员受限，会员通过 WG 成员域（owners/members）获得 BUILD 等权限。
 * <p>
 * 部署后请在测试服验证：会员可建/拆/交互，非会员被拒绝；必要时在 WG 全局或父区域调整优先级。
 */
public final class TerritoryFlagDefaults {

    private TerritoryFlagDefaults() {
    }

    public static void apply(ProtectedRegion region) {
        region.setFlag(Flags.BUILD, StateFlag.State.DENY);
        region.setFlag(Flags.PVP, StateFlag.State.DENY);
        region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.DENY);
        region.setFlag(Flags.USE, StateFlag.State.DENY);
        region.setFlag(Flags.INTERACT, StateFlag.State.DENY);
        region.setPriority(10);
    }
}
