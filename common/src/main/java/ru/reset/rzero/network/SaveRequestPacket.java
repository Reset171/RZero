package ru.reset.rzero.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record SaveRequestPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SaveRequestPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rzero", "save_req"));
    public static final StreamCodec<FriendlyByteBuf, SaveRequestPacket> CODEC = StreamCodec.unit(new SaveRequestPacket());
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}