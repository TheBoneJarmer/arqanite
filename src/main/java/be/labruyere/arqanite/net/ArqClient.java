package be.labruyere.arqanite.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Random;

public final class ArqClient {
    private final Socket socket;

    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }

    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    ArqClient(Socket socket) {
        this.socket = socket;
    }

    /* SOCKET METHODS */
    void write(String data) throws IOException {
        write(data.getBytes());
    }

    void write(byte[] data) throws IOException {
        var os = socket.getOutputStream();
        os.write(data);
    }

    byte[] read() throws IOException {
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

    void close() throws IOException {
        socket.close();
    }
}
