package controllers.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.UserConnected;
import models.chat.*;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;
import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import play.twirl.api.Html;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import views.html.partial.infoUser;
import views.html.partial.listUsers;

import static akka.pattern.Patterns.ask;

public class WaitingRoomCA extends UntypedActor {

    // Donde se guardan los usuarios
    // Map<userConnect,WebSocket.Out<JsonNode>>
    private static Map<UserConnected,WebSocket.Out<JsonNode>> users = new HashMap<UserConnected,WebSocket.Out<JsonNode>>();

    // Mirar como hacer que no aparectan en la lista los usuarios que estan en conversaciones

    // Donde se guardan las salas con sus usuarios
    // Map<UserConnected,UserConnected>
    // Clave: Usuario invitado
    // Value: Usuario que invita
    private static Map<UserConnected,UserConnected> requests = new HashMap<UserConnected,UserConnected>();

    // Actor que manejara los usuario, los creara, eliminara, y manejara las peticiones de chat y mensajes*

    private static ActorRef chatController = Akka.system().actorOf(Props.create(WaitingRoomCA.class,"Esto es la prueba"), "chatController");

    // variable de prueba

    public String prueba;

    public WaitingRoomCA(String prueba){

        this.prueba = prueba;

    }

    // Inserta un usuario
    public static boolean insertUser(UserConnected user,WebSocket.Out<JsonNode> out,WebSocket.In<JsonNode> in){
        //WaitingRoomCA.sendRequest("close");
        return WaitingRoomCA.sendRequest(new InsertUser(user, out, in));

    }

    // Envio una peticion y comprueba si hay errores en esa peticion
    private static boolean sendRequest(Object request){

        Errors errors;

        try{
            errors = (Errors)Await.result(ask(chatController, request, 1000), Duration.create(1, TimeUnit.SECONDS));
            // Meter la informacion en el log si hay algun error e informar al usuario del error
        }catch(Exception e){

            errors = new Errors(true);

        }

        return errors.hasErrors();

    }

