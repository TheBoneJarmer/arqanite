package be.labruyere.arqanite.net;

import java.util.ArrayList;

public class ArqActions {
    private static final ArrayList<ArqAction> actions = new ArrayList<>();

    public static ArqAction get(String command) {
        return actions.stream().filter(x -> x.getCommand().equals(command)).findFirst().orElse(null);
    }

    public static void add(ArqAction action) {
        actions.add(action);
    }
}
