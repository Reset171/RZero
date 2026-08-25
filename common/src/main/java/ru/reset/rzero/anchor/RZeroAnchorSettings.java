package ru.reset.rzero.anchor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public record RZeroAnchorSettings(AnchorMode mode,
                                  int rotationSeconds,
                                  int rollbackCooldownSeconds,
                                  List<UUID> pinned) {

    public static final int MIN_ROTATION_SECONDS = 5;

    public static final Codec<RZeroAnchorSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("mode", "fixed").xmap(AnchorMode::byId, AnchorMode::id).forGetter(RZeroAnchorSettings::mode),
            Codec.INT.optionalFieldOf("rotation_seconds", 300).forGetter(RZeroAnchorSettings::rotationSeconds),
            Codec.INT.optionalFieldOf("rollback_cooldown_seconds", 0).forGetter(RZeroAnchorSettings::rollbackCooldownSeconds),
            UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("pinned", List.of()).forGetter(RZeroAnchorSettings::pinned)
    ).apply(instance, (m, r, c, p) -> new RZeroAnchorSettings(m, r, c, p).sanitize()));

    public RZeroAnchorSettings {
        mode = mode == null ? AnchorMode.FIXED : mode;
        rotationSeconds = Math.max(MIN_ROTATION_SECONDS, rotationSeconds);
        rollbackCooldownSeconds = Math.max(0, rollbackCooldownSeconds);
        pinned = pinned == null ? List.of() : List.copyOf(new LinkedHashSet<>(pinned));
    }

    public static RZeroAnchorSettings defaults() {
        return new RZeroAnchorSettings(AnchorMode.FIXED, 300, 0, List.of());
    }

    public RZeroAnchorSettings sanitize() {
        return new RZeroAnchorSettings(mode, rotationSeconds, rollbackCooldownSeconds, pinned);
    }

    public RZeroAnchorSettings withMode(AnchorMode newMode) {
        return new RZeroAnchorSettings(newMode, rotationSeconds, rollbackCooldownSeconds, pinned);
    }

    public RZeroAnchorSettings withPinned(List<UUID> newPinned) {
        return new RZeroAnchorSettings(mode, rotationSeconds, rollbackCooldownSeconds, newPinned);
    }

    public RZeroAnchorSettings withRotationSeconds(int seconds) {
        return new RZeroAnchorSettings(mode, seconds, rollbackCooldownSeconds, pinned);
    }

    public RZeroAnchorSettings withRollbackCooldownSeconds(int seconds) {
        return new RZeroAnchorSettings(mode, rotationSeconds, seconds, pinned);
    }

    public JsonObject toJson() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this)
                .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
    }

    public static RZeroAnchorSettings fromJson(JsonObject obj) {
        if (obj == null) return defaults();
        return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(RZeroAnchorSettings::defaults);
    }
}