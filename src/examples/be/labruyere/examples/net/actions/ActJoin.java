package be.labruyere.examples.net.actions;

import be.labruyere.arqanite.ArqLogger;
import be.labruyere.arqanite.net.ArqAction;
import be.labruyere.arqanite.net.ArqAsyncClient;
import be.labruyere.arqanore.exceptions.ArqanoreException;

public class ActJoin extends ArqAction {
    public ActJoin() {
        super("join");
    }

    @Override
    public void runAsync(String body) throws ArqanoreException {
        ArqLogger.logInfo("[CLIENT] ping");
        ArqAsyncClient.send("ping", "");
    }

    @Override
    public void runAsync(int clientId, String body) throws ArqanoreException {
        ArqLogger.logInfo("[SERVER] Client " + clientId + " joined");
    }
}
