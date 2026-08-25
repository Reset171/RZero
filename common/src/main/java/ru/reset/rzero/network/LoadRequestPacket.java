package ru.reset.rzero.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record LoadRequestPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LoadRequestPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rzero", "load_req"));
    public static final StreamCodec<FriendlyByteBuf, LoadRequestPacket> CODEC = StreamCodec.unit(new LoadRequestPacket());
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}