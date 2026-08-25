package ru.reset.rzero.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.chunk.LevelChunk;
import ru.reset.rzero.ModGameRules;
import ru.reset.rzero.RZero;
import ru.reset.rzero.checkpoint.CheckpointManager;
import ru.reset.rzero.event.ChunkEvents;
import ru.reset.rzero.event.CommandEvents;
import ru.reset.rzero.event.EntityEvents;
import ru.reset.rzero.event.PlayerEvents;
import ru.reset.rzero.network.LoadRequestPacket;
import ru.reset.rzero.network.MarkChatPacket;
import ru.reset.rzero.network.RollbackChatPacket;
import ru.reset.rzero.network.RzerochashTogglePacket;
import ru.reset.rzero.network.SaveRequestPacket;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;

public class RZeroFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, RZero.RBD_SOUND_ID, RZero.RBD_SOUND);
        ru.reset.rzero.RZeroConfig.load();

        CustomGameRuleCategory RZERO_CATEGORY = new CustomGameRuleCategory(ResourceLocation.fromNamespaceAndPath(RZero.MODID, "category"), Component.translatable("gamerule.category.rzero"));
        ModGameRules.RULE_AUTO_SAVE = GameRuleRegistry.register("rzeroAutoSave", RZERO_CATEGORY, GameRuleFactory.createBooleanRule(true));
        ModGameRules.RULE_FIXED_INTERVAL = GameRuleRegistry.register("rzeroFixedInterval", RZERO_CATEGORY, GameRuleFactory.createIntRule(300));
        ModGameRules.RULE_USE_RANDOM_INTERVAL = GameRuleRegistry.register("rzeroUseRandomInterval", RZERO_CATEGORY, GameRuleFactory.createBooleanRule(false));
        ModGameRules.RULE_RANDOM_MIN = GameRuleRegistry.register("rzeroRandomIntervalMin", RZERO_CATEGORY, GameRuleFactory.createIntRule(300));
        ModGameRules.RULE_RANDOM_MAX = GameRuleRegistry.register("rzeroRandomIntervalMax", RZERO_CATEGORY, GameRuleFactory.createIntRule(600));
        ModGameRules.RULE_PLAY_ROLLBACK_SOUND = GameRuleRegistry.register("rzeroPlayRollbackSound", RZERO_CATEGORY, GameRuleFactory.createBooleanRule(true));
        ModGameRules.RULE_USE_ADAPTIVE_MODE = GameRuleRegistry.register("rzeroUseAdaptiveMode", RZERO_CATEGORY, GameRuleFactory.createBooleanRule(false));
        ModGameRules.RULE_ADAPTIVE_INTERVAL = GameRuleRegistry.register("rzeroAdaptiveInterval", RZERO_CATEGORY, GameRuleFactory.createIntRule(300));

        PayloadTypeRegistry.playC2S().register(SaveRequestPacket.TYPE, SaveRequestPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(LoadRequestPacket.TYPE, LoadRequestPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(MarkChatPacket.TYPE, MarkChatPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RollbackChatPacket.TYPE, RollbackChatPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RzerochashTogglePacket.TYPE, RzerochashTogglePacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SaveRequestPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().hasPermissions(2)) {
                    CheckpointManager.setCheckpoint(context.player());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(LoadRequestPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().hasPermissions(2)) {
                    RestoreQueues.pendingDeathRollback = context.player().getUUID();
                }
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerEvents.onPlayerJoin(handler.getPlayer());
            ServerPlayNetworking.send(handler.getPlayer(), new RzerochashTogglePacket(RZeroRuntime.rzerochashEnabled));
        });
        
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            PlayerEvents.onRightClickBlock(player, world);
            return InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            PlayerEvents.onRightClickItem(player, world, player.getItemInHand(hand));
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CommandEvents.onRegisterCommands(dispatcher, registryAccess, environment));
        ServerLifecycleEvents.SERVER_STARTED.register(ru.reset.rzero.event.ServerLifecycleEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ru.reset.rzero.event.ServerLifecycleEvents.onServerStopping());
        
        ServerChunkEvents.CHUNK_LOAD.register((serverLevel, chunk) -> ChunkEvents.onChunkLoad(serverLevel, (LevelChunk) chunk));
        ServerChunkEvents.CHUNK_UNLOAD.register((serverLevel, chunk) -> ChunkEvents.onChunkUnload(serverLevel, (LevelChunk) chunk));
        
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (world instanceof ServerLevel sl) EntityEvents.onEntityJoin(entity, sl);
        });
        
        ServerTickEvents.END_SERVER_TICK.register(ru.reset.rzero.event.ServerTickEvents::onServerTickPost);
    }
}