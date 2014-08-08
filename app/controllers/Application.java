package controllers;

import play.data.Form;
import play.mvc.*;

import com.fasterxml.jackson.databind.JsonNode; 
import views.html.*;

import models.*;
import views.html.defaultpages.error;
import views.html.home.presentation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Application extends Controller {

    public static class Profile{
        private String username;
        private String userDescription;
        private String bornDate;
        private String sex;

        public String validate(){
            Date date = null;
            DateFormat formatter = null;
            String errorMsg = null;
            if(username != null && userDescription != null && bornDate != null && sex != null){
                try {
                    formatter = new SimpleDateFormat("yyyy-MM-dd");
                    date = formatter.parse(bornDate);
                    //System.out.println("fecha de nacimiento: " + formatter.format(date.getTime()));
                    if(!sex.equals(User.Sex.UNKNOWN.toString()) && !sex.equals(User.Sex.FEMALE.toString())
                            && !sex.equals(User.Sex.MALE.toString()))
                        errorMsg =  "Error al validar el formulario: El sexo " + sex.toString() + " del usuario no está reconocido";
                } catch (ParseException e) {
                    errorMsg = "Error al validar el formulario: " + e.getMessage();
                }
            }else{
                errorMsg = "Error al validar el formulario; faltan campos";
            }
            return errorMsg;
        }


        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public String getUserDescription() {
            return userDescription;
        }
        public void setUserDescription(String userDescription) {
            this.userDescription = userDescription;
        }
        public String getBornDate() {
            return bornDate;
        }
        public void setBornDate(String bornDate) {
            this.bornDate = bornDate;
        }
        public String getSex() {
            return sex;
        }
        public void setSex(String sex) {
            this.sex = sex;
        }
    }

    /**
     * Display the presentation page.
     */
    public static Result showPresentation() {
        return ok(presentation.render());
    }

    /**
     * Display the chat room.
     */
    public static Result chatRoom(String username) {
        if(username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.ChatRoomController.index());
        }
        return ok(chatRoom.render(username));
    }

    public static Result chatRoomJs(String username) {
        return ok(views.js.chatRoom.render(username));
    }
    
    /**
     * Handle the chat websocket.
     */
    public static WebSocket<JsonNode> chat(final String username) {
        return new WebSocket<JsonNode>() {
            
            // Called when the Websocket Handshake is done.
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                
                // Join the chat room.
                try { 
                    ChatRoom.join(username, in, out);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }



    // *************************************************************
    public static Result editUserProfile() {
        return ok(views.html.editProfile.render(Form.form(Profile.class)));
    }

    public static Result UPSaveChanges() {
        Form<Profile> profileForm = Form.form(Profile.class).bindFromRequest();
        User user;
        User.Sex sex;
        if(profileForm.hasErrors()){
            return badRequest("Error al editar perfil; el formulario se ha validado con errores: "
                    + profileForm.globalError().message());
        }else{
            user = User.getUserByEmail(session("email"));
            user.setUsername(profileForm.get().username);
            user.setUserDescription(profileForm.get().userDescription);
            user.setSex(User.Sex.valueOf(profileForm.get().sex));
            try {
                user.setBornDate(new SimpleDateFormat("yyyy-MM-dd").parse(profileForm.get().bornDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            user.save();
        }
        return redirect(routes.ChatRoomController.index());
    }
    public static Result logout() {
        session().clear();
        return ok("Bye");
    }
}
