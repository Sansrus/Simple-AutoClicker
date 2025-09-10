package org.example.sansrus.simpleautoclicker.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AddKeyScreen extends Screen {
    private final Screen parent;
    private int scrollOffset = 0;

    public AddKeyScreen(Screen parent) {
        super(Text.literal("Choose action"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        final int itemHeight = 25; // кнопка 20 + отступ 5
        final int visibleTop = 50;
        final int visibleBottom = this.height - 70;
        final int visibleHeight = Math.max(0, visibleBottom - visibleTop);

        AutoClickAction[] actions = AutoClickAction.values();
        int totalItems = actions.length;
        int totalHeight = totalItems * itemHeight;

        // Клэмп scrollOffset на допустимый диапазон
        int maxScroll = Math.max(0, totalHeight - visibleHeight);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        // Добавляем только те кнопки, которые пересекают видимую область
        for (int i = 0; i < totalItems; i++) {
            AutoClickAction action = actions[i];
            int y = visibleTop - scrollOffset + i * itemHeight;

            // если кнопка хотя бы частично в видимой зоне -> добавляем
            if (y + 20 > visibleTop && y < visibleBottom) {
                Text label = getActionText(action);
                addDrawableChild(ButtonWidget.builder(label, btn -> {
                    AutoClickerConfig.Entry e = new AutoClickerConfig.Entry();
                    e.action = action;
                    // сохраняем уже переведённое имя
                    e.name = label.getString();
                    AutoClickerConfig.getInstance().entries.add(e);
                    AutoClickerConfig.getInstance().save();
                    assert client != null;
                    client.setScreen(new AutoClickerListScreen(parent));
                }).dimensions(20, y, 180, 20).build());
            }
        }

        // Фиксированная кнопка «Назад» — расположена ниже видимой области списка
        addDrawableChild(ButtonWidget.builder(
                                Text.translatable("gui.simpleautoclicker.button.cancel"),
                                btn -> {
                                    assert client != null;
                                    client.setScreen(parent);
                                }
                        )
                        .dimensions(20, this.height - 40, 180, 20)
                        .build()
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (client == null) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        final int itemHeight = 25;
        final int visibleTop = 50;
        final int visibleBottom = this.height - 70;
        final int visibleHeight = Math.max(0, visibleBottom - visibleTop);

        int totalItems = AutoClickAction.values().length;
        int totalHeight = totalItems * itemHeight;
        if (totalHeight > visibleHeight) {
            int maxScroll = Math.max(0, totalHeight - visibleHeight);
            // verticalAmount > 0 -> прокрутка вверх (колёсико к себе). Следует инвертировать при необходимости.
            int delta = (int)(verticalAmount * 20);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - delta));
            clearChildren();
            init();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Choose action"), this.width / 2, 10, 0xFFFFFFFF);

        // Рисуем скроллбар (если нужно)
        final int itemHeight = 25;
        final int visibleTop = 50;
        final int visibleBottom = this.height - 70;
        final int visibleHeight = Math.max(0, visibleBottom - visibleTop);

        int totalItems = AutoClickAction.values().length;
        int totalHeight = totalItems * itemHeight;
        if (totalHeight > visibleHeight) {
            int maxScroll = Math.max(0, totalHeight - visibleHeight);
            if (maxScroll > 0) {
                int scrollBarHeight = Math.max(10, visibleHeight * visibleHeight / totalHeight);
                int scrollBarY = visibleTop + (int)((float)scrollOffset / maxScroll * (visibleHeight - scrollBarHeight));
                // фон полосы
                context.fill(this.width - 15, visibleTop, this.width - 10, visibleBottom, 0x80FFFFFF);
                // сам бегунок
                context.fill(this.width - 15, scrollBarY, this.width - 10, scrollBarY + scrollBarHeight, 0xFFFFFFFF);
            }
        }
    }

    private Text getActionText(AutoClickAction action) {
        return switch (action) {
            case FORWARD -> Text.translatable("key.forward");
            case BACKWARD -> Text.translatable("key.back");
            case LEFT -> Text.translatable("gui.simpleautoclicker.action.left");
            case RIGHT -> Text.translatable("gui.simpleautoclicker.action.right");
            case JUMP -> Text.translatable("key.jump");
            case SNEAK -> Text.translatable("key.sneak");
            case USE_ITEM -> Text.translatable("gui.simpleautoclicker.action.use_item");
            case ATTACK -> Text.translatable("gui.simpleautoclicker.action.attack");
            case SPRINT -> Text.translatable("gui.simpleautoclicker.action.sprint");
            case DROP -> Text.translatable("gui.simpleautoclicker.action.drop");
            case PICK_BLOCK -> Text.translatable("gui.simpleautoclicker.action.pick_block");
            case SWAP_HANDS -> Text.translatable("gui.simpleautoclicker.action.swap_hands");
            case TOGGLE_PERSPECTIVE -> Text.translatable("gui.simpleautoclicker.action.toggle_perspective");
            case SCREENSHOT -> Text.translatable("gui.simpleautoclicker.action.screenshot");
            case HOTBAR_1 -> Text.translatable("gui.simpleautoclicker.action.hotbar1");
            case HOTBAR_2 -> Text.translatable("gui.simpleautoclicker.action.hotbar2");
            case HOTBAR_3 -> Text.translatable("gui.simpleautoclicker.action.hotbar3");
            case HOTBAR_4 -> Text.translatable("gui.simpleautoclicker.action.hotbar4");
            case HOTBAR_5 -> Text.translatable("gui.simpleautoclicker.action.hotbar5");
            case HOTBAR_6 -> Text.translatable("gui.simpleautoclicker.action.hotbar6");
            case HOTBAR_7 -> Text.translatable("gui.simpleautoclicker.action.hotbar7");
            case HOTBAR_8 -> Text.translatable("gui.simpleautoclicker.action.hotbar8");
            case HOTBAR_9 -> Text.translatable("gui.simpleautoclicker.action.hotbar9");
            default -> Text.literal(action.name());
        };
    }
}
