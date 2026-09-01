package ru.reset.rzero.mixin.player;

import ru.reset.rzero.anchor.RollbackCooldown;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;
import ru.reset.rzero.runtime.SnapshotRegistry;
import ru.reset.rzero.metrics.PlayerMetrics;
import ru.reset.rzero.metrics.MetricsWriter;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.RZero;
import ru.reset.rzero.util.DetOrder;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void rzero$onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        boolean hasAnchor = DetOrder.anyValueMatches(SnapshotRegistry.activeSnapshots,
                data -> data.isAnchor(player.getUUID()));

        if (hasAnchor) {
            int cooldownSeconds = RZeroRuntime.anchorSettings().rollbackCooldownSeconds();
            if (!RollbackCooldown.tryConsume(cooldownSeconds, player.server.getTickCount())) {
                RZero.logInfo("[RZero] Rollback suppressed by cooldown for {} ({}s)",
                        player.getName().getString(), cooldownSeconds);
                return;
            }

            it.unimi.dsi.fastutil.longs.LongArrayList deaths = PlayerMetrics.deathTimestamps.computeIfAbsent(player.getUUID(), k -> new it.unimi.dsi.fastutil.longs.LongArrayList());
            deaths.add((long) player.server.getTickCount());

            MetricsWriter.write(player, 100.0, 0, 0, 0, 0f, 0, "DEATH_ROLLBACK", java.util.Collections.emptyList(), java.util.Collections.emptyList(), 0);

            player.setHealth(player.getMaxHealth());
            player.removeAllEffects();
            RestoreQueues.pendingDeathRollback = player.getUUID();
            PlayerMetrics.lastRespawnOrCheckpointTick.put(player.getUUID(), (long) player.server.getTickCount());
            
            ci.cancel();
        }
    }
}
