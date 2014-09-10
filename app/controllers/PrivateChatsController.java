package controllers;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.actors.WaitingRoomCA;
import models.ChatRoom;
import models.User;
import models.UserConnected;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.chatPrueba;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by javi on 1/09/14.
 */
public class PrivateChatsController extends Controller {

    public static ArrayList<ChatCA> privateChats = new ArrayList<ChatCA>();//no vale
    public static Map<UserConnected, ActorRef> privateChatsMap;

    public static Result showChatView() {
        return ok(chatPrueba.render(User.getUserByEmail(session("email")).getUsername()));
    }

    /**
     * Handle the chat websocket.
     */
    public static WebSocket<JsonNode> chat() {
        final User user = User.getUserByEmail(session("email"));
        final UserConnected userConnected = null;//sacarlo de la BDD
        return new WebSocket<JsonNode>() {
            // Called when the Websocket Handshake is done.
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){

                // Join the chat room.
                try {
                    privateChatsMap.get(userConnected).tell(new ChatCA.Join(userConnected, out), null);



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
