package ru.reset.rzero.util;

import ru.reset.rzero.RZero;

import java.util.Arrays;

public final class RZBenchmark {

    public enum Phase {
        PREPARE, PLAYERS_ONLINE, OFFLINE_FILES, WORLD_STATE, SERVER_GLOBALS, SCOREBOARD,
        CHUNK_BLOCKS, CHUNK_BE_CLEAR, CHUNK_BE_LOAD, CHUNK_TICKS, CHUNK_POIS, CHUNK_PUSH,
        REMOVE_FOREIGN, SPAWN_NBT, APPLY_MOB_RAM, BRAIN_RELINK, BRAIN_DEEPCLONE,
        REMOVE_SNAPSHOT_ENTITIES, DROP_STALE_EVENTS, MENU_REOPEN,
        CAPTURE_PLAYERS, CAPTURE_GLOBALS, CAPTURE_RAIDS, CAPTURE_WORLD_STATE,
        CAPTURE_CHUNKS, CAPTURE_BE, CAPTURE_TICKS_POIS, CAPTURE_ENTITIES
    }

    private static final long[] NANOS = new long[Phase.values().length];
    private static final int[] COUNTS = new int[Phase.values().length];

    private static volatile boolean open;
    private static volatile boolean timedOut;
    private static String title = "";
    private static long startNanos;
    private static int startTick;
    private static int deferredTicks;
    private static int blocksChanged;
    private static int chunks;
    private static int mobs;
    private static int players;
    private static String asyncNote = null;

    private RZBenchmark() {}

    public static void begin(String newTitle, int currentTick) {
        if (!RZero.verbose()) return;
        open = true;
        timedOut = false;
        title = newTitle;
        startNanos = System.nanoTime();
        startTick = currentTick;
        Arrays.fill(NANOS, 0L);
        Arrays.fill(COUNTS, 0);
        blocksChanged = 0;
        chunks = 0;
        mobs = 0;
        players = 0;
        deferredTicks = 0;
        asyncNote = null;
    }

    public static void accum(Phase phase, long phaseStartNanos) {
        if (!open) return;
        NANOS[phase.ordinal()] += System.nanoTime() - phaseStartNanos;
        COUNTS[phase.ordinal()]++;
    }

    public static void addBlocksChanged(int n) { if (open) blocksChanged += n; }
    public static void addChunks(int n) { if (open) chunks += n; }
    public static void addMobs(int n) { if (open) mobs += n; }
    public static void addPlayers(int n) { if (open) players += n; }
    public static void setAsyncNote(String note) { if (open) asyncNote = note; }

    public static boolean isOpen() { return open; }

    public static void tick(int currentTick) {
        if (!open) return;
        deferredTicks = Math.max(0, currentTick - startTick);
        if (deferredTicks > 600) timedOut = true;
    }

    private static final Phase[] RESTORE_TOP_LEVEL = {
            Phase.PREPARE, Phase.PLAYERS_ONLINE, Phase.OFFLINE_FILES, Phase.WORLD_STATE,
            Phase.SERVER_GLOBALS, Phase.SCOREBOARD, Phase.CHUNK_BLOCKS, Phase.CHUNK_BE_CLEAR,
            Phase.CHUNK_BE_LOAD, Phase.CHUNK_TICKS, Phase.CHUNK_POIS, Phase.CHUNK_PUSH,
            Phase.REMOVE_FOREIGN, Phase.SPAWN_NBT, Phase.BRAIN_RELINK, Phase.APPLY_MOB_RAM,
            Phase.REMOVE_SNAPSHOT_ENTITIES, Phase.DROP_STALE_EVENTS, Phase.MENU_REOPEN
    };

    private static final Phase[] CAPTURE_TOP_LEVEL = {
            Phase.CAPTURE_PLAYERS, Phase.CAPTURE_GLOBALS, Phase.CAPTURE_RAIDS,
            Phase.CAPTURE_WORLD_STATE, Phase.CAPTURE_CHUNKS
    };

    public static void endAndLog() {
        if (!open || !RZero.verbose()) {
            open = false;
            return;
        }
        open = false;
        long total = System.nanoTime() - startNanos;
        Phase[] topLevel = title.startsWith("ROLLBACK") ? RESTORE_TOP_LEVEL : CAPTURE_TOP_LEVEL;
        long work = 0;
        for (Phase phase : topLevel) {
            work += NANOS[phase.ordinal()];
        }
        StringBuilder sb = new StringBuilder(768);
        sb.append("[RZero][Benchmark] === ").append(title).append(": ")
                .append(ms(total)).append(" ms (wall) | work ").append(ms(work)).append(" ms ===");
        if (deferredTicks > 0) {
            sb.append(" (deferred ").append(deferredTicks).append(" ticks");
            if (timedOut) sb.append(", TIMEOUT");
            sb.append(')');
        }
        if (title.startsWith("ROLLBACK")) {
            appendRestoreTree(sb);
        } else {
            appendCaptureTree(sb);
        }
        RZero.logInfo(sb.toString());
    }

