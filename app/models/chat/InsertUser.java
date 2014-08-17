package models.chat;

import com.fasterxml.jackson.databind.JsonNode;
import models.UserConnected;
import play.mvc.WebSocket;
import org.jboss.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder;

/**
 * Created by alex on 12/08/14.
 */
public class InsertUser {

    private UserConnected userConnected;
    private WebSocket.Out<JsonNode> out;
    private WebSocket.In<JsonNode> in;

    public InsertUser(UserConnected user, WebSocket.Out<JsonNode> out, WebSocket.In<JsonNode> in){

        this.userConnected = user;
        this.out = out;
        this.in = in;

    }

    public WebSocket.In<JsonNode> getIn() {
        return in;
    }

    public void setIn(WebSocket.In<JsonNode> in) {
        this.in = in;
    }

    public WebSocket.Out<JsonNode> getOut() {
        return out;
    }

    public void setOut(WebSocket.Out<JsonNode> out) {
        this.out = out;
    }

    public UserConnected getUserConnected() {
        return userConnected;
    }

    public void setUser(UserConnected user) {
        this.userConnected = user;
    }
}
