package be.labruyere.arqanite.net;

import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.net.Socket;

public abstract class ArqAction {
    private final String command;

    public String getCommand() {
        return command;
    }

    public ArqAction(String command) {
        this.command = command;
    }

    public void runAsync(int clientId, String body) throws Exception {

    }

    public void runAsync(String body) throws Exception {

    }

    public String run(String body) throws Exception {
        return null;
    }
}
