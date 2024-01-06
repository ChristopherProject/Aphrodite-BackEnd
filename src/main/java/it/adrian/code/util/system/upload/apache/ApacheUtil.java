package it.adrian.code.util.system.upload.apache;

import it.adrian.code.util.encryption.Encryption;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Random;

public class ApacheUtil {

    private static final String BASE_DIRECTORY = "C:" + File.separator + "xampp" + File.separator + "htdocs" + File.separator + "media";

    public static String saveAvatar(InputStream inputStream, String userId, String fileExtension) throws IOException {
        String userDirectory = BASE_DIRECTORY + File.separator + "avatar" + File.separator + Base64.getEncoder().encodeToString((Encryption.encryptPassword(userId + new Random().nextInt(10000))).getBytes()).replace("=", "");
        String fileName = generateFileName(userId, fileExtension);
        Path directory = Paths.get(userDirectory);
        Files.createDirectories(directory);
        Path filePath = directory.resolve(fileName);
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
             OutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return "http://localhost" + userDirectory.replace("C:" + File.separator + "xampp" + File.separator + "htdocs", "").replace(File.separator, "/") + "/" + fileName;
        }
    }

    private static String generateFileName(String userId, String fileExtension) {
        return Base64.getEncoder().encodeToString(("profile_pic_of_" + userId).getBytes()).replace("=", "") + fileExtension;
    }
}