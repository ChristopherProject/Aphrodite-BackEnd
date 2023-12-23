package it.adrian.code.handler.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.database.Config;
import it.adrian.code.util.database.Querys;
import it.adrian.code.util.json.JSON;
import it.adrian.code.util.math.MathUtil;
import it.adrian.code.util.web.Requests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HandlerRegistration implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        if ("POST".equals(t.getRequestMethod())) {
            InputStream is = t.getRequestBody();
            StringBuilder requestBodyBuilder = new StringBuilder();
            int b;
            while ((b = is.read()) != -1) requestBodyBuilder.append((char) b);
            String requestBody = requestBodyBuilder.toString();
            if (requestBody.isEmpty()) {
                Requests.sendUnauthorizedResponse(t, "invalid request body is empty");
                return;
            }
            JsonNode jsonObject = JSON.parseStringToJson(requestBody);
            String responseJson;
            if (requestBody.contains("username") && !(jsonObject.get("username").asText() == null || jsonObject.get("username").asText().equals("")) && requestBody.contains("password") && !(jsonObject.get("password").asText() == null || jsonObject.get("password").asText().equals(""))) {
                boolean success = Querys.registerUser(jsonObject.get("username").asText(), jsonObject.get("password").asText());
                if (jsonObject.get("password").asText().length() < 8) {
                    Headers headers = t.getResponseHeaders();
                    headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
                    headers.set("Content-Type", "application/json");
                    t.sendResponseHeaders(400, 0);
                    try (OutputStream os = t.getResponseBody()) {
                        os.write("{\"error\": \"too short password, please select another one\"}".getBytes());
                    }
                    return;
                }
                //String tmp = MathUtil.encryptPassword(jsonObject.get("password").asText());
                //String data = "{\"state\": \"success\", \"username\": \"" + jsonObject.get("username").asText() + "\", \"hash_password\": \"" + tmp + "\"}";
                responseJson = success ? "{\"state\": \"success\"}" : "{\"state\": \"failed\", \"error\": \"this username is already taken\"}";
            } else {
                responseJson = "{\"error\": \"401 Unauthorized\"}";
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
            Requests.sendUnauthorizedResponse(t, "405 Method Not Allowed");
        }
    }
}