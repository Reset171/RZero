package ru.reset.rzero.util;

import net.minecraft.world.entity.Entity;

public final class EntitySortBuffers {
    public long[] msb = new long[1024];
    public long[] lsb = new long[1024];
    public int[] indices = new int[1024];
    public Entity[] temp = new Entity[1024];
}
