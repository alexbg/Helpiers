package models.chat;

import models.UserConnected;
import play.mvc.WebSocket;

/**
 * Created by alex on 21/08/14.
 */
public class InfoUser {

    private String userName;
    private WebSocket.Out out;

    public InfoUser(String userName,WebSocket.Out out) {

        this.userName = userName;
        this.out = out;

    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public WebSocket.Out getOut() {
        return out;
    }

    public void setOut(WebSocket.Out out) {
        this.out = out;
    }
}
