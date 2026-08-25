package ru.reset.rzero.checkpoint.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class EntityRAMSnapshot {
    public final Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = new LinkedHashMap<>();

    public boolean hasLookControl = false;
    public double lookX, lookY, lookZ;
    public float lookYMaxRotSpeed, lookXMaxRotAngle;
    public int lookAtCooldown;

    public boolean hasMoveControl = false;
    public double moveX, moveY, moveZ;
    public double moveSpeedModifier;
    public float moveStrafeForwards, moveStrafeRight;
    public net.minecraft.world.entity.ai.control.MoveControl.Operation moveOperation;

    public float walkDistO, walkDist, moveDist, flyDist;
    public float nextStep = 1.0F;
    public float yHeadRot, yHeadRotO, yBodyRot, yBodyRotO;
    public int ambientSoundTime;

    public boolean hasBatTarget = false;
    public int batTargetX, batTargetY, batTargetZ;

    public Object[] clonedBrainAndNav;

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        if (hasLookControl) {
            tag.putBoolean("hl", true);
            tag.putDouble("lx", lookX);
            tag.putDouble("ly", lookY);
            tag.putDouble("lz", lookZ);
            tag.putFloat("lyMax", lookYMaxRotSpeed);
            tag.putFloat("lxMax", lookXMaxRotAngle);
            tag.putInt("lCd", lookAtCooldown);
        }
        if (hasMoveControl) {
            tag.putBoolean("hm", true);
            tag.putDouble("mx", moveX);
            tag.putDouble("my", moveY);
            tag.putDouble("mz", moveZ);
            tag.putDouble("msp", moveSpeedModifier);
            tag.putFloat("msf", moveStrafeForwards);
            tag.putFloat("msr", moveStrafeRight);
            if (moveOperation != null) tag.putString("mop", moveOperation.name());
        }

        tag.putFloat("wDo", walkDistO);
        tag.putFloat("wD", walkDist);
        tag.putFloat("mD", moveDist);
        tag.putFloat("fD", flyDist);
        tag.putFloat("nS", nextStep);
        tag.putFloat("yhR", yHeadRot);
        tag.putFloat("yhRo", yHeadRotO);
        tag.putFloat("ybR", yBodyRot);
        tag.putFloat("ybRo", yBodyRotO);
        tag.putInt("aST", ambientSoundTime);

        if (hasBatTarget) {
            tag.putBoolean("hBT", true);
            tag.putInt("btX", batTargetX);
            tag.putInt("btY", batTargetY);
            tag.putInt("btZ", batTargetZ);
        }

        return tag;
    }

    public static EntityRAMSnapshot fromNBT(CompoundTag tag) {
        EntityRAMSnapshot s = new EntityRAMSnapshot();
        if (tag.getBoolean("hl")) {
            s.hasLookControl = true;
            s.lookX = tag.getDouble("lx");
            s.lookY = tag.getDouble("ly");
            s.lookZ = tag.getDouble("lz");
            s.lookYMaxRotSpeed = tag.getFloat("lyMax");
            s.lookXMaxRotAngle = tag.getFloat("lxMax");
            s.lookAtCooldown = tag.getInt("lCd");
        }
        if (tag.getBoolean("hm")) {
            s.hasMoveControl = true;
            s.moveX = tag.getDouble("mx");
            s.moveY = tag.getDouble("my");
            s.moveZ = tag.getDouble("mz");
            s.moveSpeedModifier = tag.getDouble("msp");
            s.moveStrafeForwards = tag.getFloat("msf");
            s.moveStrafeRight = tag.getFloat("msr");
            if (tag.contains("mop")) {
                try {
                    s.moveOperation = MoveControl.Operation.valueOf(tag.getString("mop"));
                } catch (IllegalArgumentException ignored) {
                    s.moveOperation = MoveControl.Operation.WAIT;
                }
            }
        }

        s.walkDistO = tag.getFloat("wDo");
        s.walkDist = tag.getFloat("wD");
        s.moveDist = tag.getFloat("mD");
        s.flyDist = tag.getFloat("fD");
        s.nextStep = tag.contains("nS") ? tag.getFloat("nS") : 1.0F;
        s.yHeadRot = tag.getFloat("yhR");
        s.yHeadRotO = tag.getFloat("yhRo");
        s.yBodyRot = tag.getFloat("ybR");
        s.yBodyRotO = tag.getFloat("ybRo");
        s.ambientSoundTime = tag.getInt("aST");

        if (tag.getBoolean("hBT")) {
            s.hasBatTarget = true;
            s.batTargetX = tag.getInt("btX");
            s.batTargetY = tag.getInt("btY");
            s.batTargetZ = tag.getInt("btZ");
        }

        return s;
    }
}
