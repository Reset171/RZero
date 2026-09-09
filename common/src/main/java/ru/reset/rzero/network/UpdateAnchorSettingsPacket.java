package ru.reset.rzero.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ru.reset.rzero.anchor.AnchorMode;
import ru.reset.rzero.anchor.RZeroAnchorSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record UpdateAnchorSettingsPacket(RZeroAnchorSettings settings) implements CustomPacketPayload {
    public static final Type<UpdateAnchorSettingsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("rzero", "update_anchor_settings"));

    public static final StreamCodec<FriendlyByteBuf, UpdateAnchorSettingsPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUtf(packet.settings().mode().id());
                buf.writeVarInt(packet.settings().rotationSeconds());
                buf.writeVarInt(packet.settings().rollbackCooldownSeconds());
                List<UUID> pinned = packet.settings().pinned();
                buf.writeVarInt(pinned.size());
                for (UUID id : pinned) {
                    buf.writeUUID(id);
                }
            },
            buf -> {
                AnchorMode mode = AnchorMode.byId(buf.readUtf());
                int rotationSeconds = buf.readVarInt();
                int cooldownSeconds = buf.readVarInt();
                int size = buf.readVarInt();
                List<UUID> pinned = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    pinned.add(buf.readUUID());
                }
                return new UpdateAnchorSettingsPacket(new RZeroAnchorSettings(mode, rotationSeconds, cooldownSeconds, pinned));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}