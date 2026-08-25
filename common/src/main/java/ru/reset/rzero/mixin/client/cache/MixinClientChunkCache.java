package ru.reset.rzero.mixin.client.cache;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.reset.rzero.client.cache.RZeroClientCache;
import ru.reset.rzero.client.cache.RZeroFakeChunk;
import ru.reset.rzero.client.cache.SpatialGrid;
import ru.reset.rzero.client.cache.ext.IRZeroClientChunkCache;

import java.util.function.Consumer;

@Mixin(ClientChunkCache.class)
public abstract class MixinClientChunkCache implements IRZeroClientChunkCache {

    @Shadow @Final private LevelChunk emptyChunk;

    @Unique
    private SpatialGrid<RZeroFakeChunk> rzero$grid;

    @ModifyReturnValue(
            method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("RETURN")
    )
    private LevelChunk rzero$substituteFakeChunk(LevelChunk original, int x, int z, ChunkStatus status, boolean orEmpty) {
        SpatialGrid<RZeroFakeChunk> grid = rzero$grid;
        if (grid == null) return original;

        if (original != (orEmpty ? this.emptyChunk : null)) return original;

        RZeroFakeChunk fake = grid.get(x, z);
        return fake != null ? fake : original;
    }

    @Inject(
            method = "replaceWithPacketData",
            at = @At("HEAD")
    )
    private void rzero$onRealChunkArrived(int x, int z, FriendlyByteBuf buf, CompoundTag heightmaps,
                                          Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
                                          CallbackInfoReturnable<LevelChunk> cir) {
        RZeroClientCache.get().onRealChunkArrived(x, z);
    }

    @Override
    public void rzero$setGrid(SpatialGrid<RZeroFakeChunk> grid) {
        this.rzero$grid = grid;
    }
}
