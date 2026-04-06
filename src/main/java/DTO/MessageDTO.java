package DTO;

import model.MessageType;

import java.time.Instant;

public class MessageDTO {
    private Long id;
    private String content;
    private MessageType messageType;
    private String senderUsername;
    private Instant createdAt;
    private String photoUrl;
    private String photoName;
    private String photoContentType;

    public MessageDTO(Long id,
                      String content,
                      MessageType messageType,
                      String senderUsername,
                      Instant createdAt,
                      String photoUrl,
                      String photoName,
                      String photoContentType) {
        this.id = id;
        this.content = content;
        this.messageType = messageType;
        this.senderUsername = senderUsername;
        this.createdAt = createdAt;
        this.photoUrl = photoUrl;
        this.photoName = photoName;
        this.photoContentType = photoContentType;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getPhotoName() {
        return photoName;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }
}