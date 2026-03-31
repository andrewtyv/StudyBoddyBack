package DTO;

import model.MessageType;

import java.time.Instant;

public class MessageWsDTO {
    private Long id;
    private Long roomId;
    private String senderUsername;
    private String content;
    private MessageType messageType;
    private Instant createdAt;
    private String photoUrl;

    public MessageWsDTO() {
    }

    public MessageWsDTO(Long id, Long roomId, String senderUsername, String content, MessageType messageType, Instant createdAt, String photoUrl) {
        this.id = id;
        this.roomId = roomId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.messageType = messageType;
        this.createdAt = createdAt;
        this.photoUrl = photoUrl;
    }

    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getContent() {
        return content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}