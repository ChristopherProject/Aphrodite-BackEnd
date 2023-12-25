package it.adrian.code.handler.chat;

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
import java.util.Objects;

public class HandlerDeleteMessage implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson = null;
        final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
        if (jwt == null || !Querys.validateJWT(jwt)) {
            Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.UNAUTHORIZED, "cannot delete current message invalid or expired token.");
            return;
        }
        if (query.contains("message_id") && !(queryParams.get("message_id") == null || queryParams.get("message_id").equals(""))) {
            JsonNode session = Encryption.getSessionJSON(jwt);
            String currentUsername = session.get("username").asText();
            String userID = Objects.requireNonNull(Querys.findUserByUsername(currentUsername)).get("user_id");
            if (Objects.requireNonNull(Querys.getMessageByID(queryParams.get("message_id"))).get("from").equals(userID)) {
                if (Querys.deleteMessage(queryParams.get("message_id"))) {
                    responseJson = "{\"success\": \"message deleted correctly\"}";
                } else {
                    responseJson = "{\"error\": \"cannot deleted message invalid request\"}";
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
    }
}