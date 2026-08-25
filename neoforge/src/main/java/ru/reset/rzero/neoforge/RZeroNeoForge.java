package ru.reset.rzero.neoforge;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.reset.rzero.ModGameRules;
import ru.reset.rzero.RZero;
import ru.reset.rzero.RZeroClient;
import ru.reset.rzero.checkpoint.CheckpointManager;
import ru.reset.rzero.event.ChunkEvents;
import ru.reset.rzero.event.CommandEvents;
import ru.reset.rzero.event.EntityEvents;
import ru.reset.rzero.event.PlayerEvents;
import ru.reset.rzero.event.ServerLifecycleEvents;
import ru.reset.rzero.event.ServerTickEvents;
import ru.reset.rzero.network.LoadRequestPacket;
import ru.reset.rzero.network.MarkChatPacket;
import ru.reset.rzero.network.RollbackChatPacket;
import ru.reset.rzero.network.RzerochashTogglePacket;
import ru.reset.rzero.network.SaveRequestPacket;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;

@Mod(RZero.MODID)
public class RZeroNeoForge {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, RZero.MODID);

    public RZeroNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        SOUND_EVENTS.register("rbd_sound", () -> RZero.RBD_SOUND);
        ru.reset.rzero.RZeroConfig.load();

        net.neoforged.fml.ModLoadingContext.get().registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                () -> (minecraft, screen) -> ru.reset.rzero.client.gui.RZeroConfigScreen.create(screen)
        );

        SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            ServerPlayer p = (ServerPlayer) e.getEntity();
            PlayerEvents.onPlayerJoin(p);
            ru.reset.rzero.platform.Services.PLATFORM.sendToPlayer(p, new RzerochashTogglePacket(RZeroRuntime.rzerochashEnabled));
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> PlayerEvents.onRightClickBlock(e.getEntity(), e.getLevel()));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> PlayerEvents.onRightClickItem(e.getEntity(), e.getLevel(), e.getItemStack()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> CommandEvents.onRegisterCommands(e.getDispatcher(), e.getBuildContext(), e.getCommandSelection()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> ServerLifecycleEvents.onServerStarted(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> ServerLifecycleEvents.onServerStopping());
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load e) -> {
            if (e.getLevel() instanceof ServerLevel sl && e.getChunk() instanceof LevelChunk lc) ChunkEvents.onChunkLoad(sl, lc);
        });
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Unload e) -> {
            if (e.getLevel() instanceof ServerLevel sl && e.getChunk() instanceof LevelChunk lc) ChunkEvents.onChunkUnload(sl, lc);
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.getLevel() instanceof ServerLevel sl) EntityEvents.onEntityJoin(e.getEntity(), sl);
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> ServerTickEvents.onServerTickPost(e.getServer()));

        ModGameRules.RULE_AUTO_SAVE = GameRules.register("rzeroAutoSave", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
        ModGameRules.RULE_FIXED_INTERVAL = GameRules.register("rzeroFixedInterval", GameRules.Category.MISC, GameRules.IntegerValue.create(300));
        ModGameRules.RULE_USE_RANDOM_INTERVAL = GameRules.register("rzeroUseRandomInterval", GameRules.Category.MISC, GameRules.BooleanValue.create(false));
        ModGameRules.RULE_RANDOM_MIN = GameRules.register("rzeroRandomIntervalMin", GameRules.Category.MISC, GameRules.IntegerValue.create(300));
        ModGameRules.RULE_RANDOM_MAX = GameRules.register("rzeroRandomIntervalMax", GameRules.Category.MISC, GameRules.IntegerValue.create(600));
        ModGameRules.RULE_PLAY_ROLLBACK_SOUND = GameRules.register("rzeroPlayRollbackSound", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
        ModGameRules.RULE_USE_ADAPTIVE_MODE = GameRules.register("rzeroUseAdaptiveMode", GameRules.Category.MISC, GameRules.BooleanValue.create(false));
        ModGameRules.RULE_ADAPTIVE_INTERVAL = GameRules.register("rzeroAdaptiveInterval", GameRules.Category.MISC, GameRules.IntegerValue.create(300));
    }

    public void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(SaveRequestPacket.TYPE, SaveRequestPacket.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player().hasPermissions(2)) {
                    CheckpointManager.setCheckpoint((ServerPlayer) context.player());
                }
            });
        });

        registrar.playToServer(LoadRequestPacket.TYPE, LoadRequestPacket.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player().hasPermissions(2)) {
                    RestoreQueues.pendingDeathRollback = context.player().getUUID();
                }
            });
        });

        registrar.playToClient(MarkChatPacket.TYPE, MarkChatPacket.CODEC, (payload, context) -> {
            context.enqueueWork(RZeroClient::handleMarkChat);
        });

        registrar.playToClient(RollbackChatPacket.TYPE, RollbackChatPacket.CODEC, (payload, context) -> {
            ru.reset.rzero.client.cache.RZeroClientCache.get().armInterDimensionalRollback();
            context.enqueueWork(() -> RZeroClient.handleRollbackChat(payload));
        });

        registrar.playToClient(RzerochashTogglePacket.TYPE, RzerochashTogglePacket.CODEC, (payload, context) -> {
            context.enqueueWork(() -> RZeroClient.handleRzerochashToggle(payload.enabled()));
        });
    }
}