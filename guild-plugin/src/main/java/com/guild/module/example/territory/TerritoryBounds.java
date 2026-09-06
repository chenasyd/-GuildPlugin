package com.guild.module.example.territory;

import com.sk89q.worldedit.math.BlockVector3;

/** 立方体边界归一化与 {@link BlockVector3} 转换。 */
public final class TerritoryBounds {

    public record Normalized(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        public BlockVector3 minVector() {
            return BlockVector3.at(minX, minY, minZ);
        }

        public BlockVector3 maxVector() {
            return BlockVector3.at(maxX, maxY, maxZ);
        }
    }

    private TerritoryBounds() {
    }

    public static Normalized normalize(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new Normalized(
                Math.min(x1, x2),
                Math.min(y1, y2),
                Math.min(z1, z2),
                Math.max(x1, x2),
                Math.max(y1, y2),
                Math.max(z1, z2)
        );
    }

    public static Normalized fromRequest(TerritoryClaimRequest request) {
        return normalize(
                request.getMinX(), request.getMinY(), request.getMinZ(),
                request.getMaxX(), request.getMaxY(), request.getMaxZ()
        );
    }

    public static Normalized fromRecord(TerritoryRecord record) {
        return normalize(
                record.getMinX(), record.getMinY(), record.getMinZ(),
                record.getMaxX(), record.getMaxY(), record.getMaxZ()
        );
    }
}
