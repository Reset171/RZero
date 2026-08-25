package ru.reset.rzero.client.cache;

import ru.reset.rzero.RZero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import ru.reset.rzero.client.cache.ext.IRZeroClientChunkCache;
import ru.reset.rzero.client.cache.ext.IRZeroLevelLightEngine;
import ru.reset.rzero.client.cache.ext.IRZeroLightEngine;
import ru.reset.rzero.mixin.client.cache.LevelRendererAccessor;
import ru.reset.rzero.runtime.RZeroRuntime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class RZeroClientCache {

    private static final RZeroClientCache INSTANCE = new RZeroClientCache();
    public static RZeroClientCache get() { return INSTANCE; }

    private final AtomicInteger fakeIdCounter = new AtomicInteger(Integer.MIN_VALUE / 2);

    private volatile boolean inRollback;

    private volatile boolean enabled = true;

    private volatile boolean capturePending;

    private boolean pendingSectionRefresh;
    private int pendingRefreshAttempts;
    private static final int MAX_REFRESH_ATTEMPTS = 200;

    private static final int SESSION_TTL_TICKS = 200;
    private static final int LEAVE_BUFFER_CHUNKS = 4;

    private long sessionExpiresAtTick = -1L;
    private volatile ResourceKey<Level> snapshotDimension;
    private int snapshotCenterX;
    private int snapshotCenterZ;
    private int snapshotRadius;

    private int captureAttempts;

    private static final int MAX_CAPTURE_ATTEMPTS = 200;


    private volatile SpatialGrid<RZeroFakeChunk> snapshotGrid;

    private final Map<UUID, SnapshotEntityState> snapshotEntities = new LinkedHashMap<>();

    public static boolean isCapturing = false;


    private volatile SpatialGrid<RZeroFakeChunk> activeGrid;

    private volatile SpatialGrid<Boolean> activeLightColumns;

    private volatile SpatialSectionGrid<DataLayer> blockLightGrid;

    private volatile SpatialSectionGrid<DataLayer> skyLightGrid;

    private int activeFakeCount;

    private final Map<UUID, Integer> spawnedFakeEntityIds = new LinkedHashMap<>();

    private final Map<Long, List<UUID>> fakeEntityByChunk = new HashMap<>();

    private RZeroClientCache() {}


    private volatile boolean isInterDimensionalRollback;
    private long interDimensionalRollbackArmTick = -1L;

    public ResourceKey<Level> snapshotDimension() { return snapshotDimension; }
    public boolean isInRollback() { return inRollback; }
    public boolean isPendingSectionRefresh() { return pendingSectionRefresh; }

    public void clearPendingRefresh() {
        this.pendingSectionRefresh = false;
        this.pendingRefreshAttempts = 0;
    }

    public boolean isInterDimensionalRollback() {
        if (!isInterDimensionalRollback) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && interDimensionalRollbackArmTick > 0) {
            if (mc.level.getGameTime() - interDimensionalRollbackArmTick > 100) {
                isInterDimensionalRollback = false;
                return false;
            }
        }
        return isInterDimensionalRollback;
    }

    public void armInterDimensionalRollback() {
        this.isInterDimensionalRollback = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            this.interDimensionalRollbackArmTick = mc.level.getGameTime();
        }
    }

    public void clearInterDimensionalRollback() {
        this.isInterDimensionalRollback = false;
        this.interDimensionalRollbackArmTick = -1L;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean newEnabled) {
        if (this.enabled == newEnabled) return;
        this.enabled = newEnabled;
        if (!newEnabled) {
            ClientLevel level = Minecraft.getInstance().level;
            clearInternal(level);
            capturePending = false;
            captureAttempts = 0;
            RZero.LOGGER.info("[RZero][cache] rzerochash DISABLED — snapshot wiped, active fakes detached");
        } else {
            RZero.LOGGER.info("[RZero][cache] rzerochash ENABLED — next /rzero set will capture");
        }
    }

    public RZeroFakeChunk getFakeChunk(int x, int z) {
        if (!enabled) return null;
        SpatialGrid<RZeroFakeChunk> g = activeGrid;
        if (g == null) return null;
        return g.get(x, z);
    }

    public boolean isCapturePending() { return capturePending; }

    public void requestCapture() {
        if (!enabled) return;
        ClientLevel level = Minecraft.getInstance().level;
        clearInternal(level);
        captureAttempts = 0;
        capturePending = true;
        RZero.LOGGER.info("[RZero][cache] requestCapture queued; will retry until chunks stream in (max {} ticks)",
                MAX_CAPTURE_ATTEMPTS);
    }

    public void cancelCapture() {
        if (capturePending) {
            RZero.LOGGER.info("[RZero][cache] capture cancelled after {} attempt(s)", captureAttempts);
        }
        capturePending = false;
        captureAttempts = 0;
    }

    public boolean tryDeferredCapture(ClientLevel level) {
        if (!enabled) { capturePending = false; return false; }
        if (!capturePending) return false;
        captureAttempts++;
        boolean ok = capture(level);
        if (ok) {
            capturePending = false;
            return true;
        }
        if (captureAttempts >= MAX_CAPTURE_ATTEMPTS) {
            RZero.LOGGER.warn("[RZero][cache] giving up after {} attempts — no chunks ever streamed in",
                    captureAttempts);
            capturePending = false;
        }
        return false;
    }


    public boolean capture(ClientLevel level) {
        if (!enabled) return false;
        if (level == null) return false;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;

        int centerX = player.chunkPosition().x;
        int centerZ = player.chunkPosition().z;
        int radius = Math.max(2, Minecraft.getInstance().options.getEffectiveRenderDistance());
        ClientChunkCache cache = level.getChunkSource();
        LevelLightEngine lightEngine = level.getLightEngine();
        LayerLightEventListener blockLightView = lightEngine.getLayerListener(LightLayer.BLOCK);
        LayerLightEventListener skyLightView   = lightEngine.getLayerListener(LightLayer.SKY);

        SpatialGrid<RZeroFakeChunk> grid = new SpatialGrid<>(centerX, centerZ, radius);

        int captured = 0;
        int skipped = 0;
        int alreadyFake = 0;

        isCapturing = true;
        try {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int cx = centerX + dx;
                    int cz = centerZ + dz;
                    LevelChunk live = cache.getChunk(cx, cz, ChunkStatus.FULL, false);
                    if (live == null) { skipped++; continue; }
                    if (live instanceof RZeroFakeChunk) { alreadyFake++; continue; }

                    RZeroFakeChunk snapshot = snapshotChunk(level, live, blockLightView, skyLightView);
                    if (snapshot != null) {
                        grid.set(cx, cz, snapshot);
                        captured++;
                    }
                }
            }
        } finally {
            isCapturing = false;
        }

        int total = (2 * radius + 1) * (2 * radius + 1);

        if (captured == 0) {
            if (captureAttempts <= 1 || captureAttempts % 20 == 0) {
                RZero.LOGGER.info(
                        "[RZero][cache] capture pending: 0/{} chunks ready (skipped(null)={}, alreadyFake={}, attempt={})",
                        total, skipped, alreadyFake, captureAttempts);
            }
            return false;
        }

        this.snapshotGrid = grid;

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof LocalPlayer) continue;
            if (entity instanceof RemotePlayer) continue;
            if (Math.abs(entity.chunkPosition().x - centerX) > radius) continue;
            if (Math.abs(entity.chunkPosition().z - centerZ) > radius) continue;

            snapshotEntities.put(entity.getUUID(), new SnapshotEntityState(entity));
        }

        this.snapshotDimension = level.dimension();
        this.snapshotCenterX = centerX;
        this.snapshotCenterZ = centerZ;
        this.snapshotRadius = radius;

        var restore = RZeroRuntime.clientRestore();
        if (restore.meshCacheEnabled()) {
            int meshRadius = Math.min(radius, restore.meshCacheRadius());
            try {
                ru.reset.rzero.client.cache.mesh.RZeroMeshCache.get()
                        .capture(level, centerX, centerZ, meshRadius, restore.meshCacheBudgetBytes());
            } catch (Throwable t) {
                RZero.LOGGER.warn("[RZero][mesh] capture failed, continuing with block-data cache only", t);
                ru.reset.rzero.client.cache.mesh.RZeroMeshCache.get().clear();
            }
        }

        RZero.LOGGER.info(
                "[RZero][cache] capture OK: chunks={}/{} (skipped(null)={}, alreadyFake={}), entities={}, dim={}, center=[{},{}], r={}, attempt={}",
                captured, total, skipped, alreadyFake, snapshotEntities.size(),
                this.snapshotDimension.location(), centerX, centerZ, radius, captureAttempts);
        return true;
    }

    private static RZeroFakeChunk snapshotChunk(ClientLevel level, LevelChunk live,
                                                LayerLightEventListener blockLightView,
                                                LayerLightEventListener skyLightView) {
        LevelChunkSection[] liveSections = live.getSections();
        LevelChunkSection[] copySections = new LevelChunkSection[liveSections.length];
        DataLayer[] blockLight = new DataLayer[liveSections.length];
        DataLayer[] skyLight   = new DataLayer[liveSections.length];

        int sectionIdx = 0;
        for (int sectionY = live.getMinSection(); sectionY < live.getMaxSection(); sectionY++, sectionIdx++) {
            LevelChunkSection src = liveSections[sectionIdx];
            if (src == null) continue;

            @SuppressWarnings("unchecked")
            PalettedContainer<net.minecraft.world.level.block.state.BlockState> statesCopy =
                    ((PalettedContainer<net.minecraft.world.level.block.state.BlockState>) src.getStates()).copy();

            copySections[sectionIdx] = new LevelChunkSection(statesCopy, src.getBiomes());
            ((ru.reset.rzero.access.IRZeroLevelChunkSection) copySections[sectionIdx]).rzero$copyCountsFrom(src);

            long sectionPosKey = SectionPos.asLong(live.getPos().x, sectionY, live.getPos().z);
            DataLayer bl = blockLightView.getDataLayerData(SectionPos.of(sectionPosKey));
            DataLayer sl = skyLightView   != null ? skyLightView.getDataLayerData(SectionPos.of(sectionPosKey)) : null;
            blockLight[sectionIdx] = (bl != null && !bl.isEmpty()) ? bl : null;
            skyLight[sectionIdx]   = (sl != null && !sl.isEmpty()) ? sl : null;
        }

        RZeroFakeChunk fake = new RZeroFakeChunk(level, live.getPos(), copySections, blockLight, skyLight);
        for (Map.Entry<Heightmap.Types, Heightmap> hm : live.getHeightmaps()) {
            fake.rzero$setHeightmap(hm.getKey(), hm.getValue());
        }
        return fake;
    }


    public void inject(ClientLevel level) {
        if (!enabled) return;
        if (level == null) return;
        if (this.snapshotDimension == null || !this.snapshotDimension.equals(level.dimension())) {
            RZero.LOGGER.info("[RZero][cache] inject() skipped: dimension mismatch (snapshot={}, live={})",
                    this.snapshotDimension != null ? this.snapshotDimension.location() : "null",
                    level.dimension().location());
            return;
        }
        SpatialGrid<RZeroFakeChunk> snapshot = this.snapshotGrid;
        if (snapshot == null && snapshotEntities.isEmpty()) {
            if (capturePending) {
                RZero.LOGGER.warn(
                        "[RZero][cache] inject(): capture still pending after {} attempt(s) — nothing to show",
                        captureAttempts);
            } else {
                RZero.LOGGER.info("[RZero][cache] inject(): nothing captured, skipping");
            }
            return;
        }

        detachActive(level);

        ClientChunkCache cache = level.getChunkSource();
        IRZeroClientChunkCache cacheExt = (IRZeroClientChunkCache) cache;
        LevelLightEngine lightEngine = level.getLightEngine();
        IRZeroLevelLightEngine lightExt = IRZeroLevelLightEngine.get(lightEngine);
        IRZeroLightEngine blockLightExt = IRZeroLightEngine.get(lightEngine.getLayerListener(LightLayer.BLOCK));
        IRZeroLightEngine skyLightExt   = IRZeroLightEngine.get(lightEngine.getLayerListener(LightLayer.SKY));

        cache.updateViewCenter(snapshotCenterX, snapshotCenterZ);

        int minSection = level.getMinSection();
        int maxSection = level.getMaxSection();
        SpatialGrid<RZeroFakeChunk> active = new SpatialGrid<>(snapshotCenterX, snapshotCenterZ, snapshotRadius);
        SpatialGrid<Boolean> activeColumns = new SpatialGrid<>(snapshotCenterX, snapshotCenterZ, snapshotRadius);
        SpatialSectionGrid<DataLayer> blockLight = new SpatialSectionGrid<>(
                snapshotCenterX, snapshotCenterZ, snapshotRadius, minSection, maxSection);
        SpatialSectionGrid<DataLayer> skyLight = new SpatialSectionGrid<>(
                snapshotCenterX, snapshotCenterZ, snapshotRadius, minSection, maxSection);

        int attached = 0;
        int skippedAlreadyReal = 0;
        if (snapshot != null) {
            int r = snapshot.radius;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int cx = snapshot.centerX + dx;
                    int cz = snapshot.centerZ + dz;
                    RZeroFakeChunk chunk = snapshot.get(cx, cz);
                    if (chunk == null) continue;

                    LevelChunk live = cache.getChunk(cx, cz, ChunkStatus.FULL, false);
                    if (live != null && !(live instanceof RZeroFakeChunk)) {
                        skippedAlreadyReal++;
                        continue;
                    }

                    active.set(cx, cz, chunk);
                    activeColumns.set(cx, cz, Boolean.TRUE);
                    attached++;
                    level.onChunkLoaded(new ChunkPos(cx, cz));

                    int sectionIdx = 0;
                    for (int sectionY = chunk.getMinSection(); sectionY < chunk.getMaxSection(); sectionY++, sectionIdx++) {
                        if (chunk.blockLight[sectionIdx] != null) {
                            blockLight.set(cx, sectionY, cz, chunk.blockLight[sectionIdx]);
                        }
                        if (chunk.skyLight[sectionIdx] != null) {
                            skyLight.set(cx, sectionY, cz, chunk.skyLight[sectionIdx]);
                        }
                    }

                }
            }
        }

        this.activeGrid = active;
        this.activeLightColumns = activeColumns;
        this.blockLightGrid = blockLight;
        this.skyLightGrid = skyLight;
        this.activeFakeCount = attached;

        cacheExt.rzero$setGrid(active);
        if (lightExt != null) lightExt.rzero$setActiveColumns(activeColumns);
        if (blockLightExt != null) blockLightExt.rzero$setSectionGrid(blockLight);
        if (skyLightExt   != null) skyLightExt.rzero$setSectionGrid(skyLight);

        this.pendingSectionRefresh = true;

        Map<UUID, Entity> existingEntities = new HashMap<>();
        for (Entity ent : level.entitiesForRendering()) {
            existingEntities.put(ent.getUUID(), ent);
        }

        int spawned = 0;
        List<SnapshotEntityState> orderedSnapshots = new ArrayList<>(snapshotEntities.values());
        orderedSnapshots.sort(Comparator.comparing(state -> state.entity.getUUID().toString()));
        for (SnapshotEntityState state : orderedSnapshots) {
            UUID uuid = state.entity.getUUID();
            Entity existing = existingEntities.get(uuid);
            boolean isExistingReal = existing != null && !spawnedFakeEntityIds.containsValue(existing.getId());

            if (isExistingReal) {
                existing.setPosRaw(state.x, state.y, state.z);
                existing.setYRot(state.yRot);
                existing.setXRot(state.xRot);
                if (existing instanceof net.minecraft.world.entity.LivingEntity le) {
                    le.yHeadRot = state.yHeadRot;
                    le.yBodyRot = state.yBodyRot;
                    le.yHeadRotO = state.yHeadRot;
                    le.yBodyRotO = state.yBodyRot;
                }
                existing.xOld = state.x;
                existing.yOld = state.y;
                existing.zOld = state.z;
                existing.xo = state.x;
                existing.yo = state.y;
                existing.zo = state.z;
                existing.xRotO = state.xRot;
                existing.yRotO = state.yRot;
                continue;
            }

            if (existing != null) {
                level.removeEntity(existing.getId(), Entity.RemovalReason.DISCARDED);
            }

            Entity fake = state.entity;
            if (fake.isRemoved()) {
                ((ru.reset.rzero.mixin.entity.MixinEntityAccessor) fake).rzero$unsetRemoved();
            }
            int id = fakeIdCounter.incrementAndGet();
            fake.setId(id);

            fake.setPosRaw(state.x, state.y, state.z);
            fake.setYRot(state.yRot);
            fake.setXRot(state.xRot);
            if (fake instanceof net.minecraft.world.entity.LivingEntity le) {
                le.yHeadRot = state.yHeadRot;
                le.yBodyRot = state.yBodyRot;
                le.yHeadRotO = state.yHeadRot;
                le.yBodyRotO = state.yBodyRot;
            }
            fake.xOld = state.x;
            fake.yOld = state.y;
            fake.zOld = state.z;
            fake.xo = state.x;
            fake.yo = state.y;
            fake.zo = state.z;
            fake.xRotO = state.xRot;
            fake.yRotO = state.yRot;

            level.addEntity(fake);
            spawnedFakeEntityIds.put(uuid, id);
            long chunkKey = ChunkPos.asLong(SectionPos.blockToSectionCoord((int) state.x),
                                            SectionPos.blockToSectionCoord((int) state.z));
            fakeEntityByChunk.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(uuid);
            spawned++;
        }

        refreshRollbackFlag();

        this.sessionExpiresAtTick = level.getGameTime() + SESSION_TTL_TICKS;

        RZero.LOGGER.info(
                "[RZero][cache] inject: attached {} fake chunks (skippedAlreadyReal={}), spawned {} fake entities (snapshot entities={}); session TTL armed for {} ticks",
                attached, skippedAlreadyReal, spawned, snapshotEntities.size(), SESSION_TTL_TICKS);
    }

    public void tickPendingRefresh() {
        if (!enabled || !pendingSectionRefresh) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || this.snapshotDimension == null || !this.snapshotDimension.equals(level.dimension())) return;

        int dx = player.chunkPosition().x - snapshotCenterX;
        int dz = player.chunkPosition().z - snapshotCenterZ;
        if (Math.abs(dx) > snapshotRadius || Math.abs(dz) > snapshotRadius) {
            if (++pendingRefreshAttempts >= MAX_REFRESH_ATTEMPTS) {
                pendingSectionRefresh = false;
                pendingRefreshAttempts = 0;
                RZero.LOGGER.info("[RZero][cache] deferred refresh abandoned: camera never entered the snapshot zone");
            }
            return;
        }

        pendingSectionRefresh = false;

        ru.reset.rzero.client.cache.mesh.RZeroMeshCache meshCache = ru.reset.rzero.client.cache.mesh.RZeroMeshCache.get();
        int meshRestored = meshCache.restore();

        if (meshRestored == 0 && meshCache.hasSnapshot() && ++pendingRefreshAttempts < MAX_REFRESH_ATTEMPTS) {
            pendingSectionRefresh = true;
            RZero.LOGGER.info("[RZero][cache] deferred refresh: 0 sections reinstated (ViewArea not ready, retry {}/{})",
                    pendingRefreshAttempts, MAX_REFRESH_ATTEMPTS);
            return;
        }

        pendingRefreshAttempts = 0;

        int marked = 0;
        SpatialGrid<RZeroFakeChunk> active = this.activeGrid;
        if (active != null) {
            int r = active.radius;
            for (int ox = -r; ox <= r; ox++) {
                for (int oz = -r; oz <= r; oz++) {
                    int cx = active.centerX + ox;
                    int cz = active.centerZ + oz;
                    RZeroFakeChunk chunk = active.get(cx, cz);
                    if (chunk == null) continue;
                    for (int sectionY = chunk.getMinSection(); sectionY < chunk.getMaxSection(); sectionY++) {
                        long originKey = BlockPos.asLong(cx << 4, sectionY << 4, cz << 4);
                        if (meshCache.hasRecord(originKey)) {
                            continue;
                        }
                        level.setSectionDirtyWithNeighbors(cx, sectionY, cz);
                        marked++;
                    }
                }
            }
        }

        LevelRenderer levelRenderer = mc.levelRenderer;
        if (levelRenderer != null) {
            LevelRendererAccessor lra = (LevelRendererAccessor) levelRenderer;
            SectionOcclusionGraph sog = lra.rzero$getSectionOcclusionGraph();
            if (sog != null) {
                sog.invalidate();
            }
        }

        RZero.LOGGER.info("[RZero][cache] deferred refresh: {} sections marked dirty for fallback, "
                + "{} sections reinstated from mesh cache", marked, meshRestored);
    }

    public void tickSession() {
        if (!enabled || !inRollback || pendingSectionRefresh) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        net.minecraft.client.player.LocalPlayer player = mc.player;
        if (level == null || player == null || this.snapshotDimension == null || !this.snapshotDimension.equals(level.dimension())) return;
        long now = level.getGameTime();

        if (sessionExpiresAtTick > 0 && now >= sessionExpiresAtTick) {
            int chunks = activeFakeCount;
            int ents = spawnedFakeEntityIds.size();
            sessionExpiresAtTick = -1L;
            detachActive(level);
            RZero.LOGGER.info("[RZero][cache] session TTL expired, dropped {} residual fake chunks + {} fake entities",
                    chunks, ents);
            return;
        }

        int dx = player.chunkPosition().x - snapshotCenterX;
        int dz = player.chunkPosition().z - snapshotCenterZ;
        int leaveLimit = snapshotRadius + LEAVE_BUFFER_CHUNKS;
        if (Math.abs(dx) > leaveLimit || Math.abs(dz) > leaveLimit) {
            int chunks = activeFakeCount;
            int ents = spawnedFakeEntityIds.size();
            sessionExpiresAtTick = -1L;
            detachActive(level);
            RZero.LOGGER.info("[RZero][cache] player left snapshot zone (Δ=[{},{}], limit={}), dropped {} residual fake chunks + {} fake entities",
                    dx, dz, leaveLimit, chunks, ents);
        }
    }


    public void onRealChunkArrived(int x, int z) {
        SpatialGrid<RZeroFakeChunk> grid = activeGrid;
        if (grid == null && spawnedFakeEntityIds.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        if (grid != null && grid.get(x, z) != null) {
            grid.set(x, z, null);
            SpatialGrid<Boolean> cols = activeLightColumns;
            if (cols != null) cols.set(x, z, null);
            SpatialSectionGrid<DataLayer> bl = blockLightGrid;
            if (bl != null) bl.clearColumn(x, z);
            SpatialSectionGrid<DataLayer> sl = skyLightGrid;
            if (sl != null) sl.clearColumn(x, z);
            activeFakeCount--;
        }

        List<UUID> inChunk = fakeEntityByChunk.remove(ChunkPos.asLong(x, z));
        if (inChunk != null) {
            for (UUID uuid : inChunk) {
                Integer fakeId = spawnedFakeEntityIds.remove(uuid);
                if (fakeId != null) {
                    level.removeEntity(fakeId, Entity.RemovalReason.DISCARDED);
                }
            }
        }

        refreshRollbackFlag();
    }

    public void onRealEntityAdded(UUID uuid) {
        Integer fakeId = spawnedFakeEntityIds.remove(uuid);
        if (fakeId == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) level.removeEntity(fakeId, Entity.RemovalReason.DISCARDED);
        refreshRollbackFlag();
    }


    private void refreshRollbackFlag() {
        inRollback = activeFakeCount > 0 || !spawnedFakeEntityIds.isEmpty();
    }

    private void detachActive(ClientLevel level) {
        if (activeFakeCount == 0 && spawnedFakeEntityIds.isEmpty()
                && activeGrid == null && activeLightColumns == null
                && blockLightGrid == null && skyLightGrid == null) return;

        if (level != null) {
            LevelLightEngine lightEngine = level.getLightEngine();
            IRZeroLevelLightEngine lightExt = IRZeroLevelLightEngine.get(lightEngine);
            IRZeroLightEngine blockLightExt = IRZeroLightEngine.get(lightEngine.getLayerListener(LightLayer.BLOCK));
            IRZeroLightEngine skyLightExt   = IRZeroLightEngine.get(lightEngine.getLayerListener(LightLayer.SKY));
            IRZeroClientChunkCache cacheExt = (IRZeroClientChunkCache) level.getChunkSource();

            cacheExt.rzero$setGrid(null);
            if (lightExt != null) lightExt.rzero$setActiveColumns(null);
            if (blockLightExt != null) blockLightExt.rzero$setSectionGrid(null);
            if (skyLightExt   != null) skyLightExt.rzero$setSectionGrid(null);

            for (Integer id : spawnedFakeEntityIds.values()) {
                level.removeEntity(id, Entity.RemovalReason.DISCARDED);
            }
        }

        this.activeGrid = null;
        this.activeLightColumns = null;
        this.blockLightGrid = null;
        this.skyLightGrid = null;
        this.activeFakeCount = 0;
        spawnedFakeEntityIds.clear();
        fakeEntityByChunk.clear();
        refreshRollbackFlag();
    }

    public void clear() {
        clearInternal(Minecraft.getInstance().level);
    }

    private void clearInternal(ClientLevel level) {
        detachActive(level);
        snapshotDimension = null;
        snapshotGrid = null;
        snapshotEntities.clear();
        pendingSectionRefresh = false;
        pendingRefreshAttempts = 0;
        ru.reset.rzero.client.cache.mesh.RZeroMeshCache.get().clear();
    }

    private static class SnapshotEntityState {
        final Entity entity;
        final double x, y, z;
        final float yRot, xRot, yHeadRot, yBodyRot;
        SnapshotEntityState(Entity entity) {
            this.entity = entity;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
            this.yRot = entity.getYRot();
            this.xRot = entity.getXRot();
            if (entity instanceof net.minecraft.world.entity.LivingEntity le) {
                this.yHeadRot = le.getYHeadRot();
                this.yBodyRot = le.yBodyRot;
            } else {
                this.yHeadRot = this.yRot;
                this.yBodyRot = this.yRot;
            }
        }
    }
}
