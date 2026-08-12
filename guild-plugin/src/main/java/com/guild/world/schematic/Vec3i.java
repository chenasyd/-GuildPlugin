package com.guild.world.schematic;

/** 三维整型向量（schematic 内部坐标）。 */
public record Vec3i(int x, int y, int z) {
    public static Vec3i of(int x, int y, int z) {
        return new Vec3i(x, y, z);
    }
}
