package it.adrian.code.util.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import it.adrian.code.util.database.Config;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.IntStream;

public class Requests {

    public static void sendUnauthorizedResponse(HttpExchange t, RESPONSES responseType, String message) throws IOException {
        Headers headers = t.getResponseHeaders();
        headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
        headers.set("Content-Type", "application/json");
        t.sendResponseHeaders(responseType.getResponseCode(), 0);
        String firstPart = responseType.name().charAt(0) + responseType.name().substring(1).toLowerCase().replace("_", " ");
        StringBuilder stringBuilder = new StringBuilder();
        if (firstPart.contains(" ")) {
            String[] parts = firstPart.split(" ");
            stringBuilder.append(parts[0]);
            IntStream.range(1, parts.length).forEachOrdered(i -> {
                stringBuilder.append(" ").append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1).toLowerCase());
            });
        }
        String response = "{\"error\": \"" + responseType.getResponseCode() + " " + stringBuilder + "\", \"message\": \"" + message + "\"}";
        try (OutputStream os = t.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    public static String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    public static void corsSettings(HttpExchange t) throws IOException {
        if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            Headers headers = t.getResponseHeaders();
            headers.set("Access-Control-Allow-Origin", Config.CORS_ORIGIN_PROTECTION);
            headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization, User-Agent");
            t.sendResponseHeaders(200, -1);
        }
    }

    public enum RESPONSES {

        BAD_REQUEST(400),
        UNAUTHORIZED(401),
        METHOD_NOT_ALLOWED(405),
        INTERNAL_SERVER_ERROR(500);

        private final int code;

        RESPONSES(int code) {
            this.code = code;
        }

        public int getResponseCode() {
            return code;
        }
    }
}