package be.labruyere.arqanite.net;

import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class ArqAsyncServer {
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

    public ArqAsyncServer() {
        random = new Random();
    }

    public boolean isRunning() {
        return server != null && server.isRunning;
    }

    public ServerClientThread[] getClients() {
        if (server == null) {
            return null;
        }

        var stream = Arrays.stream(server.clients);
        stream = stream.filter(Objects::nonNull);

        return stream.toArray(ServerClientThread[]::new);
    }

    public ServerClientThread getClient(int id) {
        if (server == null) {
            return null;
        }

        var stream = Arrays.stream(server.clients);
        stream = stream.filter(Objects::nonNull);
        stream = stream.filter(f -> f.clientId == id);

        return stream.findFirst().orElse(null);
    }

    public void start(int port) throws ArqanoreException {
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

    private class ServerThread extends Thread {
        public final ServerSocket socket;
        public final ServerClientThread[] clients;
        public boolean isRunning;

        public ServerThread(int port) throws ArqanoreException {
            super("arq_server");

            try {
                this.socket = new ServerSocket(port);
                this.socket.setSoTimeout(acceptTimeout);
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
            var acceptThread = new ServerAcceptThread();
            acceptThread.start();

            while (isRunning) {
                try {
                    for (var i = 0; i < clients.length; i++) {
                        var client = clients[i];

                        if (client == null) {
                            continue;
                        }

                        if (!client.isAlive()) {
                            clients[i] = null;
                        }
                    }

                    if (!acceptThread.isAlive()) {
                        break;
                    }

                    Thread.sleep(100);
                } catch (Exception e) {
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
                    client.disconnect("Server shutdown");
                } catch (Exception e) {
                    // Ignore
                }
            }

            try {
                socket.close();
            } catch (Exception e) {
                // Ignore
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
                            server.clients[i] = thread;
                            break;
                        }
                    }
                } catch (Exception e) {
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

                if (client.isConnected()) {
                    count++;
                }
            }

            return count;
        }
    }

    public class ServerClientThread extends Thread {
        private final int clientId;
        private final Socket socket;
        private final InputStream is;
        private final OutputStream os;
        private final StringBuilder data;
        private boolean isConnected;
        private boolean isDisconnected;
        private Object userData;

        public boolean isConnected() {
            return isConnected;
        }

        public int getClientId() {
            return clientId;
        }

        /**
         * Returns the custom user data object attached to this client thread
         * @return The user data for this client
         */
        public Object getUserData() {
            return userData;
        }

        /**
         * Sets the custom user data object for this client
         * @param userData The userdata object, can be anything.
         */
        public void setUserData(Object userData) {
            this.userData = userData;
        }

        /**
         * Returns the associated InetAddress object for the socket.
         * @return The inet address of the socket
         */
        public InetAddress getInetAddress() {
            return socket.getInetAddress();
        }

        /**
         * Returns the associated <b>remote</b> socket address for the socket.
         * @return The socket address of the socket
         */
        public SocketAddress getSocketAddress() {
            return socket.getRemoteSocketAddress();
        }

        public ServerClientThread(Socket socket) throws ArqanoreException {
            this.clientId = generateId();
            this.socket = socket;

            try {
                socket.setTcpNoDelay(true); // Enable Nagle's algorithm

                is = socket.getInputStream();
                os = socket.getOutputStream();
                isConnected = true;
                data = new StringBuilder();
            } catch (IOException e) {
                throw new ArqanoreException("Failed to instantiate server client socket", e);
            }

            this.setName("arq_server_client" + this.clientId);
        }

        public void send(ArqMessage message) throws ArqanoreException {
            try {
                os.write(message.toBytes());
            } catch (IOException e) {
                throw new ArqanoreException(e);
            }
        }

        public void send(String command, String body) throws ArqanoreException {
            var message = new ArqMessage();
            message.setAction(command);
            message.setBody(body);

            send(message);
        }

        public void disconnect(String reason) throws ArqanoreException {
            isConnected = false;
            isDisconnected = true;

            try {
                send("_close", reason);
                run("_close", reason);
            } catch (Exception e) {
                // Ignore
            }

            try {
                is.close();
                os.close();
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        @Override
        public void run() {
            var buffer = new byte[1024 * 10];
            var reason = "";

            try {
                send("_connect", Integer.toString(clientId));
            } catch (ArqanoreException e) {
                return;
            }

            try {
                run("_connect", Integer.toString(clientId));
            } catch (Exception e) {
                return;
            }

            while (isConnected) {
                try {
                    var read = is.read(buffer);

                    if (read == -1) {
                        isConnected = false;
                        continue;
                    }

                    data.append(new String(buffer, 0, read));

                    for (var message : parse()) {
                        run(message.getAction(), message.getBody());
                    }
                } catch (SocketTimeoutException e) {
                    reason = "Socket timeout";
                    break;
                } catch (SocketException e) {
                    if (!isDisconnected) {
                        reason = "Connection lost";
                    }

                    break;
                } catch (Exception e) {
                    reason = "A server error occurred";
                    break;
                }
            }

            // Only send these messages when the client was not disconnected manually
            if (!isDisconnected) {
                try {
                    send("_close", reason);
                } catch (ArqanoreException e) {
                    // Ignore
                }

                try {
                    run("_close", reason);
                } catch (Exception e) {
                    // Ignore
                }
            }

            // Only close the streams and socket when the client was not disconnected manually
            if (!isDisconnected) {
                try {
                    is.close();
                    os.close();
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }

            isConnected = false;
            isDisconnected = true;
        }

        private void run(String command, String body) throws Exception {
            var action = ArqActions.get(command);

            if (action != null) {
                action.runAsync(clientId, body);
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
