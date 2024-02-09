package be.labruyere.examples.models;

import be.labruyere.arqanore.Font;
import be.labruyere.arqanore.Model;
import be.labruyere.arqanore.exceptions.ArqanoreException;

public class Assets {
    public static Font font;
    public static Model model;

    public static void load() throws ArqanoreException {
        font = new Font("assets/default.ttf", 20, 20);
        model = new Model("assets/axis.arqm");
    }

    public static void destroy() {
        font.delete();
        model.delete();
    }
}
