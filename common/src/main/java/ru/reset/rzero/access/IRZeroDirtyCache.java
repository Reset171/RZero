package ru.reset.rzero.access;

import net.minecraft.nbt.CompoundTag;

public interface IRZeroDirtyCache {
    boolean rzero$isDirty();
    void rzero$markDirty();
    void rzero$markClean();
    CompoundTag rzero$getCachedNbt();
    void rzero$setCachedNbt(CompoundTag tag);
}
