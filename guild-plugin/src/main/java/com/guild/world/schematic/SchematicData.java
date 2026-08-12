package com.guild.world.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GuildWorld 自研 schematic（对齐 Skyllia Internal 思路）。
 *
 * <p>坐标：选区最小角为 (0,0,0)；{@link #origin} 为粘贴对齐点（相对选区）。
 * {@link #blocks} 为 RLE：{@code [paletteIndex, runLength]}。
 */
public class SchematicData {

    public static final int FORMAT_VERSION = 1;

    public int version = FORMAT_VERSION;
    public Vec3i origin = new Vec3i(0, 0, 0);
    public Size3i size = new Size3i(1, 1, 1);
    public List<String> palette = new ArrayList<>();
    /** RLE 条目：[paletteIndex, runLength] */
    public List<int[]> blocks = new ArrayList<>();
    public List<BlockEntityData> blockEntities = new ArrayList<>();

    public static class BlockEntityData {
        public int x, y, z;
        public String kind;
        public Map<String, Object> data;
    }
}
