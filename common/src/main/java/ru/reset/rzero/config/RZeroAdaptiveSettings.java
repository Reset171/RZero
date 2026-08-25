package ru.reset.rzero.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RZeroAdaptiveSettings(
        int minSaveGapSeconds,
        int relaxTimeSeconds,
        int postCombatSavePercent,
        boolean saveOnBossDefeat) {

    public static final Codec<RZeroAdaptiveSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("min_save_gap_seconds", 30).forGetter(RZeroAdaptiveSettings::minSaveGapSeconds),
            Codec.INT.optionalFieldOf("relax_time_seconds", 5).forGetter(RZeroAdaptiveSettings::relaxTimeSeconds),
            Codec.INT.optionalFieldOf("post_combat_save_percent", 70).forGetter(RZeroAdaptiveSettings::postCombatSavePercent),
            Codec.BOOL.optionalFieldOf("save_on_boss_defeat", true).forGetter(RZeroAdaptiveSettings::saveOnBossDefeat)
    ).apply(instance, (gap, relax, pct, boss) -> new RZeroAdaptiveSettings(gap, relax, pct, boss).sanitize()));

    public static RZeroAdaptiveSettings defaults() {
        return new RZeroAdaptiveSettings(30, 5, 70, true);
    }

    public RZeroAdaptiveSettings sanitize() {
        return new RZeroAdaptiveSettings(
                Math.max(5, minSaveGapSeconds),
                Math.max(1, relaxTimeSeconds),
                Math.clamp(postCombatSavePercent, 0, 100),
                saveOnBossDefeat);
    }

    public JsonObject toJson() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this)
                .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
    }

    public static RZeroAdaptiveSettings fromJson(JsonObject obj) {
        if (obj == null) return defaults();
        return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(RZeroAdaptiveSettings::defaults);
    }
}