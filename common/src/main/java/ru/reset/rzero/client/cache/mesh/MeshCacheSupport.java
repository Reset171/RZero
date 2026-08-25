package ru.reset.rzero.client.cache.mesh;

import ru.reset.rzero.RZero;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class MeshCacheSupport {

    private static final String[] INCOMPATIBLE_RENDERERS = {
            "net.caffeinemc.mods.sodium.client.SodiumClientMod",
            "me.jellysquid.mods.sodium.client.SodiumClientMod",
            "org.embeddedt.embeddium.impl.EmbeddiumMod",
            "me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer"
    };

    private static Boolean supported;
    private static String blocker;

    private MeshCacheSupport() {}

    public static boolean isSupported() {
        if (supported == null) {
            String found = null;
            for (String candidate : INCOMPATIBLE_RENDERERS) {
                if (classExists(candidate)) {
                    found = candidate;
                    break;
                }
            }
            blocker = found;
            supported = found == null;
            if (found == null) {
                RZero.LOGGER.info("[RZero][mesh] vanilla terrain renderer detected — mesh cache available");
            } else {
                RZero.LOGGER.info("[RZero][mesh] alternative terrain renderer detected ({}) — mesh cache disabled, "
                        + "block-data cache unaffected", found);
            }
        }
        return supported;
    }

    public static String blocker() {
        isSupported();
        return blocker;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, MeshCacheSupport.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static final ExecutorService SAME_THREAD_EXECUTOR = new AbstractExecutorService() {
        private volatile boolean shutdown;

        @Override
        public void shutdown() {
            this.shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            this.shutdown = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return this.shutdown;
        }

        @Override
        public boolean isTerminated() {
            return this.shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };
}
