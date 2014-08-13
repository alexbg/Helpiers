package controllers;

import com.google.gson.stream.JsonReader;
import models.Category;
import models.Topic;
import models.User;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import com.google.gson.*;
import views.html.chatPrueba;
import views.html.chatRoom;
import views.html.index;
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
            }
        }
        return redirect(routes.ChatRoomController.chat(user.getUsername())); //PARA IR AL CHAT

    }

    public static Result chat(String username){
        return ok(chatPrueba.render(username));
    }

}
