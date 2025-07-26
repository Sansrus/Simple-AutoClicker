//package org.example.sansrus.simpleautoclicker.client;
//
//import net.fabricmc.api.EnvType;
//import net.fabricmc.api.Environment;
//import net.minecraft.client.gui.screen.Screen;
//import net.minecraft.client.gui.widget.*;
//import net.minecraft.text.Text;
//
//@Environment(EnvType.CLIENT)
//public class AutoClickerListScreen extends Screen {
//    private final Screen parent;
//    private AutoClickerConfig config;
//
//    public AutoClickerListScreen(Screen parent) {
//        super(Text.translatable("gui.simpleautoclicker.title"));
//        this.parent = parent;
//        this.config = AutoClickerConfig.getInstance();
//    }
//
//    @Override
//    protected void init() {
//        super.init();
//
//        int y = 30;
//        int spacing = 20;
//
//        for (AutoClickerConfig.Entry entry : config.entries) {
//            int x = 10;
//
//            // 1) Переключатель Enable (CheckboxWidget шириной 20px)
//            var toggle = CheckboxWidget.builder(Text.translatable("gui.simpleautoclicker.button.enable"), textRenderer)
//                    .pos(10 + 20, y)
//                    .checked(entry.enabled)
//                    .callback((cb, val) -> {
//                        entry.enabled = val;
//                        config.save();
//                    })
//                    .build();
//            addDrawableChild(toggle);
//            x += 80 + spacing;  // 20px ширина + 20px отступ
//
//            // 2) Поле «Название» (TextFieldWidget шириной 100px)
//            TextFieldWidget nameField = new TextFieldWidget(
//                    textRenderer, x, y, 100, 20,
//                    Text.translatable("gui.simpleautoclicker.field.name")
//            );
//            nameField.setText(entry.name);
//            nameField.setChangedListener(val -> {
//                entry.name = val;
//                config.save();
//            });
//            addDrawableChild(nameField);
//            addSelectableChild(nameField);
//            x += 90 + spacing;
//
//            // 3) Интервал (TextFieldWidget шириной 40px)
//            TextFieldWidget intervalField = new TextFieldWidget(
//                    textRenderer, x, y, 40, 20,
//                    Text.translatable("gui.simpleautoclicker.label.interval")
//            );
//            intervalField.setText(String.valueOf(entry.intervalTicks));
//            intervalField.setChangedListener(val -> {
//                try {
//                    entry.intervalTicks = Integer.parseInt(val);
//                    config.save();
//                } catch (NumberFormatException ignored) {}
//            });
//            addDrawableChild(intervalField);
//            addSelectableChild(intervalField);
//            x += 30 + spacing;
//
//            // 4) Таймер использования (TextFieldWidget шириной 40px)
//            TextFieldWidget useField = new TextFieldWidget(
//                    textRenderer, x, y, 40, 20,
//                    Text.translatable("gui.simpleautoclicker.field.use_duration")
//            );
//            useField.setText(String.valueOf(entry.useDurationTicks));
//            useField.setChangedListener(val -> {
//                try {
//                    entry.useDurationTicks = Integer.parseInt(val);
//                    config.save();
//                } catch (NumberFormatException ignored) {}
//            });
//            addDrawableChild(useField);
//            addSelectableChild(useField);
//            x += 30 + spacing;
//
//            // 5) Кнопка «Удалить» (ButtonWidget шириной 50px)
//            ButtonWidget deleteBtn = ButtonWidget.builder(
//                            Text.translatable("gui.simpleautoclicker.button.delete"),
//                            btn -> {
//                                config.entries.remove(entry);
//                                config.save();
//                                client.setScreen(new AutoClickerListScreen(parent));
//                            }
//                    )
//                    .dimensions(x, y, 50, 20)
//                    .build();
//            addDrawableChild(deleteBtn);
//            x += 40 + spacing;
//
//            String playerName = "";
//            // 6)–7) Доп. переключатели только для ATTACK
//            if (entry.action == AutoClickAction.ATTACK) {
//                if (client != null && client.player != null) {
//                    playerName = client.player.getName().getString(); // Получаем имя как строку
//                }
//
//
//                if ("Sansrus".equals(playerName)) {
//                    var onlyEntToggle = CheckboxWidget.builder(Text.translatable("gui.simpleautoclicker.toggle.only_entity"), textRenderer)
//                            .pos(x, y)
//                            .checked(entry.onlyEntityMode)
//                            .callback((cb, val) -> {
//                                entry.onlyEntityMode = val;
//                                config.save();
//                            })
//                            .build();
//                    addDrawableChild(onlyEntToggle);
//                    x += 60 + spacing;
//
//                    // cooldownMode (Checkbox шириной 20px)
//                    var cdToggle = CheckboxWidget.builder(Text.translatable("gui.simpleautoclicker.toggle.cooldown_mode"), textRenderer)
//                            .pos(x, y)
//                            .checked(entry.cooldownMode)
//                            .callback((cb, val) -> {
//                                entry.cooldownMode = val;
//                                config.save();
//                            })
//                            .build();
//                    addDrawableChild(cdToggle);
//                    // x += 20 + spacing; // если не нужны дальше элементы
//                }
//            }
//            y += 25;
//        }
//
//        // Кнопка «+»
//        addDrawableChild(ButtonWidget.builder(
//                                Text.literal("+"),
//                                btn -> {
//                                    assert client != null;
//                                    client.setScreen(new AddKeyScreen(this));
//                                }
//                        )
//                        .dimensions(this.width - 30, 10, 20, 20)
//                        .build()
//        );
//
//        // Кнопка «Отмена»
//        addDrawableChild(ButtonWidget.builder(
//                                Text.translatable("gui.simpleautoclicker.button.cancel"),
//                                btn -> {
//                                    assert client != null;
//                                    client.setScreen(parent);
//                                }
//                        )
//                        .dimensions(10, this.height - 30, 60, 20)
//                        .build()
//        );
//    }
//
//
//    @Override
//    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
//        super.render(context, mouseX, mouseY, delta);
//        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.simpleautoclicker.title"), this.width / 2, 10, 0xFFFFFFFF);
//
//        // Имя автокликов
//        context.drawText(
//                this.textRenderer,
//                Text.translatable("gui.simpleautoclicker.field.name"),
//                110,             // X‑координата поля
//                20,       // Y‑координата над полем
//                0xFFFFFFFF,          // цвет текста (белый)
//                false              // без обрезки по границе
//        );
//
//        // Таймер ожидания
//        context.drawText(
//                this.textRenderer,
//                Text.translatable("gui.simpleautoclicker.label.interval"),
//                220,             // X‑координата поля
//                20,       // Y‑координата над полем
//                0xFFFFFFFF,          // цвет текста (белый)
//                false              // без обрезки по границе
//        );
//
//        // Таймер использования
//        context.drawText(
//                this.textRenderer,
//                Text.translatable("gui.simpleautoclicker.field.use_duration"),
//                270,             // X‑координата поля
//                20,       // Y‑координата над полем
//                0xFFFFFFFF,          // цвет текста (белый)
//                false              // без обрезки по границе
//        );
//
//        String playerName = "";
//        if (client != null && client.player != null) {
//            playerName = client.player.getName().getString(); // Получаем имя как строку
//        }
//
//        if ("Sansrus".equals(playerName)) {
//            context.drawText(
//                    this.textRenderer,
//                    Text.translatable("gui.simpleautoclicker.field.features"),
//                    385,             // X‑координата поля
//                    20,       // Y‑координата над полем
//                    0xFFFFFFFF,          // цвет текста (белый)
//                    false              // без обрезки по границе
//            );
//        }
//    }
//}


