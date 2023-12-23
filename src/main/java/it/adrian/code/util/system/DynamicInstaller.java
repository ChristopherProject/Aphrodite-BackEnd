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
                downloadFile("https://fastdl.mongodb.org/windows/mongodb-windows-x86_64-7.0.4-signed.msi", "mongodb-windows-x86_64-7.0.4-signed.msi", System.getenv("TEMP"));
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
                    boolean isInstalled = exitCode == 0;
                    return isInstalled;
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

    private static void downloadFile(String url, String fileName, String directory) {
        Path savedPath = Path.of(directory, fileName);
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, savedPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

 /*   private static void saveFile(String url, String fileName, String directory) {
        final File savedPath = new File(directory + File.separator + fileName);
        try {
            URL LINK = new URL(url);
            InputStream in;
            ByteArrayOutputStream by_arr;
            int max_data = 2024;
            try {
                in = LINK.openStream();
                by_arr = new ByteArrayOutputStream(max_data);
                int length = -1;
                byte[] buffer = new byte[max_data];
                while ((length = in.read(buffer)) > -1) {
                    by_arr.write(buffer, 0, length);
                }
                by_arr.close();
                in.close();
                try (FileOutputStream fw = new FileOutputStream(savedPath.getAbsolutePath())) {
                    fw.write(by_arr.toByteArray());
                } catch (Exception ignored) {
                }
            } catch (IOException ignored) {
            }
        } catch (Exception ignored) {
        }
    }*/
}