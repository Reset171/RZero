package ru.reset.rzero.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ru.reset.rzero.anchor.RZeroAnchorSettings;

public record RZeroSettings(
        boolean rzerochashEnabled,
        RZeroCheckpointPolicy checkpointPolicy,
        RZeroClientRestoreSettings clientRestore,
        RZeroAnchorSettings anchor,
        RZeroAdaptiveSettings adaptive) {

    public static final Codec<RZeroSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("rzerochashEnabled", true).forGetter(RZeroSettings::rzerochashEnabled),
            RZeroCheckpointPolicy.CODEC.optionalFieldOf("checkpointPolicy", RZeroCheckpointPolicy.defaults()).forGetter(RZeroSettings::checkpointPolicy),
            RZeroClientRestoreSettings.CODEC.optionalFieldOf("client_restore", RZeroClientRestoreSettings.defaults()).forGetter(RZeroSettings::clientRestore),
            RZeroAnchorSettings.CODEC.optionalFieldOf("anchor", RZeroAnchorSettings.defaults()).forGetter(RZeroSettings::anchor),
            RZeroAdaptiveSettings.CODEC.optionalFieldOf("adaptive", RZeroAdaptiveSettings.defaults()).forGetter(RZeroSettings::adaptive)
    ).apply(instance, RZeroSettings::new));

    public RZeroSettings {
        checkpointPolicy = checkpointPolicy == null
                ? RZeroCheckpointPolicy.defaults()
                : checkpointPolicy.sanitize();
        clientRestore = clientRestore == null
                ? RZeroClientRestoreSettings.defaults()
                : clientRestore.sanitize();
        anchor = anchor == null
                ? RZeroAnchorSettings.defaults()
                : anchor.sanitize();
        adaptive = adaptive == null
                ? RZeroAdaptiveSettings.defaults()
                : adaptive.sanitize();
    }

    public static RZeroSettings defaults() {
        return new RZeroSettings(
                true,
                RZeroCheckpointPolicy.defaults(),
                RZeroClientRestoreSettings.defaults(),
                RZeroAnchorSettings.defaults(),
                RZeroAdaptiveSettings.defaults());
    }

    public RZeroSettings sanitize() {
        return new RZeroSettings(rzerochashEnabled, checkpointPolicy, clientRestore, anchor, adaptive);
    }

    public RZeroSettings withRzerochashEnabled(boolean enabled) {
        return new RZeroSettings(enabled, checkpointPolicy, clientRestore, anchor, adaptive);
    }

    public RZeroSettings withCheckpointPolicy(RZeroCheckpointPolicy newPolicy) {
        return new RZeroSettings(rzerochashEnabled, newPolicy, clientRestore, anchor, adaptive);
    }

    public RZeroSettings withClientRestore(RZeroClientRestoreSettings newClientRestore) {
        return new RZeroSettings(rzerochashEnabled, checkpointPolicy, newClientRestore, anchor, adaptive);
    }

    public RZeroSettings withAnchor(RZeroAnchorSettings newAnchor) {
        return new RZeroSettings(rzerochashEnabled, checkpointPolicy, clientRestore, newAnchor, adaptive);
    }

    public RZeroSettings withAdaptive(RZeroAdaptiveSettings newAdaptive) {
        return new RZeroSettings(rzerochashEnabled, checkpointPolicy, clientRestore, anchor, newAdaptive);
    }

    public JsonObject toJson() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this)
                .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
    }

    public static RZeroSettings fromJson(JsonObject obj) {
        if (obj == null) return defaults();
        return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(RZeroSettings::defaults);
    }
}