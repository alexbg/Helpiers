package models.chat;

import models.User;
import models.UserConnected;

import java.util.Date;

/**
 * Created by alex on 12/08/14.
 */
public class ChatRequest {

    // Usuario que invita
    UserConnected user;
    //username del usuario que invita
    String guest;

    public ChatRequest(UserConnected user,String userName){

        this.user = user;
        this.guest = userName;

    }

    public ChatRequest(String userName){

        this.guest = userName;

    }

    public UserConnected getUser() {
        return user;
    }

    public void setUser(UserConnected user) {
        this.user = user;
    }

    public String getGuest() {
        return guest;
    }

    public void setGuest(String guest) {
        this.guest = guest;
    }
}