    private static void appendRestoreTree(StringBuilder sb) {
        line(sb, "Prepare", Phase.PREPARE, 0);
        if (sum(Phase.PLAYERS_ONLINE, Phase.OFFLINE_FILES) > 0 || players > 0) {
            line(sb, "Players (" + players + ")", sum(Phase.PLAYERS_ONLINE, Phase.OFFLINE_FILES));
            line(sb, "Offline Files (.dat)", Phase.OFFLINE_FILES, 1);
        }
        long world = sum(Phase.WORLD_STATE, Phase.SERVER_GLOBALS, Phase.SCOREBOARD);
        if (world > 0) {
            line(sb, "World & Globals", world);
            line(sb, "WorldState", Phase.WORLD_STATE, 1);
            line(sb, "ServerGlobals", Phase.SERVER_GLOBALS, 1);
            line(sb, "Scoreboard", Phase.SCOREBOARD, 1);
        }
        long chunkSum = sum(Phase.CHUNK_BLOCKS, Phase.CHUNK_BE_CLEAR, Phase.CHUNK_BE_LOAD,
                Phase.CHUNK_TICKS, Phase.CHUNK_POIS, Phase.CHUNK_PUSH);
        if (chunkSum > 0 || chunks > 0) {
            line(sb, "Chunks (" + chunks + ", blocks changed " + blocksChanged + ")", chunkSum);
            line(sb, "Blocks (applyDiffTo)", Phase.CHUNK_BLOCKS, 1);
            line(sb, "Block Entities (clear)", Phase.CHUNK_BE_CLEAR, 1);
            line(sb, "Block Entities (load)", Phase.CHUNK_BE_LOAD, 1);
            line(sb, "Scheduled Ticks", Phase.CHUNK_TICKS, 1);
            line(sb, "POIs", Phase.CHUNK_POIS, 1);
            line(sb, "PushUpdates", Phase.CHUNK_PUSH, 1);
        }
        long entitySum = sum(Phase.REMOVE_FOREIGN, Phase.SPAWN_NBT,
                Phase.BRAIN_RELINK, Phase.APPLY_MOB_RAM);
        if (entitySum > 0 || mobs > 0) {
            line(sb, "Entities (" + mobs + ")", entitySum);
            line(sb, "Remove Foreign", Phase.REMOVE_FOREIGN, 1);
            line(sb, "Spawn & NBT", Phase.SPAWN_NBT, 1);
            line(sb, "Brain Relink", Phase.BRAIN_RELINK, 1);
            line(sb, "Brain DeepClone", Phase.BRAIN_DEEPCLONE, 2);
            line(sb, "ApplyMobRam", Phase.APPLY_MOB_RAM, 1);
        }
        long cleanup = sum(Phase.REMOVE_SNAPSHOT_ENTITIES, Phase.DROP_STALE_EVENTS);
        if (cleanup > 0) {
            line(sb, "Cleanup", cleanup);
            line(sb, "RemoveSnapshotEntities", Phase.REMOVE_SNAPSHOT_ENTITIES, 1);
            line(sb, "DropStaleEvents", Phase.DROP_STALE_EVENTS, 1);
        }
    }

    private static void appendCaptureTree(StringBuilder sb) {
        if (players > 0) {
            line(sb, "Players (" + players + ")", Phase.CAPTURE_PLAYERS, 0);
        }
        line(sb, "Globals", Phase.CAPTURE_GLOBALS, 0);
        line(sb, "Raids & Dragon", Phase.CAPTURE_RAIDS, 0);
        line(sb, "World State", Phase.CAPTURE_WORLD_STATE, 0);
        long chunkSum = NANOS[Phase.CAPTURE_CHUNKS.ordinal()];
        if (chunkSum > 0 || chunks > 0) {
            line(sb, "Chunks (" + chunks + ")", chunkSum);
            line(sb, "Block Entities", Phase.CAPTURE_BE, 1);
            line(sb, "Ticks & POIs", Phase.CAPTURE_TICKS_POIS, 1);
            line(sb, "Entities", Phase.CAPTURE_ENTITIES, 1);
        }
        if (asyncNote != null) {
            sb.append("\n`-- ").append(asyncNote);
        }
    }

    private static long sum(Phase... phases) {
        long total = 0;
        for (Phase phase : phases) {
            total += NANOS[phase.ordinal()];
        }
        return total;
    }

    private static void line(StringBuilder sb, String label, Phase phase, int depth) {
        line(sb, label, NANOS[phase.ordinal()], depth);
    }

    private static void line(StringBuilder sb, String label, long nanos) {
        line(sb, label, nanos, 0);
    }

    private static void line(StringBuilder sb, String label, long nanos, int depth) {
        if (nanos <= 0) return;
        sb.append("\n");
        for (int i = 0; i < depth; i++) {
            sb.append("|   ");
        }
        sb.append("|-- ").append(label).append(": ").append(ms(nanos)).append(" ms");
    }

    private static String ms(long nanos) {
        return String.format("%.1f", nanos / 1_000_000.0);
    }
}
