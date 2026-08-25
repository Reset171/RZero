package ru.reset.rzero.checkpoint.capture;

import ru.reset.rzero.event.EntityEvents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.pathfinder.Path;
import ru.reset.rzero.RZero;
import ru.reset.rzero.access.IRZeroRandomState;
import ru.reset.rzero.api.DevHooks;
import ru.reset.rzero.checkpoint.data.EntityRAMSnapshot;
import ru.reset.rzero.checkpoint.data.EntitySnapshot;
import ru.reset.rzero.engine.BrainCloner;
import ru.reset.rzero.serial.RZBlobEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

public final class EntityCapture {

    private static final Field NEXT_STEP;

    static {
        Field field = null;
        try {
            field = Entity.class.getDeclaredField("nextStep");
            field.setAccessible(true);
        } catch (Exception e) {
            RZero.LOGGER.warn("[RZero] Entity.nextStep is unavailable, step sounds may desync: {}",
                    e.getMessage());
        }
        NEXT_STEP = field;
    }

    private EntityCapture() {
    }

    public static float readNextStep(Entity entity) {
        if (NEXT_STEP == null) {
            return 0f;
        }
        try {
            return NEXT_STEP.getFloat(entity);
        } catch (IllegalAccessException e) {
            return 0f;
        }
    }

