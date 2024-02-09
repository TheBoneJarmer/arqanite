package be.labruyere.examples.window;

import be.labruyere.arqanite.*;
import be.labruyere.arqanite.enums.ArqLogLevel;
import be.labruyere.arqanore.*;
import be.labruyere.arqanore.exceptions.ArqanoreException;

public class App {
    private static Window window;

    private static void onOpen() {
        ArqLogger.init(ArqLogLevel.DEBUG, false, true);
        ArqScreens.addScreen("Main", new ScreenMain());
        ArqScreens.navigate("Main", null);
    }

    private static void onClose() {

    }

    private static void onTick(double dt) {
        try {
            ArqScreens.tick(window, dt);
        } catch (ArqanoreException e) {
            ArqLogger.logError(e);
            window.close();
        }
    }

    private static void onUpdate(double at) {
        try {
            ArqScreens.update(window, at);
            ArqMusic.update();
            ArqInput.update();
        } catch (ArqanoreException e) {
            ArqLogger.logError(e);
            window.close();
        }
    }

    private static void onRender2D() {
        try {
            ArqScreens.render2D(window);
        } catch (ArqanoreException e) {
            ArqLogger.logError(e);
            window.close();
        }
    }

    private static void onChar(int codePoint) {
        try {
            ArqScreens.input(window, (char)codePoint);
        } catch (ArqanoreException e) {
            ArqLogger.logError(e);
            window.close();
        }
    }

    public static void main(String[] args) {
        var fqn = "be/labruyere/examples/window/App";

        try {
            window = new Window(1440, 786, "Window");
            window.onOpen(fqn, "onOpen");
            window.onClose(fqn, "onClose");
            window.onTick(fqn, "onTick");
            window.onUpdate(fqn, "onUpdate");
            window.onRender2D(fqn, "onRender2D");
            window.onChar(fqn, "onChar");
            window.open(false, true);
        } catch (ArqanoreException e) {
            e.printStackTrace();
        } finally {
            window.delete();
        }
    }
}
