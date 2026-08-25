package ru.reset.rzero.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RollbackChatPacket(double x, double y, double z, float yRot, float xRot, long gameTime, long dayTime) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RollbackChatPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rzero", "rollback_chat"));
    public static final StreamCodec<FriendlyByteBuf, RollbackChatPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeDouble(packet.x());
                buf.writeDouble(packet.y());
                buf.writeDouble(packet.z());
                buf.writeFloat(packet.yRot());
                buf.writeFloat(packet.xRot());
                buf.writeVarLong(packet.gameTime());
                buf.writeVarLong(packet.dayTime());
            },
            buf -> new RollbackChatPacket(
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readVarLong(),
                    buf.readVarLong()
            )
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}