package ru.reset.rzero.client.cache;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import ru.reset.rzero.client.cache.mesh.RZeroMeshCache;
import ru.reset.rzero.mixin.client.cache.LevelRendererAccessor;

public final class RZeroVisualRollbackManager {
    private RZeroVisualRollbackManager() {}

    public static void execute(Minecraft client, double x, double y, double z, float yRot, float xRot, long gameTime, long dayTime) {
        LocalPlayer player = client.player;
        if (player != null) {
            player.setPos(x, y, z);
            player.xOld = x;
            player.yOld = y;
            player.zOld = z;
            player.xo = x;
            player.yo = y;
            player.zo = z;
            player.setYRot(yRot);
            player.setXRot(xRot);
            player.yRotO = yRot;
            player.xRotO = xRot;
            player.yHeadRot = yRot;
            player.yBodyRot = yRot;
            player.yHeadRotO = yRot;
            player.yBodyRotO = yRot;
            player.yBob = 0.0f;
            player.xBob = 0.0f;
            player.yBobO = 0.0f;
            player.xBobO = 0.0f;
        }

        ClientLevel level = client.level;
        if (level == null) {
            return;
        }

        if (gameTime >= 0L) {
            level.setGameTime(gameTime);
            level.setDayTime(dayTime);
        }

        var snapDim = RZeroClientCache.get().snapshotDimension();
        if (snapDim == null || !snapDim.equals(level.dimension())) {
            return;
        }

        int cx = ((int) Math.floor(x)) >> 4;
        int cz = ((int) Math.floor(z)) >> 4;
        level.getChunkSource().updateViewCenter(cx, cz);
        RZeroClientCache.get().inject(level);

        LevelRenderer levelRenderer = client.levelRenderer;
        if (levelRenderer != null) {
            LevelRendererAccessor lra = (LevelRendererAccessor) levelRenderer;
            ViewArea viewArea = lra.rzero$getViewArea();
            if (viewArea != null) {
                viewArea.repositionCamera(x, z);
            }
        }

        RZeroMeshCache.get().restore();

        if (levelRenderer != null) {
            LevelRendererAccessor lra = (LevelRendererAccessor) levelRenderer;
            SectionOcclusionGraph sog = lra.rzero$getSectionOcclusionGraph();
            if (sog != null) {
                sog.invalidate();
                RZeroClientCache.get().requestSyncOcclusion();
            }
        }

        RZeroClientCache.get().clearPendingRefresh();
    }
}