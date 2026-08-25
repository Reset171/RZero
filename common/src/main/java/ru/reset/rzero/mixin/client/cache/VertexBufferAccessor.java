package ru.reset.rzero.mixin.client.cache;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VertexBuffer.class)
public interface VertexBufferAccessor {

    @Accessor("vertexBufferId")
    int rzero$getVertexBufferId();

    @Accessor("indexBufferId")
    int rzero$getIndexBufferId();

    @Accessor("format")
    VertexFormat rzero$getFormat();

    @Accessor("format")
    void rzero$setFormat(VertexFormat format);

    @Accessor("sequentialIndices")
    RenderSystem.AutoStorageIndexBuffer rzero$getSequentialIndices();

    @Accessor("sequentialIndices")
    void rzero$setSequentialIndices(RenderSystem.AutoStorageIndexBuffer sequentialIndices);

    @Accessor("indexType")
    VertexFormat.IndexType rzero$getIndexType();

    @Accessor("indexType")
    void rzero$setIndexType(VertexFormat.IndexType indexType);

    @Accessor("indexCount")
    int rzero$getIndexCount();

    @Accessor("indexCount")
    void rzero$setIndexCount(int indexCount);

    @Accessor("mode")
    VertexFormat.Mode rzero$getMode();

    @Accessor("mode")
    void rzero$setMode(VertexFormat.Mode mode);
}
