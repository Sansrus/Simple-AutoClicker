package org.example.sansrus.simpleautoclicker.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
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

        HudRenderCallback.EVENT.register(this::renderHud);
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

    private void renderHud(DrawContext drawContext, RenderTickCounter renderTickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        boolean on = cfg.globalEnabled;

        if (on) {
            Text onText = Text.translatable("gui.simpleautoclicker.status.on").formatted(Formatting.GREEN);
            Text fullText = Text.translatable("gui.simpleautoclicker.status", onText);
            int x = 46;
            int y = client.getWindow().getScaledHeight() - 10;
            drawContext.drawCenteredTextWithShadow(client.textRenderer, fullText, x, y, 0xFFFFFFFF);
        }
    }
}