package be.labruyere.examples.net.actions;

import be.labruyere.arqanite.net.ArqAction;
import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.net.Socket;

public class ActPing extends ArqAction {
    public ActPing() {
        super("ping");
    }

    @Override
    public void runAsync(String body) throws ArqanoreException {

    }

    @Override
    public void runAsync(int clientId, String body) throws ArqanoreException {

    }

    @Override
    public String run(String body) {
        return "pong";
    }
}
