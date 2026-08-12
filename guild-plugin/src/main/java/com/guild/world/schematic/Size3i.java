package com.guild.world.schematic;

/** Schematic 包围盒尺寸（块数）。 */
public record Size3i(int dx, int dy, int dz) {
    public int volume() {
        return dx * dy * dz;
    }
}
