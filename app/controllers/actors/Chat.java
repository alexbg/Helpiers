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
import models.chat.InsertUser;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;
import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

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

            final InsertUser user = (InsertUser) request;

            // Meto el user connected en el mapa junto a su out
            Chat.users.put(user.getUserConnected(), user.getOut());

            // Evento que se ejecuta cuando recibe un mensaje
            user.getIn().onMessage(new F.Callback<JsonNode>() {
                @Override
                public void invoke(JsonNode jsonNode) throws Throwable {


                    // Realiza la peticion de obtener usuarios
                    if(jsonNode.get("type").asText().equals("getusers")){

                        System.out.println("Obtencion de usuarios");

                        // obtengo una lista con los usuarios conectados
                        ObjectNode users = getUsersInJson();

                        // obtengo el out del usuario al que enviare la lista, y se la envio con write
                        user.getOut().write(users);

                    }
                }
            });

            // Evento que se ejecuta cuando se cierra la conexion con el usuario
            user.getIn().onClose(new F.Callback0() {
                public void invoke() {

                    System.out.println("El usuario:" + user.getUserConnected().getUser().getUsername() + " se ha desconectado");

                    // Eliminar usuario del map
                    Chat.users.remove(user.getUserConnected());

                }
            });

            // SI por algun motivo no se ha guardado el usuario en el mapa, se guardara el error
            if (!Chat.users.containsKey(user.getUserConnected())) {

               errors.setMessage("The user has not been saved in the map");

            }
        }

        // borra el usuario y envia un mensaje a todos los usuarios para que lo borren de su lista
        if("close".equals(request)){

            System.out.println("close on receive");


        }

        // indico si ha habido errores o no
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
    public static ArrayList<UserConnected> getUsers(){

        ArrayList listUsers = new ArrayList<UserConnected>();

        for(UserConnected user: Chat.users.keySet()){

            listUsers.add(user);

        }

        System.out.println(Chat.users.size());

        return listUsers;

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
