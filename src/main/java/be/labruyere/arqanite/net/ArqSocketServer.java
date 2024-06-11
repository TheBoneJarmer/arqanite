package be.labruyere.arqanite.net;

import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ArqSocketServer {
    private ServerThread thread;
    private int acceptTimeout;
    private int clientTimeout;

    public boolean isRunning() {
        return thread != null && thread.isConnected;
    }

    public int getAcceptTimeout() {
        return acceptTimeout;
    }

    public int getClientTimeout() {
        return clientTimeout;
    }

    public void setClientTimeout(int clientTimeout) {
        this.clientTimeout = clientTimeout;
    }

    public void setAcceptTimeout(int acceptTimeout) {
        this.acceptTimeout = acceptTimeout;
    }

    public void start(int port) throws ArqanoreException {
        if (thread != null && thread.isAlive()) {
            return;
        }

        thread = new ServerThread(port);
        thread.start();
    }

    public void stop() throws IOException {
        if (thread == null || !thread.isAlive()) {
            return;
        }

        thread.close();
    }

    /* EVENTS */
    protected String onAction(ArqClient client, String action, String body) {
        return null;
    }

    protected void onConnect(ArqClient client) {

    }

    protected void onClose(ArqClient client) {

    }

    protected void onError(String message, Exception e) {

    }

    /* THREADS */
    private class ServerThread extends Thread {
        public ServerSocket listener;
        public boolean isConnected;
        public boolean isDisconnected;

        public ServerThread(int port) throws ArqanoreException {
            super("arq_server");

            try {
                this.listener = new ServerSocket(port);
                this.listener.setSoTimeout(acceptTimeout);

                this.isConnected = true;
            } catch (Exception e) {
                onError("Failed to start server", e);
            }
        }

        public void close() throws IOException {
            isConnected = false;
            isDisconnected = true;

            listener.close();
        }

        @Override
        public void run() {
            while (isConnected) {
                try {
                    var socket = listener.accept();
                    socket.setSoTimeout(clientTimeout);

                    var thread = new ServerClientThread(socket);
                    thread.start();
                } catch (Exception e) {
                    onError("An error occurred during socket accept", e);
                }
            }

            if (!isDisconnected) {
                try {
                    listener.close();
                } catch (Exception e) {
                    onError("Failed to close server socket", e);
                }
            }

            isConnected = false;
            isDisconnected = true;
        }
    }

    private class ServerClientThread extends Thread {
        private final ArqClient client;

        public ServerClientThread(Socket socket) {
            this.client = new ArqClient(socket);
        }

        @Override
        public void run() {
            try {
                var bytes = client.read();

                if (bytes == null) {
                    return;
                }

                var msg = parse(bytes);
                var res = onAction(client, msg.getAction(), msg.getBody());

                if (res != null) {
                    client.write(res);
                }

                client.close();
            } catch (Exception e) {
                onError("An error occurred in client thread", e);
            }
        }

        private ArqMessage parse(byte[] data) throws ArqanoreException {
            var msg = new ArqMessage();
            msg.parse(data, data.length);

            return msg;
        }
    }
}
