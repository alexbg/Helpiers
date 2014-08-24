package models;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by javi on 14/08/14.
 */
@Entity
public class Chat extends Model {
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
    //poner REPORT
    @OneToOne
    @JoinColumn(name = "CHATREQUEST_ID")
    public ChatRequest chatRequest;
    @Temporal(TemporalType.DATE)
    public Date startTime;
    @Temporal(TemporalType.DATE)
    public Date endTime;

    public Chat(User userHost, User userOwner, ChatRequest chatRequest, Date startTime) {
        this.userHost = userHost;
        this.userOwner = userOwner;
        this.chatRequest = chatRequest;
        this.startTime = startTime;
        this.endTime = null;
    }

    // ************************************ GETTERS Y SETTERS
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
    public ChatRequest getChatRequest() {
        return chatRequest;
    }
    public void setChatRequest(ChatRequest chatRequest) {
        this.chatRequest = chatRequest;
    }
    public Date getStartTime() {
        return startTime;
    }
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    public Date getEndTime() {
        return endTime;
    }
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
