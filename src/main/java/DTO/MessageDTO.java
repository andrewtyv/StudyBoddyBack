package DTO;

import model.MessageType;

public class MessageDTO {

    private String content;
    private MessageType messageType;
    private String senderUsername;
    private Long id;
    private String photoUrl;



    public MessageDTO(){}

    public MessageDTO(Long id, String content, MessageType messageType, String senderUsername, String photoUrl) {
        this.id = id;
        this.content = content;
        this.messageType = messageType;
        this.senderUsername = senderUsername;
        this.photoUrl = photoUrl;
    }

    public String getContent() {
        return this.content;
    }

    public MessageType getMessageType() {
        return this.messageType;
    }

    public String getSenderUsername() {
        return this.senderUsername;
    }
    public String getPhotoUrl() {
        return this.photoUrl;
    }
    public Long id () {
        return this.id;
    }
}
