package ru.reset.rzero.api;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class DevHooks {
    private DevHooks() {}

    public static final List<Consumer<MinecraftServer>> ON_SERVER_STARTED = new CopyOnWriteArrayList<>();
    public static final List<Consumer<MinecraftServer>> ON_SERVER_TICK_POST = new CopyOnWriteArrayList<>();
    public static final List<Consumer<MinecraftServer>> ON_CHECKPOINT_SAVED = new CopyOnWriteArrayList<>();
    public static final List<Consumer<MinecraftServer>> ON_CHECKPOINT_LOADED = new CopyOnWriteArrayList<>();

    public static final List<BiConsumer<MinecraftServer, String>> ON_PROFILE_START = new CopyOnWriteArrayList<>();
    public static final List<ProfileEndListener> ON_PROFILE_END = new CopyOnWriteArrayList<>();

    public static final List<RegisterCommandsHook> ON_REGISTER_COMMANDS = new CopyOnWriteArrayList<>();
    public static final List<MetricsListener> ON_METRICS = new CopyOnWriteArrayList<>();

    public interface BlobProfiler {
        long startTimer();
        void onEncode(long start, int bytes);
        void onDecode(long start);
        void onDedup(boolean hit);
    }
    public static BlobProfiler BLOB_PROFILER = new BlobProfiler() {
        @Override public long startTimer() { return 0; }
        @Override public void onEncode(long start, int bytes) {}
        @Override public void onDecode(long start) {}
        @Override public void onDedup(boolean hit) {}
    };

    public interface SaveProfiler {
        void beginSave();
        void endSave(net.minecraft.server.level.ServerLevel level, int entitiesCount);
        void beginPhase(String phase);
        void endPhase(String phase);
    }
    public static SaveProfiler SAVE_PROFILER = new SaveProfiler() {
        @Override public void beginSave() {}
        @Override public void endSave(net.minecraft.server.level.ServerLevel level, int entitiesCount) {}
        @Override public void beginPhase(String phase) {}
        @Override public void endPhase(String phase) {}
    };

    public interface ClientProfiler {
        void onRespawnStart();
        void onRespawnEnd(boolean interDim);
        void onAllChangedStart();
        void onReleaseBuffers();
        void onBlockUntilClear();
        void onNewViewArea(int sectionCount);
        void onAllChangedEnd();
    }
    public static ClientProfiler CLIENT_PROFILER = new ClientProfiler() {
        @Override public void onRespawnStart() {}
        @Override public void onRespawnEnd(boolean interDim) {}
        @Override public void onAllChangedStart() {}
        @Override public void onReleaseBuffers() {}
        @Override public void onBlockUntilClear() {}
        @Override public void onNewViewArea(int sectionCount) {}
        @Override public void onAllChangedEnd() {}
    };

    public static void fireServerStarted(MinecraftServer server) { dispatch(ON_SERVER_STARTED, server); }
    public static void fireServerTickPost(MinecraftServer server) { dispatch(ON_SERVER_TICK_POST, server); }
    public static void fireCheckpointSaved(MinecraftServer server) { dispatch(ON_CHECKPOINT_SAVED, server); }
    public static void fireCheckpointLoaded(MinecraftServer server) { dispatch(ON_CHECKPOINT_LOADED, server); }

    public static void fireProfileStart(MinecraftServer server, String tag) {
        for (BiConsumer<MinecraftServer, String> l : ON_PROFILE_START) {
            try { l.accept(server, tag); } catch (Throwable ignored) {}
        }
    }

    public static void fireProfileEnd(MinecraftServer server, String tag, int chunksProcessed, int chunksQueued) {
        for (ProfileEndListener l : ON_PROFILE_END) {
            try { l.onEnd(server, tag, chunksProcessed, chunksQueued); } catch (Throwable ignored) {}
        }
    }

    public static void fireRegisterCommands(LiteralArgumentBuilder<CommandSourceStack> rzeroNode,
                                            CommandDispatcher<CommandSourceStack> dispatcher,
                                            CommandBuildContext registryAccess,
                                            Commands.CommandSelection environment) {
        for (RegisterCommandsHook h : ON_REGISTER_COMMANDS) {
            try { h.apply(rzeroNode, dispatcher, registryAccess, environment); } catch (Throwable ignored) {}
        }
    }

    public static void fireMetrics(ServerPlayer player, double threat, int mobsTrack, int mobsAttack,
                                   int freeSpace, float corneredPct, int cliffsNear, String event,
                                   List<Mob> mobs, List<PrimedTnt> tnts, int projAir) {
        for (MetricsListener l : ON_METRICS) {
            try {
                l.onMetrics(player, threat, mobsTrack, mobsAttack, freeSpace, corneredPct, cliffsNear, event, mobs, tnts, projAir);
            } catch (Throwable ignored) {}
        }
    }

    private static <T> void dispatch(List<Consumer<T>> ls, T arg) {
        for (Consumer<T> l : ls) {
            try { l.accept(arg); } catch (Throwable e) { e.printStackTrace(); }
        }
    }

    @FunctionalInterface
    public interface ProfileEndListener {
        void onEnd(MinecraftServer server, String tag, int chunksProcessed, int chunksQueued);
    }
}
