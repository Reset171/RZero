package ru.reset.rzero.mixin.level;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.reset.rzero.access.IRZeroDimensionDataStorage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(DimensionDataStorage.class)
public abstract class MixinDimensionDataStorage implements IRZeroDimensionDataStorage {
    @Shadow @Final private Map<String, SavedData> cache;

    @Unique
    private final Map<String, SavedData.Factory<?>> rzero$factories = new ConcurrentHashMap<>();

    @Override
    public Map<String, SavedData> rzero$getCache() {
        return this.cache;
    }

    @Override
    public Map<String, SavedData.Factory<?>> rzero$getFactories() {
        return this.rzero$factories;
    }

    @Inject(method = "computeIfAbsent", at = @At("HEAD"))
    private <T extends SavedData> void rzero$captureFactoryOnComputeIfAbsent(SavedData.Factory<T> factory, String id, CallbackInfoReturnable<T> cir) {
        if (factory != null && id != null) {
            this.rzero$factories.put(id, factory);
        }
    }

    @Inject(method = "get", at = @At("HEAD"))
    private <T extends SavedData> void rzero$captureFactoryOnGet(SavedData.Factory<T> factory, String id, CallbackInfoReturnable<T> cir) {
        if (factory != null && id != null) {
            this.rzero$factories.put(id, factory);
        }
    }
}
