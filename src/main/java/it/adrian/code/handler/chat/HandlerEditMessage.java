package it.adrian.code.handler.chat;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.database.Config;
import it.adrian.code.util.database.Querys;
import it.adrian.code.util.encryption.Encryption;
import it.adrian.code.util.web.Requests;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

public class HandlerEditMessage implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson = null;
        if ("POST".equals(t.getRequestMethod())) {
            final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
            if (jwt == null || !Querys.validateJWT(jwt)) {
                Requests.sendUnauthorizedResponse(t, "cannot edit message invalid or expired token.");
                return;
            }
            InputStream is = t.getRequestBody();
            StringBuilder requestBodyBuilder = new StringBuilder();
            int b;
            while ((b = is.read()) != -1) requestBodyBuilder.append((char) b);
            String requestBody = requestBodyBuilder.toString();
            if (requestBody.isEmpty()) {
                Requests.sendUnauthorizedResponse(t, "invalid request body is empty");
                return;
            }
            JSONObject jsonObject = new JSONObject(requestBody);
            String editedMessage = jsonObject.getString("message_content");
            if (editedMessage != null && query.contains("message_id") && !(queryParams.get("message_id") == null || queryParams.get("message_id").equals(""))) {
                JSONObject session = Encryption.getSessionJSON(jwt);
                String currentUsername = session.getString("username");
                String userID = Objects.requireNonNull(Querys.findUserByUsername(currentUsername)).get("user_id");
                if (Objects.requireNonNull(Querys.getMessageByID(queryParams.get("message_id"))).get("from").equals(userID)) {
                    if (Querys.updateMessage(queryParams.get("message_id"), queryParams.get("message_content"))) {
                        responseJson = "{\"success\": \"message updated correctly\"}";
                    } else {
                        responseJson = "{\"error\": \"cannot edit message invalid request\"}";
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
        } else {
            Requests.sendUnauthorizedResponse(t, "405 Method Not Allowed");
        }
    }
}