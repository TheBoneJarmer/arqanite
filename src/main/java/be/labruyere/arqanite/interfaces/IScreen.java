package be.labruyere.arqanite.interfaces;

import be.labruyere.arqanore.Window;

public interface IScreen {
    void init(Window window, Object[] args) throws Exception;
    void update(Window window, double dt) throws Exception;
    void render2D(Window window) throws Exception;
    void render3D(Window window) throws Exception;
    void input(Window window, char c) throws Exception;
}
