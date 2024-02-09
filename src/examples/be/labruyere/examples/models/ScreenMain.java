package be.labruyere.examples.models;

import be.labruyere.arqanite.ArqInput;
import be.labruyere.arqanite.interfaces.IScreen;
import be.labruyere.arqanore.*;
import be.labruyere.arqanore.enums.Keys;
import be.labruyere.arqanore.exceptions.ArqanoreException;

public class ScreenMain implements IScreen {
    private Vector3 angles;

    @Override
    public void init(Window window, Object[] args) {
        window.setClearColor(0, 175, 255);
        ArqInput.mapKey("cancel", Keys.ESCAPE);
        ArqInput.mapKey("reset", Keys.R);
        ArqInput.mapKey("up", Keys.UP);
        ArqInput.mapKey("down", Keys.DOWN);
        ArqInput.mapKey("left", Keys.LEFT);
        ArqInput.mapKey("right", Keys.RIGHT);
        ArqInput.mapKey("lctrl", Keys.LEFTCTRL);
        ArqInput.mapKey("rctrl", Keys.RIGHTCTRL);

        initScene();
    }

    private void initScene() {
        angles = new Vector3();

        var cameras = Scene.getCameras();
        var camera = cameras[0];
        camera.setPosition(0, -10, -10);
        camera.setRotation(Quaternion.rotate(0, 0, 0, 1, 45, 0, 0));
    }

    @Override
    public void tick(Window window, double dt) {

    }

    @Override
    public void update(Window window, double dt) throws ArqanoreException {
        var rotateZ = ArqInput.getState("lctrl", false) || ArqInput.getState("rctrl", false);

        if (ArqInput.getState("cancel", true)) {
            window.close();
        }

        if (ArqInput.getState("reset", true)) {
            angles = new Vector3(0, 0, 0);
        }

        if (ArqInput.getState("left", false)) angles.y -= 4;
        if (ArqInput.getState("right", false)) angles.y += 4;

        if (ArqInput.getState("up", false) && rotateZ) angles.z -= 4;
        if (ArqInput.getState("down", false) && rotateZ) angles.z += 4;

        if (ArqInput.getState("up", false) && !rotateZ) angles.x -= 4;
        if (ArqInput.getState("down", false) && !rotateZ) angles.x += 4;
    }

    @Override
    public void render2D(Window window) throws ArqanoreException {
        Renderer.renderText(window, Assets.font, "Axis X: " + angles.x, 32, 32, 255, 255, 255, 255);
        Renderer.renderText(window, Assets.font, "Axis Y: " + angles.y, 32, 64, 255, 255, 255, 255);
        Renderer.renderText(window, Assets.font, "Axis Z: " + angles.z, 32, 96, 255, 255, 255, 255);
    }

    @Override
    public void render3D(Window window) throws ArqanoreException {
        var pos = Vector3.ZERO;
        var rot = Quaternion.rotate(0, 0, 0, 1, angles.x, angles.y, angles.z);
        var scl = Vector3.ONE;

        Renderer.renderModel(window, Assets.model, pos, rot, scl, 0);
    }

    @Override
    public void input(Window window, char c) {

    }
}
