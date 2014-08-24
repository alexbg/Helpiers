package controllers;

import akka.actor.UntypedActor;
import models.Chat;
import models.ChatRequest;
import models.UserConnected;
import models.chat.ControlMessage;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by javi on 14/08/14.
 */
public class ChatController extends UntypedActor {

    public final static long TURN_TIME = 600000;
    public final static long ROUND_TIME = 2*TURN_TIME;

    private UserConnected hostUser;
    private UserConnected ownerUser;
    private Chat chat;
    private Timer timer;
    private TimerTaskMinuteEnd chatTimerTask;
    private TimerTaskTurnEnd ttte;
    private TimerTaskRoundEnd ttre;

    // Control flags
    /**
     * indica si está en ronda de puntuaciones (true) o no (false)
     */
    private boolean reputationFlag = false;

    /**
     * Flag del usuario invitado.
     * true = quiere terminar; false = no quiere terminar
     */
    private boolean conversationEndFlag_host = false;

    /**
     * Flag del usuario invitador.
     * true = quiere terminar; false = no quiere terminar
     */
    private boolean conversationEndFlag_owner = false;

    // ********************** Tareas que se ejecutarán cuando pase un tiempo determinado ****************************
    private class TimerTaskMinuteEnd extends TimerTask{
        @Override
        public void run() {
            notifyMinuteLess();
        }
    }
    private class TimerTaskTurnEnd extends TimerTask{
        @Override
        public void run() {
            notifyTurnEnd();
        }
    }
    private class TimerTaskRoundEnd extends TimerTask{
        @Override
        public void run() {
            notifyRoundEnd();
        }
    }
    // **************************************Fin tareas **********************************************

    /**
     * Constructor del ChatController. Gestiona el control de una conversación privada
     * @param host el usuario invitado
     * @param owner el usuario que envió la invitación
     * @param chatRequest modelo del ChatRequest de la invitación que dió paso al chat privado
     */
    public ChatController(UserConnected host, UserConnected owner, ChatRequest chatRequest){
        hostUser = host;
        ownerUser = owner;
        //creo un nuevo registro de chat, que se almacena en la BD
        chat = new Chat(host.getUser(), owner.getUser(), chatRequest, new Date());
        chat.save();
        //running timer task as daemon thread (will be killed automatically when ChatController finish his work)
        timer = new Timer(true);
        //execute the initial configuration
        initTimerConfig();
    }

    @Override
    public void onReceive(Object o) throws Exception {

        if(o instanceof ControlMessage) {
            ControlMessage message = (ControlMessage)o;
            if(isCorrectFormat(message)){
                switch (message.getType()){
                    case ControlMessage.restartRequest:

                        break;
                    case ControlMessage.finRequest:

                        break;
                    case ControlMessage.forcedFin:

                        break;
                    case ControlMessage.voluntaryTurnEnd:

                        break;
                    case ControlMessage.reputationPoints:
                        if(reputationFlag){
                            int points = Integer.parseInt(message.getInfo());
                            if(points>0 && points<=5){

                            }
                        }

                        break;
                }
            }
        }else {
            //unexpected message
                //do something...
        }
        
    }

    /**
     * comprueba que el formato del mensaje de control es correcto
     * @param msg
     * @return
     */
    private boolean isCorrectFormat(ControlMessage msg){
        if(msg.getUserMsOrg()!=null && msg.getType()>0 && msg.getType()<=5 && msg.getInfo()!=null){
            return true;
        }else return false;
    }

    /**
     * Initial Schedule configuration; Some tasks will be executed to control timing
     */
    private void initTimerConfig(){
        //inicio los TimerTask
        chatTimerTask = new TimerTaskMinuteEnd();
        ttte = new TimerTaskTurnEnd();
        ttre = new TimerTaskRoundEnd();
        //Defino la programación de los plazos
        //execute task every 60 seconds
        timer.scheduleAtFixedRate(chatTimerTask, 0, 60*1000);
        //execute task when user´s time is finished
        timer.scheduleAtFixedRate(ttte, 5000, TURN_TIME);
        //execute task when conversation´s time is finished
        timer.scheduleAtFixedRate(ttre, 5000, ROUND_TIME);
    }

    private boolean isAgreedEnd(){
        if(conversationEndFlag_host && conversationEndFlag_owner){
            //agreement reached -> exit
            return true;
        }else{
            //disagreement -> restart conversation
            return false;
        }
    }

    // ********************************* methods to send messages *****************************************

    /**
     * Se ejecutará cada 60 segundos. Este método lo llama la clase interna que lleva la cuenta del tiempo
     */
    private void notifyMinuteLess(){

    }

    /**
     * Se ejecutará cuando acabe un turno de un usuario. Enviará un mensaje a los participantes indicando tal hecho
     */
    private void notifyTurnEnd(){

    }
    /**
     * Se ejecutará cuando acabe una ronda(los dos usuarios han agotado sus turnos.
     * Enviará un mensaje a los participantes indicando tal hecho.
     */
    private void notifyRoundEnd(){

    }

    // *******************************************************************************************************
}
