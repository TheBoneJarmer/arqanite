package be.labruyere.arqanite.interfaces;

import be.labruyere.arqanore.Window;
import be.labruyere.arqanore.exceptions.ArqanoreException;

public interface IScreen {
    void init(Window window, Object[] args) throws ArqanoreException;
    void tick(Window window, double dt) throws ArqanoreException;
    void update(Window window, double at) throws ArqanoreException;
    void render2D(Window window) throws ArqanoreException;
    void render3D(Window window) throws ArqanoreException;
    void input(Window window, char c) throws ArqanoreException;
}
