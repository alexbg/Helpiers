package controllers;

import models.User;
import play.data.*;
import play.data.validation.Constraints.Required;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.home.login;
import views.html.home.singup;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.security.MessageDigest;
import java.io.UnsupportedEncodingException;

public class LoginController extends Controller {

    /**
     * Clase interna para recibir el formulario de Login y validarlo
     */
    public static class Login {
        @Required
        public String email;
        public String password;

        /**
         * valida el formulario recibido
         * @return String con el mensaje de error si se ha producido error al validar, o null si no ha habido error
         */
        public String validate() {
            User user = User.getUserByEmail(email);
            if (user!=null && user.authenticate(generateHashSha1(password))) {
                return null;
            }
            return "Invalid user or password";
        }

        // ************************************* Getters y Setters *********************************************
        public String getEmail() {
            return email;
        }
        public void setEmail(String email) {
            this.email = email;
        }
        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
        // ******************************************************************************************************
    }

    /**
     * Clase interna para recibir el formulario de registro y validarlo
     */
    public static class NewUser {
        @Required
        public String email;
        public String password;

        /**
         * valida el formulario recibido
         * @return String con el mensaje de error si se ha producido error, o null si no ha habido error
         */
        public String validate() {
            if (User.find.where().eq("email", email).findUnique() == null) {
                return null;
            }
            return "User already exists";
        }
        // ************************************* Getters y Setters *********************************************
        public String getEmail() {
            return email;
        }
        public void setEmail(String email) {
            this.email = email;
        }
        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
        // ****************************************************************************************************
    }

    /**
     * Muestra la vista del formulario de Login
     * @return Result de la vista html
     */
    public static Result login() {
        return ok(
                login.render(Form.form(Login.class))
        );
    }

    /**
     * genera una sesion para el usuario que se autentica. Comprueba que no ha habido errores
     * @return Result con la vista index.html
     */
    public static Result authenticate() {
        Form<Login> loginForm = Form.form(Login.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return badRequest(login.render(loginForm));
        } else {
            session().clear();
            session("email", loginForm.get().email);
            return redirect(
                    routes.ChatRoomController.index()
            );
        }
    }

    /**
     * Muestra la vista del formulario de registro. Se ejecuta ante petición GET
     * @return Result con la vista del formulario de registro
     */
    public static Result showSingup() {
        return ok(
                singup.render(Form.form(NewUser.class))
        );
    }

    /**
     * Recibe el resultado POST del formulario de registro. Crea el nuevo usuario si no hay errores
     * @return Result con redirección al formulario de login
     */
    public static Result singup(){
        Form<NewUser> singUpForm = Form.form(NewUser.class).bindFromRequest();
        User user;
        if (singUpForm.hasErrors()) {
            return badRequest("User not registered, an error has occurred");
        } else {
            user = addUser(singUpForm.get().email, generateHashSha1(singUpForm.get().password));
            if(user == null)
                return badRequest("User not valid, insert longer password and a valid email");
        }
        return redirect(routes.LoginController.login());
    }

    /**
     * Añade un nuevo usuario a la base de datos
     * @param email email del nuevo usuario
     * @param password contraseña del nuevo usuario (función resumen sha-1)
     * @return user User nuevo usuario
     */
    private static User addUser(String email, String password){
        User user = null;
        if(email!=null && email.length()>=4 && password!=null && password.length()>=4){
            user = new User(email, password);
            user.save();
        }
        return user;
    }

    /**
     * genera la función resumen de un String pasado como parámetro
     * @param password contraseña de la que se quiere generar la función resumen
     * @return String con la función resumen en hexadecimal.
     */
    private static String generateHashSha1(String password){
        String sha1 = "";
        try{
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(password.getBytes("UTF-8"));
            sha1 = byteToHex(crypt.digest());
        }
        catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }
        return sha1;
    }

    /**
     * método auxiliar para generar un String a partir de una arrat de Byte
     * @param hash array de byte
     * @return String con el string (contenido en hexadecimal)
     */
    private static String byteToHex(final byte[] hash){
        Formatter formatter = new Formatter();
        for (byte b : hash){
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

}