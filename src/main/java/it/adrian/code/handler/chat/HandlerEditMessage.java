package it.adrian.code.handler.chat;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.Config;
import it.adrian.code.util.Querys;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class HandlerEditMessage implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson = null;
        if ("POST".equals(t.getRequestMethod())) {
            String jwt = extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
            if (jwt == null || !Querys.validateJWT(jwt)) {
                sendUnauthorizedResponse(t);
                return;
            }
            InputStream is = t.getRequestBody();
            StringBuilder requestBodyBuilder = new StringBuilder();
            int b;
            while ((b = is.read()) != -1) requestBodyBuilder.append((char) b);
            String requestBody = requestBodyBuilder.toString();
            if (requestBody.isEmpty()) {
                Headers headers = t.getResponseHeaders();
                headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
                headers.set("Content-Type", "application/json");
                t.sendResponseHeaders(404, 0);
                try (OutputStream os = t.getResponseBody()) {
                    os.write("{\"error\": \"invalid request body is empty\"}".getBytes());
                }
                return;
            }
            JSONObject jsonObject = new JSONObject(requestBody);
            String editedMessage = jsonObject.getString("message_content");
            if (editedMessage != null && query.contains("message_id") && !(queryParams.get("message_id") == null || queryParams.get("message_id").equals(""))) {
                byte[] decodedBytes = Base64.getDecoder().decode(jwt.getBytes(StandardCharsets.UTF_8));
                String decodedJwt = new String(decodedBytes, StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(decodedJwt);
                String currentUsername = json.getString("username");
                String userID = Querys.findUserByUsername(currentUsername).get("user_id");
                if (Querys.getMessage(queryParams.get("message_id")).get("from").equals(userID)) {
                    if (Querys.updateMessage(queryParams.get("message_id"), queryParams.get("message_content"))) {
                        responseJson = "{\"success\": \"message updated correctly\"}";
                    } else {
                        responseJson = "{\"error\": \"cannot edit message invalid request\"}";
                    }
                }
            } else {
                responseJson = "{\"error\": \"invalid request, check your parameters\"}";
            }
            Headers headers = t.getResponseHeaders();
            headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
            headers.set("Content-Type", "application/json");
            t.sendResponseHeaders(200, responseJson.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(responseJson.getBytes());
            os.close();
        } else {
            Headers headers = t.getResponseHeaders();
            headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
            headers.set("Content-Type", "application/json");
            t.sendResponseHeaders(405, 0);
            try (OutputStream os = t.getResponseBody()) {
                os.write("{\"error\": \"405 Method Not Allowed\"}".getBytes());
            }
        }
    }

    private void sendUnauthorizedResponse(HttpExchange t) throws IOException {
        Headers headers = t.getResponseHeaders();
        headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
        headers.set("Content-Type", "application/json");
        t.sendResponseHeaders(401, 0);
        try (OutputStream os = t.getResponseBody()) {
            os.write("{\"error\": \"401 Unauthorized\"}".getBytes());
        }
    }

    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}