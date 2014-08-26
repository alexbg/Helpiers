package controllers;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Chat;
import models.ChatRequest;
import models.Robot;
import models.UserConnected;
import models.chat.ControlMessage;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.*;

import static akka.pattern.Patterns.ask;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by javi on 14/08/14.
 */
public class ChatCA extends UntypedActor {

    public final static long TURN_TIME = 600000;

    public final static String CONTROLMSG = "control";
    public final static String FINACK = "finack";
    public final static String FINNACK = "finnack";

    private UserConnected hostUser;
    private UserConnected ownerUser;
    private Chat chat;
    private Timer timer;
    private TimerTaskMinuteEnd ttme;
    private TimerTaskTurnEnd ttte;
    private NegotiationEndState ownerNES;
    private NegotiationEndState hostNES;

    private Map<UserConnected, WebSocket.Out<JsonNode>> participants = new HashMap<UserConnected, WebSocket.Out<JsonNode>>();

    // *****************************************+Control flags*******************************************
    /**
     * indica si está en ronda de puntuaciones (true) o no (false)
     */
    private boolean reputationFlag = false;//se activa en resolveNegotiationEnd()

    /**
     * Estado de la negociación del final de la conversación por parte del usuario invitado.
     * NOTANSWERED = aún no ha contestado; ENDREQUEST = quiere terminar; RESTARTREQUEST = no quiere terminar
     */
    private enum NegotiationEndState {
        NOTANSWERED,
        ENDREQUEST,
        RESTARTREQUEST,
    }

    // *****************************************Control flags FIN*******************************************

    //****************************************** Contadores para el tiempo *********************************
    private int minutes;
    private int turns;

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

    /************************************** PARTE EXPERIMENTAL **************************************
     * ya que este clase controlará todos los mensajes de texto y de control de la conversación, pasa a ser
     * muy similar a ChatRoom del ejemplo. Por lo tanto, voy a intentar meter parte del código aquí, para que
     * funcione el envío de mensajes de texto
     */

    // Default room.
    static ActorRef privateRoom = Akka.system().actorOf(Props.create(ChatCA.class));

    // Create a Robot, just for fun.
    static {
        new Robot(privateRoom);
    }

