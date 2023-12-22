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

public class HandlerFindUserByName implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        Requests.corsSettings(t);
        final String jwt = Requests.extractTokenFromHeader(t.getRequestHeaders().getFirst("Authorization"));
        if (jwt == null || !Querys.validateJWT(jwt)) {
            Requests.sendUnauthorizedResponse(t, "cant find user because jwt is expired or invalid.");
            return;
        }
        String query = t.getRequestURI().getQuery();
        Map<String, String> queryParams = Querys.parseURLQuery(query);
        String responseJson;
        if (query.contains("username") && !(queryParams.get("username") == null || queryParams.get("username").equals(""))) {
            if (Querys.findUserByUsername(queryParams.get("username")) != null) {
                responseJson = "{\"user_id\": \"" + Querys.findUserByUsername(queryParams.get("username")).get("user_id") + "\"," + " \"username\": \"" + Querys.findUserByUsername(queryParams.get("username")).get("username") + "\", \"biography\": \"" + Querys.findUserByUsername(queryParams.get("username")).get("biography") + "\",\"profile_pic\": \"" + Querys.findUserByUsername(queryParams.get("username")).get("profile_pic") + "\" }";
            } else {
                responseJson = "{\"error\": \"user not found invalid username\"}";
            }
        } else {
            responseJson = "{\"error\": \"username isn't define in request\"}";
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