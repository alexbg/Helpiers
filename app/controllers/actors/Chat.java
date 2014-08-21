package controllers.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import models.UserConnected;
import models.chat.ChatRequest;
import models.chat.InfoUser;
import models.chat.InsertUser;
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

public class Chat extends UntypedActor {

    // Donde se guardan los usuarios
    // Map<userConnect,WebSocket.Out<JsonNode>>
    private static Map<UserConnected,WebSocket.Out<JsonNode>> users = new HashMap<UserConnected,WebSocket.Out<JsonNode>>();

    // Mirar como hacer que no aparectan en la lista los usuarios que estan en conversaciones

    // Donde se guardan las salas con sus usuarios
    // Map<userCreated,userCreated>
    private static Map<String,String> rooms = new HashMap<String,String>();

    // Actor que manejara los usuario, los crearra, eliminara, y manejara las peticiones de chat y mensajes*

    private static ActorRef chatController = Akka.system().actorOf(Props.create(Chat.class), "chatController");

    // Inserta un usuario
    public static boolean insertUser(UserConnected user,WebSocket.Out<JsonNode> out,WebSocket.In<JsonNode> in){
        //Chat.sendRequest("close");
        return Chat.sendRequest(new InsertUser(user, out, in));

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
            Chat.users.put(user.getUserConnected(), user.getOut());

            // Evento que se ejecuta cuando recibe un mensaje
            user.getIn().onMessage(new F.Callback<JsonNode>() {
                @Override
                public void invoke(JsonNode jsonNode) throws Throwable {

                    // Obtengo la informacion del usuario
                    if(jsonNode.get("type").asText().equals("getInfoUser")){
                        // Hace la peticion mediante un actor para obtener la informacion del usuario
                        Chat.sendRequest(new InfoUser(jsonNode.get("username").asText(),user.getOut()));
                    }

                    if(jsonNode.get("type").asText().equals("chatRequest")){

                        System.out.println("Peticion chatRequest recibida");

                        // Hace la peticion mediante un actor para realizar y enviar la peticion
                        Chat.sendRequest(new ChatRequest(user.getUserConnected(),jsonNode.get("username").asText()));

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
                    Chat.users.remove(user.getUserConnected());

                    //Informo a los usuarios de que se va a eliminar el usuario
                    sendMessageToAll(message);

                }
            });

            // Si por algun motivo no se ha guardado el usuario en el mapa, se guardara el error
            if (!Chat.users.containsKey(user.getUserConnected())) {

               errors.setMessage("The user has not been saved in the map");

            }
        }

        // Obtiene la informacion del usuario y se la envia
        if(request instanceof InfoUser){

            InfoUser user = (InfoUser) request;

            // Obtengo la informacion del usuario
            UserConnected info = getUserInfoByUserName(user.getUserName());
            System.out.println("Preparando informacion del usuario...");
            // Si hay UserConnected, genero el mensaje
            if(info != null){

                Html infoHtml = infoUser.render(info);

                ObjectNode message = Json.newObject();

                message.put("type","infoUser");
                message.put("html",infoHtml.toString());
                System.out.println(message);

                // Envio el mensaje
                user.getOut().write(message);

                System.out.println("Informacion enviada");

            }

        }

        // Recibe la peticion del usuario para iniciar un chat
        if(request instanceof ChatRequest){

            ChatRequest chatRequest = (ChatRequest)request;

            // Obtengo el UserConnected del usuario alq ue hayq uen enviarle la informacion
            UserConnected userToSendInvite = this.getUserInfoByUserName(chatRequest.getGuest());

            // Obtengo el webSocket out del usuario al que hay que enviarle la informacion
            WebSocket.Out<JsonNode> out = Chat.users.get(userToSendInvite);

            // Si el usuario existe, preparo el envio
            if(userToSendInvite != null){

                ObjectNode message = Json.newObject();

                message.put("type","chatRequest");
                // Envio el username del que ha realizado la peticion
                message.put("username", chatRequest.getUser().getUser().getUsername());

                // Envio el mensaje
                out.write(message);

            }

        }

        // indico si ha habido errores o no y realizo la respuesta al actor
        getSender().tell(errors,self());


    }

    // Obtengo todos los usuarios en json
    private ObjectNode getUsersInJson(){

        ObjectNode list = Json.newObject();

        for(UserConnected users: Chat.users.keySet()){

            ObjectNode user = Json.newObject();

            user.put("username",users.getUser().getUsername());
            user.put("category",users.getCategory().getCategoryName());
            list.put(users.getUser().getEmail(),user);

            System.out.println(list);

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

        for(UserConnected user: Chat.users.keySet()){

            listUsers.add(user);

        }

        //System.out.println(Chat.users.size());

        return listUsers;

    }

    public static ArrayList<UserConnected> getUsers(String username){

        ArrayList listUsers = new ArrayList<UserConnected>();

        for(UserConnected user: Chat.users.keySet()){
            if(user.getUser().getUsername() != username) {
                listUsers.add(user);
            }

        }

        //System.out.println(Chat.users.size());

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
     * Chat
     * @param message　Una instancia de objectNode
     */
    private void sendMessageToAll(ObjectNode message){

        for(WebSocket.Out<JsonNode> out: Chat.users.values()){

            out.write(message);

        }

    }

    private void sendMessageToAll(ObjectNode message, WebSocket.Out<JsonNode> except){

        for(WebSocket.Out<JsonNode> out: Chat.users.values()){

            if(!out.equals(except)) {

                out.write(message);

            }

        }

    }

    /**
     * Devuelve un UserConnected en el Map users de la clase Chat
     * mediante el username.
     * @param userName El username del Model username
     * @return Devuelve el UserConnected encontrado.
     * Si no encuentra ningun UserConnected, devuelve null
     */
    private UserConnected getUserInfoByUserName(String userName){

        UserConnected info = null;

        for(UserConnected user: Chat.users.keySet()){

            if(user.equals(userName)){

                info = user;

            }

        }

        return info;
    }

    private WebSocket.Out getOutByUserName(String userName){

        WebSocket.Out<JsonNode> out = null;

        for(UserConnected user: Chat.users.keySet()){

            if(user.equals(userName)){

                out = Chat.users.get(user);

            }

        }

        return out;

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
