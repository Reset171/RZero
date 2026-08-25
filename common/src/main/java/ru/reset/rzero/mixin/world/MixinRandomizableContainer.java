package ru.reset.rzero.mixin.world;

import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.util.LootSeed;

@Mixin(RandomizableContainer.class)
public interface MixinRandomizableContainer {
    @Shadow long getLootTableSeed();
    @Shadow void setLootTableSeed(long seed);
    @Shadow Level getLevel();

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void rzero$fixDeterministicLootSeed(Player player, CallbackInfo ci) {
        if (!RZeroRuntime.checkpointPolicy().determinism().loot().containerLoot()) {
            return;
        }
        if (this.getLootTableSeed() == 0L) {
            Level level = this.getLevel();
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                long worldSeed = serverLevel.getSeed();
                long seed;
                if (this instanceof Entity entity) {
                    seed = LootSeed.mix(entity.getUUID(), worldSeed) ^ LootSeed.EQUIPMENT_SALT;
                } else {
                    RandomizableContainer container = (RandomizableContainer) this;
                    seed = LootSeed.mixPos(container.getBlockPos().asLong(), worldSeed);
                }
                this.setLootTableSeed(seed);
            }
        }
    }

}
