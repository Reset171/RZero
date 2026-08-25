package ru.reset.rzero.mixin.dirty;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.access.IRZeroDirtyCache;

@Mixin(RecipeBook.class)
public abstract class MixinRecipeBookDirty implements IRZeroDirtyCache {
    @Unique private boolean rzero$dirty = true;
    @Unique private CompoundTag rzero$cachedNbt;

    @Override
    public boolean rzero$isDirty() { return rzero$dirty; }

    @Override
    public void rzero$markDirty() { rzero$dirty = true; rzero$cachedNbt = null; }

    @Override
    public void rzero$markClean() { rzero$dirty = false; }

    @Override
    public CompoundTag rzero$getCachedNbt() { return rzero$cachedNbt; }

    @Override
    public void rzero$setCachedNbt(CompoundTag tag) { rzero$cachedNbt = tag; }

    @Inject(method = "add(Lnet/minecraft/resources/ResourceLocation;)V", at = @At("HEAD"))
    private void rzero$onAddRL(ResourceLocation rl, CallbackInfo ci) { rzero$markDirty(); }

    @Inject(method = "remove(Lnet/minecraft/resources/ResourceLocation;)V", at = @At("HEAD"))
    private void rzero$onRemoveRL(ResourceLocation rl, CallbackInfo ci) { rzero$markDirty(); }

    @Inject(method = "addHighlight(Lnet/minecraft/resources/ResourceLocation;)V", at = @At("HEAD"))
    private void rzero$onAddHighlightRL(ResourceLocation rl, CallbackInfo ci) { rzero$markDirty(); }

    @Inject(method = "removeHighlight(Lnet/minecraft/world/item/crafting/RecipeHolder;)V", at = @At("HEAD"))
    private void rzero$onRemoveHighlight(RecipeHolder<?> holder, CallbackInfo ci) { rzero$markDirty(); }

    @Inject(method = "setBookSettings", at = @At("HEAD"))
    private void rzero$onSetBookSettings(RecipeBookSettings s, CallbackInfo ci) { rzero$markDirty(); }

    @Inject(method = "setBookSetting", at = @At("HEAD"))
    private void rzero$onSetBookSetting(RecipeBookType t, boolean a, boolean b, CallbackInfo ci) { rzero$markDirty(); }

    @Inject(method = "setOpen", at = @At("HEAD"))
    private void rzero$onSetOpen(RecipeBookType t, boolean v, CallbackInfo ci) { rzero$markDirty(); }

    @Inject(method = "setFiltering", at = @At("HEAD"))
    private void rzero$onSetFiltering(RecipeBookType t, boolean v, CallbackInfo ci) { rzero$markDirty(); }

    @Inject(method = "copyOverData", at = @At("HEAD"))
    private void rzero$onCopyOverData(RecipeBook src, CallbackInfo ci) { rzero$markDirty(); }
}
