package be.labruyere.arqanite.net;

import be.labruyere.arqanite.enums.ArqConnection;
import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ArqPersistentSocketClient {
    private ClientThread thread;
    private int soTimeout;
    private int connectTimeout = 1000;

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public ArqPersistentSocketClient() {

    }

    public ArqConnection getConnection() {
        if (thread == null) {
            return null;
        }

        return thread.connection;
    }

    public void connect(String ip, int port) throws ArqanoreException {
        thread = new ClientThread(ip, port);
        thread.start();
    }

    public void close(String reason) throws ArqanoreException {
        if (thread == null) {
            return;
        }

        thread.close(reason);
    }

    public void send(String command, String body) throws ArqanoreException {
        if (thread == null) {
            return;
        }

        try {
            thread.send(command, body);
        } catch (Exception e) {
            throw new ArqanoreException("Failed to send message", e);
        }
    }

    /* EVENTS */
    protected void onAction(String action, String body) throws Exception {

    }

    protected void onConnect() {

    }

    protected void onClose(String reason) {

    }

    protected void onError(String message, Exception e) {

    }

    /* THREADS */
    private class ClientThread extends Thread {
        private final Socket socket;
        private final StringBuilder data;
        private ArqConnection connection;

        public ArqConnection getConnection() {
            return connection;
        }

        public ClientThread(String ip, int port) throws ArqanoreException {
            super("arq_client");

            try {
                socket = new Socket();
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(soTimeout);
                socket.connect(new InetSocketAddress(ip, port), connectTimeout);

                data = new StringBuilder();
                connection = ArqConnection.OPEN;
            } catch (Exception e) {
                throw new ArqanoreException(e);
            }
        }

        public void close(String reason) throws ArqanoreException {
            if (connection == ArqConnection.CLOSED) {
                throw new ArqanoreException("Cannot close connection because connection is not open");
            }

            try {
                var message = new ArqMessage();
                message.setAction("_close");
                message.setBody(reason);

                try {
                    var os = socket.getOutputStream();
                    os.write(message.toBytes());
                } catch (IOException e) {
                    onError("Failed to send message", e);
                }

                connection = ArqConnection.CLOSED;
                socket.close();
            } catch (Exception e) {
                onError("Failed to close connection", e);
            }
        }
        public void send(String command, String body) throws ArqanoreException {
            if (connection != ArqConnection.OPEN) {
                throw new ArqanoreException("Cannot send message because connection is not open");
            }

            var message = new ArqMessage();
            message.setAction(command);
            message.setBody(body);

            try {
                var os = socket.getOutputStream();
                os.write(message.toBytes());
            } catch (IOException e) {
                onError("Failed to send message", e);
            }
        }

        @Override
        public void run() {
            var closeReason = "";
            var buffer = new byte[10 * 1024];

            onConnect();

            while (true) {
                if (!closeReason.isEmpty()) {
                    break;
                }

                if (socket.isClosed()) {
                    break;
                }

                try {
                    var is = socket.getInputStream();
                    var read = is.read(buffer);

                    if (read == -1) {
                        closeReason = "Connection lost";
                    } else {
                        data.append(new String(buffer, 0, read));
                    }

                    for (var message : parse()) {
                        var action = message.getAction();
                        var body = message.getBody();

                        // If the server initialized the closure of the connection the client should not receive an error but close connection gracefully
                        if (action.equals("_close")) {
                            closeReason = body;
                            break;
                        }

                        onAction(action, body);
                    }
                } catch (SocketTimeoutException e) {
                    closeReason = "Socket timeout";
                } catch (SocketException e) {
                    if (connection == ArqConnection.OPEN) {
                        closeReason = "Connection lost";
                    }
                } catch (Exception e) {
                    onError("A client error occurred", e);
                    closeReason = "A client error occurred";
                }
            }

            try {
                if (connection != ArqConnection.CLOSED) {
                    connection = ArqConnection.CLOSED;
                    socket.close();
                }
            } catch (Exception e) {
                onError("Failed to close socket", e);
            }

            onClose(closeReason);
        }

        private ArrayList<ArqMessage> parse() {
            var result = new ArrayList<ArqMessage>();
            var index1 = data.toString().indexOf(ArqMessage.PREFIX);
            var index2 = data.toString().indexOf(ArqMessage.SUFFIX);
            var raw = "";

            while (index1 != -1 && index2 != -1) {
                raw = data.substring(index1, index2 + ArqMessage.SUFFIX.length());

                try {
                    var message = new ArqMessage();
                    message.parse(raw.getBytes(), raw.length());

                    result.add(message);
                    data.replace(index1, index2 + ArqMessage.SUFFIX.length(), "");
                } catch (Exception e) {
                    // Ignore this exception. It is possible the socket data was not complete yet thus parsing will fail. This is expected.
                }

                index1 = data.toString().indexOf(ArqMessage.PREFIX);
                index2 = data.toString().indexOf(ArqMessage.SUFFIX);
            }

            return result;
        }
    }
}
