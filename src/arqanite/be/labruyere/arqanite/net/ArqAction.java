package be.labruyere.arqanite.net;

import be.labruyere.arqanore.exceptions.ArqanoreException;

public abstract class ArqAction {
    private final String command;

    public String getCommand() {
        return command;
    }

    public ArqAction(String command) {
        this.command = command;
    }

    public void run(int clientId, String body) throws ArqanoreException {

    }

    public void run(String body) throws ArqanoreException{

    }
}
