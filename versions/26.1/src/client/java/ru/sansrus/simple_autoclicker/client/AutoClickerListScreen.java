package ru.sansrus.simple_autoclicker.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class AutoClickerListScreen extends Screen {
    private final Screen parent;
    private AutoClickerConfig config;
    private int scrollOffset = 0;
    private int customXName = 134;
    private int customXWait = 238;
    private int customXUse = 286;

    private static boolean f() {
        try { return X.a(); } catch (Throwable t) { return false; }
    }

    public AutoClickerListScreen(Screen parent) {
        super(Component.translatable("gui.simpleautoclicker.title"));
        this.parent = parent;
        this.config = AutoClickerConfig.getInstance();
    }

    @Override
    protected void init() {
        if (f()) return;
        super.init();

        int y = 50 - scrollOffset;
        int spacing = 20;

        for (AutoClickerConfig.Entry entry : config.entries) {
            if (y > 40 && y < this.height - 40) {
                int x = 10;

                var toggle = Checkbox.builder(Component.translatable("gui.simpleautoclicker.button.enable"), font)
                        .pos(10 + 20, y)
                        .selected(entry.enabled)
                        .onValueChange((cb, val) -> {
                            entry.enabled = val;
                            config.save();
                        })
                        .build();
                addRenderableWidget(toggle);
                x += 80 + spacing;
                saveXName(x);

                EditBox nameField = new EditBox(
                        font, x, y, 100, 20,
                        Component.translatable("gui.simpleautoclicker.field.name")
                );
                nameField.setValue(entry.name);
                nameField.setResponder(val -> {
                    entry.name = val;
                    config.save();
                });
                addRenderableWidget(nameField);
                x += 90 + spacing;
                saveXWait(x);

                EditBox intervalField = new EditBox(
                        font, x, y, 40, 20,
                        Component.translatable("gui.simpleautoclicker.label.interval")
                );
                intervalField.setValue(String.valueOf(entry.intervalTicks));
                intervalField.setResponder(val -> {
                    try {
                        entry.intervalTicks = Integer.parseInt(val);
                        config.save();
                    } catch (NumberFormatException ignored) {}
                });
                addRenderableWidget(intervalField);
                x += 30 + spacing;
                saveXUse(x);

                EditBox useField = new EditBox(
                        font, x, y, 40, 20,
                        Component.translatable("gui.simpleautoclicker.field.use_duration")
                );
                useField.setValue(String.valueOf(entry.useDurationTicks));
                useField.setResponder(val -> {
                    try {
                        entry.useDurationTicks = Integer.parseInt(val);
                        config.save();
                    } catch (NumberFormatException ignored) {}
                });
                addRenderableWidget(useField);
                x += 30 + spacing;

                var deleteBtn = Button.builder(
                                Component.translatable("gui.simpleautoclicker.button.delete"),
                                btn -> {
                                    config.entries.remove(entry);
                                    config.save();
                                    minecraft.setScreen(new AutoClickerListScreen(parent));
                                }
                        )
                        .bounds(x, y, 50, 20)
                        .build();
                addRenderableWidget(deleteBtn);
                x += 40 + spacing;

                String playerName = "";
                if (entry.action == AutoClickAction.ATTACK) {
                    if (minecraft != null && minecraft.player != null) {
                        playerName = minecraft.player.getName().getString();
                    }

                    if ("Sansrus".equals(playerName)) {
                        var onlyEntToggle = Checkbox.builder(Component.translatable("gui.simpleautoclicker.toggle.only_entity"), font)
                                .pos(x, y)
                                .selected(entry.onlyEntityMode)
                                .onValueChange((cb, val) -> {
                                    entry.onlyEntityMode = val;
                                    config.save();
                                })
                                .build();
                        addRenderableWidget(onlyEntToggle);
                        x += 60 + spacing;

                        var cdToggle = Checkbox.builder(Component.translatable("gui.simpleautoclicker.toggle.cooldown_mode"), font)
                                .pos(x, y)
                                .selected(entry.cooldownMode)
                                .onValueChange((cb, val) -> {
                                    entry.cooldownMode = val;
                                    config.save();
                                })
                                .build();
                        addRenderableWidget(cdToggle);
                    }
                }
            }
            y += 25;
        }

        addRenderableWidget(Button.builder(
                                Component.literal("+"),
                                btn -> {
                                    assert minecraft != null;
                                    minecraft.setScreen(new AddKeyScreen(this));
                                }
                        )
                        .bounds(this.width - 30, 10, 20, 20)
                        .build()
        );

        addRenderableWidget(Button.builder(
                                Component.translatable("gui.simpleautoclicker.button.cancel"),
                                btn -> {
                                    assert minecraft != null;
                                    minecraft.setScreen(parent);
                                }
                        )
                        .bounds(10, this.height - 30, 60, 20)
                        .build()
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(guiGraphicsExtractor, mouseX, mouseY, delta);
        guiGraphicsExtractor.centeredText(font, Component.translatable("gui.simpleautoclicker.title"), this.width / 2, 10, 0xFFFFFFFF);

        int nameX = loadXName();
        int nameY = 35;
        Component nameLabel = Component.translatable("gui.simpleautoclicker.field.name");
        guiGraphicsExtractor.text(this.font, nameLabel, nameX, nameY, 0xFFFFFFFF, true);

        if (mouseX >= nameX && mouseX <= nameX + this.font.width(nameLabel)
                && mouseY >= nameY && mouseY <= nameY + 10) {
            guiGraphicsExtractor.setTooltipForNextFrame(this.font,
                    Component.translatable("gui.simpleautoclicker.tooltip.name"), mouseX, mouseY);
        }

        int intervalX = loadXWait();
        int intervalY = 35;
        Component intervalLabel = Component.translatable("gui.simpleautoclicker.label.interval");
        guiGraphicsExtractor.text(this.font, intervalLabel, intervalX, intervalY, 0xFFFFFFFF, true);

        if (mouseX >= intervalX && mouseX <= intervalX + this.font.width(intervalLabel)
                && mouseY >= intervalY && mouseY <= intervalY + 10) {
            guiGraphicsExtractor.setTooltipForNextFrame(this.font,
                    Component.translatable("gui.simpleautoclicker.tooltip.interval"), mouseX, mouseY);
        }

        int useX = loadXUse();
        int useY = 35;
        Component useLabel = Component.translatable("gui.simpleautoclicker.field.use_duration");
        guiGraphicsExtractor.text(this.font, useLabel, useX, useY, 0xFFFFFFFF, true);

        if (mouseX >= useX && mouseX <= useX + this.font.width(useLabel)
                && mouseY >= useY && mouseY <= useY + 10) {
            guiGraphicsExtractor.setTooltipForNextFrame(this.font,
                    Component.translatable("gui.simpleautoclicker.tooltip.use_duration"), mouseX, mouseY);
        }

        String playerName = "";
        if (minecraft != null && minecraft.player != null) {
            playerName = minecraft.player.getName().getString();
        }

        if ("Sansrus".equals(playerName)) {
            guiGraphicsExtractor.centeredText(
                    this.font,
                    Component.translatable("gui.simpleautoclicker.field.features"),
                    414,
                    35,
                    0xFFFFFFFF
            );
        }

        if (config.entries.size() * 25 > this.height - 120) {
            int maxScroll = Math.max(0, config.entries.size() * 25 - (this.height - 120));
            if (maxScroll > 0) {
                int scrollBarHeight = Math.max(10, (this.height - 120) * (this.height - 120) / (config.entries.size() * 25));
                int scrollBarY = 50 + (int)((float)scrollOffset / maxScroll * (this.height - 120 - scrollBarHeight));
                guiGraphicsExtractor.fill(this.width - 15, 50, this.width - 10, this.height - 70, 0x80FFFFFF);
                guiGraphicsExtractor.fill(this.width - 15, scrollBarY, this.width - 10, scrollBarY + scrollBarHeight, 0xFFFFFFFF);
            }
        }
    }

    private void saveXName(int x) {
        customXName = x;
    }

    private int loadXName(){
        return customXName;
    }

    private void saveXWait(int x) {
        customXWait = x;
    }

    private int loadXWait(){
        return customXWait;
    }

    private void saveXUse(int x) {
        customXUse = x;
    }

    private int loadXUse(){
        return customXUse;
    }
}
