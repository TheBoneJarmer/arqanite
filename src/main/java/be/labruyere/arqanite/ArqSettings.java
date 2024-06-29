package be.labruyere.arqanite;

import be.labruyere.arqanore.enums.Keys;
import be.labruyere.arqanore.utils.EnumUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

public class ArqSettings {
    static {
        settings = new HashMap<>();

        reset();
    }

    private static final HashMap<String, String> settings;

    public static String getString(String key) {
        return settings.get(key);
    }

    public static int getInt(String key) {
        String value = getString(key);

        if (value == null) {
            return 0;
        }

        return Integer.parseInt(settings.get(key));
    }

    public static boolean getBoolean(String key) {
        String value = getString(key);

        if (value == null) {
            return false;
        }

        return value.equals("1");
    }

    public static Keys getKey(String key) {
        var value = getInt(key);

        if (value == -1) {
            return null;
        }

        return EnumUtils.convertKey(value);
    }

    public static boolean contains(String key) {
        return settings.containsKey(key);
    }

    public static void set(String key, String value) {
        if (settings.containsKey(key)) {
            settings.replace(key, value);
        } else {
            settings.put(key, value);
        }
    }

    public static void set(String key, boolean value) {
        set(key, value ? "1" : "0");
    }

    public static void set(String key, int value) {
        set(key, Integer.toString(value));
    }

    public static void set(String key, Keys value) {
        set(key, EnumUtils.convertKey(value));
    }

    public static void reset() {
        settings.clear();
    }

    public static void load() throws FileNotFoundException {
        var file = new File("settings.txt");
        var scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            var line = scanner.nextLine();
            var key = line.split("=")[0];
            var value = line.split("=")[1];

            set(key, value);
        }

        scanner.close();
    }

    public static void save() throws IOException {
        var writer = new FileWriter("settings.txt");
        var keys = new ArrayList<>(settings.keySet());

        // Sort the keys alphabetically because else the whole file is a mess
        Collections.sort(keys);

        for (var key : keys) {
            var value = getString(key);

            if (value == null || value.isEmpty()) {
                continue;
            }

            writer.write(key + "=" + getString(key) + "\n");
        }

        writer.close();
    }
}
