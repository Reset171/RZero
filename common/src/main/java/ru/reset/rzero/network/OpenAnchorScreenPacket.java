package ru.reset.rzero.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenAnchorScreenPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenAnchorScreenPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rzero", "open_anchor_screen"));
    public static final StreamCodec<FriendlyByteBuf, OpenAnchorScreenPacket> CODEC =
            StreamCodec.unit(new OpenAnchorScreenPacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}