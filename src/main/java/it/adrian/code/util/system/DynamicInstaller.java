package it.adrian.code.util.system;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class DynamicInstaller {

    public static void install() {
        switch (OSUtil.getPlatform()) {
            case linux:
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "sudo apt update && sudo apt install -y mongodb-org");
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        System.out.println("installation completed successfully.");
                    } else {
                        System.out.println("installation failed.\nExit code: " + exitCode);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case windows:
                System.out.println("downloading installer..");
                downloadFile(System.getenv("TEMP"));
                try {
                    Process process = Runtime.getRuntime().exec("msiexec /i " + System.getenv("TEMP") + File.separator + "mongodb-windows-x86_64-7.0.4-signed.msi");
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        System.out.println("installation completed successfully.");
                    } else {
                        System.out.println("installation failed.\nExit code: " + exitCode);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public static boolean checkMongoDB() {
        switch (OSUtil.getPlatform()) {
            case linux:
                try {
                    Process process = Runtime.getRuntime().exec("mongod --version");
                    BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String linea;
                    while ((linea = input.readLine()) != null) {
                        System.out.println(linea);
                    }
                    int exitCode = process.waitFor();
                    input.close();
                    return exitCode == 0;
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case windows:
                final File mongoDir = new File("C:\\Program Files\\MongoDB\\Server\\7.0\\bin");
                if (mongoDir.exists()) {
                    return true;
                }
                break;
        }
        return false;
    }

    private static void downloadFile(String directory) {
        Path savedPath = Path.of(directory, "mongodb-windows-x86_64-7.0.4-signed.msi");
        try (InputStream in = new URL("https://fastdl.mongodb.org/windows/mongodb-windows-x86_64-7.0.4-signed.msi").openStream()) {
            Files.copy(in, savedPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}