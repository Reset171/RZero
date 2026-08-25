package ru.reset.rzero.mixin.client.cache;

import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.client.cache.RZeroClientCache;
import ru.reset.rzero.access.IRZeroLevelChunkSection;

@Mixin(LevelChunkSection.class)
public abstract class MixinLevelChunkSection implements IRZeroLevelChunkSection {

    @Shadow private short nonEmptyBlockCount;
    @Shadow private short tickingBlockCount;
    @Shadow private short tickingFluidCount;

    @Inject(method = "recalcBlockCounts", at = @At("HEAD"), cancellable = true)
    private void rzero$skipRecalc(CallbackInfo ci) {
        if (RZeroClientCache.isCapturing) {
            ci.cancel();
        }
    }

    @Override
    public short rzero$getNonEmptyBlockCount() { return this.nonEmptyBlockCount; }

    @Override
    public short rzero$getTickingBlockCount() { return this.tickingBlockCount; }

    @Override
    public short rzero$getTickingFluidCount() { return this.tickingFluidCount; }

    @Override
    public void rzero$copyCountsFrom(LevelChunkSection src) {
        IRZeroLevelChunkSection mixinSrc = (IRZeroLevelChunkSection) (Object) src;
        this.nonEmptyBlockCount = mixinSrc.rzero$getNonEmptyBlockCount();
        this.tickingBlockCount = mixinSrc.rzero$getTickingBlockCount();
        this.tickingFluidCount = mixinSrc.rzero$getTickingFluidCount();
    }
}
