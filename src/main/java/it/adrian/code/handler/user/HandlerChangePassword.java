package it.adrian.code.handler.user;

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

public class HandlerChangePassword implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {

        String jwt = extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
        if (jwt == null || !Querys.validateJWT(jwt)) {
            sendUnauthorizedResponse(t);
            return;
        }
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson;
        responseJson = null;
        if (query.contains("current_password") && !(queryParams.get("current_password") == null || queryParams.get("current_password").equals("")) && query.contains("new_password") && !(queryParams.get("new_password") == null || queryParams.get("new_password").equals(""))) {
            byte[] decodedBytes = Base64.getDecoder().decode(jwt.getBytes(StandardCharsets.UTF_8));
            String decodedJwt = new String(decodedBytes, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(decodedJwt);
            String currentUsername = jsonObject.getString("username");
            String currentHashPassword = MathUtil.encrypt(queryParams.get("current_password"));
            if (currentUsername != null) {
                if (MathUtil.verifyPassword(queryParams.get("current_password"), currentHashPassword)) {
                    Querys.changePassword(currentUsername, queryParams.get("current_password"), queryParams.get("new_password"));
                    responseJson = "{\"success\": \"password was changed for account " + currentUsername + "\"}";
                } else {
                    responseJson = "{\"error\": \"invalid login data\"}";
                }
            }
        } else {
            responseJson = "{\"error\": \"invalid or isn't define fields in request\"}";
        }
        Headers headers = t.getResponseHeaders();
        headers.set("User-Agent", Config.CUSTOM_USER_AGENT);
        headers.set("Content-Type", "application/json");
        assert responseJson != null;
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