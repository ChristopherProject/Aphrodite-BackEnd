package it.adrian.code.util.system;

public class OSUtil {

    public static OS getPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OS.windows;
        } else if (osName.contains("linux")) {
            return OS.linux;
        } else {
            return osName.contains("unix") ? OS.linux : OS.unknown;
        }
    }

    public enum OS {
        linux, windows, unknown
    }
}