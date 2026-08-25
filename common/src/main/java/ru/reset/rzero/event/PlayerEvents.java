package ru.reset.rzero.event;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.Level;
import ru.reset.rzero.ModGameRules;
import ru.reset.rzero.RZero;
import ru.reset.rzero.checkpoint.CheckpointManager;
import ru.reset.rzero.checkpoint.data.RZeroAdaptiveData;
import ru.reset.rzero.checkpoint.player.AdvancementSnapshot;
import ru.reset.rzero.checkpoint.player.PlayerData;
import ru.reset.rzero.adaptive.AdaptiveSaveEngine;
import ru.reset.rzero.adaptive.PlayerSafetyCheck;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;
import ru.reset.rzero.runtime.SnapshotRegistry;
import ru.reset.rzero.metrics.PlayerMetrics;

public final class PlayerEvents {

    private PlayerEvents() {
    }

    public static void onPlayerJoin(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        if (!overworld.getGameRules().getBoolean(ModGameRules.RULE_AUTO_SAVE)) {
            return;
        }

        applyPendingOfflineRollback(player);

        boolean hasCheckpoint = SnapshotRegistry.hasCheckpoint();
        long gameTime = overworld.getGameTime();
        PlayerMetrics.lastRespawnOrCheckpointTick.put(player.getUUID(), gameTime);

        if (!hasCheckpoint && PlayerSafetyCheck.isPlayerSafe(player)) {
            CheckpointManager.setCheckpoint(player);
            AdaptiveSaveEngine.onCheckpointCreated(player.server, null, gameTime);
            AdaptiveSaveEngine.calculateNextInterval(overworld);
            RZero.LOGGER.info(
                    "[RZero] Zero Cycle Initiated: First checkpoint created upon joining the world.");
        }
    }

    private static void applyPendingOfflineRollback(ServerPlayer player) {
        PlayerData pd = RestoreQueues.pendingOfflineRollbacks.remove(player.getUUID());
        if (pd == null) {
            return;
        }
        ServerLevel overworld = player.server.overworld();
        pd.applyTo(player, player.server, overworld.registryAccess(), RZeroRuntime.checkpointPolicy());
        AdvancementSnapshot.apply(player, player.server, pd.advancements);
        RZeroAdaptiveData.markDirty(player.server);
        RZero.LOGGER.info("[RZero] Applied pending offline rollback for player {}",
                player.getName().getString());
    }

    public static void onRightClickBlock(Entity entity, Level level) {
        if (!level.isClientSide()) {
            PlayerMetrics.blocksPlacedAccumulator.merge(entity.getUUID(), 1, Integer::sum);
        }
    }

    public static void onRightClickItem(Entity entity, Level level, ItemStack itemStack) {
        if (level.isClientSide()) {
            return;
        }
        Item item = itemStack.getItem();
        boolean consumable = item.components().has(DataComponents.FOOD)
                || item instanceof PotionItem
                || item == Items.ENDER_PEARL;
        if (consumable) {
            PlayerMetrics.itemsBurnedAccumulator.merge(entity.getUUID(), 1, Integer::sum);
        }
    }
}
