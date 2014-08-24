package models.chat;

import models.UserConnected;

/**
 * Created by alex on 12/08/14.
 */
public class ChatRequest {

    UserConnected targetUser;

    ChatRequest(UserConnected user){

        this.targetUser = user;

    }

}
