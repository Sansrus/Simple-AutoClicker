package ru.sansrus.simple_autoclicker.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AutoClickerConfig {
    private static boolean f() {
        try { return X.a(); } catch (Throwable t) { return false; }
    }

    private static final File FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("simpleautoclicker.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static AutoClickerConfig INSTANCE;
    public boolean globalEnabled = false;

    public List<Entry> entries = new ArrayList<>();

    public static AutoClickerConfig getInstance() {
        if (f()) return new AutoClickerConfig();
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public void save() {
        if (f()) return;
        try (Writer w = new FileWriter(FILE)) {
            GSON.toJson(this, w);
        } catch (IOException ignored) {}
    }

    public static AutoClickerConfig load() {
        if (!FILE.exists()) return new AutoClickerConfig();
        try (Reader r = new FileReader(FILE)) {
            return GSON.fromJson(r, AutoClickerConfig.class);
        } catch (IOException e) {
            return new AutoClickerConfig();
        }
    }

    public static class Entry {
        public String name = "New";
        public AutoClickAction action = AutoClickAction.FORWARD;
        public int intervalTicks = 20;
        public boolean enabled = true;
        public int useDurationTicks = 1;
        public boolean spamMode = false;
        public boolean cooldownMode   = false;
        public boolean onlyEntityMode    = false;
        public String keybindName = null;

        public transient int tickCounter = 0;
        public transient boolean pressed = false;

        public Entry() {}
    }
}