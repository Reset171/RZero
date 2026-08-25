package ru.reset.rzero.mixin.player;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface MixinServerPlayerAccessor {
    @Accessor("wardenSpawnTracker")
    WardenSpawnTracker rzero$getWardenSpawnTracker();

    @Accessor("wardenSpawnTracker")
    void rzero$setWardenSpawnTracker(WardenSpawnTracker value);

    @Accessor("enteredNetherPosition")
    Vec3 rzero$getEnteredNetherPosition();

    @Accessor("enteredNetherPosition")
    void rzero$setEnteredNetherPosition(Vec3 value);

    @Accessor("raidOmenPosition")
    BlockPos rzero$getRaidOmenPosition();

    @Accessor("raidOmenPosition")
    void rzero$setRaidOmenPosition(BlockPos value);

    @Accessor("spawnExtraParticlesOnFall")
    boolean rzero$getSpawnExtraParticlesOnFall();

    @Accessor("spawnExtraParticlesOnFall")
    void rzero$setSpawnExtraParticlesOnFall(boolean value);

    @Accessor("respawnAngle")
    float rzero$getRespawnAngle();

    @Accessor("respawnAngle")
    void rzero$setRespawnAngle(float value);
}
