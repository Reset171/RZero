package ru.reset.rzero.block;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;

import java.util.List;

public final class SectionSnapshot {
    private static final int SECTION_SIZE = 4096;

    private final BlockState[] palette;

    private final long[] storage;

    private final int bitsPerEntry;

    private SectionSnapshot(BlockState[] palette, long[] storage, int bitsPerEntry) {
        this.palette = palette;
        this.storage = storage;
        this.bitsPerEntry = bitsPerEntry;
    }

    public static SectionSnapshot capture(PalettedContainer<BlockState> states) {
        PalettedContainerRO.PackedData<BlockState> packed =
                states.pack(Block.BLOCK_STATE_REGISTRY, PalettedContainer.Strategy.SECTION_STATES);
        List<BlockState> entries = packed.paletteEntries();
        BlockState[] palette = entries.toArray(new BlockState[0]);
        long[] storage = packed.storage().map(java.util.stream.LongStream::toArray).orElse(null);
        int bits = sectionStatesBits(palette.length);
        return new SectionSnapshot(palette, storage, bits);
    }

    private static int sectionStatesBits(int paletteSize) {
        if (paletteSize <= 1) return 0;
        int b = Mth.ceillog2(paletteSize);
        return b <= 4 ? 4 : b;
    }

    public BlockState get(int localIdx) {
        if (storage == null) return palette[0];
        int valuesPerLong = 64 / bitsPerEntry;
        int word = localIdx / valuesPerLong;
        int bit = (localIdx - word * valuesPerLong) * bitsPerEntry;
        long mask = (1L << bitsPerEntry) - 1L;
        int paletteIdx = (int) ((storage[word] >>> bit) & mask);
        return palette[paletteIdx];
    }

    public int applyDiffTo(LevelChunkSection live, LevelChunk chunk, ServerLevel level,
                           int xBase, int yBase, int zBase, boolean isDuringLoad) {
        int changed = 0;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int idx = (y << 8) | (z << 4) | x;
                    BlockState want = get(idx);
                    BlockState got = live.getBlockState(x, y, z);
                    if (got != want) {
                        net.minecraft.core.BlockPos p =
                                new net.minecraft.core.BlockPos(xBase + x, yBase + y, zBase + z);
                        if (got.hasBlockEntity()) {
                            live.setBlockState(x, y, z, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
                        }
                        chunk.setBlockState(p, want, false);
                        if (!isDuringLoad) {
                            level.getChunkSource().blockChanged(p);
                            level.getLightEngine().checkBlock(p);
                        }
                        changed++;
                    }
                }
            }
        }
        return changed;
    }


    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("b", (byte) bitsPerEntry);
        ListTag pal = new ListTag();
        for (BlockState bs : palette) pal.add(NbtUtils.writeBlockState(bs));
        tag.put("p", pal);
        if (storage != null) tag.putLongArray("s", storage);
        return tag;
    }

    public static SectionSnapshot fromNBT(CompoundTag tag, HolderGetter<Block> blockGetter) {
        int bits = tag.getByte("b") & 0xFF;
        ListTag pal = tag.getList("p", Tag.TAG_COMPOUND);
        BlockState[] palette = new BlockState[pal.size()];
        for (int i = 0; i < pal.size(); i++) {
            palette[i] = NbtUtils.readBlockState(blockGetter, pal.getCompound(i));
        }
        long[] storage = tag.contains("s", Tag.TAG_LONG_ARRAY) ? tag.getLongArray("s") : null;
        return new SectionSnapshot(palette, storage, bits);
    }

    public int paletteSize() { return palette.length; }
    public boolean isSingleState() { return storage == null; }
    public int storageWords() { return storage == null ? 0 : storage.length; }
}
