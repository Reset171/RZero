package ru.reset.rzero.adaptive;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class EscapeSpaceAnalyzer {

    private static final int MAX_VISITED = 200;

    private static final int MAX_DISTANCE = 6;

    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DZ = {-1, 1, 0, 0};

    public record Result(int reachable, int cliffs) {
    }

    private EscapeSpaceAnalyzer() {
    }

    public static Result flood(Level level, BlockPos start, LongOpenHashSet blockedCells, boolean allowDrops) {
        final int minDy = allowDrops ? -3 : -2;

        LongOpenHashSet visited = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        long startLong = start.asLong();
        int startX = start.getX();
        int startY = start.getY();
        int startZ = start.getZ();
        queue.enqueue(startLong);
        visited.add(startLong);

        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();

        int cliffs = 0;

        while (!queue.isEmpty() && visited.size() < MAX_VISITED) {
            long current = queue.dequeueLong();
            int cx = BlockPos.getX(current);
            int cy = BlockPos.getY(current);
            int cz = BlockPos.getZ(current);

            for (int i = 0; i < 4; i++) {
                int nx = cx + DX[i];
                int nz = cz + DZ[i];
                long target = 0L;
                boolean found = false;
                boolean isCliff = false;

                for (int dy = 1; dy >= minDy; dy--) {
                    int ny = cy + dy;
                    at.set(nx, ny, nz);
                    below.set(nx, ny - 1, nz);
                    BlockState state = level.getBlockState(at);
                    BlockState floor = level.getBlockState(below);

                    boolean standable = allowDrops
                            ? (floor.blocksMotion() || !floor.getFluidState().isEmpty())
                            : floor.blocksMotion();

                    if (!state.blocksMotion() && standable) {
                        target = BlockPos.asLong(nx, ny, nz);
                        found = true;
                        if (allowDrops && (dy <= minDy || floor.getFluidState().is(FluidTags.LAVA))) {
                            isCliff = true;
                        }
                        break;
                    }
                }

                if (isCliff) {
                    cliffs++;
                }
                if (!found || visited.contains(target)) {
                    continue;
                }
                if (blockedCells != null && blockedCells.contains(target)) {
                    continue;
                }
                int dist = Math.abs(nx - startX)
                        + Math.abs(BlockPos.getY(target) - startY)
                        + Math.abs(nz - startZ);
                if (dist <= MAX_DISTANCE) {
                    visited.add(target);
                    queue.enqueue(target);
                }
            }
        }
        return new Result(visited.size(), cliffs);
    }
}