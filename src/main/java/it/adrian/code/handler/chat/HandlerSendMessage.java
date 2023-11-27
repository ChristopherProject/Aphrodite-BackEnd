package it.adrian.code.handler.chat;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.Config;
import it.adrian.code.util.MathUtil;
import it.adrian.code.util.Querys;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class HandlerSendMessage implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        String jwt = extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
        if (jwt == null || !Querys.validateJWT(jwt)) {
            sendUnauthorizedResponse(t);
            return;
        }
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson = null;
        if (query.contains("chat_id") && !(queryParams.get("chat_id") == null || queryParams.get("chat_id").equals("")) && query.contains("message") && !(queryParams.get("message") == null || queryParams.get("message").equals(""))) {
            byte[] decodedBytes = Base64.getDecoder().decode(jwt.getBytes(StandardCharsets.UTF_8));
            String decodedJwt = new String(decodedBytes, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(decodedJwt);
            String currentUsername = jsonObject.getString("username");
            String userID = Querys.findUserByUsername(currentUsername).get("user_id");
            if (userID != null) {
                if (Querys.sendMessage(userID, queryParams.get("chat_id"), queryParams.get("message"), MathUtil.getUnixTimestampEpoch())) {
                    responseJson = "{\"success\": \"message sent to " + Querys.findUserById(queryParams.get("chat_id")).get("username") + "\"}";
                } else {
                    responseJson = "{\"error\": \"cannot sent message to this user please check your request data\"}";
                }
            }
        } else {
            responseJson = "{\"error\": \"invalid send message request, check your parameters\"}";
        }
        Headers headers = t.getResponseHeaders();
        headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
        headers.set("Content-Type", "application/json");
        t.sendResponseHeaders(200, responseJson.getBytes().length);
        OutputStream os = t.getResponseBody();
        os.write(responseJson.getBytes());
        os.close();
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