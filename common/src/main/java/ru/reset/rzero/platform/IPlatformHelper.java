package ru.reset.rzero.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface IPlatformHelper {
    void sendToPlayer(ServerPlayer player, CustomPacketPayload packet);
    void sendToServer(CustomPacketPayload packet);
}