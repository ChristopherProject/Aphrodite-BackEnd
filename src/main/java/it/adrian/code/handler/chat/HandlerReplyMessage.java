package it.adrian.code.handler.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.database.Config;
import it.adrian.code.util.database.Querys;
import it.adrian.code.util.encryption.Encryption;
import it.adrian.code.util.math.MathUtil;
import it.adrian.code.util.web.Requests;
import org.bson.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

public class HandlerReplyMessage implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
        if (jwt == null || !Querys.validateJWT(jwt)) {
            Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.UNAUTHORIZED, "cannot reply to current message invalid or expired token.");
            return;
        }
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson = null;
        if (query.contains("message_id") && !(queryParams.get("message_id") == null || queryParams.get("message_id").equals("")) || query.contains("content") && !(queryParams.get("content") == null || queryParams.get("content").equals(""))) {
            JsonNode session = Encryption.getSessionJSON(jwt);
            String currentUsername = session.get("username").asText();
            String yourChatID = Objects.requireNonNull(Querys.findUserByUsername(currentUsername)).get("user_id");
            if (!yourChatID.equals("")) {
                boolean isOk = Querys.replyToMessage(yourChatID, queryParams.get("message_id"), queryParams.get("content").replace("%20", " "), MathUtil.getUnixTimestampEpoch());
                if (isOk) {
                    java.util.List<Document> replies = Querys.getRepliesByMessageID(queryParams.get("message_id"));
                    assert replies != null;
                    responseJson = "{ " + "\"state\": \"success\", \"reply_id\": \"" + replies.get(replies.toArray().length - 1).get("reply_id") + "\", \"reply_content\": \"" + queryParams.get("content").replace("%20", " ") + "\" }";
                } else {
                    responseJson = "{\"state\": \"failed\", \"error\": \"invalid request, check your parameters\"}";
                }
            }
        } else {
            responseJson = "{\"error\": \"invalid chat_id isn't define in request\"}";
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