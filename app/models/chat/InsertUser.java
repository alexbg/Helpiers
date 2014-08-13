package models.chat;

import com.fasterxml.jackson.databind.JsonNode;
import models.UserConnected;
import play.mvc.WebSocket;
import org.jboss.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder;

/**
 * Created by alex on 12/08/14.
 */
public class InsertUser {

    UserConnected user;
    WebSocket.Out<JsonNode> out;
    WebSocket.In<JsonNode> in;

    InsertUser(UserConnected user,WebSocket.Out<JsonNode> out,WebSocket.In<JsonNode> in){

        this.user = user;
        this.out = out;
        this.in = in;

    }

}
