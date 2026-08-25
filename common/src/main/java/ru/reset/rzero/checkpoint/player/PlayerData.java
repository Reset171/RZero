package ru.reset.rzero.checkpoint.player;

import ru.reset.rzero.RZero;
import ru.reset.rzero.checkpoint.data.OpenMenuSnapshot;
import ru.reset.rzero.config.RZeroCheckpointPolicy;

import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class PlayerData {
    public double x, y, z;
    public double motionX, motionY, motionZ;
    public float yRot, xRot, fallDistance;

    public float health;
    public float absorptionAmount;
    public int hurtTime;
    public int deathTime;
    public int hurtByTimestamp;
    public float lastHurt;
    public int attackStrengthTicker;
    public int noActionTime;
    public int noJumpDelay;
    public int swingTime;
    public boolean swinging;
    public String swingingArm;
    public CompoundTag useItem;
    public int useItemRemaining;
    public int arrowCount;
    public int stingerCount;
    public int tickCount;

    public int airSupply;
    public int fireTicks;
    public int portalCooldown;
    public boolean hasVisualFire;
    public boolean glowingTag;

    public int hunger;
    public float foodSaturationLevel;
    public float foodExhaustionLevel;
    public int foodTickTimer;
    public int experienceLevel;
    public float experienceProgress;
    public int totalExperience;
    public int enchantmentSeed;
    public int score;
    public short sleepCounter;
    public CompoundTag abilities;
    public CompoundTag shoulderLeft;
    public CompoundTag shoulderRight;
    public CompoundTag lastDeathLocation;
    public CompoundTag currentImpulseImpactPos;
    public boolean ignoreFallDamageFromCurrentImpulse;
    public int currentImpulseContextResetGraceTime;

    public CompoundTag wardenSpawnTracker;
    public CompoundTag enteredNetherPosition;
    public boolean seenCredits;
    public boolean spawnExtraParticlesOnFall;
    public Tag raidOmenPosition;
    public float respawnAngle;
    public CompoundTag recipeBook;
    public String gameMode;

    public String dimension;

    public double spawnX, spawnY, spawnZ;
    public int spawnBlockX, spawnBlockY, spawnBlockZ;
    public boolean hasSpawnBlockPos;
    public String spawnDimension;
    public boolean spawnForced;

    public ListTag inventory;
    public ListTag enderChestInventory;
    public int selectedSlot;

    public ListTag potionEffects;
    public CompoundTag advancements;

    public UUID vehicleUUID;

    public long[] rngState;

    public ListTag itemCooldowns;

    public OpenMenuSnapshot openMenu;

    public CompoundTag toNBT(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putDouble("mX", motionX);
        tag.putDouble("mY", motionY);
        tag.putDouble("mZ", motionZ);
        tag.putFloat("yRot", yRot);
        tag.putFloat("xRot", xRot);
        tag.putFloat("fallDist", fallDistance);
        tag.putInt("tick", tickCount);

        tag.putFloat("hp", health);
        tag.putFloat("absorption", absorptionAmount);
        tag.putInt("hurtT", hurtTime);
        tag.putInt("deathT", deathTime);
        tag.putInt("hurtByTs", hurtByTimestamp);
        tag.putFloat("lastHurt", lastHurt);
        tag.putInt("attackTicker", attackStrengthTicker);
        tag.putInt("noActT", noActionTime);
        tag.putInt("noJumpD", noJumpDelay);
        tag.putInt("swingT", swingTime);
        tag.putBoolean("swinging", swinging);
        if (swingingArm != null) tag.putString("swingArm", swingingArm);
        if (useItem != null) tag.put("useItem", useItem);
        tag.putInt("useRem", useItemRemaining);
        tag.putInt("arrows", arrowCount);
        tag.putInt("stingers", stingerCount);

        tag.putInt("air", airSupply);
        tag.putInt("fire", fireTicks);
        tag.putInt("portalCd", portalCooldown);
        tag.putBoolean("visFire", hasVisualFire);
        tag.putBoolean("glow", glowingTag);

        tag.putInt("hunger", hunger);
        tag.putFloat("sat", foodSaturationLevel);
        tag.putFloat("exh", foodExhaustionLevel);
        tag.putInt("foodTick", foodTickTimer);
        tag.putInt("xpL", experienceLevel);
        tag.putFloat("xpP", experienceProgress);
        tag.putInt("xpT", totalExperience);
        tag.putInt("xpS", enchantmentSeed);
        tag.putInt("score", score);
        tag.putShort("sleepC", sleepCounter);
        if (abilities != null) tag.put("abilities", abilities);
        if (shoulderLeft != null) tag.put("shoulderL", shoulderLeft);
        if (shoulderRight != null) tag.put("shoulderR", shoulderRight);
        if (lastDeathLocation != null) tag.put("lastDeath", lastDeathLocation);
        if (currentImpulseImpactPos != null) tag.put("impPos", currentImpulseImpactPos);
        tag.putBoolean("ignFallExp", ignoreFallDamageFromCurrentImpulse);
        tag.putInt("impGrace", currentImpulseContextResetGraceTime);

        if (wardenSpawnTracker != null) tag.put("warden", wardenSpawnTracker);
        if (enteredNetherPosition != null) tag.put("nether", enteredNetherPosition);
        tag.putBoolean("seenCredits", seenCredits);
        tag.putBoolean("extraParticles", spawnExtraParticlesOnFall);
        if (raidOmenPosition != null) tag.put("raidOmen", raidOmenPosition);
        tag.putFloat("respawnAng", respawnAngle);
        if (recipeBook != null) tag.put("recipeBook", recipeBook);
        if (gameMode != null) tag.putString("gameMode", gameMode);
        if (dimension != null) tag.putString("dim", dimension);
        if (spawnDimension != null && hasSpawnBlockPos) {
            tag.putDouble("spawnX", spawnX);
            tag.putDouble("spawnY", spawnY);
            tag.putDouble("spawnZ", spawnZ);
            tag.putInt("spawnBX", spawnBlockX);
            tag.putInt("spawnBY", spawnBlockY);
            tag.putInt("spawnBZ", spawnBlockZ);
            tag.putString("spawnDim", spawnDimension);
            tag.putBoolean("spawnF", spawnForced);
        }

        if (inventory != null) tag.put("inv", inventory);
        if (enderChestInventory != null) tag.put("ec", enderChestInventory);
        tag.putInt("sel", selectedSlot);

        if (potionEffects != null) tag.put("eff", potionEffects);
        if (advancements != null) tag.put("adv", advancements);

        if (vehicleUUID != null) tag.putUUID("veh", vehicleUUID);
        if (rngState != null) tag.putLongArray("rng", rngState);

        if (itemCooldowns != null) tag.put("cooldowns", itemCooldowns);
        if (openMenu != null) tag.put("openMenu", openMenu.toNBT(lookup));
        return tag;
    }

    public static PlayerData fromNBT(CompoundTag tag) {
        PlayerData d = new PlayerData();
        d.x = tag.getDouble("x");
        d.y = tag.getDouble("y");
        d.z = tag.getDouble("z");
        d.motionX = tag.contains("mX") ? tag.getDouble("mX") : tag.getDouble("motionX");
        d.motionY = tag.contains("mY") ? tag.getDouble("mY") : tag.getDouble("motionY");
        d.motionZ = tag.contains("mZ") ? tag.getDouble("mZ") : tag.getDouble("motionZ");
        d.yRot = tag.getFloat("yRot");
        d.xRot = tag.getFloat("xRot");
        d.fallDistance = tag.contains("fallDist") ? tag.getFloat("fallDist") : tag.getFloat("fallDistance");
        d.tickCount = tag.getInt("tick");

        d.health = tag.contains("hp") ? tag.getFloat("hp") : tag.getFloat("health");
        d.absorptionAmount = tag.getFloat("absorption");
        d.hurtTime = tag.getInt("hurtT");
        d.deathTime = tag.getInt("deathT");
        d.hurtByTimestamp = tag.getInt("hurtByTs");
        d.lastHurt = tag.getFloat("lastHurt");
        d.attackStrengthTicker = tag.getInt("attackTicker");
        d.noActionTime = tag.getInt("noActT");
        d.noJumpDelay = tag.getInt("noJumpD");
        d.swingTime = tag.getInt("swingT");
        d.swinging = tag.getBoolean("swinging");
        if (tag.contains("swingArm")) d.swingingArm = tag.getString("swingArm");
        if (tag.contains("useItem", Tag.TAG_COMPOUND)) d.useItem = tag.getCompound("useItem");
        d.useItemRemaining = tag.getInt("useRem");
        d.arrowCount = tag.getInt("arrows");
        d.stingerCount = tag.getInt("stingers");

        d.airSupply = tag.getInt("air");
        d.fireTicks = tag.contains("fire") ? tag.getInt("fire") : tag.getInt("fireTicks");
        d.portalCooldown = tag.getInt("portalCd");
        d.hasVisualFire = tag.getBoolean("visFire");
        d.glowingTag = tag.getBoolean("glow");

        d.hunger = tag.getInt("hunger");
        d.foodSaturationLevel = tag.getFloat("sat");
        d.foodExhaustionLevel = tag.getFloat("exh");
        d.foodTickTimer = tag.getInt("foodTick");
        d.experienceLevel = tag.contains("xpL") ? tag.getInt("xpL") : tag.getInt("experienceLevel");
        d.experienceProgress = tag.contains("xpP") ? tag.getFloat("xpP") : tag.getFloat("experienceProgress");
        d.totalExperience = tag.getInt("xpT");
        d.enchantmentSeed = tag.getInt("xpS");
        d.score = tag.getInt("score");
        d.sleepCounter = tag.getShort("sleepC");
        if (tag.contains("abilities", Tag.TAG_COMPOUND)) d.abilities = tag.getCompound("abilities");
        if (tag.contains("shoulderL", Tag.TAG_COMPOUND)) d.shoulderLeft = tag.getCompound("shoulderL");
        if (tag.contains("shoulderR", Tag.TAG_COMPOUND)) d.shoulderRight = tag.getCompound("shoulderR");
        if (tag.contains("lastDeath", Tag.TAG_COMPOUND)) d.lastDeathLocation = tag.getCompound("lastDeath");
        if (tag.contains("impPos", Tag.TAG_COMPOUND)) d.currentImpulseImpactPos = tag.getCompound("impPos");
        d.ignoreFallDamageFromCurrentImpulse = tag.getBoolean("ignFallExp");
        d.currentImpulseContextResetGraceTime = tag.getInt("impGrace");

        if (tag.contains("warden", Tag.TAG_COMPOUND)) d.wardenSpawnTracker = tag.getCompound("warden");
        if (tag.contains("nether", Tag.TAG_COMPOUND)) d.enteredNetherPosition = tag.getCompound("nether");
        d.seenCredits = tag.getBoolean("seenCredits");
        if (tag.contains("recipeBook", Tag.TAG_COMPOUND)) d.recipeBook = tag.getCompound("recipeBook");
        d.spawnExtraParticlesOnFall = tag.getBoolean("extraParticles");
        if (tag.contains("raidOmen")) d.raidOmenPosition = tag.get("raidOmen");
        d.respawnAngle = tag.getFloat("respawnAng");

        if (tag.contains("dim")) d.dimension = tag.getString("dim");
        if (tag.contains("gameMode")) d.gameMode = tag.getString("gameMode");
        if (tag.contains("spawnDim")) d.spawnDimension = tag.getString("spawnDim");
        else if (tag.contains("spawnDimension")) d.spawnDimension = tag.getString("spawnDimension");

        if (d.spawnDimension != null) {
            if (tag.contains("spawnX") && tag.contains("spawnY") && tag.contains("spawnZ")) {
                d.spawnX = tag.getDouble("spawnX");
                d.spawnY = tag.getDouble("spawnY");
                d.spawnZ = tag.getDouble("spawnZ");
            }
            if (tag.contains("spawnBX")) {
                d.spawnBlockX = tag.getInt("spawnBX");
                d.spawnBlockY = tag.getInt("spawnBY");
                d.spawnBlockZ = tag.getInt("spawnBZ");
                d.hasSpawnBlockPos = true;
            } else if (tag.contains("spawnX")) {
                d.spawnBlockX = (int) Math.floor(tag.getDouble("spawnX"));
                d.spawnBlockY = (int) Math.floor(tag.getDouble("spawnY"));
                d.spawnBlockZ = (int) Math.floor(tag.getDouble("spawnZ"));
                d.hasSpawnBlockPos = true;
            }
            d.spawnForced = tag.contains("spawnF") ? tag.getBoolean("spawnF") : tag.getBoolean("spawnForced");
        } else {
            d.hasSpawnBlockPos = false;
        }

        if (tag.contains("inv")) d.inventory = tag.getList("inv", Tag.TAG_COMPOUND);
        if (tag.contains("ec")) d.enderChestInventory = tag.getList("ec", Tag.TAG_COMPOUND);
        else if (tag.contains("enderChestInv")) d.enderChestInventory = tag.getList("enderChestInv", Tag.TAG_COMPOUND);
        d.selectedSlot = tag.contains("sel") ? tag.getInt("sel") : tag.getInt("selectedSlot");

        if (tag.contains("eff")) d.potionEffects = tag.getList("eff", Tag.TAG_COMPOUND);
        else if (tag.contains("potionEffects")) d.potionEffects = tag.getList("potionEffects", Tag.TAG_COMPOUND);
        if (tag.contains("adv")) d.advancements = tag.getCompound("adv");
        else if (tag.contains("advancements")) d.advancements = tag.getCompound("advancements");

        if (tag.hasUUID("veh")) d.vehicleUUID = tag.getUUID("veh");
        else if (tag.hasUUID("vehicleUUID")) d.vehicleUUID = tag.getUUID("vehicleUUID");
        if (tag.contains("rng")) d.rngState = tag.getLongArray("rng");
        else if (tag.contains("rngState")) d.rngState = tag.getLongArray("rngState");

        if (tag.contains("cooldowns", Tag.TAG_LIST)) d.itemCooldowns = tag.getList("cooldowns", Tag.TAG_COMPOUND);
        if (tag.contains("openMenu", Tag.TAG_COMPOUND)) d.openMenu = OpenMenuSnapshot.fromNBT(tag.getCompound("openMenu"));
        return d;
    }

    public static PlayerData captureFrom(ServerPlayer player, HolderLookup.Provider lookup) {
        PlayerData d = new PlayerData();

        d.x = player.getX();
        d.y = player.getY();
        d.z = player.getZ();
        Vec3 vel = player.getDeltaMovement();
        d.motionX = vel.x;
        d.motionY = vel.y;
        d.motionZ = vel.z;
        d.yRot = player.getYRot();
        d.xRot = player.getXRot();
        d.fallDistance = player.fallDistance;
        d.tickCount = player.tickCount;

        d.health = player.getHealth();
        d.absorptionAmount = player.getAbsorptionAmount();
        try {
            ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor lacc =
                    (ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor)(Object) player;
            d.hurtTime = lacc.rzero$getHurtTime();
            d.deathTime = lacc.rzero$getDeathTime();
            d.lastHurt = lacc.rzero$getLastHurt();
            d.attackStrengthTicker = lacc.rzero$getAttackStrengthTicker();
            d.noActionTime = lacc.rzero$getNoActionTime();
            d.noJumpDelay = lacc.rzero$getNoJumpDelay();
            d.swingTime = lacc.rzero$getSwingTime();
            d.swinging = lacc.rzero$getSwinging();
            InteractionHand hand = lacc.rzero$getSwingingArm();
            d.swingingArm = hand == null ? null : (hand == InteractionHand.MAIN_HAND ? "main" : "off");
            ItemStack ui = lacc.rzero$getUseItem();
            if (ui != null && !ui.isEmpty()) {
                Tag enc = ui.save(lookup);
                if (enc instanceof CompoundTag c) d.useItem = c;
            }
            d.useItemRemaining = lacc.rzero$getUseItemRemaining();
        } catch (Throwable ignored) {}
        try {
            d.hurtByTimestamp = ((ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor)(Object) player)
                    .rzero$getLastHurtByMobTimestamp();
        } catch (Throwable ignored) {
            d.hurtByTimestamp = player.getLastHurtByMobTimestamp();
        }
        d.arrowCount = player.getArrowCount();
        d.stingerCount = player.getStingerCount();

        d.airSupply = player.getAirSupply();
        d.fireTicks = player.getRemainingFireTicks();
        try {
            d.portalCooldown = ((ru.reset.rzero.mixin.entity.MixinEntityAccessor)(Object) player).rzero$getPortalCooldown();
        } catch (Throwable ignored) {}
        d.hasVisualFire = false;
        d.glowingTag = player.hasGlowingTag();

        d.hunger = player.getFoodData().getFoodLevel();
        d.foodSaturationLevel = player.getFoodData().getSaturationLevel();
        try {
            CompoundTag foodTag = new CompoundTag();
            player.getFoodData().addAdditionalSaveData(foodTag);
            d.foodExhaustionLevel = foodTag.getFloat("foodExhaustionLevel");
            d.foodTickTimer = foodTag.getInt("foodTickTimer");
        } catch (Throwable ignored) {}
        d.experienceLevel = player.experienceLevel;
        d.experienceProgress = player.experienceProgress;
        d.totalExperience = player.totalExperience;
        try {
            ru.reset.rzero.mixin.player.MixinPlayer macc =
                    (ru.reset.rzero.mixin.player.MixinPlayer)(Object) player;
            d.enchantmentSeed = macc.rzero$getEnchantmentSeed();
            d.sleepCounter = (short) macc.rzero$getSleepCounter();
            Abilities abil = macc.rzero$getAbilities();
            if (abil != null) {
                CompoundTag aTag = new CompoundTag();
                abil.addSaveData(aTag);
                d.abilities = aTag;
            }
            Optional<GlobalPos> ldl = macc.rzero$getLastDeathLocation();
            if (ldl != null && ldl.isPresent()) {
                DataResult<Tag> enc = GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, ldl.get());
                enc.result().ifPresent(t -> {
                    if (t instanceof CompoundTag c) d.lastDeathLocation = c;
                });
            }
        } catch (Throwable ignored) {}
        d.score = player.getScore();
        if (!player.getShoulderEntityLeft().isEmpty()) d.shoulderLeft = player.getShoulderEntityLeft();
        if (!player.getShoulderEntityRight().isEmpty()) d.shoulderRight = player.getShoulderEntityRight();

        try {
            ru.reset.rzero.mixin.player.MixinServerPlayerAccessor sacc =
                    (ru.reset.rzero.mixin.player.MixinServerPlayerAccessor)(Object) player;
            WardenSpawnTracker wst = sacc.rzero$getWardenSpawnTracker();
            if (wst != null) {
                DataResult<Tag> enc = WardenSpawnTracker.CODEC.encodeStart(NbtOps.INSTANCE, wst);
                enc.result().ifPresent(t -> {
                    if (t instanceof CompoundTag c) d.wardenSpawnTracker = c;
                });
            }
            Vec3 nether = sacc.rzero$getEnteredNetherPosition();
            if (nether != null) {
                CompoundTag n = new CompoundTag();
                n.putDouble("x", nether.x);
                n.putDouble("y", nether.y);
                n.putDouble("z", nether.z);
                d.enteredNetherPosition = n;
            }
            d.seenCredits = player.seenCredits;
            d.spawnExtraParticlesOnFall = sacc.rzero$getSpawnExtraParticlesOnFall();
            BlockPos raidOmen = sacc.rzero$getRaidOmenPosition();
            if (raidOmen != null) {
                RZero.LOGGER.info("[RZero] Saving raidOmenPosition: " + raidOmen);
                DataResult<Tag> enc = BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, raidOmen);
                enc.result().ifPresent(t -> {
                    RZero.LOGGER.info("[RZero] Encoded raidOmenPosition tag: " + t.toString());
                    d.raidOmenPosition = t;
                });
            } else {
                RZero.LOGGER.info("[RZero] Player has NO raidOmenPosition at snapshot time!");
            }
            d.respawnAngle = sacc.rzero$getRespawnAngle();
        } catch (Throwable ignored) {}
        try {
            ru.reset.rzero.access.IRZeroDirtyCache cache =
                    (ru.reset.rzero.access.IRZeroDirtyCache)(Object) player.getRecipeBook();
            CompoundTag cached = cache.rzero$getCachedNbt();
            if (cache.rzero$isDirty() || cached == null) {
                cached = player.getRecipeBook().toNbt();
                cache.rzero$setCachedNbt(cached);
                cache.rzero$markClean();
            }
            d.recipeBook = cached;
        } catch (Throwable ignored) {}

        d.dimension = player.serverLevel().dimension().location().toString();
        d.gameMode = player.gameMode.getGameModeForPlayer().getName();

        BlockPos spawn = player.getRespawnPosition();
        ResourceKey<net.minecraft.world.level.Level> spawnDim = player.getRespawnDimension();
        if (spawn != null && spawnDim != null) {
            d.spawnBlockX = spawn.getX();
            d.spawnBlockY = spawn.getY();
            d.spawnBlockZ = spawn.getZ();
            d.spawnX = spawn.getX();
            d.spawnY = spawn.getY();
            d.spawnZ = spawn.getZ();
            d.hasSpawnBlockPos = true;
            d.spawnDimension = spawnDim.location().toString();
            d.spawnForced = player.isRespawnForced();
        } else {
            d.spawnDimension = null;
            d.hasSpawnBlockPos = false;
        }

        d.inventory = player.getInventory().save(new ListTag());
        d.enderChestInventory = player.getEnderChestInventory().createTag(player.registryAccess());
        d.selectedSlot = player.getInventory().selected;

        ListTag eff = new ListTag();
        for (var ef : player.getActiveEffects()) eff.add(ef.save());
        d.potionEffects = eff;

        if (player.getVehicle() != null) {
            d.vehicleUUID = player.getVehicle().getUUID();
        }

        if (player.getRandom() instanceof ru.reset.rzero.access.IRZeroRandomState rs) {
            d.rngState = rs.rzero$getState();
        }

        try {
            ru.reset.rzero.mixin.entity.MixinItemCooldowns icc =
                    (ru.reset.rzero.mixin.entity.MixinItemCooldowns)(Object) player.getCooldowns();
            int curTick = icc.rzero$getTickCount();
            ListTag cdList = new ListTag();
            for (var entry : icc.rzero$getCooldowns().entrySet()) {
                Item item = entry.getKey();
                Object inst = entry.getValue();
                java.lang.reflect.Field fStart = inst.getClass().getDeclaredField("startTime");
                java.lang.reflect.Field fEnd = inst.getClass().getDeclaredField("endTime");
                fStart.setAccessible(true);
                fEnd.setAccessible(true);
                int startTime = fStart.getInt(inst);
                int endTime = fEnd.getInt(inst);
                int remaining = endTime - curTick;
                if (remaining > 0) {
                    CompoundTag c = new CompoundTag();
                    ResourceLocation rid = BuiltInRegistries.ITEM.getKey(item);
                    if (rid != null) {
                        c.putString("id", rid.toString());
                        c.putInt("rem", remaining);
                        c.putInt("total", endTime - startTime);
                        cdList.add(c);
                    }
                }
            }
            d.itemCooldowns = cdList;
        } catch (Throwable ignored) {}

        d.openMenu = OpenMenuSnapshot.capture(player, lookup);

        try {
            ru.reset.rzero.mixin.player.MixinPlayer pacc =
                    (ru.reset.rzero.mixin.player.MixinPlayer)(Object) player;
            net.minecraft.world.phys.Vec3 impact = pacc.rzero$getCurrentImpulseImpactPos();
            if (impact != null) {
                ListTag list = new ListTag();
                list.add(net.minecraft.nbt.DoubleTag.valueOf(impact.x));
                list.add(net.minecraft.nbt.DoubleTag.valueOf(impact.y));
                list.add(net.minecraft.nbt.DoubleTag.valueOf(impact.z));
                CompoundTag wrap = new CompoundTag();
                wrap.put("v", list);
                d.currentImpulseImpactPos = wrap;
            }
            d.ignoreFallDamageFromCurrentImpulse = pacc.rzero$getIgnoreFallDamageFromCurrentImpulse();
            d.currentImpulseContextResetGraceTime = pacc.rzero$getCurrentImpulseContextResetGraceTime();
        } catch (Throwable ignored) {}

        return d;
    }

    public void applyTo(ServerPlayer player,
                        net.minecraft.server.MinecraftServer server,
                        HolderLookup.Provider lookup,
                        RZeroCheckpointPolicy policy) {
        RZeroCheckpointPolicy.Players playersPolicy = policy.rollback().players();
        net.minecraft.server.level.ServerLevel targetLevel = server.getLevel(
                ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                        ResourceLocation.parse(this.dimension == null
                                ? "minecraft:overworld" : this.dimension)));
        if (targetLevel == null) targetLevel = player.serverLevel();
        if (playersPolicy.position()) {
            if (player.isSleeping()) {
                player.stopSleepInBed(true, false);
            }
            player.stopRiding();
            player.teleportTo(targetLevel, x, y, z, yRot, xRot);
            targetLevel.getChunkSource().broadcast(player, new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(player));
            player.fallDistance = fallDistance;
        }
        if (playersPolicy.motion()) {
            player.setDeltaMovement(new Vec3(motionX, motionY, motionZ));
        }
        player.tickCount = tickCount;

        try {
            ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor lacc =
                    (ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor)(Object) player;
            lacc.rzero$setHurtTime(hurtTime);
            lacc.rzero$setDeathTime(deathTime);
            lacc.rzero$setDead(health <= 0.0f);
            lacc.rzero$setLastHurt(lastHurt);
            lacc.rzero$setAttackStrengthTicker(attackStrengthTicker);
            lacc.rzero$setNoActionTime(noActionTime);
            lacc.rzero$setNoJumpDelay(noJumpDelay);
            lacc.rzero$setSwingTime(swingTime);
            lacc.rzero$setSwinging(swinging);
            if (swingingArm != null) {
                lacc.rzero$setSwingingArm("off".equals(swingingArm) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            } else {
                lacc.rzero$setSwingingArm(null);
            }
            ItemStack ui = ItemStack.EMPTY;
            if (useItem != null && !useItem.isEmpty()) {
                ui = ItemStack.parse(lookup, useItem).orElse(ItemStack.EMPTY);
            }
            lacc.rzero$setUseItem(ui);
            lacc.rzero$setUseItemRemaining(useItemRemaining);
        } catch (Throwable ignored) {}

        if (playersPolicy.health()) {
            player.setHealth(health);
            player.setAbsorptionAmount(absorptionAmount);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));
        }
        try {
            ((ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor)(Object) player)
                    .rzero$setLastHurtByMobTimestamp(hurtByTimestamp);
        } catch (Throwable ignored) {}
        player.setArrowCount(arrowCount);
        player.setStingerCount(stingerCount);
        player.setAirSupply(airSupply);
        player.setRemainingFireTicks(fireTicks);
        try {
            ((ru.reset.rzero.mixin.entity.MixinEntityAccessor)(Object) player).rzero$setPortalCooldown(portalCooldown);
        } catch (Throwable ignored) {}
        player.setGlowingTag(glowingTag);

        if (playersPolicy.hunger()) {
            try {
                CompoundTag foodTag = new CompoundTag();
                foodTag.putInt("foodLevel", hunger);
                foodTag.putFloat("foodSaturationLevel", foodSaturationLevel);
                foodTag.putFloat("foodExhaustionLevel", foodExhaustionLevel);
                foodTag.putInt("foodTickTimer", foodTickTimer);
                player.getFoodData().readAdditionalSaveData(foodTag);
            } catch (Throwable ignored) {
                player.getFoodData().setFoodLevel(hunger);
            }
        }

        if (playersPolicy.experience()) {
            player.experienceLevel = experienceLevel;
            player.experienceProgress = experienceProgress;
            player.totalExperience = totalExperience;
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(
                    experienceProgress, totalExperience, experienceLevel));
        }

        try {
            ru.reset.rzero.mixin.player.MixinPlayer macc =
                    (ru.reset.rzero.mixin.player.MixinPlayer)(Object) player;
            macc.rzero$setEnchantmentSeed(enchantmentSeed);
            macc.rzero$setSleepCounter(sleepCounter);
            if (playersPolicy.abilities() && abilities != null) {
                Abilities a = macc.rzero$getAbilities();
                if (a != null) {
                    a.loadSaveData(abilities);
                    player.onUpdateAbilities();
                }
            }
            if (lastDeathLocation != null) {
                GlobalPos.CODEC.parse(NbtOps.INSTANCE, lastDeathLocation).result()
                        .ifPresent(gp -> macc.rzero$setLastDeathLocation(Optional.of(gp)));
            } else {
                macc.rzero$setLastDeathLocation(Optional.empty());
            }
        } catch (Throwable ignored) {}

        if (playersPolicy.score()) {
            player.setScore(score);
        }

        try {
            ru.reset.rzero.mixin.player.MixinPlayer macc =
                    (ru.reset.rzero.mixin.player.MixinPlayer)(Object) player;
            macc.rzero$setShoulderEntityLeft(shoulderLeft != null ? shoulderLeft : new CompoundTag());
            macc.rzero$setShoulderEntityRight(shoulderRight != null ? shoulderRight : new CompoundTag());
        } catch (Throwable ignored) {}

        try {
            ru.reset.rzero.mixin.player.MixinServerPlayerAccessor sacc =
                    (ru.reset.rzero.mixin.player.MixinServerPlayerAccessor)(Object) player;
            if (wardenSpawnTracker != null) {
                WardenSpawnTracker.CODEC.parse(NbtOps.INSTANCE, wardenSpawnTracker).result()
                        .ifPresent(sacc::rzero$setWardenSpawnTracker);
            }
            if (enteredNetherPosition != null) {
                sacc.rzero$setEnteredNetherPosition(new Vec3(
                        enteredNetherPosition.getDouble("x"),
                        enteredNetherPosition.getDouble("y"),
                        enteredNetherPosition.getDouble("z")));
            } else {
                sacc.rzero$setEnteredNetherPosition(null);
            }
            player.seenCredits = seenCredits;
            sacc.rzero$setSpawnExtraParticlesOnFall(spawnExtraParticlesOnFall);
            if (raidOmenPosition != null) {
                RZero.LOGGER.info("[RZero] raidOmenPosition tag is present: " + raidOmenPosition.toString());
                BlockPos.CODEC.parse(NbtOps.INSTANCE, raidOmenPosition).result()
                        .ifPresent(pos -> {
                            RZero.LOGGER.info("[RZero] Successfully parsed raidOmenPosition: " + pos);
                            sacc.rzero$setRaidOmenPosition(pos);
                        });
            } else {
                RZero.LOGGER.info("[RZero] raidOmenPosition tag is NULL!");
                sacc.rzero$setRaidOmenPosition(null);
            }
            sacc.rzero$setRespawnAngle(respawnAngle);
        } catch (Throwable ignored) {}
        try {
            if (playersPolicy.recipeBook() && recipeBook != null) {
                player.getRecipeBook().fromNbt(recipeBook, server.getRecipeManager());
                player.getRecipeBook().sendInitialRecipeBook(player);
            }
        } catch (Throwable ignored) {}

        if (gameMode != null) {
            switch (gameMode.toLowerCase(java.util.Locale.ROOT)) {
                case "survival" -> player.setGameMode(GameType.SURVIVAL);
                case "creative" -> player.setGameMode(GameType.CREATIVE);
                case "adventure" -> player.setGameMode(GameType.ADVENTURE);
                case "spectator" -> player.setGameMode(GameType.SPECTATOR);
                default -> {}
            }
        }

        if (playersPolicy.spawnPoint()) {
            if (spawnDimension != null && hasSpawnBlockPos) {
                ResourceKey<net.minecraft.world.level.Level> sd = ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        ResourceLocation.parse(spawnDimension));
                player.setRespawnPosition(sd,
                        new BlockPos(spawnBlockX, spawnBlockY, spawnBlockZ),
                        respawnAngle, spawnForced, false);
            } else {
                player.setRespawnPosition(null, null, 0.0f, false, false);
            }
        }

        if (playersPolicy.potionEffects()) {
            player.removeAllEffects();
            if (potionEffects != null) {
                for (int i = 0; i < potionEffects.size(); i++) {
                    var effect = net.minecraft.world.effect.MobEffectInstance.load(potionEffects.getCompound(i));
                    if (effect != null) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
                    }
                }
            }
        }

        if (playersPolicy.inventory()) {
            player.getInventory().clearContent();
            if (inventory != null) player.getInventory().load(inventory);
            player.getInventory().selected = selectedSlot;
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(selectedSlot));
        }
        if (playersPolicy.enderChest()) {
            player.getEnderChestInventory().clearContent();
            if (enderChestInventory != null) {
                player.getEnderChestInventory().fromTag(enderChestInventory, player.registryAccess());
            }
        }


        if (rngState != null && player.getRandom() instanceof ru.reset.rzero.access.IRZeroRandomState rs) {
            rs.rzero$setState(rngState);
        }

        if (playersPolicy.cooldowns()) {
            try {
                ru.reset.rzero.mixin.entity.MixinItemCooldowns icc =
                        (ru.reset.rzero.mixin.entity.MixinItemCooldowns)(Object) player.getCooldowns();
                for (var existing : new java.util.ArrayList<>(icc.rzero$getCooldowns().keySet())) {
                    player.getCooldowns().removeCooldown(existing);
                }
                if (itemCooldowns != null) {
                    for (int i = 0; i < itemCooldowns.size(); i++) {
                        CompoundTag c = itemCooldowns.getCompound(i);
                        ResourceLocation rid = ResourceLocation.parse(c.getString("id"));
                        Item item = BuiltInRegistries.ITEM.get(rid);
                        if (item != null) {
                            int rem = c.getInt("rem");
                            if (rem > 0) player.getCooldowns().addCooldown(item, rem);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        try {
            java.util.Collection<net.minecraft.world.entity.ai.attributes.AttributeInstance> dirty =
                    new java.util.ArrayList<>();
            for (net.minecraft.world.entity.ai.attributes.AttributeInstance ai
                    : player.getAttributes().getSyncableAttributes()) {
                dirty.add(ai);
            }
            if (!dirty.isEmpty()) {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(
                        player.getId(), dirty));
            }
        } catch (Throwable ignored) {}

        try {
            ru.reset.rzero.mixin.player.MixinPlayer pacc =
                    (ru.reset.rzero.mixin.player.MixinPlayer)(Object) player;
            net.minecraft.world.phys.Vec3 impact = null;
            if (currentImpulseImpactPos != null && currentImpulseImpactPos.contains("v", Tag.TAG_LIST)) {
                ListTag l = currentImpulseImpactPos.getList("v", Tag.TAG_DOUBLE);
                if (l.size() == 3) {
                    impact = new net.minecraft.world.phys.Vec3(
                            l.getDouble(0), l.getDouble(1), l.getDouble(2));
                }
            }
            pacc.rzero$setCurrentImpulseImpactPos(impact);
            pacc.rzero$setIgnoreFallDamageFromCurrentImpulse(ignoreFallDamageFromCurrentImpulse);
            pacc.rzero$setCurrentImpulseContextResetGraceTime(currentImpulseContextResetGraceTime);
        } catch (Throwable ignored) {}

        if (playersPolicy.openMenu() && openMenu != null) {
            try {
                openMenu.restore(player, player.serverLevel(), lookup);
            } catch (Throwable t) {
                RZero.LOGGER.warn(
                        "[RZero] OpenMenu restore failed: {}", t.getMessage());
            }
        }
    }
}
