package it.adrian.code.handler.auth;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.Config;
import it.adrian.code.util.MathUtil;
import it.adrian.code.util.Querys;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HandlerRegistration implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        if ("POST".equals(t.getRequestMethod())) {
            InputStream is = t.getRequestBody();
            StringBuilder requestBodyBuilder = new StringBuilder();
            int b;
            while ((b = is.read()) != -1) requestBodyBuilder.append((char) b);
            String requestBody = requestBodyBuilder.toString();
            if(requestBody.isEmpty()){
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
            String responseJson;
            if (requestBody.contains("username") && !(jsonObject.getString("username") == null || jsonObject.getString("username").equals("")) && requestBody.contains("password") && !(jsonObject.getString("password") == null || jsonObject.getString("password").equals(""))) {
                boolean success = Querys.registerUser(jsonObject.getString("username"), jsonObject.getString("password"));
                if(jsonObject.getString("password").length() <  8){
                    Headers headers = t.getResponseHeaders();
                    headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
                    headers.set("Content-Type", "application/json");
                    t.sendResponseHeaders(400, 0);
                    try (OutputStream os = t.getResponseBody()) {
                        os.write("{\"error\": \"too short password, please select another one\"}".getBytes());
                    }
                    return;
                }
                String tmp = MathUtil.encrypt(jsonObject.getString("password"));
                String data = "{\"state\": \"success\", \"username\": \"" + jsonObject.getString("username") + "\", \"hash_password\": \"" + tmp + "\"}";
                responseJson = success ? data : "{\"state\": \"failed\", \"error\": \"this username is already taken\"}";
            } else {
                responseJson = "{\"error\": \"401 Unauthorized\"}";
            }
            Headers headers = t.getResponseHeaders();
            headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
            headers.set("Content-Type", "application/json");
            t.sendResponseHeaders(200, responseJson.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(responseJson.getBytes());
            os.close();
        } else {
            sendUnauthorizedResponse(t);
        }
    }

    private void sendUnauthorizedResponse(HttpExchange t) throws IOException {
        Headers headers = t.getResponseHeaders();
        headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
        headers.set("Content-Type", "application/json");
        t.sendResponseHeaders(401, 0);
        try (OutputStream os = t.getResponseBody()) {
            os.write("{\"error\": \"405 Method Not Allowed\"}".getBytes());
        }
    }
}