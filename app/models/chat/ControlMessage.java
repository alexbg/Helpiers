package models.chat;

import models.UserConnected;

/**
 * Created by javi on 19/08/14. Clase que contiene información de control de un chat privado
 */
public class ControlMessage {

    // *********************************** Control messages types *********************************************
    public final static int restartRequest = 1;
    public final static int finRequest = 2;
    public final static int forcedFin = 3;
    public final static int voluntaryTurnEnd = 4;
    public final static int reputationPoints = 5;
    // *********************************************************************************************************

    /**
     * usuario (UserConnected) que envió el mensaje
     */
    public UserConnected userMsOrg;
    /**
     * Tipo de mensaje de control
     */
    public int type;
    /**
     * Información añadida
     */
    public String info;

    public ControlMessage(UserConnected userMsOrg, int type, String info) {
        this.userMsOrg = userMsOrg;
        this.type = type;
        this.info = info;
    }
    // ******************************* GETTERS Y SETTERS *****************************************************
    public UserConnected getUserMsOrg() {
        return userMsOrg;
    }
    public void setUserMsOrg(UserConnected userMsOrg) {
        this.userMsOrg = userMsOrg;
    }
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    public String getInfo() {
        return info;
    }
    public void setInfo(String info) {
        this.info = info;
    }
}
