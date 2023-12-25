package it.adrian.code.handler.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.database.Config;
import it.adrian.code.util.database.Querys;
import it.adrian.code.util.encryption.Encryption;
import it.adrian.code.util.json.JSON;
import it.adrian.code.util.web.Requests;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class HandlerEditMessage implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson = null;
        if ("POST".equals(t.getRequestMethod())) {
            final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
            if (jwt == null || !Querys.validateJWT(jwt)) {
                Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.UNAUTHORIZED, "cannot edit message invalid or expired token.");
                return;
            }
            InputStream is = t.getRequestBody();
            StringBuilder requestBodyBuilder = new StringBuilder();
            int b;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                while ((b = reader.read()) != -1) {
                    requestBodyBuilder.append((char) b);
                }
            } catch (IOException e) {
                Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.INTERNAL_SERVER_ERROR, "invalid request cant retrieve message");
                return;
            }
            String requestBody = requestBodyBuilder.toString();
            if (requestBody.isEmpty()) {
                Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.BAD_REQUEST, "invalid request body is empty");
                return;
            }
            JsonNode jsonObject = JSON.parseStringToJson(requestBody);
            String editedMessage = jsonObject.get("message_content").asText();
            if (editedMessage != null && query.contains("message_id") && !(queryParams.get("message_id") == null || queryParams.get("message_id").equals(""))) {
                JsonNode session = Encryption.getSessionJSON(jwt);
                String currentUsername = session.get("username").asText();
                String userID = Objects.requireNonNull(Querys.findUserByUsername(currentUsername)).get("user_id");
                if (Objects.requireNonNull(Querys.getMessageByID(queryParams.get("message_id"))).get("from").equals(userID)) {
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
            headers.set("Access-Control-Allow-Origin", Config.CORS_ORIGIN_PROTECTION);
            assert responseJson != null;
            t.sendResponseHeaders(200, responseJson.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(responseJson.getBytes());
            os.close();
        } else {
            Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.METHOD_NOT_ALLOWED, "invalid call method");
        }
    }
}