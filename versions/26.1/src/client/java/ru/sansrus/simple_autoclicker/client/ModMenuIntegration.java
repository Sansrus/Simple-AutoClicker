package ru.sansrus.simple_autoclicker.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    private static boolean f() {
        try { return X.a(); } catch (Throwable t) { return false; }
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (f()) return parent -> null;
        return AutoClickerListScreen::new;
    }
}
