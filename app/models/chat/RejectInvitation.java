package models.chat;

import models.UserConnected;

/**
 * Created by alex on 12/08/14.
 */
public class RejectInvitation {

    UserConnected user;

    public RejectInvitation(UserConnected user){

        this.user = user;

    }

    public UserConnected getUser() {
        return user;
    }

    public void setUser(UserConnected user) {
        this.user = user;
    }
}
