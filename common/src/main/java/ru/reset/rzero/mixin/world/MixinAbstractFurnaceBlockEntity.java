package ru.reset.rzero.mixin.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MixinAbstractFurnaceBlockEntity {

    @Redirect(method = "createExperience",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"),
            require = 0)
    private static double rzero$deterministicFurnaceXpRoll(
            ServerLevel level, Vec3 pos, int count, float frac) {
        long worldSeed = level.getSeed();
        long seed = Double.doubleToLongBits(pos.x) * 3129871L
                ^ Double.doubleToLongBits(pos.y) * 116129781L
                ^ Double.doubleToLongBits(pos.z)
                ^ ((long) count << 17)
                ^ Float.floatToIntBits(frac)
                ^ worldSeed;
        net.minecraft.util.RandomSource r = new net.minecraft.world.level.levelgen.LegacyRandomSource(seed);
        return r.nextDouble();
    }
}
