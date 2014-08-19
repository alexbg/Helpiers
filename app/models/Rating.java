package models;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by javi on 11/08/14.
 */

public class Rating extends Model{
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne(fetch= FetchType.LAZY,optional=false)
    @JoinColumn(name="ID_USER")
    private User user;
    @OneToOne
    @JoinColumn(name = "CHAT_ID")
    private Chat chat;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="ID_CATEGORY")
    private Category category;

    private Integer value;

    @Temporal(TemporalType.DATE)
    private Date creationDate;

    public static Finder<Long, Rating> find = new Finder<Long,Rating>(Long.class, Rating.class);

    public Rating(User user, Category category, Integer value) {
        this.user = user;
        this.category = category;
        this.value = value;
        this.creationDate = new Date();
    }
    // ************************************************************* GETTERS Y SETTERS
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
    public Integer getValue() {
        return value;
    }
    public void setValue(Integer value) {
        this.value = value;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public static Finder<Long, Rating> getFind() {
        return find;
    }
    public static void setFind(Finder<Long, Rating> find) {
        Rating.find = find;
    }
}
