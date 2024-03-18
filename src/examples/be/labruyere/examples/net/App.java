package be.labruyere.examples.net;

import be.labruyere.arqanite.net.ArqActions;
import be.labruyere.arqanite.net.ArqClient;
import be.labruyere.arqanite.net.ArqServer;
import be.labruyere.arqanore.exceptions.ArqanoreException;
import be.labruyere.examples.net.actions.ActPing;

public class App {
    public static void main(String[] args) {
        ArqActions.add(new ActPing());

        try {
            ArqServer.start(9090);
            ArqClient.setHost("localhost");
            ArqClient.setPort(9090);

            var res = ArqClient.send("ping", "");
            System.out.println(res);
        } catch (ArqanoreException e) {
            e.printStackTrace();
        }
    }
}
