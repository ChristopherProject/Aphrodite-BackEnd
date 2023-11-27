package it.adrian.code.handler.chat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class HandlerChatUpdate implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {

    }

    //selected_chat = user_id del altro = chat_id
    //prende i messaggi in base al timestamp e l'id del tizio (chat_id)
    //e vede se nel json mongo é presente anche il tuo
    //li raggruppa in un jsonArray { "data": { messages: { message: {} } }  }
}
