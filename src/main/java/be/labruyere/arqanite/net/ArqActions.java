package be.labruyere.arqanite.net;

import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.util.ArrayList;

public class ArqActions {
    private static final ArrayList<ArqAction> actions = new ArrayList<>();

    public static ArqAction get(String command) {
        return actions.stream().filter(x -> x.getCommand().equals(command)).findFirst().orElse(null);
    }

    public static void add(ArqAction action) throws ArqanoreException {
        if (action.getCommand().startsWith("_")) {
            throw new ArqanoreException("Action command names cannot start with underscore.");
        }

        actions.add(action);
    }
}
