package ru.reset.rzero.mixin.level;

import net.minecraft.Util;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.util.EntitySortBuffers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(ServerChunkCache.class)
public abstract class MixinServerChunkCache {
    @Shadow public abstract Level getLevel();

    @Unique
    private static final ThreadLocal<List<Entity>> rzero$REUSABLE_ENTITY_LIST =
            ThreadLocal.withInitial(ArrayList::new);

    @Unique
    private static final ThreadLocal<EntitySortBuffers> rzero$ENTITY_BUFFERS =
            ThreadLocal.withInitial(EntitySortBuffers::new);

    @Unique
    private long[] rzero$incomingKeys = new long[1024];
    @Unique
    private long[] rzero$cachedChunkKeys = new long[0];
    @Unique
    private it.unimi.dsi.fastutil.longs.LongOpenHashSet rzero$cachedChunkKeySet = null;
    @Unique
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Object> rzero$chunkItemMap =
            new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();

    @Redirect(
            method = "tickChunks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;shuffle(Ljava/util/List;Lnet/minecraft/util/RandomSource;)V")
    )
    private void rzero$deterministicChunkOrder(List<?> chunks, RandomSource ignored) {
        int size = chunks.size();
        if (size <= 1) {
            return;
        }
        if (!RZeroRuntime.naturalSpawnPolicy().enabled()) {
            Util.shuffle(chunks, ignored);
            return;
        }

        if (rzero$incomingKeys.length < size) {
            rzero$incomingKeys = new long[Math.max(size, rzero$incomingKeys.length * 2)];
        }
        long[] keys = rzero$incomingKeys;

        @SuppressWarnings("unchecked")
        List<Object> mutable = (List<Object>) chunks;

        rzero$chunkItemMap.clear();
        boolean cacheValid = rzero$cachedChunkKeySet != null && rzero$cachedChunkKeys.length == size;
        for (int i = 0; i < size; i++) {
            Object item = mutable.get(i);
            long key = ((MixinChunkAndHolderAccessor) item).rzero$getChunk().getPos().toLong();
            keys[i] = key;
            rzero$chunkItemMap.put(key, item);
            if (cacheValid && !rzero$cachedChunkKeySet.contains(key)) {
                cacheValid = false;
            }
        }

        if (!cacheValid) {
            long[] sorted = new long[size];
            System.arraycopy(keys, 0, sorted, 0, size);
            it.unimi.dsi.fastutil.longs.LongArrays.quickSort(sorted);
            rzero$cachedChunkKeys = sorted;
            rzero$cachedChunkKeySet = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(sorted);
        }

        for (int i = 0; i < size; i++) {
            mutable.set(i, rzero$chunkItemMap.get(rzero$cachedChunkKeys[i]));
        }

        Util.shuffle(mutable, RandomSource.create(this.getLevel().getGameTime()));
    }

    @ModifyArg(
            method = "tickChunks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;createState(ILjava/lang/Iterable;Lnet/minecraft/world/level/NaturalSpawner$ChunkGetter;Lnet/minecraft/world/level/LocalMobCapCalculator;)Lnet/minecraft/world/level/NaturalSpawner$SpawnState;"),
            index = 1
    )
    private Iterable<Entity> rzero$canonicalEntityOrder(Iterable<Entity> entities) {
        if (!RZeroRuntime.naturalSpawnPolicy().enabled()) {
            return entities;
        }
        List<Entity> sorted = rzero$REUSABLE_ENTITY_LIST.get();
        sorted.clear();
        entities.forEach(sorted::add);
        int size = sorted.size();
        if (size > 1) {
            EntitySortBuffers buffers = rzero$ENTITY_BUFFERS.get();
            long[] msb = buffers.msb;
            long[] lsb = buffers.lsb;
            int[] indices = buffers.indices;
            Entity[] temp = buffers.temp;
            if (msb.length < size) {
                int newCap = Math.max(size, msb.length * 2);
                msb = new long[newCap];
                lsb = new long[newCap];
                indices = new int[newCap];
                temp = new Entity[newCap];
                buffers.msb = msb;
                buffers.lsb = lsb;
                buffers.indices = indices;
                buffers.temp = temp;
            }

            for (int i = 0; i < size; i++) {
                Entity entity = sorted.get(i);
                UUID uuid = entity.getUUID();
                msb[i] = uuid.getMostSignificantBits();
                lsb[i] = uuid.getLeastSignificantBits();
                temp[i] = entity;
                indices[i] = i;
            }

            final long[] finalMsb = msb;
            final long[] finalLsb = lsb;
            it.unimi.dsi.fastutil.ints.IntArrays.quickSort(indices, 0, size, (i1, i2) -> {
                int cmp = Long.compare(finalMsb[i1], finalMsb[i2]);
                return cmp != 0 ? cmp : Long.compare(finalLsb[i1], finalLsb[i2]);
            });

            for (int i = 0; i < size; i++) {
                sorted.set(i, temp[indices[i]]);
            }
        }
        return sorted;
    }
}
