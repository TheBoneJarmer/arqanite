package be.labruyere.arqanite;

import be.labruyere.arqanore.Joystick;
import be.labruyere.arqanore.Keyboard;
import be.labruyere.arqanore.enums.Keys;
import be.labruyere.arqanore.exceptions.ArqanoreException;

import java.util.HashMap;

public class ArqInput {
    static {
        states = new HashMap<>();
        joystickDisabled = new boolean[15];
        joystickConnected = new boolean[15];
    }

    private static final HashMap<String, State> states;
    private static final boolean[] joystickDisabled;
    private static final boolean[] joystickConnected;
    private static boolean keyboardDisabled;
    private static int joystickId;

    public static int getJoystickId() {
        return joystickId;
    }

    public static void setJoystickId(int joystickId) {
        ArqInput.joystickId = joystickId;
    }

    public static boolean isJoystickEnabled() {
        return !joystickDisabled[joystickId];
    }

    public static boolean isJoystickConnected() throws ArqanoreException {
        return joystickConnected[joystickId];
    }

    public static boolean isKeyboardDisabled() {
        return keyboardDisabled;
    }

    public static void enableJoystick() {
        joystickDisabled[joystickId] = false;
    }

    public static void disableJoystick() {
        joystickDisabled[joystickId] = true;
    }

    public static void enableKeyboard() {
        keyboardDisabled = false;
    }

    public static void disableKeyboard() {
        keyboardDisabled = true;
    }

    public static void map(String name, Keys key, String joystick) {
        mapKey(name, key);
        mapJoystick(name, joystick);
    }

    public static void mapKey(String name, Keys key) {
        var existing = states.get(name);

        if (existing == null) {
            var state = new State();
            state.keyboard = key.toString();

            states.put(name, state);
        } else {
            existing.keyboard = key.toString();
        }
    }

    public static void mapJoystick(String name, String joystick) {
        var existing = states.get(name);

        if (existing == null) {
            var state = new State();
            state.joystick = joystick;

            states.put(name, state);
        } else {
            existing.joystick = joystick;
        }
    }

    public static boolean getState(String name, boolean pressed) {
        var state = states.get(name);

        if (state == null) {
            //ArqLogger.logError("Input state " + name + " not found");
            return false;
        }

        return inputState(pressed, state.value);
    }

    public static void update() throws ArqanoreException {
        for (var set : states.entrySet()) {
            var state = set.getValue();
            var value = state.value;
            var keyboard = state.keyboard;
            var joystick = state.joystick;

            state.value = updateState(keyboard, joystick, value);
        }

        for (var i=0; i<15; i++) {
            if (Joystick.isConnected(i) && !joystickConnected[i]) {
                //ArqLogger.logInfo("Joystick " + i + " connected");
                joystickConnected[i] = true;
            }

            if (!Joystick.isConnected(i) && joystickConnected[i]) {
                //ArqLogger.logInfo("Joystick " + i + " disconnected");
                joystickConnected[i] = false;
            }
        }
    }

    /* HELPER FUNCTIONS */
    private static byte updateState(String key, String stick, byte state) throws ArqanoreException {
        var down = false;
        var keyboard = false;
        var joystick = false;

        if (!keyboardDisabled) {
            keyboard = Keyboard.keyDown(Keys.valueOf(key));
        }

        if (!joystickDisabled[joystickId]) {
            joystick = joystickState(stick);
        }

        down = keyboard || joystick;

        if (state == 2 && !down) state = 0;
        if (state == 1) state = 2;
        if (state == 0 && down) state = 1;

        return state;
    }

    private static boolean inputState(boolean pressed, byte state) {
        return (pressed && state == 1) || (!pressed && state > 0);
    }

    private static boolean joystickState(String data) throws ArqanoreException {
        if (data == null || data.isEmpty()) {
            return false;
        }

        if (!Joystick.isConnected(joystickId)) {
            return false;
        }

        try {
            var type = data.split(",")[0];
            var index = Integer.parseInt(data.split(",")[1]);
            var axis = Joystick.getAxes(0);
            var buttons = Joystick.getButtons(0);

            if (type.equals("axis")) {
                var direction = Integer.parseInt(data.split(",")[2]);
                var value = Math.round(axis[index]);

                return value == direction;
            }

            if (type.equals("button")) {
                return buttons[index] == 1;
            }
        } catch (Exception e) {
            //ArqLogger.logError("An error occurred while retrieving the joystick state", e);
            return false;
        }

        return false;
    }

    /* STATE */
    private static class State {
        public byte value;
        public String keyboard;
        public String joystick;
    }
}
