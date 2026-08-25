package ru.reset.rzero.checkpoint.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public record ChunkScope(ResourceKey<Level> dim,
                         int minChunkX, int minChunkZ,
                         int maxChunkX, int maxChunkZ) {

    public static ChunkScope around(ServerLevel level, BlockPos anchor, int chunkRadius) {
        if (chunkRadius < 0) {
            throw new IllegalArgumentException("chunkRadius must be >= 0, got " + chunkRadius);
        }
        int cx = anchor.getX() >> 4;
        int cz = anchor.getZ() >> 4;
        return new ChunkScope(level.dimension(),
                cx - chunkRadius, cz - chunkRadius,
                cx + chunkRadius, cz + chunkRadius);
    }

    public boolean includes(ResourceKey<Level> testDim, long chunkKey) {
        if (!this.dim.equals(testDim)) return false;
        ChunkPos cp = new ChunkPos(chunkKey);
        return cp.x >= minChunkX && cp.x <= maxChunkX
                && cp.z >= minChunkZ && cp.z <= maxChunkZ;
    }

    public boolean coversDim(ResourceKey<Level> testDim) {
        return this.dim.equals(testDim);
    }

    public AABB toAABB(ServerLevel level) {
        return new AABB(
                minChunkX << 4, level.getMinBuildHeight(), minChunkZ << 4,
                (maxChunkX + 1) << 4, level.getMaxBuildHeight(), (maxChunkZ + 1) << 4);
    }
}
