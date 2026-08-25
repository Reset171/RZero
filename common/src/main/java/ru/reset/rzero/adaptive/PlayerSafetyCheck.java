package ru.reset.rzero.adaptive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class PlayerSafetyCheck {

    private static final int GROUND_PROBE_DEPTH = 256;

    private static final float FALL_DAMAGE_GRACE = 3.0F;

    private PlayerSafetyCheck() {
    }

    public static boolean isPlayerSafe(ServerPlayer player) {
        if (player.isDeadOrDying()
                || player.isInLava()
                || player.isOnFire()
                || player.hasEffect(MobEffects.LEVITATION)) {
            return false;
        }
        if (!player.onGround() && !willSurviveLanding(player)) {
            return false;
        }
        return !hasImminentExplosion(player);
    }

    private static boolean willSurviveLanding(ServerPlayer player) {
        Level level = player.level();
        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos(player.getX(), player.getY(), player.getZ());
        boolean foundGround = false;
        float expectedDamage = 0;

        for (int i = 0; i < GROUND_PROBE_DEPTH; i++) {
            pos.move(Direction.DOWN);
            if (pos.getY() < level.getMinBuildHeight()) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (state.getFluidState().is(FluidTags.LAVA)) {
                return false;
            }
            foundGround = true;
            if (state.getFluidState().isEmpty()) {
                float totalFall = player.fallDistance + (float) (player.getY() - pos.getY());
                expectedDamage = totalFall - FALL_DAMAGE_GRACE;
            }
            break;
        }
        return foundGround && expectedDamage < player.getHealth();
    }

    private static boolean hasImminentExplosion(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(10.0);
        List<Entity> threats = player.level().getEntities(player, box,
                e -> (e instanceof PrimedTnt)
                        || (e instanceof Creeper c && (c.getSwellDir() > 0 || c.isIgnited())));
        return !threats.isEmpty();
    }
}