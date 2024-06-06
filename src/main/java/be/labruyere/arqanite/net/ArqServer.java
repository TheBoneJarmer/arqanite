package be.labruyere.arqanite.net;

import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ArqServer {
    private ServerThread server;
    private int acceptTimeout;
    private int clientTimeout;

    public boolean isRunning() {
        return server != null && server.isConnected;
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
        if (server != null && server.isAlive()) {
            return;
        }

        server = new ServerThread(port);
        server.start();
    }

    public void stop() throws ArqanoreException {
        if (server == null || !server.isAlive()) {
            return;
        }

        server.close();
    }

    private class ServerThread extends Thread {
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
                throw new ArqanoreException(e);
            }
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
                    break;
                }
            }

            if (!isDisconnected) {
                try {
                    listener.close();
                } catch (Exception e) {
                    // Ignore
                }
            }

            isConnected = false;
            isDisconnected = true;
        }
    }

    private static class ServerClientThread extends Thread {
        private final Socket socket;

        public ServerClientThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                var bytes = read(socket);

                if (bytes == null) {
                    return;
                }

                var msg = parse(bytes);
                var res = run(msg.getAction(), msg.getBody());

                if (res != null) {
                    write(socket, res.getBytes());
                }

                socket.close();
            } catch (Exception e) {
                // Ignore
            }
        }

        private ArqMessage parse(byte[] data) throws ArqanoreException {
            var msg = new ArqMessage();
            msg.parse(data, data.length);

            return msg;
        }

        private String run(String command, String body) throws Exception {
            var action = ArqActions.get(command);

            if (action == null) {
                return null;
            }

            var res = action.run(body);

            if (res == null) {
                return "null";
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
