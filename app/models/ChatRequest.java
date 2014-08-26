package models;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by javi on 14/08/14.
 */
@Entity
public class ChatRequest extends Model {

    @Id
    @GeneratedValue
    public Long id;
    //usuario invitado
    @ManyToOne
    @JoinColumn(name = "USERHOST_ID")
    public User userHost;
    //usuario invitador
    @ManyToOne
    @JoinColumn(name = "USEROWNER_ID")
    public User userOwner;
    @OneToOne
    @JoinColumn(name = "CHAT_ID")
    public Chat chat;
    @Enumerated(EnumType.STRING)
    public Status status;
    @Temporal(TemporalType.DATE)
    public Date creationDate;
    @Temporal(TemporalType.DATE)
    public Date statusUpdateDate;

    public ChatRequest(User userHost, User userOwner, Date creationDate) {
        this.userHost = userHost;
        this.userOwner = userOwner;
        this.status = Status.ONHOLD;
        this.creationDate = creationDate;
        this.statusUpdateDate = null;
    }

    public enum Status{
        ACCEPTED,
        REJECTED,
        CANCELED,
        ONHOLD
    }

    public static Finder<Long,ChatRequest> find = new Finder<Long,ChatRequest>(
            Long.class, ChatRequest.class
    );

    // ************************************ GETTERS y SETTERS

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public User getUserHost() {
        return userHost;
    }
    public void setUserHost(User userHost) {
        this.userHost = userHost;
    }
    public User getUserOwner() {
        return userOwner;
    }
    public void setUserOwner(User userOwner) {
        this.userOwner = userOwner;
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public Date getStatusUpdateDate() {
        return statusUpdateDate;
    }
    public void setStatusUpdateDate(Date statusUpdateDate) {
        this.statusUpdateDate = statusUpdateDate;
    }
}
