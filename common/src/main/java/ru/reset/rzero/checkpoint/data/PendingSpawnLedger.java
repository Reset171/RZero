package ru.reset.rzero.checkpoint.data;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class PendingSpawnLedger extends SavedData {

    public static final String FILE_ID = "rzero_spawn_ledger";

    public static final int MAX_CATCHUP_EPOCHS = 2;

    private final Long2LongMap chunkLastEpoch = new Long2LongOpenHashMap();

    private final Long2LongMap monsterLastEpoch = new Long2LongOpenHashMap();

    public PendingSpawnLedger() {
        this.chunkLastEpoch.defaultReturnValue(Long.MIN_VALUE);
        this.monsterLastEpoch.defaultReturnValue(Long.MIN_VALUE);
    }

    public static PendingSpawnLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(PendingSpawnLedger::new, PendingSpawnLedger::load, null), FILE_ID);
    }

    public long catchUpFrom(long chunkKey, long nowEpoch) {
        long last = this.chunkLastEpoch.get(chunkKey);
        this.chunkLastEpoch.put(chunkKey, nowEpoch);
        this.setDirty();

        if (last == Long.MIN_VALUE) {
            return nowEpoch + 1;
        }
        if (last >= nowEpoch) {
            return nowEpoch + 1;
        }
        long first = last + 1;
        long capped = nowEpoch - MAX_CATCHUP_EPOCHS + 1;
        return Math.max(first, capped);
    }

    public boolean monsterOwesReplay(long chunkKey, long dayEpoch) {
        if (this.monsterLastEpoch.get(chunkKey) == dayEpoch) {
            return false;
        }
        this.monsterLastEpoch.put(chunkKey, dayEpoch);
        this.setDirty();
        return true;
    }

    public void clearMonsterProgress() {
        if (!this.monsterLastEpoch.isEmpty()) {
            this.monsterLastEpoch.clear();
            this.setDirty();
        }
    }

    public static PendingSpawnLedger load(CompoundTag tag, HolderLookup.Provider lookup) {
        PendingSpawnLedger data = new PendingSpawnLedger();
        if (tag.contains("chunks") && tag.contains("epochs")) {
            long[] chunks = tag.getLongArray("chunks");
            long[] epochs = tag.getLongArray("epochs");
            int n = Math.min(chunks.length, epochs.length);
            for (int i = 0; i < n; i++) {
                data.chunkLastEpoch.put(chunks[i], epochs[i]);
            }
        }
        if (tag.contains("mdChunks") && tag.contains("mdEpochs")) {
            long[] chunks = tag.getLongArray("mdChunks");
            long[] epochs = tag.getLongArray("mdEpochs");
            int n = Math.min(chunks.length, epochs.length);
            for (int i = 0; i < n; i++) {
                data.monsterLastEpoch.put(chunks[i], epochs[i]);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        long[] chunks = new long[this.chunkLastEpoch.size()];
        long[] epochs = new long[this.chunkLastEpoch.size()];
        int i = 0;
        for (Long2LongMap.Entry e : this.chunkLastEpoch.long2LongEntrySet()) {
            chunks[i] = e.getLongKey();
            epochs[i] = e.getLongValue();
            i++;
        }
        tag.putLongArray("chunks", chunks);
        tag.putLongArray("epochs", epochs);

        long[] mChunks = new long[this.monsterLastEpoch.size()];
        long[] mEpochs = new long[this.monsterLastEpoch.size()];
        int j = 0;
        for (Long2LongMap.Entry e : this.monsterLastEpoch.long2LongEntrySet()) {
            mChunks[j] = e.getLongKey();
            mEpochs[j] = e.getLongValue();
            j++;
        }
        tag.putLongArray("mdChunks", mChunks);
        tag.putLongArray("mdEpochs", mEpochs);
        return tag;
    }
}
