package it.adrian.code.handler.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.database.Config;
import it.adrian.code.util.database.Querys;
import it.adrian.code.util.encryption.Encryption;
import it.adrian.code.util.json.JSON;
import it.adrian.code.util.system.upload.MultipartFile;
import it.adrian.code.util.system.upload.MultipartStreamReader;
import it.adrian.code.util.system.upload.apache.ApacheUtil;
import it.adrian.code.util.web.Requests;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

public class HandlerUpdateProfilePhoto implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
        if (jwt == null || !Querys.validateJWT(jwt)) {
            Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.UNAUTHORIZED, "token is invalid or expired cant retrieve userData information to complete this operation.");return;
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
        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            String boundary = contentType.split(";")[1].trim().split("=")[1];
            MultipartStreamReader multipartReader = new MultipartStreamReader(t.getRequestBody(), boundary);
            System.out.println(t.getRequestBody());//sun.net.httpserver.FixedLengthInputStream@3358cb66
            MultipartFile file = multipartReader.readNextPart();
            if (file != null) {
                String contentTypeOfFile = file.getContentType();
                String extension = "." + contentTypeOfFile.split("/")[1].toLowerCase();
                boolean isValidImage = (contentTypeOfFile.equals("image/png") || contentTypeOfFile.equals("image/jpeg"));
                if (isValidImage) {
                    String imageUrl = ApacheUtil.saveAvatar(file.getInputStream(), userID, extension);
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

                    t.sendResponseHeaders(200, responseJson.getBytes().length);
                    OutputStream os = t.getResponseBody();
                    os.write(responseJson.getBytes());
                    os.close();
                }
            }
        }
    }
}