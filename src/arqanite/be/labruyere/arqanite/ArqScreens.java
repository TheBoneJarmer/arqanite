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

    public static String getCurrentScreenName() {
        if (current == null) {
            return null;
        }
        
        return current.screen;
    }
    
    public static String getNextScreenName() {
        if (next == null) {
            return null;
        }
        
        return next.screen;
    }

    public static <T extends IScreen> T getCurrentScreen() {
        if (current == null) {
            return null;
        }

        return (T)screens.get(current.screen);
    }

    public static <T extends IScreen> T getScreenNext() {
        if (next == null) {
            return null;
        }

        return (T)screens.get(next.screen);
    }

    public static void addScreen(String key, IScreen screen) throws ArqanoreException {
        if (screens.containsKey(key)) {
            throw new ArqanoreException("A screen with key " + key + " has already been added");
        }

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
    
    public static void navigate(String screen) throws ArqanoreException {
        navigate(screen, null);
    }
    
    public static void navigate(String screen, Object arg0) throws ArqanoreException {
        var args = new Object[1];
        args[0] = arg0;
        
        navigate(screen, args);
    }
    
    public static void navigate(String screen, Object arg0, Object arg1) throws ArqanoreException {
        var args = new Object[2];
        args[0] = arg0;
        args[1] = arg1;
        
        navigate(screen, args);
    }
    
    public static void navigate(String screen, Object arg0, Object arg1, Object arg2) throws ArqanoreException {
        var args = new Object[3];
        args[0] = arg0;
        args[1] = arg1;
        args[2] = arg2;
        
        navigate(screen, args);
    }
    
    public static void navigate(String screen, Object arg0, Object arg1, Object arg2, Object arg3) throws ArqanoreException {
        var args = new Object[4];
        args[0] = arg0;
        args[1] = arg1;
        args[2] = arg2;
        args[3] = arg3;
        
        navigate(screen, args);
    }
    
    public static void navigate(String screen, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) throws ArqanoreException {
        var args = new Object[5];
        args[0] = arg0;
        args[1] = arg1;
        args[2] = arg2;
        args[3] = arg3;
        args[4] = arg4;
        
        navigate(screen, args);
    }

    public static void navigate(String screen, Object[] args) throws ArqanoreException {
        if (!screens.containsKey(screen)) {
            throw new ArqanoreException("Screen " + screen + " not found");
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
