package models;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by javi on 28/07/14.
 */
@Entity
public class Topic extends Model{
    @Id
    @GeneratedValue
    private Long id;
    private String topicText;
    @Temporal(TemporalType.DATE)
    private Date creationDate;
    @OneToOne(mappedBy = "topic")
    private User user;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="ID_CATEGORY")
    private Category category;

    public static Finder<Long,Topic> find = new Finder<Long,Topic>(Long.class, Topic.class);

    public Topic(String topicText, Category category, User user) {
        this.topicText = topicText;
        this.creationDate = new Date();
        this.category = category;
        this.user = user;
    }

    // ***************************************** GETTERS Y SETTERS
    public String getTopicText() {
        return topicText;
    }
    public void setTopicText(String topicText) {
        this.topicText = topicText;
    }
    public Date getCreationDate() {
        return this.creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }
    public static Finder<Long, Topic> getFind() {
        return find;
    }
    public static void setFind(Finder<Long, Topic> find) {
        Topic.find = find;
    }
}
