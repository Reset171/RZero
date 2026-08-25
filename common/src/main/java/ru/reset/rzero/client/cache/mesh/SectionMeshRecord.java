package ru.reset.rzero.client.cache.mesh;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

import java.util.List;

public final class SectionMeshRecord {

    public final long originKey;

    public final SectionRenderDispatcher.CompiledSection compiled;

    public final long[] blockEntityPositions;

    public final long[] globalBlockEntityPositions;

    public final List<LayerMesh> layers;

    public SectionMeshRecord(long originKey,
                             SectionRenderDispatcher.CompiledSection compiled,
                             long[] blockEntityPositions,
                             long[] globalBlockEntityPositions,
                             List<LayerMesh> layers) {
        this.originKey = originKey;
        this.compiled = compiled;
        this.blockEntityPositions = blockEntityPositions;
        this.globalBlockEntityPositions = globalBlockEntityPositions;
        this.layers = layers;
    }

    public long byteSize() {
        long total = 0L;
        for (LayerMesh layer : this.layers) {
            total += layer.vertexSize() + Math.max(0L, layer.indexSize());
        }
        return total;
    }

    public record LayerMesh(RenderType type,
                            long vertexOffset,
                            long vertexSize,
                            long indexOffset,
                            long indexSize,
                            VertexFormat format,
                            RenderSystem.AutoStorageIndexBuffer sequentialIndices,
                            VertexFormat.IndexType indexType,
                            int indexCount,
                            VertexFormat.Mode mode) {

        public boolean hasOwnIndexBuffer() {
            return this.sequentialIndices == null && this.indexOffset >= 0L && this.indexSize > 0L;
        }
    }
}
