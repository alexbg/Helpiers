package controllers.actors;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;

import com.fasterxml.jackson.databind.JsonNode;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import static akka.pattern.Patterns.ask;




public class Chat extends UntypedActor {

    // Donde se guardan los usuarios
    // Map<userConnect,WebSocket.Out<JsonNode>>
    private static Map<String,WebSocket.Out<JsonNode>> users = new HashMap<String,WebSocket.Out<JsonNode>>();

    // Mirar como hacer que no aparectan en la lista los usuarios que estan en conversaciones


    // Donde se guardan las salas con sus usuarios
    // Map<userCreated,userCreated>
    private static Map<String,String> rooms = new HashMap<String,String>();

    // Las peticiones estan en la base de datos


    // Actor que manejara los usuarios

    private static ActorRef chatController = Akka.system().actorOf(Props.create(Chat.class), "chatController");

    // Inserta un usuario
    public static boolean setUser(String email,WebSocket.Out<JsonNode> out,WebSocket.In<JsonNode> in){
        //Chat.sendRequest("close");
        return Chat.sendRequest(new Insert(email, out, in));

    }

    // Obtiene los usuarios
    public static String[] getUsers(){

        String[] users = new String[Chat.users.size()];
        int i = 0;
        for ( String key : Chat.users.keySet() ) {

            users[i] = key;

            i++;
        }

        return users;

    }

    // Envio una peticion y comprueba si hay errores en esa peticion
    private static boolean sendRequest(Object request){

        Errors errors;

        try{
            errors = (Errors)Await.result(ask(chatController, request, 1000), Duration.create(1, TimeUnit.SECONDS));
        }catch(Exception e){

            errors = new Errors(true);

        }

        return errors.hasErrors();

    }

    @Override
    public void onReceive(Object message) throws Exception {

        Errors errors = new Errors(false);
        // si el mensaje es insertar, entonces guarda el usuario en el map users

        if (message instanceof Insert) {

            final Insert user = (Insert) message;

            Chat.users.put(user.email, user.channel);

            user.control.onMessage(new F.Callback<JsonNode>() {
                @Override
                public void invoke(JsonNode jsonNode) throws Throwable {

                }
            });
            // Muestro un mensaje en la consola de quien se ha desconectado
            user.control.onClose(new F.Callback0() {
                public void invoke() {

                    System.out.println("El usuario:" + user.email + " se ha desconectado");
                    close();

                }
            });

            if (!Chat.users.containsKey(user.email)) {

               errors.setHasErrors(true);

            }
        }

        // borra el usuario y envia un mensaje a todos los usuarios para que lo borren de su lista
        if("close".equals(message)){

            System.out.println("close on receive");


        }

        // indico si ha habido errores o no
        getSender().tell(errors,self());


    }

    // Clase que describe la informacion basica del usuario
    static class Insert{

        String email;//meter UserConected)
        WebSocket.Out<JsonNode> channel;
        WebSocket.In<JsonNode> control;

        Insert(String email, WebSocket.Out<JsonNode> channel,WebSocket.In<JsonNode> control){

            this.email = email;
            this.channel = channel;
            this.control = control;

        }

    }

    static class GetData{

        //Chat.users;


    }

    // Permite detectar que error se ha producido
    static class Errors {

        private boolean hasErrors;


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

    }

    private boolean close(){

        return Chat.sendRequest("close");

    }

    private boolean findUser(String id){

        return Chat.sendRequest(id);

    }

    private boolean sendAll(){

        return true;

    }
}