package org.example.sansrus.simpleautoclicker.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class AutoClickerListScreen extends Screen {
    private final Screen parent;
    private AutoClickerConfig config;
    private int scrollOffset = 0;

    public AutoClickerListScreen(Screen parent) {
        super(Text.translatable("gui.simpleautoclicker.title"));
        this.parent = parent;
        this.config = AutoClickerConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        int y = 50 - scrollOffset;
        int spacing = 20;

        for (AutoClickerConfig.Entry entry : config.entries) {
            // Проверяем, видим ли элемент на экране
            if (y > 40 && y < this.height - 40) {
                int x = 10;

                // 1) Переключатель Enable (CheckboxWidget шириной 20px)
                var toggle = CheckboxWidget.builder(Text.translatable("gui.simpleautoclicker.button.enable"), textRenderer)
                        .pos(10 + 20, y)
                        .checked(entry.enabled)
                        .callback((cb, val) -> {
                            entry.enabled = val;
                            config.save();
                        })
                        .build();
                addDrawableChild(toggle);
                x += 80 + spacing;  // 20px ширина + 20px отступ

                // 2) Поле «Название» (TextFieldWidget шириной 100px)
                TextFieldWidget nameField = new TextFieldWidget(
                        textRenderer, x, y, 100, 20,
                        Text.translatable("gui.simpleautoclicker.field.name")
                );
                nameField.setText(entry.name);
                nameField.setChangedListener(val -> {
                    entry.name = val;
                    config.save();
                });
                addDrawableChild(nameField);
                addSelectableChild(nameField);
                x += 90 + spacing;

                // 3) Интервал (TextFieldWidget шириной 40px)
                TextFieldWidget intervalField = new TextFieldWidget(
                        textRenderer, x, y, 40, 20,
                        Text.translatable("gui.simpleautoclicker.label.interval")
                );
                intervalField.setText(String.valueOf(entry.intervalTicks));
                intervalField.setChangedListener(val -> {
                    try {
                        entry.intervalTicks = Integer.parseInt(val);
                        config.save();
                    } catch (NumberFormatException ignored) {}
                });
                addDrawableChild(intervalField);
                addSelectableChild(intervalField);
                x += 30 + spacing;

                // 4) Таймер использования (TextFieldWidget шириной 40px)
                TextFieldWidget useField = new TextFieldWidget(
                        textRenderer, x, y, 40, 20,
                        Text.translatable("gui.simpleautoclicker.field.use_duration")
                );
                useField.setText(String.valueOf(entry.useDurationTicks));
                useField.setChangedListener(val -> {
                    try {
                        entry.useDurationTicks = Integer.parseInt(val);
                        config.save();
                    } catch (NumberFormatException ignored) {}
                });
                addDrawableChild(useField);
                addSelectableChild(useField);
                x += 30 + spacing;

                // 5) Кнопка «Удалить» (ButtonWidget шириной 50px)
                ButtonWidget deleteBtn = ButtonWidget.builder(
                                Text.translatable("gui.simpleautoclicker.button.delete"),
                                btn -> {
                                    config.entries.remove(entry);
                                    config.save();
                                    client.setScreen(new AutoClickerListScreen(parent));
                                }
                        )
                        .dimensions(x, y, 50, 20)
                        .build();
                addDrawableChild(deleteBtn);
                x += 40 + spacing;

                String playerName = "";
                // 6)–7) Доп. переключатели только для ATTACK
                if (entry.action == AutoClickAction.ATTACK) {
                    if (client != null && client.player != null) {
                        playerName = client.player.getName().getString(); // Получаем имя как строку
                    }

                    if ("Sansrus".equals(playerName)) {
                        var onlyEntToggle = CheckboxWidget.builder(Text.translatable("gui.simpleautoclicker.toggle.only_entity"), textRenderer)
                                .pos(x, y)
                                .checked(entry.onlyEntityMode)
                                .callback((cb, val) -> {
                                    entry.onlyEntityMode = val;
                                    config.save();
                                })
                                .build();
                        addDrawableChild(onlyEntToggle);
                        x += 60 + spacing;

                        // cooldownMode (Checkbox шириной 20px)
                        var cdToggle = CheckboxWidget.builder(Text.translatable("gui.simpleautoclicker.toggle.cooldown_mode"), textRenderer)
                                .pos(x, y)
                                .checked(entry.cooldownMode)
                                .callback((cb, val) -> {
                                    entry.cooldownMode = val;
                                    config.save();
                                })
                                .build();
                        addDrawableChild(cdToggle);
                        // x += 20 + spacing; // если не нужны дальше элементы
                    }
                }
            }
            y += 25;
        }

        // Кнопка «+»
        addDrawableChild(ButtonWidget.builder(
                                Text.literal("+"),
                                btn -> {
                                    assert client != null;
                                    client.setScreen(new AddKeyScreen(this));
                                }
                        )
                        .dimensions(this.width - 30, 10, 20, 20)
                        .build()
        );

        // Кнопка «Отмена»
        addDrawableChild(ButtonWidget.builder(
                                Text.translatable("gui.simpleautoclicker.button.cancel"),
                                btn -> {
                                    assert client != null;
                                    client.setScreen(parent);
                                }
                        )
                        .dimensions(10, this.height - 30, 60, 20)
                        .build()
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (config.entries.size() * 25 > this.height - 120) {
            int maxScroll = Math.max(0, config.entries.size() * 25 - (this.height - 120));
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(verticalAmount * 10)));
            clearAndInit();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.simpleautoclicker.title"), this.width / 2, 10, 0xFFFFFFFF);

        // Имя автокликов
        context.drawText(
                this.textRenderer,
                Text.translatable("gui.simpleautoclicker.field.name"),
                110,             // X‑координата поля
                35,       // Y‑координата над полем
                0xFFFFFFFF,          // цвет текста (белый)
                false              // без обрезки по границе
        );

        // Таймер ожидания
        context.drawText(
                this.textRenderer,
                Text.translatable("gui.simpleautoclicker.label.interval"),
                220,             // X‑координата поля
                35,       // Y‑координата над полем
                0xFFFFFFFF,          // цвет текста (белый)
                false              // без обрезки по границе
        );

        // Таймер использования
        context.drawText(
                this.textRenderer,
                Text.translatable("gui.simpleautoclicker.field.use_duration"),
                270,             // X‑координата поля
                35,       // Y‑координата над полем
                0xFFFFFFFF,          // цвет текста (белый)
                false              // без обрезки по границе
        );

        String playerName = "";
        if (client != null && client.player != null) {
            playerName = client.player.getName().getString(); // Получаем имя как строку
        }

        if ("Sansrus".equals(playerName)) {
            context.drawText(
                    this.textRenderer,
                    Text.translatable("gui.simpleautoclicker.field.features"),
                    385,             // X‑координата поля
                    35,       // Y‑координата над полем
                    0xFFFFFFFF,          // цвет текста (белый)
                    false              // без обрезки по границе
            );
        }

        // Рисуем индикатор прокрутки если нужно
        if (config.entries.size() * 25 > this.height - 120) {
            int maxScroll = Math.max(0, config.entries.size() * 25 - (this.height - 120));
            if (maxScroll > 0) {
                int scrollBarHeight = Math.max(10, (this.height - 120) * (this.height - 120) / (config.entries.size() * 25));
                int scrollBarY = 50 + (int)((float)scrollOffset / maxScroll * (this.height - 120 - scrollBarHeight));
                context.fill(this.width - 15, 50, this.width - 10, this.height - 70, 0x80FFFFFF);
                context.fill(this.width - 15, scrollBarY, this.width - 10, scrollBarY + scrollBarHeight, 0xFFFFFFFF);
            }
        }
    }
}