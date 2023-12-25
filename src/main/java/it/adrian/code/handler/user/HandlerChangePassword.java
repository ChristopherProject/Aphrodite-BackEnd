package it.adrian.code.handler.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.database.Config;
import it.adrian.code.util.database.Querys;
import it.adrian.code.util.encryption.Encryption;
import it.adrian.code.util.web.Requests;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class HandlerChangePassword implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
        if (jwt == null || !Querys.validateJWT(jwt)) {
            Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.UNAUTHORIZED, "cannot change password for current user because jwt is expired or invalid.");
            return;
        }
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson = null;
        if (query.contains("current_password") && !(queryParams.get("current_password") == null || queryParams.get("current_password").equals("")) && query.contains("new_password") && !(queryParams.get("new_password") == null || queryParams.get("new_password").equals(""))) {
            JsonNode session = Encryption.getSessionJSON(jwt);
            String currentUsername = session.get("username").asText();
            String currentHashPassword = Encryption.encryptPassword(queryParams.get("current_password"));
            if (currentUsername != null) {
                if (Encryption.verifyPassword(queryParams.get("current_password"), currentHashPassword)) {
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
        headers.set("Access-Control-Allow-Origin", Config.CORS_ORIGIN_PROTECTION);
        t.sendResponseHeaders(200, responseJson.getBytes().length);
        OutputStream os = t.getResponseBody();
        os.write(responseJson.getBytes());
        os.close();
    }
}