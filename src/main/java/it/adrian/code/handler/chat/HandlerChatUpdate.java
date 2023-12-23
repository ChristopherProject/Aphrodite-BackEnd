package it.adrian.code.handler.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.database.Config;
import it.adrian.code.util.database.Querys;
import it.adrian.code.util.encryption.Encryption;
import it.adrian.code.util.web.Requests;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HandlerChatUpdate implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
        if (jwt == null || !Querys.validateJWT(jwt)) {
            Requests.sendUnauthorizedResponse(t, "invalid authorization to get this chat, no session userData token found.");
            return;
        }
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson;
        if (query.contains("chat_id") && !(queryParams.get("chat_id") == null || queryParams.get("chat_id").equals(""))) {
            JsonNode session = Encryption.getSessionJSON(jwt);
            String currentUsername = session.get("username").asText();
            String yourChatID = Objects.requireNonNull(Querys.findUserByUsername(currentUsername)).get("user_id");
            List<JsonNode> messages = Querys.getMessagesBetweenUsers(yourChatID, queryParams.get("chat_id"));
            assert messages != null;
            responseJson = messages.toString();
        } else {
            responseJson = "{\"error\": \"invalid chat_id isn't define in request\"}";
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