    @Override
    public void onReceive(Object request) throws Exception {

        Errors errors = new Errors(false);

        // si el mensaje es insertar, entonces guarda el usuario en el map users
        if (request instanceof InsertUser) {

            // Envio el usuario conectado a todos los usuarios
            // user almacena el UserConnected del usuario que ha pedido la conexion
            final InsertUser user = (InsertUser) request;

            Html html = listUsers.render(user.getUserConnected());

            //Preparo el mensaje a enviar
            ObjectNode message = Json.newObject();

            // Le digo que es un mensaje de tipo open
            message.put("type","open");

            // Envio el mensaje

            message.put("html",html.toString());

            // Envio el id que seria el username
            message.put("id",user.getUserConnected().getUser().getUsername());

            // Envio el mensaje a todos los usuarios exepto a si mismo
            sendMessageToAll(message);

            // Meto el user connected en el mapa junto a su out
            WaitingRoomCA.users.put(user.getUserConnected(), user.getOut());

            // Evento que se ejecuta cuando recibe un mensaje
            user.getIn().onMessage(new F.Callback<JsonNode>() {
                @Override
                public void invoke(JsonNode jsonNode) throws Throwable {

                    // Obtengo la informacion del usuario
                    if(jsonNode.get("type").asText().equals("getInfoUser")){

                        // Hace la peticion mediante un actor para obtener la informacion del usuario
                        WaitingRoomCA.sendRequest(new InfoUser(jsonNode.get("username").asText(), user.getOut()));

                    }

                    // Realiza la peticion de invitacion mediante el actor
                    if(jsonNode.get("type").asText().equals("chatRequest")){

                        // Hace la peticion mediante un actor para realizar y enviar la peticion
                        WaitingRoomCA.sendRequest(new ChatRequest(user.getUserConnected(), jsonNode.get("username").asText()));

                    }

                    // Realiza la peticion de rechazo de la peticion mediante el actor
                    if(jsonNode.get("type").asText().equals("rejectInvitation")){

                        WaitingRoomCA.sendRequest(new RejectInvitation(user.getUserConnected()));

                    }

                    // Realiza la peticion de aceptacion de la peticion mediante el actor
                    if(jsonNode.get("type").asText().equals("acceptInvitation")){

                        WaitingRoomCA.sendRequest(new AcceptInvMsg(user.getUserConnected()));

                    }

                    if(jsonNode.get("type").asText().equals("cancelInvitation")){

                        WaitingRoomCA.sendRequest(new CancelInvMsg((user.getUserConnected())));

                    }
                }
            });

            // Evento que se ejecuta cuando se cierra la conexion con el usuario
            user.getIn().onClose(new F.Callback0() {
                public void invoke() {

                    System.out.println("El usuario:" + user.getUserConnected().getUser().getUsername() + " se ha desconectado");

                    // Preparo la informacion a enviar a los usuarios

                    ObjectNode message = Json.newObject();
                    message.put("type", "close");
                    message.put("id", user.getUserConnected().getUser().getUsername());

                    // Eliminar usuario del map
                    WaitingRoomCA.users.remove(user.getUserConnected());

                    //Informo a los usuarios de que se va a eliminar el usuario
                    sendMessageToAll(message);

                }
            });

            // Si por algun motivo no se ha guardado el usuario en el mapa, se guardara el error
            if (!WaitingRoomCA.users.containsKey(user.getUserConnected())) {

               errors.setMessage("The user has not been saved in the map");

            }
        }

        // Obtiene la informacion del usuario y se la envia
        if(request instanceof InfoUser){

            InfoUser user = (InfoUser) request;

            // Obtengo la informacion del usuario
            UserConnected info = getUserInfoByUserName(user.getUserName());

            // Si hay UserConnected, genero el mensaje
            if(info != null){

                Html infoHtml = infoUser.render(info);

                ObjectNode message = Json.newObject();

                message.put("type","infoUser");
                message.put("html",infoHtml.toString());


                // Envio el mensaje
                user.getOut().write(message);

            }

        }

        // Recibe la peticion del usuario para iniciar un chat
        if(request instanceof ChatRequest){

            ChatRequest chatRequest = (ChatRequest)request;

            // Obtengo el UserConnected del usuario al que hay que enviarle la informacion
            UserConnected userToSendInvite = this.getUserInfoByUserName(chatRequest.getGuest());

            // Si el usuario existe, preparo el envio y compruebo si alguien ya le ha invitado
            if((userToSendInvite != null && !WaitingRoomCA.requests.containsKey(userToSendInvite))){

                // Obtengo el webSocket out del usuario al que hay que enviarle la informacion
                WebSocket.Out<JsonNode> out = WaitingRoomCA.users.get(userToSendInvite);

                // Meto los usuarios en el mapa requests(Ahora el invitado como clave)
                // Clave es el invitado y value es el que ha realizado la invitacion
                WaitingRoomCA.requests.put(userToSendInvite,chatRequest.getUser());

                // Tambien lo pongo al reves para poder cancelar la peticion
                WaitingRoomCA.requests.put(chatRequest.getUser(),userToSendInvite);

                ObjectNode message = Json.newObject();

                message.put("type","chatRequest");
                // Envio el username del que ha realizado la peticion
                message.put("username", chatRequest.getUser().getUser().getUsername());

                // Envio el mensaje
                out.write(message);

            }
            else{
                // Envio un mensaje al usuario que realizo la peticion informando del error

                // Obtengo el webSocket out del usuario para informarle del error
                WebSocket.Out<JsonNode> out = WaitingRoomCA.users.get(chatRequest.getUser());

                ObjectNode message = Json.newObject();

                message.put("type","error");
                message.put("message","El usuario ya ha sido invitado");

                // Envio el mensaje
                out.write(message);

            }

        }

        // Recibe el rechazo de una invitacion
        if(request instanceof RejectInvitation){

            RejectInvitation requestAC = (RejectInvitation) request;

            if(WaitingRoomCA.requests.containsKey(requestAC.getUser())){

                // Obtengo el UserConnected del que se quiere rechazar
                UserConnected host = WaitingRoomCA.requests.get(requestAC.getUser());

                // Obtengo el out del usuario que inicio la peticion

                WebSocket.Out<JsonNode> out = WaitingRoomCA.users.get(host);

                // Envio un mensaje al que inicio la invitacion

                ObjectNode message = Json.newObject();

                message.put("type","rejectinvitation");

                out.write(message);

                // Elimino a los usuarios de las peticiones
                WaitingRoomCA.requests.remove(requestAC.getUser());
                WaitingRoomCA.requests.remove(host);

            }

        }

        if(request instanceof AcceptInvMsg){

            AcceptInvMsg accept = (AcceptInvMsg) request;

            if(WaitingRoomCA.requests.containsKey(accept.getUser())){

                // Obtengo el UserConnected del que se quiere aceptar
                UserConnected host = WaitingRoomCA.requests.get(accept.getUser());

                // Obtengo el out del usuario que inicio la peticion
                WebSocket.Out<JsonNode> out = WaitingRoomCA.users.get(host);

                // Envio un mensaje al que inicio la invitacion

                ObjectNode message = Json.newObject();

                message.put("type","acceptinvitation");

                out.write(message);

                // Preparar la sala de chat

                // Elimino a los usuarios de las peticiones
                WaitingRoomCA.requests.remove(accept.getUser());
                WaitingRoomCA.requests.remove((host));

                System.out.println("Invitacion acceptada");
            }

        }

        if(request instanceof CancelInvMsg){

            CancelInvMsg cancel = (CancelInvMsg) request;

            if(WaitingRoomCA.requests.containsKey(cancel.getUser())){

                // Obtengo el UserConnected del que envio la informacion
                UserConnected guest = WaitingRoomCA.requests.get(cancel.getUser());

                // Obtengo el out del usuario que inicio la peticion
                WebSocket.Out<JsonNode> out = WaitingRoomCA.users.get(guest);

                // FALTA ENVIAR LA INFORMACION AL USUARIO DE QUE SE HA CANCELADO LA INVITACION

                ObjectNode message = Json.newObject();

                message.put("type","cancelInvitation");

                out.write(message);

                // Elimino a los usuarios de las peticiones
                WaitingRoomCA.requests.remove(cancel.getUser());
                WaitingRoomCA.requests.remove((guest));

                System.out.println("Invitacion Cancelada");
            }

        }

        // indico si ha habido errores o no y realizo la respuesta al actor
        getSender().tell(errors,self());


    }

