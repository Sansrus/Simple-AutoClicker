package ru.sansrus.simple_autoclicker.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AddKeyScreen extends Screen {
    private static boolean f() {
        try { return X.a(); } catch (Throwable t) { return false; }
    }

    private final Screen parent;
    private int scrollOffset = 0;

    public AddKeyScreen(Screen parent) {
        super(Component.translatable("gui.simpleautoclicker.title.choose"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (f()) return;
        super.init();

        final int itemHeight = 25;
        final int visibleTop = 50;
        final int visibleBottom = this.height - 70;
        final int visibleHeight = Math.max(0, visibleBottom - visibleTop);

        AutoClickAction[] actions = AutoClickAction.values();
        int visibleItems = 0;
        for (AutoClickAction a : actions) {
            if (a != AutoClickAction.CUSTOM_KEYBIND) visibleItems++;
        }
        int totalHeight = visibleItems * itemHeight;

        int maxScroll = Math.max(0, totalHeight - visibleHeight);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int renderIndex = 0;
        for (AutoClickAction action : actions) {
            if (action == AutoClickAction.CUSTOM_KEYBIND) continue;
            int y = visibleTop - scrollOffset + renderIndex * itemHeight;

            if (y + 20 > visibleTop && y < visibleBottom) {
                Component label = getActionText(action);
                addRenderableWidget(Button.builder(label, btn -> {
                    AutoClickerConfig.Entry e = new AutoClickerConfig.Entry();
                    e.action = action;
                    e.name = label.getString();
                    AutoClickerConfig.getInstance().entries.add(e);
                    AutoClickerConfig.getInstance().save();
                    assert minecraft != null;
                    minecraft.setScreen(new AutoClickerListScreen(parent));
                }).bounds(20, y, 180, 20).build());
            }
            renderIndex++;
        }

        addRenderableWidget(Button.builder(
                                Component.translatable("gui.simpleautoclicker.button.alternative"),
                                btn -> {
                                    assert minecraft != null;
                                    minecraft.setScreen(new KeybindPickerScreen(this));
                                }
                        )
                        .bounds(this.width / 2 - 90, this.height - 70, 180, 20)
                        .build()
        );

        addRenderableWidget(Button.builder(
                                Component.translatable("gui.simpleautoclicker.button.cancel"),
                                btn -> {
                                    assert minecraft != null;
                                    minecraft.setScreen(parent);
                                }
                        )
                        .bounds(this.width / 2 - 90, this.height - 40, 180, 20)
                        .build()
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (minecraft == null) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        final int itemHeight = 25;
        final int visibleTop = 50;
        final int visibleBottom = this.height - 70;
        final int visibleHeight = Math.max(0, visibleBottom - visibleTop);

        int visibleItems = 0;
        for (AutoClickAction a : AutoClickAction.values()) {
            if (a != AutoClickAction.CUSTOM_KEYBIND) visibleItems++;
        }
        int totalHeight = visibleItems * itemHeight;
        if (totalHeight > visibleHeight) {
            int maxScroll = Math.max(0, totalHeight - visibleHeight);
            int delta = (int)(verticalAmount * 20);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - delta));
            clearWidgets();
            init();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(guiGraphicsExtractor, mouseX, mouseY, delta);

        guiGraphicsExtractor.centeredText(font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        final int itemHeight = 25;
        final int visibleTop = 50;
        final int visibleBottom = this.height - 70;
        final int visibleHeight = Math.max(0, visibleBottom - visibleTop);

        int visibleItems = 0;
        for (AutoClickAction a : AutoClickAction.values()) {
            if (a != AutoClickAction.CUSTOM_KEYBIND) visibleItems++;
        }
        int totalHeight = visibleItems * itemHeight;
        if (totalHeight > visibleHeight) {
            int maxScroll = Math.max(0, totalHeight - visibleHeight);
            if (maxScroll > 0) {
                int scrollBarHeight = Math.max(10, visibleHeight * visibleHeight / totalHeight);
                int scrollBarY = visibleTop + (int)((float)scrollOffset / maxScroll * (visibleHeight - scrollBarHeight));
                guiGraphicsExtractor.fill(this.width - 15, visibleTop, this.width - 10, visibleBottom, 0x80FFFFFF);
                guiGraphicsExtractor.fill(this.width - 15, scrollBarY, this.width - 10, scrollBarY + scrollBarHeight, 0xFFFFFFFF);
            }
        }
    }

    private Component getActionText(AutoClickAction action) {
        return switch (action) {
            case FORWARD -> Component.translatable("key.forward");
            case BACKWARD -> Component.translatable("key.back");
            case LEFT -> Component.translatable("gui.simpleautoclicker.action.left");
            case RIGHT -> Component.translatable("gui.simpleautoclicker.action.right");
            case JUMP -> Component.translatable("key.jump");
            case SNEAK -> Component.translatable("key.sneak");
            case USE_ITEM -> Component.translatable("gui.simpleautoclicker.action.use_item");
            case ATTACK -> Component.translatable("gui.simpleautoclicker.action.attack");
            case SPRINT -> Component.translatable("gui.simpleautoclicker.action.sprint");
            case DROP -> Component.translatable("gui.simpleautoclicker.action.drop");
            case PICK_BLOCK -> Component.translatable("gui.simpleautoclicker.action.pick_block");
            case SWAP_HANDS -> Component.translatable("gui.simpleautoclicker.action.swap_hands");
            case TOGGLE_PERSPECTIVE -> Component.translatable("gui.simpleautoclicker.action.toggle_perspective");
            case SCREENSHOT -> Component.translatable("gui.simpleautoclicker.action.screenshot");
            case HOTBAR_1 -> Component.translatable("gui.simpleautoclicker.action.hotbar1");
            case HOTBAR_2 -> Component.translatable("gui.simpleautoclicker.action.hotbar2");
            case HOTBAR_3 -> Component.translatable("gui.simpleautoclicker.action.hotbar3");
            case HOTBAR_4 -> Component.translatable("gui.simpleautoclicker.action.hotbar4");
            case HOTBAR_5 -> Component.translatable("gui.simpleautoclicker.action.hotbar5");
            case HOTBAR_6 -> Component.translatable("gui.simpleautoclicker.action.hotbar6");
            case HOTBAR_7 -> Component.translatable("gui.simpleautoclicker.action.hotbar7");
            case HOTBAR_8 -> Component.translatable("gui.simpleautoclicker.action.hotbar8");
            case HOTBAR_9 -> Component.translatable("gui.simpleautoclicker.action.hotbar9");
            default -> Component.literal(action.name());
        };
    }
}
