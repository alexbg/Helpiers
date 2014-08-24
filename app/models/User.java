package models;

import org.omg.CORBA.UNKNOWN;
import play.db.ebean.Model;

import javax.persistence.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class User extends Model {

	private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue
    public Long id;
    public String email;
	public String username = "Unnamed";
    public String password;
    public String userDescription;
    @Enumerated(EnumType.STRING)
    public Sex sex;
    @Temporal(TemporalType.DATE)
    public Date bornDate;
    @Temporal(TemporalType.DATE)
    public Date registerDate;
    @OneToOne
    @JoinColumn(name = "TOPIC_ID")
    public Topic topic;


    public List<Rating> RatingList;

	public static Finder<Long,User> find = new Finder<Long,User>(Long.class, User.class);

    public User(String email, String passw){
        this.email = email;
        this.password = passw;
        this.registerDate = new Date();
    }
    public User(){
        this.email = null;
        this.password = null;
        this.username = "Unnamed";
        this.userDescription = null;
        this.sex = Sex.UNKNOWN;
        this.bornDate = null;
        this.registerDate = null;
        this.topic = null;
    }


    public static User getUserByEmail(String email){
        User user = null;
        user = find.where().eq("email", email).findUnique();
        return user;
    }

    public enum Sex {
        MALE,
        FEMALE,
        UNKNOWN,
    }

    public boolean authenticate(String password){
		if(this.password.equals(password))
			return true;
		else return false;
	}

    //Método no comprobado aún
    public ArrayList<Rating> getUserReputation(){
        return new ArrayList<Rating>(Rating.find.where().eq("ID_USER", id).findList());
    }
	
	
	//**************************** GETTERS Y SETTERS
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public static Finder<Long, User> getFind() {
		return find;
	}
	public static void setFind(Finder<Long, User> find) {
		User.find = find;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getUserDescription() {

        /*if(userDescription == null ){
            userDescription = "No disponible";
        }*/
        return userDescription;
    }
    public void setUserDescription(String userDescription) {
        this.userDescription = userDescription;
    }
    public Sex getSex() {
        if(sex == null){
            sex = sex.UNKNOWN;
        }
        return sex;
    }
    public void setSex(Sex sex) {
        this.sex = sex;
    }
    public Date getBornDate() {
        return bornDate;
    }
    public void setBornDate(Date bornDate) {
        this.bornDate = bornDate;
    }
    public Date getRegisterDate() {
        return registerDate;
    }
    public void setRegisterDate(Date registerDate) {
        this.registerDate = registerDate;
    }
    public Topic getTopic() {
        return topic;
    }
    public void setTopic(Topic topic) {
        this.topic = topic;
    }
    public String getStingBornDate(){
        String result;
        DateFormat formatter = null;
        formatter = new SimpleDateFormat("yyyy-MM-dd");
        if(getBornDate() != null) {
            result = formatter.format(getBornDate().getTime());
        }
        else{
            result = "";
        }

        return result;
    }
    public String getStingRegDate(){
        String result;
        DateFormat formatter = null;
        formatter = new SimpleDateFormat("yyyy-MM-dd");
        if(getRegisterDate() != null){
            result = formatter.format(getRegisterDate().getTime());
        }
        else{
            result = "";
        }
        return result;
    }
}
