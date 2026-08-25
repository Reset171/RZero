package ru.reset.rzero.mixin.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.engine.SpawnCatchUp;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevelCatchUp {

    @Inject(method = "startTickingChunk", at = @At("RETURN"))
    private void rzero$catchUpSpawns(LevelChunk chunk, CallbackInfo ci) {
        SpawnCatchUp.onChunkStartTicking((ServerLevel) (Object) this, chunk);
    }
}