    public static void writeNextStep(Entity entity, float value) {
        if (NEXT_STEP == null) {
            return;
        }
        try {
            NEXT_STEP.setFloat(entity, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    public static EntitySnapshot buildSnapshot(Entity entity, long chunkKey) {
        CompoundTag nbt = new CompoundTag();
        if (!entity.save(nbt)) {
            return null;
        }
        EntitySnapshot snap = newSnapshot(entity, chunkKey);
        snap.setNbt(nbt);
        applyMobNavigation(entity, snap);
        applyRngState(entity, snap);
        return snap;
    }

    public static Captured captureWithRam(Entity entity,
                                          long chunkKey,
                                          RZBlobEncoder.Session session) {
        CompoundTag nbt = new CompoundTag();
        if (!entity.save(nbt)) {
            return null;
        }
        EntitySnapshot snap = newSnapshot(entity, chunkKey);
        session.submitEntity(snap, nbt);

        EntityRAMSnapshot ram = new EntityRAMSnapshot();
        captureBrainMemories(entity, ram);
        submitBrainClone(entity, ram, session);
        if (entity instanceof Mob mob) {
            captureMobRam(mob, ram);
            applyMobNavigation(entity, snap);
        }
        applyRngState(entity, snap);
        snap.captureLivingTimers(entity);
        return new Captured(snap, ram);
    }

    public record Captured(EntitySnapshot snapshot, EntityRAMSnapshot ram) {
    }

    private static EntitySnapshot newSnapshot(Entity entity, long chunkKey) {
        EntitySnapshot snap = new EntitySnapshot();
        snap.uuid = entity.getUUID();
        snap.entityId = entity.getId();
        snap.posX = entity.getX();
        snap.posY = entity.getY();
        snap.posZ = entity.getZ();
        snap.chunkKey = chunkKey;
        snap.tickCount = entity.tickCount;
        return snap;
    }

    private static void applyMobNavigation(Entity entity, EntitySnapshot snap) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        if (mob.getTarget() != null) {
            snap.targetUuid = mob.getTarget().getUUID();
        }
        Path path = mob.getNavigation().getPath();
        if (path == null) {
            return;
        }
        snap.hasPath = true;
        snap.pathTargetX = path.getTarget().getX();
        snap.pathTargetY = path.getTarget().getY();
        snap.pathTargetZ = path.getTarget().getZ();
        snap.pathSpeed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    private static void applyRngState(Entity entity, EntitySnapshot snap) {
        if (entity.getRandom() instanceof IRZeroRandomState rState) {
            snap.rngState = rState.rzero$getState();
        }
    }

    @SuppressWarnings("unchecked")
    private static void captureBrainMemories(Entity entity, EntityRAMSnapshot ram) {
        if (!(entity instanceof LivingEntity le) || le.getBrain() == null
                || le.getBrain().memories == null) {
            return;
        }
        for (var entry : le.getBrain().memories.entrySet()) {
            ram.memories.put(entry.getKey(),
                    (Optional<? extends net.minecraft.world.entity.ai.memory.ExpirableValue<?>>)
                            entry.getValue());
        }
    }

    private static void submitBrainClone(Entity entity,
                                         EntityRAMSnapshot ram,
                                         RZBlobEncoder.Session session) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        Object[] bundle = new Object[]{mob.getBrain(), mob.getNavigation()};
        session.submitTask(() -> {
            DevHooks.SAVE_PROFILER.beginPhase("entityClone");
            try {
                ram.clonedBrainAndNav = BrainCloner.deepClone(bundle);
            } catch (Exception ex) {
                RZero.LOGGER.error("[RZero] Error cloning BrainAndNav for " + entity.getUUID(), ex);
            } finally {
                DevHooks.SAVE_PROFILER.endPhase("entityClone");
            }
        });
    }

    private static void captureMobRam(Mob mob, EntityRAMSnapshot ram) {
        if (mob.getLookControl() != null) {
            ram.hasLookControl = true;
            ram.lookX = mob.getLookControl().wantedX;
            ram.lookY = mob.getLookControl().wantedY;
            ram.lookZ = mob.getLookControl().wantedZ;
            ram.lookYMaxRotSpeed = mob.getLookControl().yMaxRotSpeed;
            ram.lookXMaxRotAngle = mob.getLookControl().xMaxRotAngle;
            ram.lookAtCooldown = mob.getLookControl().lookAtCooldown;
        }
        if (mob.getMoveControl() != null) {
            ram.hasMoveControl = true;
            ram.moveX = mob.getMoveControl().wantedX;
            ram.moveY = mob.getMoveControl().wantedY;
            ram.moveZ = mob.getMoveControl().wantedZ;
            ram.moveSpeedModifier = mob.getMoveControl().speedModifier;
            ram.moveStrafeForwards = mob.getMoveControl().strafeForwards;
            ram.moveStrafeRight = mob.getMoveControl().strafeRight;
            ram.moveOperation = mob.getMoveControl().operation;
        }

        ram.walkDistO = mob.walkDistO;
        ram.walkDist = mob.walkDist;
        ram.moveDist = mob.moveDist;
        ram.flyDist = mob.flyDist;
        ram.nextStep = readNextStep(mob);

        ram.yHeadRot = mob.yHeadRot;
        ram.yHeadRotO = mob.yHeadRotO;
        ram.yBodyRot = mob.yBodyRot;
        ram.yBodyRotO = mob.yBodyRotO;
        ram.ambientSoundTime = mob.ambientSoundTime;
    }

    public static void applyMobRam(Mob mob, EntityRAMSnapshot ram) {
        if (ram.hasLookControl && mob.getLookControl() != null) {
            mob.getLookControl().wantedX = ram.lookX;
            mob.getLookControl().wantedY = ram.lookY;
            mob.getLookControl().wantedZ = ram.lookZ;
            mob.getLookControl().yMaxRotSpeed = ram.lookYMaxRotSpeed;
            mob.getLookControl().xMaxRotAngle = ram.lookXMaxRotAngle;
            mob.getLookControl().lookAtCooldown = ram.lookAtCooldown;
        }
        if (ram.hasMoveControl && mob.getMoveControl() != null) {
            mob.getMoveControl().wantedX = ram.moveX;
            mob.getMoveControl().wantedY = ram.moveY;
            mob.getMoveControl().wantedZ = ram.moveZ;
            mob.getMoveControl().speedModifier = ram.moveSpeedModifier;
            mob.getMoveControl().strafeForwards = ram.moveStrafeForwards;
            mob.getMoveControl().strafeRight = ram.moveStrafeRight;
            mob.getMoveControl().operation = ram.moveOperation;
        }

        mob.walkDistO = ram.walkDistO;
        mob.walkDist = ram.walkDist;
        mob.moveDist = ram.moveDist;
        mob.flyDist = ram.flyDist;
        writeNextStep(mob, ram.nextStep);

        mob.yHeadRot = ram.yHeadRot;
        mob.yHeadRotO = ram.yHeadRotO;
        mob.yBodyRot = ram.yBodyRot;
        mob.yBodyRotO = ram.yBodyRotO;
        mob.ambientSoundTime = ram.ambientSoundTime;
    }
}
