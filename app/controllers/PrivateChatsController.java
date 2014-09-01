package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.ChatRoom;
import models.User;
import models.UserConnected;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.chatPrueba;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by javi on 1/09/14.
 */
public class PrivateChatsController extends Controller {

    public static ArrayList<ChatCA> privateChats = new ArrayList<ChatCA>();

    public static Result showChatView(String username) {
        if(username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.ChatRoomController.index());
        }
        return ok(chatPrueba.render(username));
    }

    /**
     * Handle the chat websocket.
     */
    public static WebSocket<JsonNode> chat() {
        final User user = User.getUserByEmail(session("email"));
        return new WebSocket<JsonNode>() {
            // Called when the Websocket Handshake is done.
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){

                // Join the chat room.
                try {
                    getChatCAOfUser(user).join(getUCfromChatByUser(user), in, out);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    private static ChatCA getChatCAOfUser(User user){
        ChatCA chat = null;
        Iterator<ChatCA> it = privateChats.iterator();
        do{
            chat = it.next();
            if(chat.getHostUser().getUser().equals(user) || chat.getOwnerUser().getUser().equals(user))
                return chat;
        }while(it.hasNext());
        return null;
    }

    private static UserConnected getUCfromChatByUser(User user){
        ChatCA chat = getChatCAOfUser(user);
        if(chat != null){
            if(chat.getHostUser().getUser().equals(user))
                return chat.getHostUser();
            if(chat.getOwnerUser().getUser().equals(user))
                return chat.getOwnerUser();
        }
        return null;
    }

}
