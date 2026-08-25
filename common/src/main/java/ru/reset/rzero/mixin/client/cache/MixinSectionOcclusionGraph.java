package ru.reset.rzero.mixin.client.cache;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.reset.rzero.client.cache.RZeroClientCache;
import ru.reset.rzero.client.cache.mesh.MeshCacheSupport;

import java.util.concurrent.ExecutorService;

@Mixin(SectionOcclusionGraph.class)
public class MixinSectionOcclusionGraph {
    @Redirect(
            method = "scheduleFullUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;backgroundExecutor()Ljava/util/concurrent/ExecutorService;")
    )
    private ExecutorService rzero$syncExecutor() {
        if (MeshCacheSupport.isSupported() && RZeroClientCache.get().isInRollback()) {
            return MeshCacheSupport.SAME_THREAD_EXECUTOR;
        }
        return net.minecraft.Util.backgroundExecutor();
    }
}