    /**
     * Join the default room.
     */
    public static void join(final UserConnected user, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception{
        // Send the Join message to the room
        String result = (String) Await.result(ask(privateRoom, new Join(user, out), 1000), Duration.create(1, SECONDS));
        if("OK".equals(result)) {
            // For each event received on the socket,
            in.onMessage(new F.Callback<JsonNode>() {
                public void invoke(JsonNode event) {
                    //si es un mensaje de texto (habla)
                    if(event.get("type").asText().equals("talk")){
                        privateRoom.tell(new Talk(user.getUser().getUsername(), event.get("info").asText()), null);
                    }


                }
            });
            // When the socket is closed.
            in.onClose(new F.Callback0() {
                public void invoke() {
                    // Send a Quit message to the room.
                    privateRoom.tell(new Quit(user), null);
                }
            });
        } else {
            // Cannot connect, create a Json error.
            ObjectNode error = Json.newObject();
            error.put("error", result);
            // Send the error to the socket.
            out.write(error);
        }
    }
    // Members of this room.
    private Map<UserConnected, WebSocket.Out<JsonNode>> members = new HashMap<UserConnected, WebSocket.Out<JsonNode>>();

    public void onReceive(Object message) throws Exception {
        if(message instanceof Talk)  {
            // Received a Talk message
            Talk talk = (Talk)message;
            //envío mensaje de tipo talk (talk), de texto(text) del usuario (username), y su texto (text)
            notifyAll("talk", "text", talk.username, talk.text);

        }else if (message instanceof Join) {
            // Received a Join message
            Join join = (Join)message;
            if(members.size()<2){//controlo que como máximo entren dos usuarios en el chat privado.
                members.put(join.user, join.channel);
                notifyAll("control", "join", join.user.getUser().getUsername(), "has entered the room");
                getSender().tell("OK", getSelf());
                /**
                 * SI no vale el constructor normal, hay que añadir a uno de los participantes como owner y otro
                 * como host. El primero en entrar en la sala (si el map tiene tamaño 1) es el usuario owner.
                 */
                if(members.size() == 1){
                    ownerUser = join.user;
                    ownerNES = NegotiationEndState.NOTANSWERED;
                }else if(members.size() == 2){//si es el segundo del map, es el host
                    hostUser = join.user;
                    hostNES = NegotiationEndState.NOTANSWERED;
                }
            }
        }else if(message instanceof Quit)  {
            // Received a Quit message, RUDE exit!!
            Quit quit = (Quit)message;
            members.remove(quit.user);
            notifyAll("control", "quit", quit.user.getUser().getUsername(), "has left the room");

            //**********************************Resto de mensajes de control **************************************
        }else if(message instanceof ControlMessage){
            String NESnotAnsw;
            ControlMessage msg = (ControlMessage)message;
            if(!isCorrectFormat(msg))//si el mensaje recibido no tiene el formato correcto...
                return;//salgo
            switch (msg.getKind()){
                /**
                 * restartRequest: El usuario indica que quiere emprezar una nueva ronda.
                 * Se envía una vez acabado el tiempo de la ronda normal de conversación.
                 */
                case ControlMessage.restartRequest:
                    if(msg.getUserMsOrg().getUser().getUsername().equals(ownerUser.getUser().getUsername())){
                        ownerNES = NegotiationEndState.RESTARTREQUEST;
                    }else if(msg.getUserMsOrg().getUser().getUsername().equals(hostUser.getUser().getUsername())){
                        hostNES = NegotiationEndState.RESTARTREQUEST;
                    }
                    if(isBothUserAnswer()){//si los dos usuarios han contestado algo: hay que mandar respuesta
                        resolveNegotiationEnd();
                    }
                    break;
                /**
                 * finRequest: El usuario indica que quiere acabar la conversación(no quiere empezar otra ronda).
                 * Se envía una vez acabado el tiempo de la ronda normal de conversación.
                 */
                case ControlMessage.finRequest:
                    if(msg.getUserMsOrg().getUser().getUsername(). equals(ownerUser.getUser().getUsername())){
                        ownerNES = NegotiationEndState.ENDREQUEST;
                    }else if(msg.getUserMsOrg().getUser().getUsername(). equals(hostUser.getUser().getUsername())){
                        ownerNES = NegotiationEndState.ENDREQUEST;
                    }
                    if(isBothUserAnswer()){//si los dos usuarios han contestado algo: hay que mandar respuesta
                        resolveNegotiationEnd();
                    }
                    break;
                /**
                 * forcedFin: indica que quiere salir de la conversación sin consultar al otro usuario.
                 */
                case ControlMessage.forcedFin:
                    timer.cancel();
                    timer.purge();
                    break;
                /**
                 * voluntaryTurnEnd: el usuario que lo envía termina voluntariamente su turno.
                 */
                case ControlMessage.voluntaryTurnEnd:
                    //Notificar cambio de turno y reiniciar tiempo de turno y el de minuto
                    notifyTurnEnd();
                    ttte.cancel();
                    ttme.cancel();
                    ttte.run();
                    ttme.run();
                    break;
                case ControlMessage.reputationPoints:
                    if(reputationFlag){
                        int points = Integer.parseInt(msg.getInfo());
                        if(points>0 && points<=5){

                        }
                    }

                    break;
                //**********************************FIN de mensajes de control **************************************
            }
        } else {
            unhandled(message);
        }

    }

    // Send a Json event to all members
    public void notifyAll(String type, String kind, String user, String text) {
        for(WebSocket.Out<JsonNode> channel: members.values()) {
            ObjectNode event = Json.newObject();
            event.put("type", type);
            event.put("kind", kind);
            event.put("user", user);
            event.put("info", text);

            ArrayNode m = event.putArray("members");
            for(UserConnected u: members.keySet()) {
                m.add(u.getUser().getUsername());
            }
            channel.write(event);
        }
    }

    // -- Messages
    public static class Join {
        final UserConnected user;
        final WebSocket.Out<JsonNode> channel;
        public Join(UserConnected username, WebSocket.Out<JsonNode> channel) {
            this.user = username;
            this.channel = channel;
        }
    }

    public static class Talk {
        final String username;
        final String text;
        public Talk(String username, String text) {
            this.username = username;
            this.text = text;
        }
    }

    public static class Quit {
        final UserConnected user;
        public Quit(UserConnected user) {
            this.user = user;
        }
    }

    // *************************************** FIN PARTE EXPERIMENTAL *********************************

    /**
     * CONSTRUCTOR del ChatCA. Gestiona el control de una conversación privada
     * @param host el usuario invitado
     * @param owner el usuario que envió la invitación
     * @param chatRequest modelo del ChatRequest de la invitación que dió paso al chat privado
     */
    public ChatCA(UserConnected host, UserConnected owner, ChatRequest chatRequest){
        hostUser = host;
        ownerUser = owner;

        //creo un nuevo registro de chat, que se almacena en la BD
        chat = new Chat(host.getUser(), owner.getUser(), chatRequest, new Date());
        chat.save();
        //running timer task as daemon thread (will be killed automatically when ChatCA finish his work)
        timer = new Timer(true);
        //execute the initial configuration
        initTimerConfig();
    }

    /*@Override
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
        
    }*/

    /**
     * comprueba que el formato del mensaje de control es correcto
     * @param msg
     * @return
     */
    private boolean isCorrectFormat(ControlMessage msg){
        if(msg.getUserMsOrg()!=null && msg.getKind()>0 && msg.getKind()<=5 && msg.getInfo()!=null){
            return true;
        }else return false;
    }

    /**
     * Initial Schedule configuration; Some tasks will be executed to control timing
     */
    private void initTimerConfig(){
        //inicio los TimerTask
        ttme = new TimerTaskMinuteEnd();
        ttte = new TimerTaskTurnEnd();
        //Defino la programación de los plazos
        //execute task every 60 seconds
        timer.scheduleAtFixedRate(ttme, 0, 60*1000);
        //execute task when user´s time is finished
        timer.scheduleAtFixedRate(ttte, 5000, TURN_TIME);
        //execute task when conversation´s time is finished: cuando el contador de turnos llegue a dos
    }

    /**
     * reinicia el chat y todos sus parámetros, inicia todas las tareas del timer.
     * Parámetros reiniciados: Respuesta a la negociación de fin de conversación -> a no contestado
     * Flag de acceso a la ronda de puntuaciones -> false (no concedido)
     * Timers: se activan los timers de sus tareas programadas.
     */
    private void startChat(){
        hostNES = NegotiationEndState.NOTANSWERED;
        ownerNES = NegotiationEndState.NOTANSWERED;
        reputationFlag = false;
        initTimerConfig();
        //reinicio contadores
        minutes = 0;
        turns = 0;
    }

    /**
     * Comprueba los flags para determinar si se ha producido un final de conversación con acuerdo (los dos
     * han aceptado salir de la conversación)
     * @return true si hay acuerdo, false si no hay acuerdo
     */
    private boolean isAgreedEnd(){
        String st = NegotiationEndState.ENDREQUEST.name();
        boolean res = false;
        if(ownerNES!=null && hostNES!=null){
            if(ownerNES.name().equals(st)  && hostNES.name().equals(st)){
                //agreement reached -> exit
                res = true;
            }else{
                //disagreement -> restart conversation
                res = false;
            }
        }else{
            //not answered jet -> not agreeded end
            res = false;
        }
        return res;
    }

    /**
     * Comprueba si han contestado a la negociación de fin de la conversación
     * @return true si han contestado los dos, false en caso contrario
     */
    private boolean isBothUserAnswer(){
        String notAnsw = NegotiationEndState.NOTANSWERED.name();
        if(ownerNES!=null && hostNES!=null && !ownerNES.name().equals(notAnsw) && !hostNES.name().equals(notAnsw)){
            return true;
        }else{
            return false;
        }
    }

    /**
     * Resuelve la ronda de negociación. Determina si los dos usuarios están de acuerdo o no en empezar una nueva ronda.
     * Si uno de los dos quiere reiniciar el chat, entonces se reinicia el chat.
     * Si los dos están de acuerdo en terminar, entonces es un final con consenso y se notifica el final del Chat a los usuarios
     */
    private void resolveNegotiationEnd(){
        if(isAgreedEnd()){//Es un final de mutuo acuerdo.
            //envío mensaje FINACK
            notifyAll(CONTROLMSG, FINACK, "", "");//da paso a la ronda de puntuaciones
            reputationFlag = true;
        }else{//no hay acuerdo. Uno quiere empezar una ronda y el otro no quiere
            //envío mensaje FINNACK
            notifyAll(CONTROLMSG, FINNACK, "", "");
            //hay que empezar otra ronda; reiniciar valores
            startChat();
        }
    }

    // ********************************* methods to send messages *****************************************

    /**
     * Se ejecutará cada 60 segundos. Este método lo llama la clase interna que lleva la cuenta del tiempo
     */
    private void notifyMinuteLess(){
        minutes++;
    }

    /**
     * Se ejecutará cuando acabe un turno de un usuario. Enviará un mensaje a los participantes indicando tal hecho
     */
    private void notifyTurnEnd(){
        turns++;
        if(turns >= 2)
            notifyRoundEnd();
    }
    /**
     * Se ejecutará cuando acabe una ronda(los dos usuarios han agotado sus turnos.
     * Enviará un mensaje a los participantes indicando tal hecho.
     */
    private void notifyRoundEnd(){
        timer.cancel();//paro los timers
        timer.purge();//elimino las tareas canceladas

    }

    // *******************************************************************************************************
}
