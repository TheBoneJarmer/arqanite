package be.labruyere.arqanite;

import be.labruyere.arqanite.enums.ArqLogLevel;
import be.labruyere.arqanite.enums.ArqLogType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

public class ArqLogger {
    private static Path folder;
    private static Path file;
    private static boolean toFile;
    private static boolean toConsole;
    private static ArqLogLevel level;

    public static boolean isToFile() {
        return toFile;
    }

    public static boolean isToConsole() {
        return toConsole;
    }

    public static ArqLogLevel getLevel() {
        return level;
    }

    public static void logInfo(String message) {
        log(ArqLogType.INFO, message);
    }

    public static void logWarning(String message) {
        log(ArqLogType.WARNING, message);
    }

    public static void logError(String message) {
        log(ArqLogType.ERROR, message);
    }

    public static void logError(Exception e) {
        logError(null, e);
    }

    public static void logSuccess(String message) {
        log(ArqLogType.SUCCESS, message);
    }

    public static void logError(String message, Exception e) {
        if (message != null) {
            log(ArqLogType.ERROR, message);
        }

        if (level == ArqLogLevel.DEBUG) {
            var sw = new StringWriter();
            var pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            log(ArqLogType.ERROR, e.getMessage());
            log(ArqLogType.ERROR, sw.toString());
        }

        if (level == ArqLogLevel.PRODUCTION) {
            log(ArqLogType.ERROR, "An exception occurred");
        }
    }

    private static void log(ArqLogType type, String message) {
        var time = LocalTime.now();
        var line = "[" + time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + "]";

        if (type == ArqLogType.INFO) line += "[INFO] ";
        if (type == ArqLogType.WARNING) line += "[WARNING] ";
        if (type == ArqLogType.ERROR) line += "[ERROR] ";
        if (type == ArqLogType.SUCCESS) line += "[SUCCESS] ";

        line += message;

        if (toFile) {
            try {
                var writer = new FileWriter(file.toString(), true);
                writer.write(line + "\n");
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (toConsole) {
            if (type == ArqLogType.ERROR) {
                System.err.println(line);
            } else {
                System.out.println(line);
            }
        }
    }

    public static void init(ArqLogLevel level, boolean toFile, boolean toConsole) {
        ArqLogger.level = level;
        ArqLogger.toFile = toFile;
        ArqLogger.toConsole = toConsole;

        if (toFile) {
            folder = createFolder();
            file = createFile(folder);
        }
    }

    private static Path createFile(Path folder) {
        var date = LocalDateTime.now();
        var path = Paths.get(folder + "/" + date.getYear() + "-" + date.getMonth().getValue() + "-" + date.getDayOfMonth() + ".log");

        if (Files.exists(path)) {
            return path;
        }

        try {
            Files.createFile(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return path;
    }

    private static Path createFolder() {
        var path = Paths.get("logs");

        if (Files.exists(path)) {
            return path;
        }

        try {
            path = Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log folder", e);
        }

        return path;
    }
}
