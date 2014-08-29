package models.chat;

import models.UserConnected;

/**
 * Created by alex on 25/08/14.
 */
public class CancelInvMsg {

    UserConnected user;

    public CancelInvMsg(UserConnected user){

        this.user = user;

    }

    public UserConnected getUser() {
        return user;
    }

    public void setUser(UserConnected user) {
        this.user = user;
    }
}
