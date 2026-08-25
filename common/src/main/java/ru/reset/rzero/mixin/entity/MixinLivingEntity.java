package ru.reset.rzero.mixin.entity;

import ru.reset.rzero.adaptive.AdaptiveSaveEngine;
import ru.reset.rzero.metrics.PlayerMetrics;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.SnapshotRegistry;
import ru.reset.rzero.util.LootSeed;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Unique
    private RandomSource rzero$savedDropRandom;

    @Redirect(method = "knockback",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"),
            require = 0)
    private double rzero$deterministicKnockbackRandom() {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!RZeroRuntime.checkpointPolicy().determinism().combat().knockbackRng()) {
            return self.level().getRandom().nextDouble();
        }
        return self.getRandom().nextDouble();
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void rzero$onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide() || entity.getServer() == null) {
            return;
        }

        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
            AdaptiveSaveEngine.lastBossDeathTick = entity.getServer().getTickCount();
            if (RZeroRuntime.adaptiveSettings().saveOnBossDefeat()) {
                var anchors = SnapshotRegistry.findAnchorPlayers(entity.getServer());
                if (!anchors.isEmpty()) {
                    ru.reset.rzero.checkpoint.CheckpointManager.setCheckpoint(anchors.get(0));
                }
            }
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"))
    private void rzero$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerMetrics.damageDealtAccumulator.merge(player.getUUID(), amount, Float::sum);
        } else if (entity instanceof ServerPlayer player) {
            PlayerMetrics.damageTakenAccumulator.merge(player.getUUID(), amount, Float::sum);
            if (source.getSourcePosition() != null) {
                net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                net.minecraft.world.phys.Vec3 dir = source.getSourcePosition().subtract(player.position()).normalize();
                if (look.dot(dir) < 0) {
                    PlayerMetrics.damageBehindAccumulator.merge(player.getUUID(), amount, Float::sum);
                }
            }
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void rzero$beginDeterministicDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if (!RZeroRuntime.checkpointPolicy().determinism().loot().mobDeathDrops()) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer) return;

        long seed = LootSeed.mix(self.getUUID(), level.getSeed()) ^ LootSeed.EQUIPMENT_SALT;
        ru.reset.rzero.access.IRZeroEntityRandom acc = (ru.reset.rzero.access.IRZeroEntityRandom) self;
        this.rzero$savedDropRandom = self.getRandom();
        acc.rzero$setRandom(RandomSource.create(seed));
    }

    @Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
    private void rzero$endDeterministicDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if (!RZeroRuntime.checkpointPolicy().determinism().loot().mobDeathDrops()) {
            return;
        }
        if (this.rzero$savedDropRandom == null) return;
        LivingEntity self = (LivingEntity) (Object) this;
        ru.reset.rzero.access.IRZeroEntityRandom acc = (ru.reset.rzero.access.IRZeroEntityRandom) self;
        acc.rzero$setRandom(this.rzero$savedDropRandom);
        this.rzero$savedDropRandom = null;
    }
}
