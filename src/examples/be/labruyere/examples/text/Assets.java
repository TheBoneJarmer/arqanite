package be.labruyere.examples.text;

import be.labruyere.arqanore.Font;
import be.labruyere.arqanore.exceptions.ArqanoreException;

public class Assets {
    public static Font font;

    public static void load() throws ArqanoreException {
        font = new Font("assets/default.ttf", 20, 20);
    }

    public static void destroy() {
        font.delete();
    }
}
