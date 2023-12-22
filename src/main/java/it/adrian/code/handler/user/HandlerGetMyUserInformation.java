package it.adrian.code.handler.user;

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

public class HandlerGetMyUserInformation implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
        if (jwt == null || !Querys.validateJWT(jwt)) {
            Requests.sendUnauthorizedResponse(t, "impossible find userData without valid jwt.");
            return;
        }
        String responseJson;
        JSONObject session = Encryption.getSessionJSON(jwt);
        String currentUsername = session.getString("username");
        JSONObject jsonObject = new JSONObject();
        if (currentUsername != null) {
            jsonObject.put("_id", Querys.findUserByUsername(currentUsername).get("user_id"));
            responseJson = jsonObject.toString();
        } else {
            responseJson = "{\"error\": \"invalid token or expired\"}";
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