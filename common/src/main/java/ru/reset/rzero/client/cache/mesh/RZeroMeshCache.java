package ru.reset.rzero.client.cache.mesh;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import ru.reset.rzero.RZero;
import ru.reset.rzero.mixin.client.cache.LevelRendererAccessor;
import ru.reset.rzero.mixin.client.cache.VertexBufferAccessor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RZeroMeshCache {

    private static final RZeroMeshCache INSTANCE = new RZeroMeshCache();

    public static RZeroMeshCache get() {
        return INSTANCE;
    }

    private static final int GL_ARRAY_BUFFER = 34962;
    private static final int GL_ELEMENT_ARRAY_BUFFER = 34963;

    private final GeometryArena arena = new GeometryArena();
    private final Long2ObjectMap<SectionMeshRecord> records = new Long2ObjectOpenHashMap<>();

    private ResourceKey<Level> snapshotDimension;
    private int snapshotCenterX;
    private int snapshotCenterZ;
    private int snapshotRadius;

    private long lastCaptureMillis;
    private int lastCapturedSections;
    private int lastRestoredSections;
    private int lastSkippedFormat;
    private long lastRestoreTimeNanos;

    private static final long RESTORE_DEBOUNCE_NANOS = 50_000_000L;

    private RZeroMeshCache() {}

    public boolean hasSnapshot() {
        return !this.records.isEmpty() && this.arena.isAllocated() && this.snapshotDimension != null;
    }

    public boolean hasRecord(long originKey) {
        return this.records.containsKey(originKey);
    }

    public long arenaBytes() {
        return this.arena.used();
    }

    public int sectionCount() {
        return this.records.size();
    }

    public void clear() {
        if (!this.records.isEmpty() || this.arena.isAllocated()) {
            RZero.logInfo("[RZero][mesh] dropping snapshot ({} sections, {} MB)",
                    this.records.size(), this.arena.used() / (1024 * 1024));
        }
        this.records.clear();
        this.arena.free();
        this.snapshotDimension = null;
        this.lastCapturedSections = 0;
        this.lastRestoredSections = 0;
        this.lastRestoreTimeNanos = 0L;
    }


    public boolean capture(ClientLevel level, int centerX, int centerZ, int radius, long maxBytes) {
        this.clear();
        if (level == null || maxBytes <= 0L || !MeshCacheSupport.isSupported()) {
            return false;
        }
        ViewArea viewArea = viewArea();
        if (viewArea == null || viewArea.sections == null) {
            RZero.logInfo("[RZero][mesh] capture skipped: no ViewArea yet");
            return false;
        }

        long start = System.nanoTime();
        List<PendingLayer> pending = new ArrayList<>();
        List<PendingSection> sections = new ArrayList<>();
        long totalBytes = 0L;

        for (SectionRenderDispatcher.RenderSection section : viewArea.sections) {
            if (section == null) {
                continue;
            }
            BlockPos origin = section.getOrigin();
            int cx = origin.getX() >> 4;
            int cz = origin.getZ() >> 4;
            if (Math.abs(cx - centerX) > radius || Math.abs(cz - centerZ) > radius) {
                continue;
            }
            SectionRenderDispatcher.CompiledSection compiled = section.getCompiled();
            if (compiled == SectionRenderDispatcher.CompiledSection.UNCOMPILED) {
                continue;
            }

            java.util.List<net.minecraft.world.level.block.entity.BlockEntity> bes =
                    compiled.getRenderableBlockEntities();
            long[] bePositions = new long[bes.size()];
            for (int i = 0; i < bes.size(); i++) {
                bePositions[i] = bes.get(i).getBlockPos().asLong();
            }

            long[] globalPositions = new long[0];
            try {
                java.util.Set<net.minecraft.world.level.block.entity.BlockEntity> globals =
                        ((ru.reset.rzero.mixin.client.cache.RenderSectionAccessor) section)
                                .getGlobalBlockEntities();
                if (globals != null && !globals.isEmpty()) {
                    globalPositions = new long[globals.size()];
                    int gi = 0;
                    for (net.minecraft.world.level.block.entity.BlockEntity gbe : globals) {
                        globalPositions[gi++] = gbe.getBlockPos().asLong();
                    }
                }
            } catch (Throwable ignored) {}

            int dx = cx - centerX;
            int dz = cz - centerZ;
            PendingSection ps = new PendingSection(origin.asLong(), compiled, dx * dx + dz * dz,
                    bePositions, globalPositions);
            sections.add(ps);

            if (compiled.hasNoRenderableLayers()) {
                continue;
            }

            for (RenderType type : RenderType.chunkBufferLayers()) {
                VertexBuffer vb = section.getBuffer(type);
                if (vb == null || vb.isInvalid()) {
                    continue;
                }
                VertexBufferAccessor acc = (VertexBufferAccessor) vb;
                VertexFormat format = acc.rzero$getFormat();
                int indexCount = acc.rzero$getIndexCount();
                VertexFormat.Mode mode = acc.rzero$getMode();
                if (format == null || indexCount <= 0 || mode == null) {
                    continue;
                }
                int vertexId = acc.rzero$getVertexBufferId();
                long vertexSize = GeometryArena.queryBufferSize(vertexId);
                if (vertexSize <= 0L) {
                    continue;
                }
                RenderSystem.AutoStorageIndexBuffer sequential = acc.rzero$getSequentialIndices();
                long indexSize = 0L;
                int indexId = 0;
                if (sequential == null) {
                    indexId = acc.rzero$getIndexBufferId();
                    indexSize = GeometryArena.queryBufferSize(indexId);
                }

                pending.add(new PendingLayer(ps, type, vertexId, vertexSize, indexId, indexSize,
                        format, sequential, acc.rzero$getIndexType(), indexCount, mode));
                ps.expectedLayers++;
                totalBytes += vertexSize + indexSize;
            }
        }

        if (pending.isEmpty()) {
            RZero.logInfo("[RZero][mesh] capture found no compiled geometry in r={} — nothing to cache", radius);
            return false;
        }

        long budget = Math.min(totalBytes, maxBytes);
        if (!this.arena.allocate(budget)) {
            return false;
        }

        pending.sort(Comparator.comparingInt(p -> p.section.distanceSq));

        int droppedBudget = 0;
        for (PendingLayer layer : pending) {
            long vertexOffset = this.arena.push(layer.vertexId, layer.vertexSize);
            if (vertexOffset < 0L) {
                droppedBudget++;
                continue;
            }
            long indexOffset = -1L;
            if (layer.sequential == null && layer.indexSize > 0L) {
                indexOffset = this.arena.push(layer.indexId, layer.indexSize);
                if (indexOffset < 0L) {
                    droppedBudget++;
                    continue;
                }
            }
            layer.section.layers.add(new SectionMeshRecord.LayerMesh(
                    layer.type, vertexOffset, layer.vertexSize, indexOffset, layer.indexSize,
                    layer.format, layer.sequential, layer.indexType, layer.indexCount, layer.mode));
        }

        for (PendingSection ps : sections) {
            if (ps.layers.size() != ps.expectedLayers) {
                continue;
            }
            this.records.put(ps.originKey,
                    new SectionMeshRecord(ps.originKey, ps.compiled, ps.bePositions, ps.globalPositions, ps.layers));
        }

        this.snapshotDimension = level.dimension();
        this.snapshotCenterX = centerX;
        this.snapshotCenterZ = centerZ;
        this.snapshotRadius = radius;
        this.lastCapturedSections = this.records.size();
        this.lastCaptureMillis = (System.nanoTime() - start) / 1_000_000L;

        RZero.logInfo("[RZero][mesh] capture OK: {} sections, {} layers, {}/{} MB arena "
                        + "(requested {} MB, dropped {} layers over budget), dim={}, center=[{},{}], r={}, {} ms",
                this.records.size(), pending.size() - droppedBudget,
                this.arena.used() / (1024 * 1024), this.arena.capacity() / (1024 * 1024),
                totalBytes / (1024 * 1024), droppedBudget,
                this.snapshotDimension.location(), centerX, centerZ, radius, this.lastCaptureMillis);
        return true;
    }


    public int restore() {
        if (!this.hasSnapshot() || !MeshCacheSupport.isSupported()) {
            return 0;
        }
        long now = System.nanoTime();
        if (now - this.lastRestoreTimeNanos < RESTORE_DEBOUNCE_NANOS && this.lastRestoredSections > 0) {
            return this.lastRestoredSections;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || this.snapshotDimension == null || !this.snapshotDimension.equals(level.dimension())) {
            return 0;
        }
        ViewArea viewArea = viewArea();
        if (viewArea == null || viewArea.sections == null) {
            return 0;
        }

        long start = System.nanoTime();
        int restored = 0;
        int skippedFormat = 0;
        BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();

        for (SectionRenderDispatcher.RenderSection section : viewArea.sections) {
            if (section == null) {
                continue;
            }
            SectionMeshRecord record = this.records.get(section.getOrigin().asLong());
            if (record == null) {
                continue;
            }

            if (record.layers.isEmpty() && !record.compiled.hasNoRenderableLayers()) {
                continue;
            }

            boolean ok = true;
            for (SectionMeshRecord.LayerMesh layer : record.layers) {
                if (!restoreLayer(section, layer)) {
                    ok = false;
                    skippedFormat++;
                }
            }
            if (!ok) {
                continue;
            }

            java.util.List<net.minecraft.world.level.block.entity.BlockEntity> renderList =
                    record.compiled.getRenderableBlockEntities();
            if (record.blockEntityPositions.length > 0) {
                renderList.clear();
                if (level != null) {
                    for (long pos : record.blockEntityPositions) {
                        mutPos.set(pos);
                        net.minecraft.world.level.block.entity.BlockEntity be =
                                level.getBlockEntity(mutPos);
                        if (be != null) renderList.add(be);
                    }
                }
            } else if (!renderList.isEmpty()) {
                renderList.clear();
            }

            section.compiled.set(record.compiled);

            if (record.globalBlockEntityPositions.length > 0) {
                try {
                    java.util.Set<net.minecraft.world.level.block.entity.BlockEntity> globals =
                            ((ru.reset.rzero.mixin.client.cache.RenderSectionAccessor) section)
                                    .getGlobalBlockEntities();
                    if (globals != null) {
                        globals.clear();
                        if (level != null) {
                            for (long pos : record.globalBlockEntityPositions) {
                                mutPos.set(pos);
                                net.minecraft.world.level.block.entity.BlockEntity be =
                                        level.getBlockEntity(mutPos);
                                if (be != null) globals.add(be);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            section.setNotDirty();
            restored++;
        }

        long millis = (System.nanoTime() - start) / 1_000_000L;
        this.lastRestoredSections = restored;
        this.lastSkippedFormat = skippedFormat;
        this.lastRestoreTimeNanos = System.nanoTime();
        RZero.logInfo("[RZero][Perf-Profile][mesh] restore: {} of {} sections reinstated without re-meshing "
                        + "({} layers skipped) | elapsed: {} ms",
                restored, this.records.size(), skippedFormat, millis);
        return restored;
    }

    private boolean restoreLayer(SectionRenderDispatcher.RenderSection section,
                                 SectionMeshRecord.LayerMesh layer) {
        VertexBuffer vb = section.getBuffer(layer.type());
        if (vb == null || vb.isInvalid()) {
            return false;
        }
        VertexBufferAccessor acc = (VertexBufferAccessor) vb;
        int vertexId = acc.rzero$getVertexBufferId();
        if (vertexId == 0) {
            return false;
        }

        vb.bind();
        try {
            if (!this.arena.pull(layer.vertexOffset(), layer.vertexSize(), vertexId)) {
                return false;
            }

            VertexFormat current = acc.rzero$getFormat();
            if (!layer.format().equals(current)) {
                if (current != null) {
                    current.clearBufferState();
                }
                GlStateManager._glBindBuffer(GL_ARRAY_BUFFER, vertexId);
                layer.format().setupBufferState();
            }
            acc.rzero$setFormat(layer.format());

            if (layer.sequentialIndices() != null) {
                layer.sequentialIndices().bind(layer.indexCount());
                int bufferName = ((ru.reset.rzero.mixin.client.cache.AutoStorageIndexBufferAccessor) (Object) layer.sequentialIndices()).rzero$getName();
                GlStateManager._glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName);
                acc.rzero$setSequentialIndices(layer.sequentialIndices());
            } else if (layer.hasOwnIndexBuffer()) {
                int indexId = acc.rzero$getIndexBufferId();
                if (indexId == 0) {
                    return false;
                }
                if (!this.arena.pull(layer.indexOffset(), layer.indexSize(), indexId)) {
                    return false;
                }
                GlStateManager._glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexId);
                acc.rzero$setSequentialIndices(null);
            } else {
                return false;
            }

            acc.rzero$setIndexType(layer.indexType());
            acc.rzero$setIndexCount(layer.indexCount());
            acc.rzero$setMode(layer.mode());
            return true;
        } catch (Throwable t) {
            RZero.LOGGER.warn("[RZero][mesh] failed to restore a {} layer, leaving it to re-mesh",
                    layer.type(), t);
            return false;
        } finally {
            VertexBuffer.unbind();
        }
    }


    private static ViewArea viewArea() {
        LevelRenderer renderer = Minecraft.getInstance().levelRenderer;
        if (renderer == null) {
            return null;
        }
        return ((LevelRendererAccessor) renderer).rzero$getViewArea();
    }

    public int snapshotCenterX() {
        return this.snapshotCenterX;
    }

    public int snapshotCenterZ() {
        return this.snapshotCenterZ;
    }

    public int snapshotRadius() {
        return this.snapshotRadius;
    }

    public ResourceKey<Level> snapshotDimension() {
        return this.snapshotDimension;
    }

    public String stats() {
        return "sections=" + this.records.size()
                + " arena=" + (this.arena.used() / (1024 * 1024)) + "MB"
                + " captured=" + this.lastCapturedSections
                + " restored=" + this.lastRestoredSections
                + " skipped=" + this.lastSkippedFormat
                + " captureMs=" + this.lastCaptureMillis;
    }

    private static final class PendingSection {
        final long originKey;
        final SectionRenderDispatcher.CompiledSection compiled;
        final int distanceSq;
        final long[] bePositions;
        final long[] globalPositions;
        final List<SectionMeshRecord.LayerMesh> layers = new ArrayList<>(4);
        int expectedLayers;

        PendingSection(long originKey, SectionRenderDispatcher.CompiledSection compiled, int distanceSq,
                       long[] bePositions, long[] globalPositions) {
            this.originKey = originKey;
            this.compiled = compiled;
            this.distanceSq = distanceSq;
            this.bePositions = bePositions;
            this.globalPositions = globalPositions;
        }
    }

    private static final class PendingLayer {
        final PendingSection section;
        final RenderType type;
        final int vertexId;
        final long vertexSize;
        final int indexId;
        final long indexSize;
        final VertexFormat format;
        final RenderSystem.AutoStorageIndexBuffer sequential;
        final VertexFormat.IndexType indexType;
        final int indexCount;
        final VertexFormat.Mode mode;

        PendingLayer(PendingSection section, RenderType type, int vertexId, long vertexSize,
                     int indexId, long indexSize, VertexFormat format,
                     RenderSystem.AutoStorageIndexBuffer sequential, VertexFormat.IndexType indexType,
                     int indexCount, VertexFormat.Mode mode) {
            this.section = section;
            this.type = type;
            this.vertexId = vertexId;
            this.vertexSize = vertexSize;
            this.indexId = indexId;
            this.indexSize = indexSize;
            this.format = format;
            this.sequential = sequential;
            this.indexType = indexType;
            this.indexCount = indexCount;
            this.mode = mode;
        }
    }
}
