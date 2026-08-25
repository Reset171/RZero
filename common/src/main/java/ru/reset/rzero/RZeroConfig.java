package ru.reset.rzero;

import ru.reset.rzero.config.RZeroSettings;
import ru.reset.rzero.runtime.RZeroRuntime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RZeroConfig {
    private static final Path CONFIG_PATH = Paths.get("config", "rzero.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                RZeroRuntime.setSettings(RZeroSettings.fromJson(obj));
            } catch (Exception e) {
                RZero.LOGGER.error("[RZero] Failed to load config", e);
                RZeroRuntime.setSettings(RZeroSettings.defaults());
            }
        } else {
            RZeroRuntime.setSettings(RZeroSettings.defaults());
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject obj = RZeroRuntime.settings().toJson();
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(obj, writer);
            }
        } catch (Exception e) {
            RZero.LOGGER.error("[RZero] Failed to save config", e);
        }
    }
}
