package it.adrian.code.util.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import it.adrian.code.util.database.Config;

import java.io.IOException;
import java.io.OutputStream;

public class Requests {

    public static void sendUnauthorizedResponse(HttpExchange t, String message) throws IOException {
        Headers headers = t.getResponseHeaders();
        headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
        headers.set("Content-Type", "application/json");
        t.sendResponseHeaders(401, 0);
        String response = "{\"error\": \"401 Unauthorized\", \"message\": \"" + message + "\"}";
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
}