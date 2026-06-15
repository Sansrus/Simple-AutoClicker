package ru.sansrus.simple_autoclicker.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeybindPickerScreen extends Screen {
    private static boolean f() {
        try { return X.a(); } catch (Throwable t) { return false; }
    }

    private final Screen parent;
    private EditBox searchBox;
    private String filter = "";
    private int scrollOffset = 0;
    private List<KeyMapping> allMappings = new ArrayList<>();
    private List<CategoryGroup> groups = new ArrayList<>();
    private List<Button> listButtons = new ArrayList<>();
    private int totalContentHeight = 0;

    private static final int HEADER_HEIGHT = 15;
    private static final int ITEM_HEIGHT = 20;
    private static final int ROW_STEP = 25;
    private static final int VISIBLE_TOP = 50;

    private record KeyMappingEntry(KeyMapping mapping, Component categoryLabel) {}

    private static class CategoryGroup {
        final Component label;
        final List<KeyMappingEntry> entries;
        CategoryGroup(Component label, List<KeyMappingEntry> entries) {
            this.label = label;
            this.entries = entries;
        }
    }

    public KeybindPickerScreen(Screen parent) {
        super(Component.translatable("gui.simpleautoclicker.choose_keybind"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (f()) return;
        super.init();
        allMappings = collectKeyMappings();

        if (searchBox == null) {
            searchBox = new EditBox(font, this.width / 2 - 100, 20, 200, 20,
                    Component.translatable("gui.simpleautoclicker.search"));
            searchBox.setResponder(val -> {
                filter = val.toLowerCase();
                rebuildGroups();
                scrollOffset = 0;
                rebuildList();
            });
            addRenderableWidget(searchBox);
        }

        rebuildGroups();
        rebuildList();
    }

    private void rebuildGroups() {
        groups.clear();

        List<KeyMappingEntry> entries = new ArrayList<>();
        for (KeyMapping km : allMappings) {
            entries.add(new KeyMappingEntry(km, km.getCategory().label()));
        }

        entries.sort(Comparator.comparing((KeyMappingEntry e) -> e.categoryLabel().getString())
                .thenComparing(e -> Component.translatable(e.mapping().getName()).getString()));

        if (!filter.isEmpty()) {
            Map<String, List<KeyMappingEntry>> catGroups = new LinkedHashMap<>();
            for (KeyMappingEntry e : entries) {
                catGroups.computeIfAbsent(e.categoryLabel().getString(), k -> new ArrayList<>()).add(e);
            }

            List<String> matchingCategories = new ArrayList<>();
            for (Map.Entry<String, List<KeyMappingEntry>> entry : catGroups.entrySet()) {
                String catName = entry.getKey();
                boolean catMatch = catName.toLowerCase().contains(filter);

                if (catMatch) {
                    matchingCategories.add(entry.getKey());
                } else {
                    for (KeyMappingEntry em : entry.getValue()) {
                        String actionName = Component.translatable(em.mapping().getName()).getString().toLowerCase();
                        if (actionName.contains(filter)) {
                            matchingCategories.add(entry.getKey());
                            break;
                        }
                    }
                }
            }

            List<KeyMappingEntry> filtered = new ArrayList<>();
            for (Map.Entry<String, List<KeyMappingEntry>> entry : catGroups.entrySet()) {
                if (!matchingCategories.contains(entry.getKey())) continue;
                boolean catMatch = entry.getKey().toLowerCase().contains(filter);
                if (catMatch) {
                    filtered.addAll(entry.getValue());
                } else {
                    for (KeyMappingEntry em : entry.getValue()) {
                        String actionName = Component.translatable(em.mapping().getName()).getString().toLowerCase();
                        if (actionName.contains(filter)) {
                            filtered.add(em);
                        }
                    }
                }
            }
            entries = filtered;
        }

        Map<String, List<KeyMappingEntry>> grouped = new LinkedHashMap<>();
        for (KeyMappingEntry e : entries) {
            grouped.computeIfAbsent(e.categoryLabel().getString(), k -> new ArrayList<>()).add(e);
        }

        for (List<KeyMappingEntry> list : grouped.values()) {
            groups.add(new CategoryGroup(list.get(0).categoryLabel(), list));
        }
    }

    private void rebuildList() {
        for (Button btn : listButtons) {
            removeWidget(btn);
        }
        listButtons.clear();

        if (groups.isEmpty()) return;

        final int leftX = 20;
        final int btnWidth = 180;
        int y = VISIBLE_TOP;
        int visibleBottom = this.height - 40;

        for (CategoryGroup group : groups) {
            y += HEADER_HEIGHT;

            for (KeyMappingEntry entry : group.entries) {
                int buttonY = y - scrollOffset;

                if (buttonY + ITEM_HEIGHT > VISIBLE_TOP && buttonY < visibleBottom) {
                    Component label = Component.translatable(entry.mapping().getName());
                    Button btn = Button.builder(label, b -> {
                        AutoClickerConfig.Entry e = new AutoClickerConfig.Entry();
                        e.action = AutoClickAction.CUSTOM_KEYBIND;
                        e.keybindName = entry.mapping().getName();
                        e.name = Component.translatable(entry.mapping().getName()).getString();
                        AutoClickerConfig.getInstance().entries.add(e);
                        AutoClickerConfig.getInstance().save();
                        assert minecraft != null;
                        minecraft.setScreen(new AutoClickerListScreen(parent));
                    }).bounds(leftX, buttonY, btnWidth, ITEM_HEIGHT).build();
                    addRenderableWidget(btn);
                    listButtons.add(btn);
                }

                y += ROW_STEP;
            }
        }

        totalContentHeight = y - VISIBLE_TOP;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int visibleBottom = this.height - 40;
        int visibleHeight = Math.max(0, visibleBottom - VISIBLE_TOP);

        if (totalContentHeight > visibleHeight) {
            int maxScroll = Math.max(0, totalContentHeight - visibleHeight);
            int delta = (int) (verticalAmount * ITEM_HEIGHT);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - delta));
            rebuildList();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gge, int mouseX, int mouseY, float delta) {
        super.extractRenderState(gge, mouseX, mouseY, delta);

        gge.centeredText(font, this.title, this.width / 2, 5, 0xFFFFFFFF);

        if (groups.isEmpty()) {
            gge.centeredText(font,
                    Component.translatable("gui.simpleautoclicker.no_results"),
                    this.width / 2, this.height / 2, 0xFFFFFFFF);
            return;
        }

        int visibleBottom = this.height - 40;
        int y = VISIBLE_TOP;

        for (CategoryGroup group : groups) {
            int headerY = y - scrollOffset;

            if (headerY + HEADER_HEIGHT > VISIBLE_TOP && headerY < visibleBottom) {
                gge.text(font, group.label, 22, headerY + 3, 0xFFFFFFFF, true);
            }

            y += HEADER_HEIGHT;

            for (KeyMappingEntry ignored : group.entries) {
                y += ROW_STEP;
            }
        }

        int visibleHeight = Math.max(0, visibleBottom - VISIBLE_TOP);
        if (totalContentHeight > visibleHeight) {
            int maxScroll = Math.max(0, totalContentHeight - visibleHeight);
            if (maxScroll > 0) {
                int scrollBarHeight = Math.max(10, visibleHeight * visibleHeight / totalContentHeight);
                int scrollBarY = VISIBLE_TOP + (int) ((float) scrollOffset / maxScroll * (visibleHeight - scrollBarHeight));
                gge.fill(this.width - 15, VISIBLE_TOP, this.width - 10, visibleBottom, 0x80FFFFFF);
                gge.fill(this.width - 15, scrollBarY, this.width - 10, scrollBarY + scrollBarHeight, 0xFFFFFFFF);
            }
        }

        gge.centeredText(font,
                Component.translatable("gui.simpleautoclicker.warning.not_all"),
                this.width / 2, this.height - 15, 0xFF808080);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private List<KeyMapping> collectKeyMappings() {
        List<KeyMapping> result = new ArrayList<>();
        try {
            Field f = Options.class.getField("keyMappings");
            Minecraft client = Minecraft.getInstance();
            KeyMapping[] mappings = (KeyMapping[]) f.get(client.options);
            for (KeyMapping km : mappings) {
                result.add(km);
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
