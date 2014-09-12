package models;

import play.db.ebean.Model;

import javax.persistence.*;

/**
 * Created by javi on 8/08/14.
 */
@Entity
public class UserConnected extends Model {

    @Id
    @GeneratedValue
    private Long id;
    @OneToOne
    @JoinColumn(name = "USER_ID")
    private User user;
    private Topic topic;
    private Category category;

    public static Model.Finder<Long,UserConnected> find = new Model.Finder<Long,UserConnected>(Long.class, UserConnected.class);

    public UserConnected(){
        user = null;
        topic = null;
        category = null;
    }

    public UserConnected(User user, Topic topic, Category category) {
        this.user = user;
        this.topic = topic;
        this.category = category;
    }

    public static UserConnected getUserConnectedByUser(User user){
        UserConnected result = null;
        result = find.where().eq("USER_ID", user.getId()).findUnique();
        return result;
    }

    //@Override
    public boolean equals(String username) {
        //if (this == o) return true;
        if (username == null) return false;

        //String that = o;

        if (!user.getUsername().equals(username)) return false;

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) return false;
        if (this == o) return true;
        if (!(o instanceof UserConnected)) return false;

        UserConnected that = (UserConnected) o;

        if (!user.getUsername().equals(that.user.getUsername())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return user.hashCode();
    }

    @Override
    public String toString() {
        return "UserConnected{" +
                "user=" + user +
                ", topic=" + topic +
                ", category=" + category +
                '}';
    }

    // ******************************** GETTERS AND SETTERS *********************************************
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public Topic getTopic() {
        return topic;
    }
    public void setTopic(Topic topic) {
        this.topic = topic;
    }
    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
}
