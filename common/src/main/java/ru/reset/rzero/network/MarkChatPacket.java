package ru.reset.rzero.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record MarkChatPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MarkChatPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rzero", "mark_chat"));
    public static final StreamCodec<FriendlyByteBuf, MarkChatPacket> CODEC = StreamCodec.unit(new MarkChatPacket());
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}