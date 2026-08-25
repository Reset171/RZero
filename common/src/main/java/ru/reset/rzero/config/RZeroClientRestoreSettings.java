package ru.reset.rzero.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RZeroClientRestoreSettings(
        boolean cameraType,
        boolean stopFutureSounds,
        boolean clearParticles,
        boolean restoreInventoryScreen,
        boolean restoreCreativeTab,
        boolean restoreRecipeBookTab,
        boolean restoreSearchText,
        boolean resetHurtAnimation,
        boolean snapPlayerRotation,
        boolean skipEquipAnimation,
        boolean restoreChatState,
        boolean suppressTerrainLoadingScreen,
        boolean displayChatNotifications,
        boolean meshCacheEnabled,
        int meshCacheRadius,
        int meshCacheBudgetMb) {

    public static final int MIN_MESH_RADIUS = 2;
    public static final int MAX_MESH_RADIUS = 32;
    public static final int MAX_MESH_BUDGET_MB = 4096;

    public static final Codec<RZeroClientRestoreSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("camera_type", true).forGetter(RZeroClientRestoreSettings::cameraType),
            Codec.BOOL.optionalFieldOf("stop_future_sounds", true).forGetter(RZeroClientRestoreSettings::stopFutureSounds),
            Codec.BOOL.optionalFieldOf("clear_particles", true).forGetter(RZeroClientRestoreSettings::clearParticles),
            Codec.BOOL.optionalFieldOf("restore_inventory_screen", true).forGetter(RZeroClientRestoreSettings::restoreInventoryScreen),
            Codec.BOOL.optionalFieldOf("restore_creative_tab", true).forGetter(RZeroClientRestoreSettings::restoreCreativeTab),
            Codec.BOOL.optionalFieldOf("restore_recipe_book_tab", true).forGetter(RZeroClientRestoreSettings::restoreRecipeBookTab),
            Codec.BOOL.optionalFieldOf("restore_search_text", true).forGetter(RZeroClientRestoreSettings::restoreSearchText),
            Codec.BOOL.optionalFieldOf("reset_hurt_animation", true).forGetter(RZeroClientRestoreSettings::resetHurtAnimation),
            Codec.BOOL.optionalFieldOf("snap_player_rotation", true).forGetter(RZeroClientRestoreSettings::snapPlayerRotation),
            Codec.BOOL.optionalFieldOf("skip_equip_animation", true).forGetter(RZeroClientRestoreSettings::skipEquipAnimation),
            Codec.BOOL.optionalFieldOf("restore_chat_state", true).forGetter(RZeroClientRestoreSettings::restoreChatState),
            Codec.BOOL.optionalFieldOf("suppress_terrain_loading_screen", false).forGetter(RZeroClientRestoreSettings::suppressTerrainLoadingScreen),
            Codec.BOOL.optionalFieldOf("display_chat_notifications", true).forGetter(RZeroClientRestoreSettings::displayChatNotifications),
            Codec.BOOL.optionalFieldOf("mesh_cache_enabled", false).forGetter(RZeroClientRestoreSettings::meshCacheEnabled),
            Codec.INT.optionalFieldOf("mesh_cache_radius", 12).forGetter(RZeroClientRestoreSettings::meshCacheRadius),
            Codec.INT.optionalFieldOf("mesh_cache_budget_mb", 512).forGetter(RZeroClientRestoreSettings::meshCacheBudgetMb)
    ).apply(instance, (c, sf, cp, ri, rc, rrb, rs, rh, sp, se, rcs, st, dcn, mc, mr, mb) ->
            new RZeroClientRestoreSettings(c, sf, cp, ri, rc, rrb, rs, rh, sp, se, rcs, st, dcn, mc, mr, mb).sanitize()));

    public static RZeroClientRestoreSettings defaults() {
        return new RZeroClientRestoreSettings(
                true, true, true, true, true, true, true, true,
                true, true, true, false, true, false, 12, 512);
    }

    public RZeroClientRestoreSettings sanitize() {
        int radius = Math.max(MIN_MESH_RADIUS, Math.min(MAX_MESH_RADIUS, this.meshCacheRadius));
        int budget = Math.max(0, Math.min(MAX_MESH_BUDGET_MB, this.meshCacheBudgetMb));
        if (radius == this.meshCacheRadius && budget == this.meshCacheBudgetMb) {
            return this;
        }
        return new RZeroClientRestoreSettings(
                this.cameraType,
                this.stopFutureSounds,
                this.clearParticles,
                this.restoreInventoryScreen,
                this.restoreCreativeTab,
                this.restoreRecipeBookTab,
                this.restoreSearchText,
                this.resetHurtAnimation,
                this.snapPlayerRotation,
                this.skipEquipAnimation,
                this.restoreChatState,
                this.suppressTerrainLoadingScreen,
                this.displayChatNotifications,
                this.meshCacheEnabled,
                radius,
                budget);
    }

    public long meshCacheBudgetBytes() {
        return this.meshCacheEnabled ? (long) this.meshCacheBudgetMb * 1024L * 1024L : 0L;
    }

    public JsonObject toJson() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this)
                .result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
    }

    public static RZeroClientRestoreSettings fromJson(JsonObject obj) {
        if (obj == null) return defaults();
        return CODEC.parse(JsonOps.INSTANCE, obj).result().orElseGet(RZeroClientRestoreSettings::defaults);
    }
}