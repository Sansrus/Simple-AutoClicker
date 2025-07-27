package org.example.sansrus.simpleautoclicker.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class AddKeyScreen extends Screen {
    private final Screen parent;

    public AddKeyScreen(Screen parent) {
        super(Text.literal("Choose action"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = 20;
        for (AutoClickAction action : AutoClickAction.values()) {
            Text label = getActionText(action);

            addDrawableChild(ButtonWidget.builder(label, btn -> {
                AutoClickerConfig.Entry e = new AutoClickerConfig.Entry();
                e.action = action;
                // Сохраняем в конфиг уже переведённое название
                e.name = label.getString();
                AutoClickerConfig.getInstance().entries.add(e);
                AutoClickerConfig.getInstance().save();
                assert client != null;
                client.setScreen(new AutoClickerListScreen(parent));
            }).dimensions(20, y, 120, 20).build());

            y += 25;
        }

        // Кнопка «Назад»
        addDrawableChild(ButtonWidget.builder(
                                Text.translatable("gui.simpleautoclicker.button.cancel"),
                                btn -> {
                                    assert client != null;
                                    client.setScreen(parent);
                                }
                        )
                        .dimensions(20, y, 120, 20)
                        .build()
        );
    }

    private Text getActionText(AutoClickAction action) {
        switch (action) {
            case FORWARD:  return Text.translatable("key.forward");
            case BACKWARD: return Text.translatable("key.back");
            case LEFT:     return Text.translatable("gui.simpleautoclicker.action.left");
            case RIGHT:    return Text.translatable("gui.simpleautoclicker.action.right");
            case JUMP:     return Text.translatable("key.jump");
            case SNEAK:    return Text.translatable("key.sneak");
            case USE_ITEM:return Text.translatable("gui.simpleautoclicker.action.use_item");
            case ATTACK:   return Text.translatable("gui.simpleautoclicker.action.attack");
            default:       return Text.literal(action.name());
        }
    }
}