    // Obtengo todos los usuarios en json
    private ObjectNode getUsersInJson(){

        ObjectNode list = Json.newObject();

        for(UserConnected users: WaitingRoomCA.users.keySet()){

            ObjectNode user = Json.newObject();

            user.put("username",users.getUser().getUsername());
            user.put("category",users.getCategory().getCategoryName());
            list.put(users.getUser().getEmail(),user);



        }

        return list;

    }

    // Obtengo todos los userConnected del map users

    /**
     * Devuelve un arrayList con todos los UserConnected que estan
     * en el Map users
     * @return Devuelve un arrayList de UserConnected
     */
    public static ArrayList<UserConnected> getUsers(){

        ArrayList listUsers = new ArrayList<UserConnected>();

        for(UserConnected user: WaitingRoomCA.users.keySet()){

            listUsers.add(user);

        }

        return listUsers;

    }

    public static ArrayList<UserConnected> getUsers(String username){

        ArrayList listUsers = new ArrayList<UserConnected>();

        for(UserConnected user: WaitingRoomCA.users.keySet()){
            if(user.getUser().getUsername() != username) {
                listUsers.add(user);
            }

        }



        return listUsers;

    }

    /**
     * Devuelve en un objectNode con la informacion del
     * usuario que se le pasa como parametro
     * @see EN CONSTRUCCION
     * @param user Una instancia de UserConnected
     * @return
     */
    private JsonNode getUser(UserConnected user){

        ObjectNode data = Json.newObject();

        data.put("username",user.getUser().getUsername());
        data.put("category",user.getCategory().getCategoryName());

        return data;

    }

    // Envia un mensaje a todos los usuarios. El mensaje se pasa como parametro

    /**
     * Envia un Json a todos los usuarios conectados.
     * Los usuarios los obtiene mediante el map users de la clase
     * WaitingRoomCA
     * @param message　Una instancia de objectNode
     */
    private void sendMessageToAll(ObjectNode message){

        for(WebSocket.Out<JsonNode> out: WaitingRoomCA.users.values()){

            out.write(message);

        }

    }

    private void sendMessageToAll(ObjectNode message, WebSocket.Out<JsonNode> except){

        for(WebSocket.Out<JsonNode> out: WaitingRoomCA.users.values()){

            if(!out.equals(except)) {

                out.write(message);

            }

        }

    }

    /**
     * Devuelve un UserConnected en el Map users de la clase WaitingRoomCA
     * mediante el username.
     * @param userName El username del Model username
     * @return Devuelve el UserConnected encontrado.
     * Si no encuentra ningun UserConnected, devuelve null
     */
    private UserConnected getUserInfoByUserName(String userName){

        UserConnected info = null;

        for(UserConnected user: WaitingRoomCA.users.keySet()){

            if(user.equals(userName)){

                info = user;

            }

        }

        return info;
    }

    private WebSocket.Out getOutByUserName(String userName){

        WebSocket.Out<JsonNode> out = null;

        for(UserConnected user: WaitingRoomCA.users.keySet()){

            if(user.equals(userName)){

                out = WaitingRoomCA.users.get(user);

            }

        }

        return out;

    }

    public UserConnected findUserByValue(UserConnected value){

        UserConnected user = null;

        for(UserConnected key: WaitingRoomCA.requests.keySet()){

            if(WaitingRoomCA.users.get(key).equals(value)){

                user = key;

            }

        }

        return user;

    }































    // Permite detectar que error se ha producido
    static class Errors {

        private boolean hasErrors;
        private String message;

        Errors(boolean hasErrors) {

            this.hasErrors = hasErrors;

        }

        // true = Tiene errores false = no tiene errores
        public boolean hasErrors(){

            return this.hasErrors;

        }

        // cambia errors por el valor que le pases true o false
        public void setHasErrors(boolean has){

            this.hasErrors = has;

        }

        // Permite indicar que hay un error y guardar un mensaje
        public void setErrors(String message){

            this.hasErrors = true;
            this.message = message;
            Logger.error(message);

        }

        // Pone el mensaje del error que le pases como parametro
        public void setMessage(String message){

            this.message = message;

        }

        // Obtiene el mensaje del error
        public String getMessage(){

            return this.message;

        }

    }
}
