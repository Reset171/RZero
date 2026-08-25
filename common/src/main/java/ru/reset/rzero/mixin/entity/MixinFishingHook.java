package ru.reset.rzero.mixin.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class MixinFishingHook {

    @Mutable @Shadow @Final private int luck;
    @Mutable @Shadow @Final private int lureSpeed;
    @Shadow private boolean biting;
    @Shadow private int outOfWaterTime;
    @Shadow private int life;
    @Shadow private int nibble;
    @Shadow private int timeUntilLured;
    @Shadow private int timeUntilHooked;
    @Shadow private float fishAngle;
    @Shadow private boolean openWater;
    @Shadow private FishingHook.FishHookState currentState;

    @Shadow public abstract Player getPlayerOwner();

    @Unique
    private java.util.UUID rzero$savedOwnerUUID;

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void rzero$addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        tag.putInt("rzero$luck", this.luck);
        tag.putInt("rzero$lureSpeed", this.lureSpeed);
        tag.putBoolean("rzero$biting", this.biting);
        tag.putInt("rzero$outOfWaterTime", this.outOfWaterTime);
        tag.putInt("rzero$life", this.life);
        tag.putInt("rzero$nibble", this.nibble);
        tag.putInt("rzero$timeUntilLured", this.timeUntilLured);
        tag.putInt("rzero$timeUntilHooked", this.timeUntilHooked);
        tag.putFloat("rzero$fishAngle", this.fishAngle);
        tag.putBoolean("rzero$openWater", this.openWater);
        if (this.currentState != null) {
            tag.putString("rzero$currentState", this.currentState.name());
        }
        
        Player owner = this.getPlayerOwner();
        if (owner != null) {
            tag.putUUID("rzero$ownerUUID", owner.getUUID());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void rzero$readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (tag.contains("rzero$luck")) {
            this.luck = tag.getInt("rzero$luck");
            this.lureSpeed = tag.getInt("rzero$lureSpeed");
            this.biting = tag.getBoolean("rzero$biting");
            this.outOfWaterTime = tag.getInt("rzero$outOfWaterTime");
            this.life = tag.getInt("rzero$life");
            this.nibble = tag.getInt("rzero$nibble");
            this.timeUntilLured = tag.getInt("rzero$timeUntilLured");
            this.timeUntilHooked = tag.getInt("rzero$timeUntilHooked");
            this.fishAngle = tag.getFloat("rzero$fishAngle");
            this.openWater = tag.getBoolean("rzero$openWater");
            if (tag.contains("rzero$currentState")) {
                this.currentState = FishingHook.FishHookState.valueOf(tag.getString("rzero$currentState"));
            }
        }
        if (tag.hasUUID("rzero$ownerUUID")) {
            this.rzero$savedOwnerUUID = tag.getUUID("rzero$ownerUUID");
        }
    }

    @Inject(method = "getAddEntityPacket", at = @At("HEAD"))
    private void rzero$relinkOwnerBeforePacket(net.minecraft.server.level.ServerEntity serverEntity, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>> cir) {
        if (this.getPlayerOwner() == null && this.rzero$savedOwnerUUID != null) {
            net.minecraft.world.level.Level level = ((net.minecraft.world.entity.Entity)(Object)this).level();
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.minecraft.world.entity.Entity owner = serverLevel.getEntity(this.rzero$savedOwnerUUID);
                if (owner instanceof Player p) {
                    ((net.minecraft.world.entity.projectile.FishingHook)(Object)this).setOwner(p);
                    p.fishing = (net.minecraft.world.entity.projectile.FishingHook)(Object)this;
                    this.rzero$savedOwnerUUID = null;
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void rzero$relinkPlayer(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        FishingHook self = (FishingHook)(Object) this;
        if (!self.level().isClientSide) {
            Player player = this.getPlayerOwner();
            if (player != null && player.fishing != self) {
                player.fishing = self;
            }
        }
    }
}
