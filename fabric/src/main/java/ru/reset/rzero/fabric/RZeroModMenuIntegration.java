package ru.reset.rzero.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import ru.reset.rzero.client.gui.RZeroConfigScreen;

public class RZeroModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return RZeroConfigScreen::create;
    }
}
