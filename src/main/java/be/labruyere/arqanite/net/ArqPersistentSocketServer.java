package be.labruyere.arqanite.net;

import be.labruyere.arqanite.enums.ArqConnection;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class ArqPersistentSocketServer {
    private final Random random;
    private ServerThread server;
    private int clientTimeout;
    private int acceptTimeout;

    public int getClientTimeout() {
        return clientTimeout;
    }

    public void setClientTimeout(int clientTimeout) {
        this.clientTimeout = clientTimeout;
    }

    public int getAcceptTimeout() {
        return acceptTimeout;
    }

    public void setAcceptTimeout(int acceptTimeout) {
        this.acceptTimeout = acceptTimeout;
    }

    public ArqPersistentSocketServer() {
        random = new Random();
    }

    public boolean isRunning() {
        return server != null && server.isRunning;
    }

    public ArqPersistentClient[] getClients() {
        if (server == null) {
            return null;
        }

        var stream = Arrays.stream(server.clients);
        stream = stream.filter(Objects::nonNull);

        return stream.toArray(ArqPersistentClient[]::new);
    }

    public ArqPersistentClient getClient(int id) {
        if (server == null) {
            return null;
        }

        var stream = Arrays.stream(server.clients);
        stream = stream.filter(Objects::nonNull);
        stream = stream.filter(f -> f.getId() == id);

        return stream.findFirst().orElse(null);
    }

    public void start(int port) {
        if (server != null && server.isAlive()) {
            return;
        }

        server = new ServerThread(port);
        server.start();
    }

    public void stop() {
        if (server == null || !server.isAlive()) {
            return;
        }

        server.close();
    }

    /* EVENTS */
    protected void onAction(ArqPersistentClient client, String action, String body) throws Exception {

    }

    protected void onConnect(ArqPersistentClient client) {

    }

    protected void onClose(ArqPersistentClient client, String reason) {

    }

    protected void onError(String message, Exception e) {

    }

    protected void onError(ArqPersistentClient client, String message, Exception e) {

    }

    /* THREADS */
    private class ServerThread extends Thread {
        public ServerSocket socket;
        public ArqPersistentClient[] clients;
        public boolean isRunning;

        public ServerThread(int port) {
            super("arq_server");

            try {
                this.socket = new ServerSocket(port);
                this.socket.setSoTimeout(acceptTimeout);
                this.clients = new ArqPersistentClient[1000];
                this.isRunning = true;
            } catch (Exception e) {
                onError("Failed to start server", e);
            }
        }

        public void close() {
            isRunning = false;
        }

        @Override
        public void run() {
            var acceptThread = new ServerAcceptThread();
            acceptThread.start();

            while (isRunning) {
                try {
                    for (var i = 0; i < clients.length; i++) {
                        var client = clients[i];

                        if (client == null) {
                            continue;
                        }

                        if (client.connection == ArqConnection.CLOSED) {
                            clients[i] = null;
                        }
                    }

                    if (!acceptThread.isAlive()) {
                        break;
                    }

                    Thread.sleep(100);
                } catch (Exception e) {
                    onError("An exception occurred in the server thread. Shutting down.", e);
                    break;
                }
            }

            for (var client : clients) {
                if (client == null) {
                    continue;
                }

                if (client.connection == ArqConnection.CLOSED) {
                    continue;
                }

                try {
                    client.close("Server shutdown");
                } catch (Exception e) {
                    onError("Failed to disconnect client " + client.getId(), e);
                }
            }

            try {
                socket.close();
            } catch (Exception e) {
                onError("Failed to close server socket", e);
            }

            isRunning = false;
            server = null;
        }
    }

    private class ServerAcceptThread extends Thread {

        public ServerAcceptThread() {
            super("arq_server_accept");
        }

        @Override
        public void run() {
            while (server.isRunning) {
                int count = getCount();

                try {
                    var socket = server.socket.accept();
                    socket.setSoTimeout(clientTimeout);

                    if (count == server.clients.length) {
                        var message = new ArqMessage();
                        message.setAction("_close");
                        message.setBody("Server is full");

                        var os = socket.getOutputStream();
                        os.write(message.toBytes());

                        os.close();
                        socket.close();

                        continue;
                    }

                    var thread = new ServerClientThread(socket);
                    thread.start();

                    for (int i = 0; i < server.clients.length; i++) {
                        if (server.clients[i] == null) {
                            server.clients[i] = thread.client;
                            break;
                        }
                    }
                } catch (SocketException e) {
                    // Do nothing with this exception. This one is thrown because the server socket state has changed.
                    // Therefore the server thread handles this one.
                    break;
                } catch (Exception e) {
                    onError("An exception occurred in accept thread. Thread is being terminated now so new connections won't be handled!", e);
                    break;
                }
            }
        }

        private int getCount() {
            var count = 0;

            for (var i = 0; i < server.clients.length; i++) {
                var client = server.clients[i];

                if (client == null) {
                    continue;
                }

                if (client.connection == ArqConnection.OPEN) {
                    count++;
                }
            }

            return count;
        }
    }

    public class ServerClientThread extends Thread {
        private int clientId;
        private ArqPersistentClient client;
        private StringBuilder data;

        public ServerClientThread(Socket socket) {
            try {
                this.clientId = generateId();
                this.client = new ArqPersistentClient(socket, clientId);

                data = new StringBuilder();
            } catch (IOException e) {
                onError("Failed to instantiate server client", e);
            }

            this.setName("arq_server_client" + this.clientId);
        }

        @Override
        public void run() {
            var closeReason = "";

            onConnect(client);

            while (true) {
                if (closeReason != null && !closeReason.isEmpty()) {
                    break;
                }

                if (client.connection == ArqConnection.CLOSED) {
                    break;
                }

                try {
                    var chunk = client.read();

                    if (chunk == null) {
                        closeReason = "Connection lost";
                    } else {
                        data.append(chunk);
                    }

                    for (var message : parse()) {
                        var action = message.getAction();
                        var body = message.getBody();

                        if (action.equals("_close")) {
                            closeReason = body;

                            client.connection = ArqConnection.CLOSED;
                            client.close();
                            break;
                        }

                        onAction(client, action, body);
                    }
                } catch (SocketTimeoutException e) {
                    closeReason = "Socket timeout";
                } catch (SocketException e) {
                    if (client.connection == ArqConnection.OPEN) {
                        closeReason = "Connection lost";
                    }
                } catch (Exception e) {
                    onError(client, "A server error occurred", e);
                    closeReason = "An server error occurred";
                }
            }

            if (client.connection != ArqConnection.CLOSED) {
                try {
                    client.close(closeReason);
                } catch (Exception e) {
                    onError("Failed to close client socket", e);
                }
            }

            onClose(client, closeReason);
        }

        private ArrayList<ArqMessage> parse() throws Exception {
            var result = new ArrayList<ArqMessage>();
            var index1 = data.toString().indexOf(ArqMessage.PREFIX);
            var index2 = data.toString().indexOf(ArqMessage.SUFFIX);
            var raw = "";

            while (index1 != -1 && index2 != -1) {
                //ArqLogger.logInfo("[" + getClientId() + "][RAW] " + data);
                raw = data.substring(index1, index2 + ArqMessage.SUFFIX.length());

                var message = new ArqMessage();
                message.parse(raw.getBytes(), raw.length());

                result.add(message);
                data.replace(index1, index2 + ArqMessage.SUFFIX.length(), "");

                index1 = data.toString().indexOf(ArqMessage.PREFIX);
                index2 = data.toString().indexOf(ArqMessage.SUFFIX);
            }

            return result;
        }

        private int generateId() {
            var result = 0;

            while (true) {
                var id = random.nextInt(100000, 999999);
                var existing = getClient(id);

                if (existing != null) {
                    continue;
                }

                result = id;
                break;
            }

            return result;
        }
    }
}
