package be.labruyere.arqanite.net;

import be.labruyere.arqanore.exceptions.ArqanoreException;

public class ArqMessage {
    public static final String PREFIX = "<ARQ>";
    public static final String SUFFIX = "</ARQ>";

    private String command;
    private String body;

    public String getCommand() {
        return command;
    }

    public String getBody() {
        return body;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setBody(String body) {
        this.body = body;

        if (body == null || body.isEmpty()) {
            this.body = "null";
        }
    }

    public ArqMessage() {
        this.command = null;
        this.body = null;
    }

    public void parse(byte[] bytes, int length) throws ArqanoreException {
        var raw = new String(bytes);

        try {
            raw = raw.substring(0, length);
            raw = raw.replace(PREFIX, "");
            raw = raw.replace(SUFFIX, "");

            command = raw.split("\\#\\$\\#")[0];
            body = raw.split("\\#\\$\\#")[1];

            if (body.equals("null")) {
                body = null;
            }
        } catch (Exception e) {
            throw new ArqanoreException("Failed to parse message '" + raw + "'", e);
        }
    }

    public byte[] toBytes() {
        return (PREFIX + command + "#$#" + body + SUFFIX).getBytes();
    }
}
