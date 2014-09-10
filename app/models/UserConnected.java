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
