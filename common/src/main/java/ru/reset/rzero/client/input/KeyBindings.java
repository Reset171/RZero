package ru.reset.rzero.client.input;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyMapping SAVE_KEY = new KeyMapping("key.rzero.save", GLFW.GLFW_KEY_UNKNOWN, "key.categories.rzero");
    public static final KeyMapping LOAD_KEY = new KeyMapping("key.rzero.load", GLFW.GLFW_KEY_UNKNOWN, "key.categories.rzero");
}