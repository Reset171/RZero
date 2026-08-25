package ru.reset.rzero.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RzerochashTogglePacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<RzerochashTogglePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("rzero", "rzerochash_toggle"));
    public static final StreamCodec<FriendlyByteBuf, RzerochashTogglePacket> CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, RzerochashTogglePacket::enabled, RzerochashTogglePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
