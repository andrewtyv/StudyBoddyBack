package model;


import jakarta.persistence.*;
import org.springframework.data.repository.cdi.Eager;

import java.time.Instant;

@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id",nullable = false)
    private Room room;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private java.time.Instant createdAt = java.time.Instant.now();
//    @Lob
//    @Basic(fetch = FetchType.LAZY)
    @Column(name = "photo_data", columnDefinition = "bytea")
    private byte[] photoData;

    @Column(name = "photo_name")
    private String photoName;

    @Column(name = "photo_content_type")
    private String photoContentType;
    public Message(){

    }

    public Message(MessageType messageType, Room room, User sender, String content) {
        this.messageType = messageType;
        this.room = room;
        this.sender = sender;
        this.content = content;
    }

    public void setUser(User sender) { this.sender = sender; }

    public void setMessageType(MessageType messageType){
        this.messageType =messageType;
    }
    public void setContent(String content){
        this.content = content;
    }
    public String getContent(){
        return this.content;
    }
    public User getSender(){
        return this.sender;
    }
    public MessageType getMessageType(){
        return this.messageType;
    }
    public Room getRoom(){
        return this.room;
    }

    public Long getId() {
        return this.id;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
    public void setRoom(Room room) {
        this.room = room;
    }

    public byte[] getPhotoData() {
        return photoData;
    }

    public void setPhotoData(byte[] photoData) {
        this.photoData = photoData;
    }

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }
}
