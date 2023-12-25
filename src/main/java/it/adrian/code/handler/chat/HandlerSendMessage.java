package it.adrian.code.handler.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.database.Config;
import it.adrian.code.util.database.Querys;
import it.adrian.code.util.encryption.Encryption;
import it.adrian.code.util.json.JSON;
import it.adrian.code.util.math.MathUtil;
import it.adrian.code.util.web.Requests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class HandlerSendMessage implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson = null;
        if ("POST".equals(t.getRequestMethod())) {
            final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
            if (jwt == null || !Querys.validateJWT(jwt)) {
                Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.UNAUTHORIZED, "cant send message, please check your userData");
                return;
            }
            InputStream is = t.getRequestBody();
            StringBuilder requestBodyBuilder = new StringBuilder();
            int b;
            while ((b = is.read()) != -1) requestBodyBuilder.append((char) b);
            String requestBody = requestBodyBuilder.toString();
            if (requestBody.isEmpty()) {
                Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.BAD_REQUEST, "invalid request body is empty");
                return;
            }
            JsonNode jsonObject = JSON.parseStringToJson(requestBody);
            String message = jsonObject.get("message").asText();
            if (message != null && query.contains("chat_id") && !(queryParams.get("chat_id") == null || queryParams.get("chat_id").equals(""))) {
                JsonNode session = Encryption.getSessionJSON(jwt);
                String currentUsername = session.get("username").asText();
                String userID = Querys.findUserByUsername(currentUsername).get("user_id");
                if (userID != null) {
                    if (Querys.sendMessage(userID, queryParams.get("chat_id"), message, MathUtil.getUnixTimestampEpoch())) {
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
            headers.set("Access-Control-Allow-Origin", Config.CORS_ORIGIN_PROTECTION);
            t.sendResponseHeaders(200, responseJson.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(responseJson.getBytes());
            os.close();
        } else {
            Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.METHOD_NOT_ALLOWED, "invalid call method");
        }
    }
}