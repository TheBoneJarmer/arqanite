package be.labruyere.arqanite.net;

import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;

public class ArqAsyncClient {
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

    public boolean isConnected() {
        if (thread == null) {
            return false;
        }

        return thread.isConnected();
    }

    /**
     * Returns the associated InetAddress object for the client socket.
     *
     * @return The inet address of the socket
     */
    public InetAddress getInetAddress() {
        if (thread == null) {
            return null;
        }

        return thread.socket.getInetAddress();
    }

    /**
     * Returns the associated <b>remote</b> socket address for the client socket.
     *
     * @return The socket address of the socket
     */
    public SocketAddress getSocketAddress() {
        if (thread == null) {
            return null;
        }

        return thread.socket.getRemoteSocketAddress();
    }

    public void connect(String ip, int port) throws ArqanoreException {
        thread = new ClientThread(ip, port);
        thread.start();
    }

    public void disconnect() throws ArqanoreException {
        if (thread == null) {
            return;
        }

        thread.disconnect();
    }

    public void send(ArqMessage message) throws ArqanoreException {
        if (thread == null) {
            return;
        }

        try {
            thread.send(message);
        } catch (Exception e) {
            throw new ArqanoreException("Failed to send message", e);
        }
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

    private class ClientThread extends Thread {
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
                socket.setSoTimeout(soTimeout);
                socket.connect(new InetSocketAddress(ip, port), connectTimeout);

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
            message.setAction(command);
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
                        var action = message.getAction();
                        var body = message.getBody();

                        // If the server initialized the closure of the connection the client should not receive an error but close connection gracefully
                        if (action.equals("_close")) {
                            reason = message.getBody();

                            isConnected = false;
                            break;
                        }

                        run(action, body);
                    }
                } catch (SocketTimeoutException e) {
                    reason = "Socket timeout";
                    break;
                } catch (SocketException e) {
                    if (!isTerminated) {
                        reason = "Connection lost";
                    }

                    break;
                } catch (Exception e) {
                    reason = "A client error occurred";
                    break;
                }
            }

            try {
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

            isConnected = false;
            thread = null;
        }

        private void run(String command, String body) throws Exception {
            var action = ArqActions.get(command);

            if (action != null) {
                action.runAsync(body);
            }
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
                } catch (ArqanoreException e) {
                    // Ignore
                }

                index1 = data.toString().indexOf(ArqMessage.PREFIX);
                index2 = data.toString().indexOf(ArqMessage.SUFFIX);
            }

            return result;
        }
    }
}
