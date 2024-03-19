package be.labruyere.arqanite.net;

import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ArqClient {
    private static String host;
    private static int port;
    private static int timeout = 1000;

    public static String getHost() {
        return host;
    }

    public static void setHost(String host) {
        ArqClient.host = host;
    }

    public static int getPort() {
        return port;
    }

    public static void setPort(int port) {
        ArqClient.port = port;
    }

    public static int getTimeout() {
        return timeout;
    }

    public static void setTimeout(int timeout) {
        ArqClient.timeout = timeout;
    }

    public static String send(String action, String body) throws ArqanoreException {
        var buffer = new byte[1024 * 10];
        var sb = new StringBuilder();
        var eof = false;

        var msg = new ArqMessage();
        msg.setAction(action);
        msg.setBody(body);

        try {
            var address = new InetSocketAddress(host, port);
            var socket = new Socket();
            socket.connect(address, timeout);

            var os = socket.getOutputStream();
            var is = socket.getInputStream();

            os.write(msg.toBytes());

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

            socket.close();
        } catch (SocketTimeoutException e) {
            throw new ArqanoreException("Socket timeout", e);
        } catch (IOException e) {
            throw new ArqanoreException("Failed to send message", e);
        }

        return sb.toString();
    }
}
