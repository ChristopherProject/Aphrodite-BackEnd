package it.adrian.code.handler.profile;

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
import java.util.Map;

public class HandlerUpdateProfilePhoto implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
        if (jwt == null || !Querys.validateJWT(jwt)) {
            Requests.sendUnauthorizedResponse(t, "token is invalid or expired cant retrieve userData information to complete this operation.");
            return;
        }
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson;
        if (query.contains("profile_pic_path") && !(queryParams.get("profile_pic_path") == null || queryParams.get("profile_pic_path").equals(""))) {
            JSONObject session = Encryption.getSessionJSON(jwt);
            String currentUsername = session.getString("username");
            if (Querys.updateProfilePhoto(Querys.findUserByUsername(currentUsername).get("user_id"), queryParams.get("profile_pic_path"))) {
                responseJson = "{\"success\": \"profile photo was update for account " + currentUsername + "\"}";
            } else {
                responseJson = "{\"error\": \"cannot update profile photo for user " + currentUsername + "\"}";
            }
        } else {
            responseJson = "{\"error\": \"profile photo path isn't define in request\"}";
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