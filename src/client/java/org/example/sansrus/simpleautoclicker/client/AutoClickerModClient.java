package org.example.sansrus.simpleautoclicker.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class AutoClickerModClient implements ClientModInitializer {
    public static final KeyBinding TOGGLE_KEY = new KeyBinding(
            "key.simpleautoclicker.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, "category.simpleautoclicker");
    public static final KeyBinding GUI_KEY = new KeyBinding(
            "key.simpleautoclicker.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F4, "category.simpleautoclicker");
    private final AutoClickerConfig cfg = AutoClickerConfig.getInstance();

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(TOGGLE_KEY);
        KeyBindingHelper.registerKeyBinding(GUI_KEY);

        new AutoClickerManager();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.wasPressed()) {
                cfg.globalEnabled = !cfg.globalEnabled;
                boolean on = cfg.globalEnabled;
                assert client.player != null;

                // строим цветной текст "ON" или "OFF"
                Text onOffText = Text.translatable(
                        on
                                ? "gui.simpleautoclicker.status.on"   // ключ для "ON"
                                : "gui.simpleautoclicker.status.off"  // ключ для "OFF"
                ).formatted(on ? Formatting.GREEN : Formatting.RED);

                // отправляем сообщение "AutoClicker <ON/OFF>", переведённое по языку клиента
                client.player.sendMessage(
                        Text.translatable("gui.simpleautoclicker.status", onOffText),
                        true
                );
                cfg.save();
            }
            while (GUI_KEY.wasPressed()) {
                client.setScreen(new AutoClickerListScreen(null));
            }
        });
    }
}