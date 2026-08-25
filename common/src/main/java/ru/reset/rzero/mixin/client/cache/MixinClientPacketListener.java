package ru.reset.rzero.mixin.client.cache;

import ru.reset.rzero.RZero;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.client.cache.RZeroClientCache;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

    @Inject(method = "handleAddEntity", at = @At("HEAD"))
    private void rzero$dropFakeBeforeRealArrives(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        RZeroClientCache.get().onRealEntityAdded(packet.getUUID());
    }

    @Inject(method = "queueLightRemoval", at = @At("HEAD"), cancellable = true)
    private void rzero$preventMeshWipe(net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        if (!RZeroClientCache.get().isInRollback()) return;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;

        int renderDistance = mc.options.getEffectiveRenderDistance();
        int dx = Math.abs(packet.pos().x - mc.player.chunkPosition().x);
        int dz = Math.abs(packet.pos().z - mc.player.chunkPosition().z);

        if (dx <= renderDistance && dz <= renderDistance) {
            ci.cancel();
        }
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void rzero$profileRespawnHead(net.minecraft.network.protocol.game.ClientboundRespawnPacket packet, CallbackInfo ci) {
        ru.reset.rzero.api.DevHooks.CLIENT_PROFILER.onRespawnStart();
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void rzero$instantInjectOnRespawn(net.minecraft.network.protocol.game.ClientboundRespawnPacket packet, CallbackInfo ci) {
        boolean interDim = RZeroClientCache.get().isInterDimensionalRollback();
        if (interDim) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                var snapDim = RZeroClientCache.get().snapshotDimension();
                if (snapDim != null && snapDim.equals(mc.level.dimension())) {
                    ru.reset.rzero.client.cache.RZeroVisualRollbackManager.execute(
                            mc, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            mc.player.getYRot(), mc.player.getXRot(), -1L, -1L);
                    RZeroClientCache.get().clearInterDimensionalRollback();
                    RZero.LOGGER.info("[RZero][cache] Seamless inter-dimensional cache injected into new ClientLevel!");
                } else {
                    RZeroClientCache.get().clearInterDimensionalRollback();
                }
            }
        }
        ru.reset.rzero.api.DevHooks.CLIENT_PROFILER.onRespawnEnd(interDim);
    }
}
