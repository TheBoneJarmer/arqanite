package be.labruyere.arqanite.net;

import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ArqClient {
    {
        timeout = 0;
    }

    private String host;
    private int port;
    private int timeout;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String send(String action, String body) throws ArqanoreException {
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
