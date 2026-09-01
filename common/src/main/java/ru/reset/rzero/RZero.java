package ru.reset.rzero;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.reset.rzero.runtime.RZeroRuntime;

public final class RZero {

    public static final String MODID = "rzero";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final ResourceLocation RBD_SOUND_ID =
            ResourceLocation.fromNamespaceAndPath(MODID, "rbd_sound");
    public static final SoundEvent RBD_SOUND = SoundEvent.createVariableRangeEvent(RBD_SOUND_ID);

    public static boolean verbose() {
        return RZeroRuntime.settings().logs();
    }

    public static void logInfo(String format, Object... args) {
        if (verbose()) LOGGER.info(format, args);
    }

    private RZero() {
    }
}
