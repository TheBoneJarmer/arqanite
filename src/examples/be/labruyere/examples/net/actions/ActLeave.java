package be.labruyere.examples.net.actions;

import be.labruyere.arqanite.ArqLogger;
import be.labruyere.arqanite.net.ArqAction;
import be.labruyere.arqanite.net.ArqAsyncServer;
import be.labruyere.arqanore.exceptions.ArqanoreException;

public class ActLeave extends ArqAction {
    public ActLeave() {
        super("leave");
    }

    @Override
    public void runAsync(String body) throws ArqanoreException {
        if (!body.isEmpty()) {
            ArqLogger.logError("[CLIENT] " + body);
        }
    }

    @Override
    public void runAsync(int clientId, String body) throws ArqanoreException {
        var client = ArqAsyncServer.getClient(clientId);
        var user = client.getUserData();

        ArqLogger.logInfo("[SERVER] Client " + clientId + " left");
    }
}
