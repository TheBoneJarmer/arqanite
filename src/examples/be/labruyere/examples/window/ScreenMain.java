package be.labruyere.examples.window;

import be.labruyere.arqanite.ArqInput;
import be.labruyere.arqanite.interfaces.IScreen;
import be.labruyere.arqanore.Color;
import be.labruyere.arqanore.Window;
import be.labruyere.arqanore.enums.Keys;
import be.labruyere.arqanore.exceptions.ArqanoreException;

public class ScreenMain implements IScreen {

    @Override
    public void init(Window window, Object[] args) {
        var color = new Color(0, 175, 255);

        window.setClearColor(color);
        ArqInput.mapKey("Cancel", Keys.ESCAPE);
    }

    @Override
    public void tick(Window window, double dt) {

    }

    @Override
    public void update(Window window, double dt) {
        if (ArqInput.getState("Cancel", true)) {
            window.close();
        }
    }

    @Override
    public void render2D(Window window) {

    }

    @Override
    public void render3D(Window window) throws ArqanoreException {

    }

    @Override
    public void input(Window window, char c) {

    }
}
