package be.labruyere.examples.net.actions;

import be.labruyere.arqanite.ArqLogger;
import be.labruyere.arqanite.net.ArqAction;
import be.labruyere.arqanite.net.ArqAsyncClient;
import be.labruyere.arqanite.net.ArqAsyncServer;
import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.net.Socket;

public class ActPing extends ArqAction {
    public ActPing() {
        super("ping");
    }

    @Override
    public void runAsync(String body) throws ArqanoreException {
        ArqLogger.logInfo("[CLIENT] " + body);
        ArqAsyncClient.disconnect();
    }

    @Override
    public void runAsync(int clientId, String body) throws ArqanoreException {
        var client = ArqAsyncServer.getClient(clientId);

        if (client == null) {
            return;
        }

        client.send("ping", "pong");
    }

    @Override
    public String run(String body) {
        return "pong";
    }
}
