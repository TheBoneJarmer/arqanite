package be.labruyere.arqanite;

import be.labruyere.arqanite.interfaces.IScreen;
import be.labruyere.arqanore.Window;
import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.util.ArrayList;
import java.util.HashMap;

public class ArqScreens {
    static {
        screens = new HashMap<>();
        history = new ArrayList<>();
    }

    private static final HashMap<String, IScreen> screens;
    private static final ArrayList<ScreenEntry> history;
    private static ScreenEntry current;
    private static ScreenEntry next;

    public static String getCurrentScreen() {
        if (current == null) {
            return null;
        }

        return current.screen;
    }

    public static String getScreenNext() {
        if (next == null) {
            return null;
        }

        return next.screen;
    }

    public static void addScreen(String key, IScreen screen) {
        screens.put(key, screen);
    }

    public static void tick(Window window, double dt) throws ArqanoreException {
        if (current != null) {
            screens.get(current.screen).tick(window, dt);
        }
    }

    public static void update(Window window, double at) throws ArqanoreException {
        if (next != null) {
            history.add(current);
            current = next;

            screens.get(current.screen).init(window, current.args);
            next = null;
        }

        if (current != null) {
            screens.get(current.screen).update(window, at);
        }
    }

    public static void render2D(Window window) throws ArqanoreException {
        if (current != null) {
            screens.get(current.screen).render2D(window);
        }
    }

    public static void render3D(Window window) throws ArqanoreException {
        if (current != null) {
            screens.get(current.screen).render3D(window);
        }
    }

    public static void input(Window window, char c) throws ArqanoreException {
        if (current != null) {
            screens.get(current.screen).input(window, c);
        }
    }

    public static void navigate(String screen, Object[] args) {
        if (!screens.containsKey(screen)) {
            return;
        }

        next = new ScreenEntry();
        next.screen = screen;
        next.args = args;
    }

    public static void back() {
        if (history.isEmpty()) {
            return;
        }

        var index = history.size() - 1;
        current = history.get(index);

        history.remove(index);
    }

    private static class ScreenEntry {
        public String screen;
        public Object[] args;
    }
}
