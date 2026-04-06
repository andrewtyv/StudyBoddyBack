package DTO;

import model.MessageType;

import java.time.LocalDateTime;

public class MessageWsDTO {
    private Long id;
    private Long roomId;
    private String senderUsername;
    private String content;
    private MessageType messageType;
    private LocalDateTime createdAt;
    private String photoUrl;
    private String photoContentType;
    private String photoName;

    public MessageWsDTO(Long id,
                        Long roomId,
                        String senderUsername,
                        String content,
                        MessageType messageType,
                        LocalDateTime createdAt,
                        String photoUrl,
                        String photoContentType,
                        String photoName) {
        this.id = id;
        this.roomId = roomId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.messageType = messageType;
        this.createdAt = createdAt;
        this.photoUrl = photoUrl;
        this.photoContentType = photoContentType;
        this.photoName = photoName;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public String getPhotoName() {
        return photoName;
    }
}