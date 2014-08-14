package models;

/**
 * Created by javi on 8/08/14.
 */
public class UserConnected {

    private User user;
    private Topic topic;
    private Category category;

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

    // ******************************** GETTERS AND SETTERS
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
}
