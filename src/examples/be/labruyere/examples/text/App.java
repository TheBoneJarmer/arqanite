package be.labruyere.examples.text;

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
        } catch (ArqanoreException e) {
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
        var fqn = "be/labruyere/examples/text/App";

        try {
            window = new Window(1440, 786, "Example - Text");
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
