package be.labruyere.arqanite.net;

import be.labruyere.arqanite.ArqLogger;
import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class ArqServer {
    private static ServerThread server;

    public static boolean isRunning() {
        return server != null && server.isRunning;
    }

    public static ServerThread.ServerClientThread getClient(int id) {
        if (server == null) {
            return null;
        }

        return Arrays.stream(server.clients).filter(x -> x.clientId == id).findFirst().orElse(null);
    }

    public static void start(int port) throws ArqanoreException {
        if (server != null && server.isAlive()) {
            return;
        }

        server = new ServerThread(port);
        server.start();
    }

    public static void stop() {
        if (server == null || !server.isAlive()) {
            return;
        }

        server.close();
    }

    private static class ServerThread extends Thread {
        public final ServerSocket socket;
        public final ServerClientThread[] clients;
        public boolean isRunning;

        public ServerThread(int port) throws ArqanoreException {
            super("arq_server");

            try {
                this.socket = new ServerSocket(port);
                this.clients = new ServerClientThread[1000];
                this.isRunning = true;
            } catch (Exception e) {
                throw new ArqanoreException("Failed to start server", e);
            }
        }

        public void close() {
            isRunning = false;
        }

        @Override
        public void run() {
            ServerAcceptThread acceptThread = new ServerAcceptThread();
            acceptThread.start();

            while (isRunning) {
                try {
                    for (int i = 0; i < clients.length; i++) {
                        final var client = clients[i];

                        if (client == null) {
                            continue;
                        }

                        if (!client.isConnected()) {
                            clients[i] = null;
                        }
                    }

                    if (!acceptThread.isAlive()) {
                        break;
                    }

                    Thread.sleep(100);
                } catch (Exception e) {
                    ArqLogger.logError("Failed to process clients", e);
                    break;
                }
            }

            for (var client : clients) {
                if (client == null) {
                    continue;
                }

                if (!client.isConnected) {
                    continue;
                }

                try {
                    client.disconnect();
                } catch (Exception e) {
                    ArqLogger.logError("Failed to close server client connection", e);
                }
            }

            try {
                socket.close();
            } catch (Exception e) {
                ArqLogger.logError("Failed to close server socket", e);
            }

            isRunning = false;
            server = null;
        }

        private static class ServerAcceptThread extends Thread {

            public ServerAcceptThread() {
                super("arq_server_accept");
            }

            @Override
            public void run() {
                while (server.isRunning) {
                    int count = 0;

                    for (int i = 0; i < server.clients.length; i++) {
                        final ServerClientThread client = server.clients[i];

                        if (client == null) {
                            continue;
                        }

                        if (server.clients[i].isConnected()) {
                            count++;
                        }
                    }

                    try {
                        var socket = server.socket.accept();

                        if (count == server.clients.length) {
                            var message = new ArqMessage();
                            message.setCommand("error");
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
                                server.clients[i] = thread;
                                break;
                            }
                        }
                    } catch (SocketException e) {
                        break;
                    } catch (Exception e) {
                        ArqLogger.logError("An error occurred during socket accept", e);
                        break;
                    }
                }
            }
        }

        public static class ServerClientThread extends Thread {
            private final int clientId;
            private final Socket socket;
            private final InputStream is;
            private final OutputStream os;
            private final StringBuilder data;
            private boolean isConnected;
            private boolean isTerminated;

            public boolean isConnected() {
                return isConnected;
            }

            public int getClientId() {
                return clientId;
            }

            public ServerClientThread(Socket socket) throws ArqanoreException {
                final Random random = new Random();

                this.clientId = random.nextInt(100000, 999999);
                this.socket = socket;

                try {
                    socket.setTcpNoDelay(true);

                    is = socket.getInputStream();
                    os = socket.getOutputStream();
                    isConnected = true;
                    data = new StringBuilder();
                } catch (IOException e) {
                    throw new ArqanoreException("Failed to init server client socket", e);
                }

                this.setName("arq_server_client" + this.clientId);
            }

            public void send(ArqMessage message) throws ArqanoreException {
                if (!isConnected) {
                    return;
                }

                try {
                    os.write(message.toBytes());
                } catch (IOException e) {
                    throw new ArqanoreException(e);
                }
            }

            public void send(String command, String body) throws ArqanoreException {
                var message = new ArqMessage();
                message.setCommand(command);
                message.setBody(body);

                send(message);
            }

            public void disconnect() {
                isConnected = false;
                isTerminated = true;

                try {
                    os.close();
                    is.close();
                    socket.close();
                } catch (IOException e) {
                    ArqLogger.logError("Failed to close connection", e);
                }
            }

            @Override
            public void run() {
                var buffer = new byte[1024 * 10];

                try {
                    send("join", Integer.toString(clientId));
                } catch (ArqanoreException e) {
                    ArqLogger.logError("Failed to send join message", e);
                    return;
                }

                try {
                    run("join", Integer.toString(clientId));
                } catch (ArqanoreException e) {
                    ArqLogger.logError(null, e);
                    return;
                }

                while (isConnected) {
                    try {
                        var read = is.read(buffer);

                        if (read == -1) {
                            isTerminated = true;
                            isConnected = false;
                            continue;
                        }

                        data.append(new String(buffer, 0, read));

                        for (var message : parse()) {
                            run(message.getCommand(), message.getBody());
                        }
                    } catch (SocketException e) {
                        isConnected = false;
                    } catch (Exception e) {
                        ArqLogger.logError("[" + getClientId() + "] Unknown error", e);
                        isConnected = false;
                    }
                }

                try {
                    send("leave", Integer.toString(clientId));
                } catch (ArqanoreException e) {
                    ArqLogger.logError("Failed to send leave message", e);
                }

                try {
                    run("leave", Integer.toString(clientId));
                } catch (ArqanoreException e) {
                    ArqLogger.logError(null, e);
                }

                try {
                    is.close();
                    os.close();
                    socket.close();
                } catch (IOException e) {
                    ArqLogger.logError("Failed to close client connection", e);
                }
            }

            private void run(String command, String body) throws ArqanoreException {
                var action = ArqActions.get(command);

                if (action != null) {
                    //ArqLogger.logInfo("[" + clientId + "] " + command + " " + body);
                    action.run(clientId, body);
                } else {
                    ArqLogger.logError("Action " + command + " not found");
                }
            }

            private ArrayList<ArqMessage> parse() throws ArqanoreException {
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
        }
    }
}
