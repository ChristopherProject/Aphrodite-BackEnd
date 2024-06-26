package it.adrian.code;

import com.sun.net.httpserver.HttpServer;
import it.adrian.code.handler.auth.HandlerLogin;
import it.adrian.code.handler.auth.HandlerRegistration;
import it.adrian.code.handler.chat.*;
import it.adrian.code.handler.profile.HandlerUpdateProfileBiography;
import it.adrian.code.handler.profile.HandlerUpdateProfilePhoto;
import it.adrian.code.handler.search.HandlerFindUserById;
import it.adrian.code.handler.search.HandlerFindUserByName;
import it.adrian.code.handler.user.HandlerChangePassword;
import it.adrian.code.handler.user.HandlerGetMyUserInformation;
import it.adrian.code.util.service.MediaServerRoute;
import it.adrian.code.util.system.Configuration;
import it.adrian.code.util.system.DynamicInstaller;
import it.adrian.code.util.web.Requests;
import lombok.Getter;

import java.net.InetSocketAddress;

public final class Main {

    @Getter
    private static final Configuration configuration = new Configuration();
    private static final String SERVER_NAME = configuration.getString("server_name");

    static {
        System.out.println("«" + SERVER_NAME + "» check and validate integrity..");
        if (!DynamicInstaller.checkMongoDB()) {
            DynamicInstaller.install();
        }
    }

    public static void main(String... args) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress(configuration.getInt("chat_server_port")), 0);
        server.createContext("/", Requests::corsSettings);
        System.out.println("«" + SERVER_NAME + "» init auth request..");
        server.createContext("/api/login", new HandlerLogin());//POST, http://localhost:419/api/login, { "username": "Adrian", "password": "JAC419" }
        server.createContext("/api/register", new HandlerRegistration());//POST, http://localhost:419/api/register, { "username": "Adrian", "password": "JAC419", "country_code": "+39", "number": "3519508016" }
        System.out.println("«" + SERVER_NAME + "» init user request..");
        server.createContext("/api/getMe", new HandlerGetMyUserInformation());//GET
        server.createContext("/api/getUserById", new HandlerFindUserById());//GET, http://localhost:419/api/getUserById?user_id=5288764
        server.createContext("/api/getUserByName", new HandlerFindUserByName());//GET, http://localhost:419/api/getUserByName?username=Adrian
        server.createContext("/api/changePassword", new HandlerChangePassword());//GET, http://localhost:419/api/changePassword?current_password=JAC419&new_password=JAC4192
        server.createContext("/api/updateProfilePhoto", new HandlerUpdateProfilePhoto());//GET, http://localhost:419/api/updateProfilePhoto?profile_pic_path=https://i.imgur.com/18ND4et.png
        server.createContext("/api/updateProfileBiography", new HandlerUpdateProfileBiography());//GET, http://localhost:419/api/updateProfileBiography?profile_biography=hey%20im%20using%20aphrodite
        System.out.println("«" + SERVER_NAME + "» init messaging request..");
        server.createContext("/api/sendMessage", new HandlerSendMessage());//POST, http://localhost:419/api/sendMessage?chat_id=6741019, { "message": "ciao, come stai?" }
        server.createContext("/api/editMessage", new HandlerEditMessage());//POST, http://localhost:419/api/editMessage?message_id=9824250, { "message_content": "messaggio cambiato!" }
        server.createContext("/api/deleteMessage", new HandlerDeleteMessage());//GET, http://localhost:419/api/deleteMessage?message_id=123123123
        server.createContext("/api/getUpdate", new HandlerChatUpdate());//GET, http://localhost:419/api/getUpdate?chat_id=6741019 (id del utente la cui chat con lui vuoi vedere, se metti il tuo non va ovviamente)
        server.createContext("/api/replyMessage", new HandlerReplyMessage());//GET, http://localhost:419/api/replyMessage?message_id=8994247&content=ciaoo%20tutto%20bene%20tu?
        server.setExecutor(null);
        server.start();
        MediaServerRoute.init(SERVER_NAME, configuration.getInt("media_server_port"));//hehe not using apache shit
        System.runFinalization();
        System.gc();
    }
}