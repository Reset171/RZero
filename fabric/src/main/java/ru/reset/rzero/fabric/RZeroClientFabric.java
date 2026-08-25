package ru.reset.rzero.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import ru.reset.rzero.RZeroClient;
import ru.reset.rzero.client.cache.RZeroClientCache;
import ru.reset.rzero.client.input.KeyBindings;
import ru.reset.rzero.network.MarkChatPacket;
import ru.reset.rzero.network.RollbackChatPacket;
import ru.reset.rzero.network.RzerochashTogglePacket;

public class RZeroClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(KeyBindings.SAVE_KEY);
        KeyBindingHelper.registerKeyBinding(KeyBindings.LOAD_KEY);

        ClientPlayNetworking.registerGlobalReceiver(MarkChatPacket.TYPE, (payload, context) -> {
            context.client().execute(RZeroClient::handleMarkChat);
        });

        ClientPlayNetworking.registerGlobalReceiver(RollbackChatPacket.TYPE, (payload, context) -> {
            ru.reset.rzero.client.cache.RZeroClientCache.get().armInterDimensionalRollback();
            context.client().execute(() -> RZeroClient.handleRollbackChat(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(RzerochashTogglePacket.TYPE, (payload, context) -> {
            context.client().execute(() -> RZeroClient.handleRzerochashToggle(payload.enabled()));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            client.execute(() -> RZeroClientCache.get().clear());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> RZeroClient.clientTick());
    }
}