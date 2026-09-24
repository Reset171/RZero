package ru.reset.rzero.mixin.player;

import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.ServerStatsCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(PlayerList.class)
public interface MixinPlayerList {
    @Accessor("stats")
    Map<UUID, ServerStatsCounter> rzero$getStats();
}