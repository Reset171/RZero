package ru.reset.rzero.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import ru.reset.rzero.RZero;
import ru.reset.rzero.RZeroClient;
import ru.reset.rzero.client.cache.RZeroClientCache;
import ru.reset.rzero.client.input.KeyBindings;

@EventBusSubscriber(modid = RZero.MODID, value = Dist.CLIENT)
public class RZeroClientNeoForge {

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.SAVE_KEY);
        event.register(KeyBindings.LOAD_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        RZeroClient.clientTick();
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        RZeroClientCache.get().clear();
    }
}