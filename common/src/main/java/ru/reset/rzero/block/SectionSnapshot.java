package ru.reset.rzero.block;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.chunk.Palette;

import java.util.ArrayList;
import java.util.List;

public final class SectionSnapshot {
    private static final int SECTION_SIZE = 4096;

    private final BlockState[] palette;
    private final long[] storage;
    private final int bitsPerEntry;

    private final Holder<Biome>[] biomePalette;
    private final long[] biomeStorage;
    private final int biomeBitsPerEntry;

    private SectionSnapshot(BlockState[] palette, long[] storage, int bitsPerEntry,
                            Holder<Biome>[] biomePalette, long[] biomeStorage, int biomeBitsPerEntry) {
        this.palette = palette;
        this.storage = storage;
        this.bitsPerEntry = bitsPerEntry;
        this.biomePalette = biomePalette;
        this.biomeStorage = biomeStorage;
        this.biomeBitsPerEntry = biomeBitsPerEntry;
    }

    public static SectionSnapshot capture(LevelChunkSection sec) {
        PalettedContainerRO.PackedData<BlockState> packed =
                sec.getStates().pack(Block.BLOCK_STATE_REGISTRY, PalettedContainer.Strategy.SECTION_STATES);
        List<BlockState> entries = packed.paletteEntries();
        BlockState[] palette = entries.toArray(new BlockState[0]);
        long[] storage = packed.storage().map(java.util.stream.LongStream::toArray).orElse(null);
        int bits = sectionStatesBits(palette.length);

        Holder<Biome> firstBiome = sec.getNoiseBiome(0, 0, 0);
        boolean allSame = true;
        for (int y = 0; y < 4 && allSame; y++) {
            for (int z = 0; z < 4 && allSame; z++) {
                for (int x = 0; x < 4 && allSame; x++) {
                    if (sec.getNoiseBiome(x, y, z) != firstBiome) {
                        allSame = false;
                    }
                }
            }
        }

        Holder<Biome>[] bPalette;
        long[] bStorage;
        int bBits;

        if (allSame) {
            bPalette = (Holder<Biome>[]) new Holder<?>[]{firstBiome};
            bStorage = null;
            bBits = 0;
        } else {
            List<Holder<Biome>> bList = new ArrayList<>();
            Object2IntOpenHashMap<Holder<Biome>> bMap = new Object2IntOpenHashMap<>();
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    for (int x = 0; x < 4; x++) {
                        Holder<Biome> b = sec.getNoiseBiome(x, y, z);
                        if (!bMap.containsKey(b)) {
                            bMap.put(b, bList.size());
                            bList.add(b);
                        }
                    }
                }
            }
            bPalette = (Holder<Biome>[]) bList.toArray(new Holder<?>[0]);
            bBits = sectionBiomesBits(bPalette.length);
            int valuesPerLong = 64 / bBits;
            int words = (64 + valuesPerLong - 1) / valuesPerLong;
            bStorage = new long[words];
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    for (int x = 0; x < 4; x++) {
                        int idx = (y << 4) | (z << 2) | x;
                        int palIdx = bMap.getInt(sec.getNoiseBiome(x, y, z));
                        int word = idx / valuesPerLong;
                        int bit = (idx - word * valuesPerLong) * bBits;
                        bStorage[word] |= ((long) palIdx) << bit;
                    }
                }
            }
        }

        return new SectionSnapshot(palette, storage, bits, bPalette, bStorage, bBits);
    }

    private static int sectionStatesBits(int paletteSize) {
        if (paletteSize <= 1) return 0;
        int b = Mth.ceillog2(paletteSize);
        return b <= 4 ? 4 : b;
    }

    private static int sectionBiomesBits(int paletteSize) {
        if (paletteSize <= 1) return 0;
        int b = Mth.ceillog2(paletteSize);
        return b <= 3 ? 3 : b;
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

    public Holder<Biome> getBiome(int bx, int by, int bz) {
        if (biomePalette == null || biomePalette.length == 0) return null;
        if (biomeStorage == null) return biomePalette[0];
        int idx = (by << 4) | (bz << 2) | bx;
        int valuesPerLong = 64 / biomeBitsPerEntry;
        int word = idx / valuesPerLong;
        int bit = (idx - word * valuesPerLong) * biomeBitsPerEntry;
        long mask = (1L << biomeBitsPerEntry) - 1L;
        int paletteIdx = (int) ((biomeStorage[word] >>> bit) & mask);
        return biomePalette[paletteIdx];
    }

    public int applyDiffTo(LevelChunkSection live, LevelChunk chunk, ServerLevel level,
                           int xBase, int yBase, int zBase, boolean isDuringLoad,
                           it.unimi.dsi.fastutil.longs.LongList changedPositions,
                           boolean restoreBiomes, boolean[] biomeChangedOut) {
        int changed = 0;

        PalettedContainer<BlockState> statesContainer = live.getStates();
        PalettedContainer.Data<BlockState> liveData = statesContainer.data;
        Palette<BlockState> livePalette = liveData.palette();
        BitStorage liveStorage = liveData.storage();

        boolean skipBlocks = false;
        if (this.storage == null) {
            BlockState single = this.palette[0];
            if (livePalette.getSize() == 1 && livePalette.valueFor(0) == single) {
                skipBlocks = true;
            } else if (single.isAir() && live.hasOnlyAir()) {
                skipBlocks = true;
            }
        }

        if (!skipBlocks) {
            int valuesPerLong = this.bitsPerEntry > 0 ? 64 / this.bitsPerEntry : 0;
            long mask = this.bitsPerEntry > 0 ? (1L << this.bitsPerEntry) - 1L : 0L;
            int word = 0;
            int countInWord = 0;
            long currentWord = this.storage != null ? this.storage[0] : 0L;

            int idx = 0;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState want;
                        if (this.storage == null) {
                            want = this.palette[0];
                        } else {
                            int palIdx = (int) (currentWord & mask);
                            currentWord >>>= this.bitsPerEntry;
                            countInWord++;
                            if (countInWord == valuesPerLong) {
                                word++;
                                if (word < this.storage.length) {
                                    currentWord = this.storage[word];
                                }
                                countInWord = 0;
                            }
                            want = this.palette[palIdx];
                        }

                        BlockState got = livePalette.valueFor(liveStorage.get(idx));
                        if (got != want) {
                            net.minecraft.core.BlockPos p =
                                    new net.minecraft.core.BlockPos(xBase + x, yBase + y, zBase + z);
                            if (got.hasBlockEntity() || chunk.getBlockEntity(p) != null) {
                                chunk.removeBlockEntity(p);
                            }
                            chunk.setBlockState(p, want, false);
                            if (!isDuringLoad && changedPositions != null) {
                                changedPositions.add(p.asLong());
                            }
                            changed++;
                        }
                        idx++;
                    }
                }
            }
        }

        if (restoreBiomes && biomePalette != null && biomePalette.length > 0) {
            PalettedContainer<Holder<Biome>> biomesContainer = (PalettedContainer<Holder<Biome>>) live.getBiomes();
            PalettedContainer.Data<Holder<Biome>> biomeData = biomesContainer.data;
            Palette<Holder<Biome>> liveBiomePalette = biomeData.palette();
            BitStorage liveBiomeStorage = biomeData.storage();

            boolean skipBiomes = false;
            if (this.biomeStorage == null && liveBiomePalette.getSize() == 1) {
                if (liveBiomePalette.valueFor(0) == this.biomePalette[0]) {
                    skipBiomes = true;
                }
            }

            if (!skipBiomes) {
                int bValuesPerLong = this.biomeBitsPerEntry > 0 ? 64 / this.biomeBitsPerEntry : 0;
                long bMask = this.biomeBitsPerEntry > 0 ? (1L << this.biomeBitsPerEntry) - 1L : 0L;
                int bWord = 0;
                int bCountInWord = 0;
                long bCurrentWord = this.biomeStorage != null ? this.biomeStorage[0] : 0L;

                int bIdx = 0;
                for (int by = 0; by < 4; by++) {
                    for (int bz = 0; bz < 4; bz++) {
                        for (int bx = 0; bx < 4; bx++) {
                            Holder<Biome> want;
                            if (this.biomeStorage == null) {
                                want = this.biomePalette[0];
                            } else {
                                int palIdx = (int) (bCurrentWord & bMask);
                                bCurrentWord >>>= this.biomeBitsPerEntry;
                                bCountInWord++;
                                if (bCountInWord == bValuesPerLong) {
                                    bWord++;
                                    if (bWord < this.biomeStorage.length) {
                                        bCurrentWord = this.biomeStorage[bWord];
                                    }
                                    bCountInWord = 0;
                                }
                                want = this.biomePalette[palIdx];
                            }

                            if (want != null) {
                                Holder<Biome> got = liveBiomePalette.valueFor(liveBiomeStorage.get(bIdx));
                                if (got != want) {
                                    biomesContainer.set(bx, by, bz, want);
                                    if (biomeChangedOut != null) {
                                        biomeChangedOut[0] = true;
                                    }
                                }
                            }
                            bIdx++;
                        }
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

        if (biomePalette != null && biomePalette.length > 0) {
            tag.putByte("bb", (byte) biomeBitsPerEntry);
            ListTag bpal = new ListTag();
            for (Holder<Biome> b : biomePalette) {
                String id = (b != null && b.unwrapKey().isPresent())
                        ? b.unwrapKey().get().location().toString()
                        : "minecraft:plains";
                bpal.add(StringTag.valueOf(id));
            }
            tag.put("bp", bpal);
            if (biomeStorage != null) tag.putLongArray("bs", biomeStorage);
        }

        return tag;
    }

    public static SectionSnapshot fromNBT(CompoundTag tag, HolderGetter<Block> blockGetter, HolderGetter<Biome> biomeGetter) {
        int bits = tag.getByte("b") & 0xFF;
        ListTag pal = tag.getList("p", Tag.TAG_COMPOUND);
        BlockState[] palette = new BlockState[pal.size()];
        for (int i = 0; i < pal.size(); i++) {
            palette[i] = NbtUtils.readBlockState(blockGetter, pal.getCompound(i));
        }
        long[] storage = tag.contains("s", Tag.TAG_LONG_ARRAY) ? tag.getLongArray("s") : null;

        Holder<Biome>[] bPalette = null;
        long[] bStorage = null;
        int bBits = 0;
        if (tag.contains("bp", Tag.TAG_LIST) && biomeGetter != null) {
            bBits = tag.getByte("bb") & 0xFF;
            ListTag bpal = tag.getList("bp", Tag.TAG_STRING);
            bPalette = (Holder<Biome>[]) new Holder<?>[bpal.size()];
            for (int i = 0; i < bpal.size(); i++) {
                ResourceLocation rloc = ResourceLocation.tryParse(bpal.getString(i));
                if (rloc != null) {
                    bPalette[i] = biomeGetter.get(ResourceKey.create(Registries.BIOME, rloc)).orElse(null);
                }
            }
            bStorage = tag.contains("bs", Tag.TAG_LONG_ARRAY) ? tag.getLongArray("bs") : null;
        }

        return new SectionSnapshot(palette, storage, bits, bPalette, bStorage, bBits);
    }

    public int paletteSize() { return palette.length; }
    public boolean isSingleState() { return storage == null; }
    public int storageWords() { return storage == null ? 0 : storage.length; }
}
