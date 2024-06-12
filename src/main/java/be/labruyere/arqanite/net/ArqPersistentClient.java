package be.labruyere.arqanite.net;

import be.labruyere.arqanite.enums.ArqConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public final class ArqPersistentClient {
    private final int id;
    private final Socket socket;
    private Object userData;

    ArqConnection connection;

    public int getId() {
        return id;
    }

    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }

    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    public ArqConnection getConnection() {
        return connection;
    }

    /**
     * Returns the custom user data object attached to this client thread
     *
     * @return The user data for this client
     */
    public Object getUserData() {
        return userData;
    }

    /**
     * Sets the custom user data object for this client
     *
     * @param userData The userdata object, can be anything.
     */
    public void setUserData(Object userData) {
        this.userData = userData;
    }

    ArqPersistentClient(Socket socket, int id) throws SocketException {
        this.socket = socket;
        this.id = id;
        this.connection = ArqConnection.OPEN;

        socket.setTcpNoDelay(true); // Enable Nagle's algorithm
    }

    public void send(String name, String body) throws IOException {
        var message = new ArqMessage();
        message.setAction(name);
        message.setBody(body);

        write(message.toBytes());
    }

    public void close(String reason) throws IOException {
        connection = ArqConnection.CLOSED;

        send("_close", reason);
        close();
    }

    /* SOCKET METHODS */
    void write(String data) throws IOException {
        write(data.getBytes());
    }

    void write(byte[] data) throws IOException {
        var os = socket.getOutputStream();
        os.write(data);
    }

    String read() throws IOException {
        var buffer = new byte[1024 * 10];
        var is = socket.getInputStream();
        var read = is.read(buffer);

        if (read == -1) {
            return null;
        }

        return new String(buffer, 0, read);
    }

    void close() throws IOException {
        socket.close();
    }
}
