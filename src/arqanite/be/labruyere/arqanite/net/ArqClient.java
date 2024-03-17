package be.labruyere.arqanite.net;

import be.labruyere.arqanite.ArqLogger;
import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class ArqClient {
    private static ClientThread thread;

    public static boolean isConnected() {
        return thread.isConnected();
    }

    public static void connect(String ip, int port) throws ArqanoreException {
        thread = new ClientThread(ip, port);
        thread.start();
    }

    public static void disconnect() throws ArqanoreException {
        if (thread == null) {
            return;
        }

        thread.disconnect();
    }

    public static void send(ArqMessage message) throws ArqanoreException {
        if (thread == null) {
            return;
        }

        try {
            thread.send(message);
        } catch (Exception e) {
            throw new ArqanoreException("Failed to send message", e);
        }
    }

    public static void send(String command, String body) throws ArqanoreException {
        if (thread == null) {
            return;
        }

        try {
            thread.send(command, body);
        } catch (Exception e) {
            throw new ArqanoreException("Failed to send message", e);
        }
    }

    private static class ClientThread extends Thread {
        private final Socket socket;
        private final InputStream is;
        private final OutputStream os;
        private final StringBuilder data;
        private boolean isConnected;
        private boolean isTerminated;

        public boolean isConnected() {
            return isConnected;
        }

        public ClientThread(String ip, int port) throws ArqanoreException {
            super("arq_client");

            try {
                socket = new Socket();
                socket.setTcpNoDelay(true);
                socket.connect(new InetSocketAddress(ip, port), 1000);

                is = socket.getInputStream();
                os = socket.getOutputStream();
                isConnected = true;
                data = new StringBuilder();
            } catch (Exception e) {
                throw new ArqanoreException(e);
            }
        }

        public void disconnect() throws ArqanoreException {
            isConnected = false;
            isTerminated = true;

            try {
                socket.close();
                is.close();
                os.close();
            } catch (IOException e) {
                throw new ArqanoreException("Failed to close connection", e);
            }
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

        @Override
        public void run() {
            var buffer = new byte[10 * 1024];
            var reason = "";

            while (isConnected) {
                try {
                    var read = is.read(buffer);

                    if (read == -1) {
                        reason = "Connection lost";
                        break;
                    }

                    data.append(new String(buffer, 0, read));

                    for (var message : parse()) {
                        run(message.getCommand(), message.getBody());
                    }
                } catch (SocketException e) {
                    if (!isTerminated) {
                        reason = "Connection lost";
                    }

                    break;
                } catch (Exception e) {
                    ArqLogger.logError("An unknown error occurred", e);
                    reason = "A client error occurred";
                    break;
                }
            }

            if (!reason.isEmpty()) {
                try {
                    run("leave", reason);
                } catch (ArqanoreException e) {
                    ArqLogger.logError(e);
                }
            } else {
                try {
                    run("leave", "");
                } catch (ArqanoreException e) {
                    ArqLogger.logError(e);
                }
            }

            try {
                is.close();
                os.close();
                socket.close();
            } catch (IOException e) {
                ArqLogger.logError("Failed to close connection");
            }

            isConnected = false;
            thread = null;
        }

        private void run(String command, String body) throws ArqanoreException {
            var action = ArqActions.get(command);

            if (action != null) {
                //ArqLogger.logInfo("[CLIENT] " + command + " " + body);
                action.run(body);
            } else {
                ArqLogger.logError("Action " + command + " not found");
            }
        }

        private ArrayList<ArqMessage> parse() {
            var result = new ArrayList<ArqMessage>();
            var index1 = data.toString().indexOf(ArqMessage.PREFIX);
            var index2 = data.toString().indexOf(ArqMessage.SUFFIX);
            var raw = "";

            while (index1 != -1 && index2 != -1) {
                //ArqLogger.logInfo("[CLIENT][RAW] " + data);
                raw = data.substring(index1, index2 + ArqMessage.SUFFIX.length());

                try {
                    var message = new ArqMessage();
                    message.parse(raw.getBytes(), raw.length());

                    result.add(message);
                    data.replace(index1, index2 + ArqMessage.SUFFIX.length(), "");
                } catch (ArqanoreException e) {
                    ArqLogger.logError("Failed to parse message", e);
                }

                index1 = data.toString().indexOf(ArqMessage.PREFIX);
                index2 = data.toString().indexOf(ArqMessage.SUFFIX);
            }

            return result;
        }
    }
}
