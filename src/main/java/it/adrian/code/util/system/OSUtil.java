package it.adrian.code.util.system;

public class OSUtil {

    public static OS getPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OS.windows;
        } else if (osName.contains("mac")) {
            return OS.macos;
        } else if (osName.contains("solaris")) {
            return OS.solaris;
        } else if (osName.contains("sunos")) {
            return OS.solaris;
        } else if (osName.contains("linux")) {
            return OS.linux;
        } else {
            return osName.contains("unix") ? OS.linux : OS.unknown;
        }
    }

    public enum OS {
        linux, solaris, windows, macos, unknown
    }
}