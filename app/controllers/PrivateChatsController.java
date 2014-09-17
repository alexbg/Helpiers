package controllers;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
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

    public static Map<UserConnected, ActorRef> privateChatsMap;

    public static Result showChatView() {
        return ok(chatPrueba.render(User.getUserByEmail(session("email")).getUsername()));
        //return ok();
    }

    public static Result chatJs(String username) {
        return ok(views.js.chatRoom.render(username));
    }

    /**
     * Handle the chat websocket.
     */
    public static WebSocket<JsonNode> chat() {
        System.out.println("PrivateChatsController: entra en Chat()");
        final User user = User.getUserByEmail(session("email"));
        final UserConnected userConnected = user.getUserConnected();
        if(userConnected!=null){
            System.out.println("PrivateChatsController tiene que hacer el Join");
            return new WebSocket<JsonNode>() {
                // Called when the Websocket Handshake is done.
                public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                    // Join the chat room.
                    try {
                        privateChatsMap.get(userConnected).tell(new ChatCA.Join(userConnected, out, in), privateChatsMap.get(userConnected));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
        }else{
            System.out.println("PrivateChatsController el userConnected es null");
            return null;
        }
    }
}
