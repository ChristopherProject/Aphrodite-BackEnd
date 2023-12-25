package it.adrian.code.handler.search;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import it.adrian.code.util.database.Config;
import it.adrian.code.util.database.Querys;
import it.adrian.code.util.web.Requests;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class HandlerFindUserById implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
        if (jwt == null || !Querys.validateJWT(jwt)) {
            Requests.sendUnauthorizedResponse(t, Requests.RESPONSES.UNAUTHORIZED, "cant find user because jwt is expired or invalid.");
            return;
        }
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson;
        if (query.contains("user_id") && !(queryParams.get("user_id") == null || queryParams.get("user_id").equals(""))) {
            if (Querys.findUserById(queryParams.get("user_id")) != null) {
                responseJson = "{\"user_id\": \"" + Querys.findUserById(queryParams.get("user_id")).get("user_id") + "\"," + " \"username\": \"" + Querys.findUserById(queryParams.get("user_id")).get("username") + "\", \"biography\": \"" + Querys.findUserById(queryParams.get("user_id")).get("biography") + "\",\"profile_pic\": \"" + Querys.findUserById(queryParams.get("user_id")).get("profile_pic") + "\" }";
            } else {
                responseJson = "{\"error\": \"user not found invalid user_id\"}";
            }
        } else {
            responseJson = "{\"error\": \"user_id isn't define in request\"}";
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