package controllers;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.*;
import models.chat.ControlMessage;
import play.Logger;
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

    public final static long MINUTES = 1;
    //tiempo de duración de un turno en milisegundos
    public final static long TURN_TIME = MINUTES*60*1000;//(minutos)*(segundos/minuto)*(milisegundos/segundo)

    // ************ESPECIFICACION LOS NOMBRES DE MENSAJES *******************************
    public final static String JSON_TYPE = "type";
    public final static String JSON_TYPE_TALK = "talk";
    public final static String JSON_TYPE_CONTROL = "control";
    public final static String JSON_KIND = "kind";
    public final static String JSON_KIND_TEXT = "text";
    public final static String JSON_KIND_QUIT = "quit";
    public final static String JSON_INFO = "info";
    public final static String JSON_USER = "user";

    //enviados desde el ChatCA
    public final static String CONTROLMSG = "control";
    public final static String FIN_ACK = "fin_ack";
    public final static String FIN_NACK = "fin_nack";
    public final static String JOIN = "join";
    public final static String MINUTE_NOTIFY = "minute_notify";
    public final static String TURN_NOTIFY = "turn_notify";
    public final static String ROUND_NOTIFY = "round_notify";

    //recibidos desde el cliente
    public final static String RESTART_REQUEST = "restart_request";
    public final static String FIN_REQUEST = "fin_request";
    public final static String FORCED_FIN = "forced_fin";
    public final static String VOLUNTARY_TURN_END = "voluntary_turn_end";
    public final static String REPUTATION_POINTS = "reputation_points";


    // ************ FIN ESPECIFICACION LOS NOMBRES DE MENSAJES *******************************
    //Atributos

    private UserConnected hostUser;
    private UserConnected ownerUser;
    private Chat chat;
    private Timer timer;
    private TimerTaskMinuteEnd ttme;
    private TimerTaskTurnEnd ttte;
    private NegotiationEndState ownerNES;
    private NegotiationEndState hostNES;
    private String speaker;
    private String listener;

    // Members of this room.
    private Map<UserConnected, WebSocket.Out<JsonNode>> members = new HashMap<UserConnected, WebSocket.Out<JsonNode>>();


    // ****************************************** Control flags*******************************************
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
        RESTARTREQUEST
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
    // **************************************Fin tareas **********************************************
    /**
     * Create Props for an actor of this type.
      * @param host el usuario invitado
      * @param owner el usuario que envió la invitación
     * @param chatRequest modelo del ChatRequest de la invitación que dió paso al chat privado
     * @return a Props for creating this actor, which can then be further configured
     *         (e.g. calling `.withDispatcher()` on it)
     */
    public static Props mkProps(UserConnected host, UserConnected owner, ChatRequest chatRequest) {
        System.out.println("Entra en mkProps");
        return Props.create(ChatCA.class, host, owner, chatRequest);
    }

    /**
     * CONSTRUCTOR del ChatCA. Gestiona el control de una conversación privada. Se llama desde el mkProps de forma transparente
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
        System.out.println("Se ha ejecutado el constructor ChatCA");
    }

    /**
     * Join the default room.
     */
    public void join(final UserConnected user, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception{
        // Send the Join message to the room
        System.out.println("Se ha ejecutado el join en ChatCA");
        //obtener el ActorRef correspondiente a ese UserConnected
        final ActorRef actor = PrivateChatsController.privateChatsMap.get(user);
        //String result = (String) Await.result(ask(actor, new Join(user, out), 1000), Duration.create(1, SECONDS));
        //if("OK".equals(result)) {
            Logger.of("ChatCA").info("Se espera recibir mensajes de los participantes");

            // ****************************For each event received on the socket **************************************

            in.onMessage(new F.Callback<JsonNode>() {
                public void invoke(JsonNode event) {

                    Logger.of("ChatCA").info(Json.stringify(event));
                    //si es un mensaje de texto (habla)
                    if(event.get(JSON_TYPE).asText().equals(JSON_TYPE_TALK)){
                        actor.tell(new Talk(user.getUser().getUsername(), event.get(JSON_INFO).asText()), actor);

                    // Si es un mensaje de control: mirar que tipo de mensaje de control.
                    }else if(event.get(JSON_TYPE).asText().equals(JSON_TYPE_CONTROL)){
                        String kind = event.get(JSON_KIND).asText();
                        if(kind.equals(RESTART_REQUEST)){
                            actor.tell(new ControlMessage(user, ControlMessage.restartRequest, ""), actor);
                        }else if(kind.equals(FIN_REQUEST)){
                            actor.tell(new ControlMessage(user, ControlMessage.finRequest, ""), actor);
                        }else if(kind.equals(FORCED_FIN)){
                            actor.tell(new ControlMessage(user, ControlMessage.forcedFin, ""), actor);
                        }else if(kind.equals(VOLUNTARY_TURN_END)){
                            actor.tell(new ControlMessage(user, ControlMessage.voluntaryTurnEnd, ""), actor);
                        }else if(kind.equals(REPUTATION_POINTS)){
                            actor.tell(new ControlMessage(user, ControlMessage.reputationPoints, event.get(JSON_INFO).asText()), actor);
                        }
                    }
                }
            });
            // When the socket is closed.
            in.onClose(new F.Callback0() {
                public void invoke() {
                    // Send a Quit message to the room.
                    actor.tell(new Quit(user), actor);
                }
            });
        /*} else {
            Logger.of("ChatCA").info("El actor no ha respondido OK");
            // Cannot connect, create a Json error.
            ObjectNode error = Json.newObject();
            error.put("error", result);
            // Send the error to the socket.
            out.write(error);
        }*/
    }

    public void onReceive(Object message) throws Exception {
        if(message instanceof Talk)  {
            // Received a Talk message
            Talk talk = (Talk)message;
            //envío mensaje de tipo talk (talk), de texto(text) del usuario (username), y su texto (text)
            notifyAll(JSON_TYPE_TALK, JSON_KIND_TEXT, talk.username, talk.text);

        }else if (message instanceof Join) {
            // Received a Join message
            Join join = (Join)message;
            if(members.size()<2){//controlo que como máximo entren dos usuarios en el chat privado.
                members.put(join.user, join.channelOut);
                notifyAll(CONTROLMSG, JOIN, join.user.getUser().getUsername(), "has entered the room");
                getSender().tell("OK", getSelf());
                /*
                 * Hay que añadir a uno de los participantes como owner y otro
                 * como host. El primero en entrar en la sala (si el map tiene tamaño 1) es el usuario owner.
                 */
                if(members.size() == 1){
                    ownerUser = join.user;
                    ownerNES = NegotiationEndState.NOTANSWERED;
                    System.out.println("Se ha designado al usuario " + ownerUser.getUser().getUsername() + "como owner");
                    //este usuario tendrá el primer turno
                    speaker = join.user.getUser().getUsername();
                }else if(members.size() == 2){//si es el segundo del map, es el host
                    hostUser = join.user;
                    hostNES = NegotiationEndState.NOTANSWERED;
                    System.out.println("Se ha designado al usuario " + hostUser.getUser().getUsername() + "como host");
                    //este usuario tendrá el segundo turno
                    listener = join.user.getUser().getUsername();
                }
                join(join.user, join.channelIn, join.channelOut);
            }if(members.size() == 2){ //Si hay dos participantes... Ya estamos todos! -> empieza el Chat
                startChat();
            }
        }else if(message instanceof Quit)  {
            // Received a Quit message, RUDE exit!!
            Quit quit = (Quit)message;
            members.remove(quit.user);
            notifyAll(JSON_TYPE_CONTROL, JSON_KIND_QUIT, quit.user.getUser().getUsername(), "has left the room");

            //**********************************Resto de mensajes de control **************************************
        }else if(message instanceof ControlMessage){
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
                        Logger.of("ChatCA").info("el owner vota restart");
                    }else if(msg.getUserMsOrg().getUser().getUsername().equals(hostUser.getUser().getUsername())){
                        hostNES = NegotiationEndState.RESTARTREQUEST;
                        Logger.of("ChatCA").info("el host vota restart");
                    }
                    if(isBothUserAnswer()){//si los dos usuarios han contestado algo: hay que mandar respuesta
                        Logger.of("ChatCA").info("Los dos usuarios han contestado");
                        resolveNegotiationEnd();
                    }
                    break;
                /**
                 * finRequest: El usuario indica que quiere acabar la conversación(no quiere empezar otra ronda).
                 * Se envía una vez acabado el tiempo de la ronda normal de conversación.
                 */
                case ControlMessage.finRequest:
                    if(msg.getUserMsOrg().getUser().getUsername().equals(ownerUser.getUser().getUsername())){
                        ownerNES = NegotiationEndState.ENDREQUEST;
                    }else if(msg.getUserMsOrg().getUser().getUsername().equals(hostUser.getUser().getUsername())){
                        hostNES = NegotiationEndState.ENDREQUEST;
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
                /*
                 * reputationPoints: mensaje que lleva los puntos de reputación
                 */
                case ControlMessage.reputationPoints:
                    if(reputationFlag){
                        int points = Integer.parseInt(msg.getInfo());
                        if(points>0 && points<=5){
                            //crear clase Rating y meter registro en la BD
                            Rating rating =  new Rating(msg.getUserMsOrg().getUser(), msg.getUserMsOrg().getCategory(), points);
                            //eliminarle del Chat. Se termina la conversación.
                            members.remove(msg.getUserMsOrg());
                           // notifyAll("control", "quit", msg.getUserMsOrg().getUser().getUsername(), "has left the room");
                            rating.save();
                        }
                    }
                    break;
                //**********************************FIN de mensajes de control **************************************
            }
        } else {
            unhandled(message);
        }

    }

    // **************************************** MENSAJES **********************************************
    public static class Join {
        final UserConnected user;
        final WebSocket.Out<JsonNode> channelOut;
        final WebSocket.In<JsonNode> channelIn;
        public Join(UserConnected username, WebSocket.Out<JsonNode> channelOut, WebSocket.In<JsonNode> channelIn) {
            this.user = username;
            this.channelOut = channelOut;
            this.channelIn = channelIn;
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

    // ****************************************FIN MENSAJES **********************************************

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
        //running timer task as daemon thread (will be killed automatically when ChatCA finish his work)
        timer = new Timer(true);
        //inicio los TimerTask
        ttme = new TimerTaskMinuteEnd();
        ttte = new TimerTaskTurnEnd();
        //Defino la programación de los plazos
        //execute task every 60 seconds
        timer.scheduleAtFixedRate(ttme, 0, 60*1000);//2ºparámetro es un retardo de un minuto (para que no salte nada mas empezar)
        //execute task when user´s time is finished
        timer.scheduleAtFixedRate(ttte, 0, TURN_TIME);
        //execute task when conversation´s time is finished: cuando el contador de turnos llegue a dos
        System.out.println("Se han configurado los timers");
    }

    /**
     * reinicia el chat y todos sus parámetros, inicia todas las tareas del timer.
     * Parámetros reiniciados: Respuesta a la negociación de fin de conversación -> a no contestado
     * Flag de acceso a la ronda de puntuaciones -> false (no concedido)
     * Timers: se activan los timers de sus tareas programadas.
     */
    private void startChat(){
        System.out.println("Se van a iniciar los timers");
        hostNES = NegotiationEndState.NOTANSWERED;
        ownerNES = NegotiationEndState.NOTANSWERED;
        reputationFlag = false;
        initTimerConfig();
        //reinicio contadores
        minutes = 0;//realmente no lo uso de momento, pero puede ser útil después
        turns = 0;
        //establezco quien habla
        speaker = hostUser.getUser().getUsername();
        listener = ownerUser.getUser().getUsername();

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
                Logger.of("ChatCA").info("Acuerdo alcanzado");
            }else{
                //disagreement -> restart conversation
                Logger.of("ChatCA.isAgreedEnd").info("restart required");
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
            notifyAll(CONTROLMSG, FIN_ACK, "System", "Paso a la ronda de puntuaciones");//da paso a la ronda de puntuaciones
            reputationFlag = true;
        }else{//no hay acuerdo. Uno quiere empezar una ronda y el otro no quiere
            //envío mensaje FINNACK
            notifyAll(CONTROLMSG, FIN_NACK, "", "");
            //hay que empezar otra ronda; reiniciar valores
            Logger.of("ChatCA").info("Hay que reiniciar el chat");
            startChat();
        }
    }

    // ********************************* methods to send messages *****************************************

    /** Send a Json event to all members
     * El formato JSON es el siguiente:
     * @param type tipo de mensaje
     * @param kind clase de mensaje (dentro de un type hay uno o varios kind)
     * @param user usuario que manda el mensaje (username)
     * @param info información asociada al mensaje
     */
    public void notifyAll(String type, String kind, String user, String info) {
        for(WebSocket.Out<JsonNode> channel: members.values()) {
            ObjectNode event = Json.newObject();
            event.put(JSON_TYPE, type);
            event.put(JSON_KIND, kind);
            if(user!=null)//es nulo o puede ser nulo cuando es un mensaje de control
                event.put(JSON_USER, user);
            if(info!=null)// es o puede ser nulo cuando es un mensaje de control
                event.put(JSON_INFO, info);

            ArrayNode m = event.putArray("members");
            for(UserConnected u: members.keySet()) {
                m.add(u.getUser().getUsername());
            }
            channel.write(event);
        }
    }

    /**
     * Se ejecutará cada 60 segundos. Este método lo llama la clase interna que lleva la cuenta del tiempo
     */
    private void notifyMinuteLess(){
        minutes++;
        notifyAll(CONTROLMSG, MINUTE_NOTIFY, null, Integer.toString(minutes) + " de " + Long.toString(TURN_TIME/(60*1000)) + " minutos");
        System.out.println("Se ha ejecutado la tarea notifyMinuteLess");
    }

    /**
     * Se ejecutará cuando acabe un turno de un usuario. Enviará un mensaje a los participantes indicando tal hecho
     */
    private void notifyTurnEnd(){
        turns++;
        minutes = 0;
        if(turns > 2){
            notifyRoundEnd();
        }else{
            //cambio el speaker por el listener
            String aux = speaker;
            speaker = listener;
            listener = aux;
            notifyAll(CONTROLMSG, TURN_NOTIFY, "System", speaker);//ultimo parametro indica quien habla
        }
    }
    /**
     * Se ejecutará cuando acabe una ronda(los dos usuarios han agotado sus turnos.
     * Enviará un mensaje a los participantes indicando tal hecho.
     */
    private void notifyRoundEnd(){
        timer.cancel();//paro los timers
        //timer.purge();//elimino las tareas canceladas
        notifyAll(CONTROLMSG, ROUND_NOTIFY, "System", "Final de la conversación");
    }

    // *******************************************************************************************************


    // ******************************GETTERS Y SETTERS***********************************************
    public UserConnected getHostUser() {
        return hostUser;
    }

    public UserConnected getOwnerUser() {
        return ownerUser;
    }
}
