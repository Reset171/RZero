package ru.reset.rzero.checkpoint.data;

import ru.reset.rzero.RZero;
import ru.reset.rzero.adaptive.AdaptiveSaveEngine;
import ru.reset.rzero.runtime.RestoreQueues;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class RZeroAdaptiveData extends SavedData {
    public static final String FILE_ID = "rzero_data_adaptive";

    public RZeroAdaptiveData() {}

    public static RZeroAdaptiveData load(CompoundTag tag, HolderLookup.Provider lookup) {
        RZeroAdaptiveData data = new RZeroAdaptiveData();
        if (tag.contains("adaptive", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            AdaptiveState as = AdaptiveState.fromNBT(tag.getCompound("adaptive"));
            AdaptiveSaveEngine.restoreAdaptiveState(as);
            ru.reset.rzero.engine.EntityIdCounter.set(as.entityIdCounter);
        } else if (tag.contains("director", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            AdaptiveState as = AdaptiveState.fromNBT(tag.getCompound("director"));
            AdaptiveSaveEngine.restoreAdaptiveState(as);
            ru.reset.rzero.engine.EntityIdCounter.set(as.entityIdCounter);
        }
        if (tag.contains("pendingOfflineRollbacks", net.minecraft.nbt.Tag.TAG_LIST)) {
            RestoreQueues.pendingOfflineRollbacks.clear();
            net.minecraft.nbt.ListTag list = tag.getList("pendingOfflineRollbacks", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag ct = list.getCompound(i);
                java.util.UUID uuid = ct.getUUID("uuid");
                ru.reset.rzero.checkpoint.player.PlayerData pd = ru.reset.rzero.checkpoint.player.PlayerData.fromNBT(ct.getCompound("data"));
                RestoreQueues.pendingOfflineRollbacks.put(uuid, pd);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        AdaptiveState as = AdaptiveSaveEngine.captureAdaptiveState();
        as.entityIdCounter = ru.reset.rzero.engine.EntityIdCounter.get();
        tag.put("adaptive", as.toNBT());
        
        net.minecraft.nbt.ListTag pendingList = new net.minecraft.nbt.ListTag();
        java.util.TreeMap<java.util.UUID, ru.reset.rzero.checkpoint.player.PlayerData> sortedMap = new java.util.TreeMap<>(RestoreQueues.pendingOfflineRollbacks);
        for (java.util.Map.Entry<java.util.UUID, ru.reset.rzero.checkpoint.player.PlayerData> entry : sortedMap.entrySet()) {
            CompoundTag ct = new CompoundTag();
            ct.putUUID("uuid", entry.getKey());
            ct.put("data", entry.getValue().toNBT(lookup));
            pendingList.add(ct);
        }
        tag.put("pendingOfflineRollbacks", pendingList);
        return tag;
    }

    public static Factory<RZeroAdaptiveData> factory() {
        return new Factory<>(
            RZeroAdaptiveData::new,
            RZeroAdaptiveData::load,
            null
        );
    }

    public static void markDirty(MinecraftServer server) {
        if (server != null) {
            try {
                RZeroAdaptiveData data = server.overworld().getDataStorage().computeIfAbsent(factory(), FILE_ID);
                if (data != null) {
                    data.setDirty();
                }
            } catch (Exception e) {
                RZero.LOGGER.error("[RZero] Failed to save AdaptiveData", e);
            }
        }
    }
}