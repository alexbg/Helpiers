package controllers;

import akka.actor.UntypedActor;
import models.Chat;
import models.ChatRequest;
import models.UserConnected;

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
    /**
     * Se ejecutará cada 60 segundos. Este método lo llama la clase interna que lleva la cuenta del tiempo
     */
    private void notifyMinuteLess(){

    }

    private void notifyTurnEnd(){

    }

    private void notifyRoundEnd(){

    }
}
