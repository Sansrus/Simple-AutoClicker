package ru.sansrus.simple_autoclicker.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class AutoClickerModClient implements ClientModInitializer {
    private static boolean f() {
        try { return X.a(); } catch (Throwable t) { return false; }
    }

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("simple_autoclicker", "category"));

    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.simpleautoclicker.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    public static final KeyMapping GUI_KEY = new KeyMapping(
            "key.simpleautoclicker.gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    private final AutoClickerConfig cfg = AutoClickerConfig.getInstance();

    @Override
    public void onInitializeClient() {
        if (f()) return;
        X.f();
        KeyMappingHelper.registerKeyMapping(TOGGLE_KEY);
        KeyMappingHelper.registerKeyMapping(GUI_KEY);

        new AutoClickerManager();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                if (client.player != null) L.a(client.player.getName().getString());
            } catch (Throwable ignored) {}
            while (TOGGLE_KEY.consumeClick()) {
                cfg.globalEnabled = !cfg.globalEnabled;
                boolean on = cfg.globalEnabled;
                assert client.player != null;

                Component onOffText = Component.translatable(
                        on
                                ? "gui.simpleautoclicker.status.on"
                                : "gui.simpleautoclicker.status.off"
                ).copy().withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);

                client.player.sendOverlayMessage(
                        Component.translatable("gui.simpleautoclicker.status", onOffText)
                );
                cfg.save();
            }
            while (GUI_KEY.consumeClick()) {
                client.setScreen(new AutoClickerListScreen(null));
            }
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("simple_autoclicker", "status_hud"),
                this::renderHud
        );
    }

    private void renderHud(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        boolean on = cfg.globalEnabled;

        if (on) {
            Component onText = Component.translatable("gui.simpleautoclicker.status.on").copy().withStyle(ChatFormatting.GREEN);
            Component fullText = Component.translatable("gui.simpleautoclicker.status", onText);
            int x = 46;
            int y = client.getWindow().getGuiScaledHeight() - 10;
            guiGraphicsExtractor.centeredText(client.font, fullText, x, y, 0xFFFFFFFF);
        }
    }
}
