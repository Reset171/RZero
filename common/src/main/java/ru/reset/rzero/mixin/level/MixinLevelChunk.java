package ru.reset.rzero.mixin.level;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.SnapshotRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.block.state.BlockBehaviour;
import ru.reset.rzero.RZero;
import ru.reset.rzero.block.SectionSnapshot;
import ru.reset.rzero.checkpoint.data.CheckpointData;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk {
    @Shadow public abstract Level getLevel();
    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @Inject(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"))
    private void onSetBlockState(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        if (RZeroRuntime.isRestoring) return;
        Level level = this.getLevel();
        if (level.isClientSide()) return;
        CheckpointData data = SnapshotRegistry.activeSnapshots.get(level.dimension());
        if (data == null || data.anchorId == null) return;

        LevelChunk chunk = (LevelChunk) (Object) this;
        long chunkKey = chunk.getPos().toLong();
        SectionSnapshot[] arr = data.sectionSnapshots.get(chunkKey);
        if (arr == null) return;

        int sectionIdx = chunk.getSectionIndex(pos.getY());
        if (sectionIdx < 0 || sectionIdx >= arr.length) return;
        if (arr[sectionIdx] != null) return;

        LevelChunkSection sec = chunk.getSection(sectionIdx);
        arr[sectionIdx] = SectionSnapshot.capture(sec.getStates());
        data.setDirty();
    }

    @WrapOperation(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onPlace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V")
    )
    private void rzero$wrapOnPlace(BlockState instance, Level level, BlockPos pos, BlockState oldState, boolean isMoving, Operation<Void> original) {
        if (ru.reset.rzero.checkpoint.CheckpointManager.isRestoringChunk.get()) return;
        original.call(instance, level, pos, oldState, isMoving);
    }

    @WrapOperation(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onRemove(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V")
    )
    private void rzero$wrapOnRemove(BlockState instance, Level level, BlockPos pos, BlockState newState, boolean isMoving, Operation<Void> original) {
        if (ru.reset.rzero.checkpoint.CheckpointManager.isRestoringChunk.get()) return;
        original.call(instance, level, pos, newState, isMoving);
    }
}