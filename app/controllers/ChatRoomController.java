package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.stream.JsonReader;
import controllers.actors.Chat;
import models.Category;
import models.Topic;
import models.User;
import models.UserConnected;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import com.google.gson.*;
import play.mvc.WebSocket;
import views.html.chatPrueba;
import views.html.chatRoom;
import views.html.index;
import views.html.waitingRoom;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by javi on 28/07/14.
 */
public class ChatRoomController extends Controller {

    private static List<Category> categoryList = Category.find.all();

    public static class NewTopic{
        private String topicText;
        private String categoryName;

        /**
         * valida el formulario recibido
         * @return String con el mensaje de error si se ha producido error al validar, o null si no ha habido error
         */
        public String validate() {
            if(categoryList == null){
                getCategoryList();
            }
            if(topicText!=null && topicText.length()>0 && topicText.length()<=140 && categoryName!=null){
                return null;
            }
            return "Campos incorrectos. Tiene que rellenar todos los campos de forma correcta: Selecciona una categoría" +
                    " válida y el texto debe tener menos de 140 caracteres";
        }

        public String getTopicText() {
            return topicText;
        }
        public void setTopicText(String topicText) {
            this.topicText = topicText;
        }
        public String getCategoryName() {
            return categoryName;
        }
        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }
    }

    /**
     * Display the home page.
     */
    public static Result index() {
        return ok(index.render(getCategoryList(), Form.form(NewTopic.class), User.getUserByEmail(session("email"))));
    }

    public static List<Category> getCategoryList(){
        if(categoryList == null || (categoryList.size()<1 && categoryList.size()>=0 )){
            categoryList = loadCategoriesFromJSON();
            System.out.println("categorías cargadas con éxito!!");
        }
        return categoryList;
    }

    private static List<Category> loadCategoriesFromJSON(){
        //cargar la lista de categorías desde el JSON de categorias: hacerlo de otra forma
        String JSONPath = "./public/resources/categories.json";
        List<Category> resultList;
        Gson gson = new Gson();
        JsonReader reader = null;
        try {
            reader = new JsonReader(new FileReader(JSONPath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Ruta del JSON de categorías no es válida: "+ JSONPath +"\n"+e.getMessage());
        }
        Category[] categories = gson.fromJson(reader, Category[].class);
        resultList = new ArrayList<Category>(Arrays.asList(categories));
        //Guardar cada categoría en la base de datos: es bueno que lo haga el controlador y no directamente el modelo
        addCategoriesToDB(resultList);
        return resultList;
    }

    private static boolean addCategoriesToDB(List<Category> categoryList){
        boolean result = false;
        categoryList = new ArrayList<Category>(categoryList);
        Iterator it;
        Category category = null;
        Date date = new java.util.Date();//fecha actual en formato java.util
        if(categoryList != null){
            it = categoryList.listIterator();
            while(it.hasNext()){
                category = (Category)it.next();
                category.setCreationDate(date);//obtiene la fecha en formato sql a partir de la java.util
                category.save();
            }
            result = true;
        }
        return result;
    }

    public static Result goToChatRoom(){
        Form<NewTopic> topicForm = Form.form(NewTopic.class).bindFromRequest();
        Topic newTopic = null;
        Category category = null;
        User user = null;
        UserConnected userConnected = null;
        System.out.println("ENTRA");
        if (topicForm.hasErrors()) {
            System.out.println("error en el formulario");
            return badRequest("An error has occurred");
        } else {
            //obtener el objeto categoría (NO CREAR UNO NUEVO)
            category = Category.find.where().eq("categoryName", topicForm.get().categoryName).findUnique();
            if(category == null) return badRequest("Categoría no válida");
            //obtener el usuario usando la sesión
            user = User.getUserByEmail(session("email"));
            if(user == null){
                return badRequest("Usuario sin sesión");
            }else{
                newTopic = new Topic(topicForm.get().topicText, category, user);
                //guardar el nuevo topic en la base de datos
                newTopic.save();
                //guardo los cambios en usuario, ya que se le ha añadido un topic
                newTopic.getUser().setTopic(newTopic);
                newTopic.getUser().save();
                //creo su UserConnected
                //userConnected = new UserConnected(user, newTopic, category);

                // Obtengo los usuarios conectados
            }
        }
        //return redirect(routes.ChatRoomController.chat(user.getUsername())); //PARA IR AL CHAT
        // PENSAR EN OBTENER LOS USUARIOS POR getUsers MEDIANTE AKKA CON UN ACTOR
        return ok(waitingRoom.render(newTopic.getTopicText(), category.getCategoryName(),Chat.getUsers()));

    }

    public static Result chat(){
        return ok(chatPrueba.render( User.getUserByEmail(session("email")).getUsername()));
    }

    // Prepara y controla el webSocket de waitingRoom
    public static WebSocket<JsonNode> socketRoom() {
        // session no se puede utilizar en le websocket, por eso obtengo la informacion aqui
        // obligatoriamente tiene que ser final si se va a obtener el valor desde el webSocket
        //final String email = session("email");

        System.out.println("Ha entrado");

        // Crear el userConnected

        User user = User.getUserByEmail(session("email"));
        Topic topic = user.getTopic();
        Category category = topic.getCategory();

        final UserConnected userConnected = new UserConnected(user,topic,category);

        return new WebSocket<JsonNode>() {

            // Called when the Websocket Handshake is done.
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
                // Crear el userConnected y pasarlo al setuser
                System.out.println("Se han dado la mano");
                // inserto el usuario
                Chat.insertUser(userConnected,out,in);
            }

        };
    }

    // SOLO PARA PRUEBAS
    /*public static Result prueba(){

        User user = User.getUserByEmail(session("email"));
        Topic topic = user.getTopic();
        Category category = topic.getCategory();

        UserConnected userConnected = new UserConnected(user,topic,category);

        return ok(userConnected.getUser().getEmail());
    }*/

    // Obtienes el js waitingRoom
    public static Result waiting(){

        return ok(views.js.waitingRoom.render());

    }

}
