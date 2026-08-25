package ru.reset.rzero;

import net.minecraft.client.Minecraft;
import ru.reset.rzero.client.cache.RZeroClientCache;
import ru.reset.rzero.access.IRZeroChat;
import ru.reset.rzero.client.input.KeyBindings;
import ru.reset.rzero.config.RZeroClientRestoreSettings;
import ru.reset.rzero.network.LoadRequestPacket;
import ru.reset.rzero.network.SaveRequestPacket;
import ru.reset.rzero.platform.Services;
import ru.reset.rzero.runtime.RZeroRuntime;

public class RZeroClient {

    private static net.minecraft.client.CameraType savedCameraType = null;
    private static java.util.Set<net.minecraft.client.resources.sounds.SoundInstance> savedSounds = new java.util.HashSet<>();
    public static int skipEquipAnimationTicks = 0;
    public static int snapPlayerRotationTicks = 0;
    private static net.minecraft.world.item.CreativeModeTab savedCreativeTab = null;
    private static Class<?> savedScreenClass = null;
    private static net.minecraft.client.RecipeBookCategories savedRecipeBookCategory = null;
    private static String savedSearchText = null;

    public static void handleMarkChat() {
        Minecraft client = Minecraft.getInstance();
        RZeroClientRestoreSettings policy = RZeroRuntime.clientRestore();
        if (policy.restoreChatState() && client.gui != null && client.gui.getChat() instanceof IRZeroChat chat) {
            chat.rzero$markChat();
        }
        RZeroClientCache.get().requestCapture();
        savedCameraType = policy.cameraType() ? client.options.getCameraType() : null;

        if (policy.stopFutureSounds()) {
            ru.reset.rzero.mixin.client.sounds.SoundManagerAccessor soundManagerAccessor = (ru.reset.rzero.mixin.client.sounds.SoundManagerAccessor) client.getSoundManager();
            ru.reset.rzero.mixin.client.sounds.SoundEngineAccessor soundEngineAccessor = (ru.reset.rzero.mixin.client.sounds.SoundEngineAccessor) soundManagerAccessor.rzero$getSoundEngine();
            savedSounds.clear();
            savedSounds.addAll(soundEngineAccessor.rzero$getInstanceToChannel().keySet());
        } else {
            savedSounds.clear();
        }

        if (policy.restoreInventoryScreen()
                && (client.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
                || client.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) {
            savedScreenClass = client.screen.getClass();
        } else {
            savedScreenClass = null;
        }

        if (policy.restoreCreativeTab()
                && client.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen creativeScreen) {
            savedCreativeTab = ru.reset.rzero.mixin.client.CreativeModeInventoryScreenAccessor.rzero$getSelectedTab();
            if (policy.restoreSearchText()) {
                net.minecraft.client.gui.components.EditBox searchBox = ((ru.reset.rzero.mixin.client.CreativeModeInventoryScreenAccessor) creativeScreen).rzero$getSearchBox();
                savedSearchText = searchBox != null ? searchBox.getValue() : null;
            } else {
                savedSearchText = null;
            }
        } else {
            savedCreativeTab = null;
            if (policy.restoreSearchText()
                    && client.screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener listener) {
                net.minecraft.client.gui.screens.recipebook.RecipeBookComponent recipeBook = listener.getRecipeBookComponent();
                net.minecraft.client.gui.components.EditBox searchBox = ((ru.reset.rzero.mixin.client.recipebook.RecipeBookComponentAccessor) recipeBook).rzero$getSearchBox();
                savedSearchText = searchBox != null ? searchBox.getValue() : null;
            } else {
                savedSearchText = null;
            }
        }

        if (policy.restoreRecipeBookTab()
                && client.screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener listener) {
            net.minecraft.client.gui.screens.recipebook.RecipeBookComponent recipeBook = listener.getRecipeBookComponent();
            net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton selectedTab = ((ru.reset.rzero.mixin.client.recipebook.RecipeBookComponentAccessor) recipeBook).rzero$getSelectedTab();
            savedRecipeBookCategory = selectedTab != null ? selectedTab.getCategory() : null;
        } else {
            savedRecipeBookCategory = null;
        }
    }

    public static void handleRollbackChat(ru.reset.rzero.network.RollbackChatPacket payload) {
        Minecraft client = Minecraft.getInstance();
        ru.reset.rzero.client.cache.RZeroVisualRollbackManager.execute(
                client, payload.x(), payload.y(), payload.z(), payload.yRot(), payload.xRot(),
                payload.gameTime(), payload.dayTime());

        RZeroClientRestoreSettings policy = RZeroRuntime.clientRestore();
        if (policy.restoreChatState() && client.gui != null && client.gui.getChat() instanceof IRZeroChat chat) {
            chat.rzero$rollbackChat();
        }
        if (policy.cameraType() && savedCameraType != null) {
            client.options.setCameraType(savedCameraType);
        }

        client.tell(() -> {
            if (policy.stopFutureSounds()) {
                ru.reset.rzero.mixin.client.sounds.SoundManagerAccessor soundManagerAccessor = (ru.reset.rzero.mixin.client.sounds.SoundManagerAccessor) client.getSoundManager();
                net.minecraft.client.sounds.SoundEngine soundEngine = soundManagerAccessor.rzero$getSoundEngine();
                ru.reset.rzero.mixin.client.sounds.SoundEngineAccessor soundEngineAccessor = (ru.reset.rzero.mixin.client.sounds.SoundEngineAccessor) soundEngine;

                java.util.List<net.minecraft.client.resources.sounds.SoundInstance> soundsToStop = new java.util.ArrayList<>();
                for (net.minecraft.client.resources.sounds.SoundInstance sound : soundEngineAccessor.rzero$getInstanceToChannel().keySet()) {
                    if (!savedSounds.contains(sound)) {
                        String path = sound.getLocation().getPath();
                        if (path.contains("tnt") || path.contains("explode") || path.contains("firework") || path.contains("fuse") || path.contains("creeper")) {
                            soundsToStop.add(sound);
                        }
                    }
                }
                for (net.minecraft.client.resources.sounds.SoundInstance sound : soundsToStop) {
                    soundEngine.stop(sound);
                }
            }

            if (policy.clearParticles()) {
                ((ru.reset.rzero.mixin.client.ParticleEngineAccessor) client.particleEngine).rzero$clearParticles();
                client.particleEngine.setLevel(client.level);
            }

            ru.reset.rzero.RZeroClient.skipEquipAnimationTicks = policy.skipEquipAnimation() ? 5 : 0;
            ru.reset.rzero.RZeroClient.snapPlayerRotationTicks = policy.snapPlayerRotation() ? 5 : 0;

            if (policy.skipEquipAnimation()) {
                ru.reset.rzero.mixin.client.ItemInHandRendererAccessor handAccessor = (ru.reset.rzero.mixin.client.ItemInHandRendererAccessor) client.getEntityRenderDispatcher().getItemInHandRenderer();
                if (client.player != null) {
                    handAccessor.rzero$setMainHandItem(client.player.getMainHandItem());
                    handAccessor.rzero$setOffHandItem(client.player.getOffhandItem());
                }
                handAccessor.rzero$setMainHandHeight(1.0f);
                handAccessor.rzero$setOffHandHeight(1.0f);
                handAccessor.rzero$setOMainHandHeight(1.0f);
                handAccessor.rzero$setOOffHandHeight(1.0f);
            }

            if (policy.restoreInventoryScreen()) {
                if (savedScreenClass != null) {
                    client.setScreen(null);
                    client.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player));
                } else {
                    if (client.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
                     || client.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen
                     || client.screen instanceof net.minecraft.client.gui.screens.DeathScreen) {
                        client.setScreen(null);
                    }
                }
            }

            if (policy.resetHurtAnimation() && client.player != null) {
                ((ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor) client.player).rzero$setHurtTime(0);
                ((ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor) client.player).rzero$setDeathTime(0);
            }

            if (policy.restoreCreativeTab()
                    && savedCreativeTab != null
                    && client.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen creativeScreen) {
                ((ru.reset.rzero.mixin.client.CreativeModeInventoryScreenAccessor) creativeScreen).rzero$selectTab(savedCreativeTab);
            }

            if (policy.restoreRecipeBookTab()
                    && savedRecipeBookCategory != null
                    && client.screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener listener) {
                net.minecraft.client.gui.screens.recipebook.RecipeBookComponent recipeBook = listener.getRecipeBookComponent();
                ru.reset.rzero.mixin.client.recipebook.RecipeBookComponentAccessor accessor = (ru.reset.rzero.mixin.client.recipebook.RecipeBookComponentAccessor) recipeBook;
                for (net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton tab : accessor.rzero$getTabButtons()) {
                    if (tab.getCategory() == savedRecipeBookCategory) {
                        net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton currentTab = accessor.rzero$getSelectedTab();
                        if (currentTab != tab) {
                            if (currentTab != null) currentTab.setStateTriggered(false);
                            accessor.rzero$setSelectedTab(tab);
                            tab.setStateTriggered(true);
                            accessor.rzero$updateCollections(true);
                        }
                        break;
                    }
                }
            }

            if (policy.restoreSearchText() && savedSearchText != null) {
                if (client.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen creativeScreen) {
                    ru.reset.rzero.mixin.client.CreativeModeInventoryScreenAccessor accessor = (ru.reset.rzero.mixin.client.CreativeModeInventoryScreenAccessor) creativeScreen;
                    net.minecraft.client.gui.components.EditBox searchBox = accessor.rzero$getSearchBox();
                    if (searchBox != null) {
                        searchBox.setValue(savedSearchText);
                        accessor.rzero$refreshSearchResults();
                    }
                } else if (client.screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener listener) {
                    ru.reset.rzero.mixin.client.recipebook.RecipeBookComponentAccessor accessor = (ru.reset.rzero.mixin.client.recipebook.RecipeBookComponentAccessor) listener.getRecipeBookComponent();
                    net.minecraft.client.gui.components.EditBox searchBox = accessor.rzero$getSearchBox();
                    if (searchBox != null) {
                        searchBox.setValue(savedSearchText);
                        accessor.rzero$updateCollections(false);
                    }
                }
            }
        });
    }

    public static void handleRzerochashToggle(boolean enabled) {
        RZeroClientCache.get().setEnabled(enabled);
    }

    public static void clientTick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        while (KeyBindings.SAVE_KEY.consumeClick()) {
            Services.PLATFORM.sendToServer(new SaveRequestPacket());
        }
        while (KeyBindings.LOAD_KEY.consumeClick()) {
            Services.PLATFORM.sendToServer(new LoadRequestPacket());
        }

        RZeroClientCache cache = RZeroClientCache.get();
        if (cache.isCapturePending() && client.level != null) {
            cache.tryDeferredCapture(client.level);
        }
        if (cache.isPendingSectionRefresh()) {
            cache.tickPendingRefresh();
        }
        cache.tickSession();
    }
}
