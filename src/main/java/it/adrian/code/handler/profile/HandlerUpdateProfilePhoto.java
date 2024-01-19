package it.adrian.code.handler.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.database.Config;
import it.adrian.code.util.database.Querys;
import it.adrian.code.util.encryption.Encryption;
import it.adrian.code.util.json.JSON;
import it.adrian.code.util.service.MediaServerRoute;
import it.adrian.code.util.web.Requests;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class HandlerUpdateProfilePhoto implements HttpHandler {


    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));

        if (jwt == null || !Querys.validateJWT(jwt)) {
            Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.UNAUTHORIZED, "Token is invalid or expired. Cannot retrieve userData information to complete this operation.");
            return;
        }

        if (!t.getRequestMethod().equalsIgnoreCase("POST")) {
            Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.METHOD_NOT_ALLOWED, "Invalid request method. Expected POST.");
            return;
        }

        JsonNode session = Encryption.getSessionJSON(jwt);
        String currentUsername = session.get("username").asText();
        String userID = Objects.requireNonNull(Querys.findUserByUsername(currentUsername)).get("user_id");
        Headers headers = t.getRequestHeaders();
        String contentType = headers.getFirst("Content-Type");

        if (contentType != null && (contentType.startsWith("image/jpg") || contentType.startsWith("image/png"))) {

            String imageUrl = saveAvatar(t.getRequestBody(), userID, contentType.split("/")[1]);

            String responseJson;
            HashMap<String, String> values = new LinkedHashMap<>();
            values.put("profile_pic", imageUrl);

            boolean success = Querys.updateProfilePhoto(userID, imageUrl);

            if (success) {
                responseJson = JSON.generateJSON(values).toPrettyString();
            } else {
                responseJson = "{\"error\": \"Cannot update profile photo for user " + currentUsername + "\"}";
            }

            Headers responseHeaders = t.getResponseHeaders();
            responseHeaders.set("User-Agent", Config.CUSTOM_USER_AGENT);
            responseHeaders.set("Content-Type", "application/json");
            responseHeaders.set("Access-Control-Allow-Origin", Config.CORS_ORIGIN_PROTECTION);

            byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);

            t.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = t.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }


    private String saveAvatar(InputStream inputStream, String userId, String fileExtension) throws IOException {//remeber it isn't normal file but binary ^^
        String userDirectory = MediaServerRoute.getMEDIA_DIR() + File.separator + Encryption.encryptPassword(userId) + File.separator + Base64.getEncoder().encodeToString((Encryption.encryptPassword(userId + new Random().nextInt(10000))).getBytes()).replace("=", "");
        String fileName = generateFileName(userId, fileExtension);
        Path directory = Paths.get(userDirectory);
        Files.createDirectories(directory);
        Path filePath = directory.resolve(fileName);
        byte[] buffer = new byte[4096];
        int bytesRead;
        try (OutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return "http://localhost" + userDirectory.replace(MediaServerRoute.getMEDIA_DIR(), "").replace(File.separator, "/") + "/" + filePath.toFile().getName();
    }

    private String generateFileName(String userId, String fileExtension) {
        return Base64.getEncoder().encodeToString(("profile_pic_of_" + userId).getBytes()).replace("=", "") + "." + fileExtension;
    }
}