package ru.reset.rzero.checkpoint.player;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancementSnapshot {

    private AdvancementSnapshot() {
    }

    public static CompoundTag capture(ServerPlayer player) {
        CompoundTag advTag = new CompoundTag();
        if (player.server == null) {
            return advTag;
        }
        for (AdvancementHolder advancement : player.server.getAdvancements().getAllAdvancements()) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            CompoundTag progressTag = new CompoundTag();
            for (String criterion : progress.getCompletedCriteria()) {
                progressTag.putBoolean(criterion, true);
            }
            advTag.put(advancement.id().toString(), progressTag);
        }
        return advTag;
    }

    public static void apply(ServerPlayer player, MinecraftServer server, CompoundTag saved) {
        if (saved == null) {
            return;
        }
        ServerAdvancementManager manager = server.getAdvancements();
        for (AdvancementHolder advHolder : manager.getAllAdvancements()) {
            String advKey = advHolder.id().toString();
            CompoundTag savedProgressTag = saved.contains(advKey) ? saved.getCompound(advKey) : null;
            AdvancementProgress currentProgress =
                    player.getAdvancements().getOrStartProgress(advHolder);

            if (!currentProgress.hasProgress() && savedProgressTag == null) {
                continue;
            }

            for (String criterion : advHolder.value().criteria().keySet()) {
                boolean wasCompleted = savedProgressTag != null && savedProgressTag.getBoolean(criterion);
                var criterionProgress = currentProgress.getCriterion(criterion);
                boolean isCompleted = criterionProgress != null && criterionProgress.isDone();
                if (isCompleted && !wasCompleted) {
                    player.getAdvancements().revoke(advHolder, criterion);
                } else if (!isCompleted && wasCompleted) {
                    player.getAdvancements().award(advHolder, criterion);
                }
            }
        }
    }
}
