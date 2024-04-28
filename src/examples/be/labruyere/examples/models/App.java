package be.labruyere.examples.models;

import be.labruyere.arqanite.ArqInput;
import be.labruyere.arqanite.ArqLogger;
import be.labruyere.arqanite.ArqMusic;
import be.labruyere.arqanite.ArqScreens;
import be.labruyere.arqanore.Window;
import be.labruyere.arqanore.exceptions.ArqanoreException;

public class App {
    private static Window window;

    private static void onOpen() {
        ArqLogger.init(false, true);

        try {
            Assets.load();

            ArqScreens.addScreen("Main", new ScreenMain());
            ArqScreens.navigate("Main", null);
        } catch (Exception e) {
            ArqLogger.logError(e);
            window.close();
        }
    }

    private static void onClose() {
        Assets.destroy();
    }

    private static void onTick(double dt) {
        try {
            ArqScreens.tick(window, dt);
        } catch (Exception e) {
            ArqLogger.logError(e);
            window.close();
        }
    }

    private static void onUpdate(double at) {
        try {
            ArqScreens.update(window, at);
            ArqMusic.update();
            ArqInput.update();
        } catch (Exception e) {
            ArqLogger.logError(e);
            window.close();
        }
    }

    private static void onRender2D() {
        try {
            ArqScreens.render2D(window);
        } catch (Exception e) {
            ArqLogger.logError(e);
            window.close();
        }
    }

    private static void onRender3D() {
        try {
            ArqScreens.render3D(window);
        } catch (Exception e) {
            ArqLogger.logError(e);
            window.close();
        }
    }

    private static void onChar(int codePoint) {
        try {
            ArqScreens.input(window, (char) codePoint);
        } catch (Exception e) {
            ArqLogger.logError(e);
            window.close();
        }
    }

    public static void main(String[] args) {
        var fqn = "be/labruyere/examples/models/App";

        try {
            window = new Window(1440, 786, "Window");
            window.onOpen(fqn, "onOpen");
            window.onClose(fqn, "onClose");
            window.onTick(fqn, "onTick");
            window.onUpdate(fqn, "onUpdate");
            window.onRender2D(fqn, "onRender2D");
            window.onRender3D(fqn, "onRender3D");
            window.onChar(fqn, "onChar");
            window.open(false, true);
        } catch (ArqanoreException e) {
            e.printStackTrace();
        } finally {
            window.delete();
        }
    }
}
