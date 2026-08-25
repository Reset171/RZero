package ru.reset.rzero.checkpoint.data;

import net.minecraft.nbt.CompoundTag;

public final class AdaptiveState {
    public long lastSaveTick;
    public long currentInterval;
    public double currentIntensity;
    public double peakIntensity;
    public int relaxTicks;
    public long lastBossDeathTick;
    public int entityIdCounter;

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("lastSaveTick", lastSaveTick);
        tag.putLong("currentInterval", currentInterval);
        tag.putDouble("currentIntensity", currentIntensity);
        tag.putDouble("peakIntensity", peakIntensity);
        tag.putInt("relaxTicks", relaxTicks);
        tag.putLong("lastBossDeathTick", lastBossDeathTick);
        tag.putInt("entityIdCounter", entityIdCounter);
        return tag;
    }

    public static AdaptiveState fromNBT(CompoundTag tag) {
        AdaptiveState as = new AdaptiveState();
        as.lastSaveTick = tag.getLong("lastSaveTick");
        as.currentInterval = tag.getLong("currentInterval");
        as.currentIntensity = tag.getDouble("currentIntensity");
        as.peakIntensity = tag.getDouble("peakIntensity");
        as.relaxTicks = tag.getInt("relaxTicks");
        as.lastBossDeathTick = tag.getLong("lastBossDeathTick");
        as.entityIdCounter = tag.getInt("entityIdCounter");
        return as;
    }
}