package it.adrian.code.handler.search;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.Config;
import it.adrian.code.util.Querys;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class HandlerFindUserByName implements HttpHandler {


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
        if (query.contains("username") && !(queryParams.get("username") == null || queryParams.get("username").equals(""))) {
            if (Querys.findUserByUsername(queryParams.get("username")) != null) {
                responseJson = "{\"user_id\": \"" + Querys.findUserByUsername(queryParams.get("username")) + "\", \"username\": \"" + queryParams.get("username") + "\" }";
            } else {
                responseJson = "{\"error\": \"user not found invalid username\"}";
            }
        } else {
            responseJson = "{\"error\": \"username isn't define in request\"}";
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
