package ru.reset.rzero.serial;

import net.minecraft.nbt.CompoundTag;
import ru.reset.rzero.checkpoint.data.EntitySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class RZBlobEncoder {
    private static final int MAX_THREADS = 4;

    private static volatile ExecutorService pool;

    private RZBlobEncoder() {}

    private static ExecutorService pool() {
        ExecutorService p = pool;
        if (p == null) {
            synchronized (RZBlobEncoder.class) {
                if ((p = pool) == null) {
                    p = pool = createPool();
                }
            }
        }
        return p;
    }

    private static ExecutorService createPool() {
        int threads = Math.max(1, Math.min(MAX_THREADS, Runtime.getRuntime().availableProcessors() - 1));
        AtomicInteger n = new AtomicInteger();
        return Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "RZBlob-Encoder-" + n.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
    }

    public static void shutdown() {
        ExecutorService p;
        synchronized (RZBlobEncoder.class) {
            p = pool;
            pool = null;
        }
        if (p != null) {
            p.shutdown();
            try {
                if (!p.awaitTermination(2, TimeUnit.SECONDS)) p.shutdownNow();
            } catch (InterruptedException ignored) {
                p.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static Session newSession() {
        return new Session();
    }

    public static final class Session implements AutoCloseable {
        private final List<Future<?>> inflight = new ArrayList<>(256);

        private Session() {}

        public void submitEntity(EntitySnapshot target, CompoundTag tag) {
            inflight.add(pool().submit(() -> { target.blob = RZBlob.of(tag); }));
        }

        public void submitBe(CompoundTag tag, Consumer<RZBlob> sink) {
            inflight.add(pool().submit(() -> sink.accept(RZBlob.of(tag))));
        }

        public void submitTask(Runnable task) {
            task.run();
        }

        public void awaitAll() {
            Throwable failure = null;
            for (Future<?> f : inflight) {
                try {
                    f.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (failure == null) failure = e;
                } catch (ExecutionException e) {
                    if (failure == null) failure = e.getCause() != null ? e.getCause() : e;
                }
            }
            inflight.clear();
            if (failure != null) {
                if (failure instanceof RuntimeException re) throw re;
                if (failure instanceof Error err) throw err;
                throw new RuntimeException("RZBlob encode session failed", failure);
            }
        }

        @Override public void close() {
            awaitAll();
        }

        public int submitted() { return inflight.size(); }

        public record SessionStats(long backgroundNanos, int blobsCount) {}

        public void whenComplete(long startNanos, Consumer<SessionStats> callback) {
            final List<Future<?>> tasks = new ArrayList<>(this.inflight);
            final int count = tasks.size();
            pool().submit(() -> {
                for (Future<?> f : tasks) {
                    try {
                        f.get();
                    } catch (Throwable ignored) {}
                }
                long bgNanos = System.nanoTime() - startNanos;
                callback.accept(new SessionStats(bgNanos, count));
            });
        }
    }
}
