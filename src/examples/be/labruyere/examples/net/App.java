package be.labruyere.examples.net;

import be.labruyere.arqanite.ArqLogger;
import be.labruyere.arqanite.net.*;
import be.labruyere.arqanore.exceptions.ArqanoreException;
import be.labruyere.examples.net.actions.ActJoin;
import be.labruyere.examples.net.actions.ActLeave;
import be.labruyere.examples.net.actions.ActPing;

public class App {
    public static void main(String[] args) {
        ArqActions.add(new ActJoin());
        ArqActions.add(new ActLeave());
        ArqActions.add(new ActPing());
        ArqLogger.init(false, true);

        try {
            //run();
            runAsync();
        } catch (Exception e) {
            ArqLogger.logError(e);
        }
    }

    private static void run() throws ArqanoreException {
        ArqServer.setAcceptTimeout(0);
        ArqServer.start(9091);

        ArqClient.setHost("localhost");
        ArqClient.setPort(9091);

        var res = ArqClient.send("ping", "");
        ArqLogger.logInfo("[CLIENT1] " + res);

        ArqServer.stop();
    }

    private static void runAsync() throws ArqanoreException, InterruptedException {
        ArqAsyncServer.setAcceptTimeout(0);
        ArqAsyncServer.setClientTimeout(0);
        ArqAsyncServer.start(9090);

        while (true) {
            if (ArqAsyncClient.isConnected()) {
                continue;
            }

            ArqAsyncClient.setTimeout(0);
            ArqAsyncClient.connect("localhost", 9090);

            Thread.sleep(100);
        }
    }
}
