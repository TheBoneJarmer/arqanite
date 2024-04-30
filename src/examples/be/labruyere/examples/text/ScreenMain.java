package be.labruyere.examples.text;

import be.labruyere.arqanite.ArqInput;
import be.labruyere.arqanite.interfaces.IScreen;
import be.labruyere.arqanore.Color;
import be.labruyere.arqanore.Renderer;
import be.labruyere.arqanore.Window;
import be.labruyere.arqanore.exceptions.ArqanoreException;
import be.labruyere.arqanore.enums.Keys;

import java.util.Random;

public class ScreenMain implements IScreen {
    private String text;

    @Override
    public void init(Window window, Object[] args) {
        initWindow(window);
        initInput();
        initText();
    }

    private void initInput() {
        ArqInput.mapKey("Cancel", Keys.ESCAPE);
        ArqInput.mapKey("Refresh", Keys.SPACE);
    }

    private void initWindow(Window window) {
        window.setClearColor(0, 175, 255);
    }

    private void initText() {
        var random = new Random();
        var sb = new StringBuilder();
        var lorem = "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?";
        var words = lorem.split(" ");

        for (var i = 0; i < words.length; i++) {
            var word = words[i];

            if (random.nextBoolean()) {
                sb.append("!rgba(0,255,0,255)");
            }

            sb.append(word);

            if (i < words.length - 1) {
                sb.append(" ");
            }
        }

        text = sb.toString();
    }

    @Override
    public void tick(Window window, double dt) {

    }

    @Override
    public void update(Window window, double at) {
        if (ArqInput.getState("Cancel", true)) {
            window.close();
        }

        if (ArqInput.getState("Refresh", true)) {
            initText();
        }
    }

    @Override
    public void render2D(Window window) {
        var font = Assets.font;

        try {
            Renderer.renderParagraph(window, font, text, 32, 32, 1, 1, 8, window.getWidth() - 64, 255, 255, 255, 255);
        } catch (ArqanoreException ex) {
            ex.printStackTrace();
            window.close();
        }
    }

    @Override
    public void render3D(Window window) throws ArqanoreException {

    }

    @Override
    public void input(Window window, char c) {

    }
}
