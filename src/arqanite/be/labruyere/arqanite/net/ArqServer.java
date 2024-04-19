package be.labruyere.arqanite.net;

import be.labruyere.arqanite.ArqLogger;
import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ArqServer {
    static {
        acceptTimeout = 0;
        clientTimeout = 0;
    }

    private static ServerThread server;
    private static int acceptTimeout;
    private static int clientTimeout;

    public static boolean isRunning() {
        return server != null && server.isConnected;
    }

    public static int getAcceptTimeout() {
        return acceptTimeout;
    }

    public static int getClientTimeout() {
        return clientTimeout;
    }

    public static void setClientTimeout(int clientTimeout) {
        ArqServer.clientTimeout = clientTimeout;
    }

    public static void setAcceptTimeout(int acceptTimeout) {
        ArqServer.acceptTimeout = acceptTimeout;
    }

    public static void start(int port) throws ArqanoreException {
        if (server != null && server.isAlive()) {
            return;
        }

        server = new ServerThread(port);
        server.start();
    }

    public static void stop() throws ArqanoreException {
        if (server == null || !server.isAlive()) {
            return;
        }

        server.close();
    }

    private static class ServerThread extends Thread {
        public final ServerSocket listener;
        public boolean isConnected;
        public boolean isDisconnected;

        public ServerThread(int port) throws ArqanoreException {
            super("arq_server_static");

            try {
                this.listener = new ServerSocket(port);
                this.listener.setSoTimeout(acceptTimeout);

                this.isConnected = true;
            } catch (Exception e) {
                throw new ArqanoreException("Failed to start server", e);
            }
        }

        public void close() throws ArqanoreException {
            isConnected = false;
            isDisconnected = true;

            try {
                listener.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            while (isConnected) {
                try {
                    var socket = listener.accept();
                    socket.setSoTimeout(clientTimeout);

                    var bytes = read(socket);

                    if (bytes == null) {
                        continue;
                    }

                    var msg = parse(bytes);
                    var res = run(msg);
                    write(socket, res.getBytes());

                    socket.close();
                } catch (SocketException e) {
                    if (!isDisconnected) {
                        ArqLogger.logError(e);
                    }

                    break;
                } catch (Exception e) {
                    ArqLogger.logError(e);
                    break;
                }
            }

            if (!isDisconnected) {
                try {
                    listener.close();
                } catch (Exception e) {
                    ArqLogger.logError("Failed to close server socket", e);
                }
            }

            isConnected = false;
            isDisconnected = true;
        }

        private ArqMessage parse(byte[] data) throws ArqanoreException {
            var msg = new ArqMessage();
            msg.parse(data, data.length);

            return msg;
        }

        private String run(ArqMessage msg) throws ArqanoreException {
            var action = ArqActions.get(msg.getAction());

            if (action == null) {
                ArqLogger.logError("Action " + msg.getAction() + " not found");
                return "";
            }

            var res = action.run(msg.getBody());

            if (res == null) {
                return "";
            }

            return res;
        }

        private void write(Socket socket, byte[] data) throws IOException {
            var os = socket.getOutputStream();
            os.write(data);
        }

        private byte[] read(Socket socket) throws IOException {
            var is = socket.getInputStream();
            var buffer = new byte[1024 * 10];
            var sb = new StringBuilder();
            var eof = false;

            while (!eof) {
                var read = is.read(buffer);

                if (read == -1) {
                    eof = true;
                } else {
                    var chunk = new String(buffer, 0, read);
                    sb.append(chunk);
                }

                if (sb.toString().endsWith("</ARQ>")) {
                    break;
                }
            }

            if (eof) {
                return null;
            }

            var str = sb.toString();
            var bytes = str.getBytes();

            return bytes;
        }
    }
}
