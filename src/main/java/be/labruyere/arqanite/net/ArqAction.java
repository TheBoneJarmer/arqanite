package be.labruyere.arqanite.net;

import com.google.gson.Gson;

public abstract class ArqAction {
    private final Gson gson;
    private final String command;

    public String getCommand() {
        return command;
    }

    public ArqAction(String command) {
        this.command = command;
        this.gson = new Gson();
    }

    public void runAsync(int clientId, String body) throws Exception {

    }

    public void runAsync(String body) throws Exception {

    }

    public String run(String body) throws Exception {
        return null;
    }

    protected <T> T fromJson(String json, Class<T> cls) {
        return gson.fromJson(json, cls);
    }

    protected String toJson(Object obj) {
        return gson.toJson(obj);
    }
}
