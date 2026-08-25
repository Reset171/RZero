package ru.reset.rzero.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import java.util.Optional;

public record RZeroCheckpointPolicy(Rollback rollback, Determinism determinism) {

    public static final Codec<RZeroCheckpointPolicy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Rollback.CODEC.optionalFieldOf("rollback", Rollback.defaults()).forGetter(RZeroCheckpointPolicy::rollback),
            Determinism.CODEC.optionalFieldOf("determinism", Determinism.defaults()).forGetter(RZeroCheckpointPolicy::determinism)
    ).apply(instance, RZeroCheckpointPolicy::new));

    public RZeroCheckpointPolicy {
        rollback = rollback == null ? Rollback.defaults() : rollback.sanitize();
        determinism = determinism == null ? Determinism.defaults() : determinism.sanitize();
    }

    public static RZeroCheckpointPolicy defaults() {
        return new RZeroCheckpointPolicy(Rollback.defaults(), Determinism.defaults());
    }

    public RZeroCheckpointPolicy sanitize() {
        return new RZeroCheckpointPolicy(rollback, determinism);
    }

    public JsonObject toJson() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this)
                .result()
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .orElseGet(JsonObject::new);
    }

    public static RZeroCheckpointPolicy fromJson(JsonObject obj) {
        if (obj == null) return defaults();
        return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(RZeroCheckpointPolicy::defaults);
    }

    public CompoundTag toNbt() {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this)
                .result()
                .orElseGet(CompoundTag::new);
    }

    public static RZeroCheckpointPolicy fromNbt(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return defaults();
        return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(RZeroCheckpointPolicy::defaults);
    }

    public record Rollback(
            boolean blocks,
            boolean blockEntities,
            boolean blockTicks,
            boolean fluidTicks,
            boolean blockEvents,
            boolean pois,
            Players players,
            Entities entities,
            World world) {

        public static final Codec<Rollback> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("blocks", true).forGetter(Rollback::blocks),
                Codec.BOOL.optionalFieldOf("block_entities", true).forGetter(Rollback::blockEntities),
                Codec.BOOL.optionalFieldOf("block_ticks", true).forGetter(Rollback::blockTicks),
                Codec.BOOL.optionalFieldOf("fluid_ticks", true).forGetter(Rollback::fluidTicks),
                Codec.BOOL.optionalFieldOf("block_events", true).forGetter(Rollback::blockEvents),
                Codec.BOOL.optionalFieldOf("pois", true).forGetter(Rollback::pois),
                Players.CODEC.optionalFieldOf("players", Players.defaults()).forGetter(Rollback::players),
                Entities.CODEC.optionalFieldOf("entities", Entities.defaults()).forGetter(Rollback::entities),
                World.CODEC.optionalFieldOf("world", World.defaults()).forGetter(Rollback::world)
        ).apply(instance, Rollback::new));

        public Rollback {
            players = players == null ? Players.defaults() : players.sanitize();
            entities = entities == null ? Entities.defaults() : entities.sanitize();
            world = world == null ? World.defaults() : world.sanitize();
        }

        public static Rollback defaults() {
            return new Rollback(true, true, true, true, true, true,
                    Players.defaults(), Entities.defaults(), World.defaults());
        }

        public Rollback sanitize() {
            return new Rollback(blocks, blockEntities, blockTicks, fluidTicks, blockEvents, pois,
                    players, entities, world);
        }

        public JsonObject toJson() {
            return CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
        }

        public static Rollback fromJson(JsonObject obj) {
            if (obj == null) return defaults();
            return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(Rollback::defaults);
        }

        public CompoundTag toNbt() {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static Rollback fromNbt(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) return defaults();
            return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(Rollback::defaults);
        }
    }

    public record Players(
            boolean position,
            boolean motion,
            boolean health,
            boolean hunger,
            boolean experience,
            boolean inventory,
            boolean enderChest,
            boolean potionEffects,
            boolean cooldowns,
            boolean openMenu,
            boolean abilities,
            boolean recipeBook,
            boolean spawnPoint,
            boolean score,
            boolean preserveNewPlayerInventory) {

        public static final Codec<Players> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("position", true).forGetter(Players::position),
                Codec.BOOL.optionalFieldOf("motion", true).forGetter(Players::motion),
                Codec.BOOL.optionalFieldOf("health", true).forGetter(Players::health),
                Codec.BOOL.optionalFieldOf("hunger", true).forGetter(Players::hunger),
                Codec.BOOL.optionalFieldOf("experience", true).forGetter(Players::experience),
                Codec.BOOL.optionalFieldOf("inventory", true).forGetter(Players::inventory),
                Codec.BOOL.optionalFieldOf("ender_chest", true).forGetter(Players::enderChest),
                Codec.BOOL.optionalFieldOf("potion_effects", true).forGetter(Players::potionEffects),
                Codec.BOOL.optionalFieldOf("cooldowns", true).forGetter(Players::cooldowns),
                Codec.BOOL.optionalFieldOf("open_menu", true).forGetter(Players::openMenu),
                Codec.BOOL.optionalFieldOf("abilities", true).forGetter(Players::abilities),
                Codec.BOOL.optionalFieldOf("recipe_book", true).forGetter(Players::recipeBook),
                Codec.BOOL.optionalFieldOf("spawn_point", true).forGetter(Players::spawnPoint),
                Codec.BOOL.optionalFieldOf("score", true).forGetter(Players::score),
                Codec.BOOL.optionalFieldOf("preserve_new_player_inventory", false).forGetter(Players::preserveNewPlayerInventory)
        ).apply(instance, Players::new));

        public static Players defaults() {
            return new Players(true, true, true, true, true, true, true, true,
                    true, true, true, true, true, true, false);
        }

        public Players sanitize() {
            return this;
        }

        public JsonObject toJson() {
            return CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
        }

        public static Players fromJson(JsonObject obj) {
            if (obj == null) return defaults();
            return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(Players::defaults);
        }

        public CompoundTag toNbt() {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static Players fromNbt(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) return defaults();
            return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(Players::defaults);
        }
    }

    public record Entities(
            boolean presence,
            boolean entityId,
            boolean tickCount,
            boolean rngState,
            boolean livingTimers,
            boolean target,
            boolean navigation,
            boolean brainRam,
            boolean passengers,
            boolean mobRamCache,
            boolean droppedItems,
            boolean experienceOrbs) {

        public static final Codec<Entities> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("presence", true).forGetter(Entities::presence),
                Codec.BOOL.optionalFieldOf("entity_id", true).forGetter(Entities::entityId),
                Codec.BOOL.optionalFieldOf("tick_count", true).forGetter(Entities::tickCount),
                Codec.BOOL.optionalFieldOf("rng_state", true).forGetter(Entities::rngState),
                Codec.BOOL.optionalFieldOf("living_timers", true).forGetter(Entities::livingTimers),
                Codec.BOOL.optionalFieldOf("target", true).forGetter(Entities::target),
                Codec.BOOL.optionalFieldOf("navigation", true).forGetter(Entities::navigation),
                Codec.BOOL.optionalFieldOf("brain_ram", true).forGetter(Entities::brainRam),
                Codec.BOOL.optionalFieldOf("passengers", true).forGetter(Entities::passengers),
                Codec.BOOL.optionalFieldOf("mob_ram_cache", true).forGetter(Entities::mobRamCache),
                Codec.BOOL.optionalFieldOf("dropped_items", true).forGetter(Entities::droppedItems),
                Codec.BOOL.optionalFieldOf("experience_orbs", true).forGetter(Entities::experienceOrbs)
        ).apply(instance, Entities::new));

        public static Entities defaults() {
            return new Entities(true, true, true, true, true, true, true, true, true, true, true, true);
        }

        public Entities sanitize() {
            return this;
        }

        public JsonObject toJson() {
            return CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
        }

        public static Entities fromJson(JsonObject obj) {
            if (obj == null) return defaults();
            return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(Entities::defaults);
        }

        public CompoundTag toNbt() {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static Entities fromNbt(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) return defaults();
            return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(Entities::defaults);
        }
    }

    public record World(
            boolean time,
            boolean weather,
            boolean raids,
            boolean dragonFight,
            boolean scoreboard,
            boolean levelRng,
            ServerGlobals serverGlobals) {

        public static final Codec<World> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("time", true).forGetter(World::time),
                Codec.BOOL.optionalFieldOf("weather", true).forGetter(World::weather),
                Codec.BOOL.optionalFieldOf("raids", true).forGetter(World::raids),
                Codec.BOOL.optionalFieldOf("dragon_fight", true).forGetter(World::dragonFight),
                Codec.BOOL.optionalFieldOf("scoreboard", true).forGetter(World::scoreboard),
                Codec.BOOL.optionalFieldOf("level_rng", true).forGetter(World::levelRng),
                ServerGlobals.CODEC.optionalFieldOf("server_globals", ServerGlobals.defaults()).forGetter(World::serverGlobals)
        ).apply(instance, World::new));

        public World {
            serverGlobals = serverGlobals == null ? ServerGlobals.defaults() : serverGlobals.sanitize();
        }

        public static World defaults() {
            return new World(true, true, true, true, true, true, ServerGlobals.defaults());
        }

        public World sanitize() {
            return new World(time, weather, raids, dragonFight, scoreboard, levelRng, serverGlobals);
        }

        public JsonObject toJson() {
            return CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
        }

        public static World fromJson(JsonObject obj) {
            if (obj == null) return defaults();
            return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(World::defaults);
        }

        public CompoundTag toNbt() {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static World fromNbt(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) return defaults();
            return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(World::defaults);
        }
    }

    public record ServerGlobals(
            boolean bossbars,
            boolean forcedChunks,
            boolean worldBorder,
            boolean gameRules,
            boolean randomSequences,
            boolean savedData,
            boolean serverTickCount,
            boolean shufflingCounter) {

        public static final Codec<ServerGlobals> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("bossbars", true).forGetter(ServerGlobals::bossbars),
                Codec.BOOL.optionalFieldOf("forced_chunks", true).forGetter(ServerGlobals::forcedChunks),
                Codec.BOOL.optionalFieldOf("world_border", true).forGetter(ServerGlobals::worldBorder),
                Codec.BOOL.optionalFieldOf("game_rules", true).forGetter(ServerGlobals::gameRules),
                Codec.BOOL.optionalFieldOf("random_sequences", true).forGetter(ServerGlobals::randomSequences),
                Codec.BOOL.optionalFieldOf("saved_data", true).forGetter(ServerGlobals::savedData),
                Codec.BOOL.optionalFieldOf("server_tick_count", true).forGetter(ServerGlobals::serverTickCount),
                Codec.BOOL.optionalFieldOf("shuffling_counter", true).forGetter(ServerGlobals::shufflingCounter)
        ).apply(instance, ServerGlobals::new));

        public static ServerGlobals defaults() {
            return new ServerGlobals(true, true, true, true, true, true, true, true);
        }

        public ServerGlobals sanitize() {
            return this;
        }

        public JsonObject toJson() {
            return CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
        }

        public static ServerGlobals fromJson(JsonObject obj) {
            if (obj == null) return defaults();
            return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(ServerGlobals::defaults);
        }

        public CompoundTag toNbt() {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static ServerGlobals fromNbt(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) return defaults();
            return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(ServerGlobals::defaults);
        }
    }

    public record Determinism(
            NaturalSpawn naturalSpawn,
            MobAi mobAi,
            Loot loot,
            Spawns spawns,
            Combat combat) {

        public static final Codec<Determinism> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                NaturalSpawn.CODEC.optionalFieldOf("natural_spawn", NaturalSpawn.defaults()).forGetter(Determinism::naturalSpawn),
                MobAi.CODEC.optionalFieldOf("mob_ai", MobAi.defaults()).forGetter(Determinism::mobAi),
                Loot.CODEC.optionalFieldOf("loot", Loot.defaults()).forGetter(Determinism::loot),
                Spawns.CODEC.optionalFieldOf("spawns", Spawns.defaults()).forGetter(Determinism::spawns),
                Combat.CODEC.optionalFieldOf("combat", Combat.defaults()).forGetter(Determinism::combat)
        ).apply(instance, Determinism::new));

        public Determinism {
            naturalSpawn = naturalSpawn == null ? NaturalSpawn.defaults() : naturalSpawn.sanitize();
            mobAi = mobAi == null ? MobAi.defaults() : mobAi.sanitize();
            loot = loot == null ? Loot.defaults() : loot.sanitize();
            spawns = spawns == null ? Spawns.defaults() : spawns.sanitize();
            combat = combat == null ? Combat.defaults() : combat.sanitize();
        }

        public static Determinism defaults() {
            return new Determinism(
                    NaturalSpawn.defaults(),
                    MobAi.defaults(),
                    Loot.defaults(),
                    Spawns.defaults(),
                    Combat.defaults());
        }

        public Determinism sanitize() {
            return new Determinism(naturalSpawn, mobAi, loot, spawns, combat);
        }

        public JsonObject toJson() {
            return CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
        }

        public static Determinism fromJson(JsonObject obj) {
            if (obj == null) return defaults();
            return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(Determinism::defaults);
        }

        public CompoundTag toNbt() {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static Determinism fromNbt(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) return defaults();
            return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(Determinism::defaults);
        }
    }

    public record NaturalSpawn(
            boolean enabled,
            boolean useSpawnEngine,
            boolean localCap,
            boolean catchUp,
            boolean monsterCatchUp,
            boolean stampSpawnOrigin) {

        public static final Codec<NaturalSpawn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(NaturalSpawn::enabled),
                Codec.BOOL.optionalFieldOf("use_spawn_engine").forGetter(s -> Optional.of(s.useSpawnEngine)),
                Codec.BOOL.optionalFieldOf("use_spawn_oracle").forGetter(s -> Optional.empty()),
                Codec.BOOL.optionalFieldOf("local_cap", false).forGetter(NaturalSpawn::localCap),
                Codec.BOOL.optionalFieldOf("catch_up", false).forGetter(NaturalSpawn::catchUp),
                Codec.BOOL.optionalFieldOf("monster_catch_up", false).forGetter(NaturalSpawn::monsterCatchUp),
                Codec.BOOL.optionalFieldOf("stamp_spawn_origin", true).forGetter(NaturalSpawn::stampSpawnOrigin)
        ).apply(instance, (enabled, engineOpt, oracleOpt, localCap, catchUp, monsterCatchUp, stamp) ->
                new NaturalSpawn(enabled, engineOpt.orElseGet(() -> oracleOpt.orElse(true)), localCap, catchUp, monsterCatchUp, stamp)));

        public static NaturalSpawn defaults() {
            return new NaturalSpawn(true, true, false, false, false, true);
        }

        public NaturalSpawn sanitize() {
            return this;
        }

        public JsonObject toJson() {
            return CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
        }

        public static NaturalSpawn fromJson(JsonObject obj) {
            if (obj == null) return defaults();
            return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(NaturalSpawn::defaults);
        }

        public CompoundTag toNbt() {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static NaturalSpawn fromNbt(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) return defaults();
            return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(NaturalSpawn::defaults);
        }
    }

    public record MobAi(
            boolean brainRng,
            boolean behaviorDurationRng,
            boolean shufflingListRng,
            boolean memoryIterationOrder) {

        public static final Codec<MobAi> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("brain_rng", true).forGetter(MobAi::brainRng),
                Codec.BOOL.optionalFieldOf("behavior_duration_rng", true).forGetter(MobAi::behaviorDurationRng),
                Codec.BOOL.optionalFieldOf("shuffling_list_rng", true).forGetter(MobAi::shufflingListRng),
                Codec.BOOL.optionalFieldOf("memory_iteration_order", true).forGetter(MobAi::memoryIterationOrder)
        ).apply(instance, MobAi::new));

        public static MobAi defaults() {
            return new MobAi(true, true, true, true);
        }

        public MobAi sanitize() {
            return this;
        }

        public JsonObject toJson() {
            return CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
        }

        public static MobAi fromJson(JsonObject obj) {
            if (obj == null) return defaults();
            return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(MobAi::defaults);
        }

        public CompoundTag toNbt() {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static MobAi fromNbt(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) return defaults();
            return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(MobAi::defaults);
        }
    }

    public record Loot(boolean containerLoot, boolean mobDeathDrops) {

        public static final Codec<Loot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("container_loot", true).forGetter(Loot::containerLoot),
                Codec.BOOL.optionalFieldOf("mob_death_drops", true).forGetter(Loot::mobDeathDrops)
        ).apply(instance, Loot::new));

        public static Loot defaults() {
            return new Loot(true, true);
        }

        public Loot sanitize() {
            return this;
        }

        public JsonObject toJson() {
            return CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
        }

        public static Loot fromJson(JsonObject obj) {
            if (obj == null) return defaults();
            return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(Loot::defaults);
        }

        public CompoundTag toNbt() {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static Loot fromNbt(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) return defaults();
            return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(Loot::defaults);
        }
    }

    public record Spawns(boolean raidSeed, boolean raidSpawnPosition, boolean wanderingTrader) {

        public static final Codec<Spawns> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("raid_seed", true).forGetter(Spawns::raidSeed),
                Codec.BOOL.optionalFieldOf("raid_spawn_position", true).forGetter(Spawns::raidSpawnPosition),
                Codec.BOOL.optionalFieldOf("wandering_trader", true).forGetter(Spawns::wanderingTrader)
        ).apply(instance, Spawns::new));

        public static Spawns defaults() {
            return new Spawns(true, true, true);
        }

        public Spawns sanitize() {
            return this;
        }

        public JsonObject toJson() {
            return CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
        }

        public static Spawns fromJson(JsonObject obj) {
            if (obj == null) return defaults();
            return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(Spawns::defaults);
        }

        public CompoundTag toNbt() {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static Spawns fromNbt(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) return defaults();
            return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(Spawns::defaults);
        }
    }

    public record Combat(boolean knockbackRng) {

        public static final Codec<Combat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("knockback_rng", true).forGetter(Combat::knockbackRng)
        ).apply(instance, Combat::new));

        public static Combat defaults() {
            return new Combat(true);
        }

        public Combat sanitize() {
            return this;
        }

        public JsonObject toJson() {
            return CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
        }

        public static Combat fromJson(JsonObject obj) {
            if (obj == null) return defaults();
            return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(Combat::defaults);
        }

        public CompoundTag toNbt() {
            return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static Combat fromNbt(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) return defaults();
            return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(Combat::defaults);
        }
    }